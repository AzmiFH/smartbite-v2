package com.capstone.smartbite.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.smartbite.R

class ProgressPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dailyNutrition: com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets? = null
    private var consumedNutrition: com.capstone.smartbite.data.FirebaseService.DailyNutritionLog? = null
    private var weeklyNutrition: List<com.capstone.smartbite.data.FirebaseService.DayLog>? = null

    fun setDailyNutrition(nutrition: com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets) {
        if (this.dailyNutrition == nutrition) return // Hindari update jika data sama
        this.dailyNutrition = nutrition
        notifyItemChanged(0)
    }

    fun setConsumedNutrition(consumed: com.capstone.smartbite.data.FirebaseService.DailyNutritionLog) {
        if (this.consumedNutrition == consumed) return // Hindari update jika data sama
        this.consumedNutrition = consumed
        notifyItemChanged(0)
    }

    fun setWeeklyNutrition(weekly: List<com.capstone.smartbite.data.FirebaseService.DayLog>) {
        if (this.weeklyNutrition == weekly) return
        this.weeklyNutrition = weekly
        notifyItemChanged(1)
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
        } else if (holder is WeeklyViewHolder) {
            holder.bind(weeklyNutrition, dailyNutrition?.calories ?: 2000)
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

            // Update Linear Progress Indicators
            binding.pbProtein.progress = if (targetProt > 0) (consProt.toFloat() / targetProt * 100).toInt() else 0
            binding.pbCarbs.progress = if (targetCarb > 0) (consCarb.toFloat() / targetCarb * 100).toInt() else 0
            binding.pbFat.progress = if (targetFat > 0) (consFat.toFloat() / targetFat * 100).toInt() else 0
        }
    }

    class WeeklyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = com.capstone.smartbite.databinding.ItemWeeklyProgressBinding.bind(view)

        fun bind(weeklyData: List<com.capstone.smartbite.data.FirebaseService.DayLog>?, targetCal: Int) {
            if (weeklyData == null || weeklyData.isEmpty()) return

            val totalCal = weeklyData.sumOf { it.calories }
            binding.tvTotalWeeklyCal.text = String.format(java.util.Locale.getDefault(), "%,d", totalCal)

            // Map data to bars based on day of week
            val bars = listOf<View>(
                binding.barMon, binding.barTue, binding.barWed,
                binding.barThu, binding.barFri, binding.barSat, binding.barSun
            )

            // Reset all bars first to 0 height
            bars.forEach { bar ->
                val params = bar.layoutParams
                params.height = 0
                bar.layoutParams = params
            }

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()

            weeklyData.forEach { log ->
                try {
                    val date = sdf.parse(log.date)
                    if (date != null) {
                        cal.time = date
                        val dayIndex = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                            java.util.Calendar.MONDAY -> 0
                            java.util.Calendar.TUESDAY -> 1
                            java.util.Calendar.WEDNESDAY -> 2
                            java.util.Calendar.THURSDAY -> 3
                            java.util.Calendar.FRIDAY -> 4
                            java.util.Calendar.SATURDAY -> 5
                            java.util.Calendar.SUNDAY -> 6
                            else -> -1
                        }

                        if (dayIndex != -1) {
                            val bar = bars[dayIndex]
                            val params = bar.layoutParams
                            val density = bar.context.resources.displayMetrics.density
                            val maxHeightPx = 80 * density // Use 80dp as target 100% height for professionalism
                            
                            // Scale based on target calorie, cap at 120% of maxHeight
                            val ratio = log.calories.toFloat() / targetCal.coerceAtLeast(1).toFloat()
                            params.height = (ratio.coerceAtMost(1.2f) * maxHeightPx).toInt()
                            
                            bar.layoutParams = params
                            
                            // Highlight if exceeded target
                            if (ratio > 1.0f) {
                                bar.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                    androidx.core.content.ContextCompat.getColor(bar.context, R.color.progress_cal)
                                )
                            } else {
                                bar.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                    androidx.core.content.ContextCompat.getColor(bar.context, R.color.brand_green)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
