package com.example.sergiojosemp.canbt.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.service.BluetoothThings;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_COARSE_LOCATION = 5;
    public static java.lang.String UUID = new String("38400000-8cf0-11bd-b23e-10b96e4ef00d");
    private final ArrayList<String> mArrayAdapter = new ArrayList<String>(); //Lista de dispositivos encontrados durante la búsqueda
    private BroadcastReceiver mReceiver = null; //Objeto que recibirá eventos del sistema
    private int REQUEST_ENABLE_BT = 1;
    private TextView tv; //Texto indicador del estado del adaptador Bluetooth
    private FloatingActionButton btButton; //Botón de encendido del adaptador Bluetooth
    private Button btConnect; //Botón para iniciar la búsqueda de dispositivos
    private Button offlineButton; //modo sin conexión
    private BluetoothAdapter mBluetoothAdapter = null; //Objeto adaptador Bluetooth
    private int result = 0; //
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        prefs = getSharedPreferences("preferences",
                Context.MODE_PRIVATE);
        //Instancia de nuevo objeto bluetooth
        final BluetoothThings bluetooth = new BluetoothThings();
        mBluetoothAdapter = bluetooth.getBluetoothAdapter();

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
                //DiscoveryActivity.putExtra("bluetoothAdapter", bluetooth);
                startActivity(DiscoveryActivity);
                //mostrarDispositivos();         //Cambio de actividad a descubrimiento de dispositivos
            }
        };

        //Listener para el botón de Conectar, que lleva a la actividad de descubrimiento de dispositivos
        OnClickListener onClickListenerOffline = new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent MainMenuActivity = new Intent(getApplicationContext(), MainMenuActivity.class);

                MainMenuActivity.putExtra("caller", "MainActivity"); //Si quien llama a MainMenuActivity es MainActivity, entonces se usa el modo sin conexión
                startActivity(MainMenuActivity);
                //mostrarDispositivos();         //Cambio de actividad a descubrimiento de dispositivos
            }
        };

        //Asignación de objetos de interfaz gráfica
        tv = findViewById(R.id.Text);
        btButton = findViewById(R.id.floatButton);
        btButton.setOnClickListener(onClickListenerState);
        btConnect = findViewById(R.id.btConnect);
        btConnect.setOnClickListener(onClickListenerConnect);
        offlineButton = findViewById(R.id.offlineButton);
        offlineButton.setOnClickListener(onClickListenerOffline);


        //Mostrar estado del adaptador BT y modificar la interfaz en consecuencia
        estadoBT();

    }

    protected void estadoBT() {
        //Definimos un intent para recibir los cambios de estado del adaptador bluetooth
        Intent stateBtIntent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        //Definimos animaciones para los botones
        final Animation aparicion = AnimationUtils.loadAnimation(this, R.anim.aparicion);
        final Animation desaparicion = AnimationUtils.loadAnimation(this, R.anim.desaparicion);
        //Definimos las acciones que se llevarán a cabo según el estado del adaptador BT (apagado/encendido)
        /*final BroadcastReceiver*/ mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

                if (!mBluetoothAdapter.isEnabled()) {
                    tv.setText("Bluetooth desactivado");
                    btButton.setActivated(true);
                    btButton.setClickable(true);
                    btButton.setAlpha((float) 1.0);
                    btButton.startAnimation(aparicion);
                    btConnect.startAnimation(desaparicion);

                }

                if (mBluetoothAdapter.isEnabled()) {
                    tv.setText("Bluetooth activado");
                    btButton.setActivated(false);
                    btButton.setClickable(false);
                    btButton.startAnimation(desaparicion);
                    btConnect.startAnimation(aparicion);

                }
            }

            public void onDestroy() {
                unregisterReceiver(this);
            }
        };
        //Configuramos la aplicación para recibir el evento por parte del sistema
        IntentFilter intentFilter = new IntentFilter(stateBtIntent.getAction());
        registerReceiver(mReceiver, intentFilter);
        //Primera ejecución -> determinamos si BT on u off.
        mReceiver.onReceive(this, stateBtIntent); //Llamamos al método onReceive para actualizar el estado nada más abrir la aplicación
    }

    protected void procedimientoDeConexión(String nombre, String mac) {
        mBluetoothAdapter.cancelDiscovery();
        setContentView(R.layout.connect);
        getSupportActionBar().setTitle("Conectar a");

        TextView tName = findViewById(R.id.tName);
        TextView tMac = findViewById(R.id.tMac);
        tName.setText(nombre);
        tMac.setText(mac);

    }

    protected void rutinaBT() {

        Animation rotar = AnimationUtils.loadAnimation(this, R.anim.rotacion);

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (!mBluetoothAdapter.isEnabled()) {

            tv.setText("Bluetooth desactivado");

            enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            onActivityResult(REQUEST_ENABLE_BT, result, enableBtIntent);
            btButton.startAnimation(rotar);
        }

        if (mBluetoothAdapter.isEnabled()) {
            tv.setText("Bluetooth activado");
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mReceiver != null)
            unregisterReceiver(mReceiver);
    }


}
