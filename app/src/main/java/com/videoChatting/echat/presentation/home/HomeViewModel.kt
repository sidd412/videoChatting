package com.videoChatting.echat.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.domain.model.User
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _discoverableUsers = MutableStateFlow<List<User>>(emptyList())
    val discoverableUsers: StateFlow<List<User>> = _discoverableUsers.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getDiscoverableUsers().collect { users ->
                _discoverableUsers.value = users
            }
        }
    }

    fun onSwipeLeft(user: User) {
        val currentList = _discoverableUsers.value.toMutableList()
        currentList.remove(user)
        _discoverableUsers.value = currentList
    }

    fun onSwipeRight(user: User) {
        // Here we would typically trigger match logic or direct call
        val currentList = _discoverableUsers.value.toMutableList()
        currentList.remove(user)
        _discoverableUsers.value = currentList
    }
}
