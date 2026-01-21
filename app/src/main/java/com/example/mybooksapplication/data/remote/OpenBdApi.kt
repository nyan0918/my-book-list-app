package com.example.mybooksapplication.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenBdResponse(val summary: BookSummary?)
data class BookSummary(
    val isbn: String = "",
    val title: String = "",
    val author: String = "",
    @param:Json(name = "cover") val coverUrl: String = ""
)

interface OpenBdService {
    @GET("v1/get")
    suspend fun getBook(@Query("isbn") isbn: String): List<OpenBdResponse?>
}

object ApiClient {
    const val URL = "https://api.openbd.jp/" // OpenBDのベースURL

    // 1. Moshiのビルダーを作成
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // Kotlin対応のアダプターを追加
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(URL)
        // 2. 作成したmoshiインスタンスをFactoryに渡す
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: OpenBdService = retrofit.create(OpenBdService::class.java)
}
