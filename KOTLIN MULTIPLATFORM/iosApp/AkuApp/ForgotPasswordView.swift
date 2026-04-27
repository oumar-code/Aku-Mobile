import SwiftUI
import shared  // KMP shared framework

struct ForgotPasswordView: View {

    @State private var email: String = ""
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil
    @State private var isSuccess: Bool = false
    @Environment(\.dismiss) private var dismiss

    private let authRepository: AuthRepository

    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            if isSuccess {
                Text("Check your inbox — we sent you a reset link.")
                    .font(.body)
                    .foregroundColor(.accentColor)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            } else {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Enter your email address and we'll send you a link to reset your password.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isLoading)
                }
                .padding(.horizontal, 32)

                Spacer().frame(height: 32)

                Button(action: performRequest) {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity)
                    } else {
                        Text("Send reset link")
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading)
                .padding(.horizontal, 32)
            }

            Spacer()
        }
        .navigationTitle("Reset password")
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

    private func performRequest() {
        guard !email.isEmpty else {
            errorMessage = "Email is required."
            return
        }
        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                try await authRepository.requestPasswordReset(email: email)
                isSuccess = true
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
        case is AkuError.Network: return "Network error. Check your connection."
        case is AkuError.ServerError: return "Server error. Please try again later."
        default: return localizedDescription
        }
    }
}
