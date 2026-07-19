package com.github.pires.obd.reader

import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.control.DistanceMILOnCommand
import com.github.pires.obd.commands.control.DtcNumberCommand
import com.github.pires.obd.commands.control.EquivalentRatioCommand
import com.github.pires.obd.commands.control.ModuleVoltageCommand
import com.github.pires.obd.commands.control.TimingAdvanceCommand
import com.github.pires.obd.commands.control.TroubleCodesCommand
import com.github.pires.obd.commands.control.VinCommand
import com.github.pires.obd.commands.engine.LoadCommand
import com.github.pires.obd.commands.engine.MassAirFlowCommand
import com.github.pires.obd.commands.engine.OilTempCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.engine.RuntimeCommand
import com.github.pires.obd.commands.engine.ThrottlePositionCommand
import com.github.pires.obd.commands.fuel.AirFuelRatioCommand
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand
import com.github.pires.obd.commands.fuel.FuelLevelCommand
import com.github.pires.obd.commands.fuel.FuelTrimCommand
import com.github.pires.obd.commands.fuel.WidebandAirFuelRatioCommand
import com.github.pires.obd.commands.pressure.BarometricPressureCommand
import com.github.pires.obd.commands.pressure.FuelPressureCommand
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand
import com.github.pires.obd.enums.FuelTrim
import java.util.ArrayList

object ObdConfig {

    @JvmName("getCommands")
    fun getCommands(): ArrayList<ObdCommand> {
        val cmds = ArrayList<ObdCommand>()

        // Pressure
        cmds.add(BarometricPressureCommand())
        cmds.add(FuelPressureCommand())
        cmds.add(FuelRailPressureCommand())
        cmds.add(IntakeManifoldPressureCommand())

        // Temperature
        cmds.add(AirIntakeTemperatureCommand())
        cmds.add(AmbientAirTemperatureCommand())
        cmds.add(EngineCoolantTemperatureCommand())

        // Control
        cmds.add(ModuleVoltageCommand())
        cmds.add(EquivalentRatioCommand())
        //cmds.add(DistanceMILOnCommand())
        //cmds.add(DtcNumberCommand())
        cmds.add(TimingAdvanceCommand())
        //cmds.add(TroubleCodesCommand())
        //cmds.add(VinCommand())

        // Misc
        cmds.add(SpeedCommand()) // FIXME SharedPreferences Map size limit = 40

        // Engine
        cmds.add(LoadCommand())
        cmds.add(RPMCommand())
        cmds.add(RuntimeCommand())
        cmds.add(MassAirFlowCommand())
        cmds.add(ThrottlePositionCommand())

        // Fuel
        //cmds.add(FindFuelTypeCommand())
        cmds.add(ConsumptionRateCommand())
        // cmds.add(AverageFuelEconomyObdCommand())
        //cmds.add(FuelEconomyCommand())
        cmds.add(FuelLevelCommand())
        // cmds.add(FuelEconomyMAPObdCommand())
        // cmds.add(FuelEconomyCommandedMAPObdCommand())
        //cmds.add(FuelTrimCommand(FuelTrim.LONG_TERM_BANK_1))
        //cmds.add(FuelTrimCommand(FuelTrim.LONG_TERM_BANK_2))
        //cmds.add(FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_1))
        //cmds.add(FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_2))
        cmds.add(AirFuelRatioCommand())
        cmds.add(WidebandAirFuelRatioCommand())
        cmds.add(OilTempCommand())

        return cmds
    }
}
