package com.voicefx

import android.app.Application
import com.voicefx.core.overlay.VoiceFxOverlayService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VoiceFXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VoiceFxOverlayService.start(this)
    }
}
