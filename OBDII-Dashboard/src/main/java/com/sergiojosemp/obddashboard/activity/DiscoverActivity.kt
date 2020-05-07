package com.sergiojosemp.obddashboard.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.adapter.CustomRecyclerViewAdapter
import com.sergiojosemp.obddashboard.databinding.DiscoverActivityBinding
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.service.OBDKotlinCoroutinesTesting
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel
import kotlinx.android.synthetic.main.discover_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiscoverActivity: AppCompatActivity() {
    private val REQUEST_COARSE_LOCATION = 5
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val TAG = "OBD-Log"
    private lateinit var binding: DiscoverActivityBinding
    private var bluetoothReceiver: BroadcastReceiver? = null



    private lateinit var obd : OBDKotlinCoroutinesTesting

    inner class OBDServiceConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            obd = (service as OBDKotlinCoroutinesTesting.ObdServiceBinder).service
            obd.liveOutput.observe(binding.lifecycleOwner!!, androidx.lifecycle.Observer {
                GlobalScope.launch { Log.d(TAG,"From Discover Activity : Byte received ${it[0].toByte().toString(16)} ${it[1].toByte().toString(16)} ${it[2].toByte().toString(16)} ${it[3].toByte().toString(16)}" ) }
            })
        }

    }
    private val serviceConn : DiscoverActivity.OBDServiceConnection = OBDServiceConnection()




    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this, R.layout.discover_activity)

        val discoverViewModel: DiscoverViewModel = ViewModelProviders.of(this).get(DiscoverViewModel::class.java)
        // Observe changes on viewModel.device. When triggered, it tries to start a bluetooth socket in order to establish a bluetooth connection with the observed device


        discoverViewModel.valueReceived.observe(this, androidx.lifecycle.Observer {
            GlobalScope.launch { Log.d(TAG,"Byte received ${it[0].toByte().toString(16)} ${it[1].toByte().toString(16)} ${it[2].toByte().toString(16)} ${it[3].toByte().toString(16)}" ) }
        })

        discoverViewModel.device.observe(this, androidx.lifecycle.Observer {
            obd.num = 2 //TODO remove this - for debugging purpose
            GlobalScope.launch{ obd.connectToDevice(bluetoothAdapter!!,it.mac!!,discoverViewModel.connecting) } // Separated thread to not to block UI
        })

        discoverViewModel.connecting.observe(this, androidx.lifecycle.Observer {
            if(it == false && discoverViewModel.device.value != null) { //Only true if device connected
                val menuActivity = Intent(this, MenuActivityKT::class.java)
                startActivity(menuActivity)
            }
        })

        binding.viewmodel = discoverViewModel
        binding.lifecycleOwner = this
        // Connects to the RecyclerView
        bluetooth_device_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CustomRecyclerViewAdapter(context, mutableListOf<BluetoothDeviceModel>())
        }
        //Needed to set bold text style to CollapsingToolbarLayout title
        binding.collapsingToolbar.setExpandedTitleTypeface(Typeface.create(binding.collapsingToolbar.getExpandedTitleTypeface(), Typeface.BOLD));
        binding.collapsingToolbar.setCollapsedTitleTypeface(Typeface.create(binding.collapsingToolbar.getExpandedTitleTypeface(), Typeface.BOLD));

        // Take a list of near bluetooth devices
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Only if receiver is triggered by new device found
                if (BluetoothDevice.ACTION_FOUND == intent.action) {
                    //We take Bluetooth device from intent extra, contains name and MAC address
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    //We save discovered device on viewmodel list if it doesn't already contains it
                    val btDiscoveredDevice = BluetoothDeviceModel(name= if (device.name != null) device.name else "Unknown device" ,mac=device.address)
                    if(!binding.viewmodel!!.containsDevice(btDiscoveredDevice)){
                        val array :ArrayList<BluetoothDeviceModel> = binding.viewmodel!!.devices.value!!
                        array.add(btDiscoveredDevice)
                        binding.viewmodel!!.devices.value = array
                       Log.d(TAG,"${btDiscoveredDevice.name} with MAC address: ${btDiscoveredDevice.mac} discovered.")
                    }
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener(OnRefreshListener {
            bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.startDiscovery()
            GlobalScope.launch {
                delay(1000L) //some delay to let bluetoothAdapter to start discovering devices
                while (bluetoothAdapter?.isDiscovering ?: false){
                    delay(1000L)
                }
                //When bluetoothAdapter stops to be discovering devices, then, set Refreshing progress to false
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        })

        val intent = Intent(this,OBDKotlinCoroutinesTesting::class.java)
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        // Setting up the receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        checkLocationPermission() // We need to ckeck permissions after Android 6 in order to search bluetooth devices

        val intent = Intent(this,OBDKotlinCoroutinesTesting::class.java)
        bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);

        bluetoothAdapter?.startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        bluetoothAdapter?.cancelDiscovery()
        if (bluetoothReceiver != null)
            unregisterReceiver(bluetoothReceiver)
        if(serviceConn!=null)
            unbindService(serviceConn);
    }


    protected fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_COARSE_LOCATION
            )
        }
    }
}