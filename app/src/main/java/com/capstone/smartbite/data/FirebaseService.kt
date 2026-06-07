package com.capstone.smartbite.data

import com.capstone.smartbite.UserModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

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
        usersCollection.document(email).delete().await()
    }
}
