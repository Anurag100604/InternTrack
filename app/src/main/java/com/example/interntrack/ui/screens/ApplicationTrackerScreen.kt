package com.example.interntrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.interntrack.data.ApplicationStatus
import com.example.interntrack.data.InternshipEntity
import com.example.interntrack.ui.components.StatusBadge
import com.example.interntrack.viewmodel.InternshipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationTrackerScreen(
    applicationId: Long,
    viewModel: InternshipViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val application = viewModel.allApplications.collectAsState().value.find { it.id == applicationId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    if (application == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Application not found")
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Application") },
            text = { Text("Are you sure you want to delete this application? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteApplication(application)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status") },
            text = {
                Column {
                    ApplicationStatus.entries.forEach { status ->
                        TextButton(
                            onClick = {
                                viewModel.updateStatus(application.id, status)
                                showStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(status.icon, contentDescription = null, tint = status.contentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(status.label, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(application.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Company and Role Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(application.company, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(application.role, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Timeline Section
            Text("Application Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ApplicationTimeline(currentStatus = application.status)

            // Info Section
            Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = application.location)
                    DetailRow(icon = Icons.Default.Work, label = "Work Mode", value = application.workMode)
                    DetailRow(icon = Icons.Default.Payments, label = "Stipend", value = application.stipend)
                    DetailRow(icon = Icons.Default.CalendarMonth, label = "Applied On", value = application.applicationDate)
                    if (application.deadline.isNotBlank()) {
                        DetailRow(icon = Icons.Default.Event, label = "Deadline", value = application.deadline)
                    }
                    if (application.source.isNotBlank()) {
                        DetailRow(icon = Icons.Default.Link, label = "Source", value = application.source)
                    }
                }
            }

            // Notes Section
            if (application.notes.isNotBlank()) {
                Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = application.notes,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showStatusDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Status", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ApplicationTimeline(currentStatus: ApplicationStatus) {
    val allSteps = listOf(
        ApplicationStatus.APPLIED,
        ApplicationStatus.SHORTLISTED,
        ApplicationStatus.INTERVIEW,
        ApplicationStatus.SELECTED
    )

    val isRejected = currentStatus == ApplicationStatus.REJECTED
    
    val displayedSteps = if (isRejected) {
        listOf(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED)
    } else {
        allSteps
    }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        displayedSteps.forEachIndexed { index, step ->
            val isCompleted = if (isRejected) {
                step == ApplicationStatus.APPLIED || step == ApplicationStatus.REJECTED
            } else {
                allSteps.indexOf(step) <= allSteps.indexOf(currentStatus)
            }
            
            val isCurrent = step == currentStatus

            TimelineStep(
                status = step,
                isCompleted = isCompleted,
                isCurrent = isCurrent,
                isLast = index == displayedSteps.size - 1,
                lineColor = if (isRejected && step == ApplicationStatus.APPLIED) ApplicationStatus.REJECTED.containerColor else step.containerColor
            )
        }
    }
}

@Composable
fun TimelineStep(
    status: ApplicationStatus,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    lineColor: Color
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (isCompleted) status.containerColor else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isCurrent) BorderStroke(2.dp, status.contentColor) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = status.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isCompleted) status.contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(if (isCompleted) lineColor else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 32.dp)) {
            Text(
                text = status.label,
                style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            val statusDescription = when(status) {
                ApplicationStatus.APPLIED -> "Your application has been submitted successfully."
                ApplicationStatus.SHORTLISTED -> "Great! You've moved to the initial shortlist."
                ApplicationStatus.INTERVIEW -> "Interview rounds are in progress."
                ApplicationStatus.SELECTED -> "Congratulations! You've received an offer."
                ApplicationStatus.REJECTED -> "Application was not successful this time."
            }
            
            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            if (isCurrent) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = status.containerColor
                ) {
                    Text(
                        text = "CURRENT STEP",
                        style = MaterialTheme.typography.labelSmall,
                        color = status.contentColor,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "$label:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(text = if (value.isBlank()) "N/A" else value, style = MaterialTheme.typography.bodyMedium)
    }
}
