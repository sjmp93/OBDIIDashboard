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
import com.sergiojosemp.obddashboard.activity.VerboseActivityKT
import com.sergiojosemp.obddashboard.model.BluetoothDeviceModel
import com.sergiojosemp.obddashboard.model.ObdDataModel
import com.sergiojosemp.obddashboard.vm.DiscoverViewModel
import com.sergiojosemp.obddashboard.vm.VerboseViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

//We get context from DiscoverActivity since this adapter is dependant of Android stuff and its context.
class ObdDataRecyclerViewAdapter(private val context : Context, private val obdData: MutableList<ObdDataModel>) : RecyclerView.Adapter<ObdDataRecyclerViewAdapter.CustomViewHolder>() {
    fun setData(data: MutableList<ObdDataModel>) {
        obdData.clear()
        obdData.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return CustomViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.obd_results_verbose_row, parent, false)
        )
    }

    override fun getItemCount(): Int = obdData.size

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(obdData[position])
    }

    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(obdData: ObdDataModel) {
            val verboseViewModel: VerboseViewModel = ViewModelProviders.of(context as VerboseActivityKT).get(VerboseViewModel::class.java)

            itemView.findViewById<TextView>(R.id.obdCommandName).text = obdData.commandName
            itemView.findViewById<TextView>(R.id.obdCommandValue).text = obdData.commandData
            //itemView.findViewById<Button>(R.id.bluetoothConnectRowButton).setOnClickListener(){

            /*    GlobalScope.launch { discoverViewModel.connecting.postValue(true)
                    discoverViewModel.connect(obdData)
                } //This is a blocking call, so we execute it in parallel to not to block the UI
            */
            //}
        }
    }

    object DataBindingAdapter {
        @BindingAdapter("data")
        @JvmStatic
        fun setRecyclerViewProperties(recyclerView: RecyclerView?, data: MutableList<ObdDataModel>?) {
            val adapter = recyclerView?.adapter
            if (adapter is ObdDataRecyclerViewAdapter && data != null) {
                adapter.setData(data)
            }
        }
    }
}