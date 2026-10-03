package io.github.syntaxghost404.tappilot.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.hypot

/**
 * Hosts an overlay window's content and turns drags into window movement. Raw screen coordinates
 * are used because the window itself moves under the finger, which makes view-local coordinates
 * useless for this.
 *
 * With [interceptOnlyDrags] children still receive taps (the control bar's buttons); otherwise the
 * frame consumes every touch and reports taps through [Listener.onTap] (target markers).
 */
@SuppressLint("ViewConstructor")
internal class DragFrame(
    context: Context,
    private val interceptOnlyDrags: Boolean,
    private val listener: Listener,
) : FrameLayout(context) {

    interface Listener {
        fun onPressChanged(pressed: Boolean) {}
        fun onDragStart() {}
        fun onDrag(totalDx: Float, totalDy: Float)
        fun onDragEnd() {}
        fun onTap() {}
        fun onBack(): Boolean = false
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!interceptOnlyDrags) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> if (!dragging && pastSlop(event)) {
                startDrag()
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                listener.onPressChanged(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && pastSlop(event)) startDrag()
                if (dragging) listener.onDrag(event.rawX - downX, event.rawY - downY)
            }
            MotionEvent.ACTION_UP -> {
                listener.onPressChanged(false)
                if (dragging) {
                    listener.onDragEnd()
                } else if (!interceptOnlyDrags) {
                    performClick()
                    listener.onTap()
                }
                dragging = false
            }
            MotionEvent.ACTION_CANCEL -> {
                listener.onPressChanged(false)
                if (dragging) listener.onDragEnd()
                dragging = false
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && listener.onBack()) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun startDrag() {
        dragging = true
        listener.onPressChanged(true)
        listener.onDragStart()
    }

    private fun pastSlop(event: MotionEvent) = hypot(event.rawX - downX, event.rawY - downY) > touchSlop
}

/** Root of a focusable overlay window that closes itself on the back key. */
@SuppressLint("ViewConstructor")
internal class BackAwareFrame(context: Context, private val onBack: () -> Unit) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) onBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
