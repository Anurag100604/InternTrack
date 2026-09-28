package com.example.interntrack.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return@withContext Result.failure(Exception("Login failed. Invalid account."))
            
            var userProfile = fetchFirestoreProfile(uid)
            if (userProfile == null) {
                userProfile = UserProfile(
                    name = authResult.user?.displayName ?: email.substringBefore("@"),
                    email = email
                )
                saveFirestoreProfile(uid, userProfile)
            }
            
            val userEntity = userProfile.toEntity(uid)
            userDao.signup(userEntity)
            sessionManager.saveSession(uid)
            Result.success(userEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(user: UserEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(user.email, user.password).await()
            val uid = authResult.user?.uid ?: return@withContext Result.failure(Exception("Signup failed."))
            
            val profile = user.toProfile()
            saveFirestoreProfile(uid, profile)
            
            val userWithUid = user.copy(id = uid)
            userDao.signup(userWithUid)
            sessionManager.saveSession(uid)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
    }

    fun getSessionUserId() = sessionManager.userId

    fun getCurrentFirebaseUid(): String? = firebaseAuth.currentUser?.uid

    suspend fun getUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val remoteProfile = fetchFirestoreProfile(userId)
            if (remoteProfile != null) {
                userDao.updateProfile(remoteProfile.toEntity(userId))
                return@withContext remoteProfile
            }
        } catch (e: Exception) {
            // Fallback to local cache on error/offline
        }
        userDao.getUserById(userId)?.toProfile()
    }

    suspend fun updateProfile(userId: String, profile: UserProfile) = withContext(Dispatchers.IO) {
        try {
            saveFirestoreProfile(userId, profile)
        } catch (e: Exception) {
            // Handle offline fallback
        }
        val existing = userDao.getUserById(userId)
        val updatedEntity = profile.toEntity(userId)
        if (existing != null) {
            userDao.updateProfile(updatedEntity)
        } else {
            userDao.signup(updatedEntity)
        }
    }

    private suspend fun fetchFirestoreProfile(uid: String): UserProfile? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return null
                UserProfile.fromFirestoreMap(data)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveFirestoreProfile(uid: String, profile: UserProfile) {
        val map = profile.toFirestoreMap(uid)
        firestore.collection("users").document(uid).set(map).await()
    }
}

