package com.example.sergiojosemp.canbt.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
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
import android.widget.Button;
import android.widget.TextView;

import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.service.ObdService;

import java.io.IOException;
import java.util.ArrayList;

//import com.example.sergiojosemp.canbt.ConnectThread;

public class ConnectActivity extends AppCompatActivity {

    //Constantes
    private static final int REQUEST_COARSE_LOCATION = 5;
    private final ArrayList<String> mArrayAdapter = new ArrayList<String>(); //Lista de dispositivos encontrados durante la búsqueda
    private final BroadcastReceiver mReceiver = null; //Objeto que recibirá eventos del sistema
    private int REQUEST_ENABLE_BT = 1;
    //Interfaz gráfica
    private TextView tv; //Texto indicador del estado del adaptador Bluetooth
    private FloatingActionButton btButton; //Botón de encendido del adaptador Bluetooth
    private Button btConnect; //Botón para iniciar la búsqueda de dispositivos
    private String TAG = "";
    //Bluetooth
    private BluetoothAdapter mBluetoothAdapter; //Objeto adaptador Bluetooth
    //Variables y objetos
    private int result = 0; //
    //private ConnectThread ct;
    private ObdService service;

    private SharedPreferences prefs; //Toda la configuración se almacena en este objeto


    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((ObdService.ObdThingsBinder) binder).getService();
            service.setPrefs(prefs);
            //service.setContext(ConnectActivity.this);
            try {
                service.startService();
            } catch (IOException ioe) {
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
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("preferences",
                Context.MODE_PRIVATE);
        setContentView(R.layout.connect);
        getSupportActionBar().setTitle("Conectar a");
        //Definimos animaciones para los botones
        final Animation rotar = AnimationUtils.loadAnimation(this, R.anim.rotacion);
        final Animation aparicion = AnimationUtils.loadAnimation(this, R.anim.aparicion);
        //Configuramos el FAB
        btButton = findViewById(R.id.floatButtonConnect);
        btButton.setActivated(true);
        btButton.setClickable(true);
        btButton.setAlpha((float) 1.0);
        btButton.startAnimation(aparicion);

        //Se toman los valores de la actividad anterior
        final String nombre, mac;

        nombre = getIntent().getExtras().getString("name");
        mac = getIntent().getExtras().getString("mac");


        Intent serviceIntent = new Intent(ConnectActivity.this, ObdService.class);
        //startService(serviceIntent);
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {

        }

        //Se crea la llamada a la acción de conectar al dispositivo seleccionado
        OnClickListener onClickListenerConnect = new OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                btButton.startAnimation(rotar);
                //mostrarDispositivos();
                Log.d(TAG, "Connecting");
                //Thread de conexión asíncorono
                String macAux = mac.substring(1);
                if (mBluetoothAdapter.checkBluetoothAddress(macAux)) {
                    BluetoothDevice btd = mBluetoothAdapter.getRemoteDevice(macAux);
                    service.setBluetoothDevice(btd);
                    service.setContext(ConnectActivity.this);
                    service.connect();

                    if (service.getSock().isConnected()) {
                        Intent MainMenuActivity = new Intent(ConnectActivity.this, MainMenuActivity.class);
                        startActivity(MainMenuActivity);
                        //unbindService(serviceConn);
                    }
                }

            }
        };

        //Se asigna la acción al botón
        btButton.setOnClickListener(onClickListenerConnect);


        //Se actualiza la información
        TextView tName = findViewById(R.id.tName);
        TextView tMac = findViewById(R.id.tMac);
        tName.setText(nombre);
        tMac.setText(mac);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        mBluetoothAdapter.startDiscovery();
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
        if(serviceConn!=null && service != null && !service.getSock().isConnected()) {
            //unbindService(serviceConn);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        /*if(serviceConn!=null)
            unbindService(serviceConn);*/
    }

}