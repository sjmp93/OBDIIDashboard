package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectViewModel : ViewModel() {

    private val _deviceName = MutableLiveData<String>()
    val deviceName: LiveData<String> = _deviceName

    private val _deviceMac = MutableLiveData<String>()
    val deviceMac: LiveData<String> = _deviceMac

    private val _isConnecting = MutableLiveData<Boolean>()
    val isConnecting: LiveData<Boolean> = _isConnecting

    fun setDevice(name: String, mac: String) {
        _deviceName.value = name
        _deviceMac.value = mac
    }

    fun startConnecting() {
        _isConnecting.value = true
    }

    fun finishConnecting(success: Boolean) {
        _isConnecting.value = !success
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_MAC = "mac"
    }
}
