package com.videoChatting.echat.presentation.auth

import android.widget.Toast
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.presentation.theme.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    navController: NavController? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var consentChecked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
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

    // Credential Manager - modern Google Sign-In (handles Play App Signing correctly)
    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = "1020177538461-1j75djeebl4gmm7g0ok1pit25eutm25l.apps.googleusercontent.com"

    val handleGoogleSignIn: () -> Unit = {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.loginWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialException) {
                Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Sign-In error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
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
                        handleGoogleSignIn()
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
