package com.videoChatting.echat.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.PrivacyTip
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

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.*
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.RaiseRequestDto
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.videoChatting.echat.presentation.theme.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun SettingsScreen(appNavController: NavController? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = SessionManager(context)
    var showDeleteDialog1 by remember { mutableStateOf(false) }
    var showDeleteDialog2 by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBackgroundGradient())
            .padding(top = 25.dp) // Exactly same gap as Profile screen
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Settings", 
            fontSize = 24.sp, // Font size set to 24.sp
            fontWeight = FontWeight.ExtraBold, 
            color = getThemeTextColor()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Fixed title, scrollable content area below
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // App Settings Group
            Text("App Settings", fontSize = 15.sp, color = getThemeSubTextColor(), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Notifications, 
                        title = "Notification Settings", 
                        onClick = { appNavController?.navigate(Screen.Notifications.route) }
                    )
                    HorizontalDivider(color = getThemeGlassBorder())
                    SettingsItem(
                        icon = Icons.Default.Security, 
                        title = "Blocked Users", 
                        subtitle = "Manage blocked profiles",
                        onClick = { appNavController?.navigate("blocked_list") }
                    )
                    HorizontalDivider(color = getThemeGlassBorder())
                    SettingsItem(
                        icon = Icons.Default.Palette, 
                        title = "Theme Settings", 
                        onClick = { appNavController?.navigate(Screen.Theme.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Legal & Support Group
            Text("Legal & Support", fontSize = 15.sp, color = getThemeSubTextColor(), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Info, 
                        title = "About Us", 
                        onClick = { appNavController?.navigate("about_us") }
                    )
                    HorizontalDivider(color = getThemeGlassBorder())
                    SettingsItem(
                        icon = Icons.Default.Help, 
                        title = "Help & Support (FAQs)", 
                        onClick = { appNavController?.navigate("help_support") }
                    )
                    HorizontalDivider(color = getThemeGlassBorder())
                    SettingsItem(
                        icon = Icons.Default.PrivacyTip, 
                        title = "Privacy Policy", 
                        onClick = { appNavController?.navigate(Screen.Privacy.route) }
                    )
                    HorizontalDivider(color = getThemeGlassBorder())
                    SettingsItem(
                        icon = Icons.Default.Description, 
                        title = "Terms of Service", 
                        onClick = { appNavController?.navigate("terms_of_service") }
                    )
                    HorizontalDivider(color = getThemeGlassBorder())
                    SettingsItem(
                        icon = Icons.Default.Email, 
                        title = "Contact Us", 
                        onClick = { appNavController?.navigate("contact_us") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Account Actions Group
            Text("Account Actions", fontSize = 15.sp, color = getThemeSubTextColor(), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
                shape = RoundedCornerShape(20.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.DeleteForever, 
                    title = "Delete Account", 
                    subtitle = "Permanently wipe your profile and data",
                    onClick = { showDeleteDialog1 = true }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }


    
    // Deletion confirmation states
    var isDeleting by remember { mutableStateOf(false) }
    var understandChecked by remember { mutableStateOf(false) }
    
    if (showDeleteDialog1) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog1 = false 
                understandChecked = false
            },
            title = { Text("Delete Account (Step 1 of 2)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Warning: This action is permanent and cannot be undone. By deleting your account:", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Your remaining coin balance will be lost.\n• Your matches, friend lists, and chats will be permanently deleted.\n• You will be logged out immediately.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = understandChecked,
                            onCheckedChange = { understandChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I understand this action is permanent.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog1 = false
                        showDeleteDialog2 = true
                    },
                    enabled = understandChecked
                ) {
                    Text("Continue", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog1 = false 
                    understandChecked = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog2) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog2 = false },
            title = { Text("Final Confirmation (Step 2 of 2)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Are you absolutely sure? This will send a request to our backend database to wipe all your data permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog2 = false
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val retrofit = Retrofit.Builder()
                                    .baseUrl(com.videoChatting.echat.utils.Constants.BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build()
                                val service = retrofit.create(ApiService::class.java)

                                service.raiseRequest(
                                    RaiseRequestDto(
                                        type = "account_deletion",
                                        reason = "User requested account deletion from settings"
                                    )
                                )

                                Toast.makeText(context, "Account deletion request submitted. Data will be wiped.", Toast.LENGTH_LONG).show()
                                
                                // Clear session and redirect to login
                                sessionManager.clearSession()
                                appNavController?.navigate(Screen.Auth.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Yes, Delete Permanently", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog2 = false }) {
                    Text("Cancel")
                }
            }
        )
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
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = getThemeTextColor())
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = getThemeTextColor(), fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = getThemeSubTextColor().copy(alpha = 0.8f))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = getThemeSubTextColor().copy(alpha = 0.5f))
    }
}
