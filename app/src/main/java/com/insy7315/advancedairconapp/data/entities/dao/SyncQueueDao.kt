package com.insy7315.advancedairconapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.insy7315.advancedairconapp.data.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observeCount(): Flow<Int>

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markAttempt(id: Int, error: String?)

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}