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
    
    private var baseCalories = 0
    private var baseProtein = 0.0
    private var baseFat = 0.0
    private var baseCarbs = 0.0
    private var foodName = ""
    private var isGalleryScan = false
    private var isManualMode = false

    private data class DetectedFood(
        val nutrition: Nutrition,
        var multiplier: Double = 1.0,
        var portionLabel: String = "Sedang",
        var isManual: Boolean = false,
        var manualGrams: Double = 0.0
    )

    private val detectedFoodList = mutableListOf<DetectedFood>()
    private var mainPortions = listOf<FoodMeasurementHelper.Portion>()

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
            detectedFoodList.add(DetectedFood(nutrition))
            
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
        mainPortions = FoodMeasurementHelper.getPortionsForFood(foodName)
        val options = mainPortions.map { it.label }.toMutableList()
        options.add("Masukkan dalam Gram")

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        binding.actvPortion.setAdapter(adapter)

        // Set default to Medium portion if available, else first
        val mediumIndex = mainPortions.indexOfFirst { it.label.contains("Sedang") }
        val defaultIndex = if (mediumIndex != -1) mediumIndex else 0
        
        if (mainPortions.isNotEmpty()) {
            binding.actvPortion.setText("1 ${options[defaultIndex]}", false)
            if (detectedFoodList.isNotEmpty()) {
                detectedFoodList[0].multiplier = mainPortions[defaultIndex].multiplier
                detectedFoodList[0].portionLabel = mainPortions[defaultIndex].label
            }
            updateGramDetail(mainPortions[defaultIndex].weightGrams)
        }

        binding.actvPortion.setOnItemClickListener { _, _, position, _ ->
            if (position < mainPortions.size) {
                isManualMode = false
                binding.tilManualGrams.visibility = View.GONE
                binding.tvGramDetail.visibility = View.VISIBLE
                
                val selected = mainPortions[position]
                if (detectedFoodList.isNotEmpty()) {
                    detectedFoodList[0].multiplier = selected.multiplier
                    detectedFoodList[0].portionLabel = selected.label
                    detectedFoodList[0].isManual = false
                }
                
                binding.actvPortion.setText("1 ${selected.label}", false)
                updateGramDetail(selected.weightGrams)
                updateUI()
            } else {
                isManualMode = true
                binding.tilManualGrams.visibility = View.VISIBLE
                binding.tvGramDetail.visibility = View.GONE
                if (detectedFoodList.isNotEmpty()) {
                    detectedFoodList[0].isManual = true
                }
                binding.etManualGrams.requestFocus()
            }
        }

        binding.etManualGrams.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isManualMode && detectedFoodList.isNotEmpty()) {
                    val grams = s.toString().toDoubleOrNull() ?: 0.0
                    detectedFoodList[0].multiplier = grams / 100.0
                    detectedFoodList[0].manualGrams = grams
                    updateUI()
                }
            }
        })
    }

    private fun updateGramDetail(grams: Int) {
        binding.tvGramDetail.text = "(setara dengan ± $grams gram)"
    }

    private fun updateUI() {
        val displayFoodName = detectedFoodList.joinToString(" + ") { it.nutrition.name }
        binding.tvFoodName.text = if (displayFoodName.isNotEmpty()) displayFoodName else foodName
        binding.tvTagCategory.text = getString(R.string.food_label)

        // Update list of items in the container
        binding.fusionItemsContainer.removeAllViews()
        detectedFoodList.forEachIndexed { index, detectedFood ->
            val nutrition = detectedFood.nutrition
            val itemBinding = ItemFoodFusionBinding.inflate(layoutInflater, binding.fusionItemsContainer, false)
            
            val itemCal = FoodMeasurementHelper.calculateNutrient(nutrition.calories, detectedFood.multiplier)
            val p = FoodMeasurementHelper.calculateNutrient(nutrition.proteins, detectedFood.multiplier)
            val f = FoodMeasurementHelper.calculateNutrient(nutrition.fat, detectedFood.multiplier)
            val c = FoodMeasurementHelper.calculateNutrient(nutrition.carbohydrate, detectedFood.multiplier)

            itemBinding.tvItemName.text = nutrition.name
            itemBinding.tvItemCalories.text = getString(R.string.total_calories_format, itemCal)
            
            // Hide portion dropdown for the first item (Scanned Food) as it's already at the top
            if (index == 0) {
                itemBinding.tilItemPortion.visibility = View.GONE
            } else {
                itemBinding.tilItemPortion.visibility = View.VISIBLE
                
                // Per-item portion dropdown
                val itemPortions = FoodMeasurementHelper.getPortionsForFood(nutrition.name)
                val itemOptions = itemPortions.map { it.label }
                val itemAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, itemOptions)
                itemBinding.actvItemPortion.setAdapter(itemAdapter)
                
                // Set initial value
                val currentLabel = if (detectedFood.isManual) "${detectedFood.manualGrams}g" else detectedFood.portionLabel
                itemBinding.actvItemPortion.setText(currentLabel, false)

                itemBinding.actvItemPortion.setOnItemClickListener { _, _, position, _ ->
                    val selected = itemPortions[position]
                    detectedFood.multiplier = selected.multiplier
                    detectedFood.portionLabel = selected.label
                    detectedFood.isManual = false
                    updateUI()
                }
            }

            itemBinding.tvProteinVal.text = String.format("%.1fg", p)
            itemBinding.progressProtein.progress = (p * 2).toInt().coerceAtMost(100)

            itemBinding.tvFatVal.text = String.format("%.1fg", f)
            itemBinding.progressFat.progress = (f * 2).toInt().coerceAtMost(100)

            itemBinding.tvCarbsVal.text = String.format("%.1fg", c)
            itemBinding.progressCarbs.progress = (c * 2).toInt().coerceAtMost(100)

            binding.fusionItemsContainer.addView(itemBinding.root)
        }

        val totalCalories = detectedFoodList.sumOf { 
            FoodMeasurementHelper.calculateNutrient(it.nutrition.calories, it.multiplier) 
        }
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
                        detectedFoodList.reversed().forEachIndexed { index, detectedFood ->
                            val nutrition = detectedFood.nutrition
                            val itemCalories = FoodMeasurementHelper.calculateNutrient(nutrition.calories, detectedFood.multiplier)
                            val itemProtein = FoodMeasurementHelper.calculateNutrient(nutrition.proteins, detectedFood.multiplier)
                            val itemFat = FoodMeasurementHelper.calculateNutrient(nutrition.fat, detectedFood.multiplier)
                            val itemCarbs = FoodMeasurementHelper.calculateNutrient(nutrition.carbohydrate, detectedFood.multiplier)
                            
                            val itemImageUri = if (nutrition.name == foodName) imageUri?.toString() else null

                            val displayUnit = if (detectedFood.isManual) {
                                "${detectedFood.manualGrams} g"
                            } else {
                                detectedFood.portionLabel
                            }

                            foodRepository.addMeal(
                                email = email,
                                foodName = nutrition.name,
                                calories = itemCalories.toDouble(),
                                protein = itemProtein,
                                fat = itemFat,
                                carbs = itemCarbs,
                                quantity = detectedFood.multiplier,
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
                        if (detectedFoodList.none { it.nutrition.name.lowercase() == nutrition.name.lowercase() }) {
                            detectedFoodList.add(DetectedFood(nutrition))
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
                                if (detectedFoodList.none { it.nutrition.name.lowercase() == nutrition.name.lowercase() }) {
                                    detectedFoodList.add(DetectedFood(nutrition))
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