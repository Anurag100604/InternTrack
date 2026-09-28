package com.example.interntrack.data

import android.util.Log
import com.example.interntrack.data.api.ApiClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class InternshipRepository(
    private val dao: InternshipDao
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var listenerRegistration: ListenerRegistration? = null

    fun startFirestoreSync(userId: String, scope: CoroutineScope) {
        stopFirestoreSync()
        if (userId.isBlank()) return

        try {
            listenerRegistration = firestore.collection("applications")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val firestoreApps = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        InternshipEntity.fromFirestoreMap(doc.id, data)
                    }

                    scope.launch(Dispatchers.IO) {
                        try {
                            dao.syncUserApplications(userId, firestoreApps)
                        } catch (e: Exception) {
                            // Ignore sync error
                        }
                    }
                }
        } catch (e: Exception) {
            // Ignore setup error
        }
    }

    fun stopFirestoreSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun getAllApplications(userId: String): Flow<List<InternshipEntity>> = dao.getAllApplicationsFlow(userId)

    fun getAllSavedOpportunities(userId: String): Flow<List<SavedOpportunityEntity>> = dao.getAllSavedOpportunitiesFlow(userId)

    fun isOpportunitySaved(id: String, userId: String): Flow<Boolean> = dao.isOpportunitySavedFlow(id, userId)

    suspend fun toggleBookmark(opportunity: Opportunity, userId: String) = withContext(Dispatchers.IO) {
        val isSaved = dao.isOpportunitySavedFlow(opportunity.id, userId).first()
        if (isSaved) {
            dao.deleteSavedOpportunityById(opportunity.id, userId)
        } else {
            dao.insertSavedOpportunity(opportunity.toSavedEntity(userId))
        }
    }

    suspend fun saveOpportunity(opportunity: Opportunity, userId: String) = withContext(Dispatchers.IO) {
        dao.insertSavedOpportunity(opportunity.toSavedEntity(userId))
    }

    suspend fun removeSavedOpportunity(id: String, userId: String) = withContext(Dispatchers.IO) {
        dao.deleteSavedOpportunityById(id, userId)
    }

    fun getApplicationById(id: Long, userId: String): Flow<InternshipEntity?> = dao.getApplicationByIdFlow(id, userId)

    suspend fun insertApplication(application: InternshipEntity): Long = withContext(Dispatchers.IO) {
        var docId = application.documentId
        try {
            if (docId.isBlank()) {
                val newDocRef = firestore.collection("applications").document()
                docId = newDocRef.id
            }
            val appWithDocId = application.copy(documentId = docId)
            firestore.collection("applications").document(docId).set(appWithDocId.toFirestoreMap()).await()
            val existingLocal = dao.getAllApplications(application.userId).find { it.documentId == docId }
            val finalLocal = if (existingLocal != null) appWithDocId.copy(id = existingLocal.id) else appWithDocId
            dao.insertApplication(finalLocal)
        } catch (e: Exception) {
            val fallbackDocId = if (docId.isBlank()) "local_${System.currentTimeMillis()}" else docId
            val appWithDocId = application.copy(documentId = fallbackDocId)
            dao.insertApplication(appWithDocId)
        }
    }

    suspend fun updateApplication(application: InternshipEntity) = withContext(Dispatchers.IO) {
        try {
            var docId = application.documentId
            if (docId.isBlank()) {
                val newDocRef = firestore.collection("applications").document()
                docId = newDocRef.id
            }
            val updatedApp = application.copy(documentId = docId, updatedAt = System.currentTimeMillis())
            firestore.collection("applications").document(docId).set(updatedApp.toFirestoreMap()).await()
            dao.updateApplication(updatedApp)
        } catch (e: Exception) {
            dao.updateApplication(application.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteApplication(application: InternshipEntity) = withContext(Dispatchers.IO) {
        try {
            if (application.documentId.isNotBlank()) {
                firestore.collection("applications").document(application.documentId).delete().await()
            }
        } catch (e: Exception) {
            // Suppress offline delete failure
        }
        dao.deleteApplication(application)
    }

    suspend fun deleteById(id: Long, userId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getApplicationById(id, userId)
        if (existing != null) {
            deleteApplication(existing)
        }
    }

    suspend fun updateStatus(id: Long, userId: String, newStatus: ApplicationStatus) = withContext(Dispatchers.IO) {
        val existing = dao.getApplicationById(id, userId)
        if (existing != null) {
            val updated = existing.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            updateApplication(updated)
        }
    }

    suspend fun clearDemoApplications(userId: String) = withContext(Dispatchers.IO) {
        dao.deleteDemoApplications(userId)
    }

    fun getApiOpportunitiesFlow(what: String? = null, where: String? = null): Flow<List<Opportunity>> = flow {
        try {
            val queryWhat = if (what.isNullOrBlank()) "internship" else what.trim()
            val queryWhere = if (where == "All" || where.isNullOrBlank()) null else where.trim()

            Log.d("InternTrackAPI", "getApiOpportunitiesFlow -> what='$queryWhat', where='$queryWhere'")

            val response = ApiClient.apiService.getInternships(
                what = queryWhat,
                where = queryWhere
            )

            Log.d("InternTrackAPI", "Response count=${response.count}, success=${response.success}")

            if (response.success && response.internships.isNotEmpty()) {
                emit(response.internships)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("InternTrackAPI", "API Error: ${e.message}", e)
            try {
                val firestoreList = getFirestoreOpportunitiesFlow().first()
                emit(firestoreList)
            } catch (ignored: Exception) {
                emit(OpportunityDataSource.curatedOpportunities)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getFirestoreOpportunitiesFlow(): Flow<List<Opportunity>> = callbackFlow {
        val registration = firestore.collection("internships")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(OpportunityDataSource.curatedOpportunities)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    // Seed initial data to Firestore if internships collection is empty
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val seedList = OpportunityDataSource.curatedOpportunities
                            for (opp in seedList) {
                                firestore.collection("internships")
                                    .document(opp.id)
                                    .set(opp.toFirestoreMap())
                                    .await()
                            }
                        } catch (e: Exception) {
                            // Suppress seed error
                        }
                    }
                    trySend(OpportunityDataSource.curatedOpportunities)
                } else {
                    val opps = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        Opportunity.fromFirestoreMap(doc.id, data)
                    }
                    trySend(opps)
                }
            }

        awaitClose { registration.remove() }
    }

    fun getCuratedOpportunities(): List<Opportunity> {
        return OpportunityDataSource.curatedOpportunities
    }
}

