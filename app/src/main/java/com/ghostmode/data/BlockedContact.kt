package com.ghostmode.data

data class BlockedContact(
    val phoneNumber: String,
    val displayName: String,
    val messageIndex: Int = 0
)
