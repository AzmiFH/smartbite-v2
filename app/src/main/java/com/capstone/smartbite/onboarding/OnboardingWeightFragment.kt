package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingWeightBinding

class OnboardingWeightFragment : Fragment() {

    private var _binding: FragmentOnboardingWeightBinding? = null
    private val binding get() = _binding!!
    private lateinit var userPreference: UserPreference

    // Karena RulerKit menggunakan Float, kita siapkan variabel pembantu
    private var currentSelectedWeight: Int = WEIGHT_DEFAULT

    companion object {
        const val WEIGHT_MIN = 20
        const val WEIGHT_MAX = 300
        const val WEIGHT_DEFAULT = 60
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(
            requireContext(),
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
        )

        val savedWeight = userPreference.getUser().weight
        currentSelectedWeight = if (savedWeight in WEIGHT_MIN..WEIGHT_MAX) savedWeight else WEIGHT_DEFAULT

        setupRulerPicker()
        setupContinueButton()
    }

    private fun setupRulerPicker() {
        // Tampilkan data awal ke Text kita yang besar
        updateDisplay(currentSelectedWeight)

        // 1. Pantau perubahan angka saat penggaris sedang digeser (real-time)
        binding.weightRulerPicker.onValueChanged = { value, _ ->
            // Mengubah nilai float dari library kembali menjadi Integer
            currentSelectedWeight = value.toInt()
            updateDisplay(currentSelectedWeight)
        }

        // 2. (Opsional) Memantau angka akhir saat geseran sudah berhenti
        binding.weightRulerPicker.onScrollEnd = { value, _ ->
            currentSelectedWeight = value.toInt()
            updateDisplay(currentSelectedWeight)
        }
    }

    private fun updateDisplay(value: Int) {
        // Ubah angka besar di atas
        binding.tvWeightValue.text = value.toString()

        binding.tvWarning.visibility = when {
            value < 30 -> {
                binding.tvWarning.text = "Nilai ini sangat rendah, pastikan sudah akurat."
                View.VISIBLE
            }
            value > 200 -> {
                binding.tvWarning.text = "Nilai ini sangat tinggi, pastikan sudah akurat."
                View.VISIBLE
            }
            else -> View.GONE
        }
    }

    private fun setupContinueButton() {
        binding.btnNext.setOnClickListener {
            if (currentSelectedWeight in WEIGHT_MIN..WEIGHT_MAX) {
                val currentUser = userPreference.getUser()
                currentUser.weight = currentSelectedWeight
                userPreference.setUser(currentUser)
                (activity as? OnboardingNavigator)?.nextStep()
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Berat badan harus antara $WEIGHT_MIN - $WEIGHT_MAX kg",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}