package com.capstone.smartbite.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.capstone.smartbite.R
import com.capstone.smartbite.data.ApiConfig
import com.capstone.smartbite.data.MealDetail
import com.capstone.smartbite.databinding.FragmentRecipeDetailBinding
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val mealId = arguments?.getString("meal_id")
        val mealTitle = arguments?.getString("meal_title")
        val mealCal = arguments?.getInt("meal_cal") ?: 0
        val mealImg = arguments?.getString("meal_img")
        val mealDesc = arguments?.getString("meal_desc")
        val mealProt = arguments?.getDouble("meal_prot") ?: 0.0
        val mealCarb = arguments?.getDouble("meal_carb") ?: 0.0
        val mealFat = arguments?.getDouble("meal_fat") ?: 0.0
        
        // Initial populate from Bundle
        binding.txtToolbarTitle.text = mealTitle ?: "Detail Resep"
        binding.txtMealTitle.text = mealTitle
        binding.txtCaloriesVal.text = if (mealCal > 0) "$mealCal kcal" else "-- kcal"
        binding.txtMealDesc.text = mealDesc

        binding.txtProteinVal.text = String.format(java.util.Locale.US, "%.1fg", mealProt)
        binding.txtCarbsVal.text = String.format(java.util.Locale.US, "%.1fg", mealCarb)
        binding.txtFatVal.text = String.format(java.util.Locale.US, "%.1fg", mealFat)

        if (mealImg != null) {
            Glide.with(this).load(mealImg).placeholder(R.drawable.ic_gallery).into(binding.imgMealBanner)
        }

        // Fetch deep details from FastAPI
        if (mealId != null) {
            fetchMealDetail(mealId)
        }
    }

    private fun fetchMealDetail(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService().getMealDetail(id)
                if (response.status == "success") {
                    updateDetailUI(response.data)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetail", "Error: ${e.message}")
            }
        }
    }

    private fun updateDetailUI(detail: MealDetail) {
        // Update basic info if returned by API
        detail.tag?.let { binding.txtMealTag.text = it.uppercase() }
        detail.description?.let { binding.txtMealDesc.text = it }
        detail.calories?.let { binding.txtCaloriesVal.text = "$it kcal" }

        // Update Prep Info
        if (detail.yields != null || detail.prepTime != null || detail.cookTime != null) {
            binding.layoutPrepCard.visibility = View.VISIBLE
            binding.txtYieldsVal.text = detail.yields?.toString() ?: "--"
            binding.txtPrepTimeVal.text = detail.prepTime ?: "--"
            binding.txtCookTimeVal.text = detail.cookTime ?: "--"
        }

        // Update Ingredients
        binding.layoutIngredientsContainer.removeAllViews()
        detail.ingredients.forEach { ingredient ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_ingredient, binding.layoutIngredientsContainer, false)
            row.findViewById<TextView>(R.id.txt_ingredient_name).text = ingredient
            binding.layoutIngredientsContainer.addView(row)
        }

        // Update Instructions
        binding.layoutInstructionsContainer.removeAllViews()
        
        // Memastikan instruksi diproses per langkah
        val instructionList = detail.instructions
        
        instructionList.forEachIndexed { index, step ->
            if (step.isNotBlank()) {
                val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_instruction, binding.layoutInstructionsContainer, false)
                
                // Set nomor urut (1, 2, 3...)
                val tvNumber = row.findViewById<TextView>(R.id.txt_step_number)
                tvNumber.text = (index + 1).toString()
                
                // Set teks instruksi
                val tvDesc = row.findViewById<TextView>(R.id.txt_step_desc)
                tvDesc.text = step.trim()
                
                binding.layoutInstructionsContainer.addView(row)
            }
        }
        
        // Update Macros if available
        detail.macros?.let {
            binding.txtProteinVal.text = String.format(java.util.Locale.US, "%.1fg", it.protein)
            binding.txtCarbsVal.text = String.format(java.util.Locale.US, "%.1fg", it.carbs)
            binding.txtFatVal.text = String.format(java.util.Locale.US, "%.1fg", it.fat)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
