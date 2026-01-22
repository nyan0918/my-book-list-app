package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.remote.BookSummary
import javax.inject.Inject

class SearchBookByIsbnUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(isbn: String): BookSummary? {
        if (isbn.isBlank()) return null
        return repository.fetchBookInfo(isbn)
    }
}