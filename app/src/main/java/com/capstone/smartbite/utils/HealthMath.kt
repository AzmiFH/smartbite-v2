package com.capstone.smartbite.utils

import com.capstone.smartbite.UserModel
import kotlin.math.min

object HealthMath {

    private fun isMale(gender: String?): Boolean {
        return gender == "Male" || gender == "Pria"
    }

    fun calculateBMR(user: UserModel): Double {
        return if (isMale(user.gender)) {
            (10.0 * user.weight) + (6.25 * user.height) - (5.0 * user.age) + 5.0
        } else {
            (10.0 * user.weight) + (6.25 * user.height) - (5.0 * user.age) - 161.0
        }
    }

    fun calculateTDEE(user: UserModel): Double {
        val bmr = calculateBMR(user)
        /**
         * Faktor Aktivitas (PAL - Physical Activity Level) sesuai standar WHO:
         * - Low (Sedentary): 1.2
         * - Medium (Lightly Active - Olahraga 1-3 hari): 1.375 (Diperbarui agar sinkron dengan deskripsi UI)
         * - High (Very Active - Olahraga 4-7 hari): 1.725
         * 
         * Catatan: Level "Moderately Active" (1.55) dilewati untuk menyederhanakan pilihan user 
         * menjadi 3 kategori utama yang kontras.
         */
        val factor = when (user.activityLevel) {
            "Low" -> 1.2
            "Medium" -> 1.375
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
        val absoluteMinCalories = if (isMale(user.gender)) 1500.0 else 1200.0
        val absoluteFloorDeficit = tdee - absoluteMinCalories
        
        // Ambil nilai terkecil dari berbagai batasan aman
        var safeDeficit = min(bmrProtectionDeficit, 1000.0) // Max 1000 kkal/hari
        safeDeficit = min(safeDeficit, absoluteFloorDeficit)
        
        if (safeDeficit < 0) safeDeficit = 0.0
        
        // Konversi ke Kg/Minggu (7.700 kkal = 1 kg)
        return (safeDeficit * 7.0) / 7700.0
    }

    /**
     * Menentukan Maksimal (Max) Penambahan Berat Badan (kg/minggu)
     * Standar lean bulking: Maksimal surplus 500 kkal/hari (~0.5 kg/minggu)
     */
    fun getMaxWeightGainPerWeek(): Double {
        return 0.5
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
     * Mendapatkan kategori BMI (Standar Kemenkes RI untuk Asia/Indonesia)
     */
    fun getBMICategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi <= 25.1 -> "Normal"
            bmi <= 27.1 -> "Overweight"
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
        
        // Sekarang mengambil target mingguan dari profil user secara dinamis
        val weeklyGoalRate = user.weeklyRate
        val dailyAdjustment = (weeklyGoalRate * 7700) / 7

        val targetCalories = when (user.goal) {
            "Weight Loss Focus" -> (tdee - dailyAdjustment).toInt()
            "Muscle Building" -> (tdee + dailyAdjustment).toInt()
            else -> tdee.toInt()
        }.coerceAtLeast(if (isMale(user.gender)) 1500 else 1200)

        // Distribusi Makro berdasarkan Goal (Standard Fitness/Indonesian AKG adjustment)
        val (pRatio, cRatio, fRatio) = when (user.goal) {
            "Weight Loss Focus" -> Triple(0.25, 0.50, 0.25) // High protein for satiety/muscle preservation
            "Muscle Building" -> Triple(0.25, 0.55, 0.20)  // High carb for energy, high protein for growth
            else -> Triple(0.15, 0.60, 0.25)                // Standard AKG Indonesia (approx)
        }

        val protein = (targetCalories * pRatio / 4).toInt()
        val carbs = (targetCalories * cRatio / 4).toInt()
        val fat = (targetCalories * fRatio / 9).toInt()

        return DailyNutritionTargets(targetCalories, protein, carbs, fat)
    }

    data class DailyNutritionTargets(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )
}
