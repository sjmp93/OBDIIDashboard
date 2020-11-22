package com.sergiojosemp.obddashboard.activity

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.MainActivityBinding
import com.sergiojosemp.obddashboard.model.BluetoothModel
import com.sergiojosemp.obddashboard.service.OBDKotlinCoroutinesTesting
import com.sergiojosemp.obddashboard.vm.StartViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class StartMenuActivity: AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST = 0
    private val ONLINE_EXTRA = "ONLINE_EXTRA"

    private lateinit var viewModel: StartViewModel
    private lateinit var binding: MainActivityBinding
    private val btDevice = BluetoothAdapter.getDefaultAdapter()
    private val btEventReceiver: BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(contxt: Context?, intent: Intent?){
            System.out.println("Switching BT state from ${btDevice.isEnabled} to ${!btDevice.isEnabled}")
            //To avoid triggering switch method twice (during intermediate state and final state) we check State inside intent extra
            if ((intent!!.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON ||
                            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) &&
                    viewModel.data.value!!.state != btDevice.isEnabled) {
                viewModel.switchBT()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Binding with layout file
        binding = DataBindingUtil.setContentView<MainActivityBinding>(this,
                R.layout.main_activity)

        viewModel = ViewModelProviders.of(this).get(StartViewModel::class.java)

        //Bind boolean value with BT device state (if BT adapter is enabled, then, the app starts with that state for TextData...)
        viewModel.data.value =
            BluetoothModel(state = btDevice.isEnabled)

        //Observe changes at data to switch btDevice state when data is changed
        viewModel.data.observe(this, Observer<BluetoothModel>(){
            System.out.println("Observer triggered, switching BT state from ${btDevice.isEnabled} to ${!btDevice.isEnabled}")
            if(viewModel.data.value!!.state!!) btDevice.enable() else btDevice.disable()
        })

        bindingSetup(binding)
        requestPermissions()

        //Request corresponging permissions for BT functionality
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                MY_PERMISSIONS_REQUEST);

        /*val newIntent = Intent("test")
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmReceiver = AlarmReceiver()
        alarmReceiver.SetContext(this)
        alarmReceiver.SetAlarm()
        */
    }


    fun bindingSetup(binding: MainActivityBinding){
        //Binding ViewModel with view
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        //Navigation to DiscoveryActivity
        binding.discoverButton.setOnClickListener {
            val DiscoverActivity =
                Intent(applicationContext, DiscoverActivity::class.java)
            startActivity(DiscoverActivity)
        }

        binding.offlineModeButton.setOnClickListener{
            val menuActivity =
                Intent(applicationContext, MenuActivityKT::class.java)
            menuActivity.putExtra(ONLINE_EXTRA, false)
            startActivity(menuActivity)
        }


    }

    override fun onStart() {
        super.onStart()
        //Setup for Bluetooth's state changes event receiver
        val bluetoothStatusIntent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
        val intentFilter = IntentFilter(bluetoothStatusIntent.action)
        registerReceiver(btEventReceiver, intentFilter)
    }

    fun requestPermissions() {
        // Here, thisActivity is the current activity
        val permissionsToRequest: MutableList<String> =
            ArrayList()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.INTERNET)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WAKE_LOCK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.WAKE_LOCK)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS)
        }
        if (permissionsToRequest.size != 0) {
            val permissionsToRequestArray =
                arrayOfNulls<String>(permissionsToRequest.size)
            //permissionsToRequest.toArray<String>(permissionsToRequestArray)
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray<String>(),
                MY_PERMISSIONS_REQUEST
            )
        } else {
            Log.d(
                "OBDDashboard-log",
                getText(R.string.grant_permissions_from_settings).toString()
            )
        }
    }
}







