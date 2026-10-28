package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    fun getRecordingById(id: Long): Flow<RecordingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("UPDATE recordings SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecording(id: Long)

    @Query("DELETE FROM recordings")
    suspend fun deleteAllRecordings()
}
