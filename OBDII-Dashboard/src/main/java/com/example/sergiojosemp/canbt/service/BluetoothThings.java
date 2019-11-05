package com.example.sergiojosemp.canbt.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class BluetoothThings implements Serializable {
    private BluetoothAdapter mBluetoothAdapter;
    public BluetoothThings(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setBluetoothAdapter(BluetoothAdapter btAdapter){
        mBluetoothAdapter = btAdapter;
    }
    public BluetoothAdapter getBluetoothAdapter(){
        return mBluetoothAdapter;
    }


}
