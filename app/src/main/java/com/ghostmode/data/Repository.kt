package com.ghostmode.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class Repository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghostmode_prefs", Context.MODE_PRIVATE)

    fun getBlockedContacts(): MutableList<BlockedContact> {
        val json = prefs.getString("blocked_contacts", "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<BlockedContact>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(BlockedContact(
                phoneNumber = obj.getString("phoneNumber"),
                displayName = obj.getString("displayName"),
                messageIndex = obj.optInt("messageIndex", 0)
            ))
        }
        return list
    }

    fun addBlockedContact(contact: BlockedContact) {
        val contacts = getBlockedContacts()
        val existing = contacts.indexOfFirst { it.phoneNumber == contact.phoneNumber }
        if (existing >= 0) contacts[existing] = contact else contacts.add(contact)
        saveContacts(contacts)
    }

    fun removeBlockedContact(phoneNumber: String) {
        val contacts = getBlockedContacts()
        contacts.removeAll { it.phoneNumber == phoneNumber }
        saveContacts(contacts)
    }

    private fun saveContacts(contacts: List<BlockedContact>) {
        val array = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject()
            obj.put("phoneNumber", contact.phoneNumber)
            obj.put("displayName", contact.displayName)
            obj.put("messageIndex", contact.messageIndex)
            array.put(obj)
        }
        prefs.edit().putString("blocked_contacts", array.toString()).apply()
    }

    fun isBlockUnknownEnabled(): Boolean = prefs.getBoolean("block_unknown", false)
    fun setBlockUnknown(enabled: Boolean) { prefs.edit().putBoolean("block_unknown", enabled).apply() }
    fun getUnknownMessageIndex(): Int = prefs.getInt("unknown_msg_index", 0)
    fun setUnknownMessageIndex(index: Int) { prefs.edit().putInt("unknown_msg_index", index).apply() }
}
