package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sergiojosemp.obddashboard.model.BluetoothModel

class StartViewModel : ViewModel(){
    val data: MutableLiveData<BluetoothModel>

    init{
        data = MutableLiveData()
    }

    fun switchBT(){
        var _data = data.value ?: BluetoothModel()
        _data.commute()
        data.value = _data
    }
}