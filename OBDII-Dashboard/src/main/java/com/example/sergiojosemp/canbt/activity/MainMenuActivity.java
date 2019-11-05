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
import android.widget.TextView;

import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.service.ObdService;

import java.io.File;
import java.io.IOException;

public class MainMenuActivity extends AppCompatActivity {

    private ObdService service;
    private TextView obd_indicator;
    private boolean isBound = false;
    private FloatingActionButton dashboard;
    private FloatingActionButton settings;
    private FloatingActionButton records;
    private String TAG = "";
    @Inject
    private SharedPreferences prefs; //Toda la configuración se almacena en este objeto
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            if(getIntent().getExtras() != null && !getIntent().getExtras().getString("caller").equals("MainActivity")) {
                service = ((ObdService.ObdThingsBinder) binder).getService();
                service.setContext(MainMenuActivity.this);
                //try {
                //service.startService();
                //Hacer aquí el checkDevice()?
                try {
                    Log.d(TAG, "Connected");
                    service.isObdDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                service.checkDevice();
                //}// catch (IOException ioe) {
                //}
            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            if(getIntent().getExtras() == null) {
                BluetoothSocket sock = service.getSock();
                if (sock.isConnected()) {
                    /*try {
                        sock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                }
            }
        }
    };

    //private BluetoothAdapter mBluetoothAdapter; //Objeto adaptador Bluetooth
    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(MainMenuActivity.this, ObdService.class);
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {
            isBound = true;
        }
    }

    protected void onPause() {
        super.onPause();
        if(serviceConn!=null)
            unbindService(serviceConn);
    }

    public void setObdIndicatorOn() {
        dashboard.setActivated(true);
        dashboard.setClickable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.connector);
            icon.setTint(getResources().getColor(R.color.obd_icon));
            ImageView icono = (ImageView) findViewById(R.id.imageView2);
            icono.setImageResource(R.drawable.connector);

        }
        //this.obd_indicator.setTextColor(ContextCompat.getColor(MainMenuActivity.this, R.color.engine_coolant_progress));
    }



    public void setObdIndicatorNotAvailable() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.connector);
            icon.setTint(getResources().getColor(R.color.obd_disabled));
            ImageView icono = (ImageView) findViewById(R.id.imageView2);
            icono.setImageResource(R.drawable.connector);

        }
        //this.obd_indicator.setTextColor(ContextCompat.getColor(MainMenuActivity.this, R.color.engine_coolant_progress));
    }


    public void setObdIndicatorOff() {
        dashboard.setActivated(false);
        dashboard.setClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.connector);
            icon.setTint(getResources().getColor(R.color.night));
            ImageView icono = (ImageView) findViewById(R.id.imageView2);
            icono.setImageResource(R.drawable.connector);

        }
        //this.obd_indicator.setTextColor(ContextCompat.getColor(MainMenuActivity.this, R.color.fuel_consumption_rate_iconbg));
    }

    protected void onCreate(Bundle savedInstanceState) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_menu);
        prefs = getSharedPreferences("preferences",
                Context.MODE_MULTI_PROCESS);


        obd_indicator = findViewById(R.id.obd_indicator);
        dashboard = findViewById(R.id.floatingActionButton8);

        settings = findViewById(R.id.floatingActionButton);
        dashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent DashboardActivity = new Intent(MainMenuActivity.this, DashboardActivity.class);
                startActivity(DashboardActivity);
            }
        });
        dashboard.setActivated(false);
        dashboard.setClickable(false);

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent SettingsActivity = new Intent(MainMenuActivity.this, SettingsActivity.class);
                startActivity(SettingsActivity);
            }
        });

        records = findViewById(R.id.floatingActionButton7);
        records.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



 /*               Intent ChartActivity = new Intent(getApplicationContext(), ChartActivity.class);
                //DiscoveryActivity.putExtra("bluetoothAdapter", bluetooth);
                startActivity(ChartActivity);
                //mostrarDispositivos();         //Cambio de actividad a descubrimiento de dispositivos
*/

                final int REQUEST_CODE_PICK_DIR = 1;
                final int REQUEST_CODE_PICK_FILE = 2;

                Intent fileExploreIntent = new Intent(
                        FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                        null,
                        MainMenuActivity.this,
                        FileBrowserActivity.class
                );
                File sdcard = Environment.getExternalStorageDirectory();
                String path = sdcard.getPath() + "/" + prefs.getString(SettingsActivity.DIRECTORY_FULL_LOGGING_KEY,
                        getString(R.string.default_dirname_full_logging)) + "/";

                fileExploreIntent.putExtra(
                    FileBrowserActivity.startDirectoryParameter, path
                  );//Here you can add optional start directory parameter, and file browser will start from that directory.
                startActivityForResult(
                        fileExploreIntent,
                        REQUEST_CODE_PICK_FILE
                );



            }
        });
        //Puede no ser necesario
        if(getIntent().getExtras() != null && getIntent().getExtras().getString("caller").equals("MainActivity")){
            setObdIndicatorNotAvailable();
            dashboard.setActivated(false);
            dashboard.setClickable(false);

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
/*        try {
            service.getSock().close();
            if(serviceConn!=null)
                unbindService(serviceConn);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }






}

