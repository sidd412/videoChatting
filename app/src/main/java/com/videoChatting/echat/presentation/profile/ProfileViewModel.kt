package com.videoChatting.echat.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.domain.repository.UserRepository
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.UserDto
import com.videoChatting.echat.data.remote.model.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    val userProfile: StateFlow<UserDto?> = _userProfile

    init {
        loadProfile()
    }

    fun loadProfile() {
        val profile = sessionManager.getUserProfile()
        if (profile != null) {
            _userProfile.value = profile
        }
    }

    fun updateProfile(
        name: String?,
        gender: String?,
        age: Int?,
        dob: String?,
        bio: String?,
        contactNumber: String?,
        avatar: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = UpdateProfileRequest(
                    name = name,
                    gender = gender,
                    age = age,
                    dob = dob,
                    bio = bio,
                    contactNumber = contactNumber,
                    avatar = avatar
                )
                val response = apiService.updateProfile(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    val updatedUser = response.body()?.user
                    if (updatedUser != null) {
                        sessionManager.saveUserProfile(updatedUser)
                        _userProfile.value = updatedUser
                    }
                    onSuccess()
                } else {
                    onError(response.message())
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onLogoutSuccess()
        }
    }
}
