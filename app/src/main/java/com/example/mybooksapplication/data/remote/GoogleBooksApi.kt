package com.example.mybooksapplication.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksService {
    @GET("books/v1/volumes")
    suspend fun searchBook(@Query("q") query: String): GoogleBooksResponse
}