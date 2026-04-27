package com.akuplatform.shared.notifications

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of [NotificationService].
 *
 * Uses `UNUserNotificationCenter` to request notification permission and check
 * its current status.  Suspend functions bridge the ObjC completion-handler
 * callbacks with Kotlin coroutines.
 *
 * Usage: instantiate once in `AkuApp` and call [requestPermission] early in
 * the app lifecycle (e.g. in `AkuApp.init` or on the first task launch).
 */
class IosNotificationService : NotificationService {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun isPermissionGranted(): Boolean =
        suspendCancellableCoroutine { cont ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                val granted =
                    settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                cont.resume(granted)
            }
        }

    override suspend fun requestPermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            val options = UNAuthorizationOptionAlert or
                    UNAuthorizationOptionBadge or
                    UNAuthorizationOptionSound
            center.requestAuthorizationWithOptions(options) { granted, _ ->
                cont.resume(granted)
            }
        }
}
