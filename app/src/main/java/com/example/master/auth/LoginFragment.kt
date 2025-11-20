package com.example.master.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.example.master.MasterApplication
import com.example.master.R
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class LoginFragment : Fragment() {
    
    private lateinit var viewModel: AuthViewModel
    private val credentialManager by lazy { CredentialManager.create(requireContext()) }
    
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
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToRegister = {
                            findNavController().navigate(R.id.action_login_to_register)
                        },
                        onLoginSuccess = {
                            findNavController().navigate(R.id.action_login_to_home)
                        },
                        onGoogleSignIn = {
                            launchGoogleSignIn()
                        }
                    )
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = kotlin.runCatching {
                credentialManager.getCredential(requireContext(), request)
            }
            result.onSuccess { credResult ->
                val tokenResult = kotlin.runCatching {
                    GoogleIdTokenCredential.createFrom(credResult.credential.data).idToken
                }
                tokenResult.onSuccess { idToken ->
                    if (!idToken.isNullOrEmpty()) {
                        viewModel.signInWithGoogle(idToken)
                    } else {
                        viewModel.reportError("Unable to sign in with Google")
                    }
                }.onFailure { e ->
                    viewModel.reportError(e.localizedMessage ?: "Unable to sign in with Google")
                }
            }.onFailure { e ->
                viewModel.reportError(e.localizedMessage ?: "Unable to sign in with Google")
            }
        }
    }
}
