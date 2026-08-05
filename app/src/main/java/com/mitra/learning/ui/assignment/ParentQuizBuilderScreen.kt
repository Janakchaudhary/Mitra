package com.mitra.learning.ui.assignment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mitra.learning.learning.assignment.ParentQuizTopic
import com.mitra.learning.learning.curriculum.BuiltInCurriculum

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentQuizBuilderScreen(
    state: ParentQuizBuilderUiState,
    onTitle: (String) -> Unit,
    onTopic: (ParentQuizTopic) -> Unit,
    onCount: (Int) -> Unit,
    onSkill: (String) -> Unit,
    onBook: (String) -> Unit,
    onChapter: (String) -> Unit,
    onCreate: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val availableChapterQuestions = state.selectedChapterId
        ?.let { state.questionCountByChapter[it] }
        ?: 0
    val chapterHasEnoughQuestions = state.topic != ParentQuizTopic.PREPARED_BOOK ||
        availableChapterQuestions >= state.questionCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "પાછા") }
                Column(Modifier.weight(1f)) {
                    Text("Focused Test Builder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("એક પુસ્તક • એક પાઠ • 20/25 ગુણ")
                }
                Icon(Icons.Default.Quiz, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        item {
            QuizStepCard(number = 1, title = "કસોટીનો પ્રકાર", subtitle = "પાઠવાર અભ્યાસ માટે ‘પુસ્તકનો પાઠ’ પસંદ રાખો") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParentQuizTopic.entries.forEach { topic ->
                        FilterChip(
                            selected = state.topic == topic,
                            onClick = { onTopic(topic) },
                            label = { Text(topic.titleGujarati) },
                        )
                    }
                }
            }
        }

        if (state.topic == ParentQuizTopic.PREPARED_BOOK) {
            item {
                QuizStepCard(number = 2, title = "પુસ્તક પસંદ કરો", subtitle = "ફક્ત તૈયાર પાઠ ધરાવતા પુસ્તકો દેખાય છે") {
                    if (state.books.isEmpty()) {
                        Text("હજુ કોઈ READY પુસ્તક નથી. My Books માં પાઠ Prepare કરો અથવા .mitrabook Import કરો.")
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.books.forEach { book ->
                                FilterChip(
                                    selected = state.selectedBookId == book.id,
                                    onClick = { onBook(book.id) },
                                    label = { Text(book.title) },
                                    leadingIcon = { Icon(Icons.Default.MenuBook, null) },
                                )
                            }
                        }
                    }
                }
            }

            item {
                QuizStepCard(number = 3, title = "પાઠ પસંદ કરો", subtitle = "બાળકને આ પાઠમાંથી જ પ્રશ્નો મળશે") {
                    if (state.chapters.isEmpty()) {
                        Text("પસંદ કરેલા પુસ્તકમાં READY પાઠ નથી.")
                    }
                }
            }
            items(state.chapters, key = { it.id }) { chapter ->
                val selected = state.selectedChapterId == chapter.id
                val questionCount = state.questionCountByChapter[chapter.id] ?: 0
                Card(
                    onClick = { onChapter(chapter.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                            Box(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text((chapter.chapterNumber ?: 0).toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(chapter.titleGujarati, fontWeight = FontWeight.Bold)
                            Text("PDF પાનું ${chapter.startPage}–${chapter.endPage} • $questionCount તૈયાર પ્રશ્ન")
                        }
                        if (selected) Icon(Icons.Default.CheckCircle, "પસંદ", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (state.topic == ParentQuizTopic.SKILL) {
            item {
                QuizStepCard(number = 2, title = "કૌશલ્ય પસંદ કરો", subtitle = "એક જ કૌશલ્ય પર ધ્યાન કેન્દ્રિત કરો") {
                    val excluded = setOf(BuiltInCurriculum.GUJ_READ_ALOUD, BuiltInCurriculum.ENG_READ_ALOUD)
                    BuiltInCurriculum.concepts
                        .filter { it.builtIn && it.id !in excluded }
                        .groupBy { it.subject }
                        .forEach { (subject, concepts) ->
                            Text(subject, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                concepts.forEach { concept ->
                                    FilterChip(
                                        selected = state.selectedSkillConceptId == concept.id,
                                        onClick = { onSkill(concept.id) },
                                        label = { Text(concept.titleGujarati) },
                                    )
                                }
                            }
                        }
                }
            }
        }

        item {
            val step = if (state.topic == ParentQuizTopic.PREPARED_BOOK) 4 else 3
            QuizStepCard(number = step, title = "ગુણ અને પ્રશ્ન", subtitle = "દરેક પ્રશ્ન 1 ગુણ") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(20, 25).forEach { count ->
                        val enabled = state.topic != ParentQuizTopic.PREPARED_BOOK || availableChapterQuestions >= count
                        FilterChip(
                            selected = state.questionCount == count,
                            onClick = { onCount(count) },
                            enabled = enabled,
                            label = { Text("$count ગુણ") },
                        )
                    }
                }
                if (state.topic == ParentQuizTopic.PREPARED_BOOK && state.selectedChapterId != null && availableChapterQuestions < 20) {
                    Text(
                        "આ પાઠમાં $availableChapterQuestions અલગ પ્રશ્ન છે. ઓછામાં ઓછા 20 માટે પાઠ ફરી Prepare કરો અથવા વધુ પ્રશ્નોવાળી .mitrabook Import કરો.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitle,
                label = { Text("કસોટીનું નામ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            val selectedBook = state.books.firstOrNull { it.id == state.selectedBookId }
            val selectedChapter = state.chapters.firstOrNull { it.id == state.selectedChapterId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("કસોટી સારાંશ", fontWeight = FontWeight.Bold)
                    Text(
                        when (state.topic) {
                            ParentQuizTopic.PREPARED_BOOK -> "${selectedBook?.title ?: "પુસ્તક પસંદ નથી"} → ${selectedChapter?.titleGujarati ?: "પાઠ પસંદ નથી"}"
                            ParentQuizTopic.SKILL -> BuiltInCurriculum.concepts.firstOrNull { it.id == state.selectedSkillConceptId }?.titleGujarati.orEmpty()
                            else -> state.topic.titleGujarati
                        }
                    )
                    Text("${state.questionCount} પ્રશ્ન • ${state.questionCount} ગુણ • voice અને typing")
                    if (state.topic == ParentQuizTopic.PREPARED_BOOK) {
                        Text("પ્રશ્નો બીજા પાઠમાંથી મિશ્ર નહીં થાય.", fontWeight = FontWeight.SemiBold)
                        Text("ઉપલબ્ધ અલગ પ્રશ્ન: $availableChapterQuestions")
                    }
                }
            }
        }

        item {
            val canCreate = !state.creating &&
                (state.topic != ParentQuizTopic.PREPARED_BOOK || state.selectedChapterId != null) &&
                chapterHasEnoughQuestions
            Button(onClick = onCreate, enabled = canCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome, null)
                Text(if (state.creating) "  ગોઠવાય છે…" else "  કસોટી બનાવો")
            }
        }

        state.activePlan?.let { plan ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Child Home પર તૈયાર", fontWeight = FontWeight.Bold)
                        Text(plan.title)
                        Text("${plan.bookTitle?.let { "$it → " }.orEmpty()}${plan.chapterTitleGujarati ?: plan.skillTitleGujarati ?: plan.topic.titleGujarati} • ${plan.questions.size} ગુણ")
                        OutlinedButton(onClick = onClear) {
                            Icon(Icons.Default.Delete, null)
                            Text(" કસોટી દૂર કરો")
                        }
                    }
                }
            }
        }

        state.message?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun QuizStepCard(
    number: Int,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}
