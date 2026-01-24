package com.example.mybooksapplication.ui.viewmodel

import app.cash.turbine.test
import com.example.mybooksapplication.MainDispatcherRule
import com.example.mybooksapplication.MockLogRule
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockLogRule = MockLogRule()

    private lateinit var searchBookByIsbnUseCase: SearchBookByIsbnUseCase
    private lateinit var saveBookUseCase: SaveBookUseCase
    private lateinit var deleteBookUseCase: DeleteBookUseCase
    private lateinit var saveMultipleBooksUseCase: SaveMultipleBooksUseCase
    private lateinit var deleteMultipleBooksUseCase: DeleteMultipleBooksUseCase
    private lateinit var getSavedBooksUseCase: GetSavedBooksUseCase
    private lateinit var getBookDetailUseCase: GetBookDetailUseCase

    private lateinit var viewModel: BookViewModel

    @Before
    fun setup() {
        searchBookByIsbnUseCase = mockk()
        saveBookUseCase = mockk(relaxed = true)
        deleteBookUseCase = mockk(relaxed = true)
        saveMultipleBooksUseCase = mockk()
        deleteMultipleBooksUseCase = mockk(relaxed = true)
        getSavedBooksUseCase = mockk(relaxed = true)
        getBookDetailUseCase = mockk(relaxed = true)

        // default saved books flow
        coEvery { getSavedBooksUseCase() } returns flow { emit(emptyList()) }

        viewModel = BookViewModel(
            getSavedBooksUseCase,
            searchBookByIsbnUseCase,
            saveBookUseCase,
            deleteBookUseCase,
            saveMultipleBooksUseCase,
            deleteMultipleBooksUseCase,
            getBookDetailUseCase
        )
    }

    @Test
    fun setBatchMode_togglesIsBatchMode_andClearsBuffer() = runTest {
        // initially false
        assertFalse(viewModel.isBatchMode.value)

        viewModel.setBatchMode(true)
        // after switching, should be true
        assertTrue(viewModel.isBatchMode.value)
        // buffer should be empty
        assertTrue(viewModel.scannedBuffer.value.isEmpty())

        viewModel.setBatchMode(false)
        assertFalse(viewModel.isBatchMode.value)
    }

    @Test
    fun onIsbnScanned_singleMode_whenFound_emitsSuccess() = runTest {
        val isbn = "123"
        val summary = BookSummary(isbn = isbn, title = "T", author = "A", coverUrl = "C")
        coEvery { searchBookByIsbnUseCase(isbn) } returns summary

        // observe scanState
        viewModel.scanState.test {
            // initial idle
            assertEquals(ScanUiState.Idle, awaitItem())

            viewModel.onIsbnScanned(isbn)
            // Loading then Success
            val loading = awaitItem()
            // loading is instance of ScanUiState.Loading
            assertTrue(loading is ScanUiState.Loading)

            val success = awaitItem()
            assertTrue(success is ScanUiState.Success)
            if (success is ScanUiState.Success) {
                assertEquals(summary, success.book)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onIsbnScanned_singleMode_whenNotFound_emitsError() = runTest {
        val isbn = "not_found"
        coEvery { searchBookByIsbnUseCase(isbn) } returns null

        viewModel.scanState.test {
            assertEquals(ScanUiState.Idle, awaitItem())

            viewModel.onIsbnScanned(isbn)

            val loading = awaitItem()
            assertTrue(loading is ScanUiState.Loading)

            val error = awaitItem()
            assertTrue(error is ScanUiState.Error)
            if (error is ScanUiState.Error) {
                assertEquals(R.string.scan_not_found, error.message)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onIsbnScanned_batchMode_addsToBuffer_andIgnoresDuplicate() = runTest {
        val isbn = "dup"
        val summary = BookSummary(isbn = isbn, title = "Title", author = "Author", coverUrl = "url")
        coEvery { searchBookByIsbnUseCase(isbn) } returns summary

        viewModel.setBatchMode(true)

        // first scan
        viewModel.onIsbnScanned(isbn)
        // advance until coroutine completes
        advanceUntilIdle()
        assertEquals(1, viewModel.scannedBuffer.value.size)
        assertEquals(summary, viewModel.scannedBuffer.value.first())

        // second scan (duplicate) should be ignored
        viewModel.onIsbnScanned(isbn)
        advanceUntilIdle()
        assertEquals(1, viewModel.scannedBuffer.value.size)
    }

    @Test
    fun saveCurrentBook_callsSaveBookUseCase_andResetsState() = runTest {
        val isbn = "save_isbn"
        val summary = BookSummary(isbn = isbn, title = "S", author = "A", coverUrl = "C")
        coEvery { searchBookByIsbnUseCase(isbn) } returns summary

        // trigger single scan to put state into Success
        viewModel.onIsbnScanned(isbn)
        advanceUntilIdle()

        // ensure it's Success
        val state = viewModel.scanState.value
        assertTrue(state is ScanUiState.Success)

        viewModel.saveCurrentBook()
        advanceUntilIdle()

        coVerify(exactly = 1) { saveBookUseCase(summary) }
        assertEquals(ScanUiState.Idle, viewModel.scanState.value)
    }

    @Test
    fun saveBufferedBooks_callsSaveMultipleBooksUseCase_andClearsBuffer() = runTest {
        val isbn1 = "b1"
        val isbn2 = "b2"
        val s1 = BookSummary(isbn = isbn1, title = "T1", author = "A1", coverUrl = "C1")
        val s2 = BookSummary(isbn = isbn2, title = "T2", author = "A2", coverUrl = "C2")
        coEvery { searchBookByIsbnUseCase(isbn1) } returns s1
        coEvery { searchBookByIsbnUseCase(isbn2) } returns s2
        coEvery { saveMultipleBooksUseCase(any()) } returns Unit

        viewModel.setBatchMode(true)
        viewModel.onIsbnScanned(isbn1)
        viewModel.onIsbnScanned(isbn2)
        advanceUntilIdle()

        assertEquals(2, viewModel.scannedBuffer.value.size)

        viewModel.saveBufferedBooks()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            saveMultipleBooksUseCase(match {
                it.size == 2 && it.containsAll(
                    listOf(s1, s2)
                )
            })
        }
        assertTrue(viewModel.scannedBuffer.value.isEmpty())
    }

    @Test
    fun toggleSelection_addsAndRemovesId() = runTest {
        val id = 42
        assertTrue(viewModel.selectedBookIds.value.isEmpty())

        viewModel.toggleSelection(id)
        assertTrue(viewModel.selectedBookIds.value.contains(id))

        viewModel.toggleSelection(id)
        assertFalse(viewModel.selectedBookIds.value.contains(id))
    }

    @Test
    fun deleteSelectedBooks_callsDeleteMultipleBooksUseCase_withFilteredBooks_andClearsSelection() =
        runTest {
            val books = listOf(
                BookEntity(id = 1, isbn = "a", title = "A", author = "a", coverUrl = "c"),
                BookEntity(id = 2, isbn = "b", title = "B", author = "b", coverUrl = "d")
            )

            // select id=2
            viewModel.toggleSelection(2)
            assertTrue(viewModel.selectedBookIds.value.contains(2))

            viewModel.deleteSelectedBooks(books)
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteMultipleBooksUseCase(match { it.size == 1 && it.first().id == 2 }) }
            assertTrue(viewModel.selectedBookIds.value.isEmpty())
        }

    @Test
    fun deleteBook_callsDeleteBookUseCase() = runTest {
        val book = BookEntity(id = 10, isbn = "x", title = "X", author = "X", coverUrl = "u")
        viewModel.deleteBook(book)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteBookUseCase(book) }
    }

    @Test
    fun getBookDetail_delegatesToUseCase_andEmitsExpected() = runTest {
        val book = BookEntity(id = 99, isbn = "i", title = "T", author = "A", coverUrl = "C")
        coEvery { getBookDetailUseCase(99) } returns flow { emit(book) }

        viewModel.getBookDetail(99).test {
            val emitted = awaitItem()
            assertEquals(book, emitted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onIsbnScanned_whenScanStateNotIdle_earlyReturns() = runTest {
        val isbn = "busy"
        val summary = BookSummary(isbn = isbn, title = "T", author = "A", coverUrl = "C")
        // make search suspend so first call sets Loading and blocks
        coEvery { searchBookByIsbnUseCase(isbn) } coAnswers {
            // suspend until test advances
            delay(50)
            summary
        }

        // trigger first scan which will set Loading
        viewModel.onIsbnScanned(isbn)
        // Do NOT advanceUntilIdle yet, call again immediately -> should be early return
        viewModel.onIsbnScanned(isbn)

        // advance to complete suspended search
        advanceUntilIdle()

        // verify searchBookByIsbnUseCase called exactly once
        coVerify(exactly = 1) { searchBookByIsbnUseCase(isbn) }
    }

    @Test
    fun saveCurrentBook_whenNotSuccess_doesNothing() = runTest {
        // ensure scanState is Idle
        viewModel.resetScanState()

        viewModel.saveCurrentBook()
        advanceUntilIdle()

        coVerify(exactly = 0) { saveBookUseCase(any()) }
    }

    @Test
    fun saveBufferedBooks_whenEmpty_doesNothing() = runTest {
        // ensure buffer empty
        viewModel.setBatchMode(true)
        viewModel.saveBufferedBooks()
        advanceUntilIdle()

        coVerify(exactly = 0) { saveMultipleBooksUseCase(any()) }
    }

    @Test
    fun deleteSelectedBooks_whenEmpty_doesNothing() = runTest {
        val books = emptyList<BookEntity>()
        // ensure no selection
        viewModel.clearSelection()

        viewModel.deleteSelectedBooks(books)
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteMultipleBooksUseCase(any()) }
    }

    @Test
    fun resetScanState_setsIdle_fromNonIdle() = runTest {
        // arrange: make search return a summary so scanState becomes Success
        val isbn = "reset123"
        val summary = BookSummary(isbn = isbn, title = "T", author = "A", coverUrl = "C")
        coEvery { searchBookByIsbnUseCase(isbn) } returns summary

        // act: perform scan to set state to Success
        viewModel.onIsbnScanned(isbn)
        advanceUntilIdle()

        // ensure it's non-idle (Success)
        val stateBefore = viewModel.scanState.value
        assertTrue(stateBefore is ScanUiState.Success)

        // act: reset
        viewModel.resetScanState()

        // assert: is Idle
        assertEquals(ScanUiState.Idle, viewModel.scanState.value)
    }

    @Test
    fun clearSelection_clearsSelectedIds() = runTest {
        viewModel.toggleSelection(5)
        assertTrue(viewModel.selectedBookIds.value.contains(5))

        viewModel.clearSelection()
        assertTrue(viewModel.selectedBookIds.value.isEmpty())
    }
}