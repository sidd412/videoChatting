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

data class CreateOrderRequest(
    val amount: Int,
    val coins: Int
)

data class CreateOrderResponse(
    val success: Boolean,
    val orderId: String,
    val paymentLink: String?,
    val sdkPayload: Map<String, Any>?
)

data class VerifyPaymentResponse(
    val success: Boolean,
    val status: String,
    val message: String?
)

data class TransactionItem(
    val orderId: String,
    val amount: Int,
    val coins: Int,
    val status: String,
    val createdAt: String
)

data class PurchaseHistoryResponse(
    val success: Boolean,
    val transactions: List<TransactionItem>
)

data class PurchaseSuccessEvent(
    val coinsAdded: Int,
    val newBalance: Int
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager,
    val playBillingManager: PlayBillingManager
) : ViewModel() {

    private val _currentCoins = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 100)
    val currentCoins: StateFlow<Int> = _currentCoins

    // Full-screen non-cancellable overlay during server verification
    private val _isVerifyingPurchase = MutableStateFlow(false)
    val isVerifyingPurchase: StateFlow<Boolean> = _isVerifyingPurchase

    // Success popup dialog event
    private val _purchaseSuccessEvent = MutableStateFlow<PurchaseSuccessEvent?>(null)
    val purchaseSuccessEvent: StateFlow<PurchaseSuccessEvent?> = _purchaseSuccessEvent

    private val _paymentMessage = MutableStateFlow("")
    val paymentMessage: StateFlow<String> = _paymentMessage

    // Fallbacks
    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl

    private val _sdkPayload = MutableStateFlow<Map<String, Any>?>(null)
    val sdkPayload: StateFlow<Map<String, Any>?> = _sdkPayload

    private var currentOrderId: String? = null

    private val _purchaseHistory = MutableStateFlow<List<TransactionItem>>(emptyList())
    val purchaseHistory: StateFlow<List<TransactionItem>> = _purchaseHistory

    init {
        // 1. Connect to Google Play Billing
        playBillingManager.startConnection()
        setupPlayBillingCallbacks()

        // 2. Listen to real-time socket events
        viewModelScope.launch {
            socketManager.matchEvents.collect { event ->
                if (event is com.videoChatting.echat.data.remote.SocketEvent.WalletUpdate) {
                    _currentCoins.value = event.coinsBalance
                    sessionManager.updateCoins(event.coinsBalance)
                }
            }
        }
        viewModelScope.launch {
            socketManager.userStatusEvents.collect { (userId, _) ->
                val currentUser = sessionManager.getUserProfile()
                if (currentUser?.userId == userId) {
                    fetchLatestProfile()
                }
            }
        }
        fetchLatestProfile()
        fetchPurchaseHistory()
    }

    private fun getCoinsForProductId(productId: String): Int {
        return when (productId) {
            "talksy_coins_50" -> 50
            "talksy_coins_100" -> 100
            "talksy_coins_260" -> 260
            "talksy_coins_550" -> 550
            "talksy_coins_2000" -> 2000
            else -> 50
        }
    }

    private fun setupPlayBillingCallbacks() {
        playBillingManager.onPurchaseCompleted = { purchaseToken, orderId, productId ->
            viewModelScope.launch {
                _isVerifyingPurchase.value = true
                _paymentMessage.value = ""
                try {
                    val request = VerifyPlayPurchaseRequest(
                        purchaseToken = purchaseToken,
                        productId = productId,
                        orderId = orderId,
                        packageName = "com.videoChatting.echat"
                    )
                    val response = apiService.verifyPlayPurchase(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        val coinsAdded = getCoinsForProductId(productId)
                        val newBal = body.coinsBalance ?: ((_currentCoins.value) + coinsAdded)
                        _currentCoins.value = newBal
                        sessionManager.updateCoins(newBal)

                        fetchLatestProfile()
                        fetchPurchaseHistory()

                        // Trigger animated celebratory success dialog
                        _purchaseSuccessEvent.value = PurchaseSuccessEvent(coinsAdded, newBal)
                    } else {
                        _paymentMessage.value = "Verification failed: ${response.message()}"
                    }
                } catch (e: Exception) {
                    _paymentMessage.value = "Verification error: ${e.localizedMessage}"
                } finally {
                    _isVerifyingPurchase.value = false
                }
            }
        }

        playBillingManager.onPurchaseFailed = { errorMessage ->
            if (errorMessage != "Purchase canceled") {
                _paymentMessage.value = errorMessage
                viewModelScope.launch {
                    delay(3500)
                    if (_paymentMessage.value == errorMessage) {
                        _paymentMessage.value = ""
                    }
                }
            }
        }
    }

    fun purchaseWithGooglePlay(activity: Activity, productId: String) {
        _paymentMessage.value = ""
        playBillingManager.launchBillingFlow(activity, productId)
    }

    fun dismissSuccessDialog() {
        _purchaseSuccessEvent.value = null
    }

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
            try {
                val response = apiService.getPurchaseHistory()
                if (response.isSuccessful && response.body() != null) {
                    _purchaseHistory.value = response.body()!!.transactions
                }
            } catch (_: Exception) { }
        }
    }

    fun isGuestUser(): Boolean {
        return sessionManager.getUserProfile()?.userId?.startsWith("guest_") == true
    }

    fun dismissRazorpayCheckout() {
        _sdkPayload.value = null
    }

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
                        _paymentMessage.value = "Payment Verification Failed"
                    }
                } catch (e: Exception) {
                    _paymentMessage.value = "Verification Error: ${e.message}"
                } finally {
                    _isVerifyingPurchase.value = false
                }
            }
        }
    }

    fun dismissPaymentWebView() {
        _paymentUrl.value = null
        _paymentMessage.value = ""
    }
}
