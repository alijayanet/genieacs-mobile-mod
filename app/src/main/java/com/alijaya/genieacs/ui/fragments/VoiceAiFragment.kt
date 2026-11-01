package com.alijaya.genieacs.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alijaya.genieacs.data.SessionManager
import com.alijaya.genieacs.databinding.FragmentVoiceAiBinding
import com.alijaya.genieacs.network.ApiService
import com.alijaya.genieacs.ui.adapters.ChatMessage
import com.alijaya.genieacs.ui.adapters.VoiceChatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

class VoiceAiFragment : Fragment() {

    private var _binding: FragmentVoiceAiBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var api: ApiService
    private lateinit var chatAdapter: VoiceChatAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var currentQueryJob: Job? = null
    private var isThinkingOrSpeaking = false

    // SPEECH RECOGNITION LAUNCHER (VOICE INPUT VIA MIC)
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim()
            if (!spoken.isNullOrEmpty()) {
                val cleanCommand = normalizeSpeechIntent(spoken)
                processUserQuestion(cleanCommand)
            }
        }
    }

    // AUDIO RECORD PERMISSION LAUNCHER
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "Izin mikrofon diperlukan untuk mendengarkan perintah suara", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceAiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        api = ApiService(session)

        initTtsFallback()
        setupChatRecyclerView()
        setupListeners()
    }

    private fun initTtsFallback() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
                isTtsReady = true
            }
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = VoiceChatAdapter()
        binding.rvVoiceChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvVoiceChat.adapter = chatAdapter

        // Salam awal
        val greeting = if (session.userRole == SessionManager.ROLE_ADMIN) {
            "Halo Admin! Saya Asisten AI GenieACS dengan pengenalan suara dinamis. Anda bisa langsung bicara seperti \"cek redaman\", \"berapa modem mati\", atau ketuk tombol pintasan di bawah."
        } else {
            "Halo! Saya asisten pintar WiFi GenieACS. Ketuk bola mikrofon untuk bertanya cara ganti sandi WiFi, cek kondisi sinyal, atau restart modem."
        }
        chatAdapter.addMessage(ChatMessage("Asisten AI", greeting, false))
    }

    /**
     * Normalisasi fonetik cerdas untuk mengoreksi salah tangkap suara mikrofon
     * (misal: "chat" -> "cek", "rekaman" -> "redaman", "model" -> "modem", "ribut" -> "reboot")
     */
    private fun normalizeSpeechIntent(text: String): String {
        var t = text.trim()
        val fixes = listOf(
            // Cek / Lihat
            Regex("(?i)\\b(chat|chek|ceck|cekkan|lihatkan|tengok|tolong cek|tolong lihat)\\b") to "cek",
            // Redaman (sering terdengar "rekaman", "pedoman", "redam", "rendaman")
            Regex("(?i)\\b(rekaman|pedoman|redam|rendaman|rekeman|rekaman optik|rekaman sinyal)\\b") to "redaman",
            // Modem / ONU / ONT (sering terdengar "model", "modern", "mode")
            Regex("(?i)\\b(model|modern|mode|moge|odem|odm|onom)\\b") to "modem",
            // Reboot / Restart (sering terdengar "ribut", "ribot", "rebot")
            Regex("(?i)\\b(ribut|ribot|rebot|re-boot|re start|restat|restrt)\\b") to "reboot",
            // Sinyal
            Regex("(?i)\\b(signal|singal|sinyalnya)\\b") to "sinyal",
            // Mati / Offline
            Regex("(?i)\\b(padam|putus|off line|ofline|terputus)\\b") to "mati",
            // Kritis / Buruk
            Regex("(?i)\\b(krisis|parah|hancur|anjlok)\\b") to "kritis",
            // WiFi
            Regex("(?i)\\b(waifi|waepai|wai fai|wify|wi fi)\\b") to "wifi",
            // PPPoE
            Regex("(?i)\\b(pipoe|pipo e|pipo|pipoet|pepowe)\\b") to "pppoe",
            // Berapa
            Regex("(?i)\\b(brp|brapa|ada berapa)\\b") to "berapa"
        )
        for ((pattern, repl) in fixes) {
            t = t.replace(pattern, repl)
        }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    private fun setupListeners() {
        // Klik Orb Mikrofon Tengah (Aktifkan Suara / Interupsi)
        binding.btnVoiceOrb.setOnClickListener {
            vibrate(30)
            checkAndStartVoiceRecognition()
        }

        // Tahan Orb (Buka Pilihan Perintah Cepat)
        binding.btnVoiceOrb.setOnLongClickListener {
            vibrate(40)
            showVoicePromptDialog()
            true
        }

        // Tombol Mic Kecil di Input Bar Bawah
        binding.btnMicInput.setOnClickListener {
            vibrate(25)
            checkAndStartVoiceRecognition()
        }

        // Tombol Interupsi Suara
        binding.btnInterruptVoice.setOnClickListener {
            vibrate(35)
            interruptAndReset()
        }

        // Quick Suggestion Chips (1-Tap Perintah Cepat)
        binding.chipRxKritis.setOnClickListener {
            vibrate(20)
            processUserQuestion("cek redaman kritis")
        }

        binding.chipModemMati.setOnClickListener {
            vibrate(20)
            processUserQuestion("berapa modem yang mati hari ini")
        }

        binding.chipStatusModem.setOnClickListener {
            vibrate(20)
            processUserQuestion("berapa total modem dan statusnya")
        }

        binding.chipModemBaru.setOnClickListener {
            vibrate(20)
            processUserQuestion("cek modem baru")
        }

        // Kirim teks manual
        binding.btnSendVoiceText.setOnClickListener {
            val q = binding.etVoiceText.text.toString().trim()
            if (q.isNotEmpty()) {
                binding.etVoiceText.setText("")
                val cleanCommand = normalizeSpeechIntent(q)
                processUserQuestion(cleanCommand)
            }
        }
    }

    private fun checkAndStartVoiceRecognition() {
        if (isThinkingOrSpeaking) {
            interruptAndReset()
            return
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecognition() {
        vibrate(30)
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan ucapkan perintah suara GenieACS Anda...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Layanan suara tidak tersedia, silakan pilih perintah...", Toast.LENGTH_SHORT).show()
            showVoicePromptDialog()
        }
    }

    private fun showVoicePromptDialog() {
        val options = if (session.userRole == SessionManager.ROLE_ADMIN) {
            arrayOf(
                "Cek redaman kritis",
                "Ada berapa modem yang mati hari ini",
                "Cek status modem sumi",
                "Ketik perintah lain..."
            )
        } else {
            arrayOf(
                "Bagaimana status koneksi modem saya?",
                "Apakah sinyal optik saya stabil?",
                "Tolong bantu restart modem saya",
                "Ketik pertanyaan lain..."
            )
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Pilih Contoh Perintah:")
            .setItems(options) { _, which ->
                if (which == options.size - 1) {
                    binding.etVoiceText.requestFocus()
                } else {
                    processUserQuestion(options[which])
                }
            }
            .show()
    }

    private fun processUserQuestion(question: String) {
        chatAdapter.addMessage(ChatMessage("Anda", question, true))
        binding.rvVoiceChat.scrollToPosition(chatAdapter.itemCount - 1)

        setSpeakingState(true, "Memproses data server...")

        currentQueryJob = lifecycleScope.launch {
            val res = api.askVoiceAssistant(question)
            if (res.isSuccess) {
                val pair = res.getOrNull()!!
                val replyText = pair.first
                val speechText = pair.second

                chatAdapter.addMessage(ChatMessage("Asisten AI", replyText, false))
                binding.rvVoiceChat.scrollToPosition(chatAdapter.itemCount - 1)

                speakResponse(speechText)
            } else {
                val err = "Gagal memproses: " + (res.exceptionOrNull()?.message ?: "Koneksi terputus")
                chatAdapter.addMessage(ChatMessage("Sistem", err, false))
                binding.rvVoiceChat.scrollToPosition(chatAdapter.itemCount - 1)
                setSpeakingState(false, "Ketuk bola mikrofon untuk bicara")
            }
        }
    }

    /**
     * Memainkan suara Neural Indonesia (id-ID-GadisNeural) via server streaming
     */
    private fun speakResponse(text: String) {
        if (text.isEmpty()) {
            setSpeakingState(false, "Ketuk bola mikrofon untuk bicara")
            return
        }

        stopAudioPlayback()
        setSpeakingState(true, "Menjawab dengan suara neural...")

        val base = session.serverUrl.trimEnd('/')
        val ttsPath = if (base.contains("server.alijaya.com")) {
            "$base/bot-proxy/api/voice-assistant/tts"
        } else {
            "$base/api/voice-assistant/tts"
        }

        val enc = URLEncoder.encode(text, "UTF-8")
        val audioUrl = "$ttsPath?text=$enc&voice=id-ID-GadisNeural"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build()
                )
                player.setDataSource(audioUrl)
                player.setOnPreparedListener { mp ->
                    mp.start()
                }
                player.setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                    activity?.runOnUiThread {
                        setSpeakingState(false, "Ketuk bola mikrofon untuk bicara")
                    }
                }
                player.setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    activity?.runOnUiThread {
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "genieacs_utt")
                    }
                    true
                }
                player.prepareAsync()
                mediaPlayer = player
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "genieacs_utt")
                }
            }
        }
    }

    private fun stopAudioPlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    private fun interruptAndReset() {
        currentQueryJob?.cancel()
        currentQueryJob = null
        stopAudioPlayback()
        setSpeakingState(false, "Jawaban dihentikan. Ketuk bola mikrofon untuk bicara baru.")
        Toast.makeText(requireContext(), "Jawaban dihentikan", Toast.LENGTH_SHORT).show()
    }

    private fun setSpeakingState(active: Boolean, statusMsg: String) {
        isThinkingOrSpeaking = active
        binding.tvOrbStatus.text = statusMsg
        binding.btnInterruptVoice.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun vibrate(ms: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms.coerceIn(10, 500), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms.coerceIn(10, 500))
            }
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        stopAudioPlayback()
        tts?.shutdown()
        _binding = null
        super.onDestroyView()
    }
}
