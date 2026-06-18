package com.capstone.smartbite.data

import android.content.Context
import com.capstone.smartbite.data.local.FoodDatabase
import com.capstone.smartbite.data.local.FoodHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class FoodRepository(context: Context) {
    private val foodDatabase = FoodDatabase.getDatabase(context)
    private val foodHistoryDao = foodDatabase.foodHistoryDao()
    private val firebaseService = FirebaseService()

    /**
     * Save a meal to both local database and Firebase.
     */
    suspend fun addMeal(
        email: String,
        foodName: String,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        quantity: Double,
        unit: String,
        imageUrl: String? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

        // 1. Save to Room (Local)
        val entity = FoodHistoryEntity(
            foodName = foodName,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            quantity = quantity,
            unit = unit,
            date = dateString,
            timestamp = timestamp,
            imageUrl = imageUrl,
            userEmail = email
        )
        foodHistoryDao.insert(entity)

        // 2. Save to Firebase (Remote)
        try {
            firebaseService.addMealLog(email, calories, protein, carbs, fat)
        } catch (e: Exception) {
            // Log error or handle offline state
            // In a full implementation, we might mark this entry as "pending sync"
        }
    }

    fun getDailyHistory(email: String, date: String): Flow<List<FoodHistoryEntity>> {
        return foodHistoryDao.getDailyHistory(email, date)
    }

    fun getWeeklyHistory(email: String, startDate: String, endDate: String): Flow<List<FoodHistoryEntity>> {
        return foodHistoryDao.getWeeklyHistory(email, startDate, endDate)
    }

    fun getAllHistory(email: String): Flow<List<FoodHistoryEntity>> {
        return foodHistoryDao.getAllHistory(email)
    }
}
