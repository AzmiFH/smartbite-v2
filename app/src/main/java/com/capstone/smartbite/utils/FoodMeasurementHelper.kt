package com.capstone.smartbite.utils

import com.capstone.smartbite.R
import java.util.Locale

object FoodMeasurementHelper {

    /**
     * Data class for a food portion definition.
     */
    data class Portion(
        val label: String,
        val weightGrams: Int,
        val multiplier: Double // Multiplier relative to the 100g base nutrition
    )

    /**
     * Map of food names to their available portions.
     */
    private val portionMap = mapOf(
        "nasi putih" to listOf(
            Portion("Piring Kecil", 100, 1.0),
            Portion("Piring Sedang", 200, 2.0),
            Portion("Piring Besar", 300, 3.0)
        ),
        "nasi goreng" to listOf(
            Portion("Piring Kecil", 150, 1.5),
            Portion("Piring Sedang", 250, 2.5),
            Portion("Piring Besar", 400, 4.0)
        ),
        "ayam goreng" to listOf(
            Portion("Potong Kecil", 50, 0.5),
            Portion("Potong Sedang", 100, 1.0),
            Portion("Potong Besar", 150, 1.5)
        ),
        "mie instan" to listOf(
            Portion("Bungkus Standar", 80, 0.8),
            Portion("Porsi Double", 160, 1.6)
        ),
        "telur dadar" to listOf(
            Portion("Butir Kecil", 40, 0.4),
            Portion("Butir Sedang", 60, 0.6),
            Portion("Butir Besar", 80, 0.8)
        ),
        "rendang" to listOf(
            Portion("Potong Kecil", 40, 0.4),
            Portion("Potong Sedang", 70, 0.7),
            Portion("Potong Besar", 100, 1.0)
        ),
        "sate ayam" to listOf(
            Portion("5 Tusuk", 75, 0.75),
            Portion("10 Tusuk", 150, 1.5)
        )
    )

    /**
     * Default portions for foods not in the map.
     */
    private val defaultPortions = listOf(
        Portion("Porsi Kecil", 50, 0.5),
        Portion("Porsi Sedang", 100, 1.0),
        Portion("Porsi Besar", 200, 2.0)
    )

    fun getPortionsForFood(foodName: String): List<Portion> {
        val normalizedName = foodName.lowercase(Locale.getDefault())
        return portionMap[normalizedName] ?: defaultPortions
    }

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
