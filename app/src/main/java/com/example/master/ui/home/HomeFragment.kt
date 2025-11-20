package com.example.master.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.master.R
import com.example.master.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.composeHome.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    HomeRoute(homeViewModel = homeViewModel)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                homeViewModel.navigationEvents.collect { event ->
                    when (event) {
                        is HomeNavigationEvent.NavigateToPlay -> {
                            Toast.makeText(requireContext(), "Bắt đầu level ${event.level}", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_home_to_dashboard)
                        }
						is HomeNavigationEvent.NavigateToDailyChallenge -> {
							Toast.makeText(requireContext(), "Mở thử thách: ${event.challengeTitle}", Toast.LENGTH_SHORT).show()
							findNavController().navigate(R.id.action_home_to_daily)
						}
                        HomeNavigationEvent.NavigateToAchievements -> {
                            findNavController().navigate(R.id.action_home_to_dashboard)
                        }
                        HomeNavigationEvent.NavigateToStore -> {
                            findNavController().navigate(R.id.navigation_store)
                        }
                        is HomeNavigationEvent.NavigateToQuest -> {
                            Toast.makeText(requireContext(), "Quest: ${event.quest.title}", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.navigation_store)
                        }
                        is HomeNavigationEvent.NavigateToBooster -> {
                            Toast.makeText(requireContext(), event.booster.title, Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.navigation_store)
                        }
                        is HomeNavigationEvent.ThemeApplied -> {
                            Toast.makeText(requireContext(), "Áp dụng chủ đề ${event.themeName}", Toast.LENGTH_SHORT).show()
                        }
                        is HomeNavigationEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
