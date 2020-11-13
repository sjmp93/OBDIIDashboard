package com.sergiojosemp.obddashboard.vm


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sergiojosemp.obddashboard.model.ObdDataModel

class VerboseViewModel : ViewModel(){
    val compassIndicator: MutableLiveData<String> ?= MutableLiveData()
    val accelerationIndicator: MutableLiveData<String> ?= MutableLiveData()
    val obdResultsList: MutableLiveData<ArrayList<ObdDataModel>> ?= MutableLiveData()
    init{
        val dummyObdData = ArrayList<ObdDataModel>();
        var obdDataModel = ObdDataModel("Prueba","Datos")

        for (i in 0..100){
            dummyObdData.add(obdDataModel)
        }
        obdResultsList?.postValue(dummyObdData)

    }
}