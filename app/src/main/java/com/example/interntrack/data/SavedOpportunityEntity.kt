package com.example.interntrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "saved_opportunities")
data class SavedOpportunityEntity(
    @PrimaryKey(autoGenerate = true)
    val dbId: Long = 0,
    val userId: String,
    val id: String,
    val company: String,
    val role: String,
    val location: String,
    val stipend: String,
    val workMode: String,
    val internshipType: String,
    val deadline: String,
    val description: String,
    val requirements: List<String>,
    val skills: List<String>,
    val savedAt: Long = System.currentTimeMillis()
)

fun SavedOpportunityEntity.toOpportunity(): Opportunity {
    return Opportunity(
        id = id,
        company = company,
        role = role,
        location = location,
        stipend = stipend,
        workMode = workMode,
        internshipType = internshipType,
        deadline = deadline,
        description = description,
        requirements = requirements,
        skills = skills
    )
}

fun Opportunity.toSavedEntity(userId: String): SavedOpportunityEntity {
    return SavedOpportunityEntity(
        userId = userId,
        id = id,
        company = company,
        role = role,
        location = location,
        stipend = stipend,
        workMode = workMode,
        internshipType = internshipType,
        deadline = deadline,
        description = description,
        requirements = requirements,
        skills = skills
    )
}

class ListTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return gson.toJson(list)
    }
}
