package ltd.realquick.nitnem.util

import android.view.Choreographer
import androidx.recyclerview.widget.RecyclerView

class AutoScroller(private val recyclerView: RecyclerView) {

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

        val maxY = getMaxScroll()
        if (getScrollOffset() >= maxY) return

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

        val maxY = getMaxScroll()
        if (maxY <= 0 || getScrollOffset() >= maxY) {
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
                    val nextY = (getScrollOffset() + deltaY).coerceAtMost(maxY)
                    recyclerView.scrollBy(0, nextY - getScrollOffset())
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

    private fun getScrollOffset(): Int = recyclerView.computeVerticalScrollOffset()

    private fun getMaxScroll(): Int {
        return (recyclerView.computeVerticalScrollRange() - recyclerView.computeVerticalScrollExtent())
            .coerceAtLeast(0)
    }

    companion object {
        const val DEFAULT_SPEED = 100
        const val MIN_SPEED = 40
        const val MAX_SPEED = 180
    }
}
