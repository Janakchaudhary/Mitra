package com.mitra.learning.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChildHomeScreen(
    onBooks: () -> Unit,
    onParent: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🦁", style = MaterialTheme.typography.displayLarge)
        Text("મિત્ર", style = MaterialTheme.typography.displayMedium)
        Text("શીખો • રમો • શોધો", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Text("  રમીએ — coming in Milestone 3")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBooks, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Text("  પુસ્તક")
        }
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onParent) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Text("  Parent")
        }
    }
}
