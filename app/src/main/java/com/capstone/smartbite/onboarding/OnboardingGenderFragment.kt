package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingGenderBinding

class OnboardingGenderFragment : Fragment() {

    private var _binding: FragmentOnboardingGenderBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference
    private var selectedGender: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGenderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext())
        val user = userPreference.getUser()

        // Pre-select if already exists
        if (!user.gender.isNullOrEmpty()) {
            updateUI(user.gender!!)
        }

        binding.btnMale.setOnClickListener {
            updateUI("Male")
        }

        binding.btnFemale.setOnClickListener {
            updateUI("Female")
        }

        binding.btnNext.setOnClickListener {
            if (selectedGender != null) {
                user.gender = selectedGender
                userPreference.setUser(user)
                (activity as? OnboardingActivity)?.nextStep()
            } else {
                Toast.makeText(requireContext(), "Silakan pilih jenis kelamin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(gender: String) {
        selectedGender = gender
        if (gender == "Male") {
            binding.btnMale.setBackgroundResource(R.drawable.bg_gender_card_selected)
            binding.ivCheckMale.setImageResource(R.drawable.ic_check_circle_filled)
            
            binding.btnFemale.setBackgroundResource(R.drawable.bg_gender_card_unselected)
            binding.ivCheckFemale.setImageResource(R.drawable.ic_check_circle_outline)
        } else {
            binding.btnFemale.setBackgroundResource(R.drawable.bg_gender_card_selected)
            binding.ivCheckFemale.setImageResource(R.drawable.ic_check_circle_filled)
            
            binding.btnMale.setBackgroundResource(R.drawable.bg_gender_card_unselected)
            binding.ivCheckMale.setImageResource(R.drawable.ic_check_circle_outline)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}