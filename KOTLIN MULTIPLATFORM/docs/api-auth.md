# API Usage & Authentication Flow

This document describes how to interact with the Akulearn Wave 3 REST API and how the shared
authentication layer works end-to-end.

---

## Table of Contents

1. [Base URL](#base-url)
2. [Request & Response Models](#request--response-models)
3. [API Endpoints](#api-endpoints)
4. [Error Handling](#error-handling)
5. [Authentication Flow](#authentication-flow)
6. [Using AuthRepository from Platform Code](#using-authrepository-from-platform-code)
7. [Token Lifecycle](#token-lifecycle)

---

## Base URL

```
https://api.akulearn.com/v3
```

All paths below are relative to this base URL. The client is configured inside
`shared/src/commonMain/kotlin/com/akuplatform/shared/api/Wave3ApiClient.kt`.

---

## Request & Response Models

### Login / Register response — `TokenResponse`

All auth endpoints that return tokens share the same JSON schema:

```json
{
  "access_token":  "<JWT>",
  "refresh_token": "<opaque-token>",
  "expires_in":    3600
}
```

| Field           | Type   | Description                                           |
|-----------------|--------|-------------------------------------------------------|
| `access_token`  | string | Short-lived JWT used to authorise API requests.       |
| `refresh_token` | string | Long-lived opaque token used to obtain a new access token. |
| `expires_in`    | long   | Seconds until `access_token` expires.                 |

The shared layer converts this into an `AuthToken` data class (see
`shared/.../auth/model/AuthToken.kt`) and stores an additional computed field:

| Field        | Type | Description                                                    |
|--------------|------|----------------------------------------------------------------|
| `expiresAt`  | long | Unix epoch seconds at which the access token expires (computed locally). |

---

## API Endpoints

### `POST /auth/login`

Authenticate an existing user.

**Request body**
```json
{ "email": "user@example.com", "password": "s3cret" }
```

**Success response** `200 OK` — `TokenResponse` (see above).

**Kotlin call**
```kotlin
val result: Result<AuthToken> = wave3ApiClient.authenticate(email, password)
```

---

### `POST /auth/register`

Create a new user account. On success the user is automatically logged in.

**Request body**
```json
{ "email": "user@example.com", "password": "s3cret", "name": "Alice" }
```

**Success response** `201 Created` — `TokenResponse`.

**Kotlin call**
```kotlin
val result: Result<AuthToken> = wave3ApiClient.register(email, password, name)
```

---

### `POST /auth/refresh`

Exchange a refresh token for a new access token.

**Request body**
```json
{ "refresh_token": "<opaque-token>" }
```

**Success response** `200 OK` — `TokenResponse`.

**Kotlin call**
```kotlin
val result: Result<AuthToken> = wave3ApiClient.refreshToken(refreshToken)
```

---

### `POST /auth/forgot-password`

Trigger a password-reset email for the given address.

**Request body**
```json
{ "email": "user@example.com" }
```

**Success response** `204 No Content`.

**Kotlin call**
```kotlin
val result: Result<Unit> = wave3ApiClient.requestPasswordReset(email)
```

---

## Error Handling

`Wave3ApiClient` wraps every call with `safeCall { }`, which maps all failures to the
`ApiError` sealed class:

| Subclass                      | Cause                                                        |
|-------------------------------|--------------------------------------------------------------|
| `ApiError.Network(message)`   | Transport failure — no connectivity, DNS error, timeout.     |
| `ApiError.Unauthorized()`     | Server returned `401 Unauthorized` or `403 Forbidden`.       |
| `ApiError.ServerError(code)`  | Server returned any other non-2xx status.                    |

Every public method returns `Result<T>`. Callers should use `onSuccess` / `onFailure` or
`fold` to handle outcomes without throwing:

```kotlin
authRepository.login(email, password)
    .onSuccess { token -> /* navigate to home */ }
    .onFailure { error ->
        when (error) {
            is ApiError.Unauthorized -> showMessage("Invalid credentials")
            is ApiError.Network      -> showMessage("Check your connection")
            else                     -> showMessage("Something went wrong")
        }
    }
```

---

## Authentication Flow

```
User enters credentials
        │
        ▼
AuthRepository.login(email, password)
        │
        ▼
Wave3ApiClient.authenticate(email, password)   ──► POST /auth/login
        │ Result<AuthToken>
        ▼
SessionManager.saveSession(token)
  ├── TokenStorage.saveToken(token)   (encrypted on-device)
  └── _isLoggedIn.value = true        (StateFlow update → UI reacts)
        │
        ▼
UI observes isLoggedIn → navigates to HomeScreen
```

### Startup / token restore

```
App launch
        │
        ▼
AuthRepository.initialize()
        │
        ├── TokenStorage.getToken()
        │       ├── null  → isLoggedIn = false  → show LoginScreen
        │       └── token found
        │               │
        │               ├── token.isExpired() == false
        │               │       └── isLoggedIn = true  → show HomeScreen
        │               │
        │               └── token.isExpired() == true
        │                       │
        │                       ▼
        │               Wave3ApiClient.refreshToken(refreshToken)
        │                       ├── success → save new token → HomeScreen
        │                       └── failure → clearSession() → LoginScreen
```

### Logout

```
User taps "Sign out"
        │
        ▼
AuthRepository.logout()
        │
        ▼
SessionManager.clearSession()
  ├── TokenStorage.clearToken()   (wipes encrypted store)
  └── _isLoggedIn.value = false   (UI navigates back to LoginScreen)
```

---

## Using AuthRepository from Platform Code

`AuthRepository` is provided through Koin. Inject it wherever you need authentication state:

### Android (ViewModel)

```kotlin
class HomeViewModel(private val authRepository: AuthRepository) : ViewModel() {
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
```

### Observing login state (Compose)

```kotlin
val isLoggedIn by authRepository.isLoggedIn.collectAsState()
```

`isLoggedIn` is a `StateFlow<Boolean>` that emits whenever the session changes, so the
navigation graph reacts automatically without polling.

---

## Token Lifecycle

| Event                    | Access token | Refresh token | Stored?      |
|--------------------------|-------------|---------------|--------------|
| Login / Register         | Issued       | Issued        | Yes (encrypted) |
| `refreshToken` call      | Replaced     | Replaced      | Yes (encrypted) |
| `logout` / `clearSession`| Deleted      | Deleted       | Cleared       |

Tokens are stored on Android using `EncryptedSharedPreferences` (AES-256-GCM, keys in the
Android Keystore). On iOS they are stored in the system Keychain via `IosTokenStorage`.

`AuthToken.isExpired()` compares `expiresAt` (Unix epoch seconds computed at token creation)
against the current clock. If `expiresAt == 0` the token is treated as non-expired to avoid
false positives on older tokens that pre-date this field.
