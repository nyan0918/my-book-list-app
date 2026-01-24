package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.BookSummary
import javax.inject.Inject

class SaveMultipleBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(summaries: List<BookSummary>) {
        val entities = summaries.map { summary ->
            BookEntity(
                isbn = summary.isbn,
                title = summary.title,
                author = summary.author,
                coverUrl = summary.coverUrl
            )
        }
        repository.saveBooks(entities)
    }
}