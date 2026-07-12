package com.videoChatting.echat.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.navigation.Screen

@Composable
fun SettingsScreen(appNavController: NavController? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))

        // Preferences Group
        Text("Account Preferences", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.Tune, 
            title = "Match Preferences", 
            subtitle = "Age, Gender, Location",
            onClick = { appNavController?.navigate(Screen.Preferences.route) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Wallet & Billing Group
        Text("Wallet & Billing", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.MonetizationOn, 
            title = "My Wallet", 
            subtitle = "View balance and buy coins",
            onClick = { appNavController?.navigate("wallet") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.History, 
            title = "My Purchases", 
            subtitle = "View transaction history",
            onClick = { appNavController?.navigate(Screen.PurchaseHistory.route) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App Settings Group
        Text("App Settings", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.Notifications, 
            title = "Notification Settings", 
            onClick = { appNavController?.navigate(Screen.Notifications.route) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.Security, 
            title = "Privacy Settings", 
            onClick = { appNavController?.navigate(Screen.Privacy.route) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.Security, 
            title = "Blocked Users", 
            subtitle = "Manage blocked profiles",
            onClick = { appNavController?.navigate("blocked_list") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Default.Palette, 
            title = "Theme Settings", 
            onClick = { appNavController?.navigate(Screen.Theme.route) }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
