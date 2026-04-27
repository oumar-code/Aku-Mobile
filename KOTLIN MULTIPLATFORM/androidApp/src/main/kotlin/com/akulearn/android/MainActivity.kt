package com.akulearn.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.akulearn.android.ui.RateUsDialog
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
                        courseRepository = courseRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun AkulearnApp(
    authRepository: AuthRepository,
    courseRepository: CourseRepository
) {
    val navController = rememberNavController()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()

    // Request POST_NOTIFICATIONS permission on Android 13+ at app launch.
    RequestNotificationPermission()

    // Initialize session (reads persisted token; auto-refreshes if expired).
    LaunchedEffect(Unit) { authRepository.initialize() }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            // Check onboarding state after session is ready
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
            var showRateUs by remember { mutableStateOf(false) }

            HomeScreen(
                uiState = uiState,
                onBrowseCourses = { navController.navigate("courses") },
                onCourseClick = { courseId -> navController.navigate("course/$courseId") },
                onProfile = { navController.navigate("profile") },
                onLogout = {
                    viewModel.logout {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )

            if (showRateUs) {
                RateUsDialog(onDismiss = { showRateUs = false })
            }
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
            LessonsScreen(
                courseTitle = uiState.course?.title ?: "Lessons",
                lessons = uiState.lessons,
                isLoading = uiState.isLoading,
                onLessonClick = { lesson ->
                    // Encode the lesson as JSON to pass via route
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
                Json.decodeFromString(Lesson.serializer(), java.net.URLDecoder.decode(lessonJson, "UTF-8"))
            } catch (e: Exception) {
                return@composable
            }
            val viewModel: LessonPlayerViewModel = viewModel(
                factory = LessonPlayerViewModel.Factory(courseRepository, lesson)
            )
            val uiState by viewModel.uiState.collectAsState()
            LessonPlayerScreen(
                uiState = uiState,
                onMarkComplete = viewModel::markComplete,
                onErrorDismissed = viewModel::clearError,
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
}

