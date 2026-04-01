package ltd.realquick.nitnem.ui.bani

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.R as iconsR
import dev.oneuiproject.oneui.design.R as designR
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.BaniRepository
import ltd.realquick.nitnem.data.PrefsManager
import ltd.realquick.nitnem.data.model.Bani
import ltd.realquick.nitnem.databinding.ActivityBaniBinding
import ltd.realquick.nitnem.util.AutoScroller
import kotlin.math.roundToInt

class BaniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaniBinding
    private lateinit var prefs: PrefsManager
    private lateinit var repo: BaniRepository
    private lateinit var autoScroller: AutoScroller
    private lateinit var resumeCardView: View
    private lateinit var fullscreenBackCallback: OnBackPressedCallback

    private var slug: String = ""
    private var isFullscreen = false
    private val sectionViews = linkedMapOf<String, View>()
    private val sections = mutableListOf<String>()
    private var selectedSectionIndex = -1
    private var suppressSectionScroll = false
    private var resumeScrollFraction: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        repo = BaniRepository(this)
        autoScroller = AutoScroller(binding.scrollView).also {
            it.onStateChanged = ::updateAutoScrollControls
            it.scrollUnitPxProvider = ::resolveAutoScrollUnitPx
        }
        fullscreenBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                setFullscreen(false)
            }
        }
        onBackPressedDispatcher.addCallback(this, fullscreenBackCallback)
        supportFragmentManager.setFragmentResultListener(
            ScrollSpeedDialogFragment.RESULT_KEY,
            this
        ) { _, bundle ->
            applyScrollSpeed(bundle.getInt(ScrollSpeedDialogFragment.RESULT_SPEED))
        }

        slug = intent.getStringExtra(EXTRA_SLUG) ?: run {
            finish()
            return
        }
        val baniTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        title = baniTitle

        setupToolbar(baniTitle)
        setupResumeCard()
        setupScrollHandling()
        setupControls()
        loadBani()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
        saveScrollPosition()
    }

    override fun onResume() {
        super.onResume()
        applyRuntimePreferences()
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupToolbar(title: String) {
        binding.toolbarLayout.setTitle(title)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bani, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_auto_scroll)?.apply {
            isVisible = prefs.autoScrollEnabled
            title = getString(
                if (autoScroller.isScrolling) R.string.auto_scroll_pause else R.string.auto_scroll_play
            )
            setIcon(
                if (autoScroller.isScrolling) iconsR.drawable.ic_oui_control_pause
                else iconsR.drawable.ic_oui_control_play
            )
        }
        menu.findItem(R.id.action_fullscreen)?.title = getString(
            if (isFullscreen) R.string.action_exit_fullscreen else R.string.action_enter_fullscreen
        )
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_auto_scroll -> {
                if (prefs.autoScrollEnabled) {
                    val isPlaying = autoScroller.toggle()
                    if (isPlaying) {
                        binding.toolbarLayout.setExpanded(false, true)
                    }
                }
                true
            }

            R.id.action_font_decrease -> {
                adjustFontSize(-PrefsManager.FONT_SIZE_STEP)
                true
            }

            R.id.action_font_increase -> {
                adjustFontSize(PrefsManager.FONT_SIZE_STEP)
                true
            }

            R.id.action_fullscreen -> {
                setFullscreen(!isFullscreen)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupResumeCard() {
        resumeCardView = layoutInflater.inflate(
            designR.layout.oui_des_preference_suggestion_card,
            binding.resumeCardContainer,
            false
        )
        binding.resumeCardContainer.addView(resumeCardView)
        binding.resumeCardContainer.isVisible = false
        resumeCardView.setBackgroundResource(R.drawable.resume_card_bg)

        resumeCardView.findViewById<ImageView>(android.R.id.icon)
            .setImageResource(designR.drawable.oui_des_preference_suggestion_card_icon)
        resumeCardView.findViewById<TextView>(android.R.id.title).text = getString(R.string.resume_title)
        resumeCardView.findViewById<TextView>(android.R.id.summary).text = getString(R.string.resume_summary)
        resumeCardView.findViewById<TextView>(designR.id.action_button_text).text =
            getString(R.string.resume_action)
        resumeCardView.findViewById<View>(designR.id.exit_button).setOnClickListener {
            hideResumeCard()
        }
    }

    private fun setupScrollHandling() {
        binding.scrollView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                stopAutoScroll()
            }
            false
        }

        binding.scrollView.setOnScrollChangeListener { _: View, _: Int, scrollY: Int, _: Int, _: Int ->
            updateProgress(scrollY)
            updateCurrentSection(scrollY)
        }
    }

    private fun setupControls() {
        autoScroller.updateSpeed(prefs.getScrollSpeed(if (prefs.perBaniSpeed) slug else null))

        binding.btnAutoScrollSpeed.setOnClickListener {
            showScrollSpeedDialog()
        }

        applyGoToTopPreference()
        updateAutoScrollControls(autoScroller.isScrolling)
    }

    private fun updateAutoScrollControls(isPlaying: Boolean) {
        binding.btnAutoScrollSpeed.contentDescription = getString(R.string.scroll_speed_title)
        binding.btnAutoScrollSpeed.isVisible = prefs.autoScrollEnabled && isPlaying
        invalidateOptionsMenu()
    }

    private fun loadBani() {
        val bani = try {
            repo.loadBani(slug)
        } catch (_: Exception) {
            finish()
            return
        }

        renderBani(bani)
        setupSections()
        applyKeepScreenOn()

        binding.scrollView.post {
            checkResumePosition()
            updateProgress(binding.scrollView.scrollY)
            updateCurrentSection(binding.scrollView.scrollY)
        }
    }

    private fun renderBani(bani: Bani) {
        binding.contentContainer.removeAllViews()
        sectionViews.clear()

        val language = prefs.transliterationLanguage
        val fontSize = prefs.getFontSize(if (prefs.perBaniFontSize) slug else null)
        val centerAlign = prefs.centerAlign
        var astpadiNumber = 0

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
                setTextColor(resolveThemeColor(android.R.attr.textColorPrimary))
                setLineSpacing(0f, 1.3f)
                gravity = if (centerAlign) Gravity.CENTER_HORIZONTAL else Gravity.START
                setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.paragraph_spacing))
            }

            if (slug == SUKHMANI_SLUG && paragraph.section == SUKHMANI_ASTPADI_MARKER) {
                astpadiNumber += 1
                sectionViews[getString(R.string.astpadi_tab_title, astpadiNumber)] = textView
            }

            binding.contentContainer.addView(textView)
        }
    }

    private fun setupSections() {
        binding.sectionTabs.clearOnTabSelectedListeners()
        binding.sectionTabs.removeAllTabs()
        sections.clear()
        selectedSectionIndex = -1

        if (slug != SUKHMANI_SLUG || sectionViews.isEmpty()) {
            binding.sectionTabs.isVisible = false
            return
        }

        sections.addAll(sectionViews.keys)
        binding.sectionTabs.isVisible = true

        sections.forEach { section ->
            binding.sectionTabs.addTab(binding.sectionTabs.newTab().setText(section), false)
        }

        binding.sectionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedSectionIndex = tab.position
                if (!suppressSectionScroll) {
                    sections.getOrNull(tab.position)?.let(::scrollToSection)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) {
                if (!suppressSectionScroll) {
                    sections.getOrNull(tab.position)?.let(::scrollToSection)
                }
            }
        })

        selectSectionTab(0)
    }

    private fun scrollToSection(section: String) {
        stopAutoScroll()
        val view = sectionViews[section] ?: return
        binding.scrollView.post {
            binding.scrollView.smoothScrollTo(0, view.top)
        }
    }

    private fun updateCurrentSection(scrollY: Int) {
        if (sections.isEmpty()) return

        var currentIndex = 0
        for ((index, section) in sections.withIndex()) {
            val view = sectionViews[section] ?: continue
            if (view.top <= scrollY + 100) {
                currentIndex = index
            }
        }

        selectSectionTab(currentIndex)
    }

    private fun selectSectionTab(index: Int) {
        val tab = binding.sectionTabs.getTabAt(index) ?: return
        if (selectedSectionIndex == index && tab.isSelected) return

        selectedSectionIndex = index
        if (!tab.isSelected) {
            suppressSectionScroll = true
            tab.select()
            suppressSectionScroll = false
        }
    }

    private fun updateProgress(scrollY: Int) {
        binding.progressBar.progress = getScrollProgress(scrollY)
    }

    private fun adjustFontSize(delta: Float) {
        val key = if (prefs.perBaniFontSize) slug else null
        val currentSize = prefs.getFontSize(key)
        val newSize = (currentSize + delta).coerceIn(PrefsManager.MIN_FONT_SIZE, PrefsManager.MAX_FONT_SIZE)
        val scrollFraction = getScrollFraction(binding.scrollView.scrollY)
        prefs.setFontSize(key, newSize)

        for (index in 0 until binding.contentContainer.childCount) {
            val view = binding.contentContainer.getChildAt(index)
            if (view is TextView) {
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize)
            }
        }

        binding.scrollView.post {
            val targetScrollY = getScrollYForFraction(scrollFraction)
            binding.scrollView.scrollTo(0, targetScrollY)
            updateProgress(targetScrollY)
            updateCurrentSection(targetScrollY)
        }
    }

    private fun resolveAutoScrollUnitPx(): Float {
        for (index in 0 until binding.contentContainer.childCount) {
            val view = binding.contentContainer.getChildAt(index)
            if (view is TextView && view.lineHeight > 0) {
                return view.lineHeight.toFloat()
            }
        }
        return 1f
    }

    private fun showScrollSpeedDialog() {
        if (!prefs.autoScrollEnabled) return
        if (supportFragmentManager.findFragmentByTag(ScrollSpeedDialogFragment.TAG) != null) return

        ScrollSpeedDialogFragment.newInstance(autoScroller.speed)
            .show(supportFragmentManager, ScrollSpeedDialogFragment.TAG)
    }

    private fun applyScrollSpeed(speed: Int) {
        autoScroller.updateSpeed(speed)
        persistScrollSpeed()
    }

    private fun applyKeepScreenOn() {
        if (prefs.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyGoToTopPreference() {
        val candidates = binding.scrollView.javaClass.declaredMethods + binding.scrollView.javaClass.methods
        val method = candidates.firstOrNull { candidate ->
            candidate.name == "seslSetGoToTopEnabled" &&
                candidate.parameterTypes.contentEquals(arrayOf(Boolean::class.javaPrimitiveType))
        } ?: return
        runCatching {
            method.isAccessible = true
            method.invoke(binding.scrollView, prefs.backToTopEnabled)
        }
    }

    private fun setFullscreen(enabled: Boolean) {
        if (isFullscreen == enabled) return

        isFullscreen = enabled
        fullscreenBackCallback.isEnabled = enabled

        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        invalidateOptionsMenu()
    }

    private fun applyRuntimePreferences() {
        applyKeepScreenOn()
        autoScroller.updateSpeed(prefs.getScrollSpeed(if (prefs.perBaniSpeed) slug else null))
        if (!prefs.autoScrollEnabled) {
            stopAutoScroll()
        }
        applyGoToTopPreference()
        updateAutoScrollControls(autoScroller.isScrolling)
    }

    private fun stopAutoScroll() {
        if (autoScroller.isScrolling) {
            autoScroller.stop()
        }
    }

    private fun persistScrollSpeed() {
        prefs.setScrollSpeed(if (prefs.perBaniSpeed) slug else null, autoScroller.speed)
    }

    private fun hideResumeCard() {
        binding.resumeCardContainer.isVisible = false
        resumeScrollFraction = null
    }

    private fun saveScrollPosition() {
        if (!prefs.rememberPosition) return
        prefs.setScrollPosition(slug, binding.scrollView.scrollY)
    }

    private fun checkResumePosition() {
        if (!prefs.rememberPosition) return

        val savedPos = prefs.getScrollPosition(slug)
        if (savedPos <= 0) return
        if (getScrollProgress(savedPos) !in MIN_RESUME_PROGRESS until MAX_RESUME_PROGRESS) {
            return
        }

        resumeScrollFraction = getScrollFraction(savedPos)
        binding.resumeCardContainer.isVisible = true
        resumeCardView.findViewById<View>(designR.id.action_button_container).setOnClickListener {
            val targetFraction = resumeScrollFraction ?: getScrollFraction(savedPos)
            hideResumeCard()
            binding.scrollView.post {
                val targetScrollY = getScrollYForFraction(targetFraction)
                binding.scrollView.scrollTo(0, targetScrollY)
                updateProgress(targetScrollY)
                updateCurrentSection(targetScrollY)
                binding.toolbarLayout.setExpanded(false, true)
            }
        }
    }

    private fun getScrollProgress(scrollY: Int): Int {
        val maxScroll = getMaxScroll()
        if (maxScroll <= 0) return 0
        return (scrollY * 100 / maxScroll).coerceIn(0, 100)
    }

    private fun getScrollFraction(scrollY: Int): Float {
        val maxScroll = getMaxScroll()
        if (maxScroll <= 0) return 0f
        return (scrollY.toFloat() / maxScroll).coerceIn(0f, 1f)
    }

    private fun getScrollYForFraction(fraction: Float): Int {
        val maxScroll = getMaxScroll()
        if (maxScroll <= 0) return 0
        return (maxScroll * fraction.coerceIn(0f, 1f)).roundToInt()
    }

    private fun getMaxScroll(): Int {
        val child = binding.scrollView.getChildAt(0) ?: return 0
        return (child.height - binding.scrollView.height).coerceAtLeast(0)
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return if (typedValue.resourceId != 0) {
            getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_TITLE = "title"

        private const val SUKHMANI_SLUG = "sukhmani-sahib"
        private const val SUKHMANI_ASTPADI_MARKER = "asaTapadhee ||"
        private const val MIN_RESUME_PROGRESS = 5
        private const val MAX_RESUME_PROGRESS = 85
    }
}
