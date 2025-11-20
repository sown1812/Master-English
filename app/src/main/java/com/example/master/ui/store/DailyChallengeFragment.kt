package com.example.master.ui.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.master.MasterApplication
import com.example.master.databinding.FragmentStoreBinding

class DailyChallengeFragment : Fragment() {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val app = requireActivity().application as MasterApplication
        val factory = StoreViewModelFactory(app.repository, app.authManager, app.gameStateStore, app.apiService)
        val viewModel = ViewModelProvider(this, factory)[StoreViewModel::class.java]

        _binding = FragmentStoreBinding.inflate(inflater, container, false)

        binding.composeStore.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    DailyChallengeScreen(
                        stateFlow = viewModel.uiState,
                        onStart = viewModel::startDailyChallenge,
                        onSubmit = { viewModel.submitDailyChallenge(score = 50) }
                    )
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
