package com.videoChatting.echat.presentation.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.presentation.theme.*

import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.navigation.Screen

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    navController: NavController? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var consentChecked by remember { mutableStateOf(false) }
    
    // Check if token already exists (auto-login)
    val sessionManager = remember { SessionManager(context) }
    LaunchedEffect(key1 = true) {
        if (sessionManager.getAuthToken() != null) {
            onLoginSuccess()
        }
    }

    // Handle authentication state changes
    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onLoginSuccess()
        } else if (state is AuthState.Error) {
            Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    // Statically reference client ID to prevent resource shrinking (R8) from stripping it, with a solid fallback
    val webClientId = remember {
        try {
            context.getString(com.videoChatting.echat.R.string.default_web_client_id)
        } catch (e: Exception) {
            "1020177538461-1j75djeebl4gmm7g0ok1pit25eutm25l.apps.googleusercontent.com"
        }
    }

    // Google Sign-In settings
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
    }
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            } else {
                Toast.makeText(context, "Google identity token missing", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBackgroundGradient())
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        val textColor = getThemeTextColor()
        val subTextColor = getThemeSubTextColor()
        val cardBorder = getThemeGlassBorder()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Talksy",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect securely & instantly",
                fontSize = 16.sp,
                color = subTextColor
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Consent Checkbox and policy links
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Checkbox(
                    checked = consentChecked,
                    onCheckedChange = { consentChecked = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ElectricIndigo,
                        uncheckedColor = subTextColor.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val annotatedText = buildAnnotatedString {
                    append("I agree to the ")
                    pushStringAnnotation(tag = "TOS", annotation = "tos")
                    withStyle(style = SpanStyle(color = ElectricViolet, fontWeight = FontWeight.Bold)) {
                        append("Terms of Service")
                    }
                    pop()
                    append(" and ")
                    pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                    withStyle(style = SpanStyle(color = ElectricViolet, fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                    pop()
                    append(".")
                }

                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(color = subTextColor),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "TOS", start = offset, end = offset)
                            .firstOrNull()?.let {
                                navController?.navigate("terms_of_service")
                            }
                        annotatedText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                            .firstOrNull()?.let {
                                navController?.navigate("privacy")
                            }
                    }
                )
            }

            // Continue with Google Button
            OutlinedButton(
                onClick = {
                    if (consentChecked) {
                        // Sign out first to always show Google account selection dialog
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    } else {
                        Toast.makeText(context, "Please agree to Terms and Privacy Policy first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                border = BorderStroke(1.dp, cardBorder),
                enabled = consentChecked && state !is AuthState.Loading
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(color = ElectricIndigo, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sign in with Google", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue with Guest Button
            Button(
                onClick = {
                    if (consentChecked) {
                        viewModel.loginAsGuest("Guest User")
                    } else {
                        Toast.makeText(context, "Please agree to Terms and Privacy Policy first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color.White
                ),
                enabled = consentChecked && state !is AuthState.Loading
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Login as Guest", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
