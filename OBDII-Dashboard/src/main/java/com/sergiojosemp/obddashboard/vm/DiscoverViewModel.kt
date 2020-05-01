package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.model.BluetoothModel

class DiscoverViewModel: ViewModel(){
    var device: MutableLiveData<BluetoothDeviceModel>
    var devices: MutableLiveData<ArrayList<BluetoothDeviceModel>>
    var connecting: MutableLiveData<Boolean>
    //val pr: MutableList = MutableList(1, )

    init{
        device = MutableLiveData()
        devices = MutableLiveData()
        connecting = MutableLiveData()
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
        //mostrarDispositivos();
        //Log.d(TAG, getText(R.string.connecting_text).toString())
        //Thread de conexión asíncorono
        device.value = _device
        System.out.println("Connecting")
    }
}