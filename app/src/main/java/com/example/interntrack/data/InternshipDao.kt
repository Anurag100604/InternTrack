package com.example.interntrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InternshipDao {

    @Query("SELECT * FROM applications WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllApplicationsFlow(userId: String): Flow<List<InternshipEntity>>

    @Query("SELECT * FROM applications WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllApplications(userId: String): List<InternshipEntity>

    @Query("SELECT * FROM applications WHERE id = :id AND userId = :userId LIMIT 1")
    fun getApplicationByIdFlow(id: Long, userId: String): Flow<InternshipEntity?>

    @Query("SELECT * FROM applications WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getApplicationById(id: Long, userId: String): InternshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: InternshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(applications: List<InternshipEntity>)

    @Update
    suspend fun updateApplication(application: InternshipEntity)

    @Delete
    suspend fun deleteApplication(application: InternshipEntity)

    @Query("DELETE FROM applications WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Long, userId: String)

    @Query("DELETE FROM applications WHERE isDemo = 1 AND userId = :userId")
    suspend fun deleteDemoApplications(userId: String)

    @Query("SELECT COUNT(*) FROM applications WHERE userId = :userId")
    suspend fun getApplicationsCount(userId: String): Int

    @Transaction
    suspend fun syncUserApplications(userId: String, remoteApps: List<InternshipEntity>) {
        val localApps = getAllApplications(userId)
        val localMapByDocId = localApps.filter { it.documentId.isNotBlank() }.associateBy { it.documentId }
        val localMapByKey = localApps.associateBy { "${it.company}_${it.role}_${it.createdAt}" }

        val appsToInsert = mutableListOf<InternshipEntity>()
        val seenDocIds = mutableSetOf<String>()

        for (remote in remoteApps) {
            if (remote.documentId.isNotBlank() && remote.documentId in seenDocIds) {
                continue
            }
            if (remote.documentId.isNotBlank()) {
                seenDocIds.add(remote.documentId)
            }

            val localMatch = (if (remote.documentId.isNotBlank()) localMapByDocId[remote.documentId] else null)
                ?: localMapByKey["${remote.company}_${remote.role}_${remote.createdAt}"]

            if (localMatch != null) {
                appsToInsert.add(remote.copy(id = localMatch.id))
            } else {
                appsToInsert.add(remote.copy(id = 0))
            }
        }

        val remoteDocIds = remoteApps.mapNotNull { it.documentId.ifBlank { null } }.toSet()
        val toDelete = localApps.filter { 
            (it.documentId.isNotBlank() && it.documentId !in remoteDocIds) ||
            (it.documentId.isNotBlank() && localApps.count { item -> item.documentId == it.documentId } > 1)
        }
        
        toDelete.forEach { deleteApplication(it) }
        insertAll(appsToInsert)
    }

    // Saved Opportunities
    @Query("SELECT * FROM saved_opportunities WHERE userId = :userId ORDER BY savedAt DESC")
    fun getAllSavedOpportunitiesFlow(userId: String): Flow<List<SavedOpportunityEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_opportunities WHERE id = :id AND userId = :userId)")
    fun isOpportunitySavedFlow(id: String, userId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedOpportunity(opportunity: SavedOpportunityEntity)

    @Delete
    suspend fun deleteSavedOpportunity(opportunity: SavedOpportunityEntity)

    @Query("DELETE FROM saved_opportunities WHERE id = :id AND userId = :userId")
    suspend fun deleteSavedOpportunityById(id: String, userId: String)
}
