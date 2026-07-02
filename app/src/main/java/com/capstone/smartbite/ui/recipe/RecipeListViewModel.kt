package com.capstone.smartbite.ui.recipe

import android.app.Application
import androidx.lifecycle.*
import com.capstone.smartbite.data.ApiConfig
import com.capstone.smartbite.data.FoodRepository
import com.capstone.smartbite.data.MealItem
import kotlinx.coroutines.launch

class RecipeListViewModel(application: Application) : AndroidViewModel(application) {
    private val foodRepository = FoodRepository(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _allMeals = MutableLiveData<List<MealItem>>()
    private val _filteredMeals = MutableLiveData<List<MealItem>>()
    val filteredMeals: LiveData<List<MealItem>> = _filteredMeals

    private var currentMealType: String = "Semua Tipe"
    private var currentCalorieCategory: String = "Semua Kalori"

    private val _message = MutableLiveData<com.capstone.smartbite.utils.Event<String>>()
    val message: LiveData<com.capstone.smartbite.utils.Event<String>> = _message

    fun fetchAllRecipes(targetCalories: Int, consumedCalories: Int) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val apiService = ApiConfig.getApiService()
                val response = apiService.getPersonalizedMeals(targetCalories, consumedCalories, isSeeAll = true)
                
                val meals = mutableListOf<MealItem>()
                meals.addAll(response.data.under100)
                meals.addAll(response.data.from100to250)
                meals.addAll(response.data.from250to500)
                meals.addAll(response.data.over500)
                
                _allMeals.postValue(meals)
                applyFilters(meals, currentMealType, currentCalorieCategory)
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _isLoading.postValue(false)
                _allMeals.postValue(emptyList())
                _filteredMeals.postValue(emptyList())
            }
        }
    }

    fun setMealTypeFilter(type: String) {
        currentMealType = type
        _allMeals.value?.let { applyFilters(it, currentMealType, currentCalorieCategory) }
    }

    fun setCalorieFilter(category: String) {
        currentCalorieCategory = category
        _allMeals.value?.let { applyFilters(it, currentMealType, currentCalorieCategory) }
    }

    private fun applyFilters(meals: List<MealItem>, type: String, calories: String) {
        var filtered = meals

        if (type != "Semua Tipe") {
            filtered = filtered.filter { 
                it.tag.contains(type, ignoreCase = true) || 
                (type == "Makan Pagi" && it.tag.contains("Breakfast", ignoreCase = true))
            }
        }

        if (calories != "Semua Kalori") {
            filtered = when (calories) {
                "< 100 kcal" -> filtered.filter { it.calories < 100 }
                "100 - 250 kcal" -> filtered.filter { it.calories in 100..250 }
                "250 - 500 kcal" -> filtered.filter { it.calories in 251..500 }
                "> 500 kcal" -> filtered.filter { it.calories > 500 }
                else -> filtered
            }
        }

        _filteredMeals.postValue(filtered)
    }

    fun addMealToHistory(meal: MealItem, email: String) {
        viewModelScope.launch {
            foodRepository.addMeal(
                email = email,
                foodName = meal.title,
                calories = meal.calories.toDouble(),
                protein = meal.macros.protein,
                fat = meal.macros.fat,
                carbs = meal.macros.carbs,
                quantity = 1.0,
                unit = "porsi",
                imageUrl = meal.imageUrl
            )
            _message.postValue(com.capstone.smartbite.utils.Event("${meal.title} ditambahkan!"))
        }
    }
}
