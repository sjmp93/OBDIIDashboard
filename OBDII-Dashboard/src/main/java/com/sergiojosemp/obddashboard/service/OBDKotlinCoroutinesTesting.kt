package com.sergiojosemp.obddashboard.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


private val TAG = "OBD-Log"
class OBDKotlinCoroutinesTesting(val btAdapter: BluetoothAdapter?, val mac: String?, var byteArray: MutableLiveData<ByteArray>?, var progressBar: MutableLiveData<Boolean>?): Service() {
    //val btDevice = btAdapter!!.getRemoteDevice(mac)
    lateinit var outputStream: OutputStream
    lateinit var inputStream: InputStream
    lateinit var liveOutput: MutableLiveData<ByteArray>
    var num : Int = 0
    constructor(): this( null,  null,  null, null)
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
    fun printThing(){
        Log.d("OBD-Log","Thing ${num}")
    }

    @Throws(IOException::class)
    fun write(s: String) {
        outputStream.write(s.toByteArray())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return ObdServiceBinder()
    }

    inner class ObdServiceBinder : Binder() {
        val service: OBDKotlinCoroutinesTesting
            get() = this@OBDKotlinCoroutinesTesting
    }
}



// if (BluetoothAdapter.checkBluetoothAddress(it.mac)) {
