package com.example.interntrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "applications")
data class InternshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val company: String,
    val role: String,
    val location: String = "",
    val workMode: String = "",
    val stipend: String = "",
    val applicationDate: String = "",
    val deadline: String = "",
    val source: String = "",
    val jobUrl: String = "",
    val interviewDate: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false,
    val documentId: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> {
        val appDate = applicationDate.ifBlank { "" }
        return mapOf(
            "documentId" to documentId,
            "userId" to userId,
            "company" to company,
            "role" to role,
            "location" to location,
            "workMode" to workMode,
            "stipend" to stipend,
            "appliedDate" to appDate,
            "applicationDate" to appDate,
            "deadline" to deadline,
            "source" to source,
            "jobUrl" to jobUrl,
            "interviewDate" to interviewDate,
            "status" to status.name,
            "notes" to notes,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromFirestoreMap(docId: String, map: Map<String, Any?>): InternshipEntity {
            val appDate = (map["appliedDate"] as? String)
                ?: (map["applicationDate"] as? String)
                ?: ""
            val statusStr = map["status"] as? String ?: ApplicationStatus.APPLIED.name

            return InternshipEntity(
                id = 0,
                userId = map["userId"] as? String ?: "",
                company = map["company"] as? String ?: "",
                role = map["role"] as? String ?: "",
                location = map["location"] as? String ?: "",
                workMode = map["workMode"] as? String ?: "",
                stipend = map["stipend"] as? String ?: "",
                applicationDate = appDate,
                deadline = map["deadline"] as? String ?: "",
                source = map["source"] as? String ?: "",
                jobUrl = map["jobUrl"] as? String ?: "",
                interviewDate = map["interviewDate"] as? String ?: "",
                status = ApplicationStatus.fromString(statusStr),
                notes = map["notes"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis(),
                isDemo = false,
                documentId = docId
            )
        }
    }
}

class StatusTypeConverters {
    @TypeConverter
    fun fromStatus(status: ApplicationStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): ApplicationStatus {
        return ApplicationStatus.fromString(value)
    }
}

