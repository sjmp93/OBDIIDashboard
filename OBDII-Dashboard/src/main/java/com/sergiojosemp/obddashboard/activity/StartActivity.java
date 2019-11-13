package com.sergiojosemp.obddashboard.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.sergiojosemp.obddashboard.R;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends AppCompatActivity {


    protected static final int REQUEST_ENABLE_BT = 1;
    public static final String EXTRA = new String("Caller");
    public static final String EXTRA_CONTENT = new String("StartActivity");
    public static final String UUID = new String("38400000-8cf0-11bd-b23e-10b96e4ef00d");
    private static final int RESULT = 0; //
    private final static String PREFERENCES = "preferences";
    private static final String TAG = "";
    private static int MY_PERMISSIONS_REQUEST;

    private BroadcastReceiver eventReceiver = null; //Objeto que recibirá eventos del sistema
    private TextView bluetoothStatusText; //Texto indicador del estado del adaptador Bluetooth
    private FloatingActionButton enableBluetoothButton; //Botón de encendido del adaptador Bluetooth
    private Button bluetoothDiscoveryButton; //Botón para iniciar la búsqueda de dispositivos
    private Button offlineModeButton; //modo sin conexión
    private BluetoothAdapter bluetoothAdapter = null; //Objeto adaptador Bluetooth

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);


        preferences = getSharedPreferences(PREFERENCES, //Se inicializa para obtener las preferencias de la aplicación
                Context.MODE_PRIVATE);

        //Instancia de nuevo objeto bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        /*---------------------------  Definición de Listeners para botones ---------------------------*/
        //Listener para el botón de encendido del BT (botón con el logo de BT)
        OnClickListener onClickListenerState = new OnClickListener() {
            @Override
            public void onClick(View v) {
                rutinaBT();        //Modificación del estado actual del módulo BT mostrado en el campo de texto de la pantalla principal
            }
        };
        //Listener para el botón de Conectar, que lleva a la actividad de descubrimiento de dispositivos
        OnClickListener onClickListenerConnect = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent DiscoveryActivity = new Intent(getApplicationContext(), DiscoveryActivity.class);
                startActivity(DiscoveryActivity);
            }
        };
        //Listener para el botón de Modo Offline, que lleva a la actividad del menú de la aplicación
        OnClickListener onClickListenerOffline = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent MainMenuActivity = new Intent(getApplicationContext(), MenuActivity.class);
                MainMenuActivity.putExtra(EXTRA, EXTRA_CONTENT); // Si quien llama a MenuActivity es StartActivity, entonces se usa el modo sin conexión
                startActivity(MainMenuActivity);
            }
        };
        /*--------------------------- Inicialización de la interfaz gráfica ---------------------------*/
        //Asignación de objetos de la interfaz gráfica
        bluetoothStatusText = findViewById(R.id.bluetoothStatusText);
        enableBluetoothButton = findViewById(R.id.enableBluetoothButton);
        bluetoothDiscoveryButton = findViewById(R.id.discoverButton);
        offlineModeButton = findViewById(R.id.offlineModeButton);
        //Asignación de Listeners a los botones de la interfaz gráfica
        enableBluetoothButton.setOnClickListener(onClickListenerState);
        bluetoothDiscoveryButton.setOnClickListener(onClickListenerConnect);
        offlineModeButton.setOnClickListener(onClickListenerOffline);

        //Solicitar permisos
        requestPermissions();

        //Mostrar estado del adaptador BT y modificar la interfaz en consecuencia
        bluetoothStatusControl();


    }

    protected void bluetoothStatusControl() {
        //Definimos un intent para recibir los cambios de estado del adaptador bluetooth
        Intent bluetoothStatusIntent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        //Definimos animaciones para los botones
        final Animation appear = AnimationUtils.loadAnimation(this, R.anim.appear);
        final Animation disappear = AnimationUtils.loadAnimation(this, R.anim.disappear);
        //Definimos las acciones que se llevarán a cabo según el estado del adaptador BT (apagado/encendido)
        eventReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothStatusText.setText(R.string.bluetooth_disabled);
                    enableBluetoothButton.setActivated(true);
                    enableBluetoothButton.setClickable(true);
                    enableBluetoothButton.setAlpha((float) 1.0);
                    enableBluetoothButton.startAnimation(appear);
                    bluetoothDiscoveryButton.startAnimation(disappear);

                }

                if (bluetoothAdapter.isEnabled()) {
                    bluetoothStatusText.setText(R.string.bluetooth_enabled);
                    enableBluetoothButton.setActivated(false);
                    enableBluetoothButton.setClickable(false);
                    enableBluetoothButton.startAnimation(disappear);
                    bluetoothDiscoveryButton.startAnimation(appear);
                }
            }

            public void onDestroy() {
                unregisterReceiver(this);
            }
        };
        //Configuramos la aplicación para recibir el evento por parte del sistema
        IntentFilter intentFilter = new IntentFilter(bluetoothStatusIntent.getAction());
        registerReceiver(eventReceiver, intentFilter);
        //Primera ejecución -> determinamos si BT on u off.
        eventReceiver.onReceive(this, bluetoothStatusIntent); //Llamamos al método onReceive para actualizar el estado nada más abrir la aplicación
    }

    protected void rutinaBT() {

        Animation spin = AnimationUtils.loadAnimation(this, R.anim.spin);

        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (!bluetoothAdapter.isEnabled()) {

            bluetoothStatusText.setText(R.string.bluetooth_disabled);

            enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            onActivityResult(REQUEST_ENABLE_BT, RESULT, enableBluetoothIntent);
            enableBluetoothButton.startAnimation(spin);
        }

        if (bluetoothAdapter.isEnabled()) {
            bluetoothStatusText.setText(R.string.bluetooth_enabled);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(eventReceiver != null)
            unregisterReceiver(eventReceiver);
    }

    public void requestPermissions(){
        // Here, thisActivity is the current activity

        List<String> permissionsToRequest = new ArrayList<String> ();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(new String(Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.BLUETOOTH);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.INTERNET);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.WAKE_LOCK);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add((String )Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
        }
        if(permissionsToRequest.size() != 0) {
            String[] permissionsToRequestArray = new String[permissionsToRequest.size()];
            permissionsToRequest.toArray(permissionsToRequestArray);
            ActivityCompat.requestPermissions(this,
                    permissionsToRequestArray,
                    MY_PERMISSIONS_REQUEST);
        }else{
            Log.d(TAG, getText(R.string.grant_permissions_from_settings).toString());
        }
    }
}
