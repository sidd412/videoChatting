package com.videoChatting.echat.data.remote.model

import com.google.gson.annotations.SerializedName

data class GuestLoginRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("fcmToken") val fcmToken: String? = null
)

data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String,
    @SerializedName("fcmToken") val fcmToken: String? = null
)

data class UserPreferencesDto(
    @SerializedName("gender") val gender: String? = "All",
    @SerializedName("minAge") val minAge: Int? = 18,
    @SerializedName("maxAge") val maxAge: Int? = 99,
    @SerializedName("filterType") val filterType: String? = "country",
    @SerializedName("kmRadius") val kmRadius: Int? = 50
)

data class UserDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("gender") val gender: String? = "Not Specified",
    @SerializedName("age") val age: Int? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("contactNumber") val contactNumber: String? = null,
    @SerializedName("coinsBalance") val coinsBalance: Int? = 100,
    @SerializedName("preferences") val preferences: UserPreferencesDto? = UserPreferencesDto(),
    @SerializedName("country") val country: String = "Global"
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class UpdateProfileRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("contactNumber") val contactNumber: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("prefGender") val prefGender: String? = null,
    @SerializedName("prefMinAge") val prefMinAge: Int? = null,
    @SerializedName("prefMaxAge") val prefMaxAge: Int? = null,
    @SerializedName("filterType") val filterType: String? = null,
    @SerializedName("kmRadius") val kmRadius: Int? = null
)

data class UserProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("profile") val profile: UserProfileDto
)

data class UserProfileDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("contactNumber") val contactNumber: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("lastSeen") val lastSeen: Long,
    @SerializedName("coinsBalance") val coinsBalance: Int = 100
)

data class MessageDto(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("chatId") val chatId: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("readStatus") val readStatus: Boolean? = false
)

data class MessagesHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("messages") val messages: List<MessageDto>
)

data class ToggleInteractionRequest(
    @SerializedName("targetUserId") val targetUserId: String,
    @SerializedName("interactionType") val interactionType: String,
    @SerializedName("isActive") val isActive: Boolean
)

data class InteractionUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatar: String = "",
    @SerializedName("lastMessage") val lastMessage: String,
    @SerializedName("time") val time: String,
    @SerializedName("timestamp") val timestamp: Long? = null,
    @SerializedName("lastMessageSenderId") val lastMessageSenderId: String? = null,
    @SerializedName("lastMessageReadStatus") val lastMessageReadStatus: Boolean? = null,
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("isLiked") val isLiked: Boolean,
    @SerializedName("isAdded") val isAdded: Boolean
)

data class InteractionsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("interactions") val interactions: List<InteractionUserDto>
)

data class VerifyPlayPurchaseRequest(
    @SerializedName("purchaseToken") val purchaseToken: String,
    @SerializedName("productId") val productId: String,
    @SerializedName("orderId") val orderId: String? = null,
    @SerializedName("packageName") val packageName: String? = null
)

data class VerifyPlayPurchaseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("coinsBalance") val coinsBalance: Int? = null
)

