package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingAgeBinding
import com.shawnlin.numberpicker.NumberPicker

class OnboardingAgeFragment : Fragment() {

    private var _binding: FragmentOnboardingAgeBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingAgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext())
        val user = userPreference.getUser()

        // Explicitly using the class to avoid confusion
        val agePicker = binding.agePicker as? NumberPicker

        if (user.age > 10) {
            agePicker?.value = user.age
        }

        binding.btnNext.setOnClickListener {
            user.age = agePicker?.value ?: 23
            userPreference.setUser(user)
            (activity as? OnboardingActivity)?.nextStep()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}