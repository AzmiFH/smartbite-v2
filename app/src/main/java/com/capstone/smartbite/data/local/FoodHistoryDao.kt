package com.capstone.smartbite.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodHistory: FoodHistoryEntity)

    @Query("SELECT * FROM food_history WHERE userEmail = :email AND date = :date ORDER BY timestamp DESC")
    fun getDailyHistory(email: String, date: String): Flow<List<FoodHistoryEntity>>

    @Query("SELECT * FROM food_history WHERE userEmail = :email AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getWeeklyHistory(email: String, startDate: String, endDate: String): Flow<List<FoodHistoryEntity>>

    @Query("SELECT * FROM food_history WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getAllHistory(email: String): Flow<List<FoodHistoryEntity>>

    @Delete
    suspend fun delete(foodHistory: FoodHistoryEntity)

    @Query("DELETE FROM food_history WHERE userEmail = :email")
    suspend fun deleteAllByUser(email: String)
}
