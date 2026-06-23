package com.capstone.smartbite.data

import com.google.gson.annotations.SerializedName

data class PersonalizedMealResponse(
    @field:SerializedName("status")
    val status: String,

    @field:SerializedName("remaining_calories")
    val remainingCalories: Double,

    @field:SerializedName("data")
    val data: CategorizedMeals
)

data class CategorizedMeals(
    @field:SerializedName("under_100")
    val under100: List<MealItem>,

    @field:SerializedName("100_to_250")
    val from100to250: List<MealItem>,

    @field:SerializedName("250_to_500")
    val from250to500: List<MealItem>,

    @field:SerializedName("over_500")
    val over500: List<MealItem>
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
    val protein: Double,

    @field:SerializedName("carbs")
    val carbs: Double,

    @field:SerializedName("fat")
    val fat: Double
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
    
    @field:SerializedName("yields")
    val yields: Int? = null,

    @field:SerializedName("prep_time")
    val prepTime: String? = null,

    @field:SerializedName("cook_time")
    val cookTime: String? = null,

    @field:SerializedName("meal_types")
    val mealTypes: List<String>? = null,

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
