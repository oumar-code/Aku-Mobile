package com.akulearn.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.akuplatform.shared.course.model.Lesson
import com.akuplatform.shared.course.model.LessonContentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Simple JSON quiz format understood by the lesson player. */
@Serializable
private data class QuizQuestion(
    val question: String = "",
    val choices: List<String> = emptyList(),
    val correctIndex: Int = -1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    uiState: LessonPlayerUiState,
    onMarkComplete: () -> Unit,
    onErrorDismissed: () -> Unit,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error)
            onErrorDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.lesson?.title ?: "Lesson") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val lesson = uiState.lesson
            if (lesson == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Lesson-level progress bar (thin, beneath the top bar)
                    LinearProgressIndicator(
                        progress = { uiState.playbackFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (uiState.isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (lesson.durationMinutes > 0) {
                            Text(
                                text = "${lesson.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Content by type
                        when (lesson.contentType) {
                            LessonContentType.VIDEO -> {
                                if (lesson.contentUrl.isNotBlank()) {
                                    VideoPlayer(
                                        contentUrl = lesson.contentUrl,
                                        resumePositionMs = uiState.resumePositionMs,
                                        onPositionChanged = onPositionChanged
                                    )
                                }
                                if (lesson.body.isNotBlank()) {
                                    Text(
                                        text = lesson.body,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            LessonContentType.QUIZ -> {
                                QuizContent(body = lesson.body)
                            }
                            else -> {
                                val body = lesson.body.ifBlank { lesson.description }
                                if (body.isNotBlank()) {
                                    Text(
                                        text = body,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = "No content available for this lesson.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Mark Complete button
                        if (uiState.isCompleted) {
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("  Lesson Completed", color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Button(
                                onClick = onMarkComplete,
                                enabled = !uiState.isMarkingComplete,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (uiState.isMarkingComplete) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Mark as Complete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Plays [contentUrl] in-app using Media3 ExoPlayer, resuming from [resumePositionMs]. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayer(
    contentUrl: String,
    resumePositionMs: Long = 0L,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val exoPlayer = remember(contentUrl) {
        ExoPlayer.Builder(context).build().also { player ->
            player.setMediaItem(MediaItem.fromUri(contentUrl))
            player.prepare()
            if (resumePositionMs > 0L) {
                player.seekTo(resumePositionMs)
            }
            player.playWhenReady = false
        }
    }

    // Report playback position changes to the ViewModel
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val duration = exoPlayer.duration.takeIf { it > 0L } ?: return
                onPositionChanged(exoPlayer.currentPosition, duration)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            // Persist final position before releasing
            val duration = exoPlayer.duration.takeIf { it > 0L }
            if (duration != null) {
                onPositionChanged(exoPlayer.currentPosition, duration)
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    )
}

/**
 * Renders a multiple-choice quiz parsed from [body] JSON.
 *
 * Expected JSON format:
 * ```json
 * { "question": "...", "choices": ["A", "B", "C", "D"], "correctIndex": 0 }
 * ```
 * If the body cannot be parsed the raw text is displayed instead.
 */
@Composable
private fun QuizContent(body: String) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var revealed by remember { mutableStateOf(false) }

    val quiz = remember(body) {
        try {
            Json { ignoreUnknownKeys = true }.decodeFromString(QuizQuestion.serializer(), body)
                .takeIf {
                    it.question.isNotBlank() &&
                        it.choices.isNotEmpty() &&
                        it.correctIndex in it.choices.indices
                }
        } catch (e: Exception) {
            android.util.Log.w("QuizContent", "Failed to parse quiz JSON: ${e.message}")
            null
        }
    }

    if (quiz != null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = quiz.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            quiz.choices.forEachIndexed { index, choice ->
                val isSelected = selectedIndex == index
                val isCorrect = index == quiz.correctIndex
                val cardColor = when {
                    !revealed && isSelected -> MaterialTheme.colorScheme.primaryContainer
                    revealed && isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                    revealed && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                val border = if (isSelected && !revealed) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !revealed) { selectedIndex = index },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = border
                ) {
                    Text(
                        text = choice,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (selectedIndex >= 0 && !revealed) {
                Button(
                    onClick = { revealed = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Answer")
                }
            }

            if (revealed) {
                val correct = selectedIndex == quiz.correctIndex
                Text(
                    text = if (correct) {
                        "✓ Correct!"
                    } else {
                        "✗ Incorrect. Correct answer: ${quiz.choices.getOrNull(quiz.correctIndex) ?: ""}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        // Fall back to plain text when the body isn't valid quiz JSON
        Text(
            text = body.ifBlank { "No quiz content available." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonPlayerScreenPreview() {
    MaterialTheme {
        LessonPlayerScreen(
            uiState = LessonPlayerUiState(
                lesson = Lesson(
                    id = "1",
                    courseId = "c1",
                    title = "Introduction to Kotlin",
                    description = "Learn the basics",
                    body = "# Welcome\n\nThis lesson covers the basics of Kotlin programming.",
                    durationMinutes = 15
                ),
                playbackFraction = 0.45f
            ),
            onMarkComplete = {},
            onErrorDismissed = {},
            onBack = {}
        )
    }
}
