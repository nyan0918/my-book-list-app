package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.BookEntity
import javax.inject.Inject

class DeleteMultipleBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(books: List<BookEntity>) {
        repository.deleteBooks(books)
    }
}