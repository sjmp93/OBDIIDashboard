package com.sergiojosemp.obddashboard.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Binder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.ObdResetCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand
import com.github.pires.obd.commands.control.TroubleCodesCommand
import com.github.pires.obd.enums.ObdProtocols
import com.github.pires.obd.exceptions.MisunderstoodCommandException
import com.github.pires.obd.exceptions.StoppedException
import com.github.pires.obd.exceptions.UnsupportedCommandException
import com.github.pires.obd.reader.ObdCommandJob
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.activity.DiagnosticTroubleCodeActivity
import com.sergiojosemp.obddashboard.model.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

class ObdService : LifecycleService() {

    private val TAG = ObdService::class.java.name

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = ObdServiceBinder()
    private var commandChannel: Channel<ObdCommandJob> = Channel()

    private var jobsQueueCounter = 0L
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var verboseMode = false
    private var obdDevice = false
    private var dtc = false
    private var troubleCodes = ""

    @Volatile
    private var contextRef: Context? = null

    // ModifiedTroubleCodesObdCommand nested class for DTC retrieval
    inner class ModifiedTroubleCodesObdCommand : TroubleCodesCommand() {
        override fun getResult(): String {
            return rawData.replace("SEARCHING...", "").replace("NODATA", "")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, getText(R.string.creating_service).toString())
        serviceScope.launch {
            executeQueue()
        }
        Log.d(TAG, getText(R.string.service_created).toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        commandChannel.close()
        serviceScope.cancel()
        Log.d(TAG, getText(R.string.service_done).toString())
    }

    /**
     * Queues an OBD command job for execution. Assigns a unique ID and adds it to the channel.
     */
    fun queueJob(job: ObdCommandJob) {
        synchronized(this) {
            jobsQueueCounter++
        }.also { id ->
            Log.d(
                TAG,
                getText(R.string.adding_job).toString() + id + getText(R.string.to_queue).toString()
            )
            job.id = id
            try {
                runBlocking { commandChannel.send(job) }
                Log.d(
                    TAG,
                    getText(R.string.job).toString() + job.command.getName() + getText(R.string.queued_succesfully).toString()
                )
            } catch (e: InterruptedException) {
                job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR)
                Log.e(TAG, getText(R.string.failed_queueing_job).toString() + job.command.getName())
            }
        }
    }

    /**
     * Core command execution loop. Reads jobs from the channel and executes them on the BT socket.
     */
    private suspend fun executeQueue() {
        Log.d(TAG, getText(R.string.obd_service_working).toString())

        for (job in commandChannel) {
            try {
                Log.d(
                    TAG,
                    getText(R.string.taking_job).toString() + job.id + getText(R.string.from_queue).toString()
                )

                if (job.state == ObdCommandJob.ObdCommandJobState.NEW &&
                    bluetoothSocket?.isConnected == true) {
                    Log.d(TAG, getText(R.string.job_is_new).toString())
                    job.setState(ObdCommandJob.ObdCommandJobState.RUNNING)
                    job.command.run(bluetoothSocket!!.getInputStream(), bluetoothSocket!!.getOutputStream())
                    if (job != null) {
                        Log.d(
                            TAG,
                            getText(R.string.updating_dash).toString() +
                                    job.command.getName() +
                                    getText(R.string.updating_dash).toString() +
                                    job.command.getFormattedResult() + "..."
                        )
                    }
                } else {
                    if (bluetoothSocket?.isConnected != true) {
                        job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)
                        Log.e(TAG, getText(R.string.cant_run_closed_socket).toString())
                    } else {
                        Log.e(TAG, getText(R.string.bug_alert).toString())
                    }
                }
            } catch (e: UnsupportedCommandException) {
                job?.setState(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)
                Log.d(TAG, getText(R.string.command_not_supported).toString() + " -> " + e.message)
            } catch (e: IOException) {
                if (job != null) {
                    if (e.message?.contains("Broken pipe") == true) {
                        job.setState(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE)
                    } else {
                        job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)
                    }
                }
                Log.e(TAG, getText(R.string.io_error).toString() + " -> " + e.message)
            } catch (e: Exception) {
                job?.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)
                Log.e(TAG, getText(R.string.failed_to_run_command).toString() + " -> " + e.message)
            }
        }
    }

    /**
     * Establishes a Bluetooth RFCOMM connection to the previously set device on channel 1.
     */
    @Synchronized
    fun connectToDevice() {
        var tmp: BluetoothSocket? = null
        try {
            tmp = bluetoothDevice?.javaClass?.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                ?.invoke(bluetoothDevice, 1) as? BluetoothSocket
        } catch (e: Exception) {
            e.printStackTrace()
        }

        bluetoothSocket = tmp

        try {
            bluetoothSocket?.connect()
            Log.d(TAG, getText(R.string.connected_to_device).toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(contextRef, getText(R.string.connection_error_is_obd_adapter), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sends a dummy EchoOffCommand to verify the connected device is an OBD adapter.
     */
    @Throws(IOException::class)
    fun isObdDevice() {
        Log.d(TAG, getText(R.string.checking_if_obdr).toString())
        if (bluetoothSocket?.isConnected == true) {
            serviceScope.launch {
                try {
                    Log.d(TAG, getText(R.string.dummy_command).toString())
                    val job = ObdCommandJob(EchoOffCommand())
                    runBlocking {
                        job.command.run(
                            bluetoothSocket!!.getInputStream(),
                            bluetoothSocket!!.getOutputStream()
                        )
                    }
                    setObdDevice(true)
                    putObdDeviceQueue(true)
                    Log.d(TAG, getText(R.string.is_obd).toString())
                } catch (e: MisunderstoodCommandException) {
                    e.printStackTrace()
                } catch (e: StoppedException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Log.d(TAG, getText(R.string.status_bluetooth_error_connecting).toString())
            obdDevice = false
            putObdDeviceQueue(false)
        }
    }

    /**
     * Queues initialization commands (reset, echo off, line feed off, timeout, protocol select).
     */
    @Throws(IOException::class)
    private fun startObdConnection() {
        Log.d(TAG, getText(R.string.queueing_for_config).toString())
        emptyQueue()
        queueJob(ObdCommandJob(ObdResetCommand()))
        queueJob(ObdCommandJob(ObdResetCommand()))
        queueJob(ObdCommandJob(ObdResetCommand()))

        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        queueJob(ObdCommandJob(EchoOffCommand()))
        queueJob(ObdCommandJob(LineFeedOffCommand()))
        queueJob(ObdCommandJob(com.github.pires.obd.commands.protocol.TimeoutCommand(120)))

        val protocol = PreferencesHelper.getStringSync(this, PreferencesHelper.PROTOCOLS_LIST_KEY.toString(), "AUTO")
        queueJob(ObdCommandJob(SelectProtocolCommand(ObdProtocols.valueOf(protocol))))

        queueJob(ObdCommandJob(AmbientAirTemperatureCommand()))
        jobsQueueCounter = 0L
        Log.d(TAG, getText(R.string.initialization_jobs).toString())
    }

    /**
     * Called by Activities after binding. Triggers DTC retrieval if the bound context is DiagnosticTroubleCodeActivity.
     */
    fun startService() {
        Log.d(TAG, getText(R.string.starting_obd_service).toString())

        val ctx = contextRef
        if (ctx is DiagnosticTroubleCodeActivity && bluetoothSocket?.isConnected == true) {
            Log.d(TAG, getText(R.string.executing_queue_paused).toString())
            serviceScope.launch {
                troubleCodes = requestTroubleCodes() ?: ""
            }
        } else if (ctx is DiagnosticTroubleCodeActivity) {
            Toast.makeText(ctx, getText(R.string.error_getting_dtc), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Retrieves diagnostic trouble codes by sending configuration commands followed by a DTC query.
     * Returns null on I/O or interrupt errors. Uses runBlocking for BT operations to maintain API compatibility.
     */
    fun requestTroubleCodes(vararg params: String): String? {
        var result = ""
        synchronized(this) {
            try {
                Log.d(TAG, getText(R.string.queueing_for_config).toString())
                val input = bluetoothSocket?.getInputStream() ?: return null
                val output = bluetoothSocket?.getOutputStream() ?: return null

                runBlocking {
                    ObdResetCommand().run(input, output)
                    EchoOffCommand().run(input, output)
                    LineFeedOffCommand().run(input, output)
                    SelectProtocolCommand(ObdProtocols.AUTO).run(input, output)
                }

                val tcoc = ModifiedTroubleCodesObdCommand()
                runBlocking {
                    tcoc.run(input, output)
                }
                result = tcoc.getFormattedResult() ?: ""
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, e.message.toString())
                return null
            } catch (e: InterruptedException) {
                e.printStackTrace()
                Log.e(TAG, e.message.toString())
                return null
            }
        }
        return result
    }

    /**
     * Returns the currently stored trouble codes string.
     */
    fun getTroubleCodes(): String = troubleCodes

    /**
     * Sets the stored trouble codes string.
     */
    fun setTroubleCodes(troubleCodes: String) {
        this.troubleCodes = troubleCodes
    }

    fun stopService() {}

    // -- Binder for Activity binding --

    override fun onBind(intent: android.content.Intent): android.os.IBinder = binder

    /**
     * Binder returned to Activities. Provides access to the ObdService instance.
     */
    inner class ObdServiceBinder : Binder() {
        fun getService(): ObdService = this@ObdService
    }

    // -- Property getters/setters (API compatibility with Java callers) --

    fun setVerboseMode(verboseMode: Boolean) {
        this.verboseMode = verboseMode
    }

    fun putObdDeviceQueue(value: Boolean) {
        // Legacy method kept for API compatibility; no longer used with Channel-based queue.
    }

    /** @deprecated ObD adapted checker thread is no longer used in coroutine-based service. */
    @Deprecated("Use coroutine-based approach instead")
    fun checkDevice() {}

    fun setObdDevice(obdDevice: Boolean) {
        this.obdDevice = obdDevice
    }

    /**
     * Sets the Bluetooth device to connect to. Call before [connectToDevice].
     */
    fun setBluetoothDevice(btd: BluetoothDevice?) {
        this.bluetoothDevice = btd
    }

    /**
     * Returns true if there are no pending jobs in the command channel.
     */
    fun queueEmpty(): Boolean = commandChannel.isEmpty

    /**
     * Clears all pending jobs from the command channel by closing and recreating it.
     */
    fun emptyQueue() {
        val oldChannel = commandChannel
        oldChannel.close()
        commandChannel = Channel()
        serviceScope.launch { executeQueue() }
    }

    /**
     * Returns the currently connected Bluetooth socket, or null if not connected.
     */
    fun getbluetoothSocket(): BluetoothSocket? = bluetoothSocket

    /**
     * Sets the context reference from the binding Activity for UI-related operations.
     */
    fun setContext(c: Context?) {
        this.contextRef = c
    }

    fun isDtc(): Boolean = dtc

    fun setDtc(dtc: Boolean) {
        this.dtc = dtc
    }

    /**
     * Returns the service's own context (the Service itself).
     */
    fun getDefaultContext(): Context = this
}
