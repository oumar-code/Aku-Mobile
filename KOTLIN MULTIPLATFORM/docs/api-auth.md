# API Usage & Authentication Flow

This document describes the shared authentication flow after the Supabase migration.

---

## Auth provider

Authentication is handled by Supabase GoTrue through `AuthRepository` in:

`shared/src/commonMain/kotlin/com/akuplatform/shared/auth/AuthRepository.kt`

`AuthRepository` now owns:

- login
- register
- password reset
- password change
- profile lookup
- session restore / refresh
- logout

The repository persists the active session through `SessionManager` and `TokenStorage`.

---

## Required configuration

The auth layer requires these values at runtime:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

Android passes them through `BuildConfig` into `sharedModule(...)`.
iOS reads them from process environment before constructing `AuthRepository`.

Course/content APIs are still served by `Wave3ApiClient` and can optionally use:

- `WAVE3_BASE_URL`

---

## Session model

The shared auth layer stores a platform-neutral `AuthToken`:

```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<refresh-token>",
  "expiresIn": 3600,
  "expiresAt": 1712345678
}
```

- `expiresIn` comes from Supabase
- `expiresAt` is computed locally and used for startup refresh checks

---

## Error handling

Public auth methods return `Result<T>`.

Supabase and transport failures are mapped to:

- `ApiError.Network`
- `ApiError.Unauthorized`
- `ApiError.ServerError`

Callers should continue to use `onSuccess` / `onFailure`.

---

## Authentication flow

### Login

```text
User enters credentials
        ↓
AuthRepository.login(email, password)
        ↓
Supabase auth.signInWith(Email)
        ↓
currentSessionOrNull()
        ↓
SessionManager.saveSession(token)
        ↓
isLoggedIn emits true
```

### Register

```text
User enters name, email, password
        ↓
AuthRepository.register(...)
        ↓
Supabase auth.signUpWith(Email) + user metadata
        ↓
currentSessionOrNull()
        ↓
SessionManager.saveSession(token)
```

### Startup / token restore

```text
App launch
        ↓
AuthRepository.initialize()
        ↓
TokenStorage.getToken()
        ├─ no token  → clearSession() → logged out
        ├─ valid     → importSession(token) → logged in
        └─ expired   → refreshSession(refreshToken) → saveSession(new token)
```

### Password reset

```text
AuthRepository.requestPasswordReset(email)
        ↓
Supabase auth.resetPasswordForEmail(email)
```

### Password change

```text
AuthRepository.changePassword(currentPassword, newPassword)
        ↓
Re-authenticate current email/password
        ↓
Supabase auth.updateUser { password = newPassword }
```

### Logout

```text
AuthRepository.logout()
        ↓
Supabase signOut()
        ↓
SessionManager.clearSession()
```

---

## Profile data

`AuthRepository.getProfile()` maps the current Supabase user into `UserProfile`.

Current field mapping uses Supabase auth user data:

- `id` → `UserInfo.id`
- `email` → `UserInfo.email`
- `name` → `user_metadata.name` / `user_metadata.full_name`
- `avatarUrl` → `user_metadata.avatar_url`
- `joinedAt` → `createdAt`
- `streakDays` → `user_metadata.streak_days` when present

---

## Platform usage

### Android

`androidModule` includes:

- `sharedModule(baseUrl, supabaseUrl, supabaseAnonKey)`

### iOS

The Swift app constructs:

- `SessionManager`
- `AuthRepository(sessionManager, supabaseUrl, supabaseAnonKey)`

---

## Token storage

- Android: `AndroidTokenStorage` via `EncryptedSharedPreferences`
- iOS: `IosTokenStorage` via Keychain

`AuthToken.isExpired()` is the source of truth for deciding when startup refresh is needed.
