package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.enums.AvailableCommandNames
import com.github.pires.obd.reader.LogCSVWriter
import com.github.pires.obd.reader.ObdCommandJob
import com.github.pires.obd.reader.ObdReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel : ViewModel() {

    // Speed/RPM pie chart data
    val speedText: MutableLiveData<String> = MutableLiveData("0 KM/H")
    val rpmText: MutableLiveData<String> = MutableLiveData("0 RPM")
    val voltageText: MutableLiveData<String> = MutableLiveData("0.0V")

    // Progress bar values (5 configurable parameters)
    val progressValues: MutableLiveData<FloatArray> = MutableLiveData(floatArrayOf(0f, 0f, 0f, 0f, 0f))
    val progressFormattedResults: MutableLiveData<Array<String>> = MutableLiveData(arrayOf("", "", "", "", ""))

    // GPS data
    val gpsText: MutableLiveData<String> = MutableLiveData("")

    // Sensor readings
    val compassIndicator: MutableLiveData<String> = MutableLiveData("N")
    val accelerationIndicator: MutableLiveData<String> = MutableLiveData("0.0")

    // Status indicators
    val obdStatusText: MutableLiveData<String> = MutableLiveData("")

    // Internal state for command processing and CSV logging
    private var commandResultMap = mutableMapOf<String, String>()
    private var startTime: Long = 0L
    private var endTime: Long = 0L

    fun setSpeed(speedValue: String) {
        if (speedValue.matches(Regex("[a-zA-Z]"))) {
            speedText.postValue("$speedValue KM/H")
        } else {
            speedText.postValue("${speedValue} KM/H")
        }
    }

    fun setRpm(rpmValue: String, maxRpm: Int) {
        if (rpmValue.matches(Regex("[a-zA-Z]"))) {
            rpmText.postValue("$rpmValue RPM")
        } else {
            rpmText.postValue("${rpmValue} RPM")
        }
    }

    fun setVoltage(voltageResult: String) {
        voltageText.postValue(voltageResult)
    }

    fun setCompass(dir: String) {
        compassIndicator.postValue(dir)
    }

    fun setAcceleration(gForce: String) {
        accelerationIndicator.postValue(gForce)
    }

    fun setGpsText(text: String) {
        gpsText.postValue(text)
    }

    fun setObdStatus(status: String) {
        obdStatusText.postValue(status)
    }

    fun updateProgress(index: Int, value: Float, formattedResult: String) {
        val currentValues = progressValues.value ?: floatArrayOf(0f, 0f, 0f, 0f, 0f)
        if (index in currentValues.indices) {
            currentValues[index] = value
            progressValues.postValue(currentValues.copyOf())

            val currentFormatted = progressFormattedResults.value ?: arrayOf("", "", "", "", "")
            currentFormatted[index] = formattedResult
            progressFormattedResults.postValue(currentFormatted.copyOf())
        }
    }

    fun processCommandJob(job: ObdCommandJob) {
        val cmdName = job.command.getName()
        var cmdResult = ""
        val cmdID = lookUpCommand(cmdName)

        when (job.state) {
            ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR -> {
                cmdResult = job.command.getFormattedResult() ?: ""
                obdStatusText.postValue(cmdResult.lowercase())
            }
            ObdCommandJob.ObdCommandJobState.BROKEN_PIPE -> {
                // Handle broken pipe
            }
            ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED -> {
                cmdResult = "Not supported"
            }
            else -> {
                cmdResult = job.command.getFormattedResult() ?: ""
                obdStatusText.postValue(cmdName)
            }
        }

        // Update progress bars for configurable parameters
        setProgressForCommand(job)

        when (cmdID) {
            AvailableCommandNames.SPEED.name -> {
                val calculated = job.command.getCalculatedResult() ?: ""
                if (!calculated.matches(Regex("[a-zA-Z]"))) {
                    speedText.postValue("${calculated} KM/H")
                } else {
                    speedText.postValue(calculated)
                }
            }
            AvailableCommandNames.ENGINE_RPM.name -> {
                val calculated = job.command.getCalculatedResult() ?: ""
                if (!calculated.matches(Regex("[a-zA-Z]"))) {
                    rpmText.postValue("${calculated} RPM")
                } else {
                    rpmText.postValue(calculated)
                }
            }
            AvailableCommandNames.CONTROL_MODULE_VOLTAGE.name -> {
                voltageText.postValue(job.command.getFormattedResult() ?: "")
            }
        }

        commandResultMap[cmdID] = cmdResult
    }

    fun setProgressForCommand(job: ObdCommandJob) {
        val progressIndex = getProgressIndexForCommand(job.command.getName())
        if (progressIndex >= 0 && progressIndex < 5) {
            val calculated = job.command.getCalculatedResult() ?: "0"
            val formatted = job.command.getFormattedResult() ?: ""
            try {
                updateProgress(progressIndex, calculated.toFloat(), formatted)
            } catch (_: NumberFormatException) {
                // Skip invalid values
            }
        }
    }

    fun getCommandResults(): Map<String, String> {
        return commandResultMap.toMap()
    }

    fun clearCommandResults() {
        commandResultMap.clear()
    }

    fun updateTimestamps(): Float {
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
            endTime = System.currentTimeMillis()
        } else {
            endTime = System.currentTimeMillis()
        }
        return ((endTime - startTime).toFloat()) / 1000f
    }

    fun writeCsvReading(
        csvWriter: LogCSVWriter?,
        lat: Double,
        lon: Double,
        alt: Double,
        vin: String
    ) {
        if (commandResultMap.isEmpty()) return

        val time = updateTimestamps()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val reading = ObdReading(lat, lon, alt, time, vin, commandResultMap.toMap())
                csvWriter?.writeLineCSV(reading)
            }
        }
    }

    fun queueCommandsLoop(
        updatePeriodMillis: Long,
        obdServiceInstance: Any?,
        bluetoothSocketConnected: () -> Boolean,
        queueEmpty: () -> Boolean,
        queueJob: (ObdCommandJob) -> Unit,
        getCommands: () -> ArrayList<ObdCommand>,
        isEnabledForCommand: (String) -> Boolean,
        onJobProcessed: (ObdCommandJob) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                while (true) {
                    // Check connection and queue status before queuing commands
                    delay(updatePeriodMillis)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Normal cancellation
            }
        }
    }

    companion object {
        fun lookUpCommand(txt: String): String {
            for (item in AvailableCommandNames.values()) {
                if (item.getValue().equals(txt)) return item.name
            }
            return txt
        }

        fun getProgressIndexForCommand(cmdName: String, obdDataArray: Array<String>? = null): Int {
            // Default mapping based on ObdConfig command order and progress bar defaults
            val defaultMapping = mapOf(
                "ENGINE_COOLANT_TEMP" to 0,
                "ENGINE_OIL_TEMP" to 1,
                "ENGINE_LOAD" to 2,
                "THROTTLE_POS" to 3,
                "AIR_INTAKE_TEMP" to 4,
                "AMBIENT_AIR_TEMP" to 5,
                "INTAKE_MANIFOLD_PRESSURE" to 6,
                "FUEL_RAIL_PRESSURE" to 7,
                "FUEL_PRESSURE" to 8,
                "BAROMETRIC_PRESSURE" to 9,
                "FUEL_LEVEL" to 10,
                "FUEL_CONSUMPTION_RATE" to 11,
                "MAF" to 12
            )
            val idx = defaultMapping[cmdName] ?: -1
            return if (idx < 5) idx else -1
        }
    }
}
