package com.capstone.smartbite.ui.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.smartbite.R
import com.capstone.smartbite.data.local.FoodHistoryEntity
import com.capstone.smartbite.databinding.ItemMealHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<FoodHistoryEntity, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemMealHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(private val binding: ItemMealHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FoodHistoryEntity) {
            binding.tvFoodName.text = item.foodName
            binding.tvCalories.text = "${item.calories.toInt()} kcal"
            
            val displayQty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            binding.tvQuantity.text = "$displayQty ${item.unit}"
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvTime.text = timeFormat.format(Date(item.timestamp))
            
            binding.tvProteinVal.text = "${item.protein.toInt()}g P"
            binding.tvCarbsVal.text = "${item.carbs.toInt()}g C"
            binding.tvFatVal.text = "${item.fat.toInt()}g F"

            if (item.imageUrl.isNullOrEmpty()) {
                binding.ivFoodHistory.setImageResource(R.drawable.ic_food_fork_knife)
                binding.ivFoodHistory.setColorFilter(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.brand_green))
            } else {
                binding.ivFoodHistory.clearColorFilter()
                Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.th)
                    .error(R.drawable.ic_food_fork_knife)
                    .into(binding.ivFoodHistory)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FoodHistoryEntity>() {
            override fun areItemsTheSame(oldItem: FoodHistoryEntity, newItem: FoodHistoryEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FoodHistoryEntity, newItem: FoodHistoryEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
