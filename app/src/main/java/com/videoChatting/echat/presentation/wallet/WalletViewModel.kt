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

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager,
    val playBillingManager: PlayBillingManager
) : ViewModel() {

    private val _currentCoins = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 100)
    val currentCoins: StateFlow<Int> = _currentCoins

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment

    private val _paymentMessage = MutableStateFlow("")
    val paymentMessage: StateFlow<String> = _paymentMessage

    // Payment link for WebView fallback
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

    private fun setupPlayBillingCallbacks() {
        playBillingManager.onPurchaseCompleted = { purchaseToken, orderId, productId ->
            viewModelScope.launch {
                _isProcessingPayment.value = true
                _paymentMessage.value = "Verifying purchase with server..."
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
                        if (body.coinsBalance != null) {
                            _currentCoins.value = body.coinsBalance
                            sessionManager.updateCoins(body.coinsBalance)
                        }
                        _paymentMessage.value = "🎉 " + body.message
                        fetchLatestProfile()
                        fetchPurchaseHistory()
                        delay(2500)
                        _paymentMessage.value = ""
                    } else {
                        _paymentMessage.value = "Verification failed: ${response.message()}"
                        delay(2500)
                        _paymentMessage.value = ""
                    }
                } catch (e: Exception) {
                    _paymentMessage.value = "Verification error: ${e.localizedMessage}"
                    delay(2500)
                    _paymentMessage.value = ""
                } finally {
                    _isProcessingPayment.value = false
                }
            }
        }

        playBillingManager.onPurchaseFailed = { errorMessage ->
            _paymentMessage.value = errorMessage
            _isProcessingPayment.value = false
            viewModelScope.launch {
                delay(3000)
                if (_paymentMessage.value == errorMessage) {
                    _paymentMessage.value = ""
                }
            }
        }
    }

    fun purchaseWithGooglePlay(activity: Activity, productId: String) {
        _isProcessingPayment.value = true
        _paymentMessage.value = "Opening Google Play..."
        val success = playBillingManager.launchBillingFlow(activity, productId)
        if (!success) {
            _isProcessingPayment.value = false
        }
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
        _isProcessingPayment.value = false
    }

    fun onPaymentComplete(success: Boolean) {
        _sdkPayload.value = null
        _paymentUrl.value = null
        if (success && currentOrderId != null) {
            _paymentMessage.value = "Verifying payment..."
            viewModelScope.launch {
                try {
                    val response = apiService.verifyPayment(currentOrderId!!)
                    if (response.isSuccessful && response.body()?.success == true) {
                        _paymentMessage.value = "Payment Successful! Coins Added."
                        fetchLatestProfile()
                        fetchPurchaseHistory()
                        delay(2000)
                        _isProcessingPayment.value = false
                        _paymentMessage.value = ""
                    } else {
                        _paymentMessage.value = "Payment Verification Failed"
                        delay(2000)
                        _isProcessingPayment.value = false
                        _paymentMessage.value = ""
                    }
                } catch (e: Exception) {
                    _paymentMessage.value = "Verification Error: ${e.message}"
                    delay(2000)
                    _isProcessingPayment.value = false
                    _paymentMessage.value = ""
                }
            }
        } else {
            _paymentMessage.value = "Payment was not completed"
            viewModelScope.launch {
                delay(2000)
                _isProcessingPayment.value = false
                _paymentMessage.value = ""
            }
        }
    }

    fun dismissPaymentWebView() {
        _paymentUrl.value = null
        _isProcessingPayment.value = false
        _paymentMessage.value = ""
    }
}
