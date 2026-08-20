package com.videoChatting.echat.presentation.wallet

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.components.TalksyCoinIcon
import com.videoChatting.echat.presentation.theme.*

data class CoinPack(
    val productId: String,
    val coins: Int,
    val priceInInr: Int,
    val badge: String? = null,
    val bonusCoins: String? = null
)

val coinPacks = listOf(
    CoinPack("talksy_coins_50", 50, 10, badge = "STARTER"),
    CoinPack("talksy_coins_100", 100, 20, badge = "POPULAR"),
    CoinPack("talksy_coins_260", 260, 49, badge = "🔥 BEST VALUE", bonusCoins = "+10 FREE"),
    CoinPack("talksy_coins_550", 550, 99, badge = "⚡ SUPER SAVER", bonusCoins = "+50 FREE"),
    CoinPack("talksy_coins_2000", 2000, 149, badge = "💎 VIP PACK", bonusCoins = "+1000 FREE")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val currentCoins by viewModel.currentCoins.collectAsState()
    val isVerifyingPurchase by viewModel.isVerifyingPurchase.collectAsState()
    val purchaseSuccessEvent by viewModel.purchaseSuccessEvent.collectAsState()
    val paymentMessage by viewModel.paymentMessage.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()
    val productDetailsMap by viewModel.playBillingManager.productDetailsMap.collectAsState()

    val context = LocalContext.current
    var showDailyRewardsSheet by remember { mutableStateOf(false) }

    // Fallback if paymentUrl exists
    if (paymentUrl != null) {
        PaymentWebViewScreen(
            paymentUrl = paymentUrl!!,
            onPaymentComplete = { success -> viewModel.onPaymentComplete(success) },
            onBack = { viewModel.dismissPaymentWebView() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Wallet & Rewards", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberMidnight
                )
            )
        },
        containerColor = CyberMidnight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CyberMidnight),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Balance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E143E),
                                    Color(0xFF2B1B66),
                                    Color(0xFF140D2B)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF7C3AED).copy(alpha = 0.8f),
                                    Color(0xFFFFD700).copy(alpha = 0.6f),
                                    Color(0xFF38BDF8).copy(alpha = 0.3f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT COIN BALANCE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TalksyCoinIcon(size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = currentCoins.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF3B1E78).copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "10 Coins = 1 Minute of HD Video Call",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Free Rewards Section Title
            item {
                Text(
                    text = "Earn Free Coins",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 3. Rewards Cards: Daily Bonus & Lucky Spin + Contacts & Invite
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Daily Bonus & Lucky Spin Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF3B1470),
                                        Color(0xFF5B21B6)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .clickable { showDailyRewardsSheet = true }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF240E48)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎡", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Daily Bonus & Lucky Spin",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Claim daily streak + spin wheel!",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Claim ✨",
                                    color = Color(0xFF261C4E),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Contacts & Invite Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF064E3B),
                                        Color(0xFF047857)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .clickable { navController.navigate("invite_and_contacts") }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF032D23)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎁", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Contacts & Invite",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Earn +50 coins for every friend!",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Invite →",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Recharge Packs Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recharge Coins",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Google Play ⚡ 1-Tap Pay",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (paymentMessage.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7F1D1D),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = paymentMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 5. Coin Packs List with Google Play Billing
            items(coinPacks) { pack ->
                val playDetails = productDetailsMap[pack.productId]
                val formattedPrice = playDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "₹${pack.priceInInr}"

                PremiumCoinPackRow(
                    pack = pack,
                    formattedPrice = formattedPrice,
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            viewModel.purchaseWithGooglePlay(activity, pack.productId)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- FULL-SCREEN SECURE VERIFICATION OVERLAY ---
    if (isVerifyingPurchase) {
        BackHandler(enabled = true) { /* Prevent back button during verification */ }

        Dialog(
            onDismissRequest = { /* Non-cancellable */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B071E).copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD700),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(100.dp)
                        )
                        TalksyCoinIcon(
                            size = 54.dp,
                            modifier = Modifier.scale(pulseScale)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Securing Transaction...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Verifying with Google Play & crediting coins to your wallet. Please don't close the app.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // --- CELEBRATORY SUCCESS POPUP DIALOG ---
    purchaseSuccessEvent?.let { event ->
        Dialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E143E),
                                Color(0xFF140D2B)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFF7C3AED),
                                Color(0xFF10B981)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glowing Icon Header
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        TalksyCoinIcon(size = 58.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🎉 Payment Successful!",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF065F46).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "+${event.coinsAdded} Coins Added",
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "New Balance: ${event.newBalance} Coins",
                        color = Color(0xFFFFD700),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.dismissSuccessDialog() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Awesome! 🚀",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showDailyRewardsSheet) {
        com.videoChatting.echat.presentation.rewards.DailyRewardsBottomSheet(
            onDismiss = { showDailyRewardsSheet = false }
        )
    }
}

@Composable
fun PremiumCoinPackRow(
    pack: CoinPack,
    formattedPrice: String,
    onClick: () -> Unit
) {
    val isFeatured = pack.badge != null && pack.badge.contains("BEST VALUE")
    val borderColor = if (isFeatured) Color(0xFFFFD700).copy(alpha = 0.7f) else Color(0xFF38BDF8).copy(alpha = 0.25f)
    val cardBackground = if (isFeatured) Color(0xFF1E173D) else Color(0xFF16122E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBackground)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                TalksyCoinIcon(size = 36.dp)

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${pack.coins} Coins",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (pack.bonusCoins != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF059669)
                            ) {
                                Text(
                                    text = pack.bonusCoins,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (pack.badge != null) {
                        Text(
                            text = pack.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFeatured) Color(0xFFFFD700) else Color(0xFF38BDF8)
                        )
                    } else {
                        Text(
                            text = "${pack.coins / 10} Mins Video Time",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Price Button
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = formattedPrice,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
