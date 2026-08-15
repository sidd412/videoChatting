package com.videoChatting.echat.presentation.call

import androidx.compose.ui.graphics.Color

data class GiftItem(
    val id: String,
    val name: String,
    val icon: String,
    val coinCost: Int,
    val gradientColors: List<Color>,
    val description: String
)

data class GiftReceivedEvent(
    val senderId: String,
    val senderName: String,
    val giftId: String,
    val giftName: String,
    val coins: Int,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FloatingEmojiItem(
    val id: Long = System.nanoTime(),
    val emoji: String,
    val startXPercent: Float, // 0.0f to 1.0f across screen width
    val createdAt: Long = System.currentTimeMillis()
)

object GiftCatalog {
    val AVAILABLE_GIFTS = listOf(
        GiftItem(
            id = "gift_rose",
            name = "Red Rose",
            icon = "🌹",
            coinCost = 10,
            gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            description = "A classic gesture of appreciation"
        ),
        GiftItem(
            id = "gift_chocolate",
            name = "Chocolates",
            icon = "🍫",
            coinCost = 20,
            gradientColors = listOf(Color(0xFF8B4513), Color(0xFFD2691E)),
            description = "Sweet treat to spark the chat"
        ),
        GiftItem(
            id = "gift_diamond",
            name = "Diamond",
            icon = "💎",
            coinCost = 50,
            gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
            description = "Sparkling crystal luxury"
        ),
        GiftItem(
            id = "gift_crown",
            name = "Golden Crown",
            icon = "👑",
            coinCost = 100,
            gradientColors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
            description = "For the royalty of the room"
        ),
        GiftItem(
            id = "gift_rocket",
            name = "Space Rocket",
            icon = "🚀",
            coinCost = 200,
            gradientColors = listOf(Color(0xFF7F00FF), Color(0xFFE100FF)),
            description = "Take the vibe to the moon!"
        ),
        GiftItem(
            id = "gift_sports_car",
            name = "Supercar",
            icon = "🏎️",
            coinCost = 500,
            gradientColors = listOf(Color(0xFFFF0844), Color(0xFFFFB199)),
            description = "Ultimate luxury grand entrance"
        )
    )

    val QUICK_REACTIONS = listOf("❤️", "🔥", "😂", "👏", "🥳", "😍")
}
