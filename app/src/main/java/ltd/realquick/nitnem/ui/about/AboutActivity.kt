package ltd.realquick.nitnem.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.oneuiproject.oneui.layout.AppInfoLayout
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appInfoLayout.setMainButtonClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.appInfoLayout.updateStatus = AppInfoLayout.Status.NotUpdatable

        binding.githubButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
        }

        binding.licensesButton.setOnClickListener {
            startActivity(Intent(this, OssLicensesActivity::class.java))
        }
    }

    companion object {
        private const val GITHUB_URL = "https://github.com/RealQuickLtd/nitnem"
    }
}
