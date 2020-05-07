package com.sergiojosemp.obddashboard.activity


import android.content.*
import android.nfc.Tag
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.MenuActivityBinding
import com.sergiojosemp.obddashboard.service.OBDKotlinCoroutinesTesting
import com.sergiojosemp.obddashboard.service.ObdService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MenuActivityKT : AppCompatActivity(){

    private val PREFERENCES = "preferences"


    private var obdService: ObdService? = null
    private val dashboardButton: FloatingActionButton? = null
    private val settingsButton: FloatingActionButton? = null
    private val chartsButton: FloatingActionButton? = null
    private val diagnosticTroubleCodesButton: FloatingActionButton? = null
    private val verboseButton: FloatingActionButton? = null

    private val TAG = "OBD-Log"

    @Inject
    private var preferences //Toda la configuración se almacena en este objeto
            : SharedPreferences? = null
    private lateinit var obd : OBDKotlinCoroutinesTesting
    private val serviceConn : OBDServiceConnection = OBDServiceConnection()
    private lateinit var binding: MenuActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.menu_activity)
        preferences = getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS)
        binding = DataBindingUtil.setContentView(
            this, R.layout.menu_activity)

        binding.lifecycleOwner = this
        binding.verboseButton.setOnClickListener(){
            obd.printThing()
        }
        //OBDKotlinCoroutinesTesting test = new OBDKotlinCoroutinesTesting();

        /*
        dashboardButton = findViewById(R.id.dashboardButton);
        settingsButton = findViewById(R.id.settingsButton);
        diagnosticTroubleCodesButton = findViewById(R.id.diagnosticTroubleCodesButton);
        chartsButton = findViewById(R.id.chartsButton);
        verboseButton = findViewById(R.id.verboseButton);

        dashboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent DashboardActivity = new Intent(MenuActivity.this, DashboardActivity.class);
                startActivity(DashboardActivity);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent SettingsActivity = new Intent(MenuActivity.this, SettingsActivity.class);
                startActivity(SettingsActivity);
            }
        });

        diagnosticTroubleCodesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent DiagnosticTroubleCodeActivity = new Intent(MenuActivity.this, DiagnosticTroubleCodeActivity.class);
                startActivity(DiagnosticTroubleCodeActivity);
            }
        });

        chartsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int REQUEST_CODE_PICK_FILE = 2;

                Intent fileExploreIntent = new Intent(
                        FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                        null,
                        MenuActivity.this,
                        FileBrowserActivity.class
                );
                File sdcard = Environment.getExternalStorageDirectory();
                String path = sdcard.getPath() + "/" + preferences.getString(SettingsActivity.DIRECTORY_FULL_LOGGING_KEY,
                        getString(R.string.default_dirname_full_logging)) + "/";

                fileExploreIntent.putExtra(FileBrowserActivity.startDirectoryParameter, path);//El explorador empezará desde el directorio indicado en las preferencias.
                startActivityForResult(fileExploreIntent, REQUEST_CODE_PICK_FILE);
            }
        });

        verboseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent VerboseActivity = new Intent(MenuActivity.this, VerboseActivity.class);
                startActivity(VerboseActivity);
            }
        });

        setObdIndicatorOff();
        //Modo sin conexión
        if(getIntent().getExtras() != null && getIntent().getExtras().getString(EXTRA).equals(EXTRA_CONTENT)){
            setObdIndicatorNotAvailable();
        }

     */
    }

    override fun onResume() {
        super.onResume()
        val serviceIntent = Intent(this, OBDKotlinCoroutinesTesting::class.java);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialog)
            .setTitle("Exit")
            .setMessage("Going back will result in bluetooth device being disconnected, are you sure?")
            .setPositiveButton("Ok",  DialogInterface.OnClickListener { dialog, which ->
                obd.disconnectFromDevice()
                Log.d(TAG,"Going back to discover activity")
                super.onBackPressed()})
            .setNegativeButton("Cancel", /* listener = */ null)
            .show();
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


    inner class OBDServiceConnection : ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            obd = (service as OBDKotlinCoroutinesTesting.ObdServiceBinder).service
            obd.liveOutput.observe(binding.lifecycleOwner!!, androidx.lifecycle.Observer {
                GlobalScope.launch { Log.d(TAG,"From Menu Activity: Byte received ${it[0].toByte().toString(16)} ${it[1].toByte().toString(16)} ${it[2].toByte().toString(16)} ${it[3].toByte().toString(16)}" ) }
            })
        }

    }
}