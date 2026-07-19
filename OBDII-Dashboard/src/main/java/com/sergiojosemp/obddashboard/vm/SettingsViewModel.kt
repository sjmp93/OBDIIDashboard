package com.sergiojosemp.obddashboard.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.sergiojosemp.obddashboard.model.PreferencesHelper
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext

    // LiveData for all settings, backed by DataStore flows
    val uploadData: LiveData<Boolean> = PreferencesHelper.uploadDataFlow(context).asLiveData()
    val obdUpdatePeriod: LiveData<String> = PreferencesHelper.obdUpdatePeriodFlow(context).asLiveData()
    val rpmMax: LiveData<String> = PreferencesHelper.rpmMaxFlow(context).asLiveData()
    val vehicleId: LiveData<String> = PreferencesHelper.vehicleIdFlow(context).asLiveData()
    val engineDisplacement: LiveData<String> = PreferencesHelper.engineDisplacementFlow(context).asLiveData()
    val volumetricEfficiency: LiveData<String> = PreferencesHelper.volumetricEfficiencyFlow(context).asLiveData()
    val imperialUnits: LiveData<Boolean> = PreferencesHelper.imperialUnitsFlow(context).asLiveData()
    val protocolsList: LiveData<String> = PreferencesHelper.protocolsListFlow(context).asLiveData()
    val enableGps: LiveData<Boolean> = PreferencesHelper.enableGpsFlow(context).asLiveData()
    val gpsUpdatePeriod: LiveData<String> = PreferencesHelper.gpsUpdatePeriodFlow(context).asLiveData()
    val gpsDistancePeriod: LiveData<String> = PreferencesHelper.gpsDistancePeriodFlow(context).asLiveData()
    val maxFuelEcon: LiveData<String> = PreferencesHelper.maxFuelEconFlow(context).asLiveData()
    val configReader: LiveData<String> = PreferencesHelper.configReaderFlow(context).asLiveData()
    val enableFullLogging: LiveData<Boolean> = PreferencesHelper.enableFullLoggingFlow(context).asLiveData()
    val directoryFullLogging: LiveData<String> = PreferencesHelper.directoryFullLoggingFlow(context).asLiveData()

    fun setUploadData(value: Boolean) {
        viewModelScope.launch {
            PreferencesHelper.setBoolean(context, PreferencesHelper.UPLOAD_DATA_KEY, value)
        }
    }

    fun setObdUpdatePeriod(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.OBD_UPDATE_PERIOD_KEY, value)
        }
    }

    fun setRpmMax(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.RPM_MAX_KEY, value)
        }
    }

    fun setVehicleId(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.VEHICLE_ID_KEY, value)
        }
    }

    fun setEngineDisplacement(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.ENGINE_DISPLACEMENT_KEY, value)
        }
    }

    fun setVolumetricEfficiency(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.VOLUMETRIC_EFFICIENCY_KEY, value)
        }
    }

    fun setImperialUnits(value: Boolean) {
        viewModelScope.launch {
            PreferencesHelper.setBoolean(context, PreferencesHelper.IMPERIAL_UNITS_KEY, value)
        }
    }

    fun setProtocolsList(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.PROTOCOLS_LIST_KEY, value)
        }
    }

    fun setEnableGps(value: Boolean) {
        viewModelScope.launch {
            PreferencesHelper.setBoolean(context, PreferencesHelper.ENABLE_GPS_KEY, value)
        }
    }

    fun setGpsUpdatePeriod(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.GPS_UPDATE_PERIOD_KEY, value)
        }
    }

    fun setGpsDistancePeriod(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.GPS_DISTANCE_PERIOD_KEY, value)
        }
    }

    fun setMaxFuelEcon(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.MAX_FUEL_ECON_KEY, value)
        }
    }

    fun setConfigReader(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.CONFIG_READER_KEY, value)
        }
    }

    fun setEnableFullLogging(value: Boolean) {
        viewModelScope.launch {
            PreferencesHelper.setBoolean(context, PreferencesHelper.ENABLE_FULL_LOGGING_KEY, value)
        }
    }

    fun setDirectoryFullLogging(value: String) {
        viewModelScope.launch {
            PreferencesHelper.setString(context, PreferencesHelper.DIRECTORY_FULL_LOGGING_KEY, value)
        }
    }

    fun setCommandEnabled(commandName: String, enabled: Boolean) {
        viewModelScope.launch {
            PreferencesHelper.setCommandEnabled(context, commandName, enabled)
        }
    }

    // Sync read for use in preference summary providers (called from main thread)
    fun getStringSync(key: String, defaultValue: String): String =
        PreferencesHelper.getStringSync(context, key, defaultValue)

    fun getBooleanSync(key: String, defaultValue: Boolean): Boolean =
        PreferencesHelper.getBooleanSync(context, key, defaultValue)
}
