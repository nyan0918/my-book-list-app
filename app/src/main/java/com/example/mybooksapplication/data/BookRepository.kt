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
            val response = ApiClient.service.getBook(isbn)
            val result = response.firstOrNull()?.summary
            Log.d(TAG, "result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "API Error: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun saveBook(book: BookEntity) = bookDao.insertBook(book)
    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    companion object {
        private val TAG = this::class.java.simpleName
    }
}