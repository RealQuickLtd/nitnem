package ltd.realquick.nitnem.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneui.widget.CardItemView
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.model.BaniInfo

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bani, parent, false) as CardItemView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filtered[position]
        holder.cardItem.title = item.nameEn
        holder.cardItem.showTopDivider = position != 0
        holder.cardItem.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = filtered.size

    class ViewHolder(val cardItem: CardItemView) : RecyclerView.ViewHolder(cardItem)
}
