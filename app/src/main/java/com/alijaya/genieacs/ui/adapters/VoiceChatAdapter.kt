package com.alijaya.genieacs.ui.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.alijaya.genieacs.R
import com.alijaya.genieacs.databinding.ItemVoiceChatBinding

data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean
)

class VoiceChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf()
) : RecyclerView.Adapter<VoiceChatAdapter.ViewHolder>() {

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVoiceChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class ViewHolder(private val binding: ItemVoiceChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvChatSender.text = msg.sender
            binding.tvChatMessage.text = msg.text

            val params = binding.layoutChatContainer.layoutParams as? ViewGroup.MarginLayoutParams
            val senderParams = binding.tvChatSender.layoutParams as? LinearLayout.LayoutParams
            val messageParams = binding.tvChatMessage.layoutParams as? LinearLayout.LayoutParams

            if (msg.isUser) {
                // Pesan User (Rata Kanan, Balon Biru Muda, Teks Gelap Tegas)
                binding.layoutChatContainer.gravity = Gravity.END
                senderParams?.gravity = Gravity.END
                messageParams?.gravity = Gravity.END
                binding.tvChatSender.setTextColor(Color.parseColor("#0284C7"))
                binding.tvChatMessage.setBackgroundResource(R.drawable.bg_chat_user)
                binding.tvChatMessage.setTextColor(Color.parseColor("#0F172A"))
            } else {
                // Pesan AI (Rata Kiri, Balon Putih Bersih, Teks Gelap Tegas)
                binding.layoutChatContainer.gravity = Gravity.START
                senderParams?.gravity = Gravity.START
                messageParams?.gravity = Gravity.START
                binding.tvChatSender.setTextColor(Color.parseColor("#9C1D4A"))
                binding.tvChatMessage.setBackgroundResource(R.drawable.bg_chat_ai)
                binding.tvChatMessage.setTextColor(Color.parseColor("#0F172A"))
            }

            binding.tvChatSender.layoutParams = senderParams
            binding.tvChatMessage.layoutParams = messageParams
        }
    }
}
