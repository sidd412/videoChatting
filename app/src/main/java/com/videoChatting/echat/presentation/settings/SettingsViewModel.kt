package com.videoChatting.echat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    fun uploadDummyUsersToDatabase() {
        viewModelScope.launch {
            try {
                // Get dummy users from the local flow
                val dummyUsers = userRepository.getDiscoverableUsers().first()
                dummyUsers.forEach { user ->
                    userRepository.saveUser(user)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
