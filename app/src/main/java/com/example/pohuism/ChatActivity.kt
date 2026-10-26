package com.example.pohuism

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
    private lateinit var customKeyboardPanel: LinearLayout
    private lateinit var keyboardRows: LinearLayout

    private var isKeyboardVisible = false
    private var isEnglishLayout = false
    private var isSymbolsLayout = false
    private var isShifted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.activity_chat)

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )

        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
            .orEmpty()
            .ifBlank { "Чат" }

        findViewById<TextView>(R.id.chatTitle).text = contactName
        findViewById<TextView>(R.id.backButton).setOnClickListener {
            if (isKeyboardVisible) hideCustomKeyboard() else finish()
        }

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        customKeyboardPanel = findViewById(R.id.customKeyboardPanel)
        keyboardRows = findViewById(R.id.keyboardRows)
        val sendButton: Button = findViewById(R.id.sendButton)

        // Keep the native input cursor and selection, but never open Android's IME.
        messageInput.setShowSoftInputOnFocus(false)
        messageInput.setOnClickListener { showCustomKeyboard() }

        messageAdapter = MessageAdapter(mutableListOf())
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messagesRecyclerView.adapter = messageAdapter

        sendButton.setOnClickListener { sendMessage() }
        renderCustomKeyboard()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun showCustomKeyboard() {
        isKeyboardVisible = true
        customKeyboardPanel.visibility = View.VISIBLE
        renderCustomKeyboard()
    }

    private fun hideCustomKeyboard() {
        isKeyboardVisible = false
        customKeyboardPanel.visibility = View.GONE
    }

    private fun renderCustomKeyboard() {
        if (!::keyboardRows.isInitialized) return
        keyboardRows.removeAllViews()

        val topRow = if (isSymbolsLayout) {
            listOf("1234567890", "@#\$%&*-+=", "()[]{}")
        } else if (isEnglishLayout) {
            listOf("qwertyuiop", "asdfghjkl")
        } else {
            listOf("йцукенгшщзхъ", "фывапролджэ")
        }
        topRow.forEach { buildCharacterRow(it) }

        val thirdRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        if (!isSymbolsLayout) {
            addKey(thirdRow, "⇧", weight = 1.25f, special = true) {
                isShifted = !isShifted
                renderCustomKeyboard()
            }
            val thirdLetters = if (isEnglishLayout) "zxcvbnm" else "ячсмитьбю"
            thirdLetters.forEach { character ->
                val shown = if (isShifted) character.uppercaseChar().toString()
                    else character.toString()
                addKey(thirdRow, shown) {
                    insertText(shown)
                    if (isShifted) {
                        isShifted = false
                        renderCustomKeyboard()
                    }
                }
            }
        } else {
            listOf("/", ":", ";", "!", "?", "'", "\"").forEach { character ->
                addKey(thirdRow, character.toString()) { insertText(character.toString()) }
            }
        }

        addKey(thirdRow, "⌫", weight = 1.35f, special = true) { deleteBeforeCursor() }
        keyboardRows.addView(thirdRow)

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addKey(
            bottomRow,
            if (isSymbolsLayout) "ABC" else "?123",
            weight = 1.25f,
            special = true
        ) {
            isSymbolsLayout = !isSymbolsLayout
            isShifted = false
            renderCustomKeyboard()
        }

        addKey(
            bottomRow,
            if (isEnglishLayout) "EN" else "RU",
            weight = 0.95f,
            special = true
        ) {
            isEnglishLayout = !isEnglishLayout
            isSymbolsLayout = false
            isShifted = false
            renderCustomKeyboard()
        }

        addKey(bottomRow, ",", weight = 0.65f) { insertText(",") }
        addKey(bottomRow, "☺", weight = 0.8f, special = true) { insertText("🙂") }
        addKey(bottomRow, "Пробел", weight = 3.3f) { insertText(" ") }
        addKey(bottomRow, ".", weight = 0.65f) { insertText(".") }
        addKey(bottomRow, "↵", weight = 0.9f, special = true) { insertText("\n") }

        keyboardRows.addView(bottomRow)
    }

    private fun buildCharacterRow(characters: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        characters.forEach { character ->
            val shown = if (!isSymbolsLayout && isShifted) {
                character.uppercaseChar().toString()
            } else {
                character.toString()
            }
            addKey(row, shown) {
                insertText(shown)
                if (isShifted) {
                    isShifted = false
                    renderCustomKeyboard()
                }
            }
        }

        keyboardRows.addView(row)
    }

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float = 1f,
        special: Boolean = false,
        onClick: () -> Unit
    ) {
        val key = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label.length > 1) 13f else 17f
            setTextColor(0xFF17212B.toInt())
            typeface = if (special) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            isFocusable = false
            isFocusableInTouchMode = false
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(6).toFloat()
                setColor(
                    if (special) 0xFFBEC9D7.toInt()
                    else 0xFFFFFFFF.toInt()
                )
            }
            setOnClickListener { onClick() }
        }

        val params = LinearLayout.LayoutParams(0, dp(40), weight).apply {
            setMargins(dp(2), dp(3), dp(2), dp(3))
        }
        row.addView(key, params)
    }

    private fun insertText(value: String) {
        val editable = messageInput.text
        val selectionStart = messageInput.selectionStart.coerceAtLeast(0)
        val selectionEnd = messageInput.selectionEnd.coerceAtLeast(0)
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, editable.length)
        editable.replace(start, end, value)
        messageInput.setSelection(start + value.length)
    }

    private fun deleteBeforeCursor() {
        val editable = messageInput.text
        val selectionStart = messageInput.selectionStart.coerceAtLeast(0)
        val selectionEnd = messageInput.selectionEnd.coerceAtLeast(0)
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, editable.length)

        if (start != end) {
            editable.delete(start, end)
            messageInput.setSelection(start)
        } else if (start > 0) {
            val deleteStart = Character.offsetByCodePoints(editable, start, -1)
            editable.delete(deleteStart, start)
            messageInput.setSelection(deleteStart)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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

    override fun onBackPressed() {
        if (isKeyboardVisible) {
            hideCustomKeyboard()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
