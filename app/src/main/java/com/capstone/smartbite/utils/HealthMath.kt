package com.capstone.smartbite.utils

import com.capstone.smartbite.UserModel
import kotlin.math.min

object HealthMath {

    fun calculateBMR(user: UserModel): Double {
        return if (user.gender == "Pria") {
            (10.0 * user.weight) + (6.25 * user.height) - (5.0 * user.age) + 5.0
        } else {
            (10.0 * user.weight) + (6.25 * user.height) - (5.0 * user.age) - 161.0
        }
    }

    fun calculateTDEE(user: UserModel): Double {
        val bmr = calculateBMR(user)
        val factor = when (user.activityLevel) {
            "Low" -> 1.2
            "Medium" -> 1.55
            "High" -> 1.725
            else -> 1.2
        }
        return bmr * factor
    }

    /**
     * Menentukan Minimal (Min) Penurunan Berat Badan (kg/minggu)
     */
    fun getMinWeightLossPerWeek(): Double {
        val defisitMinHarian = 250.0
        return (defisitMinHarian * 7.0) / 7700.0
    }

    /**
     * Menentukan Maksimal (Max) Penurunan Berat Badan (kg/minggu)
     * Mengikuti standar medis: 
     * 1. Defisit tidak boleh membuat asupan kalori < BMR (Proteksi BMR)
     * 2. Defisit tidak boleh > 1000 kkal/hari (Standar CDC/WHO)
     * 3. Asupan kalori minimal absolut: 1200 (W) / 1500 (M)
     */
    fun getMaxWeightLossPerWeek(user: UserModel): Double {
        val tdee = calculateTDEE(user)
        val bmr = calculateBMR(user)
        
        // Aturan 1: Defisit Maksimal Berdasarkan TDEE - BMR
        val bmrProtectionDeficit = tdee - bmr
        
        // Aturan 2: Asupan Minimal Absolut (Kalori yang tersisa setelah defisit)
        // Defisit tidak boleh > (TDEE - 1200/1500)
        val absoluteMinCalories = if (user.gender == "Pria") 1500.0 else 1200.0
        val absoluteFloorDeficit = tdee - absoluteMinCalories
        
        // Ambil nilai terkecil dari berbagai batasan aman
        var safeDeficit = min(bmrProtectionDeficit, 1000.0) // Max 1000 kkal/hari
        safeDeficit = min(safeDeficit, absoluteFloorDeficit)
        
        if (safeDeficit < 0) safeDeficit = 0.0
        
        // Konversi ke Kg/Minggu (7.700 kkal = 1 kg)
        return (safeDeficit * 7.0) / 7700.0
    }

    /**
     * Menghitung BMI (Body Mass Index)
     */
    fun calculateBMI(weight: Double, heightCm: Int): Double {
        if (heightCm <= 0) return 0.0
        val heightM = heightCm / 100.0
        return weight / (heightM * heightM)
    }

    /**
     * Mendapatkan kategori BMI
     */
    fun getBMICategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }

    /**
     * Menghitung berat badan (kg) untuk BMI dan tinggi tertentu
     */
    fun calculateWeightForBMI(bmi: Double, heightCm: Int): Double {
        if (heightCm <= 0) return 0.0
        val heightM = heightCm / 100.0
        return bmi * (heightM * heightM)
    }

    /**
     * Menghitung target nutrisi harian berdasarkan profil user
     */
    fun calculateDailyNutrition(user: com.capstone.smartbite.UserModel): DailyNutritionTargets {
        val tdee = calculateTDEE(user)
        
        // Asumsi target mingguan (kg/minggu)
        // Idealnya ini diambil dari UserPreference jika disimpan
        val weeklyGoalRate = 0.5 
        val dailyAdjustment = (weeklyGoalRate * 7700) / 7

        val targetCalories = when (user.goal) {
            "Weight Loss Focus" -> (tdee - dailyAdjustment).toInt()
            "Muscle Building" -> (tdee + dailyAdjustment).toInt()
            else -> tdee.toInt()
        }.coerceAtLeast(if (user.gender == "Pria") 1500 else 1200)

        // Distribusi Makro: 20% P, 55% C, 25% F
        val protein = (targetCalories * 0.20 / 4).toInt()
        val carbs = (targetCalories * 0.55 / 4).toInt()
        val fat = (targetCalories * 0.25 / 9).toInt()

        return DailyNutritionTargets(targetCalories, protein, carbs, fat)
    }

    data class DailyNutritionTargets(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )
}
