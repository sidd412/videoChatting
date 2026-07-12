package com.videoChatting.echat.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.SocketManager
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
    private val socketManager: SocketManager
) : ViewModel() {

    private val _currentCoins = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 100)
    val currentCoins: StateFlow<Int> = _currentCoins

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment

    private val _paymentMessage = MutableStateFlow("")
    val paymentMessage: StateFlow<String> = _paymentMessage

    // Payment link for WebView
    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl

    // Keep track of the current order ID to verify it later
    private var currentOrderId: String? = null

    private val _purchaseHistory = MutableStateFlow<List<TransactionItem>>(emptyList())
    val purchaseHistory: StateFlow<List<TransactionItem>> = _purchaseHistory

    init {
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

    fun fetchLatestProfile() {
        viewModelScope.launch {
            val userId = sessionManager.getUserProfile()?.userId ?: return@launch
            try {
                val response = apiService.getUserProfile(userId)
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

    fun initiatePayment(pack: CoinPack) {
        viewModelScope.launch {
            _isProcessingPayment.value = true
            _paymentMessage.value = "Creating secure payment..."

            try {
                val request = CreateOrderRequest(amount = pack.priceInInr, coins = pack.coins)
                val response = apiService.createPaymentOrder(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val link = body.paymentLink
                    currentOrderId = body.orderId
                    
                    if (!link.isNullOrBlank()) {
                        _paymentMessage.value = ""
                        _paymentUrl.value = link
                    } else {
                        _paymentMessage.value = "No payment link received"
                        _isProcessingPayment.value = false
                    }
                } else {
                    _paymentMessage.value = "Failed to create order"
                    _isProcessingPayment.value = false
                }
            } catch (e: Exception) {
                _paymentMessage.value = "Payment Failed: ${e.message}"
                _isProcessingPayment.value = false
            }
        }
    }

    fun onPaymentComplete(success: Boolean) {
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
