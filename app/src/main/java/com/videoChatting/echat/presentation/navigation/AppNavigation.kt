package com.videoChatting.echat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.presentation.auth.AuthScreen
import com.videoChatting.echat.presentation.call.CallScreen
import com.videoChatting.echat.presentation.onboarding.OnboardingScreen
import com.videoChatting.echat.presentation.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object Chat : Screen("chat/{userId}/{userName}") {
        fun createRoute(userId: String, userName: String) = "chat/$userId/$userName"
    }
    object Call : Screen("call/{channelName}") {
        fun createRoute(channelName: String) = "call/$channelName"
    }
    object EditProfile : Screen("edit_profile")
    object Preferences : Screen("preferences")
    object BuyMinutes : Screen("buy_minutes")
    object PurchaseHistory : Screen("purchase_history")
    object Notifications : Screen("notifications")
    object Privacy : Screen("privacy")
    object Theme : Screen("theme")
    object ConsentNotifications : Screen("consent_notifications")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    val navigateNext = {
        val profile = sessionManager.getUserProfile()
        if (profile != null && !profile.gender.isNullOrEmpty() && profile.gender != "Not Specified" && profile.gender != "Any") {
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                if (sessionManager.getAuthToken() != null) {
                    navigateNext()
                } else {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }
        composable(Screen.Auth.route) {
            AuthScreen(onLoginSuccess = {
                navigateNext()
            })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onOnboardingFinished = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Main.route) {
            com.videoChatting.echat.presentation.main.MainScreen(navController)
        }
        composable(Screen.Chat.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            com.videoChatting.echat.presentation.chat.ChatScreen(navController, userId, userName)
        }
        composable(Screen.Call.route) { backStackEntry ->
            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
            CallScreen(channelName = channelName, onCallEnded = {
                navController.popBackStack()
            })
        }
        
        // Placeholder Screens
        composable(Screen.EditProfile.route) {
            com.videoChatting.echat.presentation.profile.EditProfileScreen(navController)
        }
        composable(Screen.Preferences.route) {
            com.videoChatting.echat.presentation.settings.PreferencesScreen(navController)
        }
        composable(Screen.BuyMinutes.route) {
            com.videoChatting.echat.presentation.settings.BuyMinutesScreen(navController)
        }
        composable(Screen.PurchaseHistory.route) {
            com.videoChatting.echat.presentation.settings.PurchaseHistoryScreen(navController)
        }
        composable(Screen.Notifications.route) {
            com.videoChatting.echat.presentation.settings.PlaceholderScreen(navController, "Notification Settings")
        }
        composable(Screen.Privacy.route) {
            com.videoChatting.echat.presentation.settings.PlaceholderScreen(navController, "Privacy Settings")
        }
        composable(Screen.Theme.route) {
            com.videoChatting.echat.presentation.settings.PlaceholderScreen(navController, "Theme Settings")
        }
        composable(Screen.ConsentNotifications.route) {
            com.videoChatting.echat.presentation.settings.ConsentNotificationsScreen(navController)
        }
    }
}
