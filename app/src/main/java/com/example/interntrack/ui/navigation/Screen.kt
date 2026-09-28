package com.example.interntrack.ui.navigation

sealed class Screen(val route: String) {
    // Auth Screens
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object ProfileSetup : Screen("profile_setup")

    // Main Screens
    object Home : Screen("home")
    object Search : Screen("search")
    object Applications : Screen("applications")
    object Profile : Screen("profile")
    
    object InternshipDetails : Screen("internship_details/{id}") {
        fun createRoute(id: String) = "internship_details/$id"
    }
    
    object ApplicationDetails : Screen("application_details/{id}") {
        fun createRoute(id: Long) = "application_details/$id"
    }
    
    object AddApplication : Screen("add_application")
    object EditApplication : Screen("edit_application/{id}") {
        fun createRoute(id: Long) = "edit_application/$id"
    }
    
    object SavedInternships : Screen("saved_internships")
    object Analytics : Screen("analytics")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object About : Screen("about")
}
