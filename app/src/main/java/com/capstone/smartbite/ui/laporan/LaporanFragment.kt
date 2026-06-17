package com.capstone.smartbite.ui.laporan

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.databinding.FragmentLaporanBinding
import com.capstone.smartbite.databinding.ItemCalendarDayBinding

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCalendar()
        setupDummyData()
    }

    private fun setupCalendar() {
        val days = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        val dates = listOf("14", "15", "16", "17", "18", "19", "20")
        val dayViews = listOf(
            binding.day1, binding.day2, binding.day3, 
            binding.day4, binding.day5, binding.day6, binding.day7
        )

        for (i in dayViews.indices) {
            val dayBinding = ItemCalendarDayBinding.bind(dayViews[i].root)
            dayBinding.tvDayName.text = days[i]
            dayBinding.tvDayDate.text = dates[i]

            // Highlight Wednesday (17) as selected
            if (dates[i] == "17") {
                dayBinding.tvDayDate.setBackgroundResource(R.drawable.bg_circle_primary)
                dayBinding.tvDayDate.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.ring_orange)
                dayBinding.tvDayDate.setTextColor(Color.WHITE)
            }

            // Dummy progress for mini rings
            dayBinding.cpMiniOuter.progress = (40..90).random()
            dayBinding.cpMiniMiddle.progress = (30..80).random()
            dayBinding.cpMiniInner.progress = (20..70).random()
            
            // Grey out future days (after 17)
            if (dates[i].toInt() > 17) {
                dayBinding.cpMiniOuter.setIndicatorColor(Color.LTGRAY)
                dayBinding.cpMiniMiddle.setIndicatorColor(Color.LTGRAY)
                dayBinding.cpMiniInner.setIndicatorColor(Color.LTGRAY)
                dayBinding.cpMiniOuter.trackColor = Color.parseColor("#F5F5F5")
                dayBinding.cpMiniMiddle.trackColor = Color.parseColor("#F5F5F5")
                dayBinding.cpMiniInner.trackColor = Color.parseColor("#F5F5F5")
            }
        }
    }

    private fun setupDummyData() {
        // Main Rings
        binding.cpRingOuter.progress = 75
        binding.cpRingMiddle.progress = 60
        binding.cpRingInner.progress = 45
        
        binding.tvConsumedCalBig.text = "1500"
        binding.tvTargetCalLabel.text = "/ 2000 kcal"

        // Macro Circles
        binding.pbLaporanCarbs.progress = 80
        binding.tvCarbVal.text = "95"
        binding.tvCarbTarget.text = "/ 120g"

        binding.pbLaporanProtein.progress = 80
        binding.tvProteinVal.text = "200"
        binding.tvProteinTarget.text = "/ 250g"

        binding.pbLaporanFat.progress = 60
        binding.tvFatVal.text = "40"
        binding.tvFatTarget.text = "/ 65g"

        // Weekly Calories Chart
        binding.tvAverageCalVal.text = "Rata-rata harian: 49 kkal"
        
        // Setting bar heights (Slicing)
        binding.barMin.layoutParams.height = dpToPx(20)
        binding.barSen.layoutParams.height = dpToPx(70)
        binding.barSel.layoutParams.height = dpToPx(15)
        binding.barRab.layoutParams.height = dpToPx(100)
        binding.barKam.layoutParams.height = dpToPx(0)
        binding.barJum.layoutParams.height = dpToPx(0)
        binding.barSab.layoutParams.height = dpToPx(0)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}