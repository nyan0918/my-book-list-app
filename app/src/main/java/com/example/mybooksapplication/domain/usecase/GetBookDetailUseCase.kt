package com.example.mybooksapplication.domain.usecase

import com.example.mybooksapplication.data.BookRepository
import com.example.mybooksapplication.data.local.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookDetailUseCase @Inject constructor(
    private val repository: BookRepository
) {
    operator fun invoke(id: Int): Flow<BookEntity?> {
        return repository.getBookById(id)
    }
}