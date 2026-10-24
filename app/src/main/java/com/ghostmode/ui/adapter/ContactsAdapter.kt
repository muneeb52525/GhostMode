package com.ghostmode.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ghostmode.R
import com.ghostmode.data.BlockedContact

class ContactsAdapter(
    private val contacts: MutableList<BlockedContact>,
    private val onDelete: (BlockedContact) -> Unit,
    private val onMessageChange: (BlockedContact, Int) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val tvNumber: TextView = view.findViewById(R.id.tvContactNumber)
        val spinner: Spinner = view.findViewById(R.id.spinnerContactMessage)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteContact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = contact.displayName
        holder.tvNumber.text = contact.phoneNumber

        val messages = holder.itemView.context.resources
            .getStringArray(R.array.message_options)
        val adapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            messages
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinner.adapter = adapter
        holder.spinner.setSelection(contact.messageIndex)

        holder.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos != contact.messageIndex) {
                    onMessageChange(contact, pos)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnDelete.setOnClickListener { onDelete(contact) }
    }

    override fun getItemCount() = contacts.size

    fun remove(contact: BlockedContact) {
        val idx = contacts.indexOf(contact)
        if (idx >= 0) {
            contacts.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    fun update(newContacts: List<BlockedContact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    fun addOrUpdate(contact: BlockedContact) {
        val idx = contacts.indexOfFirst { it.phoneNumber == contact.phoneNumber }
        if (idx >= 0) {
            contacts[idx] = contact
            notifyItemChanged(idx)
        } else {
            contacts.add(contact)
            notifyItemInserted(contacts.size - 1)
        }
    }
}
