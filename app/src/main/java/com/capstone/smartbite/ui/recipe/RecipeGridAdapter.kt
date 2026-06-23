package com.capstone.smartbite.ui.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.smartbite.R
import com.capstone.smartbite.data.MealItem
import com.capstone.smartbite.databinding.ItemRecipeGridBinding

class RecipeGridAdapter(
    private val onDetailClick: (MealItem) -> Unit,
    private val onAddClick: (MealItem) -> Unit
) : ListAdapter<MealItem, RecipeGridAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = getItem(position)
        holder.bind(meal)
    }

    inner class ViewHolder(private val binding: ItemRecipeGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(meal: MealItem) {
            binding.tvMealName.text = meal.title
            binding.tvMealCal.text = meal.calories.toString()
            binding.tvMealTag.text = meal.tag
            binding.tvMealMacros.text = binding.root.context.getString(R.string.macros_format, 
                meal.macros.protein, meal.macros.carbs, meal.macros.fat)
            
            Glide.with(binding.ivMealImg.context)
                .load(meal.imageUrl)
                .placeholder(R.drawable.logo_login)
                .into(binding.ivMealImg)

            binding.root.setOnClickListener { onDetailClick(meal) }
            binding.btnAddMeal.setOnClickListener { onAddClick(meal) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MealItem>() {
            override fun areItemsTheSame(oldItem: MealItem, newItem: MealItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: MealItem, newItem: MealItem): Boolean = oldItem == newItem
        }
    }
}
