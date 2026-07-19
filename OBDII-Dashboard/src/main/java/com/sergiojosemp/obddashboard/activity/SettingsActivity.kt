package com.sergiojosemp.obddashboard.activity

import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.*
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.enums.ObdProtocols
import com.github.pires.obd.reader.ObdConfig
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.model.PreferencesHelper
import com.sergiojosemp.obddashboard.vm.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

        private lateinit var viewModel: SettingsViewModel

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            viewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
            checkGps()
            loadPreferenceValues()
            setupNumericPrefs()
            setupProtocolList()
            setupCommandsScreen()
        }

        private fun loadPreferenceValues() {
            // Values are loaded from DataStore via ViewModel; preferences display current state through summaries
            findPreference<CheckBoxPreference>("enable_gps_preference")?.isChecked =
                viewModel.enableGps.value ?: false
            findPreference<CheckBoxPreference>("imperial_units_preference")?.isChecked =
                viewModel.imperialUnits.value ?: false
            findPreference<CheckBoxPreference>("enable_full_logging")?.isChecked =
                viewModel.enableFullLogging.value ?: true
        }

        private fun setupNumericPrefs() {
            val keys = listOf(
                "engine_displacement_preference",
                "volumetric_efficiency_preference",
                "obd_update_period_preference",
                "max_fuel_econ_preference",
                "gps_update_period_preference",
                "gps_distance_period_preference",
                "rpm_max_preference",
                "vehicle_id_preference",
                "reader_config_preference",
                "dirname_full_logging"
            )
            for (key in keys) {
                findPreference<EditTextPreference>(key)?.onPreferenceChangeListener = this@SettingsFragment
            }
        }

        private fun setupProtocolList() {
            val protocolStrings = ObdProtocols.values().map { it.name }.toTypedArray()
            findPreference<ListPreference>("obd_protocols_preference")?.apply {
                entries = protocolStrings
                entryValues = protocolStrings
                onPreferenceChangeListener = this@SettingsFragment
            }
        }

        private fun setupCommandsScreen() {
            val cmdScreenKey = "obd_commands_screen"
            val cmdScreen = findPreference<PreferenceScreen>(cmdScreenKey)
            if (cmdScreen != null) {
                ObdConfig.getCommands().forEach { cmd ->
                    CheckBoxPreference(requireContext()).apply {
                        title = cmd.getName()
                        key = cmd.getName()
                        isChecked = true
                        onPreferenceChangeListener = this@SettingsFragment
                        cmdScreen.addPreference(this)
                    }
                }
            }
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val key = preference.key ?: return false

            if (key in setOf(
                "engine_displacement_preference",
                "volumetric_efficiency_preference",
                "obd_update_period_preference",
                "max_fuel_econ_preference",
                "gps_update_period_preference",
                "gps_distance_period_preference",
                "rpm_max_preference"
            )) {
                try {
                    newValue.toString().replace(",", ".").toDoubleOrNull() ?: throw NumberFormatException()
                } catch (_: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Couldn't parse '${newValue}' as a number.",
                        Toast.LENGTH_LONG
                    ).show()
                    return false
                }
            }

            when (key) {
                "upload_data_preference" -> viewModel.setUploadData(newValue as Boolean)
                "obd_update_period_preference" -> viewModel.setObdUpdatePeriod(newValue as String)
                "rpm_max_preference" -> viewModel.setRpmMax(newValue as String)
                "vehicle_id_preference" -> viewModel.setVehicleId(newValue as String)
                "engine_displacement_preference" -> viewModel.setEngineDisplacement(newValue as String)
                "volumetric_efficiency_preference" -> viewModel.setVolumetricEfficiency(newValue as String)
                "imperial_units_preference" -> viewModel.setImperialUnits(newValue as Boolean)
                "obd_protocols_preference" -> viewModel.setProtocolsList(newValue as String)
                "enable_gps_preference" -> viewModel.setEnableGps(newValue as Boolean)
                "gps_update_period_preference" -> viewModel.setGpsUpdatePeriod(newValue as String)
                "gps_distance_period_preference" -> viewModel.setGpsDistancePeriod(newValue as String)
                "max_fuel_econ_preference" -> viewModel.setMaxFuelEcon(newValue as String)
                "reader_config_preference" -> viewModel.setConfigReader(newValue as String)
                "enable_full_logging" -> viewModel.setEnableFullLogging(newValue as Boolean)
                "dirname_full_logging" -> viewModel.setDirectoryFullLogging(newValue as String)
                else -> {
                    if (newValue is Boolean) {
                        viewModel.setCommandEnabled(key, newValue)
                    }
                }
            }

            return true
        }

        private fun checkGps() {
            val locService = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locProvider = locService.getProvider(LocationManager.GPS_PROVIDER)
            if (locProvider == null) {
                hideGPSCategory()
            }
        }

        private fun hideGPSCategory() {
            val gpsCategoryKey = requireContext().getString(R.string.pref_gps_category)
            val parentScreen = preferenceScreen
            findPreference<PreferenceCategory>(gpsCategoryKey)?.let { category ->
                category.removeAll()
                parentScreen.removePreference(category)
            }
        }
    }
}
