package com.videoChatting.echat.presentation.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.videoChatting.echat.data.local.SessionManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val sessionManager = remember { SessionManager(context) }
    val userProfile = remember { sessionManager.getUserProfile() }

    // Inputs state
    var name by remember { mutableStateOf(userProfile?.name ?: "Guest User") }
    var gender by remember { mutableStateOf("Male") }
    var age by remember { mutableFloatStateOf(22f) }
    var country by remember { mutableStateOf("Global") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var locationStatus by remember { mutableStateOf("Location not detected") }

    // Match Preferences state
    var prefGender by remember { mutableStateOf("All") }
    var filterType by remember { mutableStateOf("country") } // "country" or "km"
    var kmRadius by remember { mutableFloatStateOf(50f) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            locationStatus = "Detecting location..."
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    
                    // Geocode to get Country
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        country = addresses?.firstOrNull()?.countryName ?: "Global"
                        locationStatus = "Detected: $country (${String.format("%.2f", loc.latitude)}, ${String.format("%.2f", loc.longitude)})"
                    } catch (e: Exception) {
                        country = "Global"
                        locationStatus = "Detected Coordinates: (${String.format("%.2f", loc.latitude)}, ${String.format("%.2f", loc.longitude)})"
                    }
                } else {
                    locationStatus = "Failed to detect. Location settings might be off."
                }
            }
        } else {
            Toast.makeText(context, "Location permission is required to match locally", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state) {
        if (state is OnboardingState.Success) {
            onOnboardingFinished()
        } else if (state is OnboardingState.Error) {
            Toast.makeText(context, (state as OnboardingState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. Gender Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your Gender", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        val selected = gender == g
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { gender = g },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = g,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Age Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Your Age", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("${age.toInt()} Years", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = age,
                    onValueChange = { age = it },
                    valueRange = 18f..99f,
                    steps = 81
                )
            }

            // 4. Geolocation Detection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your Location", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Button(
                    onClick = {
                        val permissions = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        val hasPermission = permissions.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (hasPermission) {
                            locationStatus = "Detecting location..."
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    latitude = loc.latitude
                                    longitude = loc.longitude
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                        country = addresses?.firstOrNull()?.countryName ?: "Global"
                                        locationStatus = "Detected: $country (${String.format("%.2f", loc.latitude)}, ${String.format("%.2f", loc.longitude)})"
                                    } catch (e: Exception) {
                                        country = "Global"
                                        locationStatus = "Detected Coordinates: (${String.format("%.2f", loc.latitude)}, ${String.format("%.2f", loc.longitude)})"
                                    }
                                } else {
                                    locationStatus = "Failed to detect automatically."
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(permissions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detect Location via GPS", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text(
                    text = locationStatus,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider()

            // 5. Match Preferences Section Header
            Text("Match Preferences", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

            // 6. Match Gender Preference
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Look For", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Male", "Female", "All").forEach { pref ->
                        val selected = prefGender == pref
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { prefGender = pref },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pref,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 7. Match Filter Preference
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Match Scope", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        "country" to "Same Country",
                        "km" to "Distance Range"
                    ).forEach { (type, label) ->
                        val selected = filterType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { filterType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 8. Distance Radius Slider (Visible only if Distance Range is chosen)
            if (filterType == "km") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Search Radius", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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

            Spacer(modifier = Modifier.height(24.dp))

            // 9. Complete Setup Button
            Button(
                onClick = {
                    viewModel.submitOnboarding(
                        name = name,
                        gender = gender,
                        age = age.toInt(),
                        country = country,
                        longitude = longitude,
                        latitude = latitude,
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
                shape = MaterialTheme.shapes.large,
                enabled = state !is OnboardingState.Loading
            ) {
                if (state is OnboardingState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Complete Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
