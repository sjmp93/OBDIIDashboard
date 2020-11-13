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
    val speedIndicator: MutableLiveData<String> = MutableLiveData()
    val rpmIndicator: MutableLiveData<String> = MutableLiveData()
    val consumptionRateIndicator: MutableLiveData<String> = MutableLiveData()
    init{
        obdResultsList?.postValue(ArrayList())
        speedIndicator.postValue("0")
        rpmIndicator.postValue("0")
        consumptionRateIndicator.postValue("0L/100KM")
    }

    fun setObdResult(obdDataModel: ObdDataModel){
        when(obdDataModel.commandName){ // If data belongs to any of the specific indicators of the VerboseView, then, post the value in the specific MutableLiveData and exit
            SpeedCommand().name -> {speedIndicator.postValue(obdDataModel.commandData); return}
            RPMCommand().name -> {rpmIndicator.postValue(obdDataModel.commandData); return}
            ConsumptionRateCommand().name -> {consumptionRateIndicator.postValue(obdDataModel.commandData + "L/100KM"); return}
        }
        val receivedDataList = obdResultsList?.value
        if(receivedDataList != null){
            if(checkIfYetReceived(obdDataModel)) {
                val commandIndex = checkIndexOfObdCommand(obdDataModel)
                receivedDataList?.removeAt(commandIndex!!)
                receivedDataList?.add(commandIndex, obdDataModel)
            }else{
                receivedDataList?.add(obdDataModel)
            }
            obdResultsList?.postValue(receivedDataList)
            Log.d(com.sergiojosemp.obddashboard.vm.TAG,"From Verbose Activity: OBD Command received -> ${obdDataModel.commandName}: ${obdDataModel.commandData} " )
        }
    }

    fun checkIfYetReceived(obdDataModel: ObdDataModel): Boolean{
        if(obdResultsList?.value != null)
            for(obdData in obdResultsList?.value!!){
                if(obdData.commandName.equals(obdDataModel.commandName))
                    return true
            }
        return false;
    }

    fun checkIndexOfObdCommand(obdDataModel: ObdDataModel): Int{
        var index = -1
        if(obdResultsList?.value != null)
            index = 0
            for(obdData in obdResultsList?.value!!){
                if(obdData.commandName.equals(obdDataModel.commandName))
                    break
                index ++
            }
        return index;
    }


}