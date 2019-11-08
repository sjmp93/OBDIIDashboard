package com.example.sergiojosemp.canbt.activity;

import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.github.vassiliev.androidfilebrowser.FileBrowserActivity;
import com.example.sergiojosemp.canbt.service.ObdService;

import java.io.File;
import java.io.IOException;

public class MenuActivity extends AppCompatActivity {

    private final static String PREFERENCES = "preferences";
    public static final String EXTRA = new String("Caller");
    public static final String EXTRA_CONTENT = new String("StartActivity");

    private ObdService obdService;
    private FloatingActionButton dashboardButton;
    private FloatingActionButton settingsButton;
    private FloatingActionButton chartsButton;
    private FloatingActionButton diagnosticTroubleCodesButton;
    private FloatingActionButton verboseButton;

    private String TAG = "";
    @Inject
    private SharedPreferences preferences; //Toda la configuración se almacena en este objeto
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {

            if(getIntent().getExtras() == null || (getIntent().getExtras() != null && !getIntent().getExtras().getString(EXTRA).equals(EXTRA_CONTENT))) {
                obdService = ((ObdService.ObdServiceBinder) binder).getService();
                obdService.setContext(MenuActivity.this);
                try {
                    Log.d(TAG, getText(R.string.status_bluetooth_connected).toString());
                    obdService.isObdDevice();
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage().toString());
                }
                obdService.checkDevice();
            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the obdService is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the obdService.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            if(getIntent().getExtras() == null) {
                BluetoothSocket bluetoothSocket = obdService.getbluetoothSocket();
                if (bluetoothSocket.isConnected()) {
                    //TODO -  disconnect device safely
                }
            }
        }
    };

    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(MenuActivity.this, ObdService.class);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        if(serviceConn!=null)
            unbindService(serviceConn);
    }

    public void setObdIndicatorOn() {
        dashboardButton.setActivated(true);
        dashboardButton.setClickable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.obd_connector_icon);
            icon.setTint(getResources().getColor(R.color.obd_connector_icon));
            ImageView iconImage = (ImageView) findViewById(R.id.obdStatusIcon);
            iconImage.setImageResource(R.drawable.obd_connector_icon);
        }
    }



    public void setObdIndicatorNotAvailable() {
        dashboardButton.setActivated(false);
        dashboardButton.setClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.obd_connector_icon);
            icon.setTint(getResources().getColor(R.color.obd_disabled));
            ImageView iconImage = (ImageView) findViewById(R.id.obdStatusIcon);
            iconImage.setImageResource(R.drawable.obd_connector_icon);
        }
    }


    public void setObdIndicatorOff() {
        dashboardButton.setActivated(false);
        dashboardButton.setClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.obd_connector_icon);
            icon.setTint(getResources().getColor(R.color.night));
            ImageView iconImage = (ImageView) findViewById(R.id.obdStatusIcon);
            iconImage.setImageResource(R.drawable.obd_connector_icon);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);
        preferences = getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS);


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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

