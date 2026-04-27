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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akulearn.android.ui.ForgotPasswordScreen
import com.akulearn.android.ui.ForgotPasswordViewModel
import com.akulearn.android.ui.HomeScreen
import com.akulearn.android.ui.HomeViewModel
import com.akulearn.android.ui.LoginScreen
import com.akulearn.android.ui.LoginViewModel
import com.akulearn.android.ui.RegisterScreen
import com.akulearn.android.ui.RegisterViewModel
import com.akuplatform.shared.auth.AuthRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkulearnApp(authRepository = authRepository)
                }
            }
        }
    }
}

@Composable
private fun AkulearnApp(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()

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
            LaunchedEffect(isLoggedIn) {
                navController.navigate(if (isLoggedIn) "home" else "login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
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
                factory = HomeViewModel.Factory(authRepository)
            )
            HomeScreen(
                onLogout = {
                    viewModel.logout {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
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
                onBack = { navController.popBackStack() }
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
    }
}
