package com.example.sergiojosemp.canbt.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.sergiojosemp.canbt.R;

import java.util.ArrayList;
import java.util.HashMap;

public class DiscoveryActivity extends AppCompatActivity {

    //Constantes
    private static final int REQUEST_COARSE_LOCATION = 5;
    private final ArrayList<String> mArrayAdapter = new ArrayList<String>(); //Lista de dispositivos encontrados durante la búsqueda
    private  BroadcastReceiver mReceiver = null; //Objeto que recibirá eventos del sistema
    private int REQUEST_ENABLE_BT = 1;
    //Interfaz gráfica
    private TextView tv; //Texto indicador del estado del adaptador Bluetooth
    private FloatingActionButton btButton; //Botón de encendido del adaptador Bluetooth
    private Button btConnect; //Botón para iniciar la búsqueda de dispositivos
    //Bluetooth
    private BluetoothAdapter mBluetoothAdapter; //Objeto adaptador Bluetooth
    private HashMap<String, BluetoothDevice> devices = new HashMap<String, BluetoothDevice>();
    //Variables y objetos
    private int result = 0; //
    private String TAG = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pruebainterfaz);
        getSupportActionBar().setTitle("Dispositivos encontrados");

        //Tomamos adaptador BT de la actividad anterior
        //BluetoothThings bluetooth = (BluetoothThings) getIntent().getExtras().getSerializable("bluetoothAdapter");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Asignación de objetos de interfaz gráfica
        final LinearLayout lTabla = findViewById(R.id.tableList);
        lTabla.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        //Tomar lista de dispositivos encontrados, para ello se crea un receiver para el intent ACTION_FOUND
        /*final BroadcastReceiver*/ mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // Cada vez que se encuentre un dispositivo
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //Tomamos el objeto BluetoothDevice del INTENT, contendrá nombre y MAC
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    //Guardamos el dispositivo encontrado en una lista
                    if (!mArrayAdapter.contains(device.getName() + "\n" + device.getAddress())) {
                        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }

                    //Creamos objetos de texto para mostrar el nombre y MAC de cada dispositivo
                    // encontrado en una vista de tabla, cada fila es un dispositivo
                    TextView tNombre = new TextView(context);
                    TextView tMac = new TextView(context);

                    final String nombre = (device.getName() != null) ? device.getName() : "Nombre desconocido";
                    final String mac = device.getAddress();
                    if (!devices.containsKey(mac)) {
                        devices.put(mac, device);

                        tNombre.setText(nombre);
                        tNombre.setTypeface(null, Typeface.BOLD);
                        tNombre.setTextColor(Color.parseColor("#0090ff")); //Color azul
                        tNombre.setTextSize(24);

                        tMac.setText("\n" + mac);
                        tMac.setTextSize(24);


                        //Creamos una fila para añadir al layout de tabla
                        TableRow tFila = new TableRow(context);

                        //Añadimos el nombre y MAC a la fila
                        tFila.addView(tNombre);
                        tMac.setVisibility(View.INVISIBLE);
                        tFila.addView(tMac);
                        tFila.setClickable(true);

                        //Creamos la acción que se tomará al pulsar sobre una fila
                        OnClickListener onClickListenerRow = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent ConnectActivity = new Intent(getApplicationContext(), ConnectActivity.class);

                                TableRow tr = (TableRow) v;

                                TextView tAuxNombre, tAuxMac;
                                tAuxNombre = (TextView) tr.getVirtualChildAt(0);
                                tAuxMac = (TextView) tr.getVirtualChildAt(1);

                                ConnectActivity.putExtra("name", tAuxNombre.getText());
                                ConnectActivity.putExtra("mac", tAuxMac.getText());

                                //Se detiene la búsqueda de dispositivos
                                mBluetoothAdapter.cancelDiscovery();

                                //DiscoveryActivity.putExtra("bluetoothAdapter", bluetooth);
                                startActivity(ConnectActivity);
                            }
                        };
                        tFila.setOnClickListener(onClickListenerRow);

                        //Se actualiza la interfaz con el nuevo dispositivo encontrado
                        tFila.setMinimumHeight(150);
                        lTabla.addView(tFila);
                    }
                    //debug
                    Log.d(TAG, mArrayAdapter.get(mArrayAdapter.size() - 1));
                }
            }

            public void onDestroy() {
                unregisterReceiver(this);
            }
        };

        //Configuramos la aplicación para recibir el evento por parte del sistema
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        checkLocationPermission(); //Necesario para poder utilizar ACTION_FOUND a partir de Android 6

        //Iniciamos la búsqueda de dispositivos
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
    }

    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    //TODO re-request
                }
                break;
            }
        }
    }
}
