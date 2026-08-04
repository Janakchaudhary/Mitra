package com.mitra.learning.ui.assignment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    onCreate: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "પાછા") }
            Column(Modifier.weight(1f)) {
                Text("Parent Test Builder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("વિષય પસંદ કરો • 20/25 પ્રશ્ન • દરેક પ્રશ્ન 1 ગુણ")
            }
        }
        OutlinedTextField(state.title, onTitle, label = { Text("કસોટીનું નામ") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("વિષય", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ParentQuizTopic.entries.forEach { topic ->
                FilterChip(selected = state.topic == topic, onClick = { onTopic(topic) }, label = { Text(topic.titleGujarati) })
            }
        }
        if (state.topic == ParentQuizTopic.SKILL) {
            Text("કૌશલ્ય પસંદ કરો", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val excluded = setOf(BuiltInCurriculum.GUJ_READ_ALOUD, BuiltInCurriculum.ENG_READ_ALOUD)
            BuiltInCurriculum.concepts
                .filter { it.builtIn && it.id !in excluded }
                .groupBy { it.subject }
                .forEach { (subject, concepts) ->
                    Text(subject, style = MaterialTheme.typography.labelLarge)
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
        Text("કુલ ગુણ / પ્રશ્ન", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(20, 25).forEach { count ->
                FilterChip(selected = state.questionCount == count, onClick = { onCount(count) }, label = { Text("$count ગુણ") })
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("કેવી રીતે કામ કરે છે?", fontWeight = FontWeight.Bold)
                Text("Mitra તૈયાર પુસ્તકના સ્થાનિક પ્રશ્નો અથવા પસંદ કરેલી કૌશલ્ય રમતમાંથી પ્રશ્નપત્ર બનાવે છે. બાળક voice કે typing થી જવાબ આપી શકે છે.")
                Text("દરેક પ્રશ્ન = 1 ગુણ. સાચા જવાબ પછી appreciation બોલીને આગળનો પ્રશ્ન આપમેળે આવે છે.")
            }
        }
        Button(onClick = onCreate, enabled = !state.creating, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, null)
            Text(if (state.creating) "  તૈયાર થાય છે…" else "  કસોટી બનાવો")
        }
        state.activePlan?.let { plan ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Active: ${plan.title}", fontWeight = FontWeight.Bold)
                    Text("${plan.skillTitleGujarati ?: plan.topic.titleGujarati} • ${plan.questions.size} ગુણ")
                    OutlinedButton(onClick = onClear) { Icon(Icons.Default.Delete, null); Text(" કસોટી દૂર કરો") }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
    }
}
