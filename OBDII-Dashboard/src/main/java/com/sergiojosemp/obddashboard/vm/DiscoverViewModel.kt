package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.model.BluetoothModel

class DiscoverViewModel: ViewModel(){
    var devices: MutableLiveData<ArrayList<BluetoothDeviceModel>>
    //val pr: MutableList = MutableList(1, )

    init{
        devices = MutableLiveData()
        devices.value = ArrayList()
    }

    fun containsDevice(device: BluetoothDeviceModel):Boolean{
        for(_device in devices.value!!){
            if(device.mac.equals(_device.mac))
                return true
        }
        return false
    }
}