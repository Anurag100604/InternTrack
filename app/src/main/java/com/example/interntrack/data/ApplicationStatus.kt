package com.example.interntrack.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ApplicationStatus(
    val label: String,
    val icon: ImageVector,
    private val lightContainer: Color,
    private val lightContent: Color,
    private val darkContainer: Color,
    private val darkContent: Color
) {
    APPLIED(
        label = "Applied",
        icon = Icons.Default.Schedule,
        lightContainer = Color(0xFFF1F5F9),
        lightContent = Color(0xFF334155),
        darkContainer = Color(0xFF1E293B),
        darkContent = Color(0xFFCBD5E1)
    ),
    SHORTLISTED(
        label = "Shortlisted",
        icon = Icons.Default.Star,
        lightContainer = Color(0xFFF5F3FF),
        lightContent = Color(0xFF6D28D9),
        darkContainer = Color(0xFF3B0764),
        darkContent = Color(0xFFDDD6FE)
    ),
    INTERVIEW(
        label = "Interview",
        icon = Icons.Default.HourglassEmpty,
        lightContainer = Color(0xFFFFFBEB),
        lightContent = Color(0xFFB45309),
        darkContainer = Color(0xFF451A03),
        darkContent = Color(0xFFFDE68A)
    ),
    SELECTED(
        label = "Selected",
        icon = Icons.Default.CheckCircle,
        lightContainer = Color(0xFFF0FDF4),
        lightContent = Color(0xFF15803D),
        darkContainer = Color(0xFF052E16),
        darkContent = Color(0xFFBBF7D0)
    ),
    REJECTED(
        label = "Rejected",
        icon = Icons.Default.ThumbDown,
        lightContainer = Color(0xFFFFF1F2),
        lightContent = Color(0xFFBE123C),
        darkContainer = Color(0xFF4C0519),
        darkContent = Color(0xFFFECDD3)
    );

    val containerColor: Color get() = lightContainer
    val contentColor: Color get() = lightContent

    fun getContainerColor(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer
    fun getContentColor(isDark: Boolean): Color = if (isDark) darkContent else lightContent

    companion object {
        fun fromString(name: String?): ApplicationStatus {
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true)
            } ?: APPLIED
        }
    }
}
