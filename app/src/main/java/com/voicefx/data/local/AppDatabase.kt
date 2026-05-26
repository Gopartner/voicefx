package com.voicefx.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voicefx.data.local.dao.JobDao
import com.voicefx.data.local.dao.VoiceNoteDao
import com.voicefx.data.local.entity.JobEntity
import com.voicefx.data.local.entity.VoiceNoteEntity

@Database(
    entities = [VoiceNoteEntity::class, JobEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun jobDao(): JobDao
}
