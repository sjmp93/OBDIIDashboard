package com.sergiojosemp.obddashboard.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.MutableLiveData
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.activity.StartMenuActivity
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


private val TAG = "OBD-Log"
class OBDKotlinCoroutinesTesting(): Service() {
    lateinit var outputStream: OutputStream
    lateinit var inputStream: InputStream
    lateinit var liveOutput: MutableLiveData<ByteArray>
    var num : Int = 0
    var btAdapter: BluetoothAdapter? = null
    var mac: String? = null
    var byteArray: MutableLiveData<ByteArray>? = null
    var progressBar: MutableLiveData<Boolean>? = null
    var btConnection: BluetoothSocket? = null
    lateinit var x: Job
    init{
        liveOutput = MutableLiveData()
        Log.d("OBD-Log", "Service started")

        /*
        try{
            liveOutput = MutableLiveData()
            val btConnection = btDevice.javaClass.getMethod("createRfcommSocket", *arrayOf<Class<*>?>(Int::class.javaPrimitiveType))
                .invoke(btDevice, 1) as BluetoothSocket
            btConnection.connect()
            Log.d(TAG, "Connected")
            progressBar.postValue(false)
            outputStream = btConnection.getOutputStream();
            inputStream = btConnection.getInputStream();

            var number: Int = 0


            val BUFFER_SIZE = 1024
            val buffer = ByteArray(4)
            var bytes = 0
            val b = BUFFER_SIZE

            GlobalScope.launch { // launch a new coroutine in background and continue
                while(true) {
                    delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                    try {
                        //outputStream.write(0)
                        //println("Data written")
                        /*
                        bytes = inputStream.read(buffer)
                        val readMessage = String(buffer, 0, bytes)
                        outputStream.write(0x01) //Engine coolant temperature
                        outputStream.write(0x05)  //Engine coolant temperature
                        println(readMessage)
                        println(bytes)*/
                        bytes = inputStream.read(buffer)
                        val a = buffer[0].toByte().toString(16)//.toByte()
                        val b = buffer[1].toByte().toString(16)//.toByte()
                        val c = buffer[2].toByte().toString(16)//.toByte()
                        val d = buffer[3].toByte().toString(16)//.toByte()
                        println("0x${a} 0x${b} 0x${c} 0x${d}")
                        byteArray.postValue(buffer)
                        liveOutput.postValue(buffer)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch(e: Exception) {
            println("Exception")
        }
*/
    }

    fun connectToDevice(bluetoothAdapter: BluetoothAdapter, mac: String, progressBar: MutableLiveData<Boolean>){
        val btDevice = bluetoothAdapter!!.getRemoteDevice(mac)
        try {
            btConnection = btDevice.javaClass.getMethod(
                "createRfcommSocket",
                *arrayOf<Class<*>?>(Int::class.javaPrimitiveType)
            )
                .invoke(btDevice, 1) as BluetoothSocket
            btConnection!!.connect()
            Log.d(TAG, "Connected")
            progressBar.postValue(false)
            sendAndReceivePrototype()
        } catch (e: Exception){
            //progressBar.postValue(false)
            Log.d(TAG, "Error connecting to device, try again")
        }
    }

    fun disconnectFromDevice(){
        try{
            x.cancel()
            btConnection?.close()
            btConnection = null
        } catch (e: Exception){
            Log.d(TAG, "Error disconnecting from device. Is any device connected?")
        }
    }


    fun sendAndReceivePrototype(){

        outputStream = btConnection!!.getOutputStream();
        inputStream = btConnection!!.getInputStream()

        val BUFFER_SIZE = 1024
        val buffer = ByteArray(4)
        var bytes = 0

        x = GlobalScope.launch { // launch a new coroutine in background and continue
            while(btConnection?.isConnected ?: false) {
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                try {
                    bytes = inputStream.read(buffer)
                    val a = buffer[0].toByte().toString(16)//.toByte()
                    val b = buffer[1].toByte().toString(16)//.toByte()
                    val c = buffer[2].toByte().toString(16)//.toByte()
                    val d = buffer[3].toByte().toString(16)//.toByte()
                    println("0x${a} 0x${b} 0x${c} 0x${d}")
                    //byteArray.postValue(buffer)
                    liveOutput.postValue(buffer)
                } catch (e: Exception) {
                    Log.d(TAG,"Possible error on I/O streaming or device disconnected")
                }
            }
        }
    }


    fun printThing(){
        Log.d(TAG,"Thing ${num}")
    }

    @Throws(IOException::class)
    fun write(s: String) {
        outputStream.write(s.toByteArray())
    }

    override fun onBind(intent: Intent?): IBinder? {
        var notification = createNotification()
        startForeground(1, notification)
        return ObdServiceBinder()
    }

    inner class ObdServiceBinder : Binder() {
        val service: OBDKotlinCoroutinesTesting
            get() = this@OBDKotlinCoroutinesTesting
    }



    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        /*val pendingIntent: PendingIntent = Intent(this, StartMenuActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }*/

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("Endless Service")
            .setContentText("This is your favorite endless service working")
            //.setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}


