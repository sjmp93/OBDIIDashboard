package com.sergiojosemp.obddashboard.adapter

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.activity.ConnectActivity
import com.sergiojosemp.obddashboard.activity.DiscoverActivity
import com.sergiojosemp.obddashboard.databinding.BluetoothDeviceRowBinding
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel

//We get context from DiscoverActivity since this adapter is dependant of Android stuff and its context.
class CustomRecyclerViewAdapter(private val context : Context, private val devices: MutableList<BluetoothDeviceModel>) : RecyclerView.Adapter<CustomRecyclerViewAdapter.CustomViewHolder>() {
    fun setData(data: MutableList<BluetoothDeviceModel>) {
        devices.clear()
        devices.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return CustomViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.bluetooth_device_row, parent, false)
        )
    }

    override fun getItemCount(): Int = devices.size

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(devices[position])

    }

    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(bluetoothDevice: BluetoothDeviceModel) {
            val discoverViewModel: DiscoverViewModel = ViewModelProviders.of(context as DiscoverActivity).get(DiscoverViewModel::class.java)

            itemView.findViewById<TextView>(R.id.bluetoothDeviceName).text = bluetoothDevice.name
            itemView.findViewById<TextView>(R.id.bluetoothDeviceMac).text = bluetoothDevice.mac
            itemView.findViewById<Button>(R.id.bluetoothConnectRowButton).setOnClickListener(){
                System.out.println("Connecting...")
                //discoverViewModel.switchConnect()
                discoverViewModel.connecting.value = true
                discoverViewModel.connect(bluetoothDevice)
            }

            /*itemView.findViewById<Button>(R.id.bluetoothConnectRowButton).setOnClickListener {
                val connectActivity = Intent(itemView.context, ConnectActivity::class.java)
                connectActivity.putExtra("name", bluetoothDevice.name)
                connectActivity.putExtra("mac", bluetoothDevice.mac)
                //We stop bluetooth discovery because we are going to connect to bluetoothDevice.name
                val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                bluetoothAdapter?.cancelDiscovery()
                //DiscoveryActivity.putExtra("bluetoothAdapter", bluetooth);
                //startActivity(itemView.context,connectActivity,null)

            }*/
        }
    }

    object DataBindingAdapter {
        @BindingAdapter("data")
        @JvmStatic
        fun setRecyclerViewProperties(recyclerView: RecyclerView?, data: MutableList<BluetoothDeviceModel>?) {
            val adapter = recyclerView?.adapter
            if (adapter is CustomRecyclerViewAdapter && data != null) {
                adapter.setData(data)
            }
        }
    }
}