package com.example.master.auth

import com.example.master.core.user.UserProfile
import com.example.master.data.local.entity.UserEntity
import com.example.master.data.repository.LearningRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val repository: LearningRepository
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: Flow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: Flow<UserEntity?> = _currentUser.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            _authState.value = AuthState.Authenticated(firebaseUser)
            scope.launch {
                val localUser = ensureLocalUser(firebaseUser)
                _currentUser.value = localUser
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                val localUser = ensureLocalUser(firebaseUser)
                
                _currentUser.value = localUser
                _authState.value = AuthState.Authenticated(firebaseUser)
                AuthResult.Success
            } else {
                _authState.value = AuthState.Unauthenticated
                AuthResult.Error("Login failed")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                val localUser = ensureLocalUser(firebaseUser)
                
                _currentUser.value = localUser
                _authState.value = AuthState.Authenticated(firebaseUser)
                AuthResult.Success
            } else {
                _authState.value = AuthState.Unauthenticated
                AuthResult.Error("Google sign-in failed")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            AuthResult.Error(e.message ?: "Google sign-in failed")
        }
    }
    
    suspend fun signUp(
        email: String, 
        password: String, 
        displayName: String
    ): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            
            // Create Firebase user
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Create local user
                val localUser = createLocalUser(firebaseUser, displayName)
                
                // Initialize achievements
                repository.initializeAchievements(firebaseUser.uid)
                
                _currentUser.value = localUser
                _authState.value = AuthState.Authenticated(firebaseUser)
                AuthResult.Success
            } else {
                _authState.value = AuthState.Unauthenticated
                AuthResult.Error("Registration failed")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
    
    suspend fun resetPassword(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send reset email")
        }
    }
    
    private suspend fun createLocalUser(
        firebaseUser: FirebaseUser,
        displayName: String? = null
    ): UserEntity {
        val user = UserEntity(
            userId = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = displayName ?: firebaseUser.displayName ?: "User",
            avatarUrl = firebaseUser.photoUrl?.toString(),
            currentLevel = 1,
            totalXP = 0,
            coins = 100, // Starting coins
            streakDays = 0,
            lastStudyDate = System.currentTimeMillis(),
            wordsLearned = 0,
            lessonsCompleted = 0,
            exercisesCompleted = 0
        )
        
        repository.insertUser(user)
        return user
    }

    private suspend fun ensureLocalUser(
        firebaseUser: FirebaseUser,
        fallbackDisplayName: String? = null
    ): UserEntity {
        val existing = repository.getUserByIdSync(firebaseUser.uid)
        if (existing != null) return existing
        val created = createLocalUser(firebaseUser, fallbackDisplayName ?: firebaseUser.displayName)
        repository.initializeAchievements(firebaseUser.uid)
        return created
    }
    
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
    
    fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun getUserProfile(): UserProfile? {
        val userId = firebaseAuth.currentUser?.uid ?: return null
        return repository.getUserProfileSync(userId)
    }

    fun observeUserProfile(): Flow<UserProfile?> {
        val userId = firebaseAuth.currentUser?.uid ?: return flowOf(null)
        return repository.getUserProfile(userId)
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}
