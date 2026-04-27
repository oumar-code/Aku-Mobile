package com.akulearn.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.akulearn.android.auth.AndroidTokenStorage
import com.akulearn.android.ui.LoginScreen
import com.akulearn.android.ui.LoginViewModel
import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.AuthRepository
import com.akuplatform.shared.auth.SessionManager

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStorage = AndroidTokenStorage(applicationContext)
        sessionManager = SessionManager(tokenStorage)
        val baseUrl = BuildConfig.WAVE3_BASE_URL.ifBlank { Wave3ApiClient.BASE_URL }
        authRepository = AuthRepository(sessionManager, Wave3ApiClient(baseUrl))

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkulearnApp(
                        sessionManager = sessionManager,
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun AkulearnApp(
    sessionManager: SessionManager,
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()

    // Initialize session once (reads persisted token from storage).
    LaunchedEffect(Unit) { sessionManager.initialize() }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            // Show a brief loading indicator while the session is being initialised.
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
                onErrorDismissed = viewModel::clearError
            )
        }

        composable("home") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Welcome to Akulearn!",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
