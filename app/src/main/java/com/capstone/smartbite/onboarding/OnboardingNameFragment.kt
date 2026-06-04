package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingNameBinding

class OnboardingNameFragment : Fragment() {

    private var _binding: FragmentOnboardingNameBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext())
        val user = userPreference.getUser()
        
        // Pre-fill if name already exists from Google Login
        if (!user.name.isNullOrEmpty()) {
            binding.edtName.setText(user.name)
        }

        binding.btnNext.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            if (name.isNotEmpty()) {
                user.name = name
                userPreference.setUser(user)
                (activity as? OnboardingActivity)?.nextStep()
            } else {
                binding.tilName.error = "Nama tidak boleh kosong"
            }
        }

        binding.btnSkip.setOnClickListener {
            (activity as? OnboardingActivity)?.nextStep()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}