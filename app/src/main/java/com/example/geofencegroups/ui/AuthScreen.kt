package com.example.geofencegroups.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.ui.components.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space

@Composable
fun AuthScreen(c: AppContainer) {
    val vm: AuthViewModel = viewModel(factory = vmFactory { AuthViewModel(c) })
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val notice by vm.notice.collectAsState()
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(Orbit.Paper).statusBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Spacer(Modifier.height(Space.xl))
        OrbitMark(Modifier.size(56.dp))
        Text("Orbit", style = MaterialTheme.typography.displayMedium)
        Text("A quieter way to keep a group together.", style = MaterialTheme.typography.bodyLarge, color = Orbit.ForestSoft)
        Spacer(Modifier.height(Space.md))
        Text(if (signUp) "Create your account" else "Welcome back", style = MaterialTheme.typography.titleLarge)
        if (signUp) OrbitField(name, { name = it }, "Name your group will see")
        OrbitField(email, { email = it }, "Email", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OrbitField(password, { password = it }, "Password", supporting = if (signUp) "At least 6 characters" else null,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        error?.let { ErrorBanner(it) }
        notice?.let { Text(it, color = Orbit.Moss, style = MaterialTheme.typography.bodyMedium) }
        PrimaryButton(if (signUp) "Create account" else "Sign in", onClick = { vm.submit(signUp, name, email, password) },
            modifier = Modifier.fillMaxWidth(), loading = busy)
        if (!signUp) QuietButton("Forgot password?", onClick = { vm.resetPassword(email) })
        QuietButton(if (signUp) "I already have an account" else "New to Orbit? Create an account", onClick = { signUp = !signUp })
        Spacer(Modifier.height(Space.xl + navBarPadding()))
    }
}
