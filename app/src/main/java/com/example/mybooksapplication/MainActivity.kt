package com.example.mybooksapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.AppDatabase
import com.example.mybooksapplication.ui.theme.MyBooksApplicationTheme
import com.example.mybooksapplication.ui.viewmodel.BookViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 依存関係の初期化 (実務ではHiltで行う部分)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BookRepository(database.bookDao())
        val viewModelFactory = BookViewModelFactory(repository)

        setContent {
            MyBooksApplicationTheme {
                MyBooksApp(viewModelFactory = viewModelFactory)
            }
        }
    }
}