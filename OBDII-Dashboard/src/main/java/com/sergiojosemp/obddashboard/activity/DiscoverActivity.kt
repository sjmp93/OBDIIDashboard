package com.sergiojosemp.obddashboard.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.adapter.CustomRecyclerViewAdapter
import com.sergiojosemp.obddashboard.databinding.DiscoverActivityBinding
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.service.OBDKotlinCoroutinesTesting
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel
import kotlinx.android.synthetic.main.discover_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class DiscoverActivity: AppCompatActivity() {
    private val REQUEST_COARSE_LOCATION = 5
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val TAG = "OBD-Log"
    private lateinit var binding: DiscoverActivityBinding
    private var bluetoothReceiver: BroadcastReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this, R.layout.discover_activity)

        val discoverViewModel: DiscoverViewModel = ViewModelProviders.of(this).get(DiscoverViewModel::class.java)
        // Observe changes on viewModel.device. When triggered, it tries to start a bluetooth socket in order to establish a bluetooth connection with the observed device

        var valueReceived: MutableLiveData<ByteArray> = MutableLiveData()
        valueReceived.observe(this, androidx.lifecycle.Observer {
            GlobalScope.launch { Log.d(TAG,"Byte received ${it[0].toByte().toString(16)} ${it[1].toByte().toString(16)} ${it[2].toByte().toString(16)} ${it[3].toByte().toString(16)}" ) }
        })
        discoverViewModel.device.observe(this, androidx.lifecycle.Observer {
            /*doAsync {
                if (BluetoothAdapter.checkBluetoothAddress(it.mac)) {
                    val bluetoothDevice: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(it.mac)
                    var tmp: BluetoothSocket? = null
                    try {
                        tmp = bluetoothDevice.javaClass.getMethod("createRfcommSocket", *arrayOf<Class<*>?>(Int::class.javaPrimitiveType))
                            .invoke(bluetoothDevice, 1) as BluetoothSocket

                        tmp!!.connect()
                        Log.d(TAG, getString(R.string.connected_to_device))

                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        discoverViewModel.connecting.postValue(false)
                    }
                }
            }.execute()*/

            val obdCoroutine = OBDKotlinCoroutinesTesting(bluetoothAdapter!!, it.mac!!, valueReceived)
            discoverViewModel.connecting.postValue(false)
        })

        binding.viewmodel = discoverViewModel
        binding.lifecycleOwner = this
        // Connects to the RecyclerView
        bluetooth_device_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CustomRecyclerViewAdapter(context, mutableListOf<BluetoothDeviceModel>())
        }

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

                        System.out.println("${btDiscoveredDevice.name} with MAC address: ${btDiscoveredDevice.mac} discovered.")
                    }
                }
            }
        }

        // Setting up the receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        checkLocationPermission() // We need to ckeck permissions after Android 6 in order to search bluetooth devices

        bluetoothAdapter?.startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        if (bluetoothReceiver != null)
            unregisterReceiver(bluetoothReceiver)
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

//https://stackoverflow.com/questions/44525388/asynctask-in-android-with-kotlin
class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}