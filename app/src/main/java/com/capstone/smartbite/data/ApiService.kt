package com.capstone.smartbite.data

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @Multipart
    @POST("/predict")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): FileUploadResponse

    @GET("api/v1/personalized-meals")
    suspend fun getPersonalizedMeals(
        @Query("target_calories") targetCalories: Int,
        @Query("consumed_calories") consumedCalories: Int,
        @Query("is_see_all") isSeeAll: Boolean = false
    ): PersonalizedMealResponse

    @GET("api/v1/personalized-meals/{id}")
    suspend fun getMealDetail(
        @Path("id") id: String
    ): MealDetailResponse
}