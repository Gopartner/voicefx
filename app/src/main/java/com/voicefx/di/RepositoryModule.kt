package com.voicefx.di

import com.voicefx.data.repository.VoiceRepositoryImpl
import com.voicefx.data.repository.WhatsAppRepositoryImpl
import com.voicefx.domain.repository.VoiceRepository
import com.voicefx.domain.repository.WhatsAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(impl: VoiceRepositoryImpl): VoiceRepository

    @Binds
    @Singleton
    abstract fun bindWhatsAppRepository(impl: WhatsAppRepositoryImpl): WhatsAppRepository
}
