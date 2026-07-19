package com.sergiojosemp.obddashboard.model

class BluetoothModel(var state: Boolean ?= false) {
    fun commute(){
        state = !state!!
    }
}