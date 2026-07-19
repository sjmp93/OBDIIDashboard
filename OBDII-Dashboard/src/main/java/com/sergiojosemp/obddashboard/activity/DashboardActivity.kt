package com.sergiojosemp.obddashboard.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.reader.LogCSVWriter
import com.github.pires.obd.reader.ObdCommandJob
import com.github.pires.obd.reader.ObdConfig
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.DashboardActivityBinding
import com.sergiojosemp.obddashboard.model.PreferencesHelper
import com.sergiojosemp.obddashboard.service.ObdService
import com.sergiojosemp.obddashboard.vm.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class DashboardActivity : AppCompatActivity(), LocationListener, GpsStatus.Listener {

    private val NO_GPS_SUPPORT = 9
    private val NO_ORIENTATION_SENSOR = 8

    private lateinit var binding: DashboardActivityBinding
    private lateinit var viewModel: DashboardViewModel

    private var obdService: ObdService? = null
    private var csvWriter: LogCSVWriter? = null
    private var lastLocation: Location? = null
    private var gpsIsStarted = false

    private var locationManager: LocationManager? = null
    private var locationProvider: android.location.LocationProvider? = null

    private var orientSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var sensorManager: SensorManager? = null

    // Progress bar maps for configurable parameters
    private val progressMap = mutableMapOf<Int, IconRoundCornerProgressBar>()
    private val inverseProgressMap = mutableMapOf<IconRoundCornerProgressBar, Int>()
    private val textMap = mutableMapOf<Int, android.widget.TextView?>()
    private val inverseTextMap = mutableMapOf<android.widget.TextView?, Int>()

    // Sensor listeners
    private val accelerometerListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val mod = kotlin.math.sqrt(x * x + y * y + z * z) / 9.81
            viewModel.setAcceleration(mod.toString().substring(0, 3))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // do nothing
        }
    }

    private val orientListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val dir = when {
                x >= 337.5 || x < 22.5 -> "N"
                x in 22.5..67.49 -> "NE"
                x in 67.5..112.49 -> "E"
                x in 112.5..157.49 -> "SE"
                x in 157.5..202.49 -> "S"
                x in 202.5..247.49 -> "SW"
                x in 247.5..292.49 -> "W"
                else -> "NW"
            }
            viewModel.setCompass(dir)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // do nothing
        }
    }

    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: android.os.IBinder) {
            obdService = (binder as ObdService.ObdServiceBinder).getService()
            obdService?.setContext(this@DashboardActivity)
            obdService?.setVerboseMode(false)
            Log.d(TAG, getString(R.string.dashboard_linking_log_text))
            obdService?.startService()

            // Start the periodic command polling loop after service is connected
            startCommandPollingLoop()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // Connection lost unexpectedly
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun startFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun stopFullScreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            startFullScreen()
        }
    }

    private fun setupProgressBars() {
        // Map progress bars to indices 0-4
        progressMap[0] = binding.progress1
        progressMap[1] = binding.progress2
        progressMap[2] = binding.progress3
        progressMap[3] = binding.progress4
        progressMap[4] = binding.progress5

        textMap[0] = binding.progressText1
        textMap[1] = binding.progressText2
        textMap[2] = binding.progressText3
        textMap[3] = binding.progressText4
        textMap[4] = binding.progressText5

        inverseProgressMap[binding.progress1] = 0
        inverseProgressMap[binding.progress2] = 1
        inverseProgressMap[binding.progress3] = 2
        inverseProgressMap[binding.progress4] = 3
        inverseProgressMap[binding.progress5] = 4

        inverseTextMap[binding.progressText1] = 0
        inverseTextMap[binding.progressText2] = 1
        inverseTextMap[binding.progressText3] = 2
        inverseTextMap[binding.progressText4] = 3
        inverseTextMap[binding.progressText5] = 4

        // Configure default progress bars
        binding.progress1.max = 120f
        binding.progress1.setProgressColor(ContextCompat.getColor(this, R.color.engine_coolant_progress))
        binding.progress1.setIconBackgroundColor(ContextCompat.getColor(this, R.color.engine_coolant_iconbg))
        binding.progress1.setIconImageResource(R.drawable.coolant_512)
        binding.progressText1.text = "0C"

        binding.progress2.max = 130f
        binding.progress2.setProgressColor(ContextCompat.getColor(this, R.color.engine_oil_progress))
        binding.progress2.setIconBackgroundColor(ContextCompat.getColor(this, R.color.engine_oil_iconbg))
        binding.progress2.setIconImageResource(R.drawable.oil_512)
        binding.progressText2.text = "0C"

        binding.progress3.max = 100f
        binding.progress3.setProgressColor(ContextCompat.getColor(this, R.color.engine_load_progress))
        binding.progress3.setIconBackgroundColor(ContextCompat.getColor(this, R.color.engine_load_iconbg))
        binding.progress3.setIconImageResource(R.drawable.engine_load_512)
        binding.progressText3.text = "0%"

        binding.progress4.max = 100f
        binding.progress4.setProgressColor(ContextCompat.getColor(this, R.color.throttle_position_progress))
        binding.progress4.setIconBackgroundColor(ContextCompat.getColor(this, R.color.throttle_position_iconbg))
        binding.progress4.setIconImageResource(R.drawable.throttle_512)
        binding.progressText4.text = "0%"

        binding.progress5.max = 120f
        binding.progress5.setProgressColor(ContextCompat.getColor(this, R.color.ait_intake_progress))
        binding.progress5.setIconBackgroundColor(ContextCompat.getColor(this, R.color.air_intake_iconbg))
        binding.progress5.setIconImageResource(R.drawable.air_intake_512)
        binding.progressText5.text = "0C"

        //binding.rpm.setIconBackgroundColor(ContextCompat.getColor(this, R.color.fuel_consumption_rate_iconbg))
    }

    private fun setupLongClickListeners() {
        val progressListener: View.OnLongClickListener = View.OnLongClickListener { v ->
            val tempBar = v as IconRoundCornerProgressBar
            val index = inverseProgressMap[tempBar] ?: return@OnLongClickListener false
            val tempText = textMap[index]

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.obd_parameters)
                .setItems(R.array.obddata) { dialog: DialogInterface, which: Int ->
                    layoutUpdate(tempBar, tempText, which)
                }
            builder.create().show()
            false
        }

        binding.progress1.setOnLongClickListener(progressListener)
        binding.progress2.setOnLongClickListener(progressListener)
        binding.progress3.setOnLongClickListener(progressListener)
        binding.progress4.setOnLongClickListener(progressListener)
        binding.progress5.setOnLongClickListener(progressListener)

        // MIL indicator long click
        binding.mil.setOnLongClickListener {
            Log.d(TAG, getString(R.string.getting_trouble_codes_text))
            false
        }
    }

    private fun layoutUpdate(
        tempBar: IconRoundCornerProgressBar,
        tempText: android.widget.TextView?,
        which: Int
    ) {
        val currentInverseIndex = inverseProgressMap[tempBar] ?: return
        if (textMap.containsKey(which)) {
            Toast.makeText(this, R.string.already, Toast.LENGTH_LONG).show()
            return
        }

        val config = when (which) {
            0 -> Triple(R.color.engine_coolant_progress, R.drawable.coolant_512, 120f)
            1 -> Triple(R.color.engine_oil_progress, R.drawable.oil_512, 130f)
            2 -> Triple(R.color.engine_load_progress, R.drawable.engine_load_512, 100f)
            3 -> Triple(R.color.throttle_position_progress, R.drawable.throttle_512, 100f)
            4 -> Triple(R.color.ait_intake_progress, R.drawable.air_intake_512, 100f)
            5 -> Triple(R.color.ambient_air_progress, R.drawable.ambient_air_512, 70f)
            6 -> Triple(R.color.intake_manifold_presure_progress, R.drawable.intake_manifold_512, 100000f)
            7 -> Triple(R.color.fuel_rail_pressure_progress, R.drawable.fuel_rail_512, 100000f)
            8 -> Triple(R.color.fuel_pressure_progress, R.drawable.fuel_pressure_512, 100000f)
            9 -> Triple(R.color.barometric_pressure_progress, R.drawable.barometric_512, 5000f)
            10 -> Triple(R.color.fuel_level_progress, R.drawable.fuel_level_512, 100f)
            11 -> Triple(R.color.fuel_consumption_rate_progress, R.drawable.fuel_consumption_512, 100f)
            12 -> Triple(R.color.mass_air_flow_progress, R.drawable.mass_air_flow_512, 150f)
            else -> return
        }

        tempBar.max = config.third
        tempBar.setProgressColor(ContextCompat.getColor(this, config.first))
        tempBar.setIconBackgroundColor(ContextCompat.getColor(this, config.first))
        tempBar.setIconImageResource(config.second)
        tempText?.text = ""

        // Update maps
        val oldTextIndex = inverseTextMap[tempText] ?: currentInverseIndex
        textMap.remove(oldTextIndex)
        textMap[which] = tempText
        inverseTextMap.remove(tempText)
        inverseTextMap[tempText] = which

        progressMap.remove(currentInverseIndex)
        progressMap[which] = tempBar
        inverseProgressMap.remove(tempBar)
        inverseProgressMap[tempBar] = which
    }

    private fun setProgressForCommand(job: ObdCommandJob) {
        val obdData = resources.getStringArray(R.array.obddata).toList()
        val cmdName = job.command.getName() ?: ""
        val progressIndex = obdData.indexOf(cmdName)

        if (progressIndex in 0..4) {
            val tempProgress = progressMap[progressIndex]
            val tempText = textMap[progressIndex]
            if (tempProgress != null) {
                try {
                    val value = job.command.getCalculatedResult()?.toFloat() ?: 0f
                    runOnUiThread {
                        tempProgress.progress = value
                        tempText?.text = job.command.getFormattedResult()
                    }
                } catch (_: NumberFormatException) {
                    // Skip invalid values
                }
            }
        }
    }

    private fun queueCommands() {
        for (command in ObdConfig.getCommands()) {
            if (PreferencesHelper.getCommandEnabledSync(this, command.getName())) {
                obdService?.queueJob(ObdCommandJob(command))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun gpsInit(): Boolean {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager != null) {
            locationProvider = locationManager?.getProvider(LocationManager.GPS_PROVIDER)
            if (locationProvider != null) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return true
                }
                locationManager?.addGpsStatusListener(this)
                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    return true
                }
            }
        }
        Log.e(TAG, "Unable to get GPS PROVIDER")
        return false
    }

    @SuppressLint("MissingPermission")
    private fun gpsStart() {
        if (!gpsIsStarted && locationProvider != null && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager?.requestLocationUpdates(
                locationProvider!!.name,
                PreferencesHelper.getGpsUpdatePeriodSync(this).toLong(),
                PreferencesHelper.getGpsDistanceUpdatePeriodSync(this),
                this
            )
            gpsIsStarted = true
        }
    }

    private fun gpsStop() {
        if (gpsIsStarted) {
            locationManager?.removeUpdates(this)
            gpsIsStarted = false
        }
    }

    override fun onGpsStatusChanged(event: Int) {
        when (event) {
            GpsStatus.GPS_EVENT_STARTED -> Log.d(TAG, getString(R.string.status_gps_started))
            GpsStatus.GPS_EVENT_STOPPED -> Log.d(TAG, getString(R.string.status_gps_stopped))
            GpsStatus.GPS_EVENT_FIRST_FIX -> Log.d(TAG, getString(R.string.status_gps_fix))
        }
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun initCsvWriter() {
        if (!PreferencesHelper.getBooleanSync(this, PreferencesHelper.ENABLE_FULL_LOGGING_KEY.toString(), false)) return

        val mils = System.currentTimeMillis()
        val sdf = SimpleDateFormat("_dd_MM_yyyy_HH_mm_ss")

        try {
            csvWriter = LogCSVWriter(
                "Log${sdf.format(Date(mils))}.csv",
                PreferencesHelper.getStringSync(this, PreferencesHelper.DIRECTORY_FULL_LOGGING_KEY.toString(), getString(R.string.default_dirname_full_logging)),
                this
            )
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.can_not_enable_logging_error), e)
        }
    }

    private fun startCommandPollingLoop() {
        lifecycleScope.launch {
            while (isActive) {
                val service = obdService
                if (service != null &&
                    service.getbluetoothSocket()?.isConnected == true &&
                    service.queueEmpty()) {

                    queueCommands()

                    // GPS data for CSV logging
                    var lat = 0.0
                    var lon = 0.0
                    var alt = 0.0

                    if (gpsIsStarted && lastLocation != null) {
                        lat = lastLocation!!.latitude
                        lon = lastLocation!!.longitude
                        alt = lastLocation!!.altitude

                        val posLen = 7
                        val sb = StringBuilder()
                        sb.append("Lat: ")
                        sb.append(lastLocation!!.latitude.toString().substring(0, minOf(posLen, lastLocation!!.latitude.toString().length)))
                        sb.append(" Lon: ")
                        sb.append(lastLocation!!.longitude.toString().substring(0, minOf(posLen, lastLocation!!.longitude.toString().length)))
                        sb.append(" Alt: ")
                        sb.append(lastLocation!!.altitude)

                        viewModel.setGpsText(sb.toString())
                    }

                    val uploadData = PreferencesHelper.getBooleanSync(this@DashboardActivity, PreferencesHelper.UPLOAD_DATA_KEY.toString(), false)
                    if (uploadData) {
                        // Upload via HTTP - currently disabled in original code
                    } else if (PreferencesHelper.getBooleanSync(this@DashboardActivity, PreferencesHelper.ENABLE_FULL_LOGGING_KEY.toString(), false)) {
                        val vin = PreferencesHelper.getStringSync(this@DashboardActivity, PreferencesHelper.VEHICLE_ID_KEY.toString(), "UNDEFINED_VIN")
                        viewModel.writeCsvReading(csvWriter, lat, lon, alt, vin)
                    }

                    // Clear command results after processing
                }

                delay(PreferencesHelper.getObdUpdatePeriodSync(this@DashboardActivity).toLong())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.dashboard_activity)
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        setupProgressBars()
        setupLongClickListeners()
        startFullScreen()

        // Bind to ObdService
        val serviceIntent = Intent(this, ObdService::class.java)
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)

        // Initialize sensors
        try {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            val sensors = sensorManager?.getSensorList(Sensor.TYPE_ORIENTATION) ?: emptyList()
            val accSensors = sensorManager?.getSensorList(Sensor.TYPE_ACCELEROMETER) ?: emptyList()

            if (sensors.isNotEmpty()) {
                orientSensor = sensors[0]
                accelerometerSensor = accSensors.getOrNull(0)
            } else {
                // showDialog(NO_ORIENTATION_SENSOR)
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.stackTrace.toString(), Toast.LENGTH_LONG).show()
        }

        initCsvWriter()
    }

    override fun onResume() {
        super.onResume()

        val serviceIntent = Intent(this, ObdService::class.java)
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)

        if (PreferencesHelper.getBooleanSync(this, PreferencesHelper.ENABLE_GPS_KEY.toString(), false)) {
            gpsInit()
            gpsStart()
        }

        try {
            sensorManager?.registerListener(orientListener, orientSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager?.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.error_text) + " " + e.stackTraceToString())
        }
    }

    override fun onPause() {
        super.onPause()

        csvWriter?.closeLogCSVWriter()
        //obdService?.setContext(null)

        try {
            unbindService(serviceConn)
        } catch (_: IllegalArgumentException) {
            // Service not bound
        }

        if (PreferencesHelper.getBooleanSync(this, PreferencesHelper.ENABLE_GPS_KEY.toString(), false)) {
            gpsStop()
        }

        sensorManager?.unregisterListener(orientListener)
        sensorManager?.unregisterListener(accelerometerListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        csvWriter = null
        obdService = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 /* REQUEST_ENABLE_BT */) {
            if (resultCode == Activity.RESULT_OK) {
                // BT enabled successfully
            } else {
                Toast.makeText(this, R.string.text_bluetooth_disabled, Toast.LENGTH_LONG).show()
                super.onBackPressed()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val TAG = "DashboardActivity"
    }
}
