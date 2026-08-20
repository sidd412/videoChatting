package com.videoChatting.echat.presentation.wallet

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.billing.PlayBillingManager
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.SocketManager
import com.videoChatting.echat.data.remote.model.VerifyPlayPurchaseRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------- Shared Data Classes ----------

data class CreateOrderRequest(val amount: Int, val coins: Int)
data class CreateOrderResponse(
    val success: Boolean,
    val orderId: String,
    val paymentLink: String?,
    val sdkPayload: Map<String, Any>?
)
data class VerifyPaymentResponse(val success: Boolean, val status: String, val message: String?)

data class TransactionItem(
    val orderId: String,
    val productId: String? = null,
    val amount: Int,
    val coins: Int,
    val status: String,
    val createdAt: String
)
data class PurchaseHistoryResponse(val success: Boolean, val transactions: List<TransactionItem>)

/** Fired when a purchase fully succeeds (server-verified). */
data class PurchaseSuccessEvent(val coinsAdded: Int, val newBalance: Int)

/** Fired when billing or server-verification fails. */
data class PurchaseFailureEvent(
    val message: String,
    val isRetryable: Boolean,
    /** Stored so UI can retry the same product */
    val lastProductId: String? = null
)

// ---------- ViewModel ----------

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager,
    val playBillingManager: PlayBillingManager
) : ViewModel() {

    private val _currentCoins = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 100)
    val currentCoins: StateFlow<Int> = _currentCoins

    /** TRUE while we are calling the server to verify a purchase. Blocks back navigation. */
    private val _isVerifyingPurchase = MutableStateFlow(false)
    val isVerifyingPurchase: StateFlow<Boolean> = _isVerifyingPurchase

    /** Non-null → show celebratory success dialog. */
    private val _purchaseSuccessEvent = MutableStateFlow<PurchaseSuccessEvent?>(null)
    val purchaseSuccessEvent: StateFlow<PurchaseSuccessEvent?> = _purchaseSuccessEvent

    /** Non-null → show failure dialog. Contains retry info. */
    private val _purchaseFailureEvent = MutableStateFlow<PurchaseFailureEvent?>(null)
    val purchaseFailureEvent: StateFlow<PurchaseFailureEvent?> = _purchaseFailureEvent

    /** TRUE for ~2 seconds → show "Purchase canceled" snackbar. */
    private val _showCancelSnackbar = MutableStateFlow(false)
    val showCancelSnackbar: StateFlow<Boolean> = _showCancelSnackbar

    private val _purchaseHistory = MutableStateFlow<List<TransactionItem>>(emptyList())
    val purchaseHistory: StateFlow<List<TransactionItem>> = _purchaseHistory

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading

    /** Fallbacks for Razorpay/WebView path (kept for compatibility). */
    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl
    private val _sdkPayload = MutableStateFlow<Map<String, Any>?>(null)
    val sdkPayload: StateFlow<Map<String, Any>?> = _sdkPayload
    private var currentOrderId: String? = null

    /** Remembers the last attempted productId so we can retry. */
    private var lastAttemptedProductId: String? = null

    init {
        playBillingManager.startConnection()
        setupPlayBillingCallbacks()

        viewModelScope.launch {
            socketManager.matchEvents.collect { event ->
                if (event is com.videoChatting.echat.data.remote.SocketEvent.WalletUpdate) {
                    _currentCoins.value = event.coinsBalance
                    sessionManager.updateCoins(event.coinsBalance)
                }
            }
        }
        fetchLatestProfile()
        fetchPurchaseHistory()
    }

    // ---------- Google Play Billing Setup ----------

    private fun setupPlayBillingCallbacks() {
        playBillingManager.onPurchaseCompleted = { purchaseToken, orderId, productId ->
            viewModelScope.launch {
                _isVerifyingPurchase.value = true
                try {
                    val response = apiService.verifyPlayPurchase(
                        VerifyPlayPurchaseRequest(
                            purchaseToken = purchaseToken,
                            productId = productId,
                            orderId = orderId,
                            packageName = "com.videoChatting.echat"
                        )
                    )
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        val coinsAdded = coinsForProductId(productId)
                        val newBal = body.coinsBalance ?: (_currentCoins.value + coinsAdded)
                        _currentCoins.value = newBal
                        sessionManager.updateCoins(newBal)
                        fetchLatestProfile()
                        fetchPurchaseHistory()
                        _purchaseSuccessEvent.value = PurchaseSuccessEvent(coinsAdded, newBal)
                    } else {
                        // Server rejected — non-retryable (token issue)
                        _purchaseFailureEvent.value = PurchaseFailureEvent(
                            message = "Payment verified by Google but our server couldn't process it. Please contact support.",
                            isRetryable = false,
                            lastProductId = productId
                        )
                    }
                } catch (e: Exception) {
                    // Network error — retryable
                    _purchaseFailureEvent.value = PurchaseFailureEvent(
                        message = "Network error while verifying your purchase. Your payment is safe — tap Retry to credit your coins.",
                        isRetryable = true,
                        lastProductId = productId
                    )
                } finally {
                    _isVerifyingPurchase.value = false
                }
            }
        }

        playBillingManager.onPurchaseFailed = { errorMessage ->
            when {
                errorMessage.contains("cancel", ignoreCase = true) ||
                errorMessage.contains("USER_CANCELED", ignoreCase = true) -> {
                    // User dismissed the Play sheet — just show a snackbar, no dialog
                    viewModelScope.launch {
                        _showCancelSnackbar.value = true
                        delay(2500)
                        _showCancelSnackbar.value = false
                    }
                }
                errorMessage.contains("ITEM_ALREADY_OWNED", ignoreCase = true) -> {
                    // Consumable not consumed — rare edge case; tell them to retry
                    _purchaseFailureEvent.value = PurchaseFailureEvent(
                        message = "Previous purchase not yet processed. Please wait a moment and try again.",
                        isRetryable = true,
                        lastProductId = lastAttemptedProductId
                    )
                }
                else -> {
                    _purchaseFailureEvent.value = PurchaseFailureEvent(
                        message = errorMessage.ifBlank { "Payment could not be completed. Please try again." },
                        isRetryable = true,
                        lastProductId = lastAttemptedProductId
                    )
                }
            }
        }
    }

    // ---------- Public Actions ----------

    fun purchaseWithGooglePlay(activity: Activity, productId: String) {
        lastAttemptedProductId = productId
        playBillingManager.launchBillingFlow(activity, productId)
    }

    fun retryPurchase(activity: Activity) {
        val productId = _purchaseFailureEvent.value?.lastProductId ?: return
        _purchaseFailureEvent.value = null
        purchaseWithGooglePlay(activity, productId)
    }

    fun dismissSuccessDialog() { _purchaseSuccessEvent.value = null }
    fun dismissFailureDialog() { _purchaseFailureEvent.value = null }

    fun fetchLatestProfile() {
        viewModelScope.launch {
            try {
                val response = apiService.getUserProfile(sessionManager.getUserProfile()?.userId ?: "")
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!.profile
                    _currentCoins.value = profile.coinsBalance
                    sessionManager.updateCoins(profile.coinsBalance)
                }
            } catch (_: Exception) { }
        }
    }

    fun fetchPurchaseHistory() {
        viewModelScope.launch {
            _isHistoryLoading.value = true
            try {
                val response = apiService.getPurchaseHistory()
                if (response.isSuccessful && response.body() != null) {
                    _purchaseHistory.value = response.body()!!.transactions
                }
            } catch (_: Exception) { } finally {
                _isHistoryLoading.value = false
            }
        }
    }

    fun isGuestUser(): Boolean =
        sessionManager.getUserProfile()?.userId?.startsWith("guest_") == true

    // ---------- Razorpay compat (legacy) ----------
    fun dismissRazorpayCheckout() { _sdkPayload.value = null }
    fun onPaymentComplete(success: Boolean) {
        _sdkPayload.value = null
        _paymentUrl.value = null
        if (success && currentOrderId != null) {
            _isVerifyingPurchase.value = true
            viewModelScope.launch {
                try {
                    val response = apiService.verifyPayment(currentOrderId!!)
                    if (response.isSuccessful && response.body()?.success == true) {
                        fetchLatestProfile()
                        fetchPurchaseHistory()
                        _purchaseSuccessEvent.value = PurchaseSuccessEvent(50, _currentCoins.value)
                    } else {
                        _purchaseFailureEvent.value = PurchaseFailureEvent("Payment Verification Failed", false)
                    }
                } catch (e: Exception) {
                    _purchaseFailureEvent.value = PurchaseFailureEvent("Verification Error: ${e.message}", true)
                } finally {
                    _isVerifyingPurchase.value = false
                }
            }
        }
    }
    fun dismissPaymentWebView() { _paymentUrl.value = null }

    // ---------- Helpers ----------
    private fun coinsForProductId(productId: String) = when (productId) {
        "talksy_coins_50" -> 50
        "talksy_coins_100" -> 100
        "talksy_coins_260" -> 260
        "talksy_coins_550" -> 550
        "talksy_coins_2000" -> 2000
        else -> 50
    }
}
