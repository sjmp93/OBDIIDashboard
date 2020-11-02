package com.sergiojosemp.obddashboard.vm


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VerboseViewModel : ViewModel(){
    val compassIndicator: MutableLiveData<String> ?= MutableLiveData()
    val accelerationIndicator: MutableLiveData<String> ?= MutableLiveData()
    init{

    }
}