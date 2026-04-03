package ltd.realquick.nitnem.ui.bani

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.util.TypedValue
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneui.design.R as designR
import ltd.realquick.nitnem.R
import ltd.realquick.nitnem.data.model.ReaderParagraph

class ReaderAdapter(
    private val onResumeContinue: () -> Unit,
    private val onResumeDismiss: () -> Unit
) : ListAdapter<ReaderAdapter.Item, RecyclerView.ViewHolder>(DiffCallback) {

    private var paragraphs: List<ReaderParagraph> = emptyList()
    private var showResumeCard = false
    private var fontSize = 18f
    private var centerAlign = false

    init {
        setHasStableIds(true)
    }

    val headerOffset: Int
        get() = if (showResumeCard) 1 else 0

    fun submitParagraphs(paragraphs: List<ReaderParagraph>, onCommitted: (() -> Unit)? = null) {
        this.paragraphs = paragraphs
        rebuildItems(onCommitted)
    }

    fun setResumeCardVisible(visible: Boolean, onCommitted: (() -> Unit)? = null) {
        if (showResumeCard == visible) {
            onCommitted?.invoke()
            return
        }
        showResumeCard = visible
        rebuildItems(onCommitted)
    }

    fun updateTypography(fontSize: Float, centerAlign: Boolean) {
        if (this.fontSize == fontSize && this.centerAlign == centerAlign) return
        this.fontSize = fontSize
        this.centerAlign = centerAlign
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_TYPOGRAPHY)
        }
    }

    override fun getItemId(position: Int): Long = getItem(position).stableId

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            Item.ResumeCard -> VIEW_TYPE_RESUME
            is Item.ParagraphItem -> VIEW_TYPE_PARAGRAPH
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_RESUME -> ResumeViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_resume_card, parent, false),
                onResumeContinue,
                onResumeDismiss
            )

            else -> ParagraphViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_reader_paragraph, parent, false) as TextView
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            Item.ResumeCard -> (holder as ResumeViewHolder).bind()
            is Item.ParagraphItem -> (holder as ParagraphViewHolder).bind(item.paragraph, fontSize, centerAlign)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_TYPOGRAPHY) && holder is ParagraphViewHolder) {
            val item = getItem(position) as? Item.ParagraphItem ?: return
            holder.applyTypography(fontSize, centerAlign)
            holder.bindText(item.paragraph.text)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    private fun rebuildItems(onCommitted: (() -> Unit)? = null) {
        submitList(
            buildList {
                if (showResumeCard) add(Item.ResumeCard)
                paragraphs.forEach { add(Item.ParagraphItem(it)) }
            },
            onCommitted
        )
    }

    private sealed interface Item {
        val stableId: Long

        data object ResumeCard : Item {
            override val stableId: Long = Long.MIN_VALUE
        }

        data class ParagraphItem(val paragraph: ReaderParagraph) : Item {
            override val stableId: Long = paragraph.id.toLong()
        }
    }

    private class ResumeViewHolder(
        itemView: View,
        onResumeContinue: () -> Unit,
        onResumeDismiss: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val card = LayoutInflater.from(itemView.context).inflate(
            designR.layout.oui_des_preference_suggestion_card,
            itemView.findViewById<FrameLayout>(R.id.resumeCardHost),
            true
        )
        private val title = card.findViewById<TextView>(android.R.id.title)
        private val summary = card.findViewById<TextView>(android.R.id.summary)
        private val actionText = card.findViewById<TextView>(designR.id.action_button_text)

        init {
            card.setBackgroundResource(R.drawable.resume_card_bg)
            card.findViewById<ImageView>(android.R.id.icon)
                .setImageResource(designR.drawable.oui_des_preference_suggestion_card_icon)
            card.findViewById<View>(designR.id.action_button_container).setOnClickListener {
                onResumeContinue()
            }
            card.findViewById<View>(designR.id.exit_button).setOnClickListener {
                onResumeDismiss()
            }
        }

        fun bind() {
            val context = itemView.context
            title.text = context.getString(R.string.resume_title)
            summary.text = context.getString(R.string.resume_summary)
            actionText.text = context.getString(R.string.resume_action)
        }
    }

    private class ParagraphViewHolder(
        private val textView: TextView
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(paragraph: ReaderParagraph, fontSize: Float, centerAlign: Boolean) {
            bindText(paragraph.text)
            applyTypography(fontSize, centerAlign)
        }

        fun bindText(text: String) {
            textView.text = text
        }

        fun applyTypography(fontSize: Float, centerAlign: Boolean) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            textView.gravity = if (centerAlign) {
                Gravity.CENTER_HORIZONTAL
            } else {
                Gravity.START
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.stableId == newItem.stableId
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_RESUME = 1
        private const val VIEW_TYPE_PARAGRAPH = 2
        private const val PAYLOAD_TYPOGRAPHY = "typography"
    }
}
