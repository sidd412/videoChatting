package com.videoChatting.echat.data.remote.model

data class ReferralInfoResponse(
    val success: Boolean,
    val referralCode: String,
    val referralCount: Int,
    val coinsEarned: Int,
    val bonusPerReferral: Int,
    val hasClaimedReferral: Boolean,
    val blockContactsMatching: Boolean
)

data class SyncContactsRequest(
    val phoneNumbers: List<String>
)

data class RegisteredContactDto(
    val userId: String,
    val name: String,
    val avatar: String,
    val isOnline: Boolean,
    val gender: String,
    val country: String,
    val contactNumber: String?
)

data class SyncContactsResponse(
    val success: Boolean,
    val registeredContacts: List<RegisteredContactDto>,
    val matchedCount: Int
)

data class ClaimReferralRequest(
    val code: String
)

data class ClaimReferralResponse(
    val success: Boolean,
    val message: String,
    val bonusCoins: Int?,
    val newBalance: Int?,
    val referrerName: String?
)

data class ToggleContactsPrivacyRequest(
    val blockContactsMatching: Boolean
)

data class ToggleContactsPrivacyResponse(
    val success: Boolean,
    val message: String,
    val blockContactsMatching: Boolean
)

// Local device contact model
data class DeviceContact(
    val name: String,
    val phoneNumber: String,
    val isRegistered: Boolean = false,
    val registeredUser: RegisteredContactDto? = null
)
