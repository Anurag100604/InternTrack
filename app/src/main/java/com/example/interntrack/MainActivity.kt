package com.example.interntrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseApp
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.interntrack.data.ApplicationStatus
import com.example.interntrack.data.UserEntity
import com.example.interntrack.data.UserProfile
import com.example.interntrack.ui.navigation.Screen
import com.example.interntrack.ui.screens.*
import com.example.interntrack.ui.screens.auth.*
import com.example.interntrack.ui.theme.InternTrackTheme
import com.example.interntrack.viewmodel.AuthState
import com.example.interntrack.viewmodel.InternshipViewModel
import com.example.interntrack.viewmodel.ThemeMode
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        setContent {
            val viewModel: InternshipViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val textSize by viewModel.textSize.collectAsState()
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            InternTrackTheme(darkTheme = darkTheme) {
                // Apply Text Scale
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = LocalDensity.current.fontScale * textSize.scale
                    )
                ) {
                    val navController = rememberNavController()
                    InternTrackApp(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}

enum class NavigationSection(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, Screen.Home.route),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, Screen.Search.route),
    APPLICATIONS("Applications", Icons.Filled.BusinessCenter, Icons.Outlined.BusinessCenter, Screen.Applications.route),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, Screen.Profile.route)
}

@Composable
fun InternTrackApp(
    navController: NavHostController,
    viewModel: InternshipViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val authState by viewModel.authState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val statistics by viewModel.statistics.collectAsState()
    val allApplications by viewModel.allApplications.collectAsState()
    val filteredApplications by viewModel.filteredApplications.collectAsState()
    val appSearchQuery by viewModel.appSearchQuery.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()

    val filteredOpportunities by viewModel.filteredOpportunities.collectAsState()
    val savedOpportunities by viewModel.savedOpportunities.collectAsState()
    val oppSearchQuery by viewModel.oppSearchQuery.collectAsState()
    val workModeFilter by viewModel.workModeFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val locationFilter by viewModel.locationFilter.collectAsState()

    val showBottomBar = NavigationSection.entries.any { it.route == currentRoute }

    // Navigation Gate: Observe AuthState changes
    LaunchedEffect(authState) {
        delay(500) // Small delay to ensure NavHost is ready
        when (authState) {
            AuthState.LOGGED_OUT -> {
                if (currentRoute != Screen.Login.route && 
                    currentRoute != Screen.SignUp.route && 
                    currentRoute != Screen.ForgotPassword.route
                ) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            AuthState.LOGGED_IN -> {
                if (currentRoute == null || 
                    currentRoute == Screen.Login.route || 
                    currentRoute == Screen.Splash.route || 
                    currentRoute == Screen.SignUp.route || 
                    currentRoute == Screen.ProfileSetup.route
                ) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {} // Do nothing for SPLASH or LOADING
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationSection.entries.forEach { section ->
                        val isSelected = currentRoute == section.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != section.route) {
                                    navController.navigate(section.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) section.selectedIcon else section.unselectedIcon,
                                    contentDescription = section.label
                                )
                            },
                            label = { Text(section.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.Home.route || currentRoute == Screen.Applications.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddApplication.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add application"
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Auth Flow
            composable(Screen.Splash.route) {
                SplashScreen(viewModel = viewModel)
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    viewModel = viewModel,
                    onSignUpSuccess = { name, email, password ->
                        viewModel.prepareSignUp(name, email, password)
                        navController.navigate(Screen.ProfileSetup.route)
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onComplete = { _, college, course, branch, gradYear, bio ->
                        viewModel.completeSignUp(college, course, branch, gradYear, bio)
                    }
                )
            }
// ... rest of the file stays same

            // Main App Flow
            composable(Screen.Home.route) {
                HomeScreen(
                    statistics = statistics,
                    recentApplications = allApplications,
                    recommendedOpportunities = filteredOpportunities,
                    userProfile = userProfile,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToApplications = {
                        viewModel.setStatusFilter(null)
                        navController.navigate(Screen.Applications.route)
                    },
                    onNavigateToApplicationsWithFilter = { status ->
                        viewModel.setStatusFilter(status)
                        navController.navigate(Screen.Applications.route)
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onAddApplicationClick = { navController.navigate(Screen.AddApplication.route) },
                    onApplicationClick = { app -> navController.navigate(Screen.ApplicationDetails.createRoute(app.id)) },
                    onOpportunityClick = { opp -> navController.navigate(Screen.InternshipDetails.createRoute(opp.id)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    opportunities = filteredOpportunities,
                    savedOpportunities = savedOpportunities,
                    searchQuery = oppSearchQuery,
                    workModeFilter = workModeFilter,
                    typeFilter = typeFilter,
                    locationFilter = locationFilter,
                    onSearchQueryChange = viewModel::setOppSearchQuery,
                    onWorkModeFilterChange = viewModel::setWorkModeFilter,
                    onTypeFilterChange = viewModel::setTypeFilter,
                    onLocationFilterChange = viewModel::setLocationFilter,
                    onOpportunityClick = { opp -> navController.navigate(Screen.InternshipDetails.createRoute(opp.id)) },
                    onTrackOpportunity = { opp -> viewModel.trackOpportunity(opp) },
                    onBookmarkOpportunity = { opp -> viewModel.toggleBookmark(opp) }
                )
            }

            composable(Screen.Applications.route) {
                ApplicationsScreen(
                    applications = filteredApplications,
                    searchQuery = appSearchQuery,
                    selectedStatusFilter = selectedStatusFilter,
                    onSearchQueryChange = viewModel::setAppSearchQuery,
                    onStatusFilterChange = viewModel::setStatusFilter,
                    onAddApplicationClick = { navController.navigate(Screen.AddApplication.route) },
                    onEditApplicationClick = { app -> navController.navigate(Screen.EditApplication.createRoute(app.id)) },
                    onDeleteApplicationClick = { app -> viewModel.deleteApplication(app) },
                    onStatusChangeClick = { id, status -> viewModel.updateStatus(id, status) },
                    onApplicationClick = { app -> navController.navigate(Screen.ApplicationDetails.createRoute(app.id)) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    userProfile = userProfile,
                    statistics = statistics,
                    onUpdateProfile = { viewModel.updateProfile(it) },
                    onClearDemoData = { viewModel.clearDemoData() },
                    onEditClick = { navController.navigate(Screen.EditProfile.route) },
                    onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onSavedClick = { navController.navigate(Screen.SavedInternships.route) }
                )
            }

            composable(
                route = Screen.InternshipDetails.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                InternshipDetailsScreen(
                    opportunityId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onTrack = { opp ->
                        viewModel.trackOpportunity(opp)
                        navController.navigate(Screen.Applications.route)
                    }
                )
            }

            composable(
                route = Screen.ApplicationDetails.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                ApplicationTrackerScreen(
                    applicationId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { appId -> navController.navigate(Screen.EditApplication.createRoute(appId)) }
                )
            }

            composable(Screen.AddApplication.route) {
                AddEditApplicationScreen(
                    applicationId = null,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditApplication.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                AddEditApplicationScreen(
                    applicationId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SavedInternships.route) {
                SavedInternshipsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpportunityClick = { id -> navController.navigate(Screen.InternshipDetails.createRoute(id)) }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAboutClick = { navController.navigate(Screen.About.route) }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}