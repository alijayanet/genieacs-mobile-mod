package com.alijaya.genieacs.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.alijaya.genieacs.R
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.ActivityMainBinding
import com.alijaya.genieacs.databinding.DialogServerConfigBinding
import com.alijaya.genieacs.data.DeviceSummary
import com.alijaya.genieacs.ui.fragments.AdminDashboardFragment
import com.alijaya.genieacs.ui.fragments.CustomerPortalFragment
import com.alijaya.genieacs.ui.fragments.DeviceDetailBottomSheet
import com.alijaya.genieacs.ui.fragments.DevicesListFragment
import com.alijaya.genieacs.ui.fragments.MapFragment
import com.alijaya.genieacs.ui.fragments.VoiceAiFragment
import com.alijaya.genieacs.ui.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        if (!session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        updateTopBar()
        setupNavigation()

        // Load initial fragment
        if (savedInstanceState == null) {
            if (session.userRole == SessionManager.ROLE_ADMIN) {
                replaceFragment(AdminDashboardFragment())
            } else {
                replaceFragment(CustomerPortalFragment())
            }
        }
    }

    private fun updateTopBar() {
        val server = session.serverUrl
        val pillText = when {
            server.contains("server.alijaya.com") -> "STB 89"
            server.contains("185") -> "STB 185"
            else -> "Server"
        }
        binding.tvServerPill.text = "🟢 $pillText"

        val title = if (session.userRole == SessionManager.ROLE_ADMIN) {
            "Admin GenieACS"
        } else {
            "Portal Pelanggan"
        }
        binding.tvAppTitle.text = title

        binding.btnTopAction.setOnClickListener {
            showAccountOptionsDialog()
        }

        binding.tvServerPill.setOnClickListener {
            showServerConfigDialog()
        }
    }

    private fun setupNavigation() {
        if (session.userRole == SessionManager.ROLE_ADMIN) {
            binding.bottomNavigation.menu.clear()
            binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_admin)
            binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_dashboard -> {
                        replaceFragment(AdminDashboardFragment())
                        true
                    }
                    R.id.nav_devices -> {
                        replaceFragment(DevicesListFragment())
                        true
                    }
                    R.id.nav_map -> {
                        replaceFragment(MapFragment())
                        true
                    }
                    R.id.nav_voice_ai -> {
                        replaceFragment(VoiceAiFragment())
                        true
                    }
                    R.id.nav_profile -> {
                        showAccountOptionsDialog()
                        false
                    }
                    else -> false
                }
            }
        } else {
            binding.bottomNavigation.menu.clear()
            binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_customer)
            binding.bottomNavigation.selectedItemId = R.id.nav_cust_home
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_cust_home -> {
                        replaceFragment(CustomerPortalFragment())
                        true
                    }
                    R.id.nav_cust_wifi -> {
                        replaceFragment(CustomerPortalFragment())
                        true
                    }
                    R.id.nav_cust_voice -> {
                        replaceFragment(VoiceAiFragment())
                        true
                    }
                    R.id.nav_cust_profile -> {
                        showAccountOptionsDialog()
                        false
                    }
                    else -> false
                }
            }
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun openDeviceDetail(device: DeviceSummary) {
        val sheet = DeviceDetailBottomSheet(device)
        sheet.show(supportFragmentManager, DeviceDetailBottomSheet.TAG)
    }

    fun openDeviceDetail(deviceId: String) {
        val sheet = DeviceDetailBottomSheet(
            DeviceSummary(
                id = deviceId,
                serialNumber = deviceId.substringAfterLast("-", deviceId),
                modelName = "ONU Modem",
                customerName = "Pelanggan $deviceId",
                pppoeUsername = deviceId.lowercase(),
                rxPower = "-21.4 dBm",
                rxPowerVal = -21.4,
                isOnline = true,
                ipAddress = "-",
                wifiSsid = "ALIJAYA-$deviceId",
                wifiPass = "alijaya2026"
            )
        )
        sheet.show(supportFragmentManager, DeviceDetailBottomSheet.TAG)
    }

    fun openDeviceListWithFilter(filter: String) {
        val frag = DevicesListFragment()
        replaceFragment(frag)
        binding.bottomNavigation.selectedItemId = R.id.nav_devices
        frag.view?.postDelayed({
            frag.applyFilter(filter)
        }, 300)
    }

    private fun showAccountOptionsDialog() {
        val userLabel = if (session.userRole == SessionManager.ROLE_ADMIN) {
            "Akun: Admin (${session.userIdentifier})"
        } else {
            "Akun Pelanggan: ${session.userIdentifier}"
        }

        val items = arrayOf(userLabel, "Ganti Server GenieACS", "Keluar (Logout)")

        AlertDialog.Builder(this)
            .setTitle("Pengaturan Akun & Server")
            .setItems(items) { _, which ->
                when (which) {
                    1 -> showServerConfigDialog()
                    2 -> {
                        session.logout()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showServerConfigDialog() {
        val dBinding = DialogServerConfigBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dBinding.etServerUrl.setText(session.serverUrl)

        dBinding.btnPreset89.setOnClickListener {
            dBinding.etServerUrl.setText("https://server.alijaya.com")
        }
        dBinding.btnPreset185.setOnClickListener {
            dBinding.etServerUrl.setText("http://192.168.8.185:3000")
        }
        dBinding.btnPresetBilling.setOnClickListener {
            dBinding.etServerUrl.setText("http://192.168.8.89:3000")
        }

        dBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dBinding.btnSave.setOnClickListener {
            val url = dBinding.etServerUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                session.serverUrl = url
                updateTopBar()
                Toast.makeText(this, "Server berhasil diubah", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
