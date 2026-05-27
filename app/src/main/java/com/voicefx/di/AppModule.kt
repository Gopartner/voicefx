package com.voicefx.di

import android.content.Context
import androidx.room.Room
import com.voicefx.BuildConfig
import com.voicefx.core.camera.CameraHelper
import com.voicefx.core.location.LocationHelper
import com.voicefx.core.network.GitHubApiService
import com.voicefx.core.overlay.OverlayStateHolder
import com.voicefx.data.local.AppDatabase
import com.voicefx.data.local.dao.JobDao
import com.voicefx.data.local.dao.VoiceNoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voicefx.db"
        ).build()
    }

    @Provides
    fun provideVoiceNoteDao(database: AppDatabase): VoiceNoteDao {
        return database.voiceNoteDao()
    }

    @Provides
    fun provideJobDao(database: AppDatabase): JobDao {
        return database.jobDao()
    }

    @Provides
    @Singleton
    fun provideGitHubApiService(): GitHubApiService {
        return GitHubApiService().apply {
            val token = BuildConfig.GITHUB_TOKEN
            val owner = BuildConfig.GITHUB_OWNER
            val repo = BuildConfig.GITHUB_REPO
            if (token.isNotBlank() && owner.isNotBlank()) {
                configure(token, owner, repo)
            }
        }
    }

    @Provides
    @Singleton
    fun provideCameraHelper(@ApplicationContext context: Context): CameraHelper {
        return CameraHelper(context)
    }

    @Provides
    @Singleton
    fun provideLocationHelper(@ApplicationContext context: Context): LocationHelper {
        return LocationHelper(context)
    }
}
