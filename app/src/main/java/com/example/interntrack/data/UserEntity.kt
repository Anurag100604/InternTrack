package com.example.interntrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String, // UID from Firebase
    val name: String,
    val email: String,
    val password: String = "",
    val college: String = "",
    val course: String = "",
    val branch: String = "",
    val graduationYear: String = "",
    val bio: String = "",
    val skills: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

fun UserEntity.toProfile(): UserProfile {
    return UserProfile(
        name = name,
        email = email,
        college = college,
        course = course,
        branch = branch,
        graduationYear = graduationYear,
        bio = bio,
        skills = skills,
        createdAt = createdAt
    )
}

fun UserProfile.toEntity(id: String): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        college = college,
        course = course,
        branch = branch,
        graduationYear = graduationYear,
        bio = bio,
        skills = skills,
        createdAt = createdAt
    )
}

