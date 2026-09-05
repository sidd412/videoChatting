package com.videoChatting.echat.data.remote

import com.videoChatting.echat.data.remote.model.AuthResponse
import com.videoChatting.echat.data.remote.model.GoogleLoginRequest
import com.videoChatting.echat.data.remote.model.GuestLoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @POST("auth/guest")
    suspend fun loginGuest(
        @Body request: GuestLoginRequest
    ): Response<AuthResponse>

    @POST("auth/google")
    suspend fun loginGoogle(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @GET("auth/profile")
    suspend fun getSelfProfile(): Response<AuthResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Body request: com.videoChatting.echat.data.remote.model.UpdateProfileRequest
    ): Response<AuthResponse>

    @GET("auth/profile/{userId}")
    suspend fun getUserProfile(
        @Path("userId") userId: String
    ): Response<com.videoChatting.echat.data.remote.model.UserProfileResponse>

    @GET("chat/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String
    ): Response<com.videoChatting.echat.data.remote.model.MessagesHistoryResponse>

    @GET("block/list")
    suspend fun getBlockedList(): Response<com.videoChatting.echat.presentation.settings.BlockedListResponse>

    @POST("block/unblock")
    suspend fun unblockUser(
        @Body request: com.videoChatting.echat.presentation.settings.UnblockRequest
    ): Response<com.videoChatting.echat.presentation.settings.UnblockResponse>

    @POST("auth/interaction")
    suspend fun toggleInteraction(
        @Body request: com.videoChatting.echat.data.remote.model.ToggleInteractionRequest
    ): Response<com.videoChatting.echat.data.remote.model.AuthResponse>

    @GET("auth/interactions")
    suspend fun getInteractions(): Response<com.videoChatting.echat.data.remote.model.InteractionsResponse>

    @GET("consent")
    suspend fun getPendingConsents(
        @retrofit2.http.Query("userId") userId: String
    ): Response<com.videoChatting.echat.data.remote.model.ConsentNotificationsResponse>

    @POST("consent/respond")
    suspend fun respondToConsent(
        @Body request: com.videoChatting.echat.data.remote.model.RespondConsentRequest
    ): Response<com.videoChatting.echat.data.remote.model.RespondConsentResponse>

    @POST("consent/revoke")
    suspend fun revokeConsent(
        @Body request: com.videoChatting.echat.data.remote.model.RevokeConsentRequest
    ): Response<com.videoChatting.echat.data.remote.model.RespondConsentResponse>

    @POST("payment/create-order")
    suspend fun createPaymentOrder(
        @Body request: com.videoChatting.echat.presentation.wallet.CreateOrderRequest
    ): Response<com.videoChatting.echat.presentation.wallet.CreateOrderResponse>

    @GET("payment/verify/{orderId}")
    suspend fun verifyPayment(
        @retrofit2.http.Path("orderId") orderId: String
    ): Response<com.videoChatting.echat.presentation.wallet.VerifyPaymentResponse>

    @GET("payment/history")
    suspend fun getPurchaseHistory(): Response<com.videoChatting.echat.presentation.wallet.PurchaseHistoryResponse>

    @POST("payment/verify-play-purchase")
    suspend fun verifyPlayPurchase(
        @Body request: com.videoChatting.echat.data.remote.model.VerifyPlayPurchaseRequest
    ): Response<com.videoChatting.echat.data.remote.model.VerifyPlayPurchaseResponse>

    @GET("payment/invoice/{orderId}")
    suspend fun getInvoice(
        @Path("orderId") orderId: String
    ): Response<com.videoChatting.echat.data.remote.model.InvoiceResponse>

    @POST("requests")
    suspend fun raiseRequest(
        @Body request: com.videoChatting.echat.data.remote.RaiseRequestDto
    ): Response<com.videoChatting.echat.data.remote.RaiseRequestResponse>

    @GET("rewards/status")
    suspend fun getRewardsStatus(): Response<com.videoChatting.echat.data.remote.model.RewardsStatusResponse>

    @POST("rewards/check-in")
    suspend fun claimDailyCheckIn(): Response<com.videoChatting.echat.data.remote.model.CheckInResponse>

    @POST("rewards/spin")
    suspend fun spinLuckyWheel(): Response<com.videoChatting.echat.data.remote.model.SpinResponse>

    // --- Contacts & Referral Endpoints ---
    @GET("contacts/referral")
    suspend fun getReferralInfo(): Response<com.videoChatting.echat.data.remote.model.ReferralInfoResponse>

    @POST("contacts/referral/claim")
    suspend fun claimReferralCode(
        @Body request: com.videoChatting.echat.data.remote.model.ClaimReferralRequest
    ): Response<com.videoChatting.echat.data.remote.model.ClaimReferralResponse>

    @POST("contacts/sync")
    suspend fun syncContacts(
        @Body request: com.videoChatting.echat.data.remote.model.SyncContactsRequest
    ): Response<com.videoChatting.echat.data.remote.model.SyncContactsResponse>

    @POST("contacts/privacy")
    suspend fun toggleContactsPrivacy(
        @Body request: com.videoChatting.echat.data.remote.model.ToggleContactsPrivacyRequest
    ): Response<com.videoChatting.echat.data.remote.model.ToggleContactsPrivacyResponse>
}

data class RaiseRequestDto(
    val type: String,
    val targetId: String? = null,
    val reason: String
)

data class RaiseRequestResponse(
    val success: Boolean,
    val message: String?,
    val requestId: String?
)

