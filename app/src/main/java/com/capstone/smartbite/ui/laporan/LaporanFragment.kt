package com.capstone.smartbite.ui.laporan

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.databinding.FragmentLaporanBinding
import com.capstone.smartbite.databinding.ItemCalendarDayBinding
import com.capstone.smartbite.ui.history.adapter.HistoryAdapter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LaporanViewModel
    private lateinit var historyAdapter: HistoryAdapter

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[LaporanViewModel::class.java]

        clearUI()
        setupRecyclerView()
        setupListeners()
        setupObservers()
        
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email != null) {
            viewModel.userEmail.value = email
            val userPref = UserPreference(requireContext(), email)
            viewModel.setDailyTargets(userPref.getUser())
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false // Prevent scroll conflicts with parent ScrollView
        }
    }

    private fun setupObservers() {
        // Observe Current Week Start for Calendar Header
        lifecycleScope.launchWhenStarted {
            viewModel.currentWeekStart.collect { weekStart ->
                updateCalendarHeader(weekStart)
            }
        }

        // Observe Weekly Logs for Calendar Items & Chart
        viewModel.weeklyLogs.observe(viewLifecycleOwner) { logs ->
            updateCalendarDays(logs)
            updateCharts(logs)
        }

        // Observe Selected Day Log for Main UI
        viewModel.selectedDayLog.observe(viewLifecycleOwner) { log ->
            updateMainUI(log)
        }

        // Observe Food History from Room
        viewModel.foodHistory.observe(viewLifecycleOwner) { history ->
            if (history.isNullOrEmpty()) {
                binding.rvHistory.visibility = View.GONE
                binding.tvHistoryEmpty.visibility = View.VISIBLE
            } else {
                binding.rvHistory.visibility = View.VISIBLE
                binding.tvHistoryEmpty.visibility = View.GONE
                historyAdapter.submitList(history)
            }
        }
        
        // Observe Targets
        lifecycleScope.launchWhenStarted {
            viewModel.dailyTargets.collect { _ ->
                // Refresh UI with targets
                updateMainUI(viewModel.selectedDayLog.value)
                updateCharts(viewModel.weeklyLogs.value ?: emptyList())
            }
        }
        
        // Observe Selection
        lifecycleScope.launchWhenStarted {
            viewModel.selectedDate.collect {
                // Trigger calendar day refresh for selection UI
                updateCalendarDays(viewModel.weeklyLogs.value ?: emptyList())
            }
        }
    }

    private fun updateCalendarHeader(weekStart: Calendar) {
        val endCalendar = weekStart.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_YEAR, 6)
        
        val langCode = com.capstone.smartbite.UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email).getLanguage()
        val locale = Locale(langCode)
        
        // Use SimpleDateFormat but ensure it respects the selected locale from preferences
        val monthFormatter = SimpleDateFormat("d MMM", locale)
        binding.tvCurrentMonthRange.text = "${monthFormatter.format(weekStart.time)} - ${monthFormatter.format(endCalendar.time)}"
    }

    private fun updateCalendarDays(logs: List<FirebaseService.DayLog>) {
        val logMap = logs.associateBy { it.date }
        val weekStart = viewModel.currentWeekStart.value.clone() as Calendar
        val selectedDateStr = dateFormatter.format(viewModel.selectedDate.value.time)
        val targets = viewModel.dailyTargets.value

        val dayViews = listOf(
            binding.day1, binding.day2, binding.day3,
            binding.day4, binding.day5, binding.day6, binding.day7
        )
        
        val langCode = com.capstone.smartbite.UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email).getLanguage()
        val locale = Locale(langCode)
        
        val dayNameFormatter = SimpleDateFormat("EEE", locale)

        for (i in 0..6) {
            val dateStr = dateFormatter.format(weekStart.time)
            val dayBinding = ItemCalendarDayBinding.bind(dayViews[i].root)
            
            dayBinding.tvDayName.text = dayNameFormatter.format(weekStart.time)
            dayBinding.tvDayDate.text = weekStart.get(Calendar.DAY_OF_MONTH).toString()

            // Selection UI
            if (dateStr == selectedDateStr) {
                dayBinding.tvDayDate.setBackgroundResource(R.drawable.bg_circle_primary)
                dayBinding.tvDayDate.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.ring_orange)
                dayBinding.tvDayDate.setTextColor(Color.WHITE)
            } else {
                dayBinding.tvDayDate.background = null
                dayBinding.tvDayDate.setTextColor(Color.BLACK)
            }

            // Mini Rings Progress
            val log = logMap[dateStr]
            if (log != null && targets != null) {
                dayBinding.cpMiniOuter.progress = (log.protein.toFloat() / targets.protein * 100).toInt().coerceIn(0, 100)
                dayBinding.cpMiniMiddle.progress = (log.carbs.toFloat() / targets.carbs * 100).toInt().coerceIn(0, 100)
                dayBinding.cpMiniInner.progress = (log.fat.toFloat() / targets.fat * 100).toInt().coerceIn(0, 100)
            } else {
                dayBinding.cpMiniOuter.progress = 0
                dayBinding.cpMiniMiddle.progress = 0
                dayBinding.cpMiniInner.progress = 0
            }

            val dateToSelect = weekStart.clone() as Calendar
            dayViews[i].root.setOnClickListener {
                viewModel.selectDate(dateToSelect)
            }

            weekStart.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun clearUI() {
        binding.tvConsumedCalBig.text = "0"
        binding.tvTargetCalLabel.text = ""
        binding.tvCarbVal.text = "0"
        binding.tvProteinVal.text = "0"
        binding.tvFatVal.text = "0"
        
        binding.cpRingOuter.progress = 0
        binding.cpRingMiddle.progress = 0
        binding.cpRingInner.progress = 0
        binding.pbLaporanCarbs.progress = 0
        binding.pbLaporanProtein.progress = 0
        binding.pbLaporanFat.progress = 0
    }

    private fun setupListeners() {
        binding.cvCaloriesChart.setOnClickListener {
            val intent = Intent(requireContext(), CalorieDetailActivity::class.java)
            startActivity(intent)
        }

        binding.ivPrevWeek.setOnClickListener {
            viewModel.setWeek(-1)
        }

        binding.ivNextWeek.setOnClickListener {
            viewModel.setWeek(1)
        }
    }

    private fun updateCharts(logs: List<FirebaseService.DayLog>) {
        val logMap = logs.associateBy { it.date }
        val weekStart = viewModel.currentWeekStart.value.clone() as Calendar
        val dailyTarget = viewModel.dailyTargets.value?.calories ?: 2000
        
        // Find max in current logs to scale Y axis if any day exceeds target
        val maxActual = logs.maxOfOrNull { it.calories } ?: 0
        val chartMax = maxOf(dailyTarget, maxActual).toFloat()

        // Update Y Axis Labels
        binding.tvY200.text = chartMax.toInt().toString()
        binding.tvY160.text = (chartMax * 0.8).toInt().toString()
        binding.tvY120.text = (chartMax * 0.6).toInt().toString()
        binding.tvY80.text = (chartMax * 0.4).toInt().toString()
        binding.tvY40.text = (chartMax * 0.2).toInt().toString()
        binding.tvY0.text = "0"

        val barViews = listOf(
            binding.barSen, binding.barSel, binding.barRab,
            binding.barKam, binding.barJum, binding.barSab, binding.barMin
        )
        
        val langCode = com.capstone.smartbite.UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email).getLanguage()
        val locale = Locale(langCode)
        
        val dayNameFormatter = SimpleDateFormat("EEE", locale)
        val labelViews = (binding.llDaysLabels as ViewGroup)

        var totalCal = 0

        for (i in 0..6) {
            val dateStr = dateFormatter.format(weekStart.time)
            val log = logMap[dateStr]
            val bar = barViews[i]
            val label = labelViews.getChildAt(i) as TextView

            label.text = dayNameFormatter.format(weekStart.time)
            
            val cal = log?.calories ?: 0
            totalCal += cal

            val params = bar.layoutParams
            val ratio = if (chartMax > 0) cal.toFloat() / chartMax else 0f
            val maxHeightPx = dpToPx(180)
            params.height = (ratio * maxHeightPx).toInt()
            
            if (cal > 0 && params.height < dpToPx(4)) {
                params.height = dpToPx(4)
            }
            
            bar.layoutParams = params

            weekStart.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Precise average calculation
        val today = Calendar.getInstance()
        val weekStartForCompare = viewModel.currentWeekStart.value
        
        val divisor: Int = if (today.after(weekStartForCompare)) {
            val diff = today.timeInMillis - weekStartForCompare.timeInMillis
            val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            if (days < 7) days else 7
        } else {
            1
        }

        val avgCal = if (divisor > 0) Math.round(totalCal.toFloat() / divisor) else 0
        binding.tvAverageCalVal.text = getString(R.string.daily_average_format, avgCal)
    }

    private fun updateMainUI(log: FirebaseService.DailyNutritionLog?) {
        val targets = viewModel.dailyTargets.value ?: return
        
        // Progress Rings
        binding.cpRingOuter.progress = log?.let { (it.protein.toFloat() / targets.protein * 100).toInt().coerceIn(0, 100) } ?: 0
        binding.cpRingMiddle.progress = log?.let { (it.carbs.toFloat() / targets.carbs * 100).toInt().coerceIn(0, 100) } ?: 0
        binding.cpRingInner.progress = log?.let { (it.fat.toFloat() / targets.fat * 100).toInt().coerceIn(0, 100) } ?: 0

        binding.tvConsumedCalBig.text = log?.calories?.toString() ?: "0"
        binding.tvTargetCalLabel.text = getString(R.string.target_cal_format, targets.calories)

        // Macros (No Target)
        binding.pbLaporanCarbs.progress = log?.let { (it.carbs.toFloat() / targets.carbs * 100).toInt().coerceIn(0, 100) } ?: 0
        binding.tvCarbVal.text = log?.carbs?.toString() ?: "0"

        binding.pbLaporanProtein.progress = log?.let { (it.protein.toFloat() / targets.protein * 100).toInt().coerceIn(0, 100) } ?: 0
        binding.tvProteinVal.text = log?.protein?.toString() ?: "0"

        binding.pbLaporanFat.progress = log?.let { (it.fat.toFloat() / targets.fat * 100).toInt().coerceIn(0, 100) } ?: 0
        binding.tvFatVal.text = log?.fat?.toString() ?: "0"
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
