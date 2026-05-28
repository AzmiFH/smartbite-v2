package com.capstone.smartbite.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FileUploadResponse(

	@field:SerializedName("error")
	val error: Boolean,

	@field:SerializedName("message")
	val message: String,

	@SerializedName("food")
	val food: String,

	@SerializedName("nutrition")
	val nutrition: Nutrition
) : Serializable

data class Nutrition(
	@SerializedName("id")
	val id: Int,

	@SerializedName("calories")
	val calories: Int,

	@SerializedName("proteins")
	val proteins: Double,

	@SerializedName("fat")
	val fat: Double,

	@SerializedName("carbohydrate")
	val carbohydrate: Double,

	@SerializedName("name")
	val name: String
) : Serializable
