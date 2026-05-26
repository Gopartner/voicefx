package com.voicefx.core.model

enum class VoicePreset(
    val displayName: String,
    val pitchFactor: Float,
    val isDefault: Boolean = false
) {
    ORIGINAL("Original", 1.0f, isDefault = true),
    CHILD("Child", 1.6f),
    TEEN("Teen", 1.3f),
    ADULT_FEMALE("Adult Female", 1.12f);

    companion object {
        fun fromDisplayName(name: String): VoicePreset {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: ORIGINAL
        }

        fun default(): VoicePreset = ORIGINAL
    }
}
