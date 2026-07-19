package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class ChartViewModel : ViewModel() {

    private val _csvValues = MutableLiveData<Map<String, List<String>>>()
    val csvValues: LiveData<Map<String, List<String>>> = _csvValues

    private val _displayableParameters = MutableLiveData<List<String>>()
    val displayableParameters: LiveData<List<String>> = _displayableParameters

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmptyFile = MutableLiveData<Boolean>()
    val isEmptyFile: LiveData<Boolean> = _isEmptyFile

    private val NO_DISPLAY_PARAMETERS = listOf(
        "TIME",
        "LATITUDE",
        "LONGITUDE",
        "ALTITUDE",
        "VIN",
        "VEHICLE_ID",
        "FUEL_TYPE",
        "ENGINE_RUNTIME",
        "DISTANCE_TRAVELED_MIL_ON",
        "DTC_NUMBER",
        "TROUBLE_CODES"
    )

    fun loadCsvFile(filePath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    parseCsvFile(File(filePath))
                }
                if (result.isEmpty()) {
                    _isEmptyFile.value = true
                } else {
                    _csvValues.value = result
                    _displayableParameters.value = result.keys.filterNot { it in NO_DISPLAY_PARAMETERS }
                }
            } catch (_: Exception) {
                _isEmptyFile.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseCsvFile(file: File): Map<String, List<String>> {
        val csvValues = HashMap<String, MutableList<String>>()
        BufferedReader(FileReader(file)).use { br ->
            var isFirstLine = true
            var header: List<String>? = null
            var line: String?

            while (br.readLine().also { line = it } != null) {
                if (isFirstLine) {
                    header = line!!.split(";")
                    for (h in header!!) {
                        csvValues[h] = mutableListOf()
                    }
                    isFirstLine = false
                } else {
                    val values = line!!.split(";")
                    var idx = 0
                    for (v in values) {
                        if (idx < header!!.size && csvValues.containsKey(header!![idx])) {
                            csvValues[header!![idx]]?.add(v)
                        }
                        idx++
                    }
                }
            }
        }
        return csvValues
    }

    fun getTimeSeries(): List<Float> {
        val timeList = _csvValues.value?.get("TIME") ?: emptyList()
        return timeList.mapNotNull {
            try {
                it.toFloat()
            } catch (_: NumberFormatException) {
                null
            }
        }
    }

    fun getMaxTime(): Float {
        val times = getTimeSeries()
        return if (times.isNotEmpty()) times.last() else 0f
    }
}
