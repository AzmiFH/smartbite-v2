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
        val root: View = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = systemBars.top)
            insets
        }

        dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        adapter = DashboardAdapter { _ ->
            // Handle item click
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        dashboardViewModel.food.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        dashboardViewModel.isLoading.observe(viewLifecycleOwner) {
            showLoading(it)
        }
        dashboardViewModel.loadActiveEvents()

        setupProgressPager()

        return root
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

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Display user's name if signed in
        val auth = Firebase.auth
        val user = auth.currentUser

        if (user != null) {
            val userName = user.displayName
            binding.tvGreeting.text = "Halo, $userName!"
        } else {
            binding.tvGreeting.text = "Halo, Guest!"
        }

        setupCurrentDate()
    }

    private fun setupCurrentDate() {
        val calendar = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID"))
        val formattedDate = dateFormat.format(calendar)
        binding.tvCurrentDate.text = formattedDate
    }
}