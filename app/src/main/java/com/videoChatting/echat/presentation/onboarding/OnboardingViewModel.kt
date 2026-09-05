package com.videoChatting.echat.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    object Success : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state: StateFlow<OnboardingState> = _state

    fun submitOnboarding(
        name: String,
        gender: String,
        age: Int,
        country: String,
        longitude: Double,
        latitude: Double,
        prefGender: String,
        prefMinAge: Int,
        prefMaxAge: Int,
        filterType: String,
        kmRadius: Int,
        referralCode: String? = null
    ) {
        _state.value = OnboardingState.Loading
        viewModelScope.launch {
            try {
                val request = UpdateProfileRequest(
                    name = name,
                    gender = gender,
                    age = age,
                    country = country,
                    longitude = longitude,
                    latitude = latitude,
                    prefGender = prefGender,
                    prefMinAge = prefMinAge,
                    prefMaxAge = prefMaxAge,
                    filterType = filterType,
                    kmRadius = kmRadius
                )
                val response = apiService.updateProfile(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    // Claim referral bonus if referral code was provided
                    if (!referralCode.isNullOrBlank()) {
                        try {
                            apiService.claimReferralCode(
                                com.videoChatting.echat.data.remote.model.ClaimReferralRequest(referralCode.trim())
                            )
                        } catch (e: Exception) {
                            // Non-fatal if referral code fails
                        }
                    }

                    // Save the updated profile locally
                    sessionManager.saveUserProfile(body.user)
                    _state.value = OnboardingState.Success
                } else {
                    _state.value = OnboardingState.Error(response.errorBody()?.string() ?: "Failed to save profile")
                }
            } catch (e: Exception) {
                _state.value = OnboardingState.Error(e.localizedMessage ?: "Unknown connection error")
            }
        }
    }
}
