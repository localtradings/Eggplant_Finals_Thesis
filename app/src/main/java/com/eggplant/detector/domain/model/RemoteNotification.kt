package com.eggplant.detector.domain.model

data class RemoteNotification(
    val id: String,
    val category: String,
    val titleEn: String,
    val bodyEn: String,
    val titleFil: String,
    val bodyFil: String,
    val publishedAt: String,
) {
    val key: String
        get() = "remote:$id"

    fun title(languageTag: String): String = if (languageTag in setOf("fil", "tl")) titleFil else titleEn

    fun body(languageTag: String): String = if (languageTag in setOf("fil", "tl")) bodyFil else bodyEn
}
