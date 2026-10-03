package io.github.syntaxghost404.tappilot.overlay

import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/** One accessibility-overlay window. All calls must happen on the main thread. */
internal class OverlayWindow(
    private val windowManager: WindowManager,
    val view: View,
    val params: LayoutParams,
) {
    var isShown = false
        private set

    fun show(): Boolean {
        if (isShown) return true
        return try {
            windowManager.addView(view, params)
            isShown = true
            true
        } catch (e: WindowManager.BadTokenException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    fun commit() {
        if (isShown) runCatching { windowManager.updateViewLayout(view, params) }
    }

    fun moveTo(x: Int, y: Int) {
        if (params.x == x && params.y == y) return
        params.x = x
        params.y = y
        commit()
    }

    fun resize(width: Int, height: Int) {
        if (params.width == width && params.height == height) return
        params.width = width
        params.height = height
        commit()
    }

    fun setTouchable(touchable: Boolean) = setFlag(LayoutParams.FLAG_NOT_TOUCHABLE, !touchable)

    fun setKeepScreenOn(keepOn: Boolean) = setFlag(LayoutParams.FLAG_KEEP_SCREEN_ON, keepOn)

    private fun setFlag(flag: Int, enabled: Boolean) {
        val updated = if (enabled) params.flags or flag else params.flags and flag.inv()
        if (updated == params.flags) return
        params.flags = updated
        commit()
    }

    fun remove() {
        if (!isShown) return
        isShown = false
        runCatching { windowManager.removeViewImmediate(view) }
    }

    companion object {
        /**
         * Layout params for a window positioned in absolute display pixels, the same coordinate
         * space gestures use: no inset fitting, allowed into the cutout, no clamping to the screen.
         */
        fun params(
            width: Int,
            height: Int,
            touchable: Boolean = true,
            focusable: Boolean = false,
            noLimits: Boolean = true,
        ): LayoutParams {
            var flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_HARDWARE_ACCELERATED
            if (noLimits) flags = flags or LayoutParams.FLAG_LAYOUT_NO_LIMITS
            if (!focusable) flags = flags or LayoutParams.FLAG_NOT_FOCUSABLE
            if (!touchable) flags = flags or LayoutParams.FLAG_NOT_TOUCHABLE
            return LayoutParams(width, height, LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT)
                .apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        fitInsetsTypes = 0
                        fitInsetsSides = 0
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                    // Overlay windows have no decor view to handle IME insets, so resizing is used.
                    @Suppress("DEPRECATION")
                    if (focusable) softInputMode = LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                }
        }
    }
}
