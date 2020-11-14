package com.sergiojosemp.obddashboard.vm


import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand
import com.sergiojosemp.obddashboard.model.ObdDataModel

class VerboseViewModel : ViewModel(){
    val compassIndicator: MutableLiveData<String> ?= MutableLiveData()
    val accelerationIndicator: MutableLiveData<String> ?= MutableLiveData()
    val obdResultsList: MutableLiveData<ArrayList<ObdDataModel>> ?= MutableLiveData()
    val obdReceivedList: ArrayList<String> = ArrayList()
    val speedIndicator: MutableLiveData<String> = MutableLiveData()
    val rpmIndicator: MutableLiveData<String> = MutableLiveData()
    val consumptionRateIndicator: MutableLiveData<String> = MutableLiveData()
    val bluetoothIndicator: MutableLiveData<String> = MutableLiveData()
    val obdIndicator: MutableLiveData<String> = MutableLiveData()

    init{
        obdResultsList?.postValue(ArrayList())
        speedIndicator.postValue("0")
        rpmIndicator.postValue("0")
        consumptionRateIndicator.postValue("0L/100KM")
        bluetoothIndicator.postValue("Connected")
        obdIndicator.postValue("No command")
    }
    @Synchronized
    fun setObdResult(obdDataModel: ObdDataModel){
        //Log command received
        obdIndicator.postValue(obdDataModel.commandName)
        Log.d(com.sergiojosemp.obddashboard.vm.TAG,"From Verbose ViewModel: OBD Command received -> ${obdDataModel.commandName}: ${obdDataModel.commandData} " )
        //Fill specific fields
        when(obdDataModel.commandName){ // If data belongs to any of the specific indicators of the VerboseView, then, post the value in the specific MutableLiveData and exit
            SpeedCommand().name -> {speedIndicator.postValue(obdDataModel.commandData); return}
            RPMCommand().name -> {rpmIndicator.postValue(obdDataModel.commandData); return}
            ConsumptionRateCommand().name -> {consumptionRateIndicator.postValue(obdDataModel.commandData + "L/100KM"); return}
        }
        //Fill other commands table
        val receivedDataList = obdResultsList?.value
        if(obdReceivedList.contains(obdDataModel.commandName)) {
            val commandIndex = obdReceivedList.indexOf(obdDataModel.commandName)
            receivedDataList?.removeAt(commandIndex)
            receivedDataList?.add(commandIndex, obdDataModel)
        }else{
            obdReceivedList.add(obdDataModel.commandName!!)
            receivedDataList?.add(obdDataModel)
        }
        obdResultsList?.postValue(receivedDataList)

        }
    }

