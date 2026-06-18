package com.capstone.smartbite.utils

import java.util.Locale

object FoodMeasurementHelper {

    /**
     * Map of food names to their default measurement units.
     * This is used to display human-friendly units in the UI.
     */
    private val foodUnitMap = mapOf(
        "ayam goreng" to "potong",
        "nasi putih" to "piring",
        "nasi goreng" to "piring",
        "telur dadar" to "butir",
        "telur rebus" to "butir",
        "mie instan" to "bungkus",
        "sate ayam" to "tusuk",
        "bakso" to "mangkok",
        "gado-gado" to "porsi",
        "rendang" to "potong",
        "tempe goreng" to "potong",
        "tahu goreng" to "potong",
        "pisang" to "buah",
        "apel" to "buah",
        "jeruk" to "buah",
        "susu" to "gelas",
        "kopi" to "cangkir",
        "teh" to "gelas"
    )

    /**
     * Get the measurement unit for a given food name.
     * Defaults to "porsi" if not found.
     */
    fun getUnitForFood(foodName: String): String {
        val normalizedName = foodName.lowercase(Locale.getDefault())
        return foodUnitMap[normalizedName] ?: "porsi"
    }

    /**
     * Calculate nutritional values based on quantity.
     * @param baseValue The value for 1 unit.
     * @param quantity The number of units.
     */
    fun calculateNutrient(baseValue: Double, quantity: Double): Double {
        return baseValue * quantity
    }

    /**
     * Calculate nutritional values based on quantity (Int version for calories).
     */
    fun calculateNutrient(baseValue: Int, quantity: Double): Int {
        return (baseValue * quantity).toInt()
    }
}
