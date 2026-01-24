package com.example.mybooksapplication.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mybooksapplication.R
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.ui.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    viewModel: BookViewModel,
    onFabClick: () -> Unit,
    onBookClick: (Int) -> Unit
) {
    val books by viewModel.savedBooks.collectAsState()
    val selectedIds by viewModel.selectedBookIds.collectAsState()

    val isSelectionMode = selectedIds.isNotEmpty()
    val showDeleteDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BookListTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onDeleteClick = { showDeleteDialog.value = true }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onFabClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            EmptyBookListPlaceholder(padding = padding)
        } else {
            BookListContent(
                padding = padding,
                books = books,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                onToggleSelection = { id -> viewModel.toggleSelection(id) },
                onItemClick = onBookClick
            )
        }

        if (showDeleteDialog.value) {
            DeleteConfirmationDialog(
                count = selectedIds.size,
                onConfirm = {
                    viewModel.deleteSelectedBooks(books)
                    showDeleteDialog.value = false
                },
                onDismiss = { showDeleteDialog.value = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookListTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (isSelectionMode) {
        TopAppBar(
            title = { Text("$selectedCount 冊選択中") },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "キャンセル")
                }
            },
            actions = {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    } else {
        TopAppBar(title = { Text(stringResource(R.string.books_top_bar_title)) })
    }
}

@Composable
private fun BookListContent(
    padding: PaddingValues,
    books: List<BookEntity>,
    selectedIds: Set<Int>,
    isSelectionMode: Boolean,
    onToggleSelection: (Int) -> Unit,
    onItemClick: (Int) -> Unit
) {
    LazyColumn(contentPadding = padding) {
        items(books, key = { it.id }) { book ->
            val isSelected = selectedIds.contains(book.id)

            BookListItem(
                book = book,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(book.id)
                    } else {
                        onItemClick(book.id)
                    }
                },
                onLongClick = { onToggleSelection(book.id) }
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookListItem(
    book: BookEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent

    ListItem(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(backgroundColor),
        headlineContent = { Text(book.title) },
        supportingContent = { Text(book.author) },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                }
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    )
}

@Composable
fun EmptyBookListPlaceholder(
    padding: PaddingValues
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(padding), contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.books_empty))
    }
}

@Composable
private fun DeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("一括削除") },
        text = { Text("選択した $count 冊を削除しますか？\nこの操作は取り消せません。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("削除する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}