package com.capstone.smartbite.ui.laporan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.data.FoodRepository
import com.capstone.smartbite.utils.HealthMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LaporanViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseService = FirebaseService()
    private val foodRepository = FoodRepository(application)
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

    // Flow untuk data mingguan (real-time dari Room)
    val weeklyLogs = combine(userEmail, currentWeekStart) { email, weekStart ->
        email to weekStart
    }.flatMapLatest { (email, weekStart) ->
        if (email != null) {
            val startStr = dateFormatter.format(weekStart.time)
            val endCal = weekStart.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 6)
            val endStr = dateFormatter.format(endCal.time)
            
            foodRepository.getWeeklyHistory(email, startStr, endStr).map { entities ->
                entities.groupBy { it.date }.map { (date, list) ->
                    FirebaseService.DayLog(
                        date = date,
                        calories = list.sumOf { it.calories }.toInt(),
                        protein = list.sumOf { it.protein }.toInt(),
                        carbs = list.sumOf { it.carbs }.toInt(),
                        fat = list.sumOf { it.fat }.toInt()
                    )
                }.sortedBy { it.date }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.asLiveData()

    // Flow untuk data harian yang dipilih (real-time dari Room)
    val selectedDayLog = combine(userEmail, selectedDate) { email, date ->
        email to date
    }.flatMapLatest { (email, date) ->
        if (email != null) {
            val dateStr = dateFormatter.format(date.time)
            foodRepository.getDailyHistory(email, dateStr).map { list ->
                FirebaseService.DailyNutritionLog(
                    calories = list.sumOf { it.calories }.toInt(),
                    protein = list.sumOf { it.protein }.toInt(),
                    carbs = list.sumOf { it.carbs }.toInt(),
                    fat = list.sumOf { it.fat }.toInt()
                )
            }
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.asLiveData()

    // Flow untuk daftar riwayat makanan
    val foodHistory = combine(userEmail, selectedDate) { email, date ->
        email to date
    }.flatMapLatest { (email, date) ->
        if (email != null) {
            val dateStr = dateFormatter.format(date.time)
            foodRepository.getDailyHistory(email, dateStr)
                .map { list -> 
                    // Ambil 10 item terbaru
                    list.take(10) 
                }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
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
