package com.capstone.smartbite.data

import com.capstone.smartbite.UserModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun addMealLog(email: String, calories: Double, protein: Double, carbs: Double, fat: Double) {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logRef = usersCollection.document(email).collection("daily_logs").document(dateString)

        val updates = hashMapOf(
            "totalCalories" to FieldValue.increment(calories),
            "totalProtein" to FieldValue.increment(protein),
            "totalCarbs" to FieldValue.increment(carbs),
            "totalFat" to FieldValue.increment(fat),
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        logRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun getDailyLog(email: String): Flow<DailyNutritionLog?> = callbackFlow {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logRef = usersCollection.document(email).collection("daily_logs").document(dateString)

        val registration = logRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Jangan panggil close(error) jika Anda ingin aplikasi tetap berjalan
                // Cukup kirim null atau data kosong, dan log error-nya
                android.util.Log.e("FirebaseService", "Firestore Error: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val log = DailyNutritionLog(
                    calories = snapshot.getDouble("totalCalories")?.toInt() ?: 0,
                    protein = snapshot.getDouble("totalProtein")?.toInt() ?: 0,
                    carbs = snapshot.getDouble("totalCarbs")?.toInt() ?: 0,
                    fat = snapshot.getDouble("totalFat")?.toInt() ?: 0
                )
                trySend(log)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun resetDailyLog(email: String) {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logRef = usersCollection.document(email).collection("daily_logs").document(dateString)

        val resetValues = hashMapOf(
            "totalCalories" to 0,
            "totalProtein" to 0,
            "totalCarbs" to 0,
            "totalFat" to 0,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        logRef.set(resetValues, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    data class DailyNutritionLog(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )

    suspend fun saveUserProfile(user: UserModel) {
        val userEmail = user.email ?: return
        val userMap = hashMapOf(
            "name" to user.name,
            "email" to user.email,
            "age" to user.age,
            "phoneNumber" to user.phoneNumber,
            "address" to user.add,
            "gender" to user.gender,
            "height" to user.height,
            "weight" to user.weight,
            "targetWeight" to user.targetWeight,
            "goal" to user.goal,
            "activityLevel" to user.activityLevel,
            "profileImageUri" to user.profileImage?.toString()
        )
        usersCollection.document(userEmail).set(userMap).await()
    }

    suspend fun getUserProfile(email: String): UserModel? {
        val document = usersCollection.document(email).get().await()
        if (document.exists()) {
            val user = UserModel()
            user.name = document.getString("name")
            user.email = document.getString("email")
            user.age = document.getLong("age")?.toInt() ?: 0
            user.phoneNumber = document.getString("phoneNumber")
            user.add = document.getString("address")
            user.gender = document.getString("gender")
            user.height = document.getLong("height")?.toInt() ?: 0
            user.weight = document.getLong("weight")?.toInt() ?: 0
            user.targetWeight = document.getLong("targetWeight")?.toInt() ?: 0
            user.goal = document.getString("goal") ?: "Weight Loss Focus"
            user.activityLevel = document.getString("activityLevel")
            user.profileImage = document.getString("profileImageUri")?.let { android.net.Uri.parse(it) }
            return user
        }
        return null
    }

    suspend fun deleteUserProfile(email: String) {
        val userRef = usersCollection.document(email)
        
        // 1. Hapus sub-koleksi daily_logs (Batch Delete)
        val dailyLogs = userRef.collection("daily_logs").get().await()
        if (!dailyLogs.isEmpty) {
            val batch = db.batch()
            for (doc in dailyLogs) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }

        // 2. Hapus dokumen profil utama
        userRef.delete().await()
    }
}
