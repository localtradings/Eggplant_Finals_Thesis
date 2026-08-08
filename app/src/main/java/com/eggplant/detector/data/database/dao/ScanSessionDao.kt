package com.eggplant.detector.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.eggplant.detector.data.database.entity.ScanDetectionEntity
import com.eggplant.detector.data.database.entity.ScanSessionEntity
import com.eggplant.detector.data.database.entity.ScanSessionWithDetections
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    @Transaction
    @Query("SELECT * FROM scan_sessions ORDER BY savedAt DESC")
    fun observeSessions(): Flow<List<ScanSessionWithDetections>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: ScanSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDetections(detections: List<ScanDetectionEntity>)

    @Query("SELECT * FROM scan_sessions WHERE id = :id LIMIT 1")
    suspend fun sessionById(id: String): ScanSessionEntity?

    @Query("SELECT * FROM scan_sessions WHERE savedAt < :cutoff AND isFavorite = 0")
    suspend fun expiredSessions(cutoff: String): List<ScanSessionEntity>

    @Query("UPDATE scan_sessions SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM scan_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Transaction
    suspend fun insertSessionWithDetections(
        session: ScanSessionEntity,
        detections: List<ScanDetectionEntity>,
    ) {
        require(detections.all { it.sessionId == session.id }) { "Every detection must belong to the session." }
        insertSession(session)
        if (detections.isNotEmpty()) insertDetections(detections)
    }
}
