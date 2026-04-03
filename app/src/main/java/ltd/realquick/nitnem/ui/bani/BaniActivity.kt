package ltd.realquick.nitnem.ui.bani

import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.R as iconsR
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.BaniRepository
import ltd.realquick.nitnem.data.PrefsManager
import ltd.realquick.nitnem.data.model.BaniLength
import ltd.realquick.nitnem.data.model.ReaderParagraph
import ltd.realquick.nitnem.databinding.ActivityBaniBinding
import ltd.realquick.nitnem.util.AutoScroller
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class BaniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaniBinding
    private lateinit var prefs: PrefsManager
    private lateinit var repo: BaniRepository
    private lateinit var autoScroller: AutoScroller
    private lateinit var readerAdapter: ReaderAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var fullscreenBackCallback: OnBackPressedCallback

    private val loadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainThreadExecutor by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getMainExecutor(this)
    }

    private var slug: String = ""
    private var isFullscreen = false
    private var selectedSectionIndex = -1
    private var suppressSectionScroll = false
    private var resumeScrollFraction: Float? = null
    private var currentLanguage: String? = null
    private var currentBaniLength: BaniLength? = null
    private var loadGeneration = 0

    private val sections = mutableListOf<String>()
    private val sectionParagraphIndices = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        repo = BaniRepository(this)
        layoutManager = LinearLayoutManager(this)
        readerAdapter = ReaderAdapter(
            onResumeContinue = ::resumeReading,
            onResumeDismiss = ::hideResumeCard
        )
        autoScroller = AutoScroller(binding.recyclerView).also {
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
        setupRecyclerView()
        setupControls()
        loadBani()
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

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
        saveScrollPosition()
    }

    override fun onResume() {
        super.onResume()
        val needsReload = currentLanguage != prefs.transliterationLanguage ||
            currentBaniLength != prefs.baniLength
        if (needsReload) {
            loadBani()
        } else {
            applyRuntimePreferences()
        }
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        loadExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun setupToolbar(title: String) {
        binding.toolbarLayout.setTitle(title)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = this@BaniActivity.layoutManager
            adapter = readerAdapter
            itemAnimator = null
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                    stopAutoScroll()
                }
                false
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val scrollY = getCurrentScrollY()
                    updateProgress(scrollY)
                    updateCurrentSection()
                }
            })
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

    private fun loadBani() {
        val generation = ++loadGeneration
        val length = prefs.baniLength
        val language = prefs.transliterationLanguage
        hideResumeCard()

        loadExecutor.execute {
            val paragraphs = try {
                repo.loadReaderParagraphs(slug, length, language)
            } catch (_: Exception) {
                mainThreadExecutor.execute {
                    if (!isFinishing && !isDestroyed) {
                        finish()
                    }
                }
                return@execute
            }

            mainThreadExecutor.execute {
                if (isDestroyed || generation != loadGeneration) return@execute

                currentLanguage = language
                currentBaniLength = length
                readerAdapter.submitParagraphs(paragraphs)
                setupSections(paragraphs)
                applyRuntimePreferences()

                binding.recyclerView.post {
                    checkResumePosition()
                    val scrollY = getCurrentScrollY()
                    updateProgress(scrollY)
                    updateCurrentSection()
                }
            }
        }
    }

    private fun setupSections(paragraphs: List<ReaderParagraph>) {
        binding.sectionTabs.clearOnTabSelectedListeners()
        binding.sectionTabs.removeAllTabs()
        sections.clear()
        sectionParagraphIndices.clear()
        selectedSectionIndex = -1

        val sectionSpecs = when (slug) {
            SUKHMANI_SLUG -> paragraphs.mapIndexedNotNull { index, paragraph ->
                if (normalizeSection(paragraph.section) == SUKHMANI_SECTION_MARKER) index else null
            }.mapIndexed { order, paragraphIndex ->
                getString(R.string.astpadi_tab_title, order + 1) to paragraphIndex
            }

            ASA_DI_VAAR_SLUG -> paragraphs.mapIndexedNotNull { index, paragraph ->
                if (normalizeSection(paragraph.section) == ASA_DI_VAAR_SECTION_MARKER) index else null
            }.mapIndexed { order, paragraphIndex ->
                getString(R.string.pauri_tab_title, order + 1) to paragraphIndex
            }

            else -> emptyList()
        }

        if (sectionSpecs.isEmpty()) {
            binding.sectionTabs.visibility = View.GONE
            return
        }

        binding.sectionTabs.visibility = View.VISIBLE
        sectionSpecs.forEach { (title, paragraphIndex) ->
            sections += title
            sectionParagraphIndices += paragraphIndex
            binding.sectionTabs.addTab(binding.sectionTabs.newTab().setText(title), false)
        }

        binding.sectionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedSectionIndex = tab.position
                if (!suppressSectionScroll) {
                    scrollToSection(tab.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) {
                if (!suppressSectionScroll) {
                    scrollToSection(tab.position)
                }
            }
        })

        selectSectionTab(0)
    }

    private fun scrollToSection(index: Int) {
        val paragraphIndex = sectionParagraphIndices.getOrNull(index) ?: return
        stopAutoScroll()
        smoothScrollToAdapterPosition(readerAdapter.headerOffset + paragraphIndex)
    }

    private fun updateCurrentSection() {
        if (sectionParagraphIndices.isEmpty()) return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        val paragraphIndex = (firstVisible - readerAdapter.headerOffset).coerceAtLeast(0)
        var currentIndex = 0
        for ((index, anchorParagraphIndex) in sectionParagraphIndices.withIndex()) {
            if (anchorParagraphIndex <= paragraphIndex) {
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
        if (newSize == currentSize) return

        val scrollFraction = getScrollFraction(getCurrentScrollY())
        prefs.setFontSize(key, newSize)
        applyReaderTypography()

        binding.recyclerView.post {
            val targetScrollY = getScrollYForFraction(scrollFraction)
            scrollToOffset(targetScrollY)
            updateProgress(targetScrollY)
            updateCurrentSection()
        }
    }

    private fun applyReaderTypography() {
        readerAdapter.updateTypography(
            fontSize = prefs.getFontSize(if (prefs.perBaniFontSize) slug else null),
            centerAlign = prefs.centerAlign
        )
    }

    private fun resolveAutoScrollUnitPx(): Float {
        for (index in 0 until binding.recyclerView.childCount) {
            val paragraphView = binding.recyclerView.getChildAt(index)
                .findViewById<View>(R.id.textParagraph)
            if (paragraphView is android.widget.TextView && paragraphView.lineHeight > 0) {
                return paragraphView.lineHeight.toFloat()
            }
        }
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            prefs.getFontSize(if (prefs.perBaniFontSize) slug else null),
            resources.displayMetrics
        ) * 1.3f
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
        binding.recyclerView.seslSetGoToTopEnabled(prefs.backToTopEnabled)
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
        applyReaderTypography()
        autoScroller.updateSpeed(prefs.getScrollSpeed(if (prefs.perBaniSpeed) slug else null))
        if (!prefs.autoScrollEnabled) {
            stopAutoScroll()
        }
        applyGoToTopPreference()
        updateAutoScrollControls(autoScroller.isScrolling)
    }

    private fun updateAutoScrollControls(isPlaying: Boolean) {
        binding.btnAutoScrollSpeed.contentDescription = getString(R.string.scroll_speed_title)
        binding.btnAutoScrollSpeed.visibility = if (prefs.autoScrollEnabled && isPlaying) {
            View.VISIBLE
        } else {
            View.GONE
        }
        invalidateOptionsMenu()
    }

    private fun stopAutoScroll() {
        if (autoScroller.isScrolling) {
            autoScroller.stop()
        }
    }

    private fun persistScrollSpeed() {
        prefs.setScrollSpeed(if (prefs.perBaniSpeed) slug else null, autoScroller.speed)
    }

    private fun hideResumeCard(onHidden: (() -> Unit)? = null) {
        resumeScrollFraction = null
        readerAdapter.setResumeCardVisible(false, onHidden)
    }

    private fun saveScrollPosition() {
        if (!prefs.rememberPosition) return
        prefs.setScrollPosition(slug, getScrollFraction(getCurrentScrollY()))
    }

    private fun checkResumePosition() {
        if (!prefs.rememberPosition) return

        val savedFraction = prefs.getScrollPosition(slug)
        if (savedFraction <= 0f) return
        if (savedFraction !in MIN_RESUME_FRACTION until MAX_RESUME_FRACTION) {
            return
        }

        resumeScrollFraction = savedFraction
        readerAdapter.setResumeCardVisible(true)
    }

    private fun resumeReading() {
        val targetFraction = resumeScrollFraction ?: prefs.getScrollPosition(slug)
        hideResumeCard {
            binding.recyclerView.post {
                val targetScrollY = getScrollYForFraction(targetFraction)
                scrollToOffset(targetScrollY)
                updateProgress(targetScrollY)
                updateCurrentSection()
                binding.toolbarLayout.setExpanded(false, true)
            }
        }
    }

    private fun getCurrentScrollY(): Int = binding.recyclerView.computeVerticalScrollOffset()

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
        return (binding.recyclerView.computeVerticalScrollRange() -
            binding.recyclerView.computeVerticalScrollExtent()).coerceAtLeast(0)
    }

    private fun scrollToOffset(scrollY: Int) {
        if (readerAdapter.itemCount == 0) return
        val clampedScrollY = scrollY.coerceIn(0, getMaxScroll())
        binding.recyclerView.stopScroll()
        binding.recyclerView.scrollBy(0, clampedScrollY - getCurrentScrollY())
    }

    private fun smoothScrollToAdapterPosition(position: Int) {
        if (readerAdapter.itemCount == 0) return
        val smoothScroller = object : LinearSmoothScroller(this) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
        }
        smoothScroller.targetPosition = position
        layoutManager.startSmoothScroll(smoothScroller)
    }

    private fun normalizeSection(section: String?): String {
        return section
            ?.lowercase()
            ?.replace(SECTION_WHITESPACE_REGEX, " ")
            ?.trim()
            .orEmpty()
    }

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_TITLE = "title"

        private const val SUKHMANI_SLUG = "sukhmani-sahib"
        private const val ASA_DI_VAAR_SLUG = "asa-di-vaar"
        private const val SUKHMANI_SECTION_MARKER = "salok ||"
        private const val ASA_DI_VAAR_SECTION_MARKER = "pauree ||"
        private const val MIN_RESUME_FRACTION = 0.05f
        private const val MAX_RESUME_FRACTION = 0.85f
        private val SECTION_WHITESPACE_REGEX = "\\s+".toRegex()
    }
}
