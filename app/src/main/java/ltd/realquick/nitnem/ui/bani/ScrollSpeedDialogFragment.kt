package ltd.realquick.nitnem.ui.bani

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.oneuiproject.oneui.preference.SeekBarPreferencePro
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.PrefsManager
import kotlin.math.abs

class ScrollSpeedDialogFragment : DialogFragment(R.layout.dialog_scroll_speed) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (childFragmentManager.findFragmentById(R.id.speedPreferencesContainer) == null) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.speedPreferencesContainer,
                    SpeedPreferencesFragment.newInstance(
                        requireArguments().getInt(ARG_SPEED, PrefsManager.DEFAULT_SCROLL_SPEED)
                    )
                )
                .commitNow()
        }

        view.findViewById<Button>(R.id.btnDone).setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    class SpeedPreferencesFragment : PreferenceFragmentCompat() {

        private lateinit var presetPreference: SeekBarPreferencePro
        private lateinit var customPreference: EditTextPreference
        private var syncingValues = false

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = DIALOG_PREFS_NAME
            setPreferencesFromResource(R.xml.reader_speed_preferences, rootKey)

            presetPreference = requireNotNull(findPreference<SeekBarPreferencePro>(KEY_PRESET_SPEED))
            customPreference = requireNotNull(findPreference<EditTextPreference>(KEY_CUSTOM_SPEED))

            customPreference.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelectAllOnFocus(true)
            }

            presetPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (syncingValues) return@OnPreferenceChangeListener true

                    val level = newValue as? Int ?: return@OnPreferenceChangeListener false
                    val speed = presetLevelToSpeed(level)
                    syncingValues = true
                    customPreference.text = speed.toString()
                    syncingValues = false
                    updateSpeed(speed)
                    true
                }

            customPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (syncingValues) return@OnPreferenceChangeListener true

                    val speed = (newValue as? String)?.trim()?.toIntOrNull()
                        ?: return@OnPreferenceChangeListener invalidCustomValue()
                    if (speed !in PrefsManager.MIN_SCROLL_SPEED..PrefsManager.MAX_SCROLL_SPEED) {
                        return@OnPreferenceChangeListener invalidCustomValue()
                    }
                    syncingValues = true
                    presetPreference.value = nearestPresetLevel(speed)
                    syncingValues = false
                    updateSpeed(speed)
                    true
                }

            syncFromSpeed(requireArguments().getInt(ARG_SPEED, PrefsManager.DEFAULT_SCROLL_SPEED))
        }

        private fun syncFromSpeed(speed: Int) {
            val clampedSpeed = speed.coerceIn(PrefsManager.MIN_SCROLL_SPEED, PrefsManager.MAX_SCROLL_SPEED)
            syncingValues = true
            presetPreference.value = nearestPresetLevel(clampedSpeed)
            customPreference.text = clampedSpeed.toString()
            syncingValues = false
        }

        private fun updateSpeed(speed: Int) {
            (parentFragment as? ScrollSpeedDialogFragment)?.dispatchSpeed(speed)
        }

        private fun invalidCustomValue(): Boolean {
            Toast.makeText(
                requireContext(),
                getString(
                    R.string.scroll_speed_custom_error,
                    PrefsManager.MIN_SCROLL_SPEED,
                    PrefsManager.MAX_SCROLL_SPEED
                ),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        private fun presetLevelToSpeed(level: Int): Int {
            return PRESET_SPEEDS[(level - MIN_PRESET_LEVEL).coerceIn(0, PRESET_SPEEDS.lastIndex)]
        }

        private fun nearestPresetLevel(speed: Int): Int {
            val clampedSpeed = speed.coerceIn(PrefsManager.MIN_SCROLL_SPEED, PrefsManager.MAX_SCROLL_SPEED)
            return (MIN_PRESET_LEVEL..MAX_PRESET_LEVEL).minByOrNull { level ->
                abs(presetLevelToSpeed(level) - clampedSpeed)
            } ?: MIN_PRESET_LEVEL
        }

        companion object {
            fun newInstance(initialSpeed: Int): SpeedPreferencesFragment {
                return SpeedPreferencesFragment().apply {
                    arguments = bundleOf(ARG_SPEED to initialSpeed)
                }
            }
        }
    }

    private fun dispatchSpeed(speed: Int) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf(RESULT_SPEED to speed)
        )
    }

    companion object {
        const val TAG = "scroll_speed_dialog"
        const val RESULT_KEY = "scroll_speed_result"
        const val RESULT_SPEED = "speed"

        private const val ARG_SPEED = "speed"
        private const val DIALOG_PREFS_NAME = "reader_speed_dialog"
        private const val KEY_PRESET_SPEED = "reader_speed_preset"
        private const val KEY_CUSTOM_SPEED = "reader_speed_custom"
        private const val MIN_PRESET_LEVEL = 2
        private const val MAX_PRESET_LEVEL = 10
        private const val DIALOG_WIDTH_RATIO = 0.9f
        private val PRESET_SPEEDS = intArrayOf(60, 70, 80, 90, 100, 115, 130, 145, 160)

        fun newInstance(initialSpeed: Int): ScrollSpeedDialogFragment {
            return ScrollSpeedDialogFragment().apply {
                arguments = bundleOf(ARG_SPEED to initialSpeed)
            }
        }
    }
}
