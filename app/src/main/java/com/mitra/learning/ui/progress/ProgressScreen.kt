package com.mitra.learning.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.data.db.entity.SessionStatus
import com.mitra.learning.learning.progress.ConceptProgress
import com.mitra.learning.learning.progress.RecentSessionProgress
import com.mitra.learning.learning.progress.SubjectProgress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    when {
        state.loading && state.dashboard == null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Progress loading…")
            }
        }

        state.error != null && state.dashboard == null -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Progress could not be loaded", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(state.error)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onRefresh) { Text("Retry") }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
        }

        else -> {
            val dashboard = requireNotNull(state.dashboard)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("Learning progress", style = MaterialTheme.typography.headlineMedium)
                            Text("Calculated on this phone", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Overview", style = MaterialTheme.typography.titleLarge)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Metric("Today", "${dashboard.todayMinutes} min")
                                Metric("7 days", "${dashboard.last7DaysMinutes} min")
                                Metric("Sessions", dashboard.completedSessions.toString())
                            }
                            Text("Overall mastery ${dashboard.overallMastery.percent()}")
                            LinearProgressIndicator(
                                progress = { dashboard.overallMastery.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val accuracy = if (dashboard.assessedAttempts == 0) 0
                            else (dashboard.correctAttempts * 100f / dashboard.assessedAttempts).roundToInt()
                            Text("Assessed answers: ${dashboard.assessedAttempts} • correct: $accuracy%")
                        }
                    }
                }

                item {
                    val weekly = dashboard.weeklyReport
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Weekly parent report", style = MaterialTheme.typography.titleLarge)
                            val accuracy = if (weekly.assessed == 0) 0 else (weekly.correct * 100f / weekly.assessed).roundToInt()
                            Text("${weekly.minutes} min • ${weekly.assessed} assessed • $accuracy% correct")
                            weekly.mostPracticedTitleGujarati?.let { Text("Most practised: $it") }
                            weekly.needsPracticeTitleGujarati?.let { Text("Needs attention: $it") }
                            if (weekly.dueReviewCount > 0) Text("${weekly.dueReviewCount} skills are due for spaced review")
                        }
                    }
                }

                dashboard.recommendation?.let { recommendation ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Suggested next practice", style = MaterialTheme.typography.titleLarge)
                                Text(recommendation.titleGujarati, style = MaterialTheme.typography.titleMedium)
                                Text("${recommendation.subject} • mastery ${recommendation.mastery.percent()}")
                                Text(
                                    if (recommendation.fromBook) "From a prepared textbook"
                                    else "Built-in fallback curriculum",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                if (dashboard.subjects.isNotEmpty()) {
                    item { SectionTitle("Subjects") }
                    items(dashboard.subjects, key = { it.subject }) { SubjectRow(it) }
                }

                if (dashboard.standard2Skills.isNotEmpty()) {
                    dashboard.standard2Skills.groupBy { it.subject }.forEach { (subject, skills) ->
                        item { SectionTitle("$subject • Standard 2 skills") }
                        items(skills, key = { "skill-${it.conceptId}" }) { ConceptRow(it, showNotStarted = true) }
                    }
                }

                if (dashboard.needsPractice.isNotEmpty()) {
                    item { SectionTitle("Needs practice") }
                    items(dashboard.needsPractice, key = { "weak-${it.conceptId}" }) { ConceptRow(it) }
                }

                if (dashboard.strongConcepts.isNotEmpty()) {
                    item { SectionTitle("Strong concepts") }
                    items(dashboard.strongConcepts, key = { "strong-${it.conceptId}" }) { ConceptRow(it) }
                }

                if (dashboard.recentSessions.isNotEmpty()) {
                    item { SectionTitle("Recent sessions") }
                    items(dashboard.recentSessions, key = { it.id }) { SessionRow(it) }
                }

                if (dashboard.completedSessions == 0) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "No completed sessions yet. Let your child finish a learning session and progress will appear here.",
                                modifier = Modifier.padding(18.dp),
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                        Text("Refresh")
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SubjectRow(item: SubjectProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.subject, style = MaterialTheme.typography.titleMedium)
                Text(item.mastery.percent())
            }
            LinearProgressIndicator(
                progress = { item.mastery.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${item.practicedConcepts} concepts practiced • ${item.totalAttempts} assessed answers", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConceptRow(item: ConceptProgress, showNotStarted: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.titleGujarati, style = MaterialTheme.typography.titleMedium)
                Text(item.mastery.percent())
            }
            LinearProgressIndicator(
                progress = { item.mastery.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (showNotStarted && item.attempts == 0) "${item.subject} • not started"
                else "${item.subject} • ${item.correctAttempts}/${item.attempts} correct • ${item.hints} hints",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SessionRow(item: RecentSessionProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.conceptTitleGujarati, style = MaterialTheme.typography.titleMedium)
                Text(item.startedAt.dateLabel(), style = MaterialTheme.typography.bodySmall)
            }
            val minutes = if (item.durationSeconds <= 0) 0 else (item.durationSeconds + 59) / 60
            Text("$minutes min • ${item.status.label()} • ${item.correct}/${item.assessed} correct")
            if (item.participation > 0) {
                Text("${item.participation} exploration activities", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
    }
}

private fun Float.percent(): String = "${(coerceIn(0f, 1f) * 100).roundToInt()}%"

private fun Long.dateLabel(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))

private fun SessionStatus.label(): String = when (this) {
    SessionStatus.COMPLETED -> "completed"
    SessionStatus.STOPPED -> "stopped"
    SessionStatus.ACTIVE -> "active"
}
