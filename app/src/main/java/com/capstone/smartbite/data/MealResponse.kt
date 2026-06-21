package com.capstone.smartbite.data

import com.google.gson.annotations.SerializedName

data class PersonalizedMealResponse(
    @field:SerializedName("status")
    val status: String,

    @field:SerializedName("remaining_calories")
    val remainingCalories: Int,

    @field:SerializedName("total_results")
    val totalResults: Int,

    @field:SerializedName("data")
    val data: List<MealItem>
)

data class MealItem(
    @field:SerializedName("id")
    val id: String,

    @field:SerializedName("title")
    val title: String,

    @field:SerializedName("description")
    val description: String,

    @field:SerializedName("calories")
    val calories: Int,

    @field:SerializedName("macros")
    val macros: MealMacros,

    @field:SerializedName("tag")
    val tag: String,

    @field:SerializedName("image_url")
    val imageUrl: String
)

data class MealMacros(
    @field:SerializedName("protein")
    val protein: Int,

    @field:SerializedName("carbs")
    val carbs: Int,

    @field:SerializedName("fat")
    val fat: Int
)

data class MealDetailResponse(
    @field:SerializedName("status")
    val status: String,

    @field:SerializedName("data")
    val data: MealDetail
)

data class MealDetail(
    @field:SerializedName("id")
    val id: String,

    @field:SerializedName("title")
    val title: String,

    @field:SerializedName("ingredients")
    val ingredients: List<String>,

    @field:SerializedName("instructions")
    val instructions: List<String>,
    
    // Adding extra fields that might come from the list but are needed in detail
    @field:SerializedName("calories")
    val calories: Int? = null,
    
    @field:SerializedName("macros")
    val macros: MealMacros? = null,
    
    @field:SerializedName("tag")
    val tag: String? = null,
    
    @field:SerializedName("image_url")
    val imageUrl: String? = null,
    
    @field:SerializedName("description")
    val description: String? = null
)
