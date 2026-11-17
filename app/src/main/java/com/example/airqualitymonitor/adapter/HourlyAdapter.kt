package com.example.airqualitymonitor.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.airqualitymonitor.databinding.ItemHourlyBinding
import com.example.airqualitymonitor.network.HourlyForecastItem
import com.example.airqualitymonitor.util.AqiUtils

class HourlyAdapter(
    private var items: List<HourlyForecastItem> = emptyList()
) : RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>() {

    fun updateData(newItems: List<HourlyForecastItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding = ItemHourlyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HourlyViewHolder(
        private val binding: ItemHourlyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyForecastItem) {
            val context = itemView.context
            
            // Format time: "2026-11-17T14:00" -> "14:00"
            val displayTime = if (item.time.contains("T")) {
                item.time.substringAfter("T")
            } else {
                item.time
            }
            binding.tvHourlyTime.text = displayTime
            binding.tvHourlyAqiBadge.text = item.aqi.toString()

            val category = AqiUtils.getCategory(item.aqi)
            val categoryColor = ContextCompat.getColor(context, category.colorRes)
            binding.tvHourlyAqiBadge.backgroundTintList = ColorStateList.valueOf(categoryColor)
            binding.tvHourlyCategory.text = category.title
        }
    }
}
