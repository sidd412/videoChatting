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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    navController: NavController,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val currentPrefGender by viewModel.prefGender.collectAsState()
    val currentPrefMinAge by viewModel.prefMinAge.collectAsState()
    val currentPrefMaxAge by viewModel.prefMaxAge.collectAsState()
    val currentFilterType by viewModel.filterType.collectAsState()
    val currentKmRadius by viewModel.kmRadius.collectAsState()

    // Local state variables initialized from ViewModel states
    var prefGender by remember { mutableStateOf("All") }
    var filterType by remember { mutableStateOf("country") }
    var kmRadius by remember { mutableFloatStateOf(50f) }

    // Sync local state when ViewModel loads details
    LaunchedEffect(currentPrefGender, currentFilterType, currentKmRadius) {
        prefGender = currentPrefGender
        filterType = currentFilterType
        kmRadius = currentKmRadius.toFloat()
    }

    LaunchedEffect(state) {
        if (state is PreferencesState.Success) {
            Toast.makeText(context, "Preferences saved successfully", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        } else if (state is PreferencesState.Error) {
            Toast.makeText(context, (state as PreferencesState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "These soft preferences help customize who you match with. If no matching candidate is found, you will match with anyone available to reduce waiting time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Preferred Gender
                    Text("Prefer matching with", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            "Male" to "👨 Male",
                            "Female" to "👩 Female",
                            "All" to "🌍 All"
                        ).forEach { (pref, label) ->
                            val selected = prefGender == pref
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { prefGender = pref },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location Scope
                    Text("Location Range", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            "country" to "Same Country",
                            "km" to "Distance Scope"
                        ).forEach { (type, label) ->
                            val selected = filterType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { filterType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (filterType == "km") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Max Distance", fontWeight = FontWeight.SemiBold)
                            Text("${kmRadius.toInt()} km", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Slider(
                            value = kmRadius,
                            onValueChange = { kmRadius = it },
                            valueRange = 10f..500f,
                            steps = 49
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.savePreferences(
                        prefGender = prefGender,
                        prefMinAge = 18,
                        prefMaxAge = 99,
                        filterType = filterType,
                        kmRadius = kmRadius.toInt()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = state !is PreferencesState.Loading
            ) {
                if (state is PreferencesState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Preferences", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
