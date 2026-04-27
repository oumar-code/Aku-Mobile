package com.akuplatform.shared.api

/**
 * Typed error hierarchy for all API operations.
 *
 * Using a sealed class means every call site can handle errors exhaustively
 * via a `when` expression rather than relying on raw exception messages.
 */
sealed class ApiError(message: String) : Exception(message) {

    /** A transport-level failure (no connectivity, DNS failure, timeout, etc.). */
    class Network(message: String = "Network error. Check your connection.") : ApiError(message)

    /** The server rejected the request with a 401 / 403 status. */
    class Unauthorized(message: String = "Invalid credentials.") : ApiError(message)

    /** The server returned an unexpected non-2xx status. */
    class ServerError(
        val code: Int,
        message: String = "Server error ($code). Please try again later."
    ) : ApiError(message)
}
