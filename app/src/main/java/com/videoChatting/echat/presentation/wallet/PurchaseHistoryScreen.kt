package com.videoChatting.echat.presentation.wallet

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.components.TalksyCoinIcon
import com.videoChatting.echat.presentation.theme.CyberMidnight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val history      by viewModel.purchaseHistory.collectAsState()
    val isLoading    by viewModel.isHistoryLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchPurchaseHistory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberMidnight)
            )
        },
        containerColor = CyberMidnight
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    // Skeleton Loader
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(5) { SkeletonRow() }
                    }
                }
                history.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        Arrangement.Center,
                        Alignment.CenterHorizontally
                    ) {
                        Text("🪙", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No purchases yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Your coin purchase history will appear here.", fontSize = 13.sp, color = Color.White.copy(0.55f))
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history) { tx ->
                            TransactionRow(tx) {
                                navController.navigate("invoice_detail/${tx.orderId}")
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionItem, onClick: () -> Unit) {
    val statusColor = when (tx.status.uppercase()) {
        "SUCCESS" -> Color(0xFF10B981)
        "PENDING" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val statusIcon = when (tx.status.uppercase()) {
        "SUCCESS" -> "✅"
        "PENDING" -> "⏳"
        else -> "❌"
    }
    val productLabel = productLabel(tx.productId, tx.coins)
    val formattedDate = formatDate(tx.createdAt)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16122E),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(14.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Coin icon
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1E143E).copy(0.8f)), Alignment.Center) {
                    TalksyCoinIcon(size = 28.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(productLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Status chip
                        Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(0.15f)) {
                            Text(
                                "$statusIcon ${tx.status.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("•", color = Color.White.copy(0.3f), fontSize = 10.sp)
                        Text(formattedDate, fontSize = 11.sp, color = Color.White.copy(0.5f))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${tx.amount}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+${tx.coins} 🪙", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF16122E), modifier = Modifier.fillMaxWidth().height(70.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(0.05f)))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(0.07f)))
                Box(Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(0.05f)))
            }
        }
    }
}

fun productLabel(productId: String?, coins: Int): String {
    return when (productId) {
        "talksy_coins_50"   -> "50 Coins · Starter Pack"
        "talksy_coins_100"  -> "100 Coins · Popular Pack"
        "talksy_coins_260"  -> "260 Coins · Best Value Pack"
        "talksy_coins_550"  -> "550 Coins · Super Saver Pack"
        "talksy_coins_2000" -> "2000 Coins · VIP Mega Pack"
        else                -> "$coins Talksy Coins"
    }
}

fun formatDate(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoString) ?: Date()
        val outputFormat = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString.take(10)
    }
}
