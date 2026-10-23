package com.ghostmode.ui

import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ghostmode.R
import com.ghostmode.data.BlockedContact
import com.ghostmode.data.Repository
import com.ghostmode.databinding.ActivityMainBinding
import com.ghostmode.ui.adapter.ContactsAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ContactsAdapter

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateDialerStatus() }

    private val contactPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.query(uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                    null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val number = cursor.getString(0) ?: return@use
                        val name = cursor.getString(1) ?: number
                        showAddDialog(number.replace(" ", ""), name)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(this)
        setupList()
        setupButtons()
        setupUnknownSection()
        updateDialerStatus()
    }

    override fun onResume() {
        super.onResume()
        updateDialerStatus()
    }

    private fun setupList() {
        adapter = ContactsAdapter(
            contacts = repository.getBlockedContacts(),
            onDelete = { contact ->
                repository.removeBlockedContact(contact.phoneNumber)
                adapter.remove(contact)
                Toast.makeText(this, "${contact.displayName} unblock ho gaya", Toast.LENGTH_SHORT).show()
            },
            onMessageChange = { contact, idx ->
                val updated = contact.copy(messageIndex = idx)
                repository.addBlockedContact(updated)
                adapter.update(updated)
            }
        )
        binding.recyclerContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerContacts.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnAddFromContacts.setOnClickListener {
            contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }
        binding.btnAddManual.setOnClickListener { showAddDialog(null, null) }
        binding.btnSetDefaultDialer.setOnClickListener { requestDefaultDialer() }
    }

    private fun setupUnknownSection() {
        val msgOptions = resources.getStringArray(R.array.message_options)
        val spinAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, msgOptions)
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUnknownMessage.adapter = spinAdapter
        binding.spinnerUnknownMessage.setSelection(repository.getUnknownMessageIndex())
        binding.switchBlockUnknown.isChecked = repository.isBlockUnknownEnabled()
        binding.spinnerUnknownMessage.isEnabled = repository.isBlockUnknownEnabled()
        binding.switchBlockUnknown.setOnCheckedChangeListener { _, checked ->
            repository.setBlockUnknown(checked)
            binding.spinnerUnknownMessage.isEnabled = checked
        }
        binding.spinnerUnknownMessage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                repository.setUnknownMessageIndex(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun showAddDialog(number: String?, name: String?) {
        val view = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etNumber = view.findViewById<android.widget.EditText>(R.id.etPhoneNumber)
        val etName = view.findViewById<android.widget.EditText>(R.id.etDisplayName)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinnerMessage)
        etNumber.setText(number ?: "")
        etName.setText(name ?: "")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.message_options)).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        AlertDialog.Builder(this)
            .setTitle("Contact Block Karein")
            .setView(view)
            .setPositiveButton("Block Karein") { _, _ ->
                val phone = etNumber.text.toString().trim()
                val displayName = etName.text.toString().trim().ifBlank { phone }
                if (phone.isNotBlank()) {
                    val contact = BlockedContact(phone, displayName, spinner.selectedItemPosition)
                    repository.addBlockedContact(contact)
                    adapter.addOrUpdate(contact)
                    Toast.makeText(this, "\"$displayName\" block ho gaya", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            roleRequest.launch(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName))
        }
    }

    private fun updateDialerStatus() {
        val tm = getSystemService(TelecomManager::class.java)
        val isDefault = tm.defaultDialerPackage == packageName
        if (isDefault) {
            binding.tvDialerStatus.text = "App active hai — Calls intercept ho rahe hain"
            binding.tvDialerStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.btnSetDefaultDialer.isEnabled = false
        } else {
            binding.tvDialerStatus.text = "Pehle Default Dialer set karein — neeche button dabain"
            binding.tvDialerStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            binding.btnSetDefaultDialer.isEnabled = true
        }
    }
}
