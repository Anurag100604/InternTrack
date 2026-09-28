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
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSetupScreen(
    onComplete: (name: String, college: String, course: String, branch: String, gradYear: String, bio: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var gradYear by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    
    val isFormValid = college.isNotBlank() && course.isNotBlank() && gradYear.isNotBlank()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        AuthHeader(
            title = "Profile Setup",
            subtitle = "Tell us about your background"
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Full Name",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        AuthTextField(
            value = college,
            onValueChange = { college = it },
            label = "College / University",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthTextField(
                value = course,
                onValueChange = { course = it },
                label = "Course (e.g. B.Tech)",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            AuthTextField(
                value = branch,
                onValueChange = { branch = it },
                label = "Branch (e.g. CSE)",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AuthTextField(
            value = gradYear,
            onValueChange = { gradYear = it },
            label = "Graduation Year",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Short Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = MaterialTheme.shapes.medium,
            maxLines = 4
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AuthButton(
            text = "Complete Setup",
            onClick = { onComplete(name, college, course, branch, gradYear, bio) },
            enabled = isFormValid
        )
    }
}

