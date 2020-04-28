package com.sergiojosemp.obddashboard

class BluetoothModel(var state: Boolean ?= false) {
    fun commute(){
        state = !state!!
    }
}