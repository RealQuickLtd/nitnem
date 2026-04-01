package ltd.realquick.nitnem.util

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.core.widget.NestedScrollView

class AutoScroller(private val scrollView: NestedScrollView) {

    private var animator: ValueAnimator? = null
    var speed: Int = DEFAULT_SPEED
        private set
    var isScrolling: Boolean = false
        private set

    fun start() {
        if (isScrolling) return
        isScrolling = true
        startAnimation()
    }

    fun pause() {
        isScrolling = false
        animator?.cancel()
        animator = null
    }

    fun toggle(): Boolean {
        if (isScrolling) pause() else start()
        return isScrolling
    }

    fun adjustSpeed(delta: Int) {
        speed = (speed + delta).coerceIn(MIN_SPEED, MAX_SPEED)
        if (isScrolling) {
            animator?.cancel()
            startAnimation()
        }
    }

    fun stop() {
        isScrolling = false
        animator?.cancel()
        animator = null
    }

    fun updateSpeed(newSpeed: Int) {
        speed = newSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    private fun startAnimation() {
        val child = scrollView.getChildAt(0) ?: return
        val currentY = scrollView.scrollY
        val maxY = child.height - scrollView.height
        if (currentY >= maxY) {
            isScrolling = false
            return
        }

        val remaining = maxY - currentY
        val durationMs = (remaining.toFloat() / speed * 1000).toLong()

        animator = ValueAnimator.ofInt(currentY, maxY).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                if (isScrolling) {
                    scrollView.scrollTo(0, anim.animatedValue as Int)
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isScrolling = false
                }
            })
            start()
        }
    }

    companion object {
        const val DEFAULT_SPEED = 60
        const val MIN_SPEED = 15
        const val MAX_SPEED = 200
        const val SPEED_STEP = 15
    }
}
