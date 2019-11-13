package com.sergiojosemp.obddashboard.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.sergiojosemp.obddashboard.R;

import java.util.ArrayList;
import java.util.HashMap;

public class DiscoveryActivity extends AppCompatActivity {


    private static final int REQUEST_COARSE_LOCATION = 5;
    private final static int DEVICE_ITEM_TEXT_SIZE = 24;
    private final static int MINIMUM_HEIGHT = 150;



    private final ArrayList<String> discoveredDevices = new ArrayList<String>(); //Lista de dispositivos encontrados durante la búsqueda
    private  BroadcastReceiver eventReceiver = null; //Objeto que recibirá eventos del sistema
    private BluetoothAdapter bluetoothAdapter; //Objeto adaptador Bluetooth
    private HashMap<String, BluetoothDevice> devices = new HashMap<String, BluetoothDevice>(); //Diccionario que relaciona una mac con el objeto BluetoothDevice correspondiente
    private String TAG = ""; // Para el log


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.discovery_activity);
        getSupportActionBar().setTitle(getText(R.string.discovered_devices_text));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Asignación de objetos de interfaz gráfica
        final LinearLayout deviceListTable = findViewById(R.id.deviceListTable);
        deviceListTable.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        //Tomar lista de dispositivos encontrados, para ello se crea un receiver para el intent ACTION_FOUND
        eventReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                // Cada vez que se encuentre un dispositivo
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    //Tomamos el objeto BluetoothDevice del INTENT, contendrá nombre y MAC
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    //Guardamos el dispositivo encontrado en una lista
                    if (!discoveredDevices.contains(device.getName() + "\n" + device.getAddress())) {
                        discoveredDevices.add(device.getName() + "\n" + device.getAddress());
                    }

                    //Creamos objetos de texto para mostrar el nombre y MAC de cada dispositivo
                    //encontrado en una vista de tabla, cada fila es un dispositivo
                    TextView nameText = new TextView(context);
                    TextView macText = new TextView(context);

                    final String name = (device.getName() != null) ? device.getName() : getText(R.string.unknown_device_text).toString();
                    final String mac = device.getAddress();
                    if (!devices.containsKey(mac)) {
                        devices.put(mac, device);

                        nameText.setText(name);
                        nameText.setTypeface(null, Typeface.BOLD);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) // A partir de Android 6, si no, será gris
                            nameText.setTextColor(getColor(R.color.device_list_item_text)); //Color azul
                        nameText.setTextSize(DEVICE_ITEM_TEXT_SIZE);
                        macText.setText("\n" + mac);
                        macText.setTextSize(DEVICE_ITEM_TEXT_SIZE);


                        //Creamos una fila para añadir al layout de tabla
                        TableRow tableRow = new TableRow(context);

                        //Añadimos el nombre y MAC a la fila
                        tableRow.addView(nameText);
                        macText.setVisibility(View.INVISIBLE);
                        tableRow.addView(macText);
                        tableRow.setClickable(true);

                        //Creamos la acción que se tomará al pulsar sobre una fila
                        OnClickListener onClickListenerRow = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent connectActivity = new Intent(getApplicationContext(), ConnectActivity.class);

                                TableRow tableRowView = (TableRow) v;

                                TextView auxNameText, auxMacText;
                                auxNameText = (TextView) tableRowView.getVirtualChildAt(0);
                                auxMacText = (TextView) tableRowView.getVirtualChildAt(1);

                                connectActivity.putExtra("name", auxNameText.getText());
                                connectActivity.putExtra("mac", auxMacText.getText());

                                //Se detiene la búsqueda de dispositivos
                                bluetoothAdapter.cancelDiscovery();

                                //DiscoveryActivity.putExtra("bluetoothAdapter", bluetooth);
                                startActivity(connectActivity);
                            }
                        };
                        tableRow.setOnClickListener(onClickListenerRow);

                        //Se actualiza la interfaz con el nuevo dispositivo encontrado
                        tableRow.setMinimumHeight(MINIMUM_HEIGHT); // Se establece el tamaño mínimo de cada item
                        deviceListTable.addView(tableRow);
                    }
                    //debug
                    Log.d(TAG, discoveredDevices.get(discoveredDevices.size() - 1));
                }
            }

            public void onDestroy() {
                unregisterReceiver(this);
            }
        };

        //Configuramos la aplicación para recibir el evento por parte del sistema
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(eventReceiver, filter);
        checkLocationPermission(); //Necesario para poder utilizar ACTION_FOUND a partir de Android 6

        //Iniciamos la búsqueda de dispositivos
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventReceiver != null)
            unregisterReceiver(eventReceiver);
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
