package com.example.pohuism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class MessageAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_STAR = 2
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].isUser) TYPE_USER else TYPE_STAR

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == TYPE_USER) {
            R.layout.item_message_user
        } else {
            R.layout.item_message_star
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.messageText.text = messages[position].text
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.lastIndex)
    }
}
