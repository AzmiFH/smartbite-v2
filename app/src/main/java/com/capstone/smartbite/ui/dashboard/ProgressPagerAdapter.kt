package com.capstone.smartbite.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.smartbite.R

class ProgressPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dailyNutrition: com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets? = null
    private var consumedNutrition: com.capstone.smartbite.data.FirebaseService.DailyNutritionLog? = null

    fun setDailyNutrition(nutrition: com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets) {
        this.dailyNutrition = nutrition
        notifyItemChanged(0)
    }

    fun setConsumedNutrition(consumed: com.capstone.smartbite.data.FirebaseService.DailyNutritionLog) {
        this.consumedNutrition = consumed
        notifyItemChanged(0)
    }

    companion object {
        private const val TYPE_DAILY = 0
        private const val TYPE_WEEKLY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_DAILY else TYPE_WEEKLY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DAILY) {
            val view = inflater.inflate(R.layout.item_daily_progress, parent, false)
            DailyViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_weekly_progress, parent, false)
            WeeklyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DailyViewHolder) {
            holder.bind(dailyNutrition, consumedNutrition)
        }
    }

    override fun getItemCount(): Int = 2

    class DailyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = com.capstone.smartbite.databinding.ItemDailyProgressBinding.bind(view)

        fun bind(target: com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets?, consumed: com.capstone.smartbite.data.FirebaseService.DailyNutritionLog?) {
            val targetCal = target?.calories ?: 2000
            val targetProt = target?.protein ?: 50
            val targetCarb = target?.carbs ?: 250
            val targetFat = target?.fat ?: 65

            val consCal = consumed?.calories ?: 0
            val consProt = consumed?.protein ?: 0
            val consCarb = consumed?.carbs ?: 0
            val consFat = consumed?.fat ?: 0

            binding.tvCalVal.text = "$consCal / $targetCal"
            binding.tvProtVal.text = "$consProt / $targetProt g"
            binding.tvCarbVal.text = "$consCarb / $targetCarb g"
            binding.tvFatVal.text = "$consFat / $targetFat g"
            
            val progress = if (targetCal > 0) (consCal.toFloat() / targetCal * 100).toInt() else 0
            binding.progressCalCircle.progress = progress
        }
    }
    class WeeklyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
