package com.alijaya.genieacs.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.FragmentDevicesListBinding
import com.alijaya.genieacs.network.ApiService
import com.alijaya.genieacs.ui.adapters.DeviceListAdapter
import com.alijaya.genieacs.ui.main.MainActivity
import kotlinx.coroutines.launch

class DevicesListFragment : Fragment() {

    private var _binding: FragmentDevicesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var api: ApiService
    private lateinit var adapter: DeviceListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        api = ApiService(session)

        adapter = DeviceListAdapter(emptyList()) { dev ->
            (activity as? MainActivity)?.openDeviceDetail(dev)
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDevices.adapter = adapter

        binding.swipeRefreshList.setOnRefreshListener {
            loadDevices(binding.etSearchDevice.text.toString().trim())
        }

        binding.etSearchDevice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadDevices(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadDevices()
    }

    fun applyFilter(filterText: String) {
        binding.etSearchDevice.setText(filterText)
    }

    private fun loadDevices(query: String = "") {
        binding.swipeRefreshList.isRefreshing = true
        lifecycleScope.launch {
            val res = api.fetchDeviceList(query)
            binding.swipeRefreshList.isRefreshing = false
            if (res.isSuccess) {
                val list = res.getOrNull() ?: emptyList()
                adapter.updateData(list)
                if (query.isNotEmpty()) {
                    binding.tvDeviceCount.text = "Ditemukan ${list.size} modem dari pencarian \"$query\""
                } else {
                    binding.tvDeviceCount.text = "Total ${list.size} Modem Terdaftar di Server GenieACS"
                }
            } else {
                binding.tvDeviceCount.text = "Gagal memuat data live dari server"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
