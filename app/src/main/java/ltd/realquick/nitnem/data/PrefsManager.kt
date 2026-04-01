package ltd.realquick.nitnem.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val transliterationLanguage: String
        get() = prefs.getString(KEY_TRANSLITERATION, "en") ?: "en"

    val centerAlign: Boolean
        get() = prefs.getBoolean(KEY_CENTER_ALIGN, false)

    val keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)

    val rememberPosition: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_POSITION, true)

    val autoScrollEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCROLL_ENABLED, true)

    val backToTopEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACK_TO_TOP_ENABLED, false)

    val perBaniSpeed: Boolean
        get() = prefs.getBoolean(KEY_PER_BANI_SPEED, false)

    val perBaniFontSize: Boolean
        get() = prefs.getBoolean(KEY_PER_BANI_FONT_SIZE, false)

    // Font size
    fun getFontSize(slug: String?): Float {
        val key = if (perBaniFontSize && slug != null) "${KEY_FONT_SIZE}_$slug" else KEY_FONT_SIZE
        return prefs.getFloat(key, DEFAULT_FONT_SIZE)
    }

    fun setFontSize(slug: String?, size: Float) {
        val key = if (perBaniFontSize && slug != null) "${KEY_FONT_SIZE}_$slug" else KEY_FONT_SIZE
        prefs.edit().putFloat(key, size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)).apply()
    }

    // Scroll speed
    fun getScrollSpeed(slug: String?): Int {
        val key = if (perBaniSpeed && slug != null) "${KEY_SCROLL_SPEED}_$slug" else KEY_SCROLL_SPEED
        return prefs.getInt(key, DEFAULT_SCROLL_SPEED)
    }

    fun setScrollSpeed(slug: String?, speed: Int) {
        val key = if (perBaniSpeed && slug != null) "${KEY_SCROLL_SPEED}_$slug" else KEY_SCROLL_SPEED
        prefs.edit().putInt(key, speed.coerceIn(MIN_SCROLL_SPEED, MAX_SCROLL_SPEED)).apply()
    }

    // Reading position
    fun getScrollPosition(slug: String): Int {
        return prefs.getInt("${KEY_SCROLL_POS}_$slug", 0)
    }

    fun setScrollPosition(slug: String, position: Int) {
        prefs.edit().putInt("${KEY_SCROLL_POS}_$slug", position).apply()
    }

    companion object {
        const val KEY_TRANSLITERATION = "transliteration_language"
        const val KEY_CENTER_ALIGN = "center_align"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_REMEMBER_POSITION = "remember_position"
        const val KEY_AUTO_SCROLL_ENABLED = "auto_scroll_enabled"
        const val KEY_BACK_TO_TOP_ENABLED = "back_to_top_enabled"
        const val KEY_PER_BANI_SPEED = "per_bani_speed"
        const val KEY_PER_BANI_FONT_SIZE = "per_bani_font_size"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_SCROLL_SPEED = "scroll_speed"
        const val KEY_SCROLL_POS = "scroll_position"

        const val DEFAULT_FONT_SIZE = 18f
        const val MIN_FONT_SIZE = 12f
        const val MAX_FONT_SIZE = 32f
        const val FONT_SIZE_STEP = 1f

        const val DEFAULT_SCROLL_SPEED = 100 // percentage of current text line height per second
        const val MIN_SCROLL_SPEED = 60
        const val MAX_SCROLL_SPEED = 160
    }
}
