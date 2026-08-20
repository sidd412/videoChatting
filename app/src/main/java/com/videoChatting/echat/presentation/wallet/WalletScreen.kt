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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
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

// ---------- Data ----------

data class CoinPack(
    val productId: String,
    val coins: Int,
    val priceInInr: Int,
    val badge: String? = null,
    val bonusCoins: String? = null
)

val coinPacks = listOf(
    CoinPack("talksy_coins_50",   50,   10, badge = "STARTER"),
    CoinPack("talksy_coins_100",  100,  20, badge = "POPULAR"),
    CoinPack("talksy_coins_260",  260,  49, badge = "🔥 BEST VALUE",  bonusCoins = "+10 FREE"),
    CoinPack("talksy_coins_550",  550,  99, badge = "⚡ SUPER SAVER", bonusCoins = "+50 FREE"),
    CoinPack("talksy_coins_2000", 2000, 149, badge = "💎 VIP PACK",   bonusCoins = "+1000 FREE")
)

// ---------- Screen ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentCoins        by viewModel.currentCoins.collectAsState()
    val isVerifying         by viewModel.isVerifyingPurchase.collectAsState()
    val successEvent        by viewModel.purchaseSuccessEvent.collectAsState()
    val failureEvent        by viewModel.purchaseFailureEvent.collectAsState()
    val showCancel          by viewModel.showCancelSnackbar.collectAsState()
    val productDetailsMap   by viewModel.playBillingManager.productDetailsMap.collectAsState()
    val paymentUrl          by viewModel.paymentUrl.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show cancel snackbar
    LaunchedEffect(showCancel) {
        if (showCancel) snackbarHostState.showSnackbar("Purchase canceled")
    }

    // Fallback WebView
    if (paymentUrl != null) {
        PaymentWebViewScreen(
            paymentUrl = paymentUrl!!,
            onPaymentComplete = { viewModel.onPaymentComplete(it) },
            onBack = { viewModel.dismissPaymentWebView() }
        )
        return
    }

    var showDailyRewardsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet & Rewards", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberMidnight)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    containerColor = Color(0xFF2D2D44),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) { Text(data.visuals.message, fontWeight = FontWeight.Medium) }
            }
        },
        containerColor = CyberMidnight
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(CyberMidnight),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Balance Card
            item { HeroBalanceCard(currentCoins) }

            // 2. Earn Free Coins
            item { SectionTitle("Earn Free Coins") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DailyBonusCard(onClick = { showDailyRewardsSheet = true })
                    ContactsInviteCard(onClick = { navController.navigate("invite_and_contacts") })
                }
            }

            // 3. Recharge Section
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    SectionTitle("Recharge Coins")
                    Text("Google Play ⚡ 1-Tap Pay", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Medium)
                }
            }

            // 4. Coin Packs
            items(coinPacks) { pack ->
                val formatted = productDetailsMap[pack.productId]
                    ?.oneTimePurchaseOfferDetails?.formattedPrice ?: "₹${pack.priceInInr}"
                PremiumCoinPackRow(pack, formatted) {
                    (context as? Activity)?.let { viewModel.purchaseWithGooglePlay(it, pack.productId) }
                }
            }

            // 5. Purchase History Link
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16122E),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate("purchase_history") }
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧾", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Purchase History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("View receipts & download invoices", fontSize = 11.sp, color = Color.White.copy(0.55f))
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF38BDF8))
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── Full-Screen Verification Overlay ────────────────────────────────
    if (isVerifying) {
        BackHandler(true) { /* block back */ }
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
        ) {
            val inf = rememberInfiniteTransition(label = "p")
            val scale by inf.animateFloat(0.9f, 1.1f,
                infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")

            Box(Modifier.fillMaxSize().background(Color(0xFF0B071E).copy(0.95f)), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Box(Modifier.size(110.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFD700), strokeWidth = 3.dp, modifier = Modifier.size(100.dp))
                        TalksyCoinIcon(size = 54.dp, modifier = Modifier.scale(scale))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Securing Transaction...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Verifying with Google Play & crediting coins.\nPlease don't close the app.",
                        color = Color.White.copy(0.7f), fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // ── Success Dialog ────────────────────────────────────────────────
    successEvent?.let { evt ->
        Dialog(onDismissRequest = { viewModel.dismissSuccessDialog() }) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E143E), Color(0xFF140D2B))))
                    .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFF7C3AED), Color(0xFF10B981))), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFD700).copy(0.3f), Color.Transparent))), Alignment.Center) {
                        TalksyCoinIcon(size = 58.dp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("🎉 Payment Successful!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF065F46).copy(0.4f), border = BorderStroke(1.dp, Color(0xFF10B981).copy(0.6f))) {
                        Text("+${evt.coinsAdded} Coins Added", color = Color(0xFF34D399), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("New Balance: ${evt.newBalance} Coins", color = Color(0xFFFFD700), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.dismissSuccessDialog() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Awesome! 🚀", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                }
            }
        }
    }

    // ── Failure Dialog ────────────────────────────────────────────────
    failureEvent?.let { evt ->
        val activity = context as? Activity
        Dialog(onDismissRequest = { viewModel.dismissFailureDialog() }) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2D0A0A), Color(0xFF1A0707))))
                    .border(1.5.dp, Color(0xFFEF4444).copy(0.5f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon
                    Box(Modifier.size(70.dp).clip(CircleShape).background(Color(0xFF7F1D1D).copy(0.5f)), Alignment.Center) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Payment Failed", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Text(evt.message, color = Color.White.copy(0.8f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Spacer(Modifier.height(24.dp))

                    if (evt.isRetryable) {
                        Button(
                            onClick = { if (activity != null) viewModel.retryPurchase(activity) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Try Again", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { viewModel.dismissFailureDialog() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel", color = Color.White.copy(0.6f))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.dismissFailureDialog() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contact Support", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { viewModel.dismissFailureDialog() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Dismiss", color = Color.White.copy(0.6f))
                        }
                    }
                }
            }
        }
    }

    if (showDailyRewardsSheet) {
        com.videoChatting.echat.presentation.rewards.DailyRewardsBottomSheet(onDismiss = { showDailyRewardsSheet = false })
    }
}

// ---------- Sub-Composables ----------

@Composable
private fun HeroBalanceCard(coins: Int) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E143E), Color(0xFF2B1B66), Color(0xFF140D2B))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF7C3AED).copy(0.8f), Color(0xFFFFD700).copy(0.6f), Color(0xFF38BDF8).copy(0.3f))), RoundedCornerShape(24.dp))
            .padding(24.dp),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CURRENT COIN BALANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color.White.copy(0.6f))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TalksyCoinIcon(size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Text(coins.toString(), fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF3B1E78).copy(0.6f), border = BorderStroke(1.dp, Color(0xFFFFD700).copy(0.3f))) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("10 Coins = 1 Minute of HD Video Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD700))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun DailyBonusCard(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF3B1470), Color(0xFF5B21B6))))
            .border(1.dp, Color(0xFFA78BFA).copy(0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF240E48)), contentAlignment = Alignment.Center) { Text("🎡", fontSize = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Daily Bonus & Lucky Spin", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Claim daily streak + spin wheel!", fontSize = 11.sp, color = Color.White.copy(0.75f))
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFD700), modifier = Modifier.padding(start = 8.dp)) {
                Text("Claim ✨", color = Color(0xFF261C4E), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ContactsInviteCard(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF064E3B), Color(0xFF047857))))
            .border(1.dp, Color(0xFF34D399).copy(0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF032D23)), Alignment.Center) { Text("🎁", fontSize = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Contacts & Invite", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Earn +50 coins for every friend!", fontSize = 11.sp, color = Color.White.copy(0.75f))
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF10B981), modifier = Modifier.padding(start = 8.dp)) {
                Text("Invite →", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
fun PremiumCoinPackRow(pack: CoinPack, formattedPrice: String, onClick: () -> Unit) {
    val isFeatured = pack.badge?.contains("BEST VALUE") == true
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (isFeatured) Color(0xFF1E173D) else Color(0xFF16122E))
            .border(1.dp, if (isFeatured) Color(0xFFFFD700).copy(0.7f) else Color(0xFF38BDF8).copy(0.25f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                TalksyCoinIcon(size = 36.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${pack.coins} Coins", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        if (pack.bonusCoins != null) {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF059669)) {
                                Text(pack.bonusCoins, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                    if (pack.badge != null)
                        Text(pack.badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isFeatured) Color(0xFFFFD700) else Color(0xFF38BDF8))
                    else
                        Text("${pack.coins / 10} Mins Video Time", fontSize = 11.sp, color = Color.White.copy(0.5f))
                }
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) { Text(formattedPrice, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
        }
    }
}
