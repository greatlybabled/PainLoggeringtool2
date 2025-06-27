package com.example.painlogger.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.painlogger.DetailedPainEntry
import com.example.painlogger.databinding.ItemBodyPartBinding

class BodyPartDetailedAdapter(private val bodyParts: List<String>) :
    RecyclerView.Adapter<BodyPartDetailedAdapter.BodyPartViewHolder>() {

    private val detailedEntries = mutableListOf<DetailedPainEntry>()
    var onIntensityChanged: ((List<DetailedPainEntry>) -> Unit)? = null

    inner class BodyPartViewHolder(val binding: ItemBodyPartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bodyPart: String) {
            val entry = detailedEntries.find { it.bodyPart == bodyPart }

            binding.apply {
                tvBodyPartName.text = bodyPart
                tvPainIntensity.text = entry?.intensity?.toString() ?: "0"

                tvPainIntensity.setOnClickListener {
                    val current = tvPainIntensity.text.toString().toIntOrNull() ?: 0
                    val newValue = if (current == 10) 0 else current + 1 // More readable cycle

                    tvPainIntensity.text = newValue.toString()
                    updateEntries(bodyPart, newValue)
                    onIntensityChanged?.invoke(detailedEntries)
                }
            }
        }

        private fun updateEntries(bodyPart: String, newValue: Int) {
            val existing = detailedEntries.find { it.bodyPart == bodyPart }

            when {
                existing?.intensity == newValue -> return
                existing != null -> existing.intensity = newValue
                else -> detailedEntries.add(DetailedPainEntry(bodyPart, newValue))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BodyPartViewHolder {
        val binding = ItemBodyPartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BodyPartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BodyPartViewHolder, position: Int) {
        holder.bind(bodyParts[position])
    }

    override fun getItemCount() = bodyParts.size

    fun updateEntries(newEntries: List<DetailedPainEntry>) {
        detailedEntries.clear()
        detailedEntries.addAll(newEntries)
        notifyDataSetChanged()
    }
}