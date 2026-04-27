import SwiftUI
import shared  // KMP shared framework

@main
struct AkuApp: App {

    // Wire up storage → session → repository — mirrors MainActivity's Koin graph.
    private let tokenStorage = IosTokenStorage()
    private let sessionManager: SessionManager
    private let authRepository: AuthRepository

    init() {
        sessionManager = SessionManager(tokenStorage: tokenStorage)
        authRepository = AuthRepository(
            sessionManager: sessionManager,
            apiClient: Wave3ApiClient()
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView(authRepository: authRepository)
                .task {
                    // Initialize session on launch (reads Keychain; auto-refreshes if expired).
                    try? await authRepository.initialize()
                }
        }
    }
}
