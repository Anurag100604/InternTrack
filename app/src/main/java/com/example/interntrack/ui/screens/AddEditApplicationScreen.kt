package com.example.interntrack.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.interntrack.data.ApplicationStatus
import com.example.interntrack.data.InternshipEntity
import com.example.interntrack.viewmodel.InternshipViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditApplicationScreen(
    applicationId: Long?,
    viewModel: InternshipViewModel,
    onBack: () -> Unit
) {
    val existingApp = if (applicationId != null) {
        viewModel.allApplications.collectAsState().value.find { it.id == applicationId }
    } else null

    var company by remember { mutableStateOf(existingApp?.company ?: "") }
    var role by remember { mutableStateOf(existingApp?.role ?: "") }
    var location by remember { mutableStateOf(existingApp?.location ?: "") }
    var workMode by remember { mutableStateOf(existingApp?.workMode ?: "") }
    var stipend by remember { mutableStateOf(existingApp?.stipend ?: "") }
    var applicationDate by remember {
        mutableStateOf(existingApp?.applicationDate ?: SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))
    }
    var deadline by remember { mutableStateOf(existingApp?.deadline ?: "") }
    var source by remember { mutableStateOf(existingApp?.source ?: "") }
    var jobUrl by remember { mutableStateOf(existingApp?.jobUrl ?: "") }
    var notes by remember { mutableStateOf(existingApp?.notes ?: "") }
    var status by remember { mutableStateOf(existingApp?.status ?: ApplicationStatus.APPLIED) }

    var statusExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (applicationId == null) "Add Application" else "Edit Application") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Internship Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company Name *") },
                placeholder = { Text("e.g. Google, Amazon") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Job Role *") },
                placeholder = { Text("e.g. Android Developer Intern") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                OutlinedTextField(
                    value = workMode,
                    onValueChange = { workMode = it },
                    label = { Text("Work Mode") },
                    placeholder = { Text("Remote/Hybrid") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = stipend,
                onValueChange = { stipend = it },
                label = { Text("Stipend") },
                placeholder = { Text("e.g. ₹50,000/mo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Tracking Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = applicationDate,
                    onValueChange = { applicationDate = it },
                    label = { Text("Application Date") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Application Source") },
                placeholder = { Text("LinkedIn, Company Portal, etc.") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobUrl,
                onValueChange = { jobUrl = it },
                label = { Text("Job URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
            )

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(status.icon, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    ApplicationStatus.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            onClick = {
                                status = s
                                statusExpanded = false
                            },
                            leadingIcon = { Icon(s.icon, contentDescription = null) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (company.isNotBlank() && role.isNotBlank()) {
                        if (applicationId == null) {
                            viewModel.addApplication(
                                company = company,
                                role = role,
                                location = location,
                                workMode = workMode,
                                stipend = stipend,
                                applicationDate = applicationDate,
                                deadline = deadline,
                                source = source,
                                jobUrl = jobUrl,
                                status = status,
                                notes = notes
                            )
                        } else {
                            existingApp?.let {
                                viewModel.updateApplication(
                                    it.copy(
                                        company = company,
                                        role = role,
                                        location = location,
                                        workMode = workMode,
                                        stipend = stipend,
                                        applicationDate = applicationDate,
                                        deadline = deadline,
                                        source = source,
                                        jobUrl = jobUrl,
                                        status = status,
                                        notes = notes
                                    )
                                )
                            }
                        }
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                enabled = company.isNotBlank() && role.isNotBlank()
            ) {
                Text(
                    text = if (applicationId == null) "Save Application" else "Update Application",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (company.isBlank() || role.isBlank()) {
                Text(
                    text = "* Company and Role are required fields",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
