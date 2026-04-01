package ltd.realquick.nitnem

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import dev.oneuiproject.oneui.layout.ToolbarLayout
import ltd.realquick.nitnem.data.BaniRepository
import ltd.realquick.nitnem.databinding.ActivityMainBinding
import ltd.realquick.nitnem.ui.about.AboutActivity
import ltd.realquick.nitnem.ui.bani.BaniActivity
import ltd.realquick.nitnem.ui.home.BaniAdapter
import ltd.realquick.nitnem.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BaniAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = BaniAdapter { bani ->
            startActivity(
                Intent(this, BaniActivity::class.java).apply {
                    putExtra(BaniActivity.EXTRA_SLUG, bani.slug)
                    putExtra(BaniActivity.EXTRA_TITLE, bani.nameEn)
                }
            )
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.submitList(BaniRepository.BANI_LIST)

        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbarLayout.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbarLayout.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    binding.toolbarLayout.startSearchMode(searchModeListener)
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private val searchModeListener = object : ToolbarLayout.SearchModeListener {
        override fun onQueryTextSubmit(query: String?): Boolean = false

        override fun onQueryTextChange(newText: String?): Boolean {
            adapter.filter(newText.orEmpty())
            return true
        }

        override fun onSearchModeToggle(searchView: SearchView, visible: Boolean) {
            if (!visible) {
                adapter.filter("")
            }
        }
    }
}
