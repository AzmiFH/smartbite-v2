package com.capstone.smartbite.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.data.ListFoodItem
import com.capstone.smartbite.data.RecomenResponse
import com.capstone.smartbite.utils.HealthMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _food = MutableLiveData<List<ListFoodItem>>()
    val food: LiveData<List<ListFoodItem>> = _food

    private val _dailyNutrition = MutableLiveData<HealthMath.DailyNutritionTargets>()
    val dailyNutrition: LiveData<HealthMath.DailyNutritionTargets> = _dailyNutrition

    private val userEmailFlow = MutableStateFlow<String?>(null)

    val consumedNutrition: LiveData<FirebaseService.DailyNutritionLog?> = 
        userEmailFlow.flatMapLatest { email ->
            if (email != null) {
                FirebaseService().getDailyLog(email)
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
                FirebaseService().getWeeklyLog(email, startDate, endDate)
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
        // Off kan sementara untuk menghindari network timeout/lag (ConnectException)
        /*
        _isLoading.value = true
        val clinet = ApiConfig.getApiService().getcalorie(calorie = 50)
        clinet.enqueue(object : Callback<RecomenResponse> {
            override fun onResponse(
                call: Call<RecomenResponse>,
                response: Response<RecomenResponse>
            ) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _food.value = response.body()?.listFood
                }else {
                    _error.value = true
                    _message.value = "Failed to load data"
                }
            }

            override fun onFailure(call: Call<RecomenResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = true
                _message.value = t.message ?: "Unknown error"
            }

        })
        */
        _isLoading.value = false
    }
}