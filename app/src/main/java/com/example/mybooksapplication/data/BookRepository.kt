package com.example.mybooksapplication.data

import android.util.Log
import com.example.mybooksapplication.BuildConfig
import com.example.mybooksapplication.data.local.BookDao
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.BookSummary
import com.example.mybooksapplication.data.remote.GoogleBooksService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val service: GoogleBooksService
) {

    // 保存済み書籍リストのFlow
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun fetchBookInfo(isbn: String): BookSummary? {
        Log.d(TAG, "isbn: $isbn")
        return try {
            val response = service.searchBook("isbn:$isbn", BuildConfig.BOOKS_API_KEY)

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

    fun getBookById(id: Int) = bookDao.getBookById(id)

    suspend fun saveBook(book: BookEntity) = bookDao.insertBook(book)

    suspend fun saveBooks(books: List<BookEntity>) = bookDao.insertAll(books)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun deleteBooks(books: List<BookEntity>) = bookDao.deleteBooks(books)

    companion object {
        private val TAG = BookRepository::class.java.simpleName
    }
}