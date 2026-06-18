package com.capstone.smartbite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_history")
data class FoodHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val quantity: Double,
    val unit: String,
    val date: String, // Format: yyyy-MM-dd
    val timestamp: Long,
    val imageUrl: String? = null,
    val userEmail: String
)
