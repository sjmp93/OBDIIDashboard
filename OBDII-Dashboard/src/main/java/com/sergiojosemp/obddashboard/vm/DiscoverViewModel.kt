package com.sergiojosemp.obddashboard.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel

val TAG: String = "OBD-Log"
class DiscoverViewModel: ViewModel(){
    var device: MutableLiveData<BluetoothDeviceModel>
    var devices: MutableLiveData<ArrayList<BluetoothDeviceModel>>
    var connecting: MutableLiveData<Boolean>
    var valueReceived: MutableLiveData<ByteArray>

    init{
        device = MutableLiveData()
        devices = MutableLiveData()
        connecting = MutableLiveData()
        valueReceived =  MutableLiveData()
        devices.value = ArrayList()
        connecting.value = false
    }

    fun containsDevice(device: BluetoothDeviceModel):Boolean{
        for(_device in devices.value!!){
            if(device.mac.equals(_device.mac))
                return true
        }
        return false
    }

    fun switchConnect(){
        connecting.value = true
    }

    fun connect(_device: BluetoothDeviceModel){
        device.postValue(_device)
        Log.d(TAG, "Connecting to ${_device.name} (MAC:${_device.mac})...")
    }
}