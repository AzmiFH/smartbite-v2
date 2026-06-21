package com.capstone.smartbite.utils

import com.capstone.smartbite.R
import java.util.Locale

object FoodMeasurementHelper {

    /**
     * Map of food names to their default measurement unit resource IDs.
     * This is used to display human-friendly units in the UI.
     */
    private val foodUnitMap = mapOf(
        "ayam goreng" to R.string.unit_piece,
        "nasi putih" to R.string.unit_plate,
        "nasi goreng" to R.string.unit_plate,
        "telur dadar" to R.string.unit_grain,
        "telur rebus" to R.string.unit_grain,
        "mie instan" to R.string.unit_pack,
        "sate ayam" to R.string.unit_skewer,
        "bakso" to R.string.unit_bowl,
        "gado-gado" to R.string.unit_portion,
        "rendang" to R.string.unit_piece,
        "tempe goreng" to R.string.unit_piece,
        "tahu goreng" to R.string.unit_piece,
        "pisang" to R.string.unit_fruit,
        "apel" to R.string.unit_fruit,
        "jeruk" to R.string.unit_fruit,
        "susu" to R.string.unit_glass,
        "kopi" to R.string.unit_cup,
        "teh" to R.string.unit_glass
    )

    /**
     * Get the measurement unit resource ID for a given food name.
     * Defaults to unit_portion if not found.
     */
    fun getUnitResIdForFood(foodName: String): Int {
        val normalizedName = foodName.lowercase(Locale.getDefault())
        return foodUnitMap[normalizedName] ?: R.string.unit_portion
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
