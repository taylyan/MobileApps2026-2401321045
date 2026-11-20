package com.huntersstory.app.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.huntersstory.app.R
import com.huntersstory.app.data.repository.CollectionItem
import com.huntersstory.app.data.repository.CollectionType

class CollectionAdapter(
    private val type: CollectionType,
    private val onEdit: (CollectionItem) -> Unit,
    private val onDelete: (CollectionItem) -> Unit
) : ListAdapter<CollectionItem, CollectionAdapter.ItemHolder>(
    ItemDiff()
) {

    class ItemHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val title: TextView =
            view.findViewById(R.id.tvItemTitle)

        val subtitle: TextView =
            view.findViewById(R.id.tvItemSubtitle)

        val notes: TextView =
            view.findViewById(R.id.tvItemNotes)

        val editButton: Button =
            view.findViewById(R.id.btnEditItem)

        val deleteButton: Button =
            view.findViewById(R.id.btnDeleteItem)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection, parent, false)

        return ItemHolder(view)
    }

    override fun onBindViewHolder(
        holder: ItemHolder,
        position: Int
    ) {
        val item = getItem(position)

        holder.title.text = item.name

        holder.subtitle.text = when (type) {
            CollectionType.EQUIPMENT ->
                "Category: ${item.secondary}"

            CollectionType.DOGS ->
                "Breed: ${item.secondary} | Age: ${item.ageYears} years"
        }

        holder.notes.text = item.notes
        holder.notes.visibility =
            if (item.notes.isBlank()) View.GONE else View.VISIBLE

        holder.editButton.setOnClickListener {
            onEdit(item)
        }

        holder.deleteButton.setOnClickListener {
            onDelete(item)
        }
    }

    class ItemDiff : DiffUtil.ItemCallback<CollectionItem>() {

        override fun areItemsTheSame(
            oldItem: CollectionItem,
            newItem: CollectionItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: CollectionItem,
            newItem: CollectionItem
        ): Boolean = oldItem == newItem
    }
}
