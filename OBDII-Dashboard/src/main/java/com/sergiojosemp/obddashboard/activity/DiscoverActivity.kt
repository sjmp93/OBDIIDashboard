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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.adapter.BluetoothDevicesRecyclerViewAdapter
import com.sergiojosemp.obddashboard.databinding.DiscoverActivityBinding
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.service.ObdService
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiscoverActivity: AppCompatActivity() {
    private val REQUEST_COARSE_LOCATION = 5
    private val ONLINE_EXTRA = "ONLINE_EXTRA"
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val TAG = "OBD-Log"
    private lateinit var binding: DiscoverActivityBinding
    private var bluetoothReceiver: BroadcastReceiver? = null



    private var obd: ObdService? = null

    inner class OBDServiceConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "ObdService disconnected unexpectedly")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            obd = (service as ObdService.ObdServiceBinder).getService()
            // LiveData observation from old service removed - migrate in later phase
        }
    }

    private val serviceConn : DiscoverActivity.OBDServiceConnection = OBDServiceConnection()




    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this, R.layout.discover_activity)

        val discoverViewModel: DiscoverViewModel = ViewModelProvider(this).get(DiscoverViewModel::class.java)
        // Observe changes on viewModel.device. When triggered, it tries to start a bluetooth socket in order to establish a bluetooth connection with the observed device


        discoverViewModel.valueReceived.observe(this, androidx.lifecycle.Observer {
            Log.d(TAG,"Byte received ${it[0].toByte().toString(16)} ${it[1].toByte().toString(16)} ${it[2].toByte().toString(16)} ${it[3].toByte().toString(16)}")
        })

        discoverViewModel.device.observe(this, androidx.lifecycle.Observer {
            // obd.connectToDevice() removed - migrate in later phase
        })

        discoverViewModel.connecting.observe(this, androidx.lifecycle.Observer {
            if(it == false && discoverViewModel.device.value != null) { //Only true if device connected
                val menuActivity = Intent(this, MenuActivityKT::class.java)
                menuActivity.putExtra(ONLINE_EXTRA, true)
                startActivity(menuActivity)
            } else if(it == false && discoverViewModel.device.value == null){

            }
        })

        binding.viewmodel = discoverViewModel
        binding.lifecycleOwner = this
        // Connects to the RecyclerView
        binding.obdDataList.apply {
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = BluetoothDevicesRecyclerViewAdapter(this@DiscoverActivity, mutableListOf<BluetoothDeviceModel>())
        }
        //Needed to set bold text style to CollapsingToolbarLayout title
        val toolbarTypeface = Typeface.DEFAULT_BOLD
        binding.collapsingToolbar.setExpandedTitleTypeface(toolbarTypeface)
        binding.collapsingToolbar.setCollapsedTitleTypeface(toolbarTypeface)

        // Take a list of near bluetooth devices
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Only if receiver is triggered by new device found
                if (BluetoothDevice.ACTION_FOUND == intent.action) {
                    //We take Bluetooth device from intent extra, contains name and MAC address
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    //We save discovered device on viewmodel list if it doesn't already contains it
                    val btDiscoveredDevice = BluetoothDeviceModel(name= if (device?.name != null) device!!.name else "Unknown device" ,mac=device!!.address)
                    if(!binding.viewmodel!!.containsDevice(btDiscoveredDevice)){
                        val array :ArrayList<BluetoothDeviceModel> = binding.viewmodel!!.devices.value!!
                        array.add(btDiscoveredDevice)
                        binding.viewmodel!!.devices.value = array
                       Log.d(TAG,"${btDiscoveredDevice.name} with MAC address: ${btDiscoveredDevice.mac} discovered.")
                    }
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            bluetoothAdapter?.cancelDiscovery()
            Log.d(TAG,"Trying to discover more devices...")
            bluetoothAdapter?.startDiscovery()
            lifecycleScope.launch {
                delay(1000L) //some delay to let bluetoothAdapter to start discovering devices
                while (bluetoothAdapter?.isDiscovering ?: false){
                    delay(1000L)
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        val intent = Intent(this,ObdService::class.java)
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        // Setting up the receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        checkLocationPermission() // We need to ckeck permissions after Android 6 in order to search bluetooth devices

        val intent = Intent(this,ObdService::class.java)
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

    override fun onBackPressed() {
        val intent = Intent(this,ObdService::class.java)
        stopService(intent)
        super.onBackPressed()
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