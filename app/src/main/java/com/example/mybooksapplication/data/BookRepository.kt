package com.example.mybooksapplication.data

import android.util.Log
import com.example.mybooksapplication.data.local.BookDao
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.ApiClient
import com.example.mybooksapplication.data.remote.BookSummary
import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {

    // 保存済み書籍リストのFlow
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun fetchBookInfo(isbn: String): BookSummary? {
        Log.d(TAG, "isbn: $isbn")
        return try {
            // ISBNで検索クエリを作成 ("isbn:978xxxx")
            val response = ApiClient.service.searchBook("isbn:$isbn")

            // 最初の1件を取得
            val item = response.items?.firstOrNull() ?: return null
            val info = item.volumeInfo

            // 画像URLの取得とHTTPS化処理
            val rawUrl = info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail ?: ""
            val secureUrl = rawUrl.replace("http://", "https://")

            // アプリ内で使う形に変換して返す
            BookSummary(
                isbn = isbn, // レスポンスにはISBNが含まれない場合があるので引数を使う
                title = info.title ?: "タイトル不明",
                // 著者がリストなのでカンマ区切り文字列にする
                author = info.authors?.joinToString(", ") ?: "著者不明",
                coverUrl = secureUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google API Error", e)
            null
        }
    }

    suspend fun saveBook(book: BookEntity) = bookDao.insertBook(book)
    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    companion object {
        private val TAG = this::class.java.simpleName
    }
}