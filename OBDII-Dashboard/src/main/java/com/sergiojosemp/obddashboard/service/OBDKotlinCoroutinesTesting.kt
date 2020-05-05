package com.sergiojosemp.obddashboard.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


private val TAG = "OBD-Log"
class OBDKotlinCoroutinesTesting(val btAdapter: BluetoothAdapter, val mac: String, var byteArray: MutableLiveData<ByteArray>) {
    val btDevice = btAdapter.getRemoteDevice(mac)
    lateinit var outputStream: OutputStream
    lateinit var inputStream: InputStream

    init{
        try{
            val btConnection = btDevice.javaClass.getMethod("createRfcommSocket", *arrayOf<Class<*>?>(Int::class.javaPrimitiveType))
                .invoke(btDevice, 1) as BluetoothSocket
            btConnection.connect()
            Log.d(TAG, "Connected")

            outputStream = btConnection.getOutputStream();
            inputStream = btConnection.getInputStream();
        } catch(e: Exception) {
            println("Exception")
        }
        var number: Int = 0


        val BUFFER_SIZE = 1024
        val buffer = ByteArray(4)
        var bytes = 0
        val b = BUFFER_SIZE

        GlobalScope.launch { // launch a new coroutine in background and continue
            while(true) {
                //number++
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                //println("Iteration number ${number}") // print after delay

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

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    fun write(s: String) {
        outputStream.write(s.toByteArray())
    }
}



// if (BluetoothAdapter.checkBluetoothAddress(it.mac)) {
