package com.example.mybooksapplication.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybooksapplication.R
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.BookSummary
import com.example.mybooksapplication.domain.usecase.DeleteBookUseCase
import com.example.mybooksapplication.domain.usecase.GetSavedBooksUseCase
import com.example.mybooksapplication.domain.usecase.SaveBookUseCase
import com.example.mybooksapplication.domain.usecase.SearchBookByIsbnUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI状態
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
    private val deleteBookUseCase: DeleteBookUseCase
) : ViewModel() {

    // 保存済みリスト（FlowをStateFlowに変換）
    val savedBooks: StateFlow<List<BookEntity>> = getSavedBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // スキャン結果の状態管理
    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    fun onIsbnScanned(isbn: String) {
        // Loading中、または既に結果(Success/Error)が出ている場合は、
        // 新しいスキャンを完全に無視してリターンする
        if (_scanState.value !is ScanUiState.Idle) {
            return
        }

        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading // ここでLoadingになるので、以降のスキャンは↑で弾かれる

            try {
                val result = searchBookByIsbnUseCase(isbn)
                if (result != null) {
                    _scanState.value = ScanUiState.Success(result)
                } else {
                    _scanState.value = ScanUiState.Error(R.string.scan_not_found)
                }
            } catch (e: Exception) {
                // エラー時もステータスを更新して、ユーザーが閉じるまで次のスキャンを止める
                _scanState.value = ScanUiState.Error(R.string.scan_communication_error)//TODO
            }
        }
    }

    fun saveCurrentBook() {
        val state = _scanState.value
        if (state is ScanUiState.Success) {
            viewModelScope.launch {
                saveBookUseCase(state.book)
                resetScanState()
            }
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch { deleteBookUseCase(book) }
    }

    fun resetScanState() {
        _scanState.value = ScanUiState.Idle
    }
}