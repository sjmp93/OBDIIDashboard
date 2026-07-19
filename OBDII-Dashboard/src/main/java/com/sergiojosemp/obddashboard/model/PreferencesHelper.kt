package com.sergiojosemp.obddashboard.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(
    name = "preferences",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "preferences"))
    }
)

object PreferencesHelper {

    val UPLOAD_DATA_KEY = booleanPreferencesKey("upload_data_preference")
    val OBD_UPDATE_PERIOD_KEY = stringPreferencesKey("obd_update_period_preference")
    val RPM_MAX_KEY = stringPreferencesKey("rpm_max_preference")
    val VEHICLE_ID_KEY = stringPreferencesKey("vehicle_id_preference")
    val ENGINE_DISPLACEMENT_KEY = stringPreferencesKey("engine_displacement_preference")
    val VOLUMETRIC_EFFICIENCY_KEY = stringPreferencesKey("volumetric_efficiency_preference")
    val IMPERIAL_UNITS_KEY = booleanPreferencesKey("imperial_units_preference")
    val PROTOCOLS_LIST_KEY = stringPreferencesKey("obd_protocols_preference")
    val ENABLE_GPS_KEY = booleanPreferencesKey("enable_gps_preference")
    val GPS_UPDATE_PERIOD_KEY = stringPreferencesKey("gps_update_period_preference")
    val GPS_DISTANCE_PERIOD_KEY = stringPreferencesKey("gps_distance_period_preference")
    val MAX_FUEL_ECON_KEY = stringPreferencesKey("max_fuel_econ_preference")
    val CONFIG_READER_KEY = stringPreferencesKey("reader_config_preference")
    val ENABLE_FULL_LOGGING_KEY = booleanPreferencesKey("enable_full_logging")
    val DIRECTORY_FULL_LOGGING_KEY = stringPreferencesKey("dirname_full_logging")

    private const val DEFAULT_OBD_UPDATE_PERIOD = "150"
    private const val DEFAULT_RPM_MAX = "7500"
    private const val DEFAULT_VEHICLE_ID = "00000000000000000000000"
    private const val DEFAULT_ENGINE_DISPLACEMENT = "1.6"
    private const val DEFAULT_VOLUMETRIC_EFFICIENCY = ".85"
    private const val DEFAULT_PROTOCOLS_LIST = "AUTO"
    private const val DEFAULT_GPS_UPDATE_PERIOD = "1"
    private const val DEFAULT_GPS_DISTANCE_PERIOD = "5"
    private const val DEFAULT_MAX_FUEL_ECON = "70"
    private const val DEFAULT_CONFIG_READER = "atsp0\natz"

    // --- Synchronous (blocking) getters for use from non-coroutine contexts ---

    fun getObdUpdatePeriodSync(context: Context): Int {
        return runBlocking {
            val preferences = context.dataStore.data.catch { e ->
                android.util.Log.e("PreferencesHelper", "Error reading OBD update period", e)
            }.first()
            val periodString = preferences[OBD_UPDATE_PERIOD_KEY] ?: DEFAULT_OBD_UPDATE_PERIOD
            var period = 1000
            try {
                period = periodString.toDoubleOrNull()?.toInt() ?: 1000
            } catch (_: Exception) {}
            if (period < 0) period = 4000
            period
        }
    }

    fun getVolumetricEfficiencySync(context: Context): Double {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val veString = preferences[VOLUMETRIC_EFFICIENCY_KEY] ?: DEFAULT_VOLUMETRIC_EFFICIENCY
            var ve = 0.85
            try {
                ve = veString.toDoubleOrNull() ?: 0.85
            } catch (_: Exception) {}
            ve
        }
    }

    fun getEngineDisplacementSync(context: Context): Double {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val edString = preferences[ENGINE_DISPLACEMENT_KEY] ?: DEFAULT_ENGINE_DISPLACEMENT
            var ed = 1.6
            try {
                ed = edString.toDoubleOrNull() ?: 1.6
            } catch (_: Exception) {}
            ed
        }
    }

    fun getMaxFuelEconomySync(context: Context): Double {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val maxStr = preferences[MAX_FUEL_ECON_KEY] ?: DEFAULT_MAX_FUEL_ECON
            var max = 70.0
            try {
                max = maxStr.toDoubleOrNull() ?: 70.0
            } catch (_: Exception) {}
            max
        }
    }

    fun getReaderConfigCommandsSync(context: Context): Array<String> {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val cmdsStr = preferences[CONFIG_READER_KEY] ?: DEFAULT_CONFIG_READER
            cmdsStr.split("\n").toTypedArray()
        }
    }

    fun getGpsUpdatePeriodSync(context: Context): Int {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val periodString = preferences[GPS_UPDATE_PERIOD_KEY] ?: DEFAULT_GPS_UPDATE_PERIOD
            var period = 1000
            try {
                period = (periodString.toDoubleOrNull() ?: 1.0 * 1000).toInt()
            } catch (_: Exception) {}
            if (period <= 0) period = 1000
            period
        }
    }

    fun getGpsDistanceUpdatePeriodSync(context: Context): Float {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val periodString = preferences[GPS_DISTANCE_PERIOD_KEY] ?: DEFAULT_GPS_DISTANCE_PERIOD
            var period = 5f
            try {
                period = periodString.toFloatOrNull() ?: 5f
            } catch (_: Exception) {}
            if (period <= 0) period = 5f
            period
        }
    }

    fun getMaxRPMSync(context: Context): Int {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val rpmString = preferences[RPM_MAX_KEY] ?: DEFAULT_RPM_MAX
            var rpm = 7500
            try {
                rpm = (rpmString.toDoubleOrNull()?.toInt()) ?: 7500
            } catch (_: Exception) {}
            if (rpm <= 0) rpm = 7500
            rpm
        }
    }

    fun getStringSync(context: Context, key: String, defaultValue: String): String {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            preferences[stringPreferencesKey(key)] ?: defaultValue
        }
    }

    fun getBooleanSync(context: Context, key: String, defaultValue: Boolean): Boolean {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            preferences[booleanPreferencesKey(key)] ?: defaultValue
        }
    }

    // --- Async (suspend) setters and getters ---

    suspend fun setString(context: Context, key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun setBoolean(context: Context, key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun setCommandEnabled(context: Context, commandName: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(commandName)] = enabled
        }
    }

    suspend fun isCommandEnabled(context: Context, commandName: String): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[booleanPreferencesKey(commandName)] ?: true
    }

    fun getCommandEnabledSync(context: Context, commandName: String): Boolean {
        return runBlocking {
            isCommandEnabled(context, commandName)
        }
    }

    // --- Flow-based getters for LiveData conversion in ViewModel ---

    fun uploadDataFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[UPLOAD_DATA_KEY] ?: false }

    fun obdUpdatePeriodFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[OBD_UPDATE_PERIOD_KEY] ?: DEFAULT_OBD_UPDATE_PERIOD }

    fun rpmMaxFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[RPM_MAX_KEY] ?: DEFAULT_RPM_MAX }

    fun vehicleIdFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[VEHICLE_ID_KEY] ?: DEFAULT_VEHICLE_ID }

    fun engineDisplacementFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[ENGINE_DISPLACEMENT_KEY] ?: DEFAULT_ENGINE_DISPLACEMENT }

    fun volumetricEfficiencyFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[VOLUMETRIC_EFFICIENCY_KEY] ?: DEFAULT_VOLUMETRIC_EFFICIENCY }

    fun imperialUnitsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[IMPERIAL_UNITS_KEY] ?: false }

    fun protocolsListFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[PROTOCOLS_LIST_KEY] ?: DEFAULT_PROTOCOLS_LIST }

    fun enableGpsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_GPS_KEY] ?: false }

    fun gpsUpdatePeriodFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[GPS_UPDATE_PERIOD_KEY] ?: DEFAULT_GPS_UPDATE_PERIOD }

    fun gpsDistancePeriodFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[GPS_DISTANCE_PERIOD_KEY] ?: DEFAULT_GPS_DISTANCE_PERIOD }

    fun maxFuelEconFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[MAX_FUEL_ECON_KEY] ?: DEFAULT_MAX_FUEL_ECON }

    fun configReaderFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[CONFIG_READER_KEY] ?: DEFAULT_CONFIG_READER }

    fun enableFullLoggingFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_FULL_LOGGING_KEY] ?: true }

    fun directoryFullLoggingFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[DIRECTORY_FULL_LOGGING_KEY] ?: "OBDDashboardLogs" }
}
