package com.example.master.auth

import com.example.master.core.user.UserProfile
import com.example.master.data.local.entity.UserEntity
import com.example.master.data.repository.LearningRepository
import com.example.master.auth.di.AuthProvider
import com.example.master.data.local.GameStateStore
import com.example.master.data.local.PendingSyncStore
import com.example.master.data.local.ShopSyncStore
import com.example.master.di.ApplicationScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val repository: LearningRepository,
    private val authProvider: AuthProvider,
    private val gameStateStore: GameStateStore,
    private val pendingSyncStore: PendingSyncStore,
    private val shopSyncStore: ShopSyncStore,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val firebaseAuth: FirebaseAuth = authProvider.firebaseAuth

    @Volatile
    private var cachedIdToken: String? = null
    private var lastUserId: String? = null

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
            appScope.launch {
                handleUserSwitch(user.uid)
                val localUser = ensureLocalUser(user)
                _currentUser.value = localUser
                refreshTokenCache(forceRefresh = false)
            }
        } else {
            cachedIdToken = null
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            lastUserId = null
        }
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: Flow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: Flow<UserEntity?> = _currentUser.asStateFlow()
    
    init {
        checkAuthStatus()
        firebaseAuth.addAuthStateListener(authListener)
    }
    
    private fun checkAuthStatus() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            _authState.value = AuthState.Authenticated(firebaseUser)
            appScope.launch {
                handleUserSwitch(firebaseUser.uid)
                val localUser = ensureLocalUser(firebaseUser)
                _currentUser.value = localUser
            }
            appScope.launch { refreshTokenCache(forceRefresh = false) }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInAnonymously(): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            val result = firebaseAuth.signInAnonymously().await().user
            if (result != null) {
                val localUser = ensureLocalUser(result, initializeAchievementsIfNew = false)
                _currentUser.value = localUser
                _authState.value = AuthState.Authenticated(result)
                refreshTokenCache(forceRefresh = false)
                AuthResult.Success
            } else {
                _authState.value = AuthState.Unauthenticated
                AuthResult.Error("Anonymous sign-in failed")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            AuthResult.Error(e.message ?: "Anonymous sign-in failed")
        }
    }
    
    suspend fun signIn(email: String, password: String): AuthResult {
        return authFlow(
            errorIfNull = "Login failed",
            initializeAchievementsIfNew = false
        ) {
            firebaseAuth.signInWithEmailAndPassword(email, password).await().user
        }
    }
    
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return authFlow(
            errorIfNull = "Google sign-in failed",
            initializeAchievementsIfNew = true
        ) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await().user
        }
    }
    
    suspend fun signUp(
        email: String, 
        password: String, 
        displayName: String
    ): AuthResult {
        return authFlow(
            errorIfNull = "Registration failed",
            initializeAchievementsIfNew = true,
            fallbackDisplayName = displayName
        ) {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            firebaseUser?.let {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                it.updateProfile(profileUpdates).await()
            }
            firebaseUser
        }
    }

    suspend fun signOut() {
        firebaseAuth.removeAuthStateListener(authListener)
        firebaseAuth.signOut()
        cachedIdToken = null
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        lastUserId = null
        firebaseAuth.addAuthStateListener(authListener)
    }

    fun isAnonymous(): Boolean = firebaseAuth.currentUser?.isAnonymous == true
    
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
            displayName = displayName ?: firebaseUser.displayName ?: "Guest",
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
        fallbackDisplayName: String? = null,
        initializeAchievementsIfNew: Boolean = false
    ): UserEntity {
        val existing = repository.getUserByIdSync(firebaseUser.uid)
        if (existing != null) return existing
        val created = createLocalUser(firebaseUser, fallbackDisplayName ?: firebaseUser.displayName)
        if (initializeAchievementsIfNew) repository.initializeAchievements(firebaseUser.uid)
        return created
    }

    private suspend fun handleUserSwitch(newUserId: String) {
        if (lastUserId == null || lastUserId == newUserId) {
            lastUserId = newUserId
            repository.keepOnlyUser(newUserId)
            return
        }
        repository.keepOnlyUser(newUserId)
        gameStateStore.clearAll()
        pendingSyncStore.clear()
        shopSyncStore.clear()
        lastUserId = newUserId
    }
    
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun getCachedIdToken(): String? = cachedIdToken

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return refreshTokenCache(forceRefresh)
    }

    private suspend fun refreshTokenCache(forceRefresh: Boolean): String? {
        val token = runCatching {
            firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        }.getOrNull()
        cachedIdToken = token
        return token
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

    private suspend fun authFlow(
        errorIfNull: String,
        initializeAchievementsIfNew: Boolean,
        fallbackDisplayName: String? = null,
        action: suspend () -> FirebaseUser?
    ): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            val firebaseUser = action()
            handleFirebaseUser(
                firebaseUser = firebaseUser,
                errorIfNull = errorIfNull,
                initializeAchievementsIfNew = initializeAchievementsIfNew,
                fallbackDisplayName = fallbackDisplayName
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            mapAuthError(e, errorIfNull)
        }
    }

    private suspend fun handleFirebaseUser(
        firebaseUser: FirebaseUser?,
        errorIfNull: String,
        initializeAchievementsIfNew: Boolean,
        fallbackDisplayName: String?
    ): AuthResult {
        return if (firebaseUser != null) {
            val localUser = ensureLocalUser(
                firebaseUser = firebaseUser,
                fallbackDisplayName = fallbackDisplayName,
                initializeAchievementsIfNew = initializeAchievementsIfNew
            )
            _currentUser.value = localUser
            _authState.value = AuthState.Authenticated(firebaseUser)
            refreshTokenCache(forceRefresh = false)
            AuthResult.Success
        } else {
            _authState.value = AuthState.Unauthenticated
            AuthResult.Error(errorIfNull)
        }
    }

    private fun mapAuthError(e: Exception, fallback: String): AuthResult.Error {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> AuthResult.Error("Invalid credentials")
            is FirebaseAuthInvalidUserException -> AuthResult.Error("Account not found or disabled")
            is FirebaseAuthException -> AuthResult.Error(e.message ?: fallback)
            else -> AuthResult.Error(fallback)
        }
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
