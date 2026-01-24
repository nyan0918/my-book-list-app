package com.example.mybooksapplication.data

import com.example.mybooksapplication.MainDispatcherRule
import com.example.mybooksapplication.MockLogRule
import com.example.mybooksapplication.data.local.BookDao
import com.example.mybooksapplication.data.local.BookEntity
import com.example.mybooksapplication.data.remote.GoogleBookItem
import com.example.mybooksapplication.data.remote.GoogleBooksResponse
import com.example.mybooksapplication.data.remote.GoogleBooksService
import com.example.mybooksapplication.data.remote.ImageLinks
import com.example.mybooksapplication.data.remote.VolumeInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockLogRule = MockLogRule()

    private lateinit var bookDao: BookDao
    private lateinit var service: GoogleBooksService
    private lateinit var repository: BookRepository

    @Before
    fun setup() {
        bookDao = mockk(relaxed = true)
        service = mockk()
    }

    @Test
    fun allBooks_returnsFlowFromDao() = runTest {
        val books =
            listOf(BookEntity(id = 1, isbn = "1", title = "T", author = "A", coverUrl = "U"))
        every { bookDao.getAllBooks() } returns flowOf(books)

        repository = BookRepository(bookDao, service)

        val emitted = repository.allBooks
            .let { flow ->
                // collect first value synchronously via toList isn't available; use single emission check
                var value: List<BookEntity>? = null
                flow.collect { value = it }
                value
            }
        assertEquals(books, emitted)
    }

    @Test
    fun fetchBookInfo_whenServiceReturnsItem_returnsBookSummary_withSecuredUrl_andAuthorsJoined() =
        runTest {
            val isbn = "123"
            val volumeInfo = VolumeInfo(
                title = "Title",
                authors = listOf("A", "B"),
                description = "desc",
                imageLinks = ImageLinks(
                    smallThumbnail = null,
                    thumbnail = "http://example.com/img.jpg"
                )
            )
            val item = GoogleBookItem(id = "id1", volumeInfo = volumeInfo)
            val response = GoogleBooksResponse(items = listOf(item))
            coEvery { service.searchBook("isbn:$isbn", any()) } returns response

            repository = BookRepository(bookDao, service)

            val result = repository.fetchBookInfo(isbn)

            assertEquals(isbn, result?.isbn)
            assertEquals("Title", result?.title)
            assertEquals("A, B", result?.author)
            assertEquals("https://example.com/img.jpg", result?.coverUrl)
        }

    @Test
    fun fetchBookInfo_whenServiceReturnsItem_withNoThumbnail_usesSmallThumbnail_orEmpty() =
        runTest {
            val isbn = "234"
            val volumeInfo = VolumeInfo(
                title = "T2",
                authors = null,
                description = null,
                imageLinks = ImageLinks(
                    smallThumbnail = "http://ex.com/small.jpg",
                    thumbnail = null
                )
            )
            val item = GoogleBookItem(id = "id2", volumeInfo = volumeInfo)
            val response = GoogleBooksResponse(items = listOf(item))
            coEvery { service.searchBook("isbn:$isbn", any()) } returns response

            repository = BookRepository(bookDao, service)

            val result = repository.fetchBookInfo(isbn)

            assertEquals("T2", result?.title)
            assertEquals("著者不明", result?.author)
            assertEquals("https://ex.com/small.jpg", result?.coverUrl)
        }

    @Test
    fun fetchBookInfo_whenServiceReturnsNoItems_returnsNull() = runTest {
        val isbn = "noitem"
        val response = GoogleBooksResponse(items = null)
        coEvery { service.searchBook("isbn:$isbn", any()) } returns response

        repository = BookRepository(bookDao, service)

        val result = repository.fetchBookInfo(isbn)
        assertNull(result)
    }

    @Test
    fun fetchBookInfo_whenServiceThrows_returnsNull() = runTest {
        val isbn = "err"
        coEvery { service.searchBook("isbn:$isbn", any()) } throws RuntimeException("fail")

        repository = BookRepository(bookDao, service)

        val result = repository.fetchBookInfo(isbn)
        assertNull(result)
    }

    @Test
    fun getBookById_delegatesToDao() = runTest {
        val id = 5
        val bookFlow =
            flowOf(BookEntity(id = id, isbn = "i", title = "t", author = "a", coverUrl = "u"))
        every { bookDao.getBookById(id) } returns bookFlow

        repository = BookRepository(bookDao, service)

        val returned = repository.getBookById(id)
        // collect one value
        var value: BookEntity? = null
        returned.collect { value = it }
        assertEquals(id, value?.id)
    }

    @Test
    fun saveBook_delegatesToDao() = runTest {
        val book = BookEntity(id = 0, isbn = "s", title = "t", author = "a", coverUrl = "u")
        coEvery { bookDao.insertBook(book) } returns Unit

        repository = BookRepository(bookDao, service)

        repository.saveBook(book)

        coVerify(exactly = 1) { bookDao.insertBook(book) }
    }

    @Test
    fun saveBooks_delegatesToDao() = runTest {
        val books =
            listOf(BookEntity(id = 1, isbn = "1", title = "t", author = "a", coverUrl = "u"))
        coEvery { bookDao.insertAll(books) } returns Unit

        repository = BookRepository(bookDao, service)

        repository.saveBooks(books)

        coVerify(exactly = 1) { bookDao.insertAll(books) }
    }

    @Test
    fun deleteBook_delegatesToDao() = runTest {
        val book = BookEntity(id = 2, isbn = "2", title = "t", author = "a", coverUrl = "u")
        coEvery { bookDao.deleteBook(book) } returns Unit

        repository = BookRepository(bookDao, service)

        repository.deleteBook(book)

        coVerify(exactly = 1) { bookDao.deleteBook(book) }
    }

    @Test
    fun deleteBooks_delegatesToDao() = runTest {
        val books =
            listOf(BookEntity(id = 3, isbn = "3", title = "t", author = "a", coverUrl = "u"))
        coEvery { bookDao.deleteBooks(books) } returns Unit

        repository = BookRepository(bookDao, service)

        repository.deleteBooks(books)

        coVerify(exactly = 1) { bookDao.deleteBooks(books) }
    }
}
