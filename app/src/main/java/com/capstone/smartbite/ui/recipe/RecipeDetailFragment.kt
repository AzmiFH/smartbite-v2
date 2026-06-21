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
        
        // Initial populate from Bundle
        binding.txtToolbarTitle.text = mealTitle ?: "Detail Resep"
        binding.txtMealTitle.text = mealTitle
        binding.txtCaloriesVal.text = if (mealCal > 0) "$mealCal kcal" else "-- kcal"
        binding.txtMealDesc.text = mealDesc

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
        // Update Ingredients
        binding.layoutIngredientsContainer.removeAllViews()
        detail.ingredients.forEach { ingredient ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_ingredient, binding.layoutIngredientsContainer, false)
            row.findViewById<TextView>(R.id.txt_ingredient_name).text = ingredient
            binding.layoutIngredientsContainer.addView(row)
        }

        // Update Instructions
        binding.layoutInstructionsContainer.removeAllViews()
        detail.instructions.forEachIndexed { index, step ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_instruction, binding.layoutInstructionsContainer, false)
            row.findViewById<TextView>(R.id.txt_step_number).text = (index + 1).toString()
            row.findViewById<TextView>(R.id.txt_step_desc).text = step
            binding.layoutInstructionsContainer.addView(row)
        }
        
        // Update Macros if available
        detail.macros?.let {
            binding.txtProteinVal.text = "${it.protein}g"
            binding.txtCarbsVal.text = "${it.carbs}g"
            binding.txtFatVal.text = "${it.fat}g"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
