package com.sergiojosemp.obddashboard.activity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.DiscoveryActivityBinding
import java.util.*

class DiscoverActivity: AppCompatActivity() {
    private val REQUEST_COARSE_LOCATION = 5
    private val DEVICE_ITEM_TEXT_SIZE = 24
    private val MINIMUM_HEIGHT = 150


    private val discoveredDevices =
        ArrayList<String>() //Lista de dispositivos encontrados durante la búsqueda

    private var eventReceiver: BroadcastReceiver? = null //Objeto que recibirá eventos del sistema

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val devices =
        HashMap<String, BluetoothDevice>() //Diccionario que relaciona una mac con el objeto BluetoothDevice correspondiente

    private val TAG = "" // Para el log
    private lateinit var binding: DiscoveryActivityBinding



    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<DiscoveryActivityBinding>(this,
            R.layout.discovery_activity)
        supportActionBar!!.title = getText(R.string.discovered_devices_text)

        //Asignación de objetos de interfaz gráfica
        //Asignación de objetos de interfaz gráfica
        val deviceListTable: LinearLayout = findViewById<LinearLayout>(R.id.deviceListTable)
        deviceListTable.showDividers = LinearLayout.SHOW_DIVIDER_NONE

        //Tomar lista de dispositivos encontrados, para ello se crea un receiver para el intent ACTION_FOUND
        eventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Cada vez que se encuentre un dispositivo
                if (BluetoothDevice.ACTION_FOUND == intent.action) {
                    //Tomamos el objeto BluetoothDevice del INTENT, contendrá nombre y MAC
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    //Guardamos el dispositivo encontrado en una lista
                    if (!discoveredDevices.contains(
                            """
                                ${device.name}
                                ${device.address}
                                """.trimIndent()
                        )
                    ) {
                        discoveredDevices.add(
                            """
                                ${device.name}
                                ${device.address}
                                """.trimIndent()
                        )
                    }

                    //Creamos objetos de texto para mostrar el nombre y MAC de cada dispositivo
                    //encontrado en una vista de tabla, cada fila es un dispositivo
                    val nameText = TextView(context)
                    val macText = TextView(context)
                    val name =
                        if (device.name != null) device.name else getText(R.string.unknown_device_text).toString()
                    val mac = device.address
                    if (!devices.containsKey(mac)) {
                        devices[mac] = device
                        nameText.text = name
                        nameText.setTypeface(null, Typeface.BOLD)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) // A partir de Android 6, si no, será gris
                            nameText.setTextColor(getColor(R.color.device_list_item_text)) //Color azul
                        nameText.textSize = DEVICE_ITEM_TEXT_SIZE.toFloat()
                        macText.text = """
                            
                            $mac
                            """.trimIndent()
                        macText.textSize = DEVICE_ITEM_TEXT_SIZE.toFloat()


                        //Creamos una fila para añadir al layout de tabla
                        val tableRow = TableRow(context)

                        //Añadimos el nombre y MAC a la fila
                        tableRow.addView(nameText)
                        macText.visibility = View.INVISIBLE
                        tableRow.addView(macText)
                        tableRow.isClickable = true

                        //Creamos la acción que se tomará al pulsar sobre una fila
                        val onClickListenerRow =
                            View.OnClickListener { v ->
                                val connectActivity = Intent(
                                    getApplicationContext(),
                                    ConnectActivity::class.java
                                )
                                val tableRowView =
                                    v as TableRow
                                val auxNameText: TextView
                                val auxMacText: TextView
                                auxNameText = tableRowView.getVirtualChildAt(0) as TextView
                                auxMacText = tableRowView.getVirtualChildAt(1) as TextView
                                connectActivity.putExtra("name", auxNameText.text)
                                connectActivity.putExtra("mac", auxMacText.text)

                                //Se detiene la búsqueda de dispositivos
                                bluetoothAdapter?.cancelDiscovery()

                                //DiscoveryActivity.putExtra("bluetoothAdapter", bluetooth);
                                startActivity(connectActivity)
                            }
                        tableRow.setOnClickListener(onClickListenerRow)

                        //Se actualiza la interfaz con el nuevo dispositivo encontrado
                        tableRow.minimumHeight =
                            MINIMUM_HEIGHT // Se establece el tamaño mínimo de cada item
                        deviceListTable.addView(tableRow)
                    }
                    //debug
                    Log.d(TAG, discoveredDevices[discoveredDevices.size - 1])
                }
            }

            fun onDestroy() {
                unregisterReceiver(this)
            }
        }

        //Configuramos la aplicación para recibir el evento por parte del sistema

        //Configuramos la aplicación para recibir el evento por parte del sistema
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(eventReceiver, filter)
        //checkLocationPermission() //Necesario para poder utilizar ACTION_FOUND a partir de Android 6


        //Iniciamos la búsqueda de dispositivos

        //Iniciamos la búsqueda de dispositivos
        bluetoothAdapter?.startDiscovery()
    }

}