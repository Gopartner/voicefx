package com.voicefx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_jobs")
data class JobEntity(
    @PrimaryKey val sessionId: String,
    val releaseId: Long,
    val preset: String,
    val status: String,
    val inputUri: String,
    val resultUri: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)
