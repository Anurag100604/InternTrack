package com.example.interntrack.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.interntrack.viewmodel.AuthState
import com.example.interntrack.viewmodel.InternshipViewModel

@Composable
fun SignUpScreen(
    viewModel: InternshipViewModel,
    onSignUpSuccess: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val loginError by viewModel.loginError.collectAsState()
    val authState by viewModel.authState.collectAsState()
    
    val isNameValid = name.isNotBlank()
    val isEmailValid = email.contains("@") && email.contains(".")
    val isPasswordValid = password.length >= 6
    val isConfirmPasswordValid = confirmPassword == password
    
    val isFormValid = isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        AuthHeader(
            title = "Create Account",
            subtitle = "Join InternTrack to start tracking"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (loginError != null) {
            Text(
                text = loginError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        AuthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Full Name",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = email.isNotEmpty() && !isEmailValid,
            supportingText = if (email.isNotEmpty() && !isEmailValid) {
                { Text("Enter a valid email address") }
            } else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = password.isNotEmpty() && !isPasswordValid,
            supportingText = if (password.isNotEmpty() && !isPasswordValid) {
                { Text("Password must be at least 6 characters") }
            } else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPassword.isNotEmpty() && !isConfirmPasswordValid,
            supportingText = if (confirmPassword.isNotEmpty() && !isConfirmPasswordValid) {
                { Text("Passwords do not match") }
            } else null
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (authState == AuthState.LOADING) {
            CircularProgressIndicator()
        } else {
            AuthButton(
                text = "Sign Up",
                onClick = { onSignUpSuccess(name, email, password) },
                enabled = isFormValid
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
