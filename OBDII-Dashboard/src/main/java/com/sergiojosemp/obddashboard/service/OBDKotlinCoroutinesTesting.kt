package com.sergiojosemp.obddashboard.service

import android.app.*
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

    fun connectToDevice(bluetoothAdapter: BluetoothAdapter, mac: String, progressBar: MutableLiveData<Boolean>?){
        val btDevice = bluetoothAdapter!!.getRemoteDevice(mac)
        try {
            btConnection = btDevice.javaClass.getMethod(
                "createRfcommSocket",
                *arrayOf<Class<*>?>(Int::class.javaPrimitiveType)
            )
                .invoke(btDevice, 1) as BluetoothSocket
            btConnection!!.connect()
            Log.d(TAG, "Connected")
            progressBar?.postValue(false)
            outputStream = btConnection!!.getOutputStream();
            inputStream = btConnection!!.getInputStream()
            if(this.btAdapter == null && this.mac == null) {
                this.btAdapter = bluetoothAdapter
                this.mac = mac
                sendAndReceivePrototype() //TODO remove this when dashboard or verbose mode works
            }
        } catch (e: Exception){
            //progressBar.postValue(false)
            //e.printStackTrace()
            btConnection?.close()
            btConnection = null
            Log.d(TAG, "Error connecting to device, try again")
        }
    }

    fun disconnectFromDevice(){
        try{
            Log.d(TAG,"Disconnecting from device")
            x.cancel()
            btConnection?.close()
            btConnection = null
        } catch (e: Exception){
            Log.d(TAG, "Error disconnecting from device. Is any device connected?")
        }
    }


    fun sendAndReceivePrototype(){



        val BUFFER_SIZE = 1024
        val buffer = ByteArray(4)
        var bytes = 0
        var error = false
        x = GlobalScope.launch { // launch a new coroutine in background and continue
            while(error || btConnection?.isConnected ?: false) {
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                try {
                    error = false
                    bytes = inputStream.read(buffer)
                    val a = buffer[0].toByte().toString(16)//.toByte()
                    val b = buffer[1].toByte().toString(16)//.toByte()
                    val c = buffer[2].toByte().toString(16)//.toByte()
                    val d = buffer[3].toByte().toString(16)//.toByte()
                    println("0x${a} 0x${b} 0x${c} 0x${d}")
                    //byteArray.postValue(buffer)
                    liveOutput.postValue(buffer)
                } catch (e: Exception) {
                    Log.d(TAG,"Device disconnected. Trying to reconnect.")
                    error = true
                    btConnection?.close()
                    btConnection = null
                    connectToDevice(btAdapter!!, mac!!, null)
                    //}
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
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, StartMenuActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder: Notification.Builder = Notification.Builder(this, notificationChannelId)

        return builder
            .setContentTitle("OBD Dashboard is running in background")
            //.setContentText("")
            //.setContentIntent(pendingIntent)
            //.setAutoCancel(true)
            .setSmallIcon(R.drawable.dtc_512)
            //.setTicker("Ticker text")
            //.setProgress(100, 20, false)
            .build()
    }
}


