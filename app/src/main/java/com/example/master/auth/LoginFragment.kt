package com.example.master.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.master.MasterApplication
import com.example.master.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginFragment : Fragment() {
    
    private lateinit var viewModel: AuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient
    private val facebookCallbackManager: CallbackManager = CallbackManager.Factory.create()
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    viewModel.reportError("Unable to sign in with Google")
                }
            } catch (e: ApiException) {
                viewModel.reportError(e.localizedMessage ?: "Unable to sign in with Google")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as MasterApplication
        viewModel = AuthViewModel(app.authManager)
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        
        LoginManager.getInstance().registerCallback(
            facebookCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    viewModel.signInWithFacebook(result.accessToken.token)
                }

                override fun onCancel() {
                    // Do nothing
                }

                override fun onError(error: FacebookException) {
                    viewModel.reportError(error.localizedMessage ?: "Unable to sign in with Facebook")
                }
            }
        )
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
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        onFacebookSignIn = {
                            startFacebookLogin()
                        },
                        onGuestSignIn = {
                            viewModel.continueAsGuest()
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        LoginManager.getInstance().unregisterCallback(facebookCallbackManager)
    }

    private fun startFacebookLogin() {
        LoginManager.getInstance()
            .logInWithReadPermissions(this, listOf("public_profile", "email"))
    }
}
