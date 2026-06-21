package com.capstone.smartbite.ui.recipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.smartbite.data.ApiConfig
import com.capstone.smartbite.data.MealDetail
import kotlinx.coroutines.launch

class RecipeDetailViewModel : ViewModel() {
    private val _mealDetail = MutableLiveData<MealDetail>()
    val mealDetail: LiveData<MealDetail> = _mealDetail

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun getMealDetail(mealId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val apiService = ApiConfig.getApiService()
                val response = apiService.getMealDetail(mealId)
                _mealDetail.postValue(response.data)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Unknown error")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
