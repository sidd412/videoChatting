package com.videoChatting.echat.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.navigation.Screen
import com.videoChatting.echat.presentation.theme.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ProfileScreen(
    appNavController: NavController? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBackgroundGradient())
            .padding(top = 25.dp) // Exactly 25dp padding from top status bar
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "My Profile", 
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold, 
            color = getThemeTextColor()
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Fixed title, scrollable content area below
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Account Details",
                fontSize = 15.sp,
                color = getThemeSubTextColor(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Rectangular Card (Rounded Corners, Glassmorphic style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(24.dp))
                    .clickable { appNavController?.navigate(Screen.EditProfile.route) },
                colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar with Edit Button Overlay on Left
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatar = userProfile?.avatar ?: ""
                        if (avatar.isNotEmpty()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person, 
                                    contentDescription = "Profile Picture", 
                                    modifier = Modifier.size(44.dp), 
                                    tint = getThemeTextColor()
                                )
                            }
                        }
                        // Overlay Edit Icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(ElectricIndigo)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit, 
                                contentDescription = "Edit Avatar", 
                                modifier = Modifier.size(12.dp), 
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name (with Gender suffix) & Email details
                    Column(modifier = Modifier.weight(1f)) {
                        val genderSuffix = when (userProfile?.gender) {
                            "Male" -> " (M)"
                            "Female" -> " (F)"
                            else -> ""
                        }
                        val nameWithGender = "${userProfile?.name ?: "Guest User"}$genderSuffix"

                        Text(
                            text = nameWithGender, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = getThemeTextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
//                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userProfile?.email ?: "No email registered", 
                            fontSize = 12.sp, 
                            color = getThemeSubTextColor().copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
//                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Coin Balance: ${userProfile?.coinsBalance ?: 0}", 
                            fontSize = 12.sp,
                            color = ElectricViolet,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Single block containing Match Preferences, Wallet, Purchases
            Text(
                text = "Manage Profile & Wallet", 
                fontSize = 15.sp, 
                color = getThemeSubTextColor(), 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                ProfileSettingsItem(
                    icon = Icons.Default.Tune, 
                    title = "Match Preferences",
                    onClick = { appNavController?.navigate(Screen.Preferences.route) }
                )
                HorizontalDivider(color = getThemeGlassBorder())
                ProfileSettingsItem(
                    icon = Icons.Default.Share, 
                    title = "Invite & Contacts (Get 50 🪙)",
                    onClick = { appNavController?.navigate(Screen.InviteAndContacts.route) }
                )
                HorizontalDivider(color = getThemeGlassBorder())
                ProfileSettingsItem(
                    icon = Icons.Default.MonetizationOn, 
                    title = "My Wallet",
                    onClick = { appNavController?.navigate("wallet") }
                )
                HorizontalDivider(color = getThemeGlassBorder())
                ProfileSettingsItem(
                    icon = Icons.Default.History, 
                    title = "My Purchases",
                    onClick = { appNavController?.navigate(Screen.PurchaseHistory.route) }
                )
            }
        }

        Spacer(modifier = Modifier.height(120.dp))

        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = getThemeGlassBackground())
        ) {
            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = getThemeTextColor())
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout from Talksy?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout {
                        appNavController?.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = title, 
            tint = getThemeTextColor()
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title, 
            fontSize = 15.sp, 
            color = getThemeTextColor(),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, 
            contentDescription = null, 
            tint = getThemeSubTextColor().copy(alpha = 0.5f)
        )
    }
}
