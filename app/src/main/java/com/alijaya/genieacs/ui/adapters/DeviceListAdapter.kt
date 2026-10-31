package com.alijaya.genieacs.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alijaya.genieacs.data.DeviceSummary
import com.alijaya.genieacs.databinding.ItemDeviceCardBinding

class DeviceListAdapter(
    private var devices: List<DeviceSummary>,
    private val onItemClick: (DeviceSummary) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    fun updateData(newDevices: List<DeviceSummary>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class ViewHolder(private val binding: ItemDeviceCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dev: DeviceSummary) {
            binding.tvCustomerName.text = dev.customerName
            binding.tvPppoeUser.text = "PPPoE: ${dev.pppoeUsername}"
            binding.tvSerialNumber.text = "SN: ${dev.serialNumber}"
            binding.tvIpAddress.text = "IP: ${dev.ipAddress}"
            binding.tvWifiPreview.text = "📶 WiFi: ${dev.wifiSsid}"

            // Status Online / Offline
            if (dev.isOnline) {
                binding.tvStatusBadge.text = "Online"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
            } else {
                binding.tvStatusBadge.text = "Offline"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
            }

            // Rx Power Badge Color
            binding.tvRxPower.text = dev.rxPower
            val rx = dev.rxPowerVal ?: -22.0
            if (rx <= -27.0) {
                binding.tvRxPower.setTextColor(Color.parseColor("#DC2626")) // Red
            } else if (rx <= -24.0) {
                binding.tvRxPower.setTextColor(Color.parseColor("#D97706")) // Yellow
            } else {
                binding.tvRxPower.setTextColor(Color.parseColor("#16A34A")) // Green
            }

            binding.root.setOnClickListener {
                onItemClick(dev)
            }
        }
    }
}
