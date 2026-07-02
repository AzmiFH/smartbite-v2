package com.capstone.smartbite.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.capstone.smartbite.R
import com.capstone.smartbite.data.MealItem
import com.capstone.smartbite.databinding.FragmentRecipeListBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth

class RecipeListFragment : Fragment() {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RecipeListViewModel
    private lateinit var adapter: RecipeGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[RecipeListViewModel::class.java]

        setupRecyclerView()
        setupFilters()
        setupObservers()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Get target and consumed calories from arguments (passed from Dashboard)
        val targetCal = arguments?.getInt("target_cal") ?: 2000
        val consumedCal = arguments?.getInt("consumed_cal") ?: 0
        
        viewModel.fetchAllRecipes(targetCal, consumedCal)
    }

    private fun setupRecyclerView() {
        adapter = RecipeGridAdapter(
            onDetailClick = { meal -> navigateToDetail(meal) },
            onAddClick = { meal -> 
                val email = FirebaseAuth.getInstance().currentUser?.email
                if (email != null) {
                    viewModel.addMealToHistory(meal, email)
                }
            }
        )
        binding.rvRecipes.adapter = adapter
        binding.rvRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun setupFilters() {
        binding.chipGroupMealType.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            val type = chip?.text?.toString() ?: "Semua Tipe"
            viewModel.setMealTypeFilter(type)
        }

        binding.chipGroupCalories.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            val category = chip?.text?.toString() ?: "Semua Kalori"
            viewModel.setCalorieFilter(category)
        }
    }

    private fun setupObservers() {
        viewModel.filteredMeals.observe(viewLifecycleOwner) { meals ->
            adapter.submitList(meals)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDetail(meal: MealItem) {
        val bundle = Bundle().apply {
            putString("meal_id", meal.id)
            putString("meal_title", meal.title)
            putInt("meal_cal", meal.calories)
            putString("meal_img", meal.imageUrl)
            putString("meal_desc", meal.description)
            putDouble("meal_prot", meal.macros.protein)
            putDouble("meal_carb", meal.macros.carbs)
            putDouble("meal_fat", meal.macros.fat)
        }
        findNavController().navigate(R.id.navigation_recipe_detail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
