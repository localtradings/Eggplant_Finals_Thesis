package com.eggplant.detector.data.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "remote_notifications",
    indices = [Index("publishedAt")],
    primaryKeys = ["id"],
)
data class RemoteNotificationEntity(
    val id: String,
    val category: String,
    val titleEn: String,
    val bodyEn: String,
    val titleFil: String,
    val bodyFil: String,
    val publishedAt: String,
    val expiresAt: String?,
)
