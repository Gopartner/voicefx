package com.voicefx.core.overlay

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.voicefx.MainActivity
import com.voicefx.ui.theme.VoiceFXTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

@AndroidEntryPoint
class VoiceFxOverlayService : Service() {

    @Inject lateinit var overlayStateHolder: OverlayStateHolder

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val waPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        serviceScope.launch {
            overlayStateHolder.state.collect { state ->
                when (state) {
                    is OverlayState.Hidden -> removeOverlay()
                    else -> showOverlay()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "voicefx_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "VoiceFX Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VoiceFX overlay service"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VoiceFX")
            .setContentText("Overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun isWhatsAppForeground(): Boolean {
        val isUsageAllowed = try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
        if (!isUsageAllowed) return false
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 2000,
                currentTime
            )
            if (stats.isNotEmpty()) {
                val recent = stats.maxByOrNull { it.lastTimeUsed } ?: return false
                val elapsed = currentTime - recent.lastTimeUsed
                return recent.packageName in waPackages && elapsed < 3000
            }
        } catch (_: Exception) {}
        return false
    }

    private fun showOverlay() {
        removeOverlay()

        val composeView = ComposeView(this).apply {
            setContent {
                VoiceFXTheme {
                    val currentState by overlayStateHolder.state.collectAsState()
                    val (px, py) = overlayStateHolder.position()
                    when (currentState) {
                        is OverlayState.QuickPanelOpen -> QuickPanelContent(
                            onDismiss = { overlayStateHolder.update(OverlayState.Visible(posX = px, posY = py)) },
                            onItemSelected = { item -> onQuickPanelItem(item) }
                        )
                        is OverlayState.Visible -> BubbleContent(
                            onClick = {
                                val sPos = overlayStateHolder.position()
                                overlayStateHolder.update(
                                    OverlayState.QuickPanelOpen(posX = sPos.first, posY = sPos.second)
                                )
                            },
                            onLongClick = { overlayStateHolder.update(OverlayState.Hidden) }
                        )
                        is OverlayState.Hidden -> { }
                    }
                }
            }
        }

        val (posX, posY) = overlayStateHolder.position()
        params = WindowManager.LayoutParams(
            if (overlayStateHolder.state.value is OverlayState.QuickPanelOpen)
                WindowManager.LayoutParams.MATCH_PARENT
            else WindowManager.LayoutParams.WRAP_CONTENT,
            if (overlayStateHolder.state.value is OverlayState.QuickPanelOpen)
                WindowManager.LayoutParams.MATCH_PARENT
            else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = posX
            y = posY
        }

        if (overlayStateHolder.state.value !is OverlayState.QuickPanelOpen) {
            composeView.setOnTouchListener { _, event ->
                val s = overlayStateHolder.state.value
                if (s is OverlayState.Visible) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            composeView.downRawX = event.rawX.toInt()
                            composeView.downRawY = event.rawY.toInt()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params?.let { p ->
                                p.x = (p.x + event.rawX.toInt() - composeView.downRawX).coerceAtLeast(0)
                                p.y = (p.y + event.rawY.toInt() - composeView.downRawY).coerceAtLeast(0)
                                windowManager.updateViewLayout(composeView, p)
                                overlayStateHolder.update(
                                    OverlayState.Visible(posX = p.x, posY = p.y)
                                )
                            }
                        }
                    }
                }
                false
            }
        }

        overlayView = composeView
        try {
            windowManager.addView(composeView, params)
        } catch (_: Exception) {}
    }

    private fun removeOverlay() {
        if (::overlayView.isInitialized && overlayView.isAttachedToWindow) {
            try { windowManager.removeView(overlayView) } catch (_: Exception) {}
        }
    }

    private fun onQuickPanelItem(item: QuickPanelItem) {
        val pos = overlayStateHolder.position()
        overlayStateHolder.update(OverlayState.Visible(posX = pos.first, posY = pos.second))
        when (item) {
            QuickPanelItem.CAMERA -> openScreen("camera_tab")
            QuickPanelItem.LOCATION -> openScreen("location_tab")
            QuickPanelItem.SETTINGS -> openScreen("settings")
            QuickPanelItem.WHATSAPP_NOTES -> openScreen("history")
            QuickPanelItem.INTERNAL_STORAGE -> openScreen("picker/internal")
            QuickPanelItem.SD_CARD -> openScreen("picker/sd_card")
            QuickPanelItem.ORIGINAL -> openScreen("recorder/ORIGINAL")
            QuickPanelItem.CHILD -> openScreen("recorder/CHILD")
            QuickPanelItem.TEEN -> openScreen("recorder/TEEN")
            QuickPanelItem.ADULT_FEMALE -> openScreen("recorder/ADULT_FEMALE")
        }
    }

    private fun openScreen(route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigation_route", route)
        }
        startActivity(intent)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, VoiceFxOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceFxOverlayService::class.java))
        }
    }
}

private var View.downRawX: Int
    get() = getTag(103) as? Int ?: 0
    set(value) = setTag(103, value)

private var View.downRawY: Int
    get() = getTag(104) as? Int ?: 0
    set(value) = setTag(104, value)

enum class QuickPanelItem(
    val label: String,
    val icon: ImageVector
) {
    ORIGINAL("Original", Icons.Default.Mic),
    CHILD("Child", Icons.Default.Person),
    TEEN("Teen", Icons.Default.Face),
    ADULT_FEMALE("Adult Female", Icons.Default.Favorite),
    INTERNAL_STORAGE("Internal Storage", Icons.Default.Folder),
    SD_CARD("SD Card", Icons.Default.SdCard),
    WHATSAPP_NOTES("WhatsApp Voice Notes", Icons.AutoMirrored.Filled.Chat),
    LOCATION("Location", Icons.Default.LocationOn),
    CAMERA("Camera", Icons.Default.CameraAlt),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
private fun BubbleContent(onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Equalizer,
            contentDescription = "VoiceFX",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPanelContent(
    onDismiss: () -> Unit,
    onItemSelected: (QuickPanelItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickPanelItem.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            QuickPanelButton(
                                item = item,
                                onClick = { onItemSelected(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPanelButton(
    item: QuickPanelItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(item.label, fontSize = 13.sp)
    }
}
