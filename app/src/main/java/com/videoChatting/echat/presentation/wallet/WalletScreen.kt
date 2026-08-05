package com.videoChatting.echat.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.theme.*

data class CoinPack(
    val coins: Int,
    val priceInInr: Int,
    val isPopular: Boolean = false
)

val coinPacks = listOf(
    CoinPack(50, 10),
    CoinPack(100, 20),
    CoinPack(260, 49, true),
    CoinPack(550, 99),
    CoinPack(2000, 149)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val currentCoins by viewModel.currentCoins.collectAsState()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsState()
    val paymentMessage by viewModel.paymentMessage.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()

    val sdkPayload by viewModel.sdkPayload.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showGuestWarning by remember { mutableStateOf(false) }
    var selectedPackForGuest by remember { mutableStateOf<CoinPack?>(null) }

    LaunchedEffect(sdkPayload) {
        sdkPayload?.let { payload ->
            val activity = context as? android.app.Activity
            if (activity != null) {
                RazorpayPaymentResultHelper.onPaymentSuccess = { _, _ ->
                    viewModel.onPaymentComplete(true)
                    RazorpayPaymentResultHelper.clear()
                }
                RazorpayPaymentResultHelper.onPaymentError = { _, _, _ ->
                    viewModel.onPaymentComplete(false)
                    RazorpayPaymentResultHelper.clear()
                }

                try {
                    com.razorpay.Checkout.preload(context.applicationContext)
                    val checkout = com.razorpay.Checkout()
                    val keyId = payload["key"] as? String
                    if (keyId != null) {
                        checkout.setKeyID(keyId)
                    }

                    val options = org.json.JSONObject()
                    for (entry in payload.entries) {
                        val key = entry.key
                        val value = entry.value
                        if (value is Map<*, *>) {
                            options.put(key, org.json.JSONObject(value))
                        } else {
                            options.put(key, value)
                        }
                    }

                    checkout.open(activity, options)
                } catch (e: Exception) {
                    viewModel.onPaymentComplete(false)
                    RazorpayPaymentResultHelper.clear()
                }
            } else {
                viewModel.dismissRazorpayCheckout()
            }
        }
    }

    // Keep WebView as fallback if paymentUrl is somehow loaded
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
                title = { Text("Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Current Balance",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = CoinGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentCoins.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "10 Coins = 1 Minute of Video Call",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Text(
                text = "Buy Coins",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (paymentMessage.isNotEmpty()) {
                Text(
                    text = paymentMessage,
                    color = if (paymentMessage.contains("Success")) Color.Green else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(coinPacks) { pack ->
                    CoinPackItem(
                        pack = pack,
                        isLoading = isProcessingPayment,
                        onClick = {
                            if (viewModel.isGuestUser()) {
                                selectedPackForGuest = pack
                                showGuestWarning = true
                            } else {
                                viewModel.initiatePayment(pack)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showGuestWarning && selectedPackForGuest != null) {
        var isChecked by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showGuestWarning = false
                selectedPackForGuest = null
            },
            title = {
                Text(
                    text = "Temporary Guest Account Warning",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = "You are currently logged in as a Guest. Guest accounts are temporary. If you logout, delete the app, or clear your phone's data, your purchased coins will be permanently lost.\n\nWe highly recommend signing in with Google to secure your balance.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isChecked = !isChecked }
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I understand the risk and want to proceed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestWarning = false
                        selectedPackForGuest?.let { viewModel.initiatePayment(it) }
                        selectedPackForGuest = null
                    },
                    enabled = isChecked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        contentColor = Color.White
                    )
                ) {
                    Text("Proceed Payment")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGuestWarning = false
                        selectedPackForGuest = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CoinPackItem(pack: CoinPack, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = CoinGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${pack.coins} Coins",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (pack.isPopular) {
                        Text(
                            text = "Popular Choice",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Button(
                onClick = onClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color.White
                )
            ) {
                Text("₹${pack.priceInInr}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
