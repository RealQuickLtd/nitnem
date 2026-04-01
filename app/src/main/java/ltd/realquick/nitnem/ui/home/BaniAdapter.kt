package ltd.realquick.nitnem.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ltd.realquick.nitnem.data.model.BaniInfo
import ltd.realquick.nitnem.databinding.ItemBaniBinding

class BaniAdapter(
    private val onClick: (BaniInfo) -> Unit
) : RecyclerView.Adapter<BaniAdapter.ViewHolder>() {

    private var items: List<BaniInfo> = emptyList()
    private var filtered: List<BaniInfo> = emptyList()

    fun submitList(list: List<BaniInfo>) {
        items = list
        filtered = list
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.nameEn.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBaniBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filtered[position]
        holder.binding.baniName.text = item.nameEn
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = filtered.size

    class ViewHolder(val binding: ItemBaniBinding) : RecyclerView.ViewHolder(binding.root)
}
