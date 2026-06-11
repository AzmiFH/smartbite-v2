package com.capstone.smartbite.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.capstone.smartbite.data.ApiConfig
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.data.ListFoodItem
import com.capstone.smartbite.data.RecomenResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardViewModel : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _food = MutableLiveData<List<ListFoodItem>>()
    val food: LiveData<List<ListFoodItem>> = _food

    private val _dailyNutrition = MutableLiveData<com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets>()
    val dailyNutrition: LiveData<com.capstone.smartbite.utils.HealthMath.DailyNutritionTargets> = _dailyNutrition

    private val _consumedNutrition = MutableLiveData<FirebaseService.DailyNutritionLog?>()
    val consumedNutrition: LiveData<FirebaseService.DailyNutritionLog?> = _consumedNutrition

    private val _error = MutableLiveData<Boolean>()
    val isError: LiveData<Boolean> = _error

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    companion object {
        private const val EVENT_ID = 1
    }

    fun loadConsumedNutrition(email: String) {
        val service = FirebaseService()
        service.getDailyLog(email)
            .asLiveData()
            .observeForever { log ->
                // observeForever is used here because this is called once from Fragment
                // but we should ideally use a more lifecycle-aware approach if possible.
                // For now, adding a simple null check/error handling proxy if needed.
                _consumedNutrition.value = log
            }
    }

    fun calculateDailyTargets(user: com.capstone.smartbite.UserModel) {
        if (user.height > 0 && user.weight > 0) {
            _dailyNutrition.value = com.capstone.smartbite.utils.HealthMath.calculateDailyNutrition(user)
        }
    }

    fun loadActiveEvents(){
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
    }
}