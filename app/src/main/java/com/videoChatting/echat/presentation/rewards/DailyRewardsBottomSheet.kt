package com.videoChatting.echat.presentation.rewards

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videoChatting.echat.presentation.theme.ElectricIndigo
import kotlinx.coroutines.launch

private val WHEEL_COLORS = listOf(
    Color(0xFFFF5252),
    Color(0xFF7C4DFF),
    Color(0xFF00E676),
    Color(0xFFFFD700),
    Color(0xFF00B0FF),
    Color(0xFFFF4081),
    Color(0xFFFF6D00),
    Color(0xFF536DFE)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRewardsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val wonPrize by viewModel.wonPrize.collectAsState()
    val checkInMessage by viewModel.checkInMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val wheelRotation = remember { Animatable(0f) }

    LaunchedEffect(checkInMessage) {
        val msg = checkInMessage
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissCheckInMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        val err = errorMessage
        if (err != null) {
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            viewModel.dismissError()
        }
    }

    val onSpinClick: () -> Unit = {
        viewModel.spinWheel { targetAngle, prize ->
            scope.launch {
                val currentAngle = wheelRotation.value
                wheelRotation.animateTo(
                    targetValue = currentAngle + targetAngle,
                    animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
                )
                viewModel.onSpinAnimationFinished(prize)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp),
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(2.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Switcher Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(
                    title = "📅 7-Day Streak",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    title = "🎡 Lucky Spin",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                // Tab 1: 7-Day Check-in Streak
                val currentStreak = status?.streak ?: 0
                val canClaimToday = status?.canCheckInToday ?: true
                val rewards = status?.checkInRewards ?: listOf(10, 20, 30, 40, 50, 75, 100)

                Text(
                    text = "Daily Login Streak 🔥",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = if (currentStreak > 0) "You are on a $currentStreak-Day Streak! Keep it going!" else "Log in daily to earn free coins and perks!",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 7 Days Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(rewards) { index, coins ->
                        val dayNumber = index + 1
                        val isPast = dayNumber < currentStreak || (dayNumber == currentStreak && !canClaimToday)
                        val isToday = dayNumber == (if (canClaimToday) (currentStreak % 7) + 1 else -1)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        isPast -> Color(0xFF1E293B)
                                        isToday -> Color(0xFF2E1065)
                                        else -> Color(0xFF151D2C)
                                    }
                                )
                                .border(
                                    width = if (isToday) 2.dp else 1.dp,
                                    color = when {
                                        isToday -> Color(0xFFFFD700)
                                        isPast -> Color(0xFF00E676)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Day $dayNumber",
                                    fontSize = 11.sp,
                                    color = if (isToday) Color(0xFFFFD700) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (dayNumber == 7) "👑" else "🪙",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+$coins",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPast) Color(0xFF00E676) else Color.White
                                )
                                if (isPast) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Claimed",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Claim Button
                Button(
                    onClick = { viewModel.claimCheckIn() },
                    enabled = canClaimToday,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Text(
                        text = if (canClaimToday) "Claim Today's Bonus 🎁" else "Claimed Today! Come Back Tomorrow ✓",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canClaimToday) Color.White else Color(0xFF64748B)
                    )
                }

            } else {
                // Tab 2: Lucky Spin Wheel
                val canSpinToday = status?.canSpinToday ?: true

                Text(
                    text = "Lucky Spin Wheel 🎡",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Spin every 24 hours to win free coin jackpots!",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // The Spinning Wheel Canvas + Center Pointer
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(wheelRotation.value)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val radius = canvasWidth / 2f
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val segmentAngle = 360f / 8f

                        for (i in 0 until 8) {
                            drawArc(
                                color = WHEEL_COLORS[i],
                                startAngle = i * segmentAngle,
                                sweepAngle = segmentAngle,
                                useCenter = true,
                                topLeft = Offset(0f, 0f),
                                size = Size(canvasWidth, canvasHeight)
                            )
                        }

                        // Outer glowing ring
                        drawCircle(
                            color = Color(0xFFFFD700),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 8f)
                        )
                    }

                    // Center Hub Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(3.dp, Color(0xFFFFD700), CircleShape)
                            .clickable(enabled = canSpinToday && !isSpinning) {
                                onSpinClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSpinning) "..." else "SPIN",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFFFFD700)
                        )
                    }

                    // Top Pointer Arrow
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-6).dp)
                    ) {
                        Text("🔻", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Spin Button
                Button(
                    onClick = { onSpinClick() },
                    enabled = canSpinToday && !isSpinning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    if (isSpinning) {
                        Text("Spinning the wheel...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else if (canSpinToday) {
                        Text("🎰 Spin the Wheel (FREE)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("Spun Today! Come back in 24 hrs", fontSize = 14.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }

    // Winner Prize Celebration Dialog
    val prize = wonPrize
    if (prize != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPrizeDialog() },
            title = {
                Text(
                    text = "🎉 JACKPOT! You Won!",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🪙", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${prize.coinsWon} Coins Added!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "New Balance: ${prize.coinsBalance} Coins",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissPrizeDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Collect & Enjoy! 🚀", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1B4B),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ElectricIndigo else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}
