package com.example.data.repository

import com.example.data.local.RecordingDao
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(private val recordingDao: RecordingDao) {

    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    fun getRecording(id: Long): Flow<RecordingEntity?> = recordingDao.getRecordingById(id)

    suspend fun saveRecording(recording: RecordingEntity): Long = withContext(Dispatchers.IO) {
        recordingDao.insertRecording(recording)
    }

    suspend fun updateTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        recordingDao.updateTitle(id, newTitle)
    }

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        // Delete video file
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}

        // Delete thumbnail if any
        recording.thumbnailPath?.let {
            try {
                val thumbFile = File(it)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            } catch (_: Exception) {}
        }

        recordingDao.deleteRecording(recording.id)
    }

    suspend fun deleteAll(recordings: List<RecordingEntity>) = withContext(Dispatchers.IO) {
        for (rec in recordings) {
            try {
                val f = File(rec.filePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
            rec.thumbnailPath?.let {
                try {
                    val tf = File(it)
                    if (tf.exists()) tf.delete()
                } catch (_: Exception) {}
            }
        }
        recordingDao.deleteAllRecordings()
    }
}
