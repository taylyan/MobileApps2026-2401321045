
package com.huntersstory.app.ui.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.huntersstory.app.R
import com.huntersstory.app.data.local.entity.HuntingTrip
import android.widget.Button

class HuntingTripAdapter(
    private val onDeleteClick: (HuntingTrip) -> Unit
) : ListAdapter<HuntingTrip, HuntingTripAdapter.TripViewHolder>(
    TripDiffCallback()
) {

    class TripViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val species: TextView =
            itemView.findViewById(R.id.tvTripSpecies)

        private val date: TextView =
            itemView.findViewById(R.id.tvTripDate)

        private val location: TextView =
            itemView.findViewById(R.id.tvTripLocation)

        private val notes: TextView =
            itemView.findViewById(R.id.tvTripNotes)

        private val deleteButton: Button =
            itemView.findViewById(R.id.btnDeleteTrip)


        fun bind(
            trip: HuntingTrip,
            onDeleteClick: (HuntingTrip) -> Unit
        ) {
            species.text = trip.gameSpecies
            date.text = trip.date
            location.text = trip.location
            notes.text = trip.notes

            notes.visibility =
                if (trip.notes.isBlank()) View.GONE else View.VISIBLE

            deleteButton.setOnClickListener {
                onDeleteClick(trip)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hunting_trip, parent, false)

        return TripViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TripViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position), onDeleteClick)
    }

    class TripDiffCallback : DiffUtil.ItemCallback<HuntingTrip>() {

        override fun areItemsTheSame(
            oldItem: HuntingTrip,
            newItem: HuntingTrip
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: HuntingTrip,
            newItem: HuntingTrip
        ): Boolean {
            return oldItem == newItem
        }
    }
}
