package com.videoChatting.echat.presentation.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.presentation.theme.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

fun getInstalledAppSha1(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val cert = signatures?.firstOrNull()?.toByteArray() ?: return "No Certificate Found"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(cert)
        digest.joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
        "Error: ${e.localizedMessage}"
    }
}

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
    var logoTapCount by remember { mutableStateOf(0) }
    var showSha1Dialog by remember { mutableStateOf(false) }
    
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

    val webClientId = "1020177538461-1j75djeebl4gmm7g0ok1pit25eutm25l.apps.googleusercontent.com"

    // Legacy Google Sign-In as reliable fallback
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
    }
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    val legacySignInLauncher = rememberLauncherForActivityResult(
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
        } catch (e: ApiException) {
            Toast.makeText(context, "Google Sign-In Error (Code: ${e.statusCode})", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Credential Manager - modern Google Sign-In
    val credentialManager = remember { CredentialManager.create(context) }

    val handleGoogleSignIn: () -> Unit = {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .setNonce(null)
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
                    // Fallback to legacy
                    googleSignInClient.signOut().addOnCompleteListener {
                        legacySignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                // User closed the picker — do nothing
            } catch (e: NoCredentialException) {
                // Credential Manager has no saved credential -> Launch interactive Google Sign-In intent
                googleSignInClient.signOut().addOnCompleteListener {
                    legacySignInLauncher.launch(googleSignInClient.signInIntent)
                }
            } catch (e: GetCredentialException) {
                // Any other credential exception -> Fallback to legacy intent
                googleSignInClient.signOut().addOnCompleteListener {
                    legacySignInLauncher.launch(googleSignInClient.signInIntent)
                }
            } catch (e: Exception) {
                googleSignInClient.signOut().addOnCompleteListener {
                    legacySignInLauncher.launch(googleSignInClient.signInIntent)
                }
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
                color = textColor,
                modifier = Modifier.clickable {
                    logoTapCount++
                    if (logoTapCount >= 5) {
                        logoTapCount = 0
                        showSha1Dialog = true
                    }
                }
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

    if (showSha1Dialog) {
        val currentSha1 = remember { getInstalledAppSha1(context) }
        AlertDialog(
            onDismissRequest = { showSha1Dialog = false },
            title = { Text("App Signing Certificate SHA-1", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This is the exact certificate hash of the running app:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = currentSha1,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricIndigo
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("App SHA-1", currentSha1)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showSha1Dialog = false
                    }
                ) {
                    Text("Copy SHA-1")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSha1Dialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
