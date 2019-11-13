package com.sergiojosemp.obddashboard.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.sergiojosemp.obddashboard.R;
import com.sergiojosemp.obddashboard.service.ObdService;


public class ConnectActivity extends AppCompatActivity {

    private final static String PREFERENCES = "preferences";
    private final static String EXTRANAME = "name";
    private final static String EXTRAMAC = "mac";

    private FloatingActionButton bluetoothConnectButton; //Botón para establecer la conexión bluetooth
    private String TAG = ""; // Para el log
    private BluetoothAdapter bluetoothAdapter; //Objeto adaptador Bluetooth
    private ObdService obdService; //Servicio que corre en fondo y se encarga de gestionar la conexión con el adaptador OBD

    private SharedPreferences preferences; //Toda la configuración se almacena en este objeto

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            obdService = ((ObdService.ObdServiceBinder) binder).getService();
            obdService.setPreferences(preferences);
            obdService.startService();
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
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        setContentView(R.layout.connect_activity);
        getSupportActionBar().setTitle(getText(R.string.connect_activity_title));
        //Definimos animaciones para los botones
        final Animation spin = AnimationUtils.loadAnimation(this, R.anim.spin);
        final Animation appear = AnimationUtils.loadAnimation(this, R.anim.appear);
        //Configuramos el FAB
        bluetoothConnectButton = findViewById(R.id.bluetoothConnectButton);
        bluetoothConnectButton.setActivated(true);
        bluetoothConnectButton.setClickable(true);
        bluetoothConnectButton.setAlpha((float) 1.0);
        bluetoothConnectButton.startAnimation(appear);

        //Se toman los valores de la actividad anterior
        final String name, mac;

        name = getIntent().getExtras().getString(EXTRANAME);
        mac = getIntent().getExtras().getString(EXTRAMAC);


        Intent serviceIntent = new Intent(ConnectActivity.this, ObdService.class);
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG,getText(R.string.service_bounded_text) + " " + getText(R.string.connect_activity));
        }

        //Se crea la llamada a la acción de conectar al dispositivo seleccionado
        OnClickListener onClickListenerConnect = new OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                bluetoothConnectButton.startAnimation(spin);
                //mostrarDispositivos();
                Log.d(TAG, getText(R.string.connecting_text).toString());
                //Thread de conexión asíncorono
                String auxMac = mac.substring(1); //Elimina el salto de línea
                if (bluetoothAdapter.checkBluetoothAddress(auxMac)) {
                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(auxMac);
                    obdService.setBluetoothDevice(bluetoothDevice);
                    obdService.setContext(ConnectActivity.this);
                    obdService.connectToDevice();

                    if (obdService.getbluetoothSocket().isConnected()) {
                        Intent MenuActivity = new Intent(ConnectActivity.this, MenuActivity.class);
                        startActivity(MenuActivity);
                    }
                }
            }
        };

        //Se asigna la acción al botón
        bluetoothConnectButton.setOnClickListener(onClickListenerConnect);


        //Se actualiza la información
        TextView nameText = findViewById(R.id.deviceNameText);
        TextView macText = findViewById(R.id.deviceMacText);
        nameText.setText(name);
        macText.setText(mac);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Se vuelve a la discoveryActivity, así se comienza con el discovery de dispositivos Bluetooth
        bluetoothAdapter.startDiscovery();
    }

    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(ConnectActivity.this, ObdService.class);
        startService(serviceIntent);
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {

        }
    }

    protected void onPause() {
        super.onPause();
        /*if(serviceConn!=null && obdService != null && obdService.getbluetoothSocket() != null && !obdService.getbluetoothSocket().isConnected()) {
            //unbindService(serviceConn);
        }*/
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        /*if(serviceConn!=null)
            unbindService(serviceConn);*/
    }

}