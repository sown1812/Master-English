package com.example.master.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.core.user.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    
    val authState = authManager.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }
    
    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }
    
    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name, displayNameError = null) }
    }
    
    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, confirmPasswordError = null) }
    }
    
    fun signIn() {
        val state = _uiState.value
        
        // Validation
        if (!validateEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Invalid email address") }
            return
        }
        
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginInProgress = AuthFlow.EMAIL
                ) 
            }
            
            when (val result = authManager.signIn(state.email, state.password)) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        ) 
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { 
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
            _uiState.update { it.copy(displayNameError = "Name is required") }
            return
        }
        
        if (!validateEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Invalid email address") }
            return
        }
        
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }
        
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
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
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        ) 
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { 
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
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    loginInProgress = AuthFlow.GOOGLE
                ) 
            }
            
            when (val result = authManager.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
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
    
    fun signInWithFacebook(token: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginInProgress = AuthFlow.FACEBOOK
                )
            }
            
            when (val result = authManager.signInWithFacebook(token)) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
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
    
    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginInProgress = AuthFlow.GUEST
                )
            }
            
            when (val result = authManager.signInAnonymously()) {
                is AuthResult.Success -> {
                    loadUserProfile()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginInProgress = AuthFlow.NONE
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
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
            _uiState.update { it.copy(emailError = "Invalid email address") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    loginInProgress = AuthFlow.NONE
                ) 
            }
            
            when (val result = authManager.resetPassword(state.email)) {
                is AuthResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset email sent!",
                            loginInProgress = AuthFlow.NONE
                        ) 
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { 
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
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
    
    fun reportError(message: String) {
        _uiState.update { 
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
}

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
)

enum class AuthFlow {
    NONE,
    EMAIL,
    GOOGLE,
    FACEBOOK,
    GUEST
}
