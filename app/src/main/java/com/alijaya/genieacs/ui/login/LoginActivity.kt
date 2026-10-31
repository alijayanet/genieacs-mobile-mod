package com.alijaya.genieacs.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.ActivityLoginBinding
import com.alijaya.genieacs.databinding.DialogServerConfigBinding
import com.alijaya.genieacs.network.ApiService
import com.alijaya.genieacs.ui.main.MainActivity
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        api = ApiService(session)

        // Cek jika sudah pernah login sebelumnya
        if (session.isLoggedIn) {
            navigateToMain()
            return
        }

        updateServerLabel()

        setupTabs()
        setupListeners()
    }

    private fun updateServerLabel() {
        binding.tvServerLabel.text = "🌐 " + session.serverUrl
    }

    private fun setupTabs() {
        binding.tabRole.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    // Pelanggan
                    binding.layoutCustomerLogin.visibility = View.VISIBLE
                    binding.layoutAdminLogin.visibility = View.GONE
                } else {
                    // Admin
                    binding.layoutCustomerLogin.visibility = View.GONE
                    binding.layoutAdminLogin.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        // Login Pelanggan
        binding.btnCustomerLogin.setOnClickListener {
            val phone = binding.etCustomerPhone.text.toString().trim()
            if (phone.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan nomor WhatsApp Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                val res = api.loginCustomerByPhone(phone)
                setLoading(false)
                if (res.isSuccess) {
                    Toast.makeText(this@LoginActivity, "Selamat datang!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Login Admin
        binding.btnAdminLogin.setOnClickListener {
            val u = binding.etAdminUsername.text.toString().trim()
            val p = binding.etAdminPassword.text.toString().trim()
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Username dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                val res = api.loginAdmin(u, p)
                setLoading(false)
                if (res.isSuccess) {
                    Toast.makeText(this@LoginActivity, "Login Admin Berhasil!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Ganti Server Config Dialog
        binding.btnServerConfig.setOnClickListener {
            showServerConfigDialog()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCustomerLogin.isEnabled = !loading
        binding.btnAdminLogin.isEnabled = !loading
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
                updateServerLabel()
                Toast.makeText(this, "Server berhasil diubah", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
