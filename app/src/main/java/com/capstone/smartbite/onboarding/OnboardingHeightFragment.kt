package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingHeightBinding

class OnboardingHeightFragment : Fragment() {

    private var _binding: FragmentOnboardingHeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference
    private var currentSelectedHeight: Int = HEIGHT_DEFAULT

    companion object {
        const val HEIGHT_MIN = 100
        const val HEIGHT_MAX = 250
        const val HEIGHT_DEFAULT = 160
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingHeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(
            requireContext(),
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
        )

        val savedHeight = userPreference.getUser().height
        currentSelectedHeight = if (savedHeight in HEIGHT_MIN..HEIGHT_MAX) savedHeight else HEIGHT_DEFAULT

        setupRulerPicker()

        binding.btnNext.setOnClickListener {
            if (currentSelectedHeight in HEIGHT_MIN..HEIGHT_MAX) {
                val currentUser = userPreference.getUser()
                currentUser.height = currentSelectedHeight
                userPreference.setUser(currentUser)
                (activity as? OnboardingNavigator)?.nextStep()
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    getString(R.string.onboarding_height_range_error, HEIGHT_MIN, HEIGHT_MAX),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRulerPicker() {
        // Tampilkan angka awal ke TextView besar
        binding.tvHeightValue.text = currentSelectedHeight.toString()

        binding.heightRulerPicker.apply {
            // Listener saat penggaris sedang digeser secara real-time
            onValueChanged = { value, _ ->
                currentSelectedHeight = value.toInt()
                binding.tvHeightValue.text = currentSelectedHeight.toString()
            }

            // Listener saat penggaris berhenti digeser
            onScrollEnd = { value, _ ->
                currentSelectedHeight = value.toInt()
                binding.tvHeightValue.text = currentSelectedHeight.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}