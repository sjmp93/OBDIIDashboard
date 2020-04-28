package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.model.BluetoothModel

class DiscoverViewModel: ViewModel(){
    val devices: MutableLiveData<List<BluetoothDeviceModel>> = TODO()

    init{
        devices = MutableLiveData()
    }

    
}