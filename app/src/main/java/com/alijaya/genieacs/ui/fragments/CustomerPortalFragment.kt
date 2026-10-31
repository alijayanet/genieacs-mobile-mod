package com.alijaya.genieacs.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.FragmentCustomerPortalBinding
import com.alijaya.genieacs.network.ApiService
import kotlinx.coroutines.launch

class CustomerPortalFragment : Fragment() {

    private var _binding: FragmentCustomerPortalBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var api: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerPortalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        api = ApiService(session)

        binding.tvCustPhoneInfo.text = "Nomor WhatsApp: " + session.userIdentifier

        setupListeners()
        loadCustomerModemData()
    }

    private fun setupListeners() {
        binding.swipeRefreshCustomer.setOnRefreshListener {
            loadCustomerModemData()
        }

        // Restart Modem Mandiri
        binding.btnCustReboot.setOnClickListener {
            showRebootConfirmDialog()
        }

        // Ganti Sandi WiFi Mandiri
        binding.btnCustChangeWifi.setOnClickListener {
            showChangeWifiDialog()
        }

        // Hubungi CS WA
        binding.btnCustChatCs.setOnClickListener {
            try {
                val url = "https://wa.me/6281947215703?text=Halo%20Admin%20Alijaya%2C%20saya%20butuh%20bantuan%20terkait%20koneksi%20WiFi%20dengan%20no%20HP%20${session.userIdentifier}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Aplikasi WhatsApp tidak terpasang", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCustomerModemData() {
        binding.swipeRefreshCustomer.isRefreshing = true
        lifecycleScope.launch {
            val res = api.getCustomerModemInfo(session.userIdentifier)
            binding.swipeRefreshCustomer.isRefreshing = false
            if (res.isSuccess) {
                val dev = res.getOrNull()!!
                binding.tvCustModemTitle.text = "Modem ${dev.productClass} (${dev.serialNumber})"
                binding.tvCustWifiSsid.text = dev.wifiSsid
                binding.tvCustWifiPass.text = dev.wifiPass

                if (dev.isOnline) {
                    binding.tvCustOnlineStatus.text = "🟢 Terhubung (Online)"
                    binding.tvCustOnlineStatus.setTextColor(Color.parseColor("#10B981"))
                } else {
                    binding.tvCustOnlineStatus.text = "🔴 Terputus (Offline)"
                    binding.tvCustOnlineStatus.setTextColor(Color.parseColor("#EF4444"))
                }

                binding.tvCustRxValue.text = dev.rxPower
                val rxVal = dev.rxPower.replace("dBm", "").trim().toDoubleOrNull() ?: -22.0
                if (rxVal <= -27.0) {
                    binding.tvCustRxValue.setTextColor(Color.parseColor("#EF4444"))
                    binding.tvCustRxDesc.text = "Sinyal Lemah - Hubungi Admin untuk Perbaikan"
                } else {
                    binding.tvCustRxValue.setTextColor(Color.parseColor("#10B981"))
                    binding.tvCustRxDesc.text = "Sinyal Bagus & Sangat Stabil"
                }
            }
        }
    }

    private fun showRebootConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Restart Modem")
            .setMessage("Modem Anda akan dimatikan dan dinyalakan ulang secara otomatis. Proses ini memakan waktu sekitar 1-2 menit. Lanjutkan?")
            .setPositiveButton("Ya, Restart") { _, _ ->
                lifecycleScope.launch {
                    Toast.makeText(requireContext(), "Mengirim sinyal reboot ke modem...", Toast.LENGTH_SHORT).show()
                    val res = api.rebootDevice(session.customerDeviceId.ifEmpty { session.userIdentifier })
                    if (res.isSuccess) {
                        Toast.makeText(requireContext(), "Perintah restart berhasil diterima!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showChangeWifiDialog() {
        val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val etSsid = EditText(requireContext()).apply {
            hint = "Nama WiFi Baru (SSID)"
            setText(binding.tvCustWifiSsid.text)
        }
        val etPass = EditText(requireContext()).apply {
            hint = "Sandi WiFi Baru (Min. 8 Karakter)"
        }
        container.addView(etSsid)
        container.addView(etPass)

        AlertDialog.Builder(requireContext())
            .setTitle("Ganti Nama & Sandi WiFi")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val newSsid = etSsid.text.toString().trim()
                val newPass = etPass.text.toString().trim()
                if (newSsid.isEmpty() || newPass.length < 8) {
                    Toast.makeText(requireContext(), "Sandi minimal 8 karakter", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val res = api.updateWifi(session.customerDeviceId.ifEmpty { session.userIdentifier }, newSsid, newPass)
                    if (res.isSuccess) {
                        Toast.makeText(requireContext(), "Pengaturan WiFi berhasil dikirim ke modem!", Toast.LENGTH_LONG).show()
                        binding.tvCustWifiSsid.text = newSsid
                        binding.tvCustWifiPass.text = newPass
                    } else {
                        Toast.makeText(requireContext(), "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
