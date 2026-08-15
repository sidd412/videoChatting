package com.videoChatting.echat.presentation.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.CheckInResponse
import com.videoChatting.echat.data.remote.model.RewardsStatusResponse
import com.videoChatting.echat.data.remote.model.SpinResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _status = MutableStateFlow<RewardsStatusResponse?>(null)
    val status: StateFlow<RewardsStatusResponse?> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _wonPrize = MutableStateFlow<SpinResponse?>(null)
    val wonPrize: StateFlow<SpinResponse?> = _wonPrize.asStateFlow()

    private val _checkInMessage = MutableStateFlow<String?>(null)
    val checkInMessage: StateFlow<String?> = _checkInMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadRewardsStatus()
    }

    fun loadRewardsStatus() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getRewardsStatus()
                if (response.isSuccessful && response.body() != null) {
                    _status.value = response.body()
                    _status.value?.coinsBalance?.let {
                        sessionManager.updateCoins(it)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun claimCheckIn() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.claimDailyCheckIn()
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        _checkInMessage.value = result.message ?: "+${result.coinsEarned} Coins Claimed!"
                        sessionManager.updateCoins(result.coinsBalance)
                        loadRewardsStatus()
                    } else {
                        _errorMessage.value = result.error ?: "Already claimed today!"
                    }
                } else {
                    _errorMessage.value = "Failed to claim reward"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun spinWheel(onTargetAngleReady: (targetAngle: Float, prize: SpinResponse) -> Unit) {
        if (_isSpinning.value) return

        viewModelScope.launch {
            try {
                _isSpinning.value = true
                val response = apiService.spinLuckyWheel()
                if (response.isSuccessful && response.body() != null) {
                    val prize = response.body()!!
                    if (prize.success) {
                        sessionManager.updateCoins(prize.coinsBalance)
                        
                        // 8 segments, each segment is 45 degrees
                        // Pointer is at top (270 degrees or 90 degrees depending on orientation)
                        val segmentAngle = 360f / 8f
                        val targetCenterAngle = (prize.prizeIndex * segmentAngle) + (segmentAngle / 2f)
                        
                        // Spin 5 full rotations (1800 deg) + target offset
                        val totalSpinAngle = 1800f + (360f - targetCenterAngle)
                        
                        onTargetAngleReady(totalSpinAngle, prize)
                    } else {
                        _isSpinning.value = false
                        _errorMessage.value = prize.error ?: "Cooldown active"
                    }
                } else {
                    _isSpinning.value = false
                    _errorMessage.value = "Failed to spin. Try again."
                }
            } catch (e: Exception) {
                _isSpinning.value = false
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun onSpinAnimationFinished(prize: SpinResponse) {
        _isSpinning.value = false
        _wonPrize.value = prize
        loadRewardsStatus()
    }

    fun dismissPrizeDialog() {
        _wonPrize.value = null
    }

    fun dismissCheckInMessage() {
        _checkInMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
