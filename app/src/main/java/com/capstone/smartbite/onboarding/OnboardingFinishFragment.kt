package com.capstone.smartbite.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.capstone.smartbite.MainActivity
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.databinding.FragmentOnboardingFinishBinding
import kotlinx.coroutines.launch

class OnboardingFinishFragment : Fragment() {

    private var _binding: FragmentOnboardingFinishBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingFinishBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGetStarted.setOnClickListener {
            val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            val userPreference = UserPreference(requireContext(), email)
            userPreference.setOnboardingFinished(true)
            
            // Get the absolute latest data before syncing to Cloud
            val finalUser = userPreference.getUser()
            
            lifecycleScope.launch {
                try {
                    FirebaseService().saveUserProfile(finalUser)
                } catch (e: Exception) {
                    // Log error or show toast if needed
                }

                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}