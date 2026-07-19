package com.sergiojosemp.obddashboard.activity

import android.app.NotificationManager
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.adapter.ObdDataRecyclerViewAdapter
import com.sergiojosemp.obddashboard.databinding.NewVerboseActivityBinding
import com.sergiojosemp.obddashboard.model.ObdDataModel
import com.sergiojosemp.obddashboard.service.ObdService
import com.sergiojosemp.obddashboard.vm.VerboseViewModel


class VerboseActivityKT : AppCompatActivity(){

    private var sensorManager: SensorManager? = null
    private lateinit var binding: NewVerboseActivityBinding
    private lateinit var viewModel: VerboseViewModel
    public var obd: ObdService? = null //TODO private - migrated from OBDKotlinCoroutinesTesting
    private val orientListener: SensorEventListener?
    private val accelerometerListener: SensorEventListener?
    private var orientSensor: Sensor? = null // Se usa para recibir la orientación a través del sensor de orientación del dispositivo
    private var accelerometerSensor: Sensor? = null // Se usa para recibir la aceleración a través del sensor de aceleración del dispositivo
    private val serviceConn : OBDServiceConnectionOnVerboseMode = OBDServiceConnectionOnVerboseMode()

    init {
        orientListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                var dir = ""
                if (x >= 337.5 || x < 22.5) {
                    dir = "N"
                } else if (x >= 22.5 && x < 67.5) {
                    dir = "NE"
                } else if (x >= 67.5 && x < 112.5) {
                    dir = "E"
                } else if (x >= 112.5 && x < 157.5) {
                    dir = "SE"
                } else if (x >= 157.5 && x < 202.5) {
                    dir = "S"
                } else if (x >= 202.5 && x < 247.5) {
                    dir = "SW"
                } else if (x >= 247.5 && x < 292.5) {
                    dir = "W"
                } else if (x >= 292.5 && x < 337.5) {
                    dir = "NW"
                }
                viewModel.compassIndicator?.postValue(dir)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // do nothing
            }
        }

        accelerometerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mod = Math.sqrt(x * x + y * y + (z * z).toDouble()) / 9.81 //G
                viewModel.accelerationIndicator?.postValue("G-Force: ${mod.toString().substring(0,4)}")
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // do nothing
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this, R.layout.new_verbose_activity)

        viewModel = ViewModelProvider(this).get(VerboseViewModel::class.java)


        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        // Connects to the RecyclerView
        binding.obdDataList.apply {
            layoutManager = LinearLayoutManager(this@VerboseActivityKT)
            adapter = ObdDataRecyclerViewAdapter(this@VerboseActivityKT, mutableListOf<ObdDataModel>())
        }
    }

    override fun onResume(){
        super.onResume()
        try {
            sensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager
            // get Orientation sensor
            val sensors = sensorManager!!.getSensorList(Sensor.TYPE_ORIENTATION)
            val accSensors = sensorManager!!.getSensorList(Sensor.TYPE_ACCELEROMETER)
            if (sensors.size > 0) {
                orientSensor = sensors[0]
                accelerometerSensor = accSensors[0]
                sensorManager!!.registerListener(
                    orientListener, orientSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                sensorManager!!.registerListener(
                    accelerometerListener, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        val serviceIntent = Intent(this, ObdService::class.java);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    override fun onPause(){
        super.onPause()
        try {
            sensorManager?.unregisterListener(orientListener, orientSensor)
            sensorManager?.unregisterListener(accelerometerListener, accelerometerSensor)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if(serviceConn!=null)
            unbindService(serviceConn);
    }




    inner class OBDServiceConnectionOnVerboseMode : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            // obd.inputStream/outputStream removed - migrate in later phase
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            obd = (service as ObdService.ObdServiceBinder).getService()
            // LiveData observation from old service removed - migrate in later phase
        }
    }
}