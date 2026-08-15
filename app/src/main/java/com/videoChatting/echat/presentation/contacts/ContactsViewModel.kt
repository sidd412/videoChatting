package com.videoChatting.echat.presentation.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.*
import com.videoChatting.echat.utils.ContactUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _referralCode = MutableStateFlow("")
    val referralCode: StateFlow<String> = _referralCode.asStateFlow()

    private val _referralCount = MutableStateFlow(0)
    val referralCount: StateFlow<Int> = _referralCount.asStateFlow()

    private val _coinsEarned = MutableStateFlow(0)
    val coinsEarned: StateFlow<Int> = _coinsEarned.asStateFlow()

    private val _bonusPerReferral = MutableStateFlow(50)
    val bonusPerReferral: StateFlow<Int> = _bonusPerReferral.asStateFlow()

    private val _hasClaimedReferral = MutableStateFlow(false)
    val hasClaimedReferral: StateFlow<Boolean> = _hasClaimedReferral.asStateFlow()

    private val _blockContactsMatching = MutableStateFlow(false)
    val blockContactsMatching: StateFlow<Boolean> = _blockContactsMatching.asStateFlow()

    private val _rawDeviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())

    private val _registeredContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val registeredContacts: StateFlow<List<DeviceContact>> = _registeredContacts.asStateFlow()

    private val _inviteContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val inviteContacts: StateFlow<List<DeviceContact>> = _inviteContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadReferralInfo()
    }

    fun loadReferralInfo() {
        viewModelScope.launch {
            try {
                val response = apiService.getReferralInfo()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _referralCode.value = data.referralCode
                    _referralCount.value = data.referralCount
                    _coinsEarned.value = data.coinsEarned
                    _bonusPerReferral.value = data.bonusPerReferral
                    _hasClaimedReferral.value = data.hasClaimedReferral
                    _blockContactsMatching.value = data.blockContactsMatching
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val rawContacts = ContactUtils.getDeviceContacts(context)
                _rawDeviceContacts.value = rawContacts

                val phoneNumbers = rawContacts.map { it.phoneNumber }
                if (phoneNumbers.isNotEmpty()) {
                    val response = apiService.syncContacts(SyncContactsRequest(phoneNumbers))
                    if (response.isSuccessful && response.body() != null) {
                        val backendRegistered = response.body()!!.registeredContacts

                        val registeredMap = backendRegistered.associateBy { it.contactNumber?.takeLast(10) ?: "" }

                        val registered = mutableListOf<DeviceContact>()
                        val nonRegistered = mutableListOf<DeviceContact>()

                        for (c in rawContacts) {
                            val last10 = c.phoneNumber.filter { it.isDigit() }.takeLast(10)
                            val matchedUser = registeredMap[last10]

                            if (matchedUser != null) {
                                registered.add(c.copy(isRegistered = true, registeredUser = matchedUser))
                            } else {
                                nonRegistered.add(c.copy(isRegistered = false))
                            }
                        }

                        _registeredContacts.value = registered
                        _inviteContacts.value = nonRegistered
                    } else {
                        _inviteContacts.value = rawContacts
                    }
                } else {
                    _inviteContacts.value = rawContacts
                }
            } catch (e: Exception) {
                _actionMessage.value = "Sync error: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun claimReferralCode(code: String, onBonusAwarded: (Int) -> Unit) {
        if (code.isBlank()) {
            _actionMessage.value = "Please enter a valid referral code"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.claimReferralCode(ClaimReferralRequest(code.trim()))
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _hasClaimedReferral.value = true
                    _actionMessage.value = data.message
                    data.bonusCoins?.let { onBonusAwarded(it) }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Invalid code"
                    _actionMessage.value = errorMsg
                }
            } catch (e: Exception) {
                _actionMessage.value = "Failed to redeem code: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePrivacy(block: Boolean) {
        viewModelScope.launch {
            try {
                _blockContactsMatching.value = block
                val response = apiService.toggleContactsPrivacy(ToggleContactsPrivacyRequest(block))
                if (response.isSuccessful && response.body() != null) {
                    _actionMessage.value = response.body()!!.message
                }
            } catch (e: Exception) {
                _actionMessage.value = "Failed to update privacy"
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
