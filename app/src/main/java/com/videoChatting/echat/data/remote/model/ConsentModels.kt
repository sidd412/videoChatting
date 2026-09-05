package com.videoChatting.echat.data.remote.model

data class ConsentNotificationDto(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val status: String,
    val timestamp: Long
)

data class ConsentNotificationsResponse(
    val notifications: List<ConsentNotificationDto>
)

data class RespondConsentRequest(
    val consentId: String,
    val action: String // "allow" or "deny"
)

data class RespondConsentResponse(
    val success: Boolean,
    val message: String
)

data class RevokeConsentRequest(
    val userId: String,
    val targetUserId: String
)
