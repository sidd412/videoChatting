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
import android.content.Intent
import androidx.navigation.NavHostController
import androidx.compose.runtime.LaunchedEffect


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
    object Wallet : Screen("wallet")
    object InviteAndContacts : Screen("invite_and_contacts")
}

@Composable
fun AppNavigation(navController: NavHostController, intent: Intent?) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    LaunchedEffect(intent) {
        // DeepLink Referral handling (e.g. talksy://invite?code=TALK-XXXX or https://talksy.app/invite?code=TALK-XXXX)
        val deepLinkCode = intent?.data?.getQueryParameter("code")
        if (!deepLinkCode.isNullOrBlank()) {
            val userProfile = sessionManager.getUserProfile()
            if (userProfile != null && sessionManager.getAuthToken() != null) {
                navController.navigate(Screen.InviteAndContacts.route)
            }
        }

        // FCM background notifications put data fields directly into the Intent extras.
        // So we check "type" (from FCM data payload) OR "notification_type" (from our foreground service)
        val type = intent?.getStringExtra("notification_type") ?: intent?.getStringExtra("type")
        if (type == "CONSENT_REQUEST") {
            navController.navigate(Screen.ConsentNotifications.route)
        } else if (type == "CHAT_MESSAGE") {
            val senderId = intent?.getStringExtra("senderId")
            val senderName = intent?.getStringExtra("senderName")
            if (senderId != null && senderName != null) {
                navController.navigate(Screen.Chat.createRoute(senderId, senderName))
            } else {
                navController.navigate(Screen.Main.route)
            }
        }
    }

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
            }, navController = navController)
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
        
        // Placeholder / Actual Screens
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
            com.videoChatting.echat.presentation.wallet.PurchaseHistoryScreen(navController)
        }
        composable("purchase_history") {
            com.videoChatting.echat.presentation.wallet.PurchaseHistoryScreen(navController)
        }
        composable(
            route = "invoice_detail/{orderId}",
            arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            com.videoChatting.echat.presentation.wallet.InvoiceDetailScreen(orderId = orderId, navController = navController)
        }
        composable(Screen.Notifications.route) {
            com.videoChatting.echat.presentation.settings.PlaceholderScreen(navController, "Notification Settings")
        }
        composable(Screen.Privacy.route) {
            com.videoChatting.echat.presentation.settings.PrivacyPolicyScreen(navController)
        }
        composable("terms_of_service") {
            com.videoChatting.echat.presentation.settings.TermsOfServiceScreen(navController)
        }
        composable("about_us") {
            com.videoChatting.echat.presentation.settings.AboutUsScreen(navController)
        }
        composable("help_support") {
            com.videoChatting.echat.presentation.settings.HelpSupportScreen(navController)
        }
        composable("contact_us") {
            com.videoChatting.echat.presentation.settings.ContactUsScreen(navController)
        }
        composable(Screen.Theme.route) {
            com.videoChatting.echat.presentation.settings.ThemeSettingsScreen(navController)
        }
        composable(Screen.ConsentNotifications.route) {
            com.videoChatting.echat.presentation.settings.ConsentNotificationsScreen(navController)
        }
        composable("blocked_list") {
            com.videoChatting.echat.presentation.settings.BlockedListScreen(navController)
        }
        composable(Screen.Wallet.route) {
            com.videoChatting.echat.presentation.wallet.WalletScreen(navController)
        }
        composable(Screen.InviteAndContacts.route) {
            com.videoChatting.echat.presentation.contacts.InviteAndContactsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { userId, userName ->
                    navController.navigate(Screen.Chat.createRoute(userId, userName))
                }
            )
        }
    }
}
