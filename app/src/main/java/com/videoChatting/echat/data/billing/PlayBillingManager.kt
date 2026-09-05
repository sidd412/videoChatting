package com.videoChatting.echat.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val TAG = "PlayBillingManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady

    var onPurchaseCompleted: ((purchaseToken: String, orderId: String?, productId: String) -> Unit)? = null
    var onPurchaseFailed: ((errorMessage: String) -> Unit)? = null

    val coinProductIds = listOf(
        "talksy_coins_50",
        "talksy_coins_100",
        "talksy_coins_260",
        "talksy_coins_550",
        "talksy_coins_2000"
    )

    fun startConnection() {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Google Play Billing setup successful")
            _isBillingReady.value = true
            queryCoinProducts()
            queryExistingPurchases()
        } else {
            Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
            _isBillingReady.value = false
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected, attempting to reconnect...")
        _isBillingReady.value = false
        // Try reconnecting
        startConnection()
    }

    fun queryCoinProducts() {
        val productList = coinProductIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = queryProductDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.d(TAG, "Loaded ${map.size} in-app products from Google Play")
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String): Boolean {
        val productDetails = _productDetailsMap.value[productId]
        if (productDetails == null) {
            Log.w(TAG, "ProductDetails not found for: $productId")
            onPurchaseFailed?.invoke("Product details not available from Google Play. Please try again.")
            queryCoinProducts()
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled Google Play purchase flow")
                onPurchaseFailed?.invoke("Purchase canceled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Pending or already-owned item — try to process any existing purchases
                Log.w(TAG, "Item already owned — checking existing purchases")
                queryExistingPurchases()
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage} (code ${billingResult.responseCode})")
                onPurchaseFailed?.invoke("Payment failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val productId = purchase.products.firstOrNull() ?: "talksy_coins_50"
                val purchaseToken = purchase.purchaseToken
                val orderId = purchase.orderId

                Log.d(TAG, "Purchase PURCHASED for $productId, consuming...")

                // Consume so the user can buy the same product again
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()

                billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Successfully consumed $productId")
                    } else {
                        Log.w(TAG, "Failed to consume: ${billingResult.debugMessage}")
                    }
                }

                // Notify ViewModel to credit coins via backend
                onPurchaseCompleted?.invoke(purchaseToken, orderId, productId)
            }
            Purchase.PurchaseState.PENDING -> {
                // Slow test card or UPI pending — Google will call onPurchasesUpdated again
                // once it transitions to PURCHASED. Keep overlay visible (do nothing here).
                val productId = purchase.products.firstOrNull() ?: ""
                Log.d(TAG, "Purchase PENDING for $productId — waiting for Google Play to confirm...")
                // Don't call onPurchaseFailed — the overlay should stay up
            }
            else -> {
                Log.w(TAG, "Unhandled purchase state: ${purchase.purchaseState}")
                onPurchaseFailed?.invoke("Purchase could not be verified. Please try again.")
            }
        }
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            }
        }
    }
}
