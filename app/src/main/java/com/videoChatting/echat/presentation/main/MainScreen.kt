package com.videoChatting.echat.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videoChatting.echat.presentation.discovery.DiscoveryScreen
import com.videoChatting.echat.presentation.home.ChatsHomeScreen
import com.videoChatting.echat.presentation.profile.ProfileScreen
import com.videoChatting.echat.presentation.settings.SettingsScreen
import com.videoChatting.echat.presentation.theme.*

@Composable
fun MainScreen(appNavController: NavController) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            GlassmorphicBottomBar(navController = bottomNavController)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = bottomNavController, startDestination = "chats_home") {
                composable("chats_home") { 
                    ChatsHomeScreen(
                        navController = appNavController,
                        onNavigateToDiscovery = {
                            bottomNavController.navigate("discovery") {
                                popUpTo(bottomNavController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    ) 
                }
                composable("discovery") { DiscoveryScreen() }
                composable("profile") { ProfileScreen(appNavController = appNavController) }
                composable("settings") { SettingsScreen(appNavController = appNavController) }
            }
        }
    }
}

@Composable
fun GlassmorphicBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bottomBgColor = if (isDark) ObsidianBlack else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bottomBgColor)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBar(
            modifier = Modifier.height(72.dp),
            containerColor = Color.Transparent,
            contentColor = Color.White,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
                label = { Text("Chats") },
                selected = currentRoute == "chats_home",
                onClick = {
                    if (currentRoute != "chats_home") {
                        navController.navigate("chats_home") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricIndigo,
                    selectedTextColor = ElectricIndigo,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                label = { Text("Discover") },
                selected = currentRoute == "discovery",
                onClick = {
                    if (currentRoute != "discovery") {
                        navController.navigate("discovery") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricIndigo,
                    selectedTextColor = ElectricIndigo,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = currentRoute == "profile",
                onClick = {
                    if (currentRoute != "profile") {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricIndigo,
                    selectedTextColor = ElectricIndigo,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentRoute == "settings",
                onClick = {
                    if (currentRoute != "settings") {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricIndigo,
                    selectedTextColor = ElectricIndigo,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
