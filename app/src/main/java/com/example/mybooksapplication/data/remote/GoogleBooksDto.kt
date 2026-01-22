package com.example.mybooksapplication.data.remote

import com.squareup.moshi.Json

data class BookSummary(
    val isbn: String = "",
    val title: String = "",
    val author: String = "",
    @param:Json(name = "cover") val coverUrl: String = ""
)

// Google Books APIのルートレスポンス
data class GoogleBooksResponse(
    val items: List<GoogleBookItem>? // 該当なしの場合はnullまたは空リスト
)

// 各書籍データ
data class GoogleBookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

// 書籍の詳細情報
data class VolumeInfo(
    val title: String?,
    val authors: List<String>?, // 著者はリストで返ってきます
    val description: String?,
    val imageLinks: ImageLinks? // 画像がない場合もあるのでNullable
)

// 画像リンク
data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?
)