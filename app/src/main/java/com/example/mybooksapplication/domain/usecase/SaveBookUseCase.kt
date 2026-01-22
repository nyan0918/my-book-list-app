package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.BookSummary
import javax.inject.Inject

class SaveBookUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(summary: BookSummary) {
        // UI用のデータモデル(Summary)を、DB用のモデル(Entity)に変換する
        val entity = BookEntity(
            isbn = summary.isbn,
            title = summary.title,
            author = summary.author,
            coverUrl = summary.coverUrl
        )
        repository.saveBook(entity)
    }
}