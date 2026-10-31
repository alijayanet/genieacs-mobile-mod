package com.alijaya.genieacs.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.alijaya.genieacs.data.DeviceSummary
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.DialogDeviceDetailBinding
import com.alijaya.genieacs.network.ApiService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class DeviceDetailBottomSheet(
    private val dev: DeviceSummary,
    private val onDeviceUpdated: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: DialogDeviceDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var api: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeviceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        api = ApiService(session)

        populateData()
        setupActions()
    }

    private fun populateData() {
        binding.tvDetailTitle.text = dev.customerName
        binding.tvDetailSubtitle.text = "PPPoE: ${dev.pppoeUsername} • SN: ${dev.serialNumber} • ${dev.modelName}"

        // Online / Offline Status
        if (dev.isOnline) {
            binding.tvDetailStatusBadge.text = "Online"
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#16A34A"))
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
        } else {
            binding.tvDetailStatusBadge.text = "Offline"
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#DC2626"))
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
        }

        // Rx Power
        binding.tvDetailRx.text = dev.rxPower
        val rx = dev.rxPowerVal ?: -22.0
        if (rx <= -27.0) {
            binding.tvDetailRx.setTextColor(Color.parseColor("#DC2626"))
        } else if (rx <= -24.0) {
            binding.tvDetailRx.setTextColor(Color.parseColor("#D97706"))
        } else {
            binding.tvDetailRx.setTextColor(Color.parseColor("#16A34A"))
        }

        // IP, ODP, Phone
        binding.tvDetailIp.text = if (dev.ipAddress.isNotEmpty()) dev.ipAddress else "-"
        binding.tvDetailOdp.text = if (dev.odpName.isNotEmpty()) dev.odpName else "Belum dipetakan"
        binding.tvDetailPhone.text = if (dev.customerPhone.isNotEmpty()) dev.customerPhone else "Tidak ada nomor"

        // WiFi Fields
        binding.etDetailWifiSsid.setText(dev.wifiSsid)
        binding.etDetailWifiPass.setText(dev.wifiPass)
    }

    private fun setupActions() {
        // Chat WA
        binding.btnDetailChatWa.setOnClickListener {
            val phone = dev.customerPhone.replace(Regex("[^0-9]"), "")
            if (phone.isNotEmpty()) {
                val cleanPhone = if (phone.startsWith("08")) "628" + phone.substring(2) else phone
                try {
                    val uri = Uri.parse("https://wa.me/$cleanPhone?text=Halo%20${dev.customerName}%2C%20terkait%20koneksi%20WiFi%20AliJaya%20Net")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "WhatsApp tidak terpasang", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Nomor HP pelanggan belum terdaftar", Toast.LENGTH_SHORT).show()
            }
        }

        // Simpan SSID Saja
        binding.btnDetailSaveSsid.setOnClickListener {
            val newSsid = binding.etDetailWifiSsid.text.toString().trim()
            if (newSsid.isEmpty()) {
                Toast.makeText(requireContext(), "Nama WiFi (SSID) tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Ubah Nama WiFi (SSID)")
                .setMessage("Terapkan SSID baru `$newSsid` ke modem ${dev.pppoeUsername} via TR-069?")
                .setPositiveButton("Ya, Simpan") { _, _ ->
                    binding.btnDetailSaveSsid.isEnabled = false
                    binding.btnDetailSaveSsid.text = "Mengirim..."
                    lifecycleScope.launch {
                        val res = api.updateWifi(dev.id, ssid = newSsid, pass = null)
                        binding.btnDetailSaveSsid.isEnabled = true
                        binding.btnDetailSaveSsid.text = "Simpan SSID"
                        if (res.isSuccess) {
                            Toast.makeText(requireContext(), res.getOrNull() ?: "SSID berhasil diperbarui!", Toast.LENGTH_LONG).show()
                            onDeviceUpdated?.invoke()
                        } else {
                            Toast.makeText(requireContext(), "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Simpan Sandi Saja
        binding.btnDetailSavePass.setOnClickListener {
            val newPass = binding.etDetailWifiPass.text.toString().trim()
            if (newPass.length < 8) {
                Toast.makeText(requireContext(), "Sandi WiFi minimal 8 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Ubah Sandi WiFi (Password)")
                .setMessage("Terapkan Sandi baru `$newPass` ke modem ${dev.pppoeUsername} via TR-069?")
                .setPositiveButton("Ya, Simpan") { _, _ ->
                    binding.btnDetailSavePass.isEnabled = false
                    binding.btnDetailSavePass.text = "Mengirim..."
                    lifecycleScope.launch {
                        val res = api.updateWifi(dev.id, ssid = null, pass = newPass)
                        binding.btnDetailSavePass.isEnabled = true
                        binding.btnDetailSavePass.text = "Simpan Sandi"
                        if (res.isSuccess) {
                            Toast.makeText(requireContext(), res.getOrNull() ?: "Sandi WiFi berhasil diperbarui!", Toast.LENGTH_LONG).show()
                            onDeviceUpdated?.invoke()
                        } else {
                            Toast.makeText(requireContext(), "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Reboot Modem
        binding.btnDetailReboot.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Restart Modem ONU")
                .setMessage("Kirim sinyal reboot TR-069 ke modem ${dev.pppoeUsername} (${dev.serialNumber})?")
                .setPositiveButton("Ya, Restart") { _, _ ->
                    lifecycleScope.launch {
                        Toast.makeText(requireContext(), "Mengirim instruksi restart...", Toast.LENGTH_SHORT).show()
                        val res = api.rebootDevice(dev.id)
                        if (res.isSuccess) {
                            Toast.makeText(requireContext(), res.getOrNull() ?: "Perintah restart berhasil dikirim!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Refresh Parameter
        binding.btnDetailRefresh.setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(requireContext(), "Mengirim perintah refreshObject ke modem...", Toast.LENGTH_SHORT).show()
                val res = api.refreshDevice(dev.id)
                if (res.isSuccess) {
                    Toast.makeText(requireContext(), res.getOrNull() ?: "Modem berhasil di-summon!", Toast.LENGTH_LONG).show()
                    onDeviceUpdated?.invoke()
                } else {
                    Toast.makeText(requireContext(), "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DeviceDetailBottomSheet"
    }
}
