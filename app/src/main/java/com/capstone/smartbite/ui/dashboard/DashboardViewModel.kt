package com.capstone.smartbite.ui.dashboard

import android.app.Application
import androidx.lifecycle.*
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.data.FoodRepository
import com.capstone.smartbite.data.ListFoodItem
import com.capstone.smartbite.utils.HealthMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val foodRepository = FoodRepository(application)
    private val firebaseService = FirebaseService()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _food = MutableLiveData<List<ListFoodItem>>()
    val food: LiveData<List<ListFoodItem>> = _food

    private val _dailyNutrition = MutableLiveData<HealthMath.DailyNutritionTargets>()
    val dailyNutrition: LiveData<HealthMath.DailyNutritionTargets> = _dailyNutrition

    private val userEmailFlow = MutableStateFlow<String?>(null)

    /**
     * Consumed nutrition now comes from Room (Offline-first).
     * We map the List<FoodHistoryEntity> to DailyNutritionLog.
     */
    val consumedNutrition: LiveData<FirebaseService.DailyNutritionLog?> = 
        userEmailFlow.flatMapLatest { email ->
            if (email != null) {
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                foodRepository.getDailyHistory(email, dateString).map { list ->
                    if (list.isEmpty()) {
                        // If Room is empty, we could fallback to Firebase or just return zeroed log
                        // For now, let's return a zeroed log or null
                        FirebaseService.DailyNutritionLog(0, 0, 0, 0)
                    } else {
                        FirebaseService.DailyNutritionLog(
                            calories = list.sumOf { it.calories }.toInt(),
                            protein = list.sumOf { it.protein }.toInt(),
                            carbs = list.sumOf { it.carbs }.toInt(),
                            fat = list.sumOf { it.fat }.toInt()
                        )
                    }
                }
            } else {
                kotlinx.coroutines.flow.flowOf(null)
            }
        }.asLiveData()

    val weeklyNutrition: LiveData<List<FirebaseService.DayLog>> = 
        userEmailFlow.flatMapLatest { email ->
            if (email != null) {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val endDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = dateFormat.format(calendar.time)
                
                foodRepository.getWeeklyHistory(email, startDate, endDate).map { entities ->
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

    private val _error = MutableLiveData<Boolean>()
    val isError: LiveData<Boolean> = _error

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun setUserEmail(email: String) {
        userEmailFlow.value = email
    }

    fun calculateDailyTargets(user: UserModel) {
        viewModelScope.launch {
            if (user.height > 0 && user.weight > 0) {
                _dailyNutrition.postValue(HealthMath.calculateDailyNutrition(user))
            }
        }
    }

    fun loadActiveEvents(){
        _isLoading.value = false
    }
}
