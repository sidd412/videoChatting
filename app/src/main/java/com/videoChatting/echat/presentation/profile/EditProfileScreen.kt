package com.videoChatting.echat.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.videoChatting.echat.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Not Specified") }
    var dob by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    
    // Local Uri state for instant high-performance preview rendering
    var selectedLocalUri by remember { mutableStateOf<Uri?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    // Gallery Image Picker launcher converting image to Base64 (with compression)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedLocalUri = it // Instantly set the local Uri to render the preview
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Compress and scale down to max 350px
                    val maxDimension = 350
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                        val ratio = width.toFloat() / height.toFloat()
                        val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                        val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                        android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                    } else {
                        originalBitmap
                    }

                    val outputStream = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val bytes = outputStream.toByteArray()
                    val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    avatarUrl = "data:$mimeType;base64,$base64String"
                } else {
                    Toast.makeText(context, "Could not decode image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load initial values once profile is loaded
    LaunchedEffect(userProfile) {
        userProfile?.let {
            name = it.name
            email = it.email ?: ""
            gender = it.gender ?: "Not Specified"
            dob = it.dob ?: ""
            bio = it.bio ?: ""
            contactNumber = it.contactNumber ?: ""
            avatarUrl = it.avatar ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar Edit Box - larger outer box to prevent icon cutting
                Box(
                    modifier = Modifier
                        .size(125.dp) // larger outer size to prevent clip
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, getThemeGlassBorder(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedLocalUri != null) {
                            // Instant high-fidelity local rendering
                            AsyncImage(
                                model = selectedLocalUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Picture Default",
                                    modifier = Modifier.size(54.dp),
                                    tint = getThemeTextColor()
                                )
                            }
                        }
                    }

                    // Edit camera badge overlay - aligned to bottom-right of the 125.dp outer box
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(ElectricIndigo)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Edit Photo",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profile Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = getThemeTextColor())
                        Spacer(modifier = Modifier.height(16.dp))

                        // Name (Editable, Single Line)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getThemeTextColor(),
                                unfocusedTextColor = getThemeTextColor()
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Email (Non-Editable, Single Line)
                        OutlinedTextField(
                            value = email,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            singleLine = true,
                            label = { Text("Email (Google Account)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = getThemeTextColor().copy(alpha = 0.6f),
                                disabledBorderColor = getThemeGlassBorder().copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Gender Selection Dropdown (Single Line)
                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = !genderExpanded }
                        ) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = getThemeTextColor(),
                                    unfocusedTextColor = getThemeTextColor()
                                )
                            )
                            ExposedDropdownMenu(
                                  expanded = genderExpanded,
                                  onDismissRequest = { genderExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Male") },
                                    onClick = {
                                        gender = "Male"
                                        genderExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Female") },
                                    onClick = {
                                        gender = "Female"
                                        genderExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Not Specified") },
                                    onClick = {
                                        gender = "Not Specified"
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // DOB (Editable, Single Line)
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getThemeTextColor(),
                                unfocusedTextColor = getThemeTextColor()
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Contact Number (Editable, Optional, Single Line)
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text("Contact Number (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getThemeTextColor(),
                                unfocusedTextColor = getThemeTextColor()
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Bio (Editable, Single Line)
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getThemeTextColor(),
                                unfocusedTextColor = getThemeTextColor()
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        viewModel.updateProfile(
                            name = name,
                            gender = gender,
                            age = userProfile?.age,
                            dob = dob,
                            bio = bio,
                            contactNumber = contactNumber,
                            avatar = avatarUrl,
                            onSuccess = {
                                isSaving = false
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { err ->
                                isSaving = false
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
