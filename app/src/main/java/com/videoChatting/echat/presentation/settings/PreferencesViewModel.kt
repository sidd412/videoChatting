package com.videoChatting.echat.presentation.settings

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

sealed class PreferencesState {
    object Idle : PreferencesState()
    object Loading : PreferencesState()
    object Success : PreferencesState()
    data class Error(val message: String) : PreferencesState()
}

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<PreferencesState>(PreferencesState.Idle)
    val state: StateFlow<PreferencesState> = _state

    private val _prefGender = MutableStateFlow("All")
    val prefGender: StateFlow<String> = _prefGender

    private val _prefMinAge = MutableStateFlow(18)
    val prefMinAge: StateFlow<Int> = _prefMinAge

    private val _prefMaxAge = MutableStateFlow(99)
    val prefMaxAge: StateFlow<Int> = _prefMaxAge

    private val _filterType = MutableStateFlow("country")
    val filterType: StateFlow<String> = _filterType

    private val _kmRadius = MutableStateFlow(50)
    val kmRadius: StateFlow<Int> = _kmRadius

    init {
        loadCurrentPreferences()
    }

    private fun loadCurrentPreferences() {
        val profile = sessionManager.getUserProfile()
        if (profile != null && profile.preferences != null) {
            _prefGender.value = profile.preferences.gender ?: "All"
            _prefMinAge.value = profile.preferences.minAge ?: 18
            _prefMaxAge.value = profile.preferences.maxAge ?: 99
            _filterType.value = profile.preferences.filterType ?: "country"
            _kmRadius.value = profile.preferences.kmRadius ?: 50
        }
    }

    fun updatePreferencesState(
        gender: String,
        minAge: Int,
        maxAge: Int,
        type: String,
        radius: Int
    ) {
        _prefGender.value = gender
        _prefMinAge.value = minAge
        _prefMaxAge.value = maxAge
        _filterType.value = type
        _kmRadius.value = radius
    }

    fun savePreferences(
        prefGender: String,
        prefMinAge: Int,
        prefMaxAge: Int,
        filterType: String,
        kmRadius: Int
    ) {
        _state.value = PreferencesState.Loading
        viewModelScope.launch {
            try {
                val request = UpdateProfileRequest(
                    prefGender = prefGender,
                    prefMinAge = prefMinAge,
                    prefMaxAge = prefMaxAge,
                    filterType = filterType,
                    kmRadius = kmRadius
                )
                val response = apiService.updateProfile(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Update local user profile cache
                    sessionManager.saveUserProfile(body.user)
                    _state.value = PreferencesState.Success
                } else {
                    _state.value = PreferencesState.Error(
                        response.errorBody()?.string() ?: "Failed to save preferences"
                    )
                }
            } catch (e: Exception) {
                _state.value = PreferencesState.Error(
                    e.localizedMessage ?: "Unknown connection error"
                )
            }
        }
    }
}
