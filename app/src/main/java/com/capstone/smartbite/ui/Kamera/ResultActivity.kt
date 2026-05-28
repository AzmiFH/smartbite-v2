package com.capstone.smartbite.ui.Kamera

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.capstone.smartbite.R
import com.capstone.smartbite.data.FileUploadResponse
import com.capstone.smartbite.databinding.ActivityResultBinding
import com.dewakoding.androidchartjs.util.ChartType

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val result = intent.getSerializableExtra("result") as? FileUploadResponse
        val imageUri = intent.getStringExtra("imageUri")?.toUri()

        // Tampilkan gambar
        imageUri?.let {
            findViewById<ImageView>(R.id.previewImageView).setImageURI(it)
        }

        // Tampilkan chart dan teks jika result tersedia
        result?.let {
            val nutrition = it.nutrition

            // Pie chart
            binding.androidChart1.setChart(
                ChartType.PIE,
                arrayOf("Calories", "Protein", "Fat", "Carbohydrates"),
                arrayOf(
                    nutrition.calories.toFloat(),
                    nutrition.proteins.toString().toFloatOrNull() ?: 0f,
                    nutrition.fat.toString().toFloatOrNull() ?: 0f,
                    nutrition.carbohydrate.toString().toFloatOrNull() ?: 0f
                ).map { value -> value.toInt() }.toTypedArray(),
                "of quantity"
            )

            // Result text
            val resultText = String.format(
                "Food: %s\nCalories: %d\nProtein: %.2fg\nFat: %.2fg\nCarbohydrates: %.2fg",
                it.food,
                nutrition.calories,
                nutrition.proteins.toString().toFloatOrNull() ?: 0f,
                nutrition.fat.toString().toFloatOrNull() ?: 0f,
                nutrition.carbohydrate.toString().toFloatOrNull() ?: 0f
            )

            findViewById<TextView>(R.id.resultTextView).text = resultText

        } ?: Log.e("ResultActivity", "No result received!")
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
    }
}
