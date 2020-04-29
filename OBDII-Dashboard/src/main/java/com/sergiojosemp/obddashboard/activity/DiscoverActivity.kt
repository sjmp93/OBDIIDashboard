package com.sergiojosemp.obddashboard.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.adapter.CustomRecyclerViewAdapter
import com.sergiojosemp.obddashboard.databinding.DiscoverActivityBinding
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.model.BluetoothModel
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel
import kotlinx.android.synthetic.main.discover_activity.*
import java.util.*
import kotlin.collections.ArrayList

class DiscoverActivity: AppCompatActivity() {
    private val REQUEST_COARSE_LOCATION = 5
    private val DEVICE_ITEM_TEXT_SIZE = 24
    private val MINIMUM_HEIGHT = 150


    private val discoveredDevices =
        ArrayList<String>() //Lista de dispositivos encontrados durante la búsqueda



    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val devices =
        HashMap<String, BluetoothDevice>() //Diccionario que relaciona una mac con el objeto BluetoothDevice correspondiente

    private val TAG = "" // Para el log
    private lateinit var binding: DiscoverActivityBinding
    private var bluetoothReceiver: BroadcastReceiver? = null //Objeto que recibirá eventos del sistema


    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this, R.layout.discover_activity)

        val discoverViewModel: DiscoverViewModel = ViewModelProviders.of(this).get(DiscoverViewModel::class.java)

        discoverViewModel.devices.observe(this, androidx.lifecycle.Observer {
            System.out.println("Observer")
        })

        binding.viewmodel = discoverViewModel
        binding.lifecycleOwner = this

        bluetooth_device_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CustomRecyclerViewAdapter( mutableListOf<BluetoothDeviceModel>())
        }
        supportActionBar!!.hide()
        supportActionBar!!.title = getText(R.string.discovered_devices_text)

        //Tomar lista de dispositivos encontrados, para ello se crea un receiver para el intent ACTION_FOUND
        bluetoothReceiver = object : BroadcastReceiver() {
            fun containDevices(discoveredDevice: BluetoothDeviceModel){

            }
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

                        System.out.println(btDiscoveredDevice.toString())
                    }
                }
            }
            fun onDestroy() {
                unregisterReceiver(this)
            }
        }

        //Configuramos la aplicación para recibir el evento por parte del sistema
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        checkLocationPermission() //Necesario para poder utilizar ACTION_FOUND a partir de Android 6
        //Iniciamos la búsqueda de dispositivos
        bluetoothAdapter?.startDiscovery()
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