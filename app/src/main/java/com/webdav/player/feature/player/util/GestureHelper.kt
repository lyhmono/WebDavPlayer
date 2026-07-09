package com.webdav.player.feature.player.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.MotionEvent
import android.view.WindowManager
import kotlin.math.abs

/**
 * 手势识别辅助类
 *
 * 支持：
 * - 左右滑动 seek
 * - 上下滑动左侧调节亮度
 * - 上下滑动右侧调节音量
 */
class GestureHelper(
    private val context: Context,
    private val onSeekPreview: (deltaMs: Long) -> Unit,
    private val onSeekConfirm: (deltaMs: Long) -> Unit,
    private val onBrightnessChange: (brightness: Float) -> Unit,
    private val onVolumeChange: (volume: Int, maxVolume: Int) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private var initialX = 0f
    private var initialY = 0f
    private var isSeeking = false
    private var initialBrightness = 0.5f
    private var initialVolume = 0

    /**
     * 处理触摸事件
     */
    fun onTouchEvent(event: MotionEvent, viewWidth: Int): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                isSeeking = false
                initialBrightness = getCurrentBrightness()
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initialX
                val dy = event.y - initialY

                if (abs(dx) > abs(dy)) {
                    // 水平滑动 - seek
                    if (abs(dx) > SEEK_THRESHOLD) {
                        isSeeking = true
                        val deltaMs = (dx * SEEK_SENSITIVITY).toLong()
                        onSeekPreview(deltaMs)
                    }
                } else {
                    // 垂直滑动
                    if (abs(dy) > VOLUME_THRESHOLD) {
                        val isLeftSide = initialX < viewWidth / 2
                        if (isLeftSide) {
                            // 左侧 - 亮度
                            val delta = -dy / 500f
                            val newBrightness = (initialBrightness + delta).coerceIn(0f, 1f)
                            onBrightnessChange(newBrightness)
                        } else {
                            // 右侧 - 音量
                            val delta = (-dy / 200f).toInt()
                            val newVolume = (initialVolume + delta).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                            onVolumeChange(newVolume, maxVolume)
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isSeeking) {
                    val dx = event.x - initialX
                    val deltaMs = (dx * SEEK_SENSITIVITY).toLong()
                    onSeekConfirm(deltaMs)
                }
                isSeeking = false
                return true
            }
        }
        return false
    }

    /**
     * 获取当前亮度
     */
    private fun getCurrentBrightness(): Float {
        return try {
            val activity = context as? Activity
            val attrs = activity?.window?.attributes
            if (attrs?.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                attrs?.screenBrightness ?: 0.5f
            } else {
                android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    128
                ) / 255f
            }
        } catch (e: Exception) {
            0.5f
        }
    }

    companion object {
        private const val SEEK_THRESHOLD = 50f
        private const val VOLUME_THRESHOLD = 30f
        private const val SEEK_SENSITIVITY = 300f

        /**
         * 设置窗口亮度
         */
        fun setWindowBrightness(context: Context, brightness: Float) {
            val activity = context as? Activity ?: return
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness.coerceIn(0f, 1f)
            window.attributes = layoutParams
        }

        /**
         * 重置窗口亮度为系统默认
         */
        fun resetWindowBrightness(context: Context) {
            val activity = context as? Activity ?: return
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }
}
