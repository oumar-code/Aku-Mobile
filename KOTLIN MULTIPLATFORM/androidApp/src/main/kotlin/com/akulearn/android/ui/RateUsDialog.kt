package com.akulearn.android.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

/**
 * Dialog shown to users after they complete their first course,
 * asking them to rate the app on the Play Store.
 */
@Composable
fun RateUsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enjoying Akulearn? ⭐") },
        text = {
            Text("Great job completing your first course! If you're enjoying the app, please take a moment to rate us on the Play Store.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val packageName = context.packageName
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                        )
                    }
                    onDismiss()
                }
            ) {
                Text("Rate Now", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Preview
@Composable
private fun RateUsDialogPreview() {
    MaterialTheme {
        RateUsDialog(onDismiss = {})
    }
}
