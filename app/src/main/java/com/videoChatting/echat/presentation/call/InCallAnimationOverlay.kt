package com.videoChatting.echat.presentation.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun InCallAnimationOverlay(
    activeGiftEvent: GiftReceivedEvent?,
    floatingEmojis: List<FloatingEmojiItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Floating Emojis Layer
        floatingEmojis.forEach { item ->
            key(item.id) {
                FloatingEmojiParticle(item = item)
            }
        }

        // 2. Full-Screen Gift Celebration Popup
        activeGiftEvent?.let { gift ->
            key(gift.timestamp) {
                GiftCelebrationBanner(gift = gift)
            }
        }
    }
}

@Composable
fun FloatingEmojiParticle(item: FloatingEmojiItem) {
    val transition = rememberInfiniteTransition(label = "wobble")
    val wobble by transition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobbleOffset"
    )

    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(item.id) {
        val startTime = System.currentTimeMillis()
        val duration = 2400f
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed / duration).coerceIn(0f, 1f)
            delay(16)
        }
    }

    if (progress < 1f) {
        val yOffset = (1f - progress) * 500f // Floats upward from bottom
        val alpha = (1f - (progress * 1.1f)).coerceIn(0f, 1f)
        val scale = 0.8f + (progress * 0.4f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp, end = 30.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = item.emoji,
                fontSize = 32.sp,
                modifier = Modifier
                    .offset(
                        x = (wobble + (item.startXPercent * 60f)).dp,
                        y = (-yOffset).dp
                    )
                    .scale(scale)
                    .alpha(alpha)
            )
        }
    }
}

@Composable
fun GiftCelebrationBanner(gift: GiftReceivedEvent) {
    var visible by remember { mutableStateOf(true) }

    // Auto dismiss after 3.2 seconds
    LaunchedEffect(gift.timestamp) {
        delay(3200)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(400, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(400)) + scaleOut(tween(300))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEE1E1B4B),
                            Color(0xEE0F172A)
                        )
                    )
                )
                .border(2.dp, Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF6584))), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            // Glowing Giant Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x66FFD700),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gift.icon,
                    fontSize = 52.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${gift.senderName} sent you",
                fontSize = 14.sp,
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = gift.giftName,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Coins Bonus Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E1065))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🪙", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+${gift.coins} Coins",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}
