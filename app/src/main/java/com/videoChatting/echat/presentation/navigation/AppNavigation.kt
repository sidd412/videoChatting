package com.videoChatting.echat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.videoChatting.echat.presentation.auth.AuthScreen
import com.videoChatting.echat.presentation.call.CallScreen
import com.videoChatting.echat.presentation.home.HomeScreen
import com.videoChatting.echat.presentation.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main")
    object Chat : Screen("chat/{userName}") {
        fun createRoute(userName: String) = "chat/$userName"
    }
    object Call : Screen("call/{channelName}") {
        fun createRoute(channelName: String) = "call/$channelName"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Auth.route) {
            AuthScreen(onLoginSuccess = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Main.route) {
            com.videoChatting.echat.presentation.main.MainScreen(navController)
        }
        composable(Screen.Chat.route) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            com.videoChatting.echat.presentation.chat.ChatScreen(navController, userName)
        }
        composable(Screen.Call.route) { backStackEntry ->
            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
            CallScreen(channelName = channelName, onCallEnded = {
                navController.popBackStack()
            })
        }
    }
}
