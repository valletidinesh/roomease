package com.roomease.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.roomease.app.SupabaseClient
import com.roomease.app.ui.theme.Primary
import com.roomease.app.ui.theme.SurfaceVar
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToCreateRoom: () -> Unit,
    onNavigateToJoinRoom: () -> Unit,
    onNavigateToDashboard: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🏠", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text("RoomEase", style = MaterialTheme.typography.headlineLarge, color = Primary, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (authMode == AuthMode.LOGIN) "Welcome back" else "Create your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))

        // Mode toggle
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceVar)) {
            AuthMode.entries.forEach { mode ->
                val selected = authMode == mode
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Primary else SurfaceVar).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { authMode = mode; errorMsg = null }) {
                        Text(
                            mode.label,
                            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it; errorMsg = null },
            label = { Text("Email") }, leadingIcon = { Icon(Icons.Filled.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it; errorMsg = null },
            label = { Text("Password") }, leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )

        if (authMode == AuthMode.REGISTER) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it; errorMsg = null },
                label = { Text("Confirm Password") }, leadingIcon = { Icon(Icons.Filled.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            )
        }

        errorMsg?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true; errorMsg = null
                    try {
                        when (authMode) {
                            AuthMode.LOGIN -> {
                                // Supabase email sign-in
                                SupabaseClient.client.auth.signInWith(Email) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                                onNavigateToDashboard()
                            }
                            AuthMode.REGISTER -> {
                                if (password != confirmPassword) {
                                    errorMsg = "Passwords do not match"; return@launch
                                }
                                // Supabase email sign-up
                                SupabaseClient.client.auth.signUpWith(Email) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                                // New user — set up or join a room
                                onNavigateToCreateRoom()
                            }
                        }
                    } catch (e: Exception) {
                        errorMsg = e.message ?: "Authentication failed"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else Text(if (authMode == AuthMode.LOGIN) "Sign In" else "Create Account", fontWeight = FontWeight.Bold)
        }

        if (authMode == AuthMode.LOGIN) {
            Spacer(Modifier.height(20.dp))
            Text("Don't have a room yet?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateToJoinRoom,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.MeetingRoom, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join a Room with Code")
            }
        }
    }
}

enum class AuthMode(val label: String) { LOGIN("Sign In"), REGISTER("Register") }
