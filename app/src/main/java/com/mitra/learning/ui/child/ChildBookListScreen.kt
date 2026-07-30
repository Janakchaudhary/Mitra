package com.mitra.learning.ui.child

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.data.db.entity.BookEntity

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChildBookListScreen(
    books: List<BookEntity>,
    onOpenPdf: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("પુસ્તક") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Text("હજુ પુસ્તક ઉમેર્યું નથી")
                    Text("Parent mode માંથી પુસ્તક ઉમેરો")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(books, key = { it.id }) { book ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPdf(book.id) }
                            .padding(vertical = 16.dp),
                    ) {
                        Text(book.title, style = MaterialTheme.typography.titleLarge)
                        Text("${book.subject} • ${book.pageCount} pages")
                    }
                }
            }
        }
    }
}
