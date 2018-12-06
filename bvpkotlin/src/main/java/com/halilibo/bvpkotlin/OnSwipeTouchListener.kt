package com.halilibo.bvpkotlin

import android.os.Handler
import android.view.MotionEvent
import android.view.View

/**
 * @author: Halil Ozercan
 */
internal abstract class OnSwipeTouchListener(
        private val doubleTapEnabled: Boolean
) : View.OnTouchListener {

    private val mHandler: Handler = Handler()
    private val futureClickRunnable = Runnable { onClick() }

    // 0: uninitialized
    // 1: horizontal
    // 2: vertical
    private var initialGesture: Int = 0

    protected var initialX: Float = 0.toFloat()
    protected var initialY: Float = 0.toFloat()
    private var decidedX: Float = 0.toFloat()
    private var decidedY: Float = 0.toFloat()
    private var lastClick: Long = 0

    internal enum class Direction {
        LEFT, RIGHT, UP, DOWN
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.actionMasked
        when (action) {

            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                initialGesture = 0
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX: Float
                val deltaY: Float
                if (initialGesture == 0) {
                    deltaX = event.x - initialX
                    deltaY = event.y - initialY
                } else {
                    deltaX = event.x - decidedX
                    deltaY = event.y - decidedY
                }

                if (initialGesture == 0 && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                    initialGesture = 1
                    decidedX = event.x
                    decidedY = event.y
                    if (deltaX > 0) {
                        onBeforeMove(Direction.RIGHT)
                    } else {
                        onBeforeMove(Direction.LEFT)
                    }
                } else if (initialGesture == 0 && Math.abs(deltaY) > SWIPE_THRESHOLD) {
                    initialGesture = 2
                    decidedX = event.x
                    decidedY = event.y
                    if (deltaY > 0) {
                        onBeforeMove(Direction.DOWN)
                    } else {
                        onBeforeMove(Direction.UP)
                    }
                }

                if (initialGesture == 1) {
                    if (deltaX > 0) {
                        onMove(Direction.RIGHT, deltaX)
                    } else {
                        onMove(Direction.LEFT, -deltaX)
                    }
                } else if (initialGesture == 2) {
                    if (deltaY > 0) {
                        onMove(Direction.DOWN, deltaY)
                    } else {
                        onMove(Direction.UP, -deltaY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (initialGesture == 0) { // Finger did not move enough to trigger a swipe
                    return if (doubleTapEnabled &&
                            System.currentTimeMillis() - lastClick <= DOUBLE_TAP_THRESHOLD &&
                            lastClick != 0L) {
                        mHandler.removeCallbacks(futureClickRunnable)
                        onDoubleTap(event)
                        true
                    } else {
                        lastClick = System.currentTimeMillis()
                        if (doubleTapEnabled)
                            mHandler.postDelayed(futureClickRunnable, DOUBLE_TAP_THRESHOLD)
                        else
                            mHandler.post(futureClickRunnable)
                        true
                    }
                }

                onAfterMove()
                initialGesture = 0
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
            }

            MotionEvent.ACTION_OUTSIDE -> {
            }
        }

        return true
    }

    abstract fun onMove(dir: Direction, diff: Float)

    abstract fun onClick()

    abstract fun onDoubleTap(event: MotionEvent)

    abstract fun onAfterMove()

    abstract fun onBeforeMove(dir: Direction)

    companion object {

        private const val SWIPE_THRESHOLD = 100
        private const val DOUBLE_TAP_THRESHOLD: Long = 150
    }
}