package com.eggplant.detector.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.eggplant.detector.data.database.entity.NotificationStateEntity
import com.eggplant.detector.data.database.entity.RemoteNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_state")
    fun observeAll(): Flow<List<NotificationStateEntity>>

    @Query("SELECT * FROM remote_notifications ORDER BY publishedAt DESC")
    fun observeRemoteNotifications(): Flow<List<RemoteNotificationEntity>>

    @Upsert
    suspend fun upsert(state: NotificationStateEntity)

    @Upsert
    suspend fun upsertRemoteNotifications(rows: List<RemoteNotificationEntity>)

    @Query("DELETE FROM remote_notifications")
    suspend fun clearRemoteNotifications()
}
