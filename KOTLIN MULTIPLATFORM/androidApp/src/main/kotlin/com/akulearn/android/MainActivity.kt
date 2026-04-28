package com.akulearn.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akulearn.android.notifications.DeepLinkHandler
import com.akulearn.android.notifications.RequestNotificationPermission
import com.akulearn.android.ui.CertificatesScreen
import com.akulearn.android.ui.CertificatesViewModel
import com.akulearn.android.ui.CourseDetailScreen
import com.akulearn.android.ui.CourseDetailViewModel
import com.akulearn.android.ui.CoursesScreen
import com.akulearn.android.ui.CoursesViewModel
import com.akulearn.android.ui.ForgotPasswordScreen
import com.akulearn.android.ui.ForgotPasswordViewModel
import com.akulearn.android.ui.HomeScreen
import com.akulearn.android.ui.HomeViewModel
import com.akulearn.android.ui.LessonPlayerScreen
import com.akulearn.android.ui.LessonPlayerViewModel
import com.akulearn.android.ui.LessonsScreen
import com.akulearn.android.ui.LoginScreen
import com.akulearn.android.ui.LoginViewModel
import com.akulearn.android.ui.OnboardingScreen
import com.akulearn.android.ui.OnboardingViewModel
import com.akulearn.android.ui.PrivacyPolicyScreen
import com.akulearn.android.ui.ProfileScreen
import com.akulearn.android.ui.ProfileViewModel
import com.akulearn.android.ui.RegisterScreen
import com.akulearn.android.ui.RegisterViewModel
import com.akulearn.android.ui.SettingsScreen
import com.akulearn.android.ui.SettingsViewModel
import com.akuplatform.shared.auth.AuthRepository
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.model.Lesson
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()
    private val courseRepository: CourseRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkulearnApp(
                        authRepository = authRepository,
                        courseRepository = courseRepository,
                        initialIntent = intent
                    )
                }
            }
        }
    }

    /** Re-deliver deep-link intents to the running NavHost when the activity is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

// Routes that are top-level bottom-nav tabs
private val bottomNavRoutes = setOf("home", "courses", "certificates", "profile")

/** Converts an `akulearn://` [Uri] to a Compose navigation route string, or `null` if unrecognised. */
private fun Uri.toNavRoute(): String? {
    if (scheme != DeepLinkHandler.SCHEME) return null
    return when (authority) {
        DeepLinkHandler.PATH_LESSON -> {
            // A lesson deep-link requires the full Lesson object which is not available from
            // just the ID without a network fetch.  Navigate to the course catalogue instead so
            // the user lands on a useful screen.  A future improvement can fetch the lesson's
            // courseId from the API and navigate to "lessons/{courseId}" directly.
            "courses"
        }
        DeepLinkHandler.PATH_COURSE -> {
            val courseId = pathSegments.firstOrNull() ?: return null
            "course/$courseId"
        }
        else -> null
    }
}

@Composable
private fun AkulearnApp(
    authRepository: AuthRepository,
    courseRepository: CourseRepository,
    initialIntent: Intent? = null
) {
    val navController = rememberNavController()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavRoutes

    // Request POST_NOTIFICATIONS permission on Android 13+ at app launch.
    RequestNotificationPermission()

    // Initialize session (reads persisted token; auto-refreshes if expired).
    LaunchedEffect(Unit) { authRepository.initialize() }

    // Handle deep-link intent delivered on cold start or via onNewIntent.
    LaunchedEffect(initialIntent, isLoggedIn) {
        if (isLoggedIn) {
            initialIntent?.data?.toNavRoute()?.let { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.weight(1f)
        ) {
            composable("splash") {
                val onboardingVm: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.Factory(navController.context)
                )
                val onboardingState by onboardingVm.uiState.collectAsState()
                LaunchedEffect(isLoggedIn, onboardingState.isChecking) {
                    if (!onboardingState.isChecking) {
                        val destination = when {
                            !onboardingState.hasSeenOnboarding -> "onboarding"
                            isLoggedIn -> "home"
                            else -> "login"
                        }
                        navController.navigate(destination) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                // Branded splash screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Akulearn",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Learn. Grow. Succeed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        CircularProgressIndicator()
                    }
                }
            }

            composable("onboarding") {
                val viewModel: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.Factory(navController.context)
                )
                OnboardingScreen(
                    onFinish = {
                        viewModel.markOnboardingSeen()
                        navController.navigate(if (isLoggedIn) "home" else "login") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("login") {
                val viewModel: LoginViewModel = viewModel(
                    factory = LoginViewModel.Factory(authRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                LaunchedEffect(uiState.isSuccess) {
                    if (uiState.isSuccess) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                LoginScreen(
                    uiState = uiState,
                    onLogin = viewModel::login,
                    onErrorDismissed = viewModel::clearError,
                    onForgotPassword = { navController.navigate("forgot-password") },
                    onRegister = { navController.navigate("register") }
                )
            }

            composable("home") {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(authRepository, courseRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                HomeScreen(
                    uiState = uiState,
                    onBrowseCourses = { navController.navigate("courses") },
                    onCourseClick = { courseId -> navController.navigate("course/$courseId") }
                )
            }

            composable("courses") {
                val viewModel: CoursesViewModel = viewModel(
                    factory = CoursesViewModel.Factory(courseRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                CoursesScreen(
                    uiState = uiState,
                    onCourseClick = { course -> navController.navigate("course/${course.id}") },
                    onSearch = viewModel::onSearch,
                    onDurationFilter = viewModel::onDurationFilter,
                    onRefresh = viewModel::loadCourses,
                    onErrorDismissed = viewModel::clearError
                )
            }

            composable("course/{courseId}") { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
                val viewModel: CourseDetailViewModel = viewModel(
                    factory = CourseDetailViewModel.Factory(courseRepository, courseId)
                )
                val uiState by viewModel.uiState.collectAsState()
                CourseDetailScreen(
                    uiState = uiState,
                    onEnroll = viewModel::enroll,
                    onEnrollmentErrorDismissed = viewModel::clearEnrollmentError,
                    onViewLessons = { navController.navigate("lessons/$courseId") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("lessons/{courseId}") { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
                val viewModel: CourseDetailViewModel = viewModel(
                    factory = CourseDetailViewModel.Factory(courseRepository, courseId)
                )
                val uiState by viewModel.uiState.collectAsState()
                val playbackPrefs = LocalContext.current.getSharedPreferences(
                    LessonPlayerViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE
                )
                val lessonProgressFractions = uiState.lessons.associate { lesson ->
                    lesson.id to (playbackPrefs.getFloat("frac_${lesson.id}", 0f))
                }
                LessonsScreen(
                    courseTitle = uiState.course?.title ?: "Lessons",
                    lessons = uiState.lessons,
                    isLoading = uiState.isLoading,
                    lessonProgressFractions = lessonProgressFractions,
                    onLessonClick = { lesson ->
                        val lessonJson = java.net.URLEncoder.encode(
                            Json.encodeToString(Lesson.serializer(), lesson), "UTF-8"
                        )
                        navController.navigate("lesson-player/$lessonJson")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "lesson-player/{lessonJson}",
                arguments = listOf(navArgument("lessonJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val lessonJson = backStackEntry.arguments?.getString("lessonJson") ?: return@composable
                val lesson = try {
                    Json.decodeFromString(
                        Lesson.serializer(),
                        java.net.URLDecoder.decode(lessonJson, "UTF-8")
                    )
                } catch (e: Exception) {
                    return@composable
                }
                val context = LocalContext.current
                val viewModel: LessonPlayerViewModel = viewModel(
                    factory = LessonPlayerViewModel.Factory(courseRepository, lesson, context)
                )
                val uiState by viewModel.uiState.collectAsState()
                LessonPlayerScreen(
                    uiState = uiState,
                    onMarkComplete = viewModel::markComplete,
                    onErrorDismissed = viewModel::clearError,
                    onPositionChanged = viewModel::onPositionChanged,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("register") {
                val viewModel: RegisterViewModel = viewModel(
                    factory = RegisterViewModel.Factory(authRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                LaunchedEffect(uiState.isSuccess) {
                    if (uiState.isSuccess) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                RegisterScreen(
                    uiState = uiState,
                    onRegister = viewModel::register,
                    onErrorDismissed = viewModel::clearError,
                    onBack = { navController.popBackStack() },
                    onPrivacyPolicy = { navController.navigate("privacy-policy") }
                )
            }

            composable("forgot-password") {
                val viewModel: ForgotPasswordViewModel = viewModel(
                    factory = ForgotPasswordViewModel.Factory(authRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                ForgotPasswordScreen(
                    uiState = uiState,
                    onSubmit = viewModel::requestReset,
                    onErrorDismissed = viewModel::clearError,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                val viewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(authRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                ProfileScreen(
                    uiState = uiState,
                    onErrorDismissed = viewModel::clearError,
                    onSettings = { navController.navigate("settings") },
                    onLogout = {
                        viewModel.logout {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(authRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                SettingsScreen(
                    uiState = uiState,
                    onChangePassword = viewModel::changePassword,
                    onErrorDismissed = viewModel::clearError,
                    onSuccessDismissed = viewModel::clearSuccess,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("certificates") {
                val viewModel: CertificatesViewModel = viewModel(
                    factory = CertificatesViewModel.Factory(courseRepository)
                )
                val uiState by viewModel.uiState.collectAsState()
                CertificatesScreen(
                    uiState = uiState,
                    onErrorDismissed = viewModel::clearError,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("privacy-policy") {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }

        // Bottom navigation bar — shown only on the four top-level tab destinations
        if (showBottomNav) {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Pop back to home (keeping home) so tabs don't stack
                        popUpTo("home") {
                            inclusive = false
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onNavigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Courses") },
            label = { Text("Courses") },
            selected = currentRoute == "courses",
            onClick = { onNavigate("courses") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = "Certificates") },
            label = { Text("Certificates") },
            selected = currentRoute == "certificates",
            onClick = { onNavigate("certificates") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { onNavigate("profile") }
        )
    }
}
