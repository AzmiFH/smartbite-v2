package com.capstone.smartbite.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.capstone.smartbite.R
import com.capstone.smartbite.databinding.FragmentDashboardBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private lateinit var dashboardViewModel: DashboardViewModel
    private val binding get() = _binding!!
    private lateinit var adapter: DashboardAdapter
    private lateinit var progressPagerAdapter: ProgressPagerAdapter

    private var lastTargetCals = 0
    private var lastConsumedCals = 0

    private lateinit var mAuth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupProgressPager() {
        progressPagerAdapter = ProgressPagerAdapter()
        binding.vpDailyProgress.adapter = progressPagerAdapter

        binding.vpDailyProgress.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
            }
        })
    }

    private fun updateDots(position: Int) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_black_bold)
        val inactiveColor = Color.parseColor("#E0E0E0")

        binding.dot1.backgroundTintList = ColorStateList.valueOf(if (position == 0) activeColor else inactiveColor)
        binding.dot2.backgroundTintList = ColorStateList.valueOf(if (position == 1) activeColor else inactiveColor)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = systemBars.top)
            insets
        }

        dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
        mAuth = FirebaseAuth.getInstance()
        
        setupCurrentDate()
        setupRecyclerView()
        setupProgressPager()
        setupObservers()

        binding.tvSeeAll.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("target_cal", lastTargetCals)
                putInt("consumed_cal", lastConsumedCals)
            }
            findNavController().navigate(R.id.navigation_recipe_list, bundle)
        }

        val user = Firebase.auth.currentUser
        if (user != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val userPreference = com.capstone.smartbite.UserPreference(requireContext(), user.email)
                val userModel = userPreference.getUser()
                
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    val userName = userModel.name ?: user.displayName ?: "User"
                    binding.tvGreeting.text = getString(R.string.greeting_halo, userName)
                    
                    dashboardViewModel.setUserEmail(user.email!!)
                    dashboardViewModel.calculateDailyTargets(userModel)
                    setupBMIStatus(userModel)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter { _ -> }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        dashboardViewModel.food.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(it)
            }
        }
        dashboardViewModel.personalizedMeals.observe(viewLifecycleOwner) {
            populatePersonalizedMeals(it)
        }
        dashboardViewModel.isLoading.observe(viewLifecycleOwner) {
            showLoading(it)
        }
        dashboardViewModel.dailyNutrition.observe(viewLifecycleOwner) {
            progressPagerAdapter.setDailyNutrition(it)
            lastTargetCals = it.calories
            checkAndFetchMeals()
        }
        dashboardViewModel.consumedNutrition.observe(viewLifecycleOwner) {
            it?.let { log ->
                progressPagerAdapter.setConsumedNutrition(log)
                lastConsumedCals = log.calories
                checkAndFetchMeals()
            }
        }
        dashboardViewModel.weeklyNutrition.observe(viewLifecycleOwner) {
            progressPagerAdapter.setWeeklyNutrition(it)
        }
        dashboardViewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { msg ->
                android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndFetchMeals() {
        if (lastTargetCals > 0) {
            dashboardViewModel.fetchPersonalizedMeals(lastTargetCals, lastConsumedCals)
        }
    }

    private fun populatePersonalizedMeals(meals: List<com.capstone.smartbite.data.MealItem>) {
        if (meals.isEmpty()) return

        binding.llMealContainer.removeAllViews()

        meals.forEach { meal ->
            val cardBinding = com.capstone.smartbite.databinding.ItemPersonalizedMealBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.llMealContainer,
                false
            )

            cardBinding.tvMealName.text = meal.title
            cardBinding.tvMealCal.text = meal.calories.toString()
            cardBinding.tvMealDesc.text = meal.description
            cardBinding.tvMealTag.text = meal.tag
            cardBinding.tvMealMacros.text = getString(R.string.macros_format, 
                meal.macros.protein, meal.macros.carbs, meal.macros.fat)
            
            Glide.with(this).load(meal.imageUrl).into(cardBinding.ivMealImg)

            cardBinding.root.setOnClickListener { navigateToDetail(meal) }
            
            cardBinding.btnAddMeal.setOnClickListener {
                dashboardViewModel.addMealToHistory(meal)
            }
            
            binding.llMealContainer.addView(cardBinding.root)
        }
    }

    private fun navigateToDetail(meal: com.capstone.smartbite.data.MealItem) {
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

    private fun setupBMIStatus(user: com.capstone.smartbite.UserModel) {
        if (user.height > 0 && user.weight > 0) {
            val bmi = com.capstone.smartbite.utils.HealthMath.calculateBMI(user.weight.toDouble(), user.height)
            val category = com.capstone.smartbite.utils.HealthMath.getBMICategory(bmi)

            binding.tvBmiWeightVal.text = user.weight.toString()
            binding.tvBmiValLabel.text = getString(R.string.bmi_label, bmi)
            
            binding.tvBmiCategory.text = when (category) {
                "Underweight" -> getString(R.string.bmi_underweight)
                "Normal" -> getString(R.string.bmi_normal)
                "Overweight" -> getString(R.string.bmi_overweight)
                "Obese" -> getString(R.string.bmi_obese)
                else -> category
            }

            val minBMI = 15f
            val maxBMI = 30f
            val bias = ((bmi.toFloat() - minBMI) / (maxBMI - minBMI)).coerceIn(0f, 1f)
            
            val params = binding.ivBmiThumb.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.horizontalBias = bias
            binding.ivBmiThumb.layoutParams = params

            val colorStart = android.graphics.Color.parseColor("#80FF80")
            val colorCenter = android.graphics.Color.parseColor("#8080FF")
            val colorEnd = android.graphics.Color.parseColor("#FF4040")

            val categoryColor = if (bias < 0.5f) {
                val t = bias / 0.5f
                interpolateColor(colorStart, colorCenter, t)
            } else {
                val t = (bias - 0.5f) / 0.5f
                interpolateColor(colorCenter, colorEnd, t)
            }
            
            binding.tvBmiCategory.setTextColor(categoryColor)
        } else {
            binding.tvBmiWeightVal.text = "--"
            binding.tvBmiValLabel.text = getString(R.string.bmi_label, 0.0)
            binding.tvBmiCategory.text = getString(R.string.bmi_data_incomplete)
            
            val params = binding.ivBmiThumb.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.horizontalBias = 0.5f
            binding.ivBmiThumb.layoutParams = params
            binding.tvBmiCategory.setTextColor(android.graphics.Color.GRAY)
        }
    }

    private fun interpolateColor(colorStart: Int, colorEnd: Int, fraction: Float): Int {
        val startA = Color.alpha(colorStart)
        val startR = Color.red(colorStart)
        val startG = Color.green(colorStart)
        val startB = Color.blue(colorStart)

        val endA = Color.alpha(colorEnd)
        val endR = Color.red(colorEnd)
        val endG = Color.green(colorEnd)
        val endB = Color.blue(colorEnd)

        return Color.argb(
            (startA + (endA - startA) * fraction).toInt(),
            (startR + (endR - startR) * fraction).toInt(),
            (startG + (endG - startG) * fraction).toInt(),
            (startB + (endB - startB) * fraction).toInt()
        )
    }

    private fun setupCurrentDate() {
        val email = mAuth.currentUser?.email
        val userPreference = com.capstone.smartbite.UserPreference(requireContext(), email)
        val langCode = userPreference.getLanguage()
        
        val dayName = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> getString(R.string.monday)
            Calendar.TUESDAY -> getString(R.string.tuesday)
            Calendar.WEDNESDAY -> getString(R.string.wednesday)
            Calendar.THURSDAY -> getString(R.string.thursday)
            Calendar.FRIDAY -> getString(R.string.friday)
            Calendar.SATURDAY -> getString(R.string.saturday)
            Calendar.SUNDAY -> getString(R.string.sunday)
            else -> ""
        }
        
        val monthName = when (Calendar.getInstance().get(Calendar.MONTH)) {
            Calendar.JANUARY -> getString(R.string.january)
            Calendar.FEBRUARY -> getString(R.string.february)
            Calendar.MARCH -> getString(R.string.march)
            Calendar.APRIL -> getString(R.string.april)
            Calendar.MAY -> getString(R.string.may)
            Calendar.JUNE -> getString(R.string.june)
            Calendar.JULY -> getString(R.string.july)
            Calendar.AUGUST -> getString(R.string.august)
            Calendar.SEPTEMBER -> getString(R.string.september)
            Calendar.OCTOBER -> getString(R.string.october)
            Calendar.NOVEMBER -> getString(R.string.november)
            Calendar.DECEMBER -> getString(R.string.december)
            else -> ""
        }
        
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        
        val formattedDate = if (langCode == "in") {
            "$dayName, $dayOfMonth $monthName $year"
        } else {
            "$dayName, $monthName $dayOfMonth, $year"
        }

        binding.tvCurrentDate.text = formattedDate
    }
}
