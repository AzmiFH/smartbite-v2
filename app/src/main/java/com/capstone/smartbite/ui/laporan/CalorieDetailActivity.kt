package com.capstone.smartbite.ui.laporan

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.capstone.smartbite.R
import com.capstone.smartbite.databinding.ActivityCalorieDetailBinding
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.data.FoodRepository
import com.google.firebase.auth.FirebaseAuth
import com.capstone.smartbite.utils.HealthMath
import com.capstone.smartbite.UserPreference
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalorieDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalorieDetailBinding
    private var calendarDay = Calendar.getInstance()
    private var calendarWeek = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    private var calendarMonth = Calendar.getInstance()
    private var selectedTab = "Hari" // Hari, Minggu, Bulan
    private lateinit var foodRepository: FoodRepository
    private val auth = FirebaseAuth.getInstance()
    private var dataJob: Job? = null
    private var targetCalories: Int = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalorieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        foodRepository = FoodRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadUserTargets()
        setupToolbar()
        setupListeners()
        updateUI()
    }

    private fun loadUserTargets() {
        val email = auth.currentUser?.email
        if (email != null) {
            val userPreference = UserPreference(this, email)
            val userModel = userPreference.getUser()
            if (userModel.height > 0 && userModel.weight > 0) {
                targetCalories = HealthMath.calculateDailyNutrition(userModel).calories
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.ivPrev.setOnClickListener {
            val cal = getActiveCalendar()
            val offset = if (selectedTab == "Hari") -1 else if (selectedTab == "Minggu") -7 else -30
            cal.add(Calendar.DAY_OF_YEAR, offset)
            updateUI()
        }

        binding.ivNext.setOnClickListener {
            val cal = getActiveCalendar()
            val offset = if (selectedTab == "Hari") 1 else if (selectedTab == "Minggu") 7 else 30
            cal.add(Calendar.DAY_OF_YEAR, offset)
            updateUI()
        }

        binding.tvTabHari.setOnClickListener { selectTab("Hari") }
        binding.tvTabMinggu.setOnClickListener { selectTab("Minggu") }
        binding.tvTabBulan.setOnClickListener { selectTab("Bulan") }
    }

    private fun selectTab(tab: String) {
        selectedTab = tab
        
        // Reset Style Semua Tab
        val tabs = listOf(binding.tvTabHari, binding.tvTabMinggu, binding.tvTabBulan)
        tabs.forEach { 
            it.background = null
            it.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            it.typeface = Typeface.DEFAULT
        }

        // Aktifkan Tab Terpilih
        val activeTab = when(tab) {
            "Hari" -> binding.tvTabHari
            "Minggu" -> binding.tvTabMinggu
            else -> binding.tvTabBulan
        }
        activeTab.setBackgroundResource(R.drawable.bg_tab_selected)
        activeTab.setTextColor(ContextCompat.getColor(this, R.color.text_black_bold))
        activeTab.typeface = Typeface.DEFAULT_BOLD

        updateUI()
    }

    private fun updateUI() {
        updateDateDisplay()
        updateChart()
        
        if (selectedTab == "Hari") {
            binding.tvLabelStat.text = "Hari Ini: "
            binding.tvUnitStat.text = "kcal"
        } else {
            binding.tvLabelStat.text = "Rata-rata harian: "
            binding.tvUnitStat.text = "kkal"
            binding.tvStatVal.text = "50" // Default/Placeholder for Minggu/Bulan
        }
    }

    private fun updateDateDisplay() {
        if (selectedTab == "Hari") {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            binding.tvSelectedRange.text = sdf.format(calendarDay.time)
        } else if (selectedTab == "Minggu") {
            val start = calendarWeek.clone() as Calendar
            val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_YEAR, 6)
            binding.tvSelectedRange.text = "${fmt.format(start.time)} - ${fmt.format(end.time)}"
        } else {
            val end = calendarMonth.clone() as Calendar
            val start = end.clone() as Calendar
            start.add(Calendar.DAY_OF_YEAR, -29)
            val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
            binding.tvSelectedRange.text = "${fmt.format(start.time)} - ${fmt.format(end.time)}"
        }
    }

    private fun updateChart() {
        binding.llBarsContainer.removeAllViews()
        binding.llLabelsContainer.removeAllViews()
        dataJob?.cancel()

        if (selectedTab == "Hari") {
            observeHourlyData()
        } else if (selectedTab == "Minggu") {
            observeWeeklyData()
        } else {
            observeMonthlyData()
        }
    }

    private fun observeHourlyData() {
        val email = auth.currentUser?.email ?: return
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarDay.time)

        dataJob = lifecycleScope.launch {
            foodRepository.getDailyHistory(email, dateString).collectLatest { entries ->
                val hourlyCalories = DoubleArray(24) { 0.0 }
                val cal = Calendar.getInstance()
                var totalCalories = 0.0
                
                entries.forEach { entry ->
                    cal.timeInMillis = entry.timestamp
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    if (hour in 0..23) {
                        hourlyCalories[hour] += entry.calories
                    }
                    totalCalories += entry.calories
                }
                
                binding.tvStatVal.text = totalCalories.toInt().toString()
                setupHourlyChart(hourlyCalories)
            }
        }
    }

    private fun observeWeeklyData() {
        val email = auth.currentUser?.email ?: return
        
        val start = calendarWeek.clone() as Calendar
        start.firstDayOfWeek = Calendar.MONDAY
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdf.format(start.time)
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_YEAR, 6)
        val endStr = sdf.format(end.time)

        dataJob = lifecycleScope.launch {
            foodRepository.getWeeklyHistory(email, startStr, endStr).collectLatest { entries ->
                val logMap = entries.groupBy { it.date }
                val weeklyCalories = IntArray(7) { 0 }
                val currentWeekStart = start.clone() as Calendar
                
                var totalCalories = 0
                var daysWithData = 0
                
                for (i in 0..6) {
                    val dateStr = sdf.format(currentWeekStart.time)
                    val dayEntries = logMap[dateStr]
                    weeklyCalories[i] = dayEntries?.sumOf { it.calories }?.toInt() ?: 0
                    totalCalories += weeklyCalories[i]
                    if (weeklyCalories[i] > 0) daysWithData++
                    currentWeekStart.add(Calendar.DAY_OF_YEAR, 1)
                }

                val avgCal = if (daysWithData > 0) totalCalories / daysWithData else 0
                binding.tvStatVal.text = avgCal.toString()
                setupWeeklyChart(weeklyCalories)
            }
        }
    }

    private fun observeMonthlyData() {
        val email = auth.currentUser?.email ?: return
        
        val start = calendarMonth.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -29)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdf.format(start.time)
        val end = calendarMonth.clone() as Calendar
        val endStr = sdf.format(end.time)

        dataJob = lifecycleScope.launch {
            foodRepository.getWeeklyHistory(email, startStr, endStr).collectLatest { entries ->
                val logMap = entries.groupBy { it.date }
                val monthlyCalories = IntArray(30) { 0 }
                val currentStart = start.clone() as Calendar
                
                var totalCalories = 0
                var daysWithData = 0
                
                for (i in 0..29) {
                    val dateStr = sdf.format(currentStart.time)
                    val dayEntries = logMap[dateStr]
                    monthlyCalories[i] = dayEntries?.sumOf { it.calories }?.toInt() ?: 0
                    totalCalories += monthlyCalories[i]
                    if (monthlyCalories[i] > 0) daysWithData++
                    currentStart.add(Calendar.DAY_OF_YEAR, 1)
                }

                val avgCal = if (daysWithData > 0) totalCalories / daysWithData else 0
                binding.tvStatVal.text = avgCal.toString()
                setupMonthlyChart(monthlyCalories)
            }
        }
    }

    private fun setupHourlyChart(hourlyCalories: DoubleArray) {
        binding.llBarsContainer.removeAllViews()
        binding.llLabelsContainer.removeAllViews()

        binding.vLineMax.background = null
        binding.vLineMax.setBackgroundColor(ContextCompat.getColor(this, R.color.background_light))
        binding.tvYMax.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        val maxInDay = (hourlyCalories.maxOrNull() ?: 100.0).coerceAtLeast(100.0)
        binding.tvYMax.text = maxInDay.toInt().toString()
        binding.tvY4.text = (maxInDay * 0.8).toInt().toString()
        binding.tvY3.text = (maxInDay * 0.6).toInt().toString()
        binding.tvY2.text = (maxInDay * 0.4).toInt().toString()
        binding.tvY1.text = (maxInDay * 0.2).toInt().toString()

        // Horizontal axis labels (00:00, 06:00, 12:00, 18:00, 24:00)
        val timeLabels = listOf("00:00", "06:00", "12:00", "18:00", "24:00")
        timeLabels.forEachIndexed { index, label ->
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                gravity = when(index) {
                    0 -> Gravity.START
                    timeLabels.size - 1 -> Gravity.END
                    else -> Gravity.CENTER
                }
            }
            binding.llLabelsContainer.addView(tv)
        }

        // Render 24 thin bars
        hourlyCalories.forEach { calories ->
            addBar(calories.toInt(), false, 6, maxInDay.toInt())
        }
    }

    private fun setupWeeklyChart(weeklyCalories: IntArray) {
        binding.llBarsContainer.removeAllViews()
        binding.llLabelsContainer.removeAllViews()

        binding.vLineMax.background = null
        binding.vLineMax.setBackgroundColor(ContextCompat.getColor(this, R.color.background_light))
        binding.tvYMax.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        // Set Y axis based on target or max found
        val maxInWeek = (weeklyCalories.maxOrNull() ?: targetCalories).coerceAtLeast(targetCalories)
        binding.tvYMax.text = maxInWeek.toString()
        binding.tvY4.text = (maxInWeek * 0.8).toInt().toString()
        binding.tvY3.text = (maxInWeek * 0.6).toInt().toString()
        binding.tvY2.text = (maxInWeek * 0.4).toInt().toString()
        binding.tvY1.text = (maxInWeek * 0.2).toInt().toString()

        val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        days.forEach { label ->
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                gravity = Gravity.CENTER
            }
            binding.llLabelsContainer.addView(tv)
        }

        weeklyCalories.forEach { calories ->
            addBar(calories, true, 24, maxInWeek)
        }
    }

    private fun setupMonthlyChart(monthlyCalories: IntArray) {
        binding.vLineMax.background = ContextCompat.getDrawable(this, R.drawable.bg_dashed_line_limit)
        
        // Find max in month or use target
        val maxInMonth = (monthlyCalories.maxOrNull() ?: targetCalories).coerceAtLeast(targetCalories)
        binding.tvYMax.text = maxInMonth.toString()
        binding.tvYMax.setTextColor(Color.parseColor("#C5E1A5"))
        
        binding.tvY4.text = (maxInMonth * 0.8).toInt().toString()
        binding.tvY3.text = (maxInMonth * 0.6).toInt().toString()
        binding.tvY2.text = (maxInMonth * 0.4).toInt().toString()
        binding.tvY1.text = (maxInMonth * 0.2).toInt().toString()

        val cal = calendarMonth.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -29)
        
        // Render 30 bars
        monthlyCalories.forEachIndexed { index, calories ->
            addBar(calories, index % 5 == 0, 6, maxInMonth)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Render 6 labels (each spanning 5 bars)
        val sdf = SimpleDateFormat("d/M", Locale.getDefault())
        val labelCal = calendarMonth.clone() as Calendar
        labelCal.add(Calendar.DAY_OF_YEAR, -29)
        
        for (i in 0 until 6) {
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, -2, 5f)
                text = sdf.format(labelCal.time)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                gravity = Gravity.CENTER
                maxLines = 1
            }
            binding.llLabelsContainer.addView(tv)
            labelCal.add(Calendar.DAY_OF_YEAR, 5)
        }
    }

    private fun addBar(heightValue: Int, showDashedLine: Boolean, barWidthDp: Int, maxValue: Int) {
        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }

        if (showDashedLine) {
            val dashedLine = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(1), -1).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                background = ContextCompat.getDrawable(context, R.drawable.bg_dashed_line_vertical)
            }
            container.addView(dashedLine)
        }

        val barHeightRatio = if (maxValue > 0) heightValue.toFloat() / maxValue else 0f
        val maxHeightPx = dpToPx(180)
        val calculatedHeight = (barHeightRatio * maxHeightPx).toInt().coerceAtLeast(if (heightValue > 0) dpToPx(4) else 0)

        val bar = View(this).apply {
            val p = FrameLayout.LayoutParams(dpToPx(barWidthDp), calculatedHeight)
            p.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams = p
            background = ContextCompat.getDrawable(context, R.drawable.bg_bar_chart)
            
            // Accuracy-based Color Coding
            val color = if (selectedTab == "Hari") {
                R.color.brand_green // Consistent color for hourly bars
            } else {
                val ratioToTarget = if (targetCalories > 0) heightValue.toFloat() / targetCalories else 0f
                when {
                    heightValue == 0 -> R.color.brand_green
                    ratioToTarget < 0.8f -> R.color.progress_carbs // Yellow (Too low)
                    ratioToTarget <= 1.1f -> R.color.brand_green // Green (Target met)
                    else -> R.color.progress_cal // Red (Exceeded)
                }
            }
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
        }
        
        container.addView(bar)
        binding.llBarsContainer.addView(container)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getActiveCalendar(): Calendar {
        return when (selectedTab) {
            "Hari" -> calendarDay
            "Minggu" -> calendarWeek
            else -> calendarMonth
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dataJob?.cancel()
    }
}
