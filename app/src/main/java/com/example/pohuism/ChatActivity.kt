package com.example.pohuism

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : Activity() {
    companion object {
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        private const val REPLY_TEXT = "Иди нахуй"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
            .orEmpty()
            .ifBlank { "Чат" }

        findViewById<TextView>(R.id.chatTitle).text = contactName
        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        val sendButton: Button = findViewById(R.id.sendButton)

        messageAdapter = MessageAdapter(mutableListOf())
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messagesRecyclerView.adapter = messageAdapter

        sendButton.setOnClickListener { sendMessage() }
        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString()
        if (text.isBlank()) return

        addMessage(ChatMessage(text = text, isUser = true))
        messageInput.text.clear()

        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                addMessage(ChatMessage(text = REPLY_TEXT, isUser = false))
            }
        }, 1_000L)
    }

    private fun addMessage(message: ChatMessage) {
        messageAdapter.addMessage(message)
        messagesRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
