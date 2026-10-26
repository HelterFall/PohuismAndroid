package com.example.pohuism

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Contact(
    val name: String,
    val initials: String,
    val color: Int,
    val avatarResId: Int? = null
)

class ContactAdapter(
    private val contacts: List<Contact>,
    private val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatarText)
        val avatarImage: ImageView = view.findViewById(R.id.avatarImage)
        val name: TextView = view.findViewById(R.id.contactName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name

        val avatarResId = contact.avatarResId
        if (avatarResId != null) {
            holder.avatarImage.setImageResource(avatarResId)
            holder.avatarImage.visibility = View.VISIBLE
            holder.avatar.visibility = View.GONE
        } else {
            holder.avatar.text = contact.initials
            holder.avatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(contact.color)
            }
            holder.avatarImage.visibility = View.GONE
            holder.avatar.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onClick(contact) }
    }

    override fun getItemCount(): Int = contacts.size
}
