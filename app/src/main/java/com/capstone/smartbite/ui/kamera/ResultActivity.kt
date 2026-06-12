package com.capstone.smartbite.ui.kamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.capstone.smartbite.R
import com.capstone.smartbite.data.FileUploadResponse
import com.capstone.smartbite.databinding.ActivityResultBinding
import com.dewakoding.androidchartjs.util.ChartType
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val result = intent.getSerializableExtra("result") as? FileUploadResponse
        val imageUri = intent.getStringExtra("imageUri")?.toUri()

        // Tampilkan gambar
        imageUri?.let {
            binding.previewImageView.setImageURI(it)
        }

        // Tampilkan data jika result tersedia
        result?.let {
            val nutrition = it.nutrition

            binding.tvFoodName.text = it.food
            binding.tvTagCategory.text = "FOOD" // Default or dynamic if available
            binding.tvCalories.text = "Total ${nutrition.calories} kcal"
            
            val protein = nutrition.proteins.toString().toFloatOrNull() ?: 0f
            val fat = nutrition.fat.toString().toFloatOrNull() ?: 0f
            val carbs = nutrition.carbohydrate.toString().toFloatOrNull() ?: 0f

            binding.tvProteinVal.text = String.format("%.1fg", protein)
            binding.tvFatVal.text = String.format("%.1fg", fat)
            binding.tvCarbsVal.text = String.format("%.1fg", carbs)

            // Hitung progress (Sederhana: 100g sebagai 100%)
            binding.progressProtein.progress = (protein * 2).toInt().coerceAtMost(100)
            binding.progressFat.progress = (fat * 2).toInt().coerceAtMost(100)
            binding.progressCarbs.progress = (carbs * 2).toInt().coerceAtMost(100)

        } ?: Log.e("ResultActivity", "No result received!")

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClose.setOnClickListener {
            // Sinyal untuk pindah ke dashboard
            setResult(RESULT_GO_TO_DASHBOARD)
            finish()
        }
        binding.btnRetake.setOnClickListener {
            // Kirim balik data lama agar bisa dibuka kembali jika kamera di-cancel
            val intent = Intent()
            intent.putExtra("last_result", result)
            intent.putExtra("last_imageUri", imageUri.toString())
            setResult(RESULT_RETAKE, intent)
            finish()
        }
        binding.btnAddMeal.setOnClickListener {
            val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            if (email != null && result != null) {
                val nutrition = result.nutrition
                val calories = nutrition.calories.toString().toDoubleOrNull() ?: 0.0
                val protein = nutrition.proteins.toString().toDoubleOrNull() ?: 0.0
                val fat = nutrition.fat.toString().toDoubleOrNull() ?: 0.0
                val carbs = nutrition.carbohydrate.toString().toDoubleOrNull() ?: 0.0

                lifecycleScope.launch {
                    try {
                        com.capstone.smartbite.data.FirebaseService().addMealLog(
                            email, calories, protein, carbs, fat
                        )
                        android.widget.Toast.makeText(this@ResultActivity, "Berhasil menambahkan makanan", android.widget.Toast.LENGTH_SHORT).show()
                        setResult(RESULT_GO_TO_DASHBOARD)
                        finish()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@ResultActivity, "Gagal menyimpan: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
