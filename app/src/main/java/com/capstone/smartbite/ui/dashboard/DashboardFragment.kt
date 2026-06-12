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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.capstone.smartbite.R
import com.capstone.smartbite.databinding.FragmentDashboardBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private lateinit var dashboardViewModel: DashboardViewModel
    private val binding get() = _binding!!
    private lateinit var adapter: DashboardAdapter
    private lateinit var progressPagerAdapter: ProgressPagerAdapter

    private lateinit var mGoogleSignInClient: GoogleSignInClient
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
        
        // 1. Inisialisasi UI dasar SEGERA
        setupCurrentDate()
        setupRecyclerView()
        setupProgressPager()
        setupObservers()

        // 2. Muat data dari cache/local secara asinkron tanpa delay buatan
        val user = Firebase.auth.currentUser
        if (user != null) {
            // Gunakan Default Dispatcher untuk kalkulasi agar tidak membebani Main Thread
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val userPreference = com.capstone.smartbite.UserPreference(requireContext(), user.email)
                val userModel = userPreference.getUser()
                
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    val userName = userModel.name ?: user.displayName ?: "User"
                    binding.tvGreeting.text = "Halo, $userName!"
                    
                    // Trigger data flow
                    dashboardViewModel.setUserEmail(user.email!!)
                    dashboardViewModel.calculateDailyTargets(userModel)
                    setupBMIStatus(userModel)
                }
            }
        }
        
        // 3. Konfigurasi Google Sign-In di latar belakang (Low Priority)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            withContext(Dispatchers.Main) {
                mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
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
        dashboardViewModel.isLoading.observe(viewLifecycleOwner) {
            showLoading(it)
        }
        dashboardViewModel.dailyNutrition.observe(viewLifecycleOwner) {
            progressPagerAdapter.setDailyNutrition(it)
        }
        dashboardViewModel.consumedNutrition.observe(viewLifecycleOwner) {
            it?.let { log ->
                progressPagerAdapter.setConsumedNutrition(log)
            }
        }
    }

    private fun setupBMIStatus(user: com.capstone.smartbite.UserModel) {
        if (user.height > 0 && user.weight > 0) {
            val bmi = com.capstone.smartbite.utils.HealthMath.calculateBMI(user.weight.toDouble(), user.height)
            val category = com.capstone.smartbite.utils.HealthMath.getBMICategory(bmi)

            binding.tvInsightTitle.text = "BMI Status"
            binding.tvInsightDesc.text = "BMI Anda saat ini adalah ${String.format("%.1f", bmi)} ($category). Terus jaga pola makanmu!"

            val (colorRes, bgColor) = when (category) {
                "Underweight" -> Pair(R.color.progress_carbs, "#FFF9C4")
                "Normal" -> Pair(R.color.brand_green, "#E8F5E9")
                "Overweight" -> Pair(R.color.progress_cal, "#FFF3E0")
                "Obese" -> Pair(R.color.progress_cal, "#FFEBEE")
                else -> Pair(R.color.brand_green, "#E8F5E9")
            }

            val color = ContextCompat.getColor(requireContext(), colorRes)
            binding.tvInsightTitle.setTextColor(color)
            binding.ivInsightIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.ivInsightIcon.imageTintList = ColorStateList.valueOf(color)
        } else {
            binding.tvInsightTitle.text = "BMI Status"
            binding.tvInsightDesc.text = "Lengkapi data profilmu untuk melihat status BMI."
        }
    }

    private fun setupCurrentDate() {
        val calendar = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID"))
        val formattedDate = dateFormat.format(calendar)
        binding.tvCurrentDate.text = formattedDate
    }
}