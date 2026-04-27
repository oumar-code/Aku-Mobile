import SwiftUI
import shared  // KMP shared framework

/// Root navigation container — mirrors `AkulearnApp` composable in MainActivity.
struct ContentView: View {

    @ObservedObject private var authState: AuthState
    private let authRepository: AuthRepository

    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
        self.authState = AuthState(authRepository: authRepository)
    }

    var body: some View {
        Group {
            if authState.isInitializing {
                ProgressView()
                    .progressViewStyle(.circular)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if authState.isLoggedIn {
                HomeView(authRepository: authRepository, authState: authState)
            } else {
                LoginView(authRepository: authRepository, authState: authState)
            }
        }
        .animation(.default, value: authState.isLoggedIn)
    }
}

// MARK: - AuthState observable

/// Bridges the KMP `StateFlow<Boolean>` into SwiftUI's observation system.
@MainActor
final class AuthState: ObservableObject {
    @Published var isLoggedIn: Bool = false
    @Published var isInitializing: Bool = true

    private let authRepository: AuthRepository

    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
        Task { await observeLoginState() }
    }

    private func observeLoginState() async {
        // KMP StateFlow is exposed as a Kotlin Flow; use Combine or a polling helper.
        // Here we use a simple Task-based observation suitable for Swift concurrency.
        isInitializing = true
        for await value in authRepository.isLoggedIn {
            isLoggedIn = value
            isInitializing = false
        }
    }
}
