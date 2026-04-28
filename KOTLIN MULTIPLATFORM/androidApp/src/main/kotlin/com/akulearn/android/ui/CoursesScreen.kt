package com.akulearn.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akuplatform.shared.course.model.Course

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    uiState: CoursesUiState,
    onCourseClick: (Course) -> Unit,
    onSearch: (String) -> Unit = {},
    onDurationFilter: (DurationFilter) -> Unit = {},
    onRefresh: () -> Unit = {},
    onErrorDismissed: () -> Unit
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
            TopAppBar(title = { Text("Courses") })
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onSearch,
                    placeholder = { Text("Search courses...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Duration filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        DurationFilter.ALL to "All",
                        DurationFilter.SHORT to "≤ 30 min",
                        DurationFilter.MEDIUM to "31–60 min",
                        DurationFilter.LONG to "> 60 min"
                    )
                    items(filters) { (filter, label) ->
                        FilterChip(
                            selected = uiState.durationFilter == filter,
                            onClick = { onDurationFilter(filter) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val displayCourses = uiState.filteredCourses
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading && displayCourses.isEmpty() -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        displayCourses.isEmpty() -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (uiState.query.isNotBlank()) "No courses match \"${uiState.query}\"" else "No courses available yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Check back soon!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(displayCourses, key = { it.id }) { course ->
                                    CourseCard(course = course, onClick = { onCourseClick(course) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (course.instructor.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By ${course.instructor}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (course.lessonCount > 0) {
                    Text(
                        text = "${course.lessonCount} lessons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (course.durationMinutes > 0) {
                    Text(
                        text = "${course.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (course.category.isNotBlank()) {
                    Text(
                        text = course.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoursesScreenPreview() {
    MaterialTheme {
        CoursesScreen(
            uiState = CoursesUiState(
                courses = listOf(
                    Course(id = "1", title = "Kotlin Basics", description = "Learn Kotlin from scratch", instructor = "Alice", lessonCount = 10, durationMinutes = 90)
                ),
                filteredCourses = listOf(
                    Course(id = "1", title = "Kotlin Basics", description = "Learn Kotlin from scratch", instructor = "Alice", lessonCount = 10, durationMinutes = 90)
                )
            ),
            onCourseClick = {},
            onErrorDismissed = {}
        )
    }
}

