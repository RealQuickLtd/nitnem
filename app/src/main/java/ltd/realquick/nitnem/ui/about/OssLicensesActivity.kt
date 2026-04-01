package ltd.realquick.nitnem.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ltd.realquick.nitnem.databinding.ActivityOssLicensesBinding
import ltd.realquick.nitnem.databinding.ItemLicenseBinding

class OssLicensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOssLicensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOssLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarLayout.setNavigationButtonOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = LicenseAdapter(LICENSES) { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private data class License(val name: String, val license: String, val url: String)

    private class LicenseAdapter(
        private val items: List<License>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<LicenseAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemLicenseBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemLicenseBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.licenseName.text = item.name
            holder.binding.licenseInfo.text = item.license
            holder.itemView.setOnClickListener { onClick(item.url) }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        private val LICENSES = listOf(
            License(
                "SESL Material Components for Android",
                "Apache License 2.0",
                "https://github.com/tribalfs/sesl-material-components-android"
            ),
            License(
                "SESL AndroidX",
                "Apache License 2.0",
                "https://github.com/tribalfs/sesl-androidx"
            ),
            License(
                "OneUI Design Library",
                "MIT License",
                "https://github.com/tribalfs/oneui-design"
            ),
            License(
                "BaniDB API",
                "GPL-3.0",
                "https://github.com/KhalisFoundation/banidb-api"
            ),
            License(
                "Rikka Refine",
                "MIT License",
                "https://github.com/ArcticFoxPro/RikkaX"
            )
        )
    }
}
