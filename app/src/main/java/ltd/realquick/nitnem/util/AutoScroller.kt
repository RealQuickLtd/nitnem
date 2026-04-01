package ltd.realquick.nitnem.util

import android.view.Choreographer
import androidx.core.widget.NestedScrollView

class AutoScroller(private val scrollView: NestedScrollView) {

    private val choreographer by lazy(LazyThreadSafetyMode.NONE) {
        Choreographer.getInstance()
    }

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        step(frameTimeNanos)
    }

    private var lastFrameNanos = 0L
    private var pendingScrollPx = 0f

    var speed: Int = DEFAULT_SPEED
        private set

    var isScrolling: Boolean = false
        private set

    var onStateChanged: ((Boolean) -> Unit)? = null
    var scrollUnitPxProvider: (() -> Float)? = null

    fun start() {
        if (isScrolling) return

        val child = scrollView.getChildAt(0) ?: return
        val maxY = (child.height - scrollView.height).coerceAtLeast(0)
        if (scrollView.scrollY >= maxY) return

        isScrolling = true
        lastFrameNanos = 0L
        pendingScrollPx = 0f
        onStateChanged?.invoke(true)
        choreographer.postFrameCallback(frameCallback)
    }

    fun pause() {
        if (!isScrolling) return
        stopInternal()
    }

    fun toggle(): Boolean {
        if (isScrolling) pause() else start()
        return isScrolling
    }

    fun stop() {
        if (!isScrolling) return
        stopInternal()
    }

    fun updateSpeed(newSpeed: Int) {
        speed = newSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    private fun step(frameTimeNanos: Long) {
        if (!isScrolling) return

        val child = scrollView.getChildAt(0) ?: run {
            stopInternal()
            return
        }
        val maxY = (child.height - scrollView.height).coerceAtLeast(0)
        if (maxY <= 0 || scrollView.scrollY >= maxY) {
            stopInternal()
            return
        }

        if (lastFrameNanos != 0L) {
            val deltaSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            if (deltaSeconds > 0f) {
                val unitPx = (scrollUnitPxProvider?.invoke() ?: 1f).coerceAtLeast(1f)
                pendingScrollPx += unitPx * (speed / 100f) * deltaSeconds

                val deltaY = pendingScrollPx.toInt()
                if (deltaY > 0) {
                    pendingScrollPx -= deltaY
                    val nextY = (scrollView.scrollY + deltaY).coerceAtMost(maxY)
                    scrollView.scrollTo(0, nextY)
                }
            }
        }

        lastFrameNanos = frameTimeNanos
        if (isScrolling) {
            choreographer.postFrameCallback(frameCallback)
        }
    }

    private fun stopInternal() {
        isScrolling = false
        lastFrameNanos = 0L
        pendingScrollPx = 0f
        choreographer.removeFrameCallback(frameCallback)
        onStateChanged?.invoke(false)
    }

    companion object {
        const val DEFAULT_SPEED = 100
        const val MIN_SPEED = 60
        const val MAX_SPEED = 160
    }
}
