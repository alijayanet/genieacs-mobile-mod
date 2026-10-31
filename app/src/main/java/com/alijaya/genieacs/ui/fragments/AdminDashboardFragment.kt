package com.alijaya.genieacs.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.FragmentAdminDashboardBinding
import com.alijaya.genieacs.network.ApiService
import com.alijaya.genieacs.ui.adapters.DeviceListAdapter
import com.alijaya.genieacs.ui.main.MainActivity
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var api: ApiService
    private lateinit var adapter: DeviceListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        api = ApiService(session)

        setupRecyclerView()
        setupListeners()
        loadDashboardData()
    }

    private fun setupRecyclerView() {
        adapter = DeviceListAdapter(emptyList()) { dev ->
            (activity as? MainActivity)?.openDeviceDetail(dev)
        }
        binding.rvRecentDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentDevices.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        binding.btnQuickCheckOffline.setOnClickListener {
            (activity as? MainActivity)?.openDeviceListWithFilter("offline")
        }

        binding.btnQuickCheckRx.setOnClickListener {
            (activity as? MainActivity)?.openDeviceListWithFilter("kritis")
        }
    }

    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val listRes = api.fetchDeviceList()
            binding.swipeRefresh.isRefreshing = false
            if (listRes.isSuccess) {
                val devices = listRes.getOrNull() ?: emptyList()
                val total = devices.size
                val online = devices.count { it.isOnline }
                val offline = total - online
                val critical = devices.count { 
                    val rx = it.rxPowerVal
                    rx != null && rx <= -27.0
                }

                binding.tvTotalDevices.text = total.toString()
                binding.tvOnlineDevices.text = online.toString()
                binding.tvOfflineDevices.text = offline.toString()
                binding.tvCriticalDevices.text = critical.toString()

                adapter.updateData(devices.take(6))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
