package ltd.realquick.nitnem.ui.bani

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.BaniRepository
import ltd.realquick.nitnem.data.PrefsManager
import ltd.realquick.nitnem.data.model.Bani
import ltd.realquick.nitnem.databinding.ActivityBaniBinding
import ltd.realquick.nitnem.util.AutoScroller

class BaniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaniBinding
    private lateinit var prefs: PrefsManager
    private lateinit var repo: BaniRepository
    private lateinit var autoScroller: AutoScroller

    private var slug: String = ""
    private var isFullscreen = false
    private var bani: Bani? = null
    private val sectionViews = mutableMapOf<String, View>()
    private val sections = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        repo = BaniRepository(this)
        autoScroller = AutoScroller(binding.scrollView)

        slug = intent.getStringExtra(EXTRA_SLUG) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        setupToolbar(title)
        setupScrollListener()
        setupFabs()
        loadBani()
    }

    override fun onPause() {
        super.onPause()
        autoScroller.stop()
        saveScrollPosition()
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupToolbar(title: String) {
        binding.toolbarLayout.setTitle(title)
        binding.toolbarLayout.setNavigationButtonOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbarLayout.toolbar.inflateMenu(R.menu.menu_bani)
        binding.toolbarLayout.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_font_decrease -> {
                    adjustFontSize(-PrefsManager.FONT_SIZE_STEP)
                    true
                }
                R.id.action_font_increase -> {
                    adjustFontSize(PrefsManager.FONT_SIZE_STEP)
                    true
                }
                R.id.action_fullscreen -> {
                    toggleFullscreen()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupScrollListener() {
        binding.scrollView.setOnScrollChangeListener { _: View, _: Int, scrollY: Int, _: Int, _: Int ->
            updateProgress(scrollY)
            updateCurrentSection(scrollY)
        }
    }

    private fun setupFabs() {
        autoScroller.updateSpeed(prefs.getScrollSpeed(if (prefs.perBaniSpeed) slug else null))

        binding.fabPlay.setOnClickListener {
            val playing = autoScroller.toggle()
            updatePlayState(playing)
        }

        binding.fabSpeedDown.setOnClickListener {
            autoScroller.adjustSpeed(-AutoScroller.SPEED_STEP)
            prefs.setScrollSpeed(if (prefs.perBaniSpeed) slug else null, autoScroller.speed)
        }

        binding.fabSpeedUp.setOnClickListener {
            autoScroller.adjustSpeed(AutoScroller.SPEED_STEP)
            prefs.setScrollSpeed(if (prefs.perBaniSpeed) slug else null, autoScroller.speed)
        }
    }

    private fun updatePlayState(playing: Boolean) {
        if (playing) {
            binding.fabPlay.setImageResource(android.R.drawable.ic_media_pause)
            binding.fabPlay.contentDescription = getString(R.string.auto_scroll_pause)
            binding.fabSpeedDown.isVisible = true
            binding.fabSpeedUp.isVisible = true
            if (prefs.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            binding.fabPlay.setImageResource(android.R.drawable.ic_media_play)
            binding.fabPlay.contentDescription = getString(R.string.auto_scroll_play)
            binding.fabSpeedDown.isVisible = false
            binding.fabSpeedUp.isVisible = false
        }
    }

    private fun loadBani() {
        bani = try {
            repo.loadBani(slug)
        } catch (e: Exception) {
            finish()
            return
        }

        renderBani(bani!!)
        setupSections(bani!!)
        applyKeepScreenOn()
        checkResumePosition()
    }

    private fun renderBani(bani: Bani) {
        val container = binding.contentContainer
        container.removeAllViews()

        val language = prefs.transliterationLanguage
        val fontSize = prefs.getFontSize(if (prefs.perBaniFontSize) slug else null)
        val centerAlign = prefs.centerAlign

        for (paragraph in bani.paragraphs) {
            val textView = TextView(this).apply {
                text = paragraph.lines.joinToString("\n") { line ->
                    when (language) {
                        "hi" -> line.hi
                        "pn" -> line.pn
                        else -> line.en
                    }
                }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
                setLineSpacing(0f, 1.3f)
                if (centerAlign) gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.paragraph_spacing))
            }

            if (paragraph.section != null) {
                sectionViews[paragraph.section] = textView
            }

            container.addView(textView)
        }
    }

    private fun setupSections(bani: Bani) {
        sections.clear()
        sections.addAll(bani.paragraphs.mapNotNull { it.section }.distinct())

        if (sections.isEmpty()) {
            binding.sectionTabsContainer.isVisible = false
            return
        }

        binding.sectionTabsContainer.isVisible = true
        binding.sectionTabs.removeAllTabs()

        for (section in sections) {
            binding.sectionTabs.addTab(
                binding.sectionTabs.newTab().setText(section)
            )
        }

        binding.sectionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val section = sections.getOrNull(tab.position) ?: return
                scrollToSection(section)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                val section = sections.getOrNull(tab.position) ?: return
                scrollToSection(section)
            }
        })
    }

    private fun scrollToSection(section: String) {
        val view = sectionViews[section] ?: return
        binding.scrollView.smoothScrollTo(0, view.top)
    }

    private fun updateCurrentSection(scrollY: Int) {
        if (sections.isEmpty()) return

        var currentIndex = 0
        for ((i, section) in sections.withIndex()) {
            val view = sectionViews[section] ?: continue
            if (view.top <= scrollY + 100) {
                currentIndex = i
            }
        }

        val tab = binding.sectionTabs.getTabAt(currentIndex)
        if (tab != null && !tab.isSelected) {
            // Remove listener to avoid recursive scroll
            tab.select()
        }
    }

    private fun updateProgress(scrollY: Int) {
        val child = binding.scrollView.getChildAt(0) ?: return
        val maxScroll = child.height - binding.scrollView.height
        val progress = if (maxScroll > 0) (scrollY * 100 / maxScroll) else 0
        binding.progressBar.progress = progress.coerceIn(0, 100)
    }

    private fun adjustFontSize(delta: Float) {
        val currentSize = prefs.getFontSize(if (prefs.perBaniFontSize) slug else null)
        val newSize = (currentSize + delta).coerceIn(PrefsManager.MIN_FONT_SIZE, PrefsManager.MAX_FONT_SIZE)
        prefs.setFontSize(if (prefs.perBaniFontSize) slug else null, newSize)

        for (i in 0 until binding.contentContainer.childCount) {
            val view = binding.contentContainer.getChildAt(i)
            if (view is TextView) {
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize)
            }
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun applyKeepScreenOn() {
        if (prefs.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun saveScrollPosition() {
        if (prefs.rememberPosition) {
            prefs.setScrollPosition(slug, binding.scrollView.scrollY)
        }
    }

    private fun checkResumePosition() {
        if (!prefs.rememberPosition) return
        val savedPos = prefs.getScrollPosition(slug)
        if (savedPos <= 0) return

        binding.resumeCard.isVisible = true
        binding.resumeButton.setOnClickListener {
            binding.resumeCard.isVisible = false
            binding.scrollView.post {
                binding.scrollView.scrollTo(0, savedPos)
                updateProgress(savedPos)
                updateCurrentSection(savedPos)
            }
        }
        binding.resumeDismiss.setOnClickListener {
            binding.resumeCard.isVisible = false
        }
    }

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_TITLE = "title"
    }
}
