package com.capstone.smartbite.ui.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.utils.HealthMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LaporanViewModel : ViewModel() {
    private val firebaseService = FirebaseService()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val userEmail = MutableStateFlow<String?>(null)
    
    // Ensure Monday is the first day of the week
    val currentWeekStart = MutableStateFlow(Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    })
    
    val selectedDate = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    })
    
    val dailyTargets = MutableStateFlow<HealthMath.DailyNutritionTargets?>(null)

    // Flow untuk data mingguan (real-time)
    val weeklyLogs = combine(userEmail, currentWeekStart) { email, weekStart ->
        email to weekStart
    }.flatMapLatest { (email, weekStart) ->
        if (email != null) {
            val startStr = dateFormatter.format(weekStart.time)
            val endCal = weekStart.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 6)
            val endStr = dateFormatter.format(endCal.time)
            firebaseService.getWeeklyLog(email, startStr, endStr)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.asLiveData()

    // Flow untuk data harian yang dipilih (real-time)
    val selectedDayLog = combine(userEmail, selectedDate) { email, date ->
        email to date
    }.flatMapLatest { (email, date) ->
        if (email != null) {
            val dateStr = dateFormatter.format(date.time)
            firebaseService.getDailyLogForDate(email, dateStr)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.asLiveData()

    fun setWeek(offset: Int) {
        val newCal = currentWeekStart.value.clone() as Calendar
        newCal.add(Calendar.DAY_OF_YEAR, offset * 7)
        currentWeekStart.value = newCal
        
        // When week changes, select the first day of that week by default
        selectDate(newCal.clone() as Calendar)
    }

    fun selectDate(date: Calendar) {
        val normalized = date.clone() as Calendar
        normalized.set(Calendar.HOUR_OF_DAY, 0)
        normalized.set(Calendar.MINUTE, 0)
        normalized.set(Calendar.SECOND, 0)
        normalized.set(Calendar.MILLISECOND, 0)
        selectedDate.value = normalized
    }

    fun setDailyTargets(user: UserModel) {
        dailyTargets.value = HealthMath.calculateDailyNutrition(user)
    }
}
