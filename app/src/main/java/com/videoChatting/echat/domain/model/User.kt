package com.videoChatting.echat.domain.model

data class User(
    val userId: String = "",
    val name: String = "",
    val bio: String = "",
    val profileImage: String = "",
    val gender: String = "Any",
    val country: String = "Global",
    val isOnline: Boolean = false,
    val coins: Int = 0
)
