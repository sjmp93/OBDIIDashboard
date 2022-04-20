package com.sergiojosemp.obddashboard.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.activity.DiscoverActivity
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

//We get context from DiscoverActivity since this adapter is dependant of Android stuff and its context.
class BluetoothDevicesRecyclerViewAdapter(private val context : Context, private val devices: MutableList<BluetoothDeviceModel>) : RecyclerView.Adapter<BluetoothDevicesRecyclerViewAdapter.CustomViewHolder>() {
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

                GlobalScope.launch { discoverViewModel.connecting.postValue(true)
                                     discoverViewModel.connect(bluetoothDevice)
                                    } //This is a blocking call, so we execute it in parallel to not to block the UI
            }
        }
    }

    object DataBindingAdapter {
        @BindingAdapter("data")
        @JvmStatic
        fun setRecyclerViewProperties(recyclerView: RecyclerView?, data: MutableList<BluetoothDeviceModel>?) {
            val adapter = recyclerView?.adapter
            if (adapter is BluetoothDevicesRecyclerViewAdapter && data != null) {
                adapter.setData(data)
            }
        }
    }
}