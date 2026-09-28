package com.example.interntrack.data

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val college: String = "",
    val course: String = "",
    val branch: String = "",
    val graduationYear: String = "",
    val bio: String = "",
    val skills: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val initials: String
        get() {
            if (name.isBlank()) return "ST"
            val parts = name.trim().split("\\s+".toRegex())
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
                parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
                else -> "ST"
            }
        }

    fun toFirestoreMap(uid: String): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "college" to college,
            "course" to course,
            "branch" to branch,
            "graduationYear" to graduationYear,
            "bio" to bio,
            "skills" to skills,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                college = map["college"] as? String ?: "",
                course = map["course"] as? String ?: "",
                branch = map["branch"] as? String ?: "",
                graduationYear = map["graduationYear"] as? String ?: "",
                bio = map["bio"] as? String ?: "",
                skills = map["skills"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

