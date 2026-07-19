package com.sergiojosemp.obddashboard.ui.settings

import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import com.github.pires.obd.enums.ObdProtocols
import com.github.pires.obd.reader.ObdConfig
import com.sergiojosemp.obddashboard.vm.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current

    var enableGps by remember { mutableStateOf(viewModel.enableGps.value ?: false) }
    var imperialUnits by remember { mutableStateOf(viewModel.imperialUnits.value ?: false) }
    var enableFullLogging by remember { mutableStateOf(viewModel.enableFullLogging.value ?: true) }
    var vehicleId by remember { mutableStateOf(viewModel.vehicleId.value ?: "") }
    var obdUpdatePeriod by remember { mutableStateOf(viewModel.obdUpdatePeriod.value ?: "260") }
    var rpmMax by remember { mutableStateOf(viewModel.rpmMax.value ?: "7500") }
    var maxFuelEcon by remember { mutableStateOf(viewModel.maxFuelEcon.value ?: "70") }
    var volumetricEfficiency by remember { mutableStateOf(viewModel.volumetricEfficiency.value ?: ".85") }
    var engineDisplacement by remember { mutableStateOf(viewModel.engineDisplacement.value ?: "1.6") }
    var protocolsList by remember { mutableStateOf(viewModel.protocolsList.value ?: ObdProtocols.AUTO.name) }
    var gpsUpdatePeriod by remember { mutableStateOf(viewModel.gpsUpdatePeriod.value ?: "1") }
    var gpsDistancePeriod by remember { mutableStateOf(viewModel.gpsDistancePeriod.value ?: "5") }
    var readerConfig by remember { mutableStateOf(viewModel.configReader.value ?: "atsp0\natz") }
    var dirnameFullLogging by remember { mutableStateOf(viewModel.directoryFullLogging.value ?: "OBDDashboardLogs") }

    val hasGps = remember {
        val locService = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locService.getProvider(LocationManager.GPS_PROVIDER) != null
    }

    var showCommandsDialog by remember { mutableStateOf(false) }
    var expandedProtocol by remember { mutableStateOf(false) }

    val protocols = remember { ObdProtocols.values().map { it.name }.toTypedArray() }

    LaunchedEffect(Unit) {
        viewModel.enableGps.observeForever { enableGps = it ?: false }
        viewModel.imperialUnits.observeForever { imperialUnits = it ?: false }
        viewModel.enableFullLogging.observeForever { enableFullLogging = it ?: true }
        viewModel.vehicleId.observeForever { vehicleId = it ?: "" }
        viewModel.obdUpdatePeriod.observeForever { obdUpdatePeriod = it ?: "260" }
        viewModel.rpmMax.observeForever { rpmMax = it ?: "7500" }
        viewModel.maxFuelEcon.observeForever { maxFuelEcon = it ?: "70" }
        viewModel.volumetricEfficiency.observeForever { volumetricEfficiency = it ?: ".85" }
        viewModel.engineDisplacement.observeForever { engineDisplacement = it ?: "1.6" }
        viewModel.protocolsList.observeForever { protocolsList = it ?: ObdProtocols.AUTO.name }
        viewModel.gpsUpdatePeriod.observeForever { gpsUpdatePeriod = it ?: "1" }
        viewModel.gpsDistancePeriod.observeForever { gpsDistancePeriod = it ?: "5" }
        viewModel.configReader.observeForever { readerConfig = it ?: "atsp0\natz" }
        viewModel.directoryFullLogging.observeForever { dirnameFullLogging = it ?: "OBDDashboardLogs" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            PreferenceCategory(title = "Data Upload") {
                EditTextPreferenceItem(
                    title = "Vehicle ID",
                    value = vehicleId,
                    onValueChange = { newValue ->
                        vehicleId = newValue
                        viewModel.setVehicleId(newValue)
                    }
                )
            }

            if (hasGps) {
                PreferenceCategory(title = "GPS") {
                    SwitchPreferenceItem(
                        title = "Enable GPS",
                        checked = enableGps,
                        onCheckedChange = { newValue ->
                            enableGps = newValue
                            viewModel.setEnableGps(newValue)
                        }
                    )

                    if (enableGps) {
                        EditTextPreferenceItem(
                            title = "Update Period (seconds)",
                            value = gpsUpdatePeriod,
                            onValueChange = { newValue ->
                                gpsUpdatePeriod = newValue
                                viewModel.setGpsUpdatePeriod(newValue)
                            },
                            validateNumber = true
                        )

                        EditTextPreferenceItem(
                            title = "Update Period (meters)",
                            value = gpsDistancePeriod,
                            onValueChange = { newValue ->
                                gpsDistancePeriod = newValue
                                viewModel.setGpsDistancePeriod(newValue)
                            },
                            validateNumber = true
                        )
                    }
                }
            }

            PreferenceCategory(title = "OBD Preferences") {
                ExposedDropdownMenuBox(
                    expanded = expandedProtocol,
                    onExpandedChange = { expandedProtocol = !expandedProtocol }
                ) {
                    OutlinedTextField(
                        value = protocolsList,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("OBD Protocol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProtocol) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProtocol,
                        onDismissRequest = { expandedProtocol = false }
                    ) {
                        protocols.forEachIndexed { index: Int, protocol: String ->
                            DropdownMenuItem(
                                text = { Text(protocol) },
                                onClick = {
                                    protocolsList = protocol
                                    viewModel.setProtocolsList(protocol)
                                    expandedProtocol = false
                                }
                            )
                        }
                    }
                }

                SwitchPreferenceItem(
                    title = "Imperial Units",
                    checked = imperialUnits,
                    onCheckedChange = { newValue ->
                        imperialUnits = newValue
                        viewModel.setImperialUnits(newValue)
                    }
                )

                EditTextPreferenceItem(
                    title = "Update Period (ms)",
                    value = obdUpdatePeriod,
                    onValueChange = { newValue ->
                        obdUpdatePeriod = newValue
                        viewModel.setObdUpdatePeriod(newValue)
                    },
                    validateNumber = true
                )

                EditTextPreferenceItem(
                    title = "Max RPM",
                    value = rpmMax,
                    onValueChange = { newValue ->
                        rpmMax = newValue
                        viewModel.setRpmMax(newValue)
                    },
                    validateNumber = true
                )

                EditTextPreferenceItem(
                    title = "Maximum Fuel Economy Value",
                    value = maxFuelEcon,
                    onValueChange = { newValue ->
                        maxFuelEcon = newValue
                        viewModel.setMaxFuelEcon(newValue)
                    },
                    validateNumber = true
                )

                EditTextPreferenceItem(
                    title = "Volumetric Efficiency",
                    value = volumetricEfficiency,
                    onValueChange = { newValue ->
                        volumetricEfficiency = newValue
                        viewModel.setVolumetricEfficiency(newValue)
                    },
                    validateNumber = true
                )

                EditTextPreferenceItem(
                    title = "Engine Displacement (liters)",
                    value = engineDisplacement,
                    onValueChange = { newValue ->
                        engineDisplacement = newValue
                        viewModel.setEngineDisplacement(newValue)
                    },
                    validateNumber = true
                )

                EditTextPreferenceItem(
                    title = "Reader Config Commands",
                    value = readerConfig.replace("\n", "\\n"),
                    onValueChange = { newValue ->
                        readerConfig = newValue.replace("\\n", "\n")
                        viewModel.setConfigReader(readerConfig)
                    }
                )
            }

            PreferenceCategory(title = "OBD Commands") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCommandsDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "OBD Commands", style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, )
                }
            }

            PreferenceCategory(title = "Full Logging Configuration") {
                SwitchPreferenceItem(
                    title = "Enable full logging",
                    checked = enableFullLogging,
                    onCheckedChange = { newValue ->
                        enableFullLogging = newValue
                        viewModel.setEnableFullLogging(newValue)
                    }
                )

                if (enableFullLogging) {
                    EditTextPreferenceItem(
                        title = "Directory name",
                        value = dirnameFullLogging,
                        onValueChange = { newValue ->
                            dirnameFullLogging = newValue
                            viewModel.setDirectoryFullLogging(newValue)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCommandsDialog) {
        CommandsSelectionDialog(
            onDismiss = { showCommandsDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun PreferenceCategory(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun SwitchPreferenceItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun EditTextPreferenceItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    validateNumber: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = value.ifEmpty { "(empty)" }, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, )
    }

    if (showDialog) {
        var inputValue by remember { mutableStateOf(value.replace("\n", " ")) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (validateNumber && inputValue.isNotBlank()) {
                            val cleanInput = inputValue.replace(",", ".")
                            if (cleanInput.toDoubleOrNull() != null) {
                                onValueChange(cleanInput)
                            }
                        } else {
                            onValueChange(inputValue)
                        }
                        showDialog = false
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
fun CommandsSelectionDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val commands = remember { ObdConfig.getCommands() }
    var selectedCommands by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "OBD Commands") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(commands.toList()) { cmd ->
                    val isChecked = selectedCommands.contains(cmd.getName())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) {
                                    selectedCommands -= cmd.getName()
                                } else {
                                    selectedCommands += cmd.getName()
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cmd.getName(), style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked = isChecked, onCheckedChange = {})
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                commands.forEach({ cmd: com.github.pires.obd.commands.ObdCommand ->
                    val name = cmd.getName()
                    viewModel.setCommandEnabled(name, selectedCommands.contains(name))
                })
                onDismiss()
            }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

