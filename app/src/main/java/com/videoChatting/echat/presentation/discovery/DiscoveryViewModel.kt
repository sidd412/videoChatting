package com.videoChatting.echat.presentation.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.domain.repository.MatchResult
import com.videoChatting.echat.domain.repository.MatchmakingRepository
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Searching : DiscoveryState()
    data class Matched(val match: MatchResult) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()
    
    private val currentUserId: String? = userRepository.getCurrentUserId()

    init {
        // Observe match status passively
        viewModelScope.launch {
            if (currentUserId != null) {
                matchmakingRepository.observeMatchStatus(currentUserId).collect { match ->
                    if (match != null) {
                        _state.value = DiscoveryState.Matched(match)
                    }
                }
            }
        }
    }

    private var searchJob: Job? = null

    fun startDiscovery() {
        if (currentUserId == null) {
            _state.value = DiscoveryState.Error("User not logged in")
            return
        }

        _state.value = DiscoveryState.Searching
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            while (_state.value is DiscoveryState.Searching) {
                val result = matchmakingRepository.findMatch(currentUserId)
                if (result.isSuccess) {
                    val match = result.getOrNull()
                    if (match != null) {
                        _state.value = DiscoveryState.Matched(match)
                        break
                    }
                }
                // Wait 3 seconds before trying again to handle simultaneous queue entries
                delay(3000)
            }
        }
    }

    fun nextPerson() {
        if (currentUserId == null) return
        
        searchJob?.cancel()
        viewModelScope.launch {
            // Leave current match
            matchmakingRepository.leaveMatch(currentUserId)
            _state.value = DiscoveryState.Idle
            // Immediately start searching again
            startDiscovery()
        }
    }

    fun toggleLike(targetUserId: String, isLiked: Boolean) {
        viewModelScope.launch {
            userRepository.toggleInteraction(targetUserId, "liked", isLiked)
        }
    }

    fun toggleAdd(targetUserId: String, isAdded: Boolean) {
        viewModelScope.launch {
            userRepository.toggleInteraction(targetUserId, "added", isAdded)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentUserId?.let {
            viewModelScope.launch {
                matchmakingRepository.leaveMatch(it)
            }
        }
    }
}
