package com.videoChatting.echat.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.videoChatting.echat.data.remote.model.UserDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "echat_session"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_PROFILE = "user_profile"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    fun saveUserProfile(user: UserDto) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER_PROFILE, userJson).apply()
    }

    fun getUserProfile(): UserDto? {
        val userJson = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            gson.fromJson(userJson, UserDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun updateCoins(coins: Int) {
        val currentUser = getUserProfile()
        if (currentUser != null) {
            saveUserProfile(currentUser.copy(coinsBalance = coins))
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
