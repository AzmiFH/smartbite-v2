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
import android.speech.RecognizerIntent
import android.view.View
import com.capstone.smartbite.data.Nutrition
import androidx.activity.result.contract.ActivityResultContracts
import com.capstone.smartbite.databinding.ActivityResultBinding
import com.capstone.smartbite.utils.FoodMeasurementHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.capstone.smartbite.data.ApiConfig
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import com.capstone.smartbite.databinding.ItemFoodFusionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var foodRepository: FoodRepository
    
    private var selectedMultiplier = 1.0
    private var baseCalories = 0
    private var baseProtein = 0.0
    private var baseFat = 0.0
    private var baseCarbs = 0.0
    private var foodName = ""
    private var isGalleryScan = false
    private var isManualMode = false

    private val detectedFoods = mutableListOf<Nutrition>()
    private var portions = listOf<FoodMeasurementHelper.Portion>()

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            binding.tvTranscription.text = spokenText
            binding.badgeVoice.visibility = View.VISIBLE
            processVoiceInput(spokenText)
        }
    }

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
        isGalleryScan = intent.getBooleanExtra("is_gallery_scan", false)

        imageUri?.let {
            binding.previewImageView.setImageURI(it)
        }

        result?.let {
            foodName = it.food
            val nutrition = it.nutrition
            detectedFoods.add(nutrition)
            
            baseCalories = nutrition.calories
            baseProtein = nutrition.proteins
            baseFat = nutrition.fat
            baseCarbs = nutrition.carbohydrate
            
            setupPortionDropdown()
            updateUI()
        } ?: Log.e("ResultActivity", "No result received!")

        setupListeners(result, imageUri)
    }

    private fun setupPortionDropdown() {
        portions = FoodMeasurementHelper.getPortionsForFood(foodName)
        val options = portions.map { "${it.label} (${it.weightGrams}g)" }.toMutableList()
        options.add("Masukkan dalam Gram ")

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        binding.actvPortion.setAdapter(adapter)

        // Set default to Medium portion if available, else first
        val mediumIndex = portions.indexOfFirst { it.label.contains("Sedang") }
        val defaultIndex = if (mediumIndex != -1) mediumIndex else 0
        
        if (portions.isNotEmpty()) {
            binding.actvPortion.setText(options[defaultIndex], false)
            selectedMultiplier = portions[defaultIndex].multiplier
            updateGramDetail(portions[defaultIndex].weightGrams)
        }

        binding.actvPortion.setOnItemClickListener { _, _, position, _ ->
            if (position < portions.size) {
                isManualMode = false
                binding.tilManualGrams.visibility = View.GONE
                selectedMultiplier = portions[position].multiplier
                updateGramDetail(portions[position].weightGrams)
                updateUI()
            } else {
                isManualMode = true
                binding.tilManualGrams.visibility = View.VISIBLE
                binding.tvGramDetail.text = "(Masukkan berat dalam gram)"
                binding.etManualGrams.requestFocus()
                // Nutrition will be updated via TextWatcher on etManualGrams
            }
        }

        binding.etManualGrams.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isManualMode) {
                    val grams = s.toString().toDoubleOrNull() ?: 0.0
                    selectedMultiplier = grams / 100.0 // Assuming API base is 100g
                    updateUI()
                }
            }
        })
    }

    private fun updateGramDetail(grams: Int) {
        binding.tvGramDetail.text = "(setara dengan ± $grams gram)"
    }

    private fun updateUI() {
        val displayFoodName = detectedFoods.joinToString(" + ") { it.name }
        binding.tvFoodName.text = if (displayFoodName.isNotEmpty()) displayFoodName else foodName
        binding.tvTagCategory.text = getString(R.string.food_label)

        // Update list of items in the container
        binding.fusionItemsContainer.removeAllViews()
        detectedFoods.forEach { nutrition ->
            val itemBinding = ItemFoodFusionBinding.inflate(layoutInflater, binding.fusionItemsContainer, false)
            
            val itemCal = FoodMeasurementHelper.calculateNutrient(nutrition.calories, selectedMultiplier)
            val p = FoodMeasurementHelper.calculateNutrient(nutrition.proteins, selectedMultiplier)
            val f = FoodMeasurementHelper.calculateNutrient(nutrition.fat, selectedMultiplier)
            val c = FoodMeasurementHelper.calculateNutrient(nutrition.carbohydrate, selectedMultiplier)

            itemBinding.tvItemName.text = nutrition.name
            itemBinding.tvItemCalories.text = getString(R.string.total_calories_format, itemCal)
            
            itemBinding.tvProteinVal.text = String.format("%.1fg", p)
            itemBinding.progressProtein.progress = (p * 2).toInt().coerceAtMost(100)

            itemBinding.tvFatVal.text = String.format("%.1fg", f)
            itemBinding.progressFat.progress = (f * 2).toInt().coerceAtMost(100)

            itemBinding.tvCarbsVal.text = String.format("%.1fg", c)
            itemBinding.progressCarbs.progress = (c * 2).toInt().coerceAtMost(100)

            binding.fusionItemsContainer.addView(itemBinding.root)
        }

        val totalBaseCalories = detectedFoods.sumOf { it.calories }
        val totalCalories = FoodMeasurementHelper.calculateNutrient(totalBaseCalories, selectedMultiplier)
        binding.tvCalories.text = getString(R.string.total_calories_format, totalCalories)
    }

    private fun setupListeners(result: FileUploadResponse?, imageUri: Uri?) {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnClose.setOnClickListener {
            setResult(RESULT_GO_TO_DASHBOARD)
            finish()
        }
        binding.btnRetake.setOnClickListener {
            val intent = Intent()
            intent.putExtra("last_result", result)
            intent.putExtra("last_imageUri", imageUri.toString())
            intent.putExtra("is_gallery_scan", isGalleryScan)
            setResult(RESULT_RETAKE, intent)
            finish()
        }

        binding.btnMic.setOnClickListener {
            startVoiceInput()
        }
        
        binding.btnAddMeal.setOnClickListener {
            val email = FirebaseAuth.getInstance().currentUser?.email
            if (email != null && result != null) {
                lifecycleScope.launch {
                    try {
                        val baseTimestamp = System.currentTimeMillis()
                        // Use reversed so the first item in the list gets the latest timestamp and shows on top
                        detectedFoods.reversed().forEachIndexed { index, nutrition ->
                            val itemCalories = FoodMeasurementHelper.calculateNutrient(nutrition.calories, selectedMultiplier)
                            val itemProtein = FoodMeasurementHelper.calculateNutrient(nutrition.proteins, selectedMultiplier)
                            val itemFat = FoodMeasurementHelper.calculateNutrient(nutrition.fat, selectedMultiplier)
                            val itemCarbs = FoodMeasurementHelper.calculateNutrient(nutrition.carbohydrate, selectedMultiplier)
                            
                            val itemImageUri = if (nutrition.name == foodName) imageUri?.toString() else null

                            val displayUnit = if (isManualMode) {
                                "${binding.etManualGrams.text} g"
                            } else {
                                binding.actvPortion.text.toString()
                                    .replace("[Porsi] ", "")
                                    .substringBefore(" (")
                            }

                            foodRepository.addMeal(
                                email = email,
                                foodName = nutrition.name,
                                calories = itemCalories.toDouble(),
                                protein = itemProtein,
                                fat = itemFat,
                                carbs = itemCarbs,
                                quantity = selectedMultiplier,
                                unit = displayUnit,
                                imageUrl = itemImageUri,
                                customTimestamp = baseTimestamp + index // Ensure unique and ordered timestamps
                            )
                        }
                        
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

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.mic_prompt))
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processVoiceInput(text: String) {
        val lowercaseText = text.lowercase()
        lifecycleScope.launch {
            try {
                binding.tvFusionStatus.text = getString(R.string.fusion_processing)
                
                // Call API to search for food items in CSV
                val apiService = ApiConfig.getApiService()
                val results = apiService.searchFood(lowercaseText)
                
                if (results.isNotEmpty()) {
                    results.forEach { nutrition: Nutrition ->
                        if (detectedFoods.none { it.name.lowercase() == nutrition.name.lowercase() }) {
                            detectedFoods.add(nutrition)
                        }
                    }
                    binding.tvFusionStatus.text = getString(R.string.fusion_vision) // Reuse existing string for "Success" look
                    updateUI()
                } else {
                    // Fallback to local keyword matching if API returns empty
                    val keywords = listOf("telur", "tempe", "tahu", "ayam", "nasi")
                    keywords.forEach { keyword ->
                        if (lowercaseText.contains(keyword)) {
                            val mockNutrition = when (keyword) {
                                "telur" -> Nutrition(0, 78, 6.0, 5.0, 0.6, "Telur")
                                "tempe" -> Nutrition(0, 193, 19.0, 11.0, 9.0, "Tempe")
                                "tahu" -> Nutrition(0, 76, 8.0, 4.8, 1.9, "Tahu")
                                else -> null
                            }
                            mockNutrition?.let { nutrition ->
                                if (detectedFoods.none { it.name.lowercase() == nutrition.name.lowercase() }) {
                                    detectedFoods.add(nutrition)
                                    updateUI()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ResultActivity", "Fusion error: ${e.message}")
                binding.tvFusionStatus.text = "Fusion Error"
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