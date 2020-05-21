package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MenuViewModel: ViewModel() {
    var obdDebugReceivedValue: MutableLiveData<String> ?= null //TODO remove this when dashboard is working
    var selectedOption: MutableLiveData<Int> ?= null
    init{
        obdDebugReceivedValue = MutableLiveData()
        selectedOption = MutableLiveData()
    }

    fun setValue(receivedValue: String){
        obdDebugReceivedValue!!.postValue(receivedValue)
    }

    /**
     *
     * @param option describes the selected option:
     *  option == 1 -> DashBoard Mode
     *  option == 2 -> Verbose Mode
     *  option == 3 -> Chart Mode
     *  option == 4 -> DTC Mode
     *  option == 5 -> Settings
     */
    fun setSelectedOption(option: Int){
        selectedOption?.postValue(option)
    }
}