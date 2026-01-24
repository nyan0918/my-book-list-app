package com.example.mybooksapplication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mybooksapplication.ui.screen.BookDetailScreen
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
                onFabClick = { navController.navigate("scan") },
                onBookClick = { bookId ->
                    navController.navigate("detail/$bookId")
                }
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
        // 詳細画面
        composable(
            route = "detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.IntType })
        ) { backStackEntry ->
            // URL引数からIDを取り出す
            val bookId = backStackEntry.arguments?.getInt("bookId") ?: 0
            val viewModel: BookViewModel = hiltViewModel()

            BookDetailScreen(
                bookId = bookId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}