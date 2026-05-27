package com.voicefx.core.overlay

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface OverlayState {
    data object Hidden : OverlayState
    data class Visible(
        val posX: Int = 0,
        val posY: Int = 200
    ) : OverlayState
    data class QuickPanelOpen(
        val posX: Int = 0,
        val posY: Int = 200
    ) : OverlayState

    companion object {
        private const val PREFS_NAME = "overlay_prefs"
        private const val KEY_POS_X = "pos_x"
        private const val KEY_POS_Y = "pos_y"

        fun load(context: Context): OverlayState {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val x = prefs.getInt(KEY_POS_X, 0)
            val y = prefs.getInt(KEY_POS_Y, 200)
            return Visible(posX = x, posY = y)
        }

        fun save(context: Context, state: OverlayState) {
            val x = when (state) {
                is OverlayState.Visible -> state.posX
                is OverlayState.QuickPanelOpen -> state.posX
                is OverlayState.Hidden -> return
            }
            val y = when (state) {
                is OverlayState.Visible -> state.posY
                is OverlayState.QuickPanelOpen -> state.posY
                is OverlayState.Hidden -> return
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putInt(KEY_POS_X, x)
                putInt(KEY_POS_Y, y)
            }
        }
    }
}

@Singleton
class OverlayStateHolder @Inject constructor(@ApplicationContext private val context: Context) {
    private val _state = MutableStateFlow(OverlayState.load(context))
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    fun update(newState: OverlayState) {
        _state.value = newState
        OverlayState.save(context, newState)
    }

    fun position(): Pair<Int, Int> {
        val s = _state.value
        return when (s) {
            is OverlayState.Visible -> s.posX to s.posY
            is OverlayState.QuickPanelOpen -> s.posX to s.posY
            is OverlayState.Hidden -> 0 to 200
        }
    }
}
