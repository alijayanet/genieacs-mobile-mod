package com.alijaya.genieacs.ui.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.FragmentMapBinding

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())

        setupWebView()
        setupListeners()
        loadMap()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webViewMap
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setGeolocationEnabled(true)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 100) {
                    binding.swipeRefreshMap.isRefreshing = false
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.swipeRefreshMap.isRefreshing = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.swipeRefreshMap.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                binding.swipeRefreshMap.isRefreshing = false
                if (request?.isForMainFrame == true) {
                    val fallback = getFallbackMapUrl()
                    if (view?.url != fallback) {
                        view?.loadUrl(fallback)
                    }
                }
            }
        }
    }

    private fun getMapUrl(): String {
        val base = session.serverUrl.trimEnd('/')
        return if (base.contains("server.alijaya.com")) {
            "$base/bot-proxy/peta"
        } else {
            "$base/peta"
        }
    }

    private fun getFallbackMapUrl(): String {
        val base = session.serverUrl.trimEnd('/')
        return if (base.contains("bot-proxy")) {
            base.replace("/bot-proxy", "") + "/peta"
        } else {
            "$base/bot-proxy/peta"
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshMap.setOnRefreshListener {
            binding.webViewMap.reload()
        }

        binding.fabRefreshMap.setOnClickListener {
            binding.webViewMap.reload()
            Toast.makeText(requireContext(), "Memuat ulang peta ODP...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMap() {
        val url = getMapUrl()
        binding.swipeRefreshMap.isRefreshing = true
        binding.webViewMap.loadUrl(url)
    }

    override fun onDestroyView() {
        binding.webViewMap.stopLoading()
        _binding = null
        super.onDestroyView()
    }
}
