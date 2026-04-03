package ltd.realquick.nitnem.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.PrefsManager
import ltd.realquick.nitnem.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var prefs: PrefsManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            prefs = PrefsManager(requireContext())
            setPreferencesFromResource(R.xml.preferences, rootKey)

            bindResumeInvalidation(PrefsManager.KEY_TRANSLITERATION) { prefs.transliterationLanguage }
            bindResumeInvalidation(PrefsManager.KEY_BANI_LENGTH) { prefs.baniLength.prefValue }
        }

        private fun bindResumeInvalidation(
            key: String,
            currentValue: () -> String
        ) {
            findPreference<DropDownPreference>(key)?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue?.toString() != currentValue()) {
                    prefs.clearScrollPositions()
                }
                true
            }
        }
    }
}
