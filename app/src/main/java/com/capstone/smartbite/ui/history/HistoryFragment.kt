package com.capstone.smartbite.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.smartbite.data.FoodRepository
import com.capstone.smartbite.databinding.FragmentHistoryBinding
import com.capstone.smartbite.ui.history.adapter.HistoryAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var foodRepository: FoodRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        foodRepository = FoodRepository(requireContext())
        setupRecyclerView()
        observeHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeHistory() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email != null) {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            viewLifecycleOwner.lifecycleScope.launch {
                foodRepository.getDailyHistory(email, dateString).collectLatest { list ->
                    if (list.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvHistory.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvHistory.visibility = View.VISIBLE
                        historyAdapter.submitList(list)
                    }
                }
            }
        } else {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
