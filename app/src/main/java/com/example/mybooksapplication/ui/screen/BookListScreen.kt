package com.example.mybooksapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mybooksapplication.R
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.ui.viewmodel.BookViewModel

@Composable
fun BookListScreen(
    viewModel: BookViewModel,
    onFabClick: () -> Unit
) {
    val books by viewModel.savedBooks.collectAsState()

    // 削除しようとしている本を一時保存する状態
    // nullならダイアログ非表示、本が入っていればダイアログ表示
    val bookToDeleteState = remember { mutableStateOf<BookEntity?>(null) }

    BookListScreenContent(
        books = books,
        bookToDeleteState = bookToDeleteState,
        onFabClick = onFabClick,
        onConfirmClick = { book -> viewModel.deleteBook(book) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreenContent(
    books: List<BookEntity>,
    bookToDeleteState: MutableState<BookEntity?>,
    onFabClick: () -> Unit,
    onConfirmClick: (BookEntity) -> Unit,
) {

    // テンプレートレイアウト
    // トップバー + FAB + コンテンツ領域
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.books_top_bar_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            // 本が一冊も無い場合のプレースホルダー表示
            EmptyBookListPlaceholder(padding = padding)
        } else {
            // 本のリスト表示
            BookList(
                padding = padding,
                books = books,
                onDeleteIconClick = { book -> bookToDeleteState.value = book }
            )
        }

        // bookToDeleteに値が入っている＝ダイアログを表示すべき状態
        if (bookToDeleteState.value != null) {
            val targetBook = bookToDeleteState.value!!

            // 削除確認ダイアログ表示
            DeleteAlertDialog(
                book = targetBook,
                onConfirmClick = {
                    onConfirmClick(targetBook) // 本当に削除実行
                    bookToDeleteState.value = null // ダイアログを閉じる
                },
                onDismissRequest = {
                    bookToDeleteState.value = null // ダイアログを閉じる
                }
            )
        }
    }
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
fun BookList(
    padding: PaddingValues,
    books: List<BookEntity>,
    onDeleteIconClick: (BookEntity) -> Unit
) {
    LazyColumn(contentPadding = padding) {
        items(books) { book ->
            ListItem(
                headlineContent = { Text(book.title) },
                supportingContent = { Text(book.author) },
                leadingContent = {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.size(75.dp),
                        placeholder = painterResource(R.drawable.ic_launcher_foreground), // 仮の画像
                        error = painterResource(R.drawable.ic_launcher_foreground) // 実際には「No Image」画像を用意して指定
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onDeleteIconClick(book) }) {
                        Icon(Icons.Default.Delete, contentDescription = "delete")
                    }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun DeleteAlertDialog(
    book: BookEntity,
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "削除の確認") },
        text = {
            Text("『${book.title}』を削除してもよろしいですか？\nこの操作は取り消せません。")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("削除する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("キャンセル")
            }
        }
    )
}