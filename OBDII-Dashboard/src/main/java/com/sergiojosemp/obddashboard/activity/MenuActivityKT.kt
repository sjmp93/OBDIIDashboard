package com.sergiojosemp.obddashboard.activity


import android.content.*
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.MenuActivityBinding
import com.sergiojosemp.obddashboard.github.vassiliev.androidfilebrowser.FileBrowserActivity
import com.sergiojosemp.obddashboard.service.OBDKotlinCoroutinesTesting
import com.sergiojosemp.obddashboard.service.ObdService
import com.sergiojosemp.obddashboard.vm.MenuViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MenuActivityKT : AppCompatActivity(){

    private val PREFERENCES = "preferences"
    private val ONLINE_EXTRA = "ONLINE_EXTRA"


    private var obdService: ObdService? = null
    private val dashboardButton: FloatingActionButton? = null
    private val settingsButton: FloatingActionButton? = null
    private val chartsButton: FloatingActionButton? = null
    private val diagnosticTroubleCodesButton: FloatingActionButton? = null
    private val verboseButton: FloatingActionButton? = null


    private val TAG = "OBD-Log"

    @Inject
    private lateinit var preferences: SharedPreferences
    private lateinit var obd : OBDKotlinCoroutinesTesting
    private val serviceConn : OBDServiceConnectionOnMenu = OBDServiceConnectionOnMenu()
    private lateinit var binding: MenuActivityBinding
    private lateinit var viewModel: MenuViewModel
    private var onlineModeFlag: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState)
        onlineModeFlag = intent.extras!!.getBoolean(ONLINE_EXTRA) ?: false
        //setContentView(R.layout.menu_activity)
        //getExtraData(ONLINE_EXTRA)
        preferences = getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS)
        binding = DataBindingUtil.setContentView(
            this, R.layout.menu_activity
        )

        viewModel = ViewModelProviders.of(this).get(MenuViewModel::class.java)

        viewModel!!.selectedOption!!.observe(this, androidx.lifecycle.Observer {

            if (it.equals(1)) { // Dashboard Mode
                val dashboardActivity = Intent(this, DashboardActivity::class.java);
                startActivity(dashboardActivity)
            } else if (it.equals(2)) { // Verbose Mode
                val verboseActivity = Intent(this, VerboseActivityKT::class.java);
                startActivity(verboseActivity)
            } else if (it.equals(3)) { // Chart Mode TODO refactor this
                val REQUEST_CODE_PICK_FILE = 2
                val fileExploreIntent = Intent(
                    FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                    null,
                    this,
                    FileBrowserActivity::class.java
                )
                val sdcard = Environment.getExternalStorageDirectory()
                var path = sdcard.getPath() + "/" + preferences.getString(
                    SettingsActivity.DIRECTORY_FULL_LOGGING_KEY,
                    getString(R.string.default_dirname_full_logging)
                ) + "/"
                fileExploreIntent.putExtra(FileBrowserActivity.startDirectoryParameter, path)
                startActivityForResult(fileExploreIntent, REQUEST_CODE_PICK_FILE)
            } else if (it.equals(4)) { // DTC Mode
                val dtcActivity = Intent(this, DiagnosticTroubleCodeActivity::class.java);
                startActivity(dtcActivity)
            } else if (it.equals(5)) { // Settings
                val settingsActivity = Intent(this, SettingsActivity::class.java);
                startActivity(settingsActivity)
            } else {
                // TODO (remove) nothing to do for the moment
            }


        })
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this
        binding.verboseButton.setOnClickListener(){
            obd.printThing()
        }


    }

    fun startDashBoard(){
        val dashboardActivity = Intent(this, DashboardActivity::class.java);
        startActivity(dashboardActivity)
        //bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    override fun onResume() {
        super.onResume()
        val serviceIntent = Intent(this, OBDKotlinCoroutinesTesting::class.java);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    override fun onBackPressed() {
        if(onlineModeFlag)
            MaterialAlertDialogBuilder(this, R.style.AlertDialog)
                .setTitle(getString(R.string.exit_title))
                .setMessage(getString(R.string.go_back_advice))
                .setPositiveButton(getString(R.string.ok_option), DialogInterface.OnClickListener { dialog, which ->
                    obd.disconnectFromDevice()
                    Log.d(TAG, "Going back to discover activity")
                    // clear activities stack and go back to start menu activity
                    val intent = Intent(this, StartMenuActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                })
                .setNegativeButton(getString(R.string.cancel_option), /* listener = */ null)
                .show();
        else{
            Log.d(TAG, "Going back to start menu activity")
            super.onBackPressed()
        }

    }

    override fun onPause() {
        super.onPause()
        if(serviceConn!=null)
            unbindService(serviceConn);
        //obd.disconnectFromDevice()
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    inner class OBDServiceConnectionOnMenu : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { //TODO here we have to fill a textView that shows OBD connection status
            obd = (service as OBDKotlinCoroutinesTesting.ObdServiceBinder).service
            obd.liveOutput.observe(binding.lifecycleOwner!!, androidx.lifecycle.Observer {
                GlobalScope.launch { Log.d(com.sergiojosemp.obddashboard.vm.TAG, "From Menu Activity: Byte received ${it[0].toByte().toString(16)} ${it[1].toByte().toString(16)} ${it[2].toByte().toString(16)} ${it[3].toByte().toString(16)}") }
            })

            obd.commandResult.observe(binding.lifecycleOwner!!, androidx.lifecycle.Observer {
                //This routine emulates led blinking when data is received
                /*GlobalScope.launch {
                    viewModel.setValue("Invisible")
                    delay(100L)
                    viewModel.setValue("")
                    delay(200L)
                    viewModel.setValue("Invisible")
                    delay(200L)
                    viewModel.setValue("")
                    delay(200L)
                    viewModel.setValue("Invisible")
                    delay(100L)
                    viewModel.setValue("")
                }*/
            })

            obd.btConnectionStatus.observe(binding.lifecycleOwner!!, androidx.lifecycle.Observer {
                GlobalScope.launch {
                    if (it) {
                        //viewModel.setValue(getString(R.string.status_obd_connected))
                        viewModel.setConnectedStatusLed(true)
                    } else {
                        //viewModel.setValue(getString(R.string.status_obd_disconnected))
                        viewModel.setConnectedStatusLed(false)
                    }
                }
            })


        }

    }
}
