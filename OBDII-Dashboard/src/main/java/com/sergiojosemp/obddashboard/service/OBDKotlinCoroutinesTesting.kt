package com.sergiojosemp.obddashboard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.ObdResetCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.reader.ObdCommandJob
import com.github.pires.obd.reader.ObdConfig
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.activity.SettingsActivity
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.model.ObdDataModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


private val TAG = "OBD-Log"
class OBDKotlinCoroutinesTesting(): Service() {
    private val PREFERENCES = "preferences"
    lateinit var outputStream: OutputStream
    lateinit var inputStream: InputStream
    var liveOutput: MutableLiveData<ByteArray>
    var commandResult: MutableLiveData<String> //TODO remove when dashboard working
    var btConnectionStatus: MutableLiveData<Boolean>
    var num : Int = 0
    var btAdapter: BluetoothAdapter? = null
    var mac: String? = null
    var byteArray: MutableLiveData<ByteArray>? = null
    var progressBar: MutableLiveData<Boolean>? = null
    var btConnection: BluetoothSocket? = null
    val obdCommandReceived: MutableLiveData<ObdDataModel> = MutableLiveData()
    val notificationsChannelId = "OBDashboard Notifications"

    var preferences: SharedPreferences? = null
    lateinit var obdCommunicationLoop: Job

    init{
        liveOutput = MutableLiveData()
        commandResult = MutableLiveData()
        btConnectionStatus = MutableLiveData()

        Log.d("OBD-Log", "Service started")
    }

    fun connectToDevice(bluetoothAdapter: BluetoothAdapter, mac: String, progressBar: MutableLiveData<Boolean>?, device: MutableLiveData<BluetoothDeviceModel>?){
        val btDevice = bluetoothAdapter!!.getRemoteDevice(mac)
        try {
            btConnection = btDevice.javaClass.getMethod(
                "createRfcommSocket",
                *arrayOf<Class<*>?>(Int::class.javaPrimitiveType)
            )
                .invoke(btDevice, 1) as BluetoothSocket
            btConnection!!.connect()
            Log.d(TAG, "Connected")
            btConnectionStatus.postValue(true)
            progressBar?.postValue(false)
            outputStream = btConnection!!.getOutputStream();
            inputStream = btConnection!!.getInputStream()
            if(this.btAdapter == null && this.mac == null) {
                this.btAdapter = bluetoothAdapter
                this.mac = mac
                sendAndReceivePrototype() //TODO remove this when dashboard or verbose mode works
            }
        } catch (e: Exception){
            device?.postValue(null)
            progressBar?.postValue(false)
            //e.printStackTrace()
            notifyError(500,getString(R.string.notification_error_connecting_device))
            disconnectRoutine()
            Log.d(TAG, "Error connecting to device, try again")
        }
    }

    fun disconnectFromDevice(){
        try{
            Log.d(TAG, "Disconnecting from device")
            obdCommunicationLoop.cancel()
            disconnectRoutine()
        } catch (e: Exception){
            Log.d(TAG, "Error disconnecting from device. Is any device connected?")
        }
    }

    fun disconnectRoutine(){
        btConnection?.close()
        btConnection = null
        btConnectionStatus.postValue(false)
    }

    fun sendAndReceivePrototype(){
        var error = false
        if(!preferences!!.getBoolean("simulator_mode_preference", false)) {
            notifyProgress(200,getString(R.string.initialization_process))
            // AT Z command
            ObdCommandJob(ObdResetCommand()).getCommand().run(inputStream, outputStream)
            ObdCommandJob(EchoOffCommand()).getCommand().run(inputStream, outputStream)
            ObdCommandJob(LineFeedOffCommand()).getCommand().run(inputStream, outputStream)
            ObdCommandJob(TimeoutCommand(620)).getCommand().run(inputStream, outputStream)
            cancelNotification(200)
        }
        // Getting protocol from preferences
        //final String protocol = preferences.getString(SettingsActivity.PROTOCOLS_LIST_KEY, "AUTO");
        //ObdCommandJob(SelectProtocolCommand(ObdProtocols.valueOf(protocol))).getCommand().run(inputStream, outputStream)

        obdCommunicationLoop = GlobalScope.launch { // launch a new coroutine in background and continue
            while(error || (btConnection?.isConnected ?: false)) {
                try {
                    var periodBetweenRequests = 0
                    for (command in ObdConfig.getCommands()) {
                        val job = ObdCommandJob(command)
                        if (preferences!!.getBoolean(command.name,false)){
                            try {
                                job.getCommand().run(inputStream, outputStream)
                                if (job.command.calculatedResult != "-40.0") {
                                    commandResult.postValue(job.command.calculatedResult)
                                    obdCommandReceived?.postValue(
                                        ObdDataModel(
                                            job.command.name,
                                            job.command.calculatedResult
                                        )
                                    )
                                }
                                Log.d(TAG, "OBD COMMAND sent and response received with value ${job.command.calculatedResult}")
                                periodBetweenRequests = SettingsActivity.getObdUpdatePeriod(preferences)
                                delay(periodBetweenRequests.toLong()) // non-blocking delay for X ms, where X is defined in SettingsActivity (default time unit is ms)
                            } catch (iob: IndexOutOfBoundsException) {
                                Log.d(TAG, "Index out of bounds exception")
                            }
                        }
                    }
                    error = false
                }catch (e: Exception) {
                    Log.d(TAG, "Device disconnected. Trying to reconnect.")
                    error = true
                    btConnection?.close()
                    btConnection = null
                    delay(233L)
                    connectToDevice(btAdapter!!, mac!!, null, null)
                }
            }
        }
    }


    fun printThing(){
        Log.d(TAG, "Thing ${num}")
    }

    @Throws(IOException::class)
    fun write(s: String) {
        outputStream.write(s.toByteArray())
    }

    override fun onBind(intent: Intent?): IBinder? {
        var notification = createNotification()
        createNotificationChannel() // Create notifications channel for general purpose notifications
        preferences = getSharedPreferences(PREFERENCES, 0) // here we initialize sharedPreferences for obd service using our context (not available until this moment)
        startForeground(1, notification)
        return ObdServiceBinder()
    }

    inner class ObdServiceBinder : Binder() {
        val service: OBDKotlinCoroutinesTesting
            get() = this@OBDKotlinCoroutinesTesting
    }


    //*******************************************************************************************************************//
    //******************************************** Notifications stuff **************************************************//
    //*******************************************************************************************************************//

    private fun createNotification(): Notification {
        val notificationChannelId = "OBD SERVICE CHANNEL"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationChannelId,
            "OBD Service Channel",
            NotificationManager.IMPORTANCE_LOW
        ).let {
            it.description = "OBD Service Channel"
            it.enableLights(true)
            it.lightColor = Color.BLUE
            it
        }
        notificationManager.createNotificationChannel(channel)

        val builder: Notification.Builder = Notification.Builder(this, notificationChannelId)

        return builder
            .setContentTitle("OBD Dashboard is running in background")
            .setSmallIcon(R.drawable.dtc_512)
            .build()
    }

    private fun createNotificationChannel() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationsChannelId,
            notificationsChannelId,
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "OBDashboard Notifications Channel"
            it.enableLights(true)
            it.lightColor = Color.BLUE
            it
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun notifyError(notificationId: Int, notificationText: String){
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val notificationBuilder = NotificationCompat.Builder(
                    applicationContext,
                    notificationsChannelId
                )
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                val notification = notificationBuilder
                    .setSmallIcon(R.drawable.dtc_512)
                    .setContentText(notificationText)
                    .setTimeoutAfter(4000)
                    .build()
                val notificationManager = NotificationManagerCompat.from(applicationContext)
                notificationManager.notify(notificationId, notification)
         }
    }

    fun notifyProgress(notificationId: Int, notificationText: String){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationBuilder = NotificationCompat.Builder(
                applicationContext,
                notificationsChannelId
            )
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            val notification = notificationBuilder
                .setProgress(100,50,true)
                .setSmallIcon(R.drawable.dtc_512)
                .setContentText(notificationText)
                .build()
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(notificationId, notification)
        }
    }

    fun cancelNotification(notificationId: Int){
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId);
    }

}


