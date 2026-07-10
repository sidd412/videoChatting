package com.videoChatting.echat.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    var prefGender by remember { mutableStateOf("All") }
    var minAge by remember { mutableStateOf(18f) }
    var maxAge by remember { mutableStateOf(99f) }
    var maxDistance by remember { mutableStateOf(50f) }
    
    val genders = listOf("All", "Male", "Female")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Preferences", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("I want to chat with", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genders.forEach { gender ->
                    FilterChip(
                        selected = prefGender == gender,
                        onClick = { prefGender = gender },
                        label = { Text(gender) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Age Range: ${minAge.toInt()} - ${maxAge.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            // A basic slider for max age for now, since Compose material3 RangeSlider can be tricky without proper setup
            Slider(
                value = maxAge,
                onValueChange = { maxAge = it },
                valueRange = 18f..99f,
                steps = 81
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Maximum Distance: ${maxDistance.toInt()} km", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Slider(
                value = maxDistance,
                onValueChange = { maxDistance = it },
                valueRange = 5f..500f,
                steps = 99
            )

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { 
                    // TODO: Save to SessionManager and backend
                    navController.popBackStack() 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
