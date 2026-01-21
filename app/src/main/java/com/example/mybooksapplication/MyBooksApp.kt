package com.example.mybooksapplication

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mybooksapplication.ui.screen.BookListScreen
import com.example.mybooksapplication.ui.screen.ScanScreen
import com.example.mybooksapplication.ui.viewmodel.BookViewModel
import com.example.mybooksapplication.ui.viewmodel.BookViewModelFactory

@Composable
fun MyBooksApp(viewModelFactory: BookViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: BookViewModel = viewModel(factory = viewModelFactory)

    NavHost(navController = navController, startDestination = "books") {
        // 書籍リスト画面
        composable("books") {
            BookListScreen(
                viewModel = viewModel,
                onFabClick = { navController.navigate("scan") }
            )
        }
        // スキャン画面
        composable("scan") {
            ScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}