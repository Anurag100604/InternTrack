package com.example.interntrack.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.interntrack.data.AppDatabase
import com.example.interntrack.data.ApplicationStatus
import com.example.interntrack.data.AuthRepository
import com.example.interntrack.data.InternshipEntity
import com.example.interntrack.data.InternshipRepository
import com.example.interntrack.data.Opportunity
import com.example.interntrack.data.SavedOpportunityEntity
import com.example.interntrack.data.SessionManager
import com.example.interntrack.data.UserEntity
import com.example.interntrack.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuthState {
    SPLASH,
    LOGGED_OUT,
    LOADING,
    LOGGED_IN
}

data class ApplicationStatistics(
    val total: Int = 0,
    val applied: Int = 0,
    val shortlisted: Int = 0,
    val interview: Int = 0,
    val selected: Int = 0,
    val rejected: Int = 0,
    val pending: Int = 0
)

class InternshipViewModel(application: Application) : AndroidViewModel(application) {

    private val internshipRepository: InternshipRepository
    private val authRepository: AuthRepository
    private val sessionManager: SessionManager

    private val _authState = MutableStateFlow(AuthState.SPLASH)
    val authState = _authState.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    // Settings
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    private val _textSize = MutableStateFlow(TextSizePreference.DEFAULT)
    val textSize = _textSize.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        internshipRepository = InternshipRepository(database.internshipDao())
        sessionManager = SessionManager(application)
        authRepository = AuthRepository(database.userDao(), sessionManager)

        // Check current Firebase user immediately
        val currentUid = authRepository.getCurrentFirebaseUid()
        if (currentUid != null) {
            onUserAuthenticated(currentUid)
        } else {
            viewModelScope.launch {
                sessionManager.userId.collect { id ->
                    if (id != null) {
                        onUserAuthenticated(id)
                    } else {
                        onUserLoggedOut()
                    }
                }
            }
        }
    }

    private fun onUserAuthenticated(userId: String) {
        _currentUserId.value = userId
        _authState.value = AuthState.LOGGED_IN
        loadProfile(userId)
        internshipRepository.startFirestoreSync(userId, viewModelScope)
    }

    private fun onUserLoggedOut() {
        internshipRepository.stopFirestoreSync()
        _currentUserId.value = null
        _authState.value = AuthState.LOGGED_OUT
        _userProfile.value = UserProfile()
    }

    private fun loadProfile(userId: String) {
        viewModelScope.launch {
            val profile = authRepository.getUserProfile(userId)
            if (profile != null) {
                _userProfile.value = profile
            }
        }
    }

    // Raw applications from Room
    @OptIn(ExperimentalCoroutinesApi::class)
    val allApplications: StateFlow<List<InternshipEntity>> = _currentUserId.flatMapLatest { id ->
        if (id != null) {
            internshipRepository.getAllApplications(id).map { list ->
                list.distinctBy { if (it.documentId.isNotBlank()) it.documentId else it.id }
            }
        } else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Saved Opportunities (Bookmarks)
    @OptIn(ExperimentalCoroutinesApi::class)
    val savedOpportunities: StateFlow<List<SavedOpportunityEntity>> = _currentUserId.flatMapLatest { id ->
        if (id != null) internshipRepository.getAllSavedOpportunities(id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun isOpportunitySaved(id: String): Flow<Boolean> {
        val userId = _currentUserId.value ?: return flowOf(false)
        return internshipRepository.isOpportunitySaved(id, userId)
    }

    // Dynamic stats derived from Room database
    val statistics: StateFlow<ApplicationStatistics> = allApplications
        .map { apps ->
            ApplicationStatistics(
                total = apps.size,
                applied = apps.count { it.status == ApplicationStatus.APPLIED },
                shortlisted = apps.count { it.status == ApplicationStatus.SHORTLISTED },
                interview = apps.count { it.status == ApplicationStatus.INTERVIEW },
                selected = apps.count { it.status == ApplicationStatus.SELECTED },
                rejected = apps.count { it.status == ApplicationStatus.REJECTED },
                pending = apps.count { it.status != ApplicationStatus.SELECTED && it.status != ApplicationStatus.REJECTED }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApplicationStatistics()
        )

    // Applications Screen Filters
    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery = _appSearchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ApplicationStatus?>(null)
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    val filteredApplications: StateFlow<List<InternshipEntity>> = combine(
        allApplications,
        _appSearchQuery,
        _selectedStatusFilter
    ) { apps, query, status ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.company.contains(query, ignoreCase = true) ||
                    app.role.contains(query, ignoreCase = true) ||
                    app.location.contains(query, ignoreCase = true) ||
                    app.notes.contains(query, ignoreCase = true)

            val matchesStatus = status == null || app.status == status

            matchesQuery && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search / Discovery Screen Filters
    private val _oppSearchQuery = MutableStateFlow("")
    val oppSearchQuery = _oppSearchQuery.asStateFlow()

    private val _workModeFilter = MutableStateFlow("All")
    val workModeFilter = _workModeFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("All")
    val typeFilter = _typeFilter.asStateFlow()

    private val _locationFilter = MutableStateFlow("All")
    val locationFilter = _locationFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allOpportunities: StateFlow<List<Opportunity>> = combine(
        _oppSearchQuery,
        _locationFilter
    ) { query, location ->
        query to location
    }.flatMapLatest { (query, location) ->
        Log.d("InternTrackVM", "allOpportunities -> query='$query', location='$location'")
        internshipRepository.getApiOpportunitiesFlow(what = query, where = location)
    }.flowOn(Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = internshipRepository.getCuratedOpportunities()
    )

    val filteredOpportunities: StateFlow<List<Opportunity>> = combine(
        allOpportunities,
        _oppSearchQuery,
        _workModeFilter,
        _typeFilter,
        _locationFilter
    ) { opps, query, workMode, type, location ->
        opps.filter { opp ->
            val matchesQuery = query.isBlank() ||
                    opp.id.startsWith("adzuna_") ||
                    opp.company.contains(query, ignoreCase = true) ||
                    opp.role.contains(query, ignoreCase = true) ||
                    opp.location.contains(query, ignoreCase = true) ||
                    opp.description.contains(query, ignoreCase = true) ||
                    opp.skills.any { it.contains(query, ignoreCase = true) }

            val matchesWorkMode = workMode == "All" || opp.workMode.equals(workMode, ignoreCase = true)
            val matchesType = type == "All" || opp.internshipType.equals(type, ignoreCase = true)

            val matchesLocation = location == "All" ||
                    opp.id.startsWith("adzuna_") ||
                    opp.location.contains(location, ignoreCase = true)

            matchesQuery && matchesWorkMode && matchesType && matchesLocation
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = internshipRepository.getCuratedOpportunities()
    )

    // User Actions
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private var signUpData: UserEntity? = null

    fun prepareSignUp(name: String, email: String, pass: String) {
        signUpData = UserEntity(
            id = "", // Will be set by Firebase
            name = name,
            email = email,
            password = pass
        )
    }

    fun completeSignUp(college: String, course: String, branch: String, gradYear: String, bio: String) {
        val base = signUpData ?: return
        val finalUser = base.copy(
            college = college,
            course = course,
            branch = branch,
            graduationYear = gradYear,
            bio = bio
        )
        signup(finalUser)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            _loginError.value = null
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    onUserAuthenticated(user.id)
                }
            } else {
                _authState.value = AuthState.LOGGED_OUT
                _loginError.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun signup(user: UserEntity) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            _loginError.value = null
            val result = authRepository.signup(user)
            if (result.isSuccess) {
                val uid = result.getOrNull()
                if (uid != null) {
                    onUserAuthenticated(uid)
                }
            } else {
                _authState.value = AuthState.LOGGED_OUT
                _loginError.value = result.exceptionOrNull()?.message ?: "Signup failed"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            onUserLoggedOut()
            _appSearchQuery.value = ""
            _oppSearchQuery.value = ""
        }
    }

    fun forgotPassword(email: String) {
        // In real app, call firebaseAuth.sendPasswordResetEmail(email)
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun setStatusFilter(status: ApplicationStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setOppSearchQuery(query: String) {
        _oppSearchQuery.value = query
    }

    fun setWorkModeFilter(mode: String) {
        _workModeFilter.value = mode
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun setLocationFilter(location: String) {
        _locationFilter.value = location
    }

    fun addApplication(
        company: String,
        role: String,
        location: String = "",
        workMode: String = "",
        stipend: String = "",
        applicationDate: String = "",
        deadline: String = "",
        source: String = "",
        jobUrl: String = "",
        interviewDate: String = "",
        status: ApplicationStatus = ApplicationStatus.APPLIED,
        notes: String = ""
    ) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val appDate = if (applicationDate.isBlank()) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date())
            } else applicationDate

            val newEntity = InternshipEntity(
                userId = userId,
                company = company.trim(),
                role = role.trim(),
                location = location.trim(),
                workMode = workMode.trim(),
                stipend = stipend.trim(),
                applicationDate = appDate,
                deadline = deadline.trim(),
                source = source.trim(),
                jobUrl = jobUrl.trim(),
                interviewDate = interviewDate.trim(),
                status = status,
                notes = notes.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDemo = false
            )
            internshipRepository.insertApplication(newEntity)
        }
    }

    fun updateApplication(application: InternshipEntity) {
        viewModelScope.launch {
            internshipRepository.updateApplication(application.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteApplication(application: InternshipEntity) {
        viewModelScope.launch {
            internshipRepository.deleteApplication(application)
        }
    }

    fun updateStatus(id: Long, newStatus: ApplicationStatus) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            internshipRepository.updateStatus(id, userId, newStatus)
        }
    }

    fun toggleBookmark(opportunity: Opportunity) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            internshipRepository.toggleBookmark(opportunity, userId)
        }
    }

    fun trackOpportunity(opportunity: Opportunity) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val today = sdf.format(Date())
            val newApp = InternshipEntity(
                userId = userId,
                company = opportunity.company,
                role = opportunity.role,
                location = opportunity.location,
                workMode = opportunity.workMode,
                stipend = opportunity.stipend,
                applicationDate = today,
                deadline = opportunity.deadline,
                source = "InternTrack Discovery",
                status = ApplicationStatus.APPLIED,
                notes = "Discovered on InternTrack. ${opportunity.description}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDemo = false
            )
            internshipRepository.insertApplication(newApp)
        }
    }

    fun updateProfile(profile: UserProfile) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            authRepository.updateProfile(userId, profile)
            _userProfile.value = profile
        }
    }

    fun clearDemoData() {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            internshipRepository.clearDemoApplications(userId)
        }
    }

    fun getOpportunityById(id: String): Opportunity? {
        return allOpportunities.value.find { it.id == id }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setTextSize(size: TextSizePreference) {
        _textSize.value = size
    }
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class TextSizePreference(val scale: Float) {
    DEFAULT(1.0f),
    LARGE(1.2f),
    EXTRA_LARGE(1.4f)
}
