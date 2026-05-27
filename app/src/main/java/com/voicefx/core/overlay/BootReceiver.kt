package com.voicefx.core.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val state = OverlayState.load(context)
            if (state !is OverlayState.Hidden) {
                VoiceFxOverlayService.start(context)
            }
        }
    }
}
