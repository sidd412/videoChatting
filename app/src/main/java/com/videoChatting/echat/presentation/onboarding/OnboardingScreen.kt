package com.videoChatting.echat.presentation.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.presentation.theme.*
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

    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state is OnboardingState.Success) {
            onOnboardingFinished()
        } else if (state is OnboardingState.Error) {
            Toast.makeText(context, (state as OnboardingState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(CyberMidnight)) {
                LinearProgressIndicator(
                    progress = { (currentStep) / 4f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = ElectricIndigo,
                    trackColor = GlassBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    
                    Text(
                        text = "Step ${currentStep + 1} of 5",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    // Skip preferences option (only in preferences step)
                    if (currentStep == 4) {
                        TextButton(onClick = {
                            viewModel.submitOnboarding(
                                name = name,
                                gender = gender,
                                age = age.toInt(),
                                country = country,
                                longitude = longitude,
                                latitude = latitude,
                                prefGender = "All",
                                prefMinAge = 18,
                                prefMaxAge = 99,
                                filterType = "country",
                                kmRadius = 50
                            )
                        }) {
                            Text("Skip", fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PremiumBackgroundGradient)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "stepAnimation"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when (step) {
                        0 -> {
                            // Step 0: Welcome Screen
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(GlassBackground)
                                    .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👋", fontSize = 56.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Welcome to eChat!",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                            Text(
                                text = "Let's personalize your card so you can match with interesting people worldwide in seconds.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { currentStep = 1 },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                            ) {
                                Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                        1 -> {
                            // Step 1: Name Input
                            Text(
                                text = "What should we call you?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                            Text(
                                text = "Choose a display name that others will see during chat.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { if (it.length <= 20) name = it },
                                        label = { Text("Display Name", color = Color.White.copy(alpha = 0.6f)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricIndigo,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedLabelColor = ElectricIndigo,
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${name.length}/20 characters",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.align(Alignment.End),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                enabled = name.isNotBlank()
                            ) {
                                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        2 -> {
                            // Step 2: Gender & Age
                            Text(
                                text = "Tell us about yourself",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Text("I identify as", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        listOf(
                                            "Male" to "👨 Male",
                                            "Female" to "👩 Female",
                                            "Other" to "🧑 Other"
                                        ).forEach { (g, label) ->
                                            val selected = gender == g
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (selected) ElectricIndigo else GlassBackground)
                                                    .border(
                                                        BorderStroke(
                                                            width = 1.dp,
                                                            color = if (selected) ElectricViolet else GlassBorder
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { gender = g },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("My age is", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text(
                                            text = "${age.toInt()} Years",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = CyberCyan
                                        )
                                    }
                                    Slider(
                                        value = age,
                                        onValueChange = { age = it },
                                        valueRange = 18f..99f,
                                        steps = 81,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyberCyan,
                                            activeTrackColor = CyberCyan,
                                            inactiveTrackColor = GlassBorder
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { currentStep = 3 },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                            ) {
                                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        3 -> {
                            // Step 3: Location
                            Text(
                                text = "Where are you located?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Pulse / scan effect representation
                                    Box(
                                        modifier = Modifier.size(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                        val pulseScale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.4f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "pulse"
                                        )
                                        val pulseAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.6f,
                                            targetValue = 0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "pulse"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .scale(pulseScale)
                                                .clip(CircleShape)
                                                .background(CyberCyan.copy(alpha = pulseAlpha))
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(CircleShape)
                                                .background(CyberCyan),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                        }
                                    }

                                    Text(
                                        text = locationStatus,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )

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
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                    ) {
                                        Text("Find My GPS Location", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { currentStep = 4 },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                            ) {
                                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        4 -> {
                            // Step 4: Match Preferences
                            Text(
                                text = "Who are you looking for?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Text("Prefer matching with", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                                                    .background(if (selected) ElectricIndigo else GlassBackground)
                                                    .border(
                                                        BorderStroke(
                                                            width = 1.dp,
                                                            color = if (selected) ElectricViolet else GlassBorder
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { prefGender = pref },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Location Range", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                                                    .background(if (selected) CyberCyan else GlassBackground)
                                                    .border(
                                                        BorderStroke(
                                                            width = 1.dp,
                                                            color = if (selected) CyberCyan.copy(alpha = 0.6f) else GlassBorder
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { filterType = type },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = Color.White,
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
                                            Text("Max Distance", fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text("${kmRadius.toInt()} km", fontWeight = FontWeight.Bold, color = CyberCyan)
                                        }
                                        Slider(
                                            value = kmRadius,
                                            onValueChange = { kmRadius = it },
                                            valueRange = 10f..500f,
                                            steps = 49,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CyberCyan,
                                                activeTrackColor = CyberCyan,
                                                inactiveTrackColor = GlassBorder
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
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
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                enabled = state !is OnboardingState.Loading
                            ) {
                                if (state is OnboardingState.Loading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Complete Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
