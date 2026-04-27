package com.akuplatform.shared.notifications

/**
 * Platform-agnostic abstraction for push-notification permission management.
 *
 * Android: permission is requested via the [RequestNotificationPermission] composable
 *          (androidApp). [requestPermission] reports the current OS-level grant state.
 * iOS:     [requestPermission] triggers the system permission dialog via
 *          UNUserNotificationCenter (IosNotificationService in iosMain).
 */
interface NotificationService {
    /** Returns `true` if notification permission is currently granted by the OS. */
    suspend fun isPermissionGranted(): Boolean

    /**
     * Requests notification permission from the OS.
     * On Android the actual system dialog is shown by the Compose composable;
     * this method only reflects the resulting state.
     * On iOS this method triggers the dialog and returns the user's decision.
     * Returns `true` if permission is (now) granted.
     */
    suspend fun requestPermission(): Boolean
}
