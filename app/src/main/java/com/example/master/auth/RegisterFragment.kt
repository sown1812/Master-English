package com.example.master.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.master.MasterApplication
import com.example.master.R

class RegisterFragment : Fragment() {
    
    private lateinit var viewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as MasterApplication
        viewModel = AuthViewModel(app.authManager)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme {
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = {
                            findNavController().popBackStack()
                        },
                        onRegisterSuccess = {
                            findNavController().navigate(R.id.action_register_to_home)
                        }
                    )
                }
            }
        }
    }
}
