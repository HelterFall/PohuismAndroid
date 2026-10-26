package com.example.pohuism

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.activity_main)

        val contacts = listOf(
            Contact("Павел Дуров", "ПД", Color.rgb(45, 105, 200), R.drawable.avatar_durov),
            Contact("Влад А4", "А4", Color.rgb(130, 75, 220), R.drawable.avatar_a4),
            Contact("Мистер Бист (MrBeast)", "МБ", Color.rgb(235, 145, 45), R.drawable.avatar_mrbeast),
            Contact("Шаман (SHAMAN)", "Ш", Color.rgb(35, 155, 115), R.drawable.avatar_shaman)
        )

        findViewById<RecyclerView>(R.id.contactsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = ContactAdapter(contacts) { contact ->
                startActivity(
                    Intent(this@MainActivity, ChatActivity::class.java)
                        .putExtra(ChatActivity.EXTRA_CONTACT_NAME, contact.name)
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }
}
