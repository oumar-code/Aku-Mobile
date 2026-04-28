package com.akulearn.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akuplatform.shared.course.model.Lesson

/**
 * Full-screen lesson list for a course, showing each lesson's title, duration,
 * a completed / not-completed indicator, and a fractional playback progress bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(
    courseTitle: String,
    lessons: List<Lesson>,
    isLoading: Boolean,
    /** Map of lessonId → playback fraction (0.0–1.0) from saved positions. */
    lessonProgressFractions: Map<String, Float> = emptyMap(),
    onLessonClick: (Lesson) -> Unit = {},
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(courseTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                lessons.isEmpty() -> {
                    Text(
                        text = "No lessons available yet.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            lessons.sortedBy { it.orderIndex },
                            key = { _, lesson -> lesson.id }
                        ) { index, lesson ->
                            LessonRow(
                                lesson = lesson,
                                number = index + 1,
                                playbackFraction = lessonProgressFractions[lesson.id] ?: 0f,
                                onClick = { onLessonClick(lesson) }
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single row within a lesson list, shared by [LessonsScreen] and [CourseDetailScreen].
 *
 * When [playbackFraction] is between 0 and 1 (exclusive), a thin [LinearProgressIndicator]
 * is shown beneath the lesson title to indicate partial progress.
 */
@Composable
fun LessonRow(
    lesson: Lesson,
    number: Int? = null,
    playbackFraction: Float = 0f,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Lesson: ${lesson.title}" }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Completion indicator
            if (lesson.isCompleted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = "Not completed",
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val prefix = if (number != null) "$number. " else ""
                Text(
                    text = "$prefix${lesson.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (lesson.isCompleted) FontWeight.Normal else FontWeight.Medium
                )
                if (lesson.durationMinutes > 0) {
                    Text(
                        text = "${lesson.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Partial-progress bar (only shown when lesson is in-progress)
        val fraction = if (lesson.isCompleted) 1f else playbackFraction
        if (fraction > 0f) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 52.dp),
                color = if (lesson.isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonsScreenPreview() {
    MaterialTheme {
        LessonsScreen(
            courseTitle = "Kotlin Basics",
            lessons = listOf(
                Lesson(id = "1", courseId = "c1", title = "Introduction", durationMinutes = 10, isCompleted = true, orderIndex = 0),
                Lesson(id = "2", courseId = "c1", title = "Variables", durationMinutes = 12, orderIndex = 1),
                Lesson(id = "3", courseId = "c1", title = "Functions", durationMinutes = 15, orderIndex = 2)
            ),
            lessonProgressFractions = mapOf("2" to 0.6f),
            isLoading = false,
            onBack = {}
        )
    }
}
