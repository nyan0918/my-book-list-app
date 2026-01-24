package com.example.mybooksapplication.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybooksapplication.R
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.BookSummary
import com.example.mybooksapplication.domain.usecase.DeleteBookUseCase
import com.example.mybooksapplication.domain.usecase.DeleteMultipleBooksUseCase
import com.example.mybooksapplication.domain.usecase.GetBookDetailUseCase
import com.example.mybooksapplication.domain.usecase.GetSavedBooksUseCase
import com.example.mybooksapplication.domain.usecase.SaveBookUseCase
import com.example.mybooksapplication.domain.usecase.SaveMultipleBooksUseCase
import com.example.mybooksapplication.domain.usecase.SearchBookByIsbnUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class Success(val book: BookSummary) : ScanUiState
    data class Error(@param:StringRes val message: Int) : ScanUiState
}

@HiltViewModel
class BookViewModel @Inject constructor(
    getSavedBooksUseCase: GetSavedBooksUseCase,
    private val searchBookByIsbnUseCase: SearchBookByIsbnUseCase,
    private val saveBookUseCase: SaveBookUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val saveMultipleBooksUseCase: SaveMultipleBooksUseCase,
    private val deleteMultipleBooksUseCase: DeleteMultipleBooksUseCase,
    private val getBookDetailUseCase: GetBookDetailUseCase
) : ViewModel() {

    val savedBooks: StateFlow<List<BookEntity>> = getSavedBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _scannedBuffer = MutableStateFlow<List<BookSummary>>(emptyList())
    val scannedBuffer: StateFlow<List<BookSummary>> = _scannedBuffer.asStateFlow()

    private val _selectedBookIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedBookIds: StateFlow<Set<Int>> = _selectedBookIds.asStateFlow()

    /**
     * モード切り替え.
     */
    fun setBatchMode(isBatch: Boolean) {
        Log.d(TAG, "[setBatchMode]isBatch: $isBatch")
        _isBatchMode.value = isBatch
        resetScanState()
        clearBuffer()
    }

    /**
     * スキャン検知時の処理.
     * @param isbn
     */
    fun onIsbnScanned(isbn: String) {
        if (_scanState.value !is ScanUiState.Idle) return
        Log.d(TAG, "[onIsbnScanned]: $isbn")
        viewModelScope.launch {
            if (_isBatchMode.value) {
                // ===========================
                // 【一括モードの処理】
                // ===========================
                if (_scannedBuffer.value.any { it.isbn == isbn }) return@launch
                _scanState.value = ScanUiState.Loading
                val result = searchBookByIsbnUseCase(isbn)
                if (result != null) {
                    _scannedBuffer.value += result
                    _scanState.value = ScanUiState.Idle
                } else {
                    _scanState.value = ScanUiState.Idle
                }

            } else {
                // ===========================
                // 【単体モードの処理】
                // ===========================
                if (_scanState.value !is ScanUiState.Idle) return@launch

                _scanState.value = ScanUiState.Loading
                val result = searchBookByIsbnUseCase(isbn)

                if (result != null) {
                    _scanState.value = ScanUiState.Success(result)
                } else {
                    _scanState.value = ScanUiState.Error(R.string.scan_not_found)
                }
            }
        }
    }

    /**
     * スキャンした本を保存する.
     */
    fun saveCurrentBook() {
        Log.d(TAG, "[saveCurrentBook]")
        val state = _scanState.value
        if (state is ScanUiState.Success) {
            viewModelScope.launch {
                saveBookUseCase(state.book)
                resetScanState()
            }
        }
    }

    /**
     * スキャンした本を保存する(一括).
     */
    fun saveBufferedBooks() {
        Log.d(TAG, "[saveBufferedBooks]")
        val books = _scannedBuffer.value
        if (books.isNotEmpty()) {
            viewModelScope.launch {
                saveMultipleBooksUseCase(books)
                clearBuffer()
            }
        }
    }

    /**
     * リストから本を削除する.
     */
    fun deleteBook(book: BookEntity) {
        Log.d(TAG, "[deleteBook]: ${book.title}")
        viewModelScope.launch { deleteBookUseCase(book) }
    }

    /**
     * スキャン状態をリセットする.
     */
    fun resetScanState() {
        Log.d(TAG, "[resetScanState]")
        _scanState.value = ScanUiState.Idle
    }

    /**
     * 選択の切り替え.
     */
    fun toggleSelection(bookId: Int) {
        Log.d(TAG, "[toggleSelection]")
        val currentIds = _selectedBookIds.value
        if (currentIds.contains(bookId)) {
            _selectedBookIds.value = currentIds - bookId
        } else {
            _selectedBookIds.value = currentIds + bookId
        }
    }

    /**
     * 選択した本を一括削除.
     */
    fun deleteSelectedBooks(allBooks: List<BookEntity>) {
        Log.d(TAG, "[deleteSelectedBooks]: ${allBooks.size}")
        val idsToDelete = _selectedBookIds.value
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            val booksToDelete = allBooks.filter { idsToDelete.contains(it.id) }

            deleteMultipleBooksUseCase(booksToDelete)
            clearSelection()
        }
    }

    /**
     * 選択モード解除.
     */
    fun clearSelection() {
        Log.d(TAG, "[clearSelection]")
        _selectedBookIds.value = emptySet()
    }

    /**
     * 書籍詳細を取得する.
     */
    fun getBookDetail(id: Int): Flow<BookEntity?> {
        return getBookDetailUseCase(id)
    }

    private fun clearBuffer() {
        Log.d(TAG, "[clearBuffer]")
        _scannedBuffer.value = emptyList()
    }

    companion object {
        private val TAG = BookViewModel::class.java.simpleName
    }
}