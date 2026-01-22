package com.example.mybooksapplication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mybooksapplication.ui.screen.BookListScreen
import com.example.mybooksapplication.ui.screen.ScanScreen
import com.example.mybooksapplication.ui.viewmodel.BookViewModel

@Composable
fun MyBooksApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "books") {
        // 書籍リスト画面
        composable("books") {
            val viewModel: BookViewModel = hiltViewModel()
            BookListScreen(
                viewModel = viewModel,
                onFabClick = { navController.navigate("scan") }
            )
        }
        // スキャン画面
        composable("scan") {
            val viewModel: BookViewModel = hiltViewModel()
            ScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}