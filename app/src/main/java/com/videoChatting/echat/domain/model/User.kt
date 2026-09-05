package com.videoChatting.echat.domain.model

data class User(
    val userId: String = "",
    val name: String = "",
    val bio: String = "",
    val profileImage: String = "",
    val gender: String = "Not Specified",
    val country: String = "Global",
    val isOnline: Boolean = false,
    val availableMinutes: Int = 30
)
