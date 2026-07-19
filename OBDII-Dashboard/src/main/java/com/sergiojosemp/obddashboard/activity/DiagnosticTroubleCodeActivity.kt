package com.sergiojosemp.obddashboard.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.DtcOptionsMenuBinding
import com.sergiojosemp.obddashboard.service.ObdService
import com.sergiojosemp.obddashboard.vm.DtcViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class DiagnosticTroubleCodeActivity : AppCompatActivity() {

    private val TAG = DiagnosticTroubleCodeActivity::class.java.name

    private lateinit var viewModel: DtcViewModel
    private var isBound = false
    private var obdService: ObdService? = null

    private var optionsBinding: DtcOptionsMenuBinding? = null

    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            obdService = (binder as ObdService.ObdServiceBinder).getService()
            obdService?.setContext(this@DiagnosticTroubleCodeActivity)
            obdService?.setDtc(true)
            Log.d(TAG, getString(R.string.dashboard_linking_log_text))
            obdService?.startService()
        }

        override fun onServiceDisconnected(className: ComponentName?) {}
    }

    private fun getDict(keyId: Int, valId: Int): Map<String, String> {
        val keys = resources.getStringArray(keyId)
        val vals = resources.getStringArray(valId)
        val dict = mutableMapOf<String, String>()
        for (i in keys.indices) {
            dict[keys[i]] = vals[i]
        }
        return dict
    }

    private fun getDtc() {
        if (obdService != null &&
            obdService?.getbluetoothSocket()?.isConnected == true &&
            obdService?.queueEmpty() == true) {
            val troubleCodes = obdService?.getTroubleCodes() ?: ""
            fillView(troubleCodes)
        }
    }

    private fun fillView(res: String) {
        val container = findViewById<android.widget.FrameLayout>(R.id.content_container)
        container.removeAllViews()
        layoutInflater.inflate(R.layout.diagnostic_trouble_code_activity, container, true)

        val listView = findViewById<android.widget.ListView>(R.id.listView)
        val dtcVals = getDict(R.array.dtc_keys, R.array.dtc_values)

        val dtcCodes = ArrayList<String>()
        if (res.isNotEmpty()) {
            for (dtcCode in res.split("\n")) {
                dtcCodes.add(dtcCode + " : " + dtcVals[dtcCode])
                Log.d(TAG, dtcCode + " : " + dtcVals[dtcCode])
            }
        } else {
            dtcCodes.add(getString(R.string.text_noerrors))
        }

        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            dtcCodes
        )
        listView.adapter = adapter
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun startFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun stopFullScreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            startFullScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        optionsBinding = DataBindingUtil.setContentView(this, R.layout.dtc_options_menu)
        viewModel = ViewModelProvider(this).get(DtcViewModel::class.java)

        optionsBinding?.lifecycleOwner = this
        optionsBinding?.viewmodel = viewModel

        optionsBinding?.clearDtcButton?.setOnClickListener {
            if (obdService != null &&
                obdService?.getbluetoothSocket()?.isConnected == true &&
                obdService?.queueEmpty() == true) {

                lifecycleScope.launch {
                    Log.d(TAG, getString(R.string.trying_reset))
                    viewModel.setOutputText(getString(R.string.trying_reset))

                    val result = withContext(Dispatchers.IO) {
                        try {
                            val clear = ResetTroubleCodesCommand()
                            clear.run(
                                obdService!!.getbluetoothSocket()!!.getInputStream(),
                                obdService!!.getbluetoothSocket()!!.getOutputStream()
                            )
                            clear.getFormattedResult()
                        } catch (e: IOException) {
                            "IO_ERROR"
                        } catch (ie: InterruptedException) {
                            "INTERRUPTED"
                        }
                    }

                    when (result) {
                        "IO_ERROR" -> {
                            viewModel.setOutputText(getString(R.string.error_establishing_connection))
                            Log.e(TAG, getString(R.string.error_establishing_connection))
                        }
                        "INTERRUPTED" -> {
                            viewModel.setOutputText(getString(R.string.error_cleaning_dtc))
                            Log.e(TAG, getString(R.string.error_cleaning_dtc))
                        }
                        else -> {
                            Log.d(TAG, getString(R.string.reset_result) + result)
                            viewModel.setOutputText(getString(R.string.reset_result) + result)
                        }
                    }

                    val refresh = Intent(applicationContext, DiagnosticTroubleCodeActivity::class.java)
                    startActivity(refresh)
                }
            }
        }

        optionsBinding?.getDtcButton?.setOnClickListener {
            getDtc()
        }

        startFullScreen()

        if (bindService(Intent(this, ObdService::class.java), serviceConn, Context.BIND_AUTO_CREATE)) {
            isBound = true
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        val serviceIntent = Intent(this, ObdService::class.java)
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {
            isBound = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            try { unbindService(serviceConn) } catch (e: Exception) {}
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try { unbindService(serviceConn) } catch (e: Exception) {}
            isBound = false
        }
    }
}
