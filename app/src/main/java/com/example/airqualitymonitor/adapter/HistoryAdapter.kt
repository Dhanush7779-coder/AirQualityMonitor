package com.example.airqualitymonitor.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.airqualitymonitor.data.AirQualityEntity
import com.example.airqualitymonitor.databinding.ItemHistoryBinding
import com.example.airqualitymonitor.util.AqiUtils

class HistoryAdapter(
    private var items: List<AirQualityEntity> = emptyList(),
    private val onDeleteClick: (AirQualityEntity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    fun updateData(newItems: List<AirQualityEntity>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position], onDeleteClick)
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: AirQualityEntity,
            onDeleteClick: (AirQualityEntity) -> Unit
        ) {
            val context = itemView.context
            val category = AqiUtils.getCategory(item.aqi)
            val categoryColor = ContextCompat.getColor(context, category.colorRes)

            binding.tvHistoryAqiBadge.text = item.aqi.toString()
            binding.tvHistoryAqiBadge.backgroundTintList = ColorStateList.valueOf(categoryColor)

            binding.tvHistoryCategory.text = item.category
            binding.tvHistoryLocation.text = item.location
            binding.tvHistoryDateTime.text = item.dateTime

            binding.tvHistoryPm25.text = "PM2.5: ${item.pm25} μg/m³"
            binding.tvHistoryPm10.text = "PM10: ${item.pm10} μg/m³"
            binding.tvHistoryCo.text = "CO: ${item.carbonMonoxide} μg/m³"

            binding.btnDeleteHistory.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
}
