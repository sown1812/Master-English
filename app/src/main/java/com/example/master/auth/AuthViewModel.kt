package com.example.master.auth

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.core.user.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val authState = authManager.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    private val _uiState = MutableStateFlow(
        savedStateHandle[KEY_UI_STATE] ?: AuthUiState()
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun onEmailChanged(email: String) {
        updateUiState { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        updateUiState { it.copy(password = password, passwordError = null) }
    }

    fun onDisplayNameChanged(name: String) {
        updateUiState { it.copy(displayName = name, displayNameError = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        updateUiState { it.copy(confirmPassword = password, confirmPasswordError = null) }
    }

    fun signIn() {
        val state = _uiState.value

        // Validation
        if (!validateEmail(state.email)) {
            updateUiState { it.copy(emailError = "Invalid email address") }
            return
        }

        if (state.password.length < 6) {
            updateUiState { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            updateUiState {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginInProgress = AuthFlow.EMAIL
                )
            }

            when (val result = authManager.signIn(state.email, state.password)) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
            }
        }
    }

    fun signUp() {
        val state = _uiState.value

        // Validation
        if (state.displayName.isBlank()) {
            updateUiState { it.copy(displayNameError = "Name is required") }
            return
        }

        if (!validateEmail(state.email)) {
            updateUiState { it.copy(emailError = "Invalid email address") }
            return
        }

        if (state.password.length < 6) {
            updateUiState { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }

        if (state.password != state.confirmPassword) {
            updateUiState { it.copy(confirmPasswordError = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            updateUiState {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginInProgress = AuthFlow.NONE
                )
            }

            when (val result = authManager.signUp(
                state.email, 
                state.password, 
                state.displayName
            )) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            updateUiState {
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    loginInProgress = AuthFlow.GOOGLE
                )
            }

            when (val result = authManager.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
            }
        }
    }
    
    fun resetPassword() {
        val state = _uiState.value

        if (!validateEmail(state.email)) {
            updateUiState { it.copy(emailError = "Invalid email address") }
            return
        }

        viewModelScope.launch {
            updateUiState {
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    loginInProgress = AuthFlow.NONE
                )
            }

            when (val result = authManager.resetPassword(state.email)) {
                is AuthResult.Success -> {
                    updateUiState {
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset email sent!",
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    updateUiState {
                        it.copy(
                            isLoading = false, 
                            errorMessage = result.message,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        updateUiState { it.copy(errorMessage = null, successMessage = null) }
    }

    fun reportError(message: String) {
        updateUiState { 
            it.copy(
                isLoading = false,
                errorMessage = message,
                loginInProgress = AuthFlow.NONE
            ) 
        }
    }
    
    private suspend fun loadUserProfile() {
        _userProfile.value = authManager.getUserProfile()
    }
    
    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun updateUiState(transform: (AuthUiState) -> AuthUiState) {
        val newState = transform(_uiState.value)
        _uiState.value = newState
        savedStateHandle[KEY_UI_STATE] = newState.redactedForSave()
    }

    private fun AuthUiState.redactedForSave(): AuthUiState = copy(
        password = "",
        confirmPassword = "",
        isLoading = false,
        loginInProgress = AuthFlow.NONE,
        errorMessage = null,
        successMessage = null
    )

    companion object {
        private const val KEY_UI_STATE = "auth_ui_state"
    }
}

@Parcelize
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val loginInProgress: AuthFlow = AuthFlow.NONE
) : Parcelable

enum class AuthFlow {
    NONE,
    EMAIL,
    GOOGLE
}
