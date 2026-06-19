package com.capstone.smartbite.ui.kamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.capstone.smartbite.data.FileUploadResponse
import com.capstone.smartbite.data.FoodRepository
import com.capstone.smartbite.R
import com.capstone.smartbite.databinding.ActivityResultBinding
import com.capstone.smartbite.utils.FoodMeasurementHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var foodRepository: FoodRepository
    
    private var quantity = 1.0
    private var baseCalories = 0
    private var baseProtein = 0.0
    private var baseFat = 0.0
    private var baseCarbs = 0.0
    private var foodName = ""
    private var unit = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        foodRepository = FoodRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val result = intent.getSerializableExtra("result") as? FileUploadResponse
        val imageUri = intent.getStringExtra("imageUri")?.toUri()

        imageUri?.let {
            binding.previewImageView.setImageURI(it)
        }

        result?.let {
            foodName = it.food
            val nutrition = it.nutrition
            baseCalories = nutrition.calories
            baseProtein = nutrition.proteins
            baseFat = nutrition.fat
            baseCarbs = nutrition.carbohydrate
            
            unit = FoodMeasurementHelper.getUnitForFood(foodName)
            
            updateUI()
        } ?: Log.e("ResultActivity", "No result received!")

        setupListeners(result, imageUri)
    }

    private fun updateUI() {
        binding.tvFoodName.text = foodName
        binding.tvTagCategory.text = getString(R.string.food_label)
        
        val displayQty = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else String.format("%.1f", quantity)
        binding.tvQuantity.text = displayQty
        binding.tvTagPortion.text = "$displayQty ${unit.uppercase(Locale.getDefault())}"

        val totalCalories = FoodMeasurementHelper.calculateNutrient(baseCalories, quantity)
        val totalProtein = FoodMeasurementHelper.calculateNutrient(baseProtein, quantity)
        val totalFat = FoodMeasurementHelper.calculateNutrient(baseFat, quantity)
        val totalCarbs = FoodMeasurementHelper.calculateNutrient(baseCarbs, quantity)

        binding.tvCalories.text = getString(R.string.total_calories_format, totalCalories)
        binding.tvProteinVal.text = String.format("%.1fg", totalProtein)
        binding.tvFatVal.text = String.format("%.1fg", totalFat)
        binding.tvCarbsVal.text = String.format("%.1fg", totalCarbs)

        binding.progressProtein.progress = (totalProtein * 2).toInt().coerceAtMost(100)
        binding.progressFat.progress = (totalFat * 2).toInt().coerceAtMost(100)
        binding.progressCarbs.progress = (totalCarbs * 2).toInt().coerceAtMost(100)
    }

    private fun setupListeners(result: FileUploadResponse?, imageUri: Uri?) {
        binding.btnMinus.setOnClickListener {
            if (quantity > 0.5) {
                quantity -= 0.5
                updateUI()
            }
        }

        binding.btnPlus.setOnClickListener {
            quantity += 0.5
            updateUI()
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClose.setOnClickListener {
            setResult(RESULT_GO_TO_DASHBOARD)
            finish()
        }
        binding.btnRetake.setOnClickListener {
            val intent = Intent()
            intent.putExtra("last_result", result)
            intent.putExtra("last_imageUri", imageUri.toString())
            setResult(RESULT_RETAKE, intent)
            finish()
        }
        
        binding.btnAddMeal.setOnClickListener {
            val email = FirebaseAuth.getInstance().currentUser?.email
            if (email != null && result != null) {
                lifecycleScope.launch {
                    try {
                        val totalCalories = FoodMeasurementHelper.calculateNutrient(baseCalories, quantity)
                        val totalProtein = FoodMeasurementHelper.calculateNutrient(baseProtein, quantity)
                        val totalFat = FoodMeasurementHelper.calculateNutrient(baseFat, quantity)
                        val totalCarbs = FoodMeasurementHelper.calculateNutrient(baseCarbs, quantity)

                        foodRepository.addMeal(
                            email = email,
                            foodName = foodName,
                            calories = totalCalories.toDouble(),
                            protein = totalProtein,
                            fat = totalFat,
                            carbs = totalCarbs,
                            quantity = quantity,
                            unit = unit,
                            imageUrl = imageUri?.toString()
                        )
                        
                        Toast.makeText(this@ResultActivity, getString(R.string.success_add_meal), Toast.LENGTH_SHORT).show()
                        setResult(RESULT_GO_TO_DASHBOARD)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@ResultActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
        const val RESULT_RETAKE = 101
        const val RESULT_GO_TO_DASHBOARD = 102
    }
}
