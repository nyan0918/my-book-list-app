package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.BookEntity
import javax.inject.Inject

class DeleteBookUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(book: BookEntity) {
        repository.deleteBook(book)
    }
}