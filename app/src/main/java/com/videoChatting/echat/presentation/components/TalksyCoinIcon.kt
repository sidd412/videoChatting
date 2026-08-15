package com.videoChatting.echat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TalksyCoinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val fontSize = (size.value * 0.55f).sp
    val borderWidth = if (size > 30.dp) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape, spotColor = Color(0xFFFFD700), ambientColor = Color(0xFFF59E0B))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFE082),
                        Color(0xFFFFB300),
                        Color(0xFFF57C00)
                    )
                )
            )
            .border(
                borderWidth,
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFF9C4),
                        Color(0xFFFFD54F),
                        Color(0xFFFF8F00)
                    )
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner subtle ring
        Box(
            modifier = Modifier
                .size(size * 0.8f)
                .clip(CircleShape)
                .border(0.5.dp, Color(0xFFFFF59D).copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "T",
                color = Color(0xFF5D4037),
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
