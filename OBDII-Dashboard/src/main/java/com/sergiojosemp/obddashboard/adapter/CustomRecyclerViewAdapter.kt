package com.sergiojosemp.obddashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel

class CustomRecyclerViewAdapter(private val devices: MutableList<BluetoothDeviceModel>) : RecyclerView.Adapter<CustomRecyclerViewAdapter.CustomViewHolder>() {

    /*fun removePost(post: Post) {
        val index = posts.indexOf(post)
        if (index != -1) {
            posts.remove(post)
            notifyItemRemoved(index)
        }
    }*/

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
            itemView.findViewById<TextView>(R.id.bluetoothDeviceName).text = bluetoothDevice.name
            itemView.findViewById<TextView>(R.id.bluetoothDeviceMac).text = bluetoothDevice.mac
            itemView.findViewById<Button>(R.id.bluetoothConnectRowButton).setOnClickListener {
                System.out.println("nop") //TODO
            }
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