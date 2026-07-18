package com.sergiojosemp.obddashboard.activity

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.ConnectActivityBinding
import com.sergiojosemp.obddashboard.service.ObdService
import com.sergiojosemp.obddashboard.vm.ConnectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectActivity : AppCompatActivity() {

    private val TAG = "ConnectActivity"

    private lateinit var binding: ConnectActivityBinding
    private lateinit var viewModel: ConnectViewModel
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isBound = false
    private var obdService: ObdService? = null
    private var deviceMac: String = ""

    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            obdService = (binder as ObdService.ObdServiceBinder).getService()
            obdService?.startService()
        }

        override fun onServiceDisconnected(className: ComponentName?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.connect_activity)
        viewModel = ViewModelProvider(this).get(ConnectViewModel::class.java)

        val name = intent.getStringExtra(ConnectViewModel.EXTRA_NAME) ?: ""
        deviceMac = intent.getStringExtra(ConnectViewModel.EXTRA_MAC) ?: ""

        viewModel.setDevice(name, deviceMac)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val spin = AnimationUtils.loadAnimation(this, R.anim.spin)
        val appear = AnimationUtils.loadAnimation(this, R.anim.appear)

        binding.bluetoothConnectButton.isActivated = true
        binding.bluetoothConnectButton.isClickable = true
        binding.bluetoothConnectButton.alpha = 1.0f
        binding.bluetoothConnectButton.startAnimation(appear)

        if (bindService(Intent(this, ObdService::class.java), serviceConn, Context.BIND_AUTO_CREATE)) {
            isBound = true
        }

        binding.bluetoothConnectButton.setOnClickListener {
            viewModel.startConnecting()
            binding.bluetoothConnectButton.startAnimation(spin)
            Log.d(TAG, getString(R.string.connecting_text))

            lifecycleScope.launch {
                val cleanMac = deviceMac.substring(1)
                val macPattern = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
                if (macPattern.matches(cleanMac)) {
                    val bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(cleanMac)

                    withContext(Dispatchers.IO) {
                        obdService?.setBluetoothDevice(bluetoothDevice)
                        obdService?.setContext(this@ConnectActivity)
                        obdService?.connectToDevice()
                    }

                    if (obdService?.getbluetoothSocket()?.isConnected == true) {
                        val menuIntent = Intent(this@ConnectActivity, MenuActivityKT::class.java)
                        startActivity(menuIntent)
                    } else {
                        viewModel.finishConnecting(false)
                    }
                } else {
                    viewModel.finishConnecting(false)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        bluetoothAdapter?.startDiscovery()
    }

    override fun onResume() {
        super.onResume()
        val serviceIntent = Intent(this, ObdService::class.java)
        startService(serviceIntent)
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {
            isBound = true
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try { unbindService(serviceConn) } catch (e: Exception) {}
            isBound = false
        }
    }
}
