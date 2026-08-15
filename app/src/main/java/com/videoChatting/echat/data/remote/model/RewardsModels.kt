package com.videoChatting.echat.data.remote.model

data class RewardsStatusResponse(
    val success: Boolean,
    val streak: Int,
    val canCheckInToday: Boolean,
    val canSpinToday: Boolean,
    val nextSpinTimeMs: Long,
    val coinsBalance: Int,
    val checkInRewards: List<Int>
)

data class CheckInResponse(
    val success: Boolean,
    val coinsEarned: Int,
    val newStreak: Int,
    val coinsBalance: Int,
    val message: String?,
    val error: String?
)

data class SpinResponse(
    val success: Boolean,
    val prizeIndex: Int,
    val coinsWon: Int,
    val label: String?,
    val coinsBalance: Int,
    val nextSpinTimeMs: Long,
    val error: String?
)
