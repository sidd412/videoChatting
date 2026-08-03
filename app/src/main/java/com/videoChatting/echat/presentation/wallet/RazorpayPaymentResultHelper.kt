package com.videoChatting.echat.presentation.wallet

import com.razorpay.PaymentData

object RazorpayPaymentResultHelper {
    var onPaymentSuccess: ((String?, PaymentData?) -> Unit)? = null
    var onPaymentError: ((Int, String?, PaymentData?) -> Unit)? = null

    fun clear() {
        onPaymentSuccess = null
        onPaymentError = null
    }
}
