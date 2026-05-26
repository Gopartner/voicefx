package com.voicefx.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicefx.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM conversion_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM conversion_jobs WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getJobById(sessionId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity)

    @Query("UPDATE conversion_jobs SET status = :status, resultUri = :resultUri, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateJob(sessionId: String, status: String, resultUri: String?, errorMessage: String?, updatedAt: Long)

    @Query("DELETE FROM conversion_jobs WHERE sessionId = :sessionId")
    suspend fun deleteById(sessionId: String)
}
