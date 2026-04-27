import SwiftUI
import shared  // KMP shared framework

struct RegisterView: View {

    @State private var name: String = ""
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil
    @Environment(\.dismiss) private var dismiss

    private let authRepository: AuthRepository
    @ObservedObject private var authState: AuthState

    init(authRepository: AuthRepository, authState: AuthState) {
        self.authRepository = authRepository
        self.authState = authState
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 16) {
                TextField("Full name", text: $name)
                    .textFieldStyle(.roundedBorder)
                    .disabled(isLoading)

                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .textFieldStyle(.roundedBorder)
                    .disabled(isLoading)

                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .disabled(isLoading)
            }

            Spacer().frame(height: 32)

            Button(action: performRegister) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Sign Up")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading)

            Spacer()
        }
        .padding(.horizontal, 32)
        .navigationTitle("Create account")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Error", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func performRegister() {
        guard !name.isEmpty, !email.isEmpty, !password.isEmpty else {
            errorMessage = "All fields are required."
            return
        }
        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                _ = try await authRepository.register(
                    email: email,
                    password: password,
                    name: name
                )
            } catch let error as AkuError {
                errorMessage = error.userMessage
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private extension AkuError {
    var userMessage: String {
        switch self {
        case is AkuError.Unauthorized: return "Registration not permitted."
        case is AkuError.Network: return "Network error. Check your connection."
        case is AkuError.ServerError: return "Server error. Please try again later."
        default: return localizedDescription
        }
    }
}
