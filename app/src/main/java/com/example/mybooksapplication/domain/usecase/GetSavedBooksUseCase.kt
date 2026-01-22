package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    operator fun invoke(): Flow<List<BookEntity>> {
        return repository.allBooks
    }
}