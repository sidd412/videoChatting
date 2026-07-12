package com.videoChatting.echat.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.GoogleLoginRequest
import com.videoChatting.echat.data.remote.model.GuestLoginRequest
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun loginAsGuest(name: String) {
        _state.value = AuthState.Loading
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result else null
            viewModelScope.launch {
                try {
                    val response = apiService.loginGuest(GuestLoginRequest(name, token))
                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!
                        sessionManager.saveAuthToken(authResponse.token)
                        sessionManager.saveUserProfile(authResponse.user)
                        _state.value = AuthState.Success
                    } else {
                        _state.value = AuthState.Error(response.errorBody()?.string() ?: "Guest Login failed")
                    }
                } catch (e: Exception) {
                    _state.value = AuthState.Error(e.localizedMessage ?: "Network connection error")
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _state.value = AuthState.Loading
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result else null
            viewModelScope.launch {
                try {
                    val response = apiService.loginGoogle(GoogleLoginRequest(idToken, token))
                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!
                        sessionManager.saveAuthToken(authResponse.token)
                        sessionManager.saveUserProfile(authResponse.user)
                        _state.value = AuthState.Success
                    } else {
                        _state.value = AuthState.Error(response.errorBody()?.string() ?: "Google Sign-In failed")
                    }
                } catch (e: Exception) {
                    _state.value = AuthState.Error(e.localizedMessage ?: "Network connection error")
                }
            }
        }
    }
}
