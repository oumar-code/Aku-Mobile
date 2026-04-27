import SwiftUI
import shared  // KMP shared framework

struct LoginView: View {

    @State private var email: String = ""
    @State private var password: String = ""
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil
    @State private var navigateToRegister: Bool = false
    @State private var navigateToForgotPassword: Bool = false

    private let authRepository: AuthRepository
    @ObservedObject private var authState: AuthState

    init(authRepository: AuthRepository, authState: AuthState) {
        self.authRepository = authRepository
        self.authState = authState
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Spacer()

                Text("Akulearn")
                    .font(.largeTitle.bold())
                    .padding(.bottom, 48)

                VStack(spacing: 16) {
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isLoading)

                    SecureField("Password", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isLoading)

                    HStack {
                        Spacer()
                        Button("Forgot password?") {
                            navigateToForgotPassword = true
                        }
                        .font(.subheadline)
                        .disabled(isLoading)
                    }
                }

                Spacer().frame(height: 24)

                Button(action: performLogin) {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity)
                    } else {
                        Text("Log In")
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading)

                Button("Don't have an account? Sign up") {
                    navigateToRegister = true
                }
                .font(.subheadline)
                .padding(.top, 12)
                .disabled(isLoading)

                Spacer()
            }
            .padding(.horizontal, 32)
            .navigationDestination(isPresented: $navigateToRegister) {
                RegisterView(authRepository: authRepository, authState: authState)
            }
            .navigationDestination(isPresented: $navigateToForgotPassword) {
                ForgotPasswordView(authRepository: authRepository)
            }
            .alert("Error", isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private func performLogin() {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Email and password are required."
            return
        }
        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                _ = try await authRepository.login(email: email, password: password)
            } catch let error as AkuError {
                errorMessage = error.userMessage
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

// MARK: - Error helper

private extension AkuError {
    var userMessage: String {
        switch self {
        case is AkuError.Unauthorized: return "Invalid email or password."
        case is AkuError.Network: return "Network error. Check your connection."
        case is AkuError.ServerError: return "Server error. Please try again later."
        default: return localizedDescription
        }
    }
}
