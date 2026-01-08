package com.example.master.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import com.example.master.core.cache.AudioCache
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.master.R
import com.example.master.auth.AuthState
import com.example.master.auth.AuthViewModel
import com.example.master.auth.LoginScreen
import com.example.master.auth.RegisterScreen
import com.example.master.core.audio.AudioPlayer
import com.example.master.core.audio.TTSManager
import com.example.master.core.network.NetworkMonitor
import com.example.master.ui.flashcard.FlashcardScreen
import com.example.master.ui.flashcard.FlashcardViewModel
import com.example.master.ui.home.HomeNavigationEvent
import com.example.master.ui.home.HomeRoute
import com.example.master.ui.home.HomeViewModel
import com.example.master.ui.lesson.LessonEvent
import com.example.master.ui.lesson.LessonScreen
import com.example.master.ui.lesson.LessonViewModel
import com.example.master.ui.notifications.NotificationsRoute
import com.example.master.ui.notifications.NotificationsViewModel
import com.example.master.ui.practice.MistakeReviewRoute
import com.example.master.ui.practice.PracticeScreen
import com.example.master.ui.profile.ProfileScreen
import com.example.master.ui.profile.ProfileViewModel
import com.example.master.ui.settings.SettingsScreen
import com.example.master.ui.settings.SettingsViewModel
import com.example.master.ui.store.StoreRoute
import com.example.master.ui.store.StoreViewModel
import com.example.master.ui.sync.SyncViewModel
import com.example.master.ui.wordle.WordleRoute
import com.example.master.ui.theme.MasterTheme
import com.example.master.ui.theme.ThemeManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.util.Locale

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MasterApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val audioCache = remember { AudioCache(context) }
    val credentialManager = remember { CredentialManager.create(context) }
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState(initial = true)

    val bottomDestinations = remember {
        listOf(
            BottomDestination("learning", "Learning", Icons.Filled.AutoAwesome),
            BottomDestination("practice", "Practice", Icons.Filled.Star),
            BottomDestination("profile", "Profile", Icons.Filled.Person),
            BottomDestination("shop", "Shop", Icons.Filled.Store),
            BottomDestination("settings", "Settings", Icons.Filled.Settings)
        )
    }

    MasterTheme {
        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            bottomBar = {
                if (shouldShowBottomBar(currentDestination)) {
                    NavigationBar(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        bottomDestinations.forEach { destination ->
                            val selected =
                                currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = {
                                    Text(
                                        text = destination.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "auth_gate",
                modifier = Modifier.padding(innerPadding)
            ) {
            composable("auth_gate") {
                val viewModel: AuthViewModel = hiltViewModel()
                val syncViewModel: SyncViewModel = hiltViewModel()
                val authState by viewModel.authState.collectAsState()

                LaunchedEffect(authState, isConnected) {
                    when (authState) {
                        is AuthState.Authenticated -> navController.navigate("learning") {
                            popUpTo("auth_gate") { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        AuthState.Unauthenticated -> {
                            if (isConnected) {
                                navController.navigate("login") {
                                    popUpTo("auth_gate") { inclusive = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        AuthState.Loading -> Unit
                    }
                    if (authState is AuthState.Authenticated && isConnected) {
                        syncViewModel.syncAll()
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!isConnected && authState is AuthState.Unauthenticated) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Can ket noi internet de dang nhap lan dau.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Neu da dang nhap truoc do, hay bat mang de dong bo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }

            composable("login") {
                val viewModel: AuthViewModel = hiltViewModel()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val serverClientId = remember {
                    context.getString(R.string.default_web_client_id)
                }
                val legacySignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode != Activity.RESULT_OK) {
                        viewModel.reportError("Google sign-in cancelled.")
                        return@rememberLauncherForActivityResult
                    }
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    runCatching {
                        val account = task.getResult(ApiException::class.java)
                        val token = account?.idToken
                        if (!token.isNullOrBlank()) {
                            viewModel.signInWithGoogle(token)
                        } else {
                            viewModel.reportError("Google sign-in returned empty token.")
                        }
                    }.onFailure { e ->
                        val status = (e as? ApiException)?.statusCode
                        val message = when (status) {
                            GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                                "Google sign-in misconfigured (check SHA-1/SHA-256 in Firebase)."
                            GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                                "Google sign-in failed. Check Firebase config and keystore SHA."
                            GoogleSignInStatusCodes.NETWORK_ERROR ->
                                "Network error during Google sign-in."
                            GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                                "Google sign-in cancelled."
                            else -> "Google sign-in failed: ${e.message}"
                        }
                        viewModel.reportError(message)
                    }
                }

                LoginScreen(
                    viewModel = viewModel,
                    isConnected = isConnected,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        navController.navigate("learning") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoogleSignIn = {
                        scope.launch {
                            if (!isConnected) {
                                viewModel.reportError("Can internet de dang nhap.")
                                return@launch
                            }
                            if (serverClientId.isBlank()) {
                                viewModel.reportError("Missing default_web_client_id.")
                                return@launch
                            }
                            val activity = context as? Activity
                            if (activity == null) {
                                viewModel.reportError("Google sign-in requires an Activity context.")
                                return@launch
                            }
                            val playServicesAvailable = GoogleApiAvailability.getInstance()
                                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
                            if (!playServicesAvailable) {
                                viewModel.reportError("Google Play Services not available. Use a Play Store emulator or real device.")
                                return@launch
                            }
                            val launchLegacySignIn = {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(serverClientId)
                                    .requestEmail()
                                    .build()
                                val legacyClient = GoogleSignIn.getClient(context, gso)
                                legacySignInLauncher.launch(legacyClient.signInIntent)
                            }
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(serverClientId)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .setPreferImmediatelyAvailableCredentials(false)
                                .build()
                            try {
                                val result = credentialManager.getCredential(activity, request)
                                val credential = result.credential
                                if (
                                    credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val token = googleCredential.idToken
                                    if (!token.isNullOrBlank()) {
                                        viewModel.signInWithGoogle(token)
                                    } else {
                                        launchLegacySignIn()
                                    }
                                } else {
                                    launchLegacySignIn()
                                }
                            } catch (e: GetCredentialException) {
                                launchLegacySignIn()
                            }
                        }
                    },
                    onAnonymousSignIn = {
                        if (!isConnected) {
                            viewModel.reportError("Can internet de dang nhap.")
                        } else {
                            viewModel.signInAnonymously()
                        }
                    }
                )
            }

            composable("register") {
                val viewModel: AuthViewModel = hiltViewModel()
                RegisterScreen(
                    viewModel = viewModel,
                    isConnected = isConnected,
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate("learning") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("learning") {
                val viewModel: HomeViewModel = hiltViewModel()
                val storeViewModel: StoreViewModel = hiltViewModel()
                val context = LocalContext.current

                LaunchedEffect(viewModel) {
                    viewModel.navigationEvents.collect { event ->
                        when (event) {
                            is HomeNavigationEvent.NavigateToPlay -> navController.navigate("lesson/${event.level}")
                            HomeNavigationEvent.NavigateToStore -> navController.navigate("shop")
                            is HomeNavigationEvent.NavigateToQuest -> navController.navigate("shop")
                            is HomeNavigationEvent.NavigateToBooster -> navController.navigate("shop")
                            is HomeNavigationEvent.NavigateToFlashcards -> navController.navigate("flashcards/${event.lessonId}")
                            is HomeNavigationEvent.ThemeApplied -> {
                                ThemeManager.applyTheme(event.themeName)
                            }
                            is HomeNavigationEvent.ShowMessage -> {
                                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                HomeRoute(
                    homeViewModel = viewModel,
                    storeViewModel = storeViewModel
                )
            }


            composable("shop") {
                val viewModel: StoreViewModel = hiltViewModel()
                StoreRoute(viewModel = viewModel)
            }

            composable("practice") {
                PracticeScreen(
                    onStartLesson = { lessonId -> navController.navigate("lesson/$lessonId") },
                    onOpenFlashcards = { lessonId -> navController.navigate("flashcards/$lessonId") },
                    onOpenShop = { navController.navigate("shop") },
                    onOpenMistakes = { navController.navigate("mistakes") },
                    onOpenWordle = { navController.navigate("wordle") }
                )
            }

            composable("profile") {
                val viewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(viewModel = viewModel)
            }

            composable("mistakes") {
                MistakeReviewRoute()
            }

            composable("settings") {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onLoggedOut = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }

            composable("notifications") {
                val viewModel: NotificationsViewModel = hiltViewModel()
                NotificationsRoute(viewModel = viewModel)
            }

            composable("wordle") {
                WordleRoute()
            }

            composable(
                route = "flashcards/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.IntType; defaultValue = 1 })
            ) {
                val viewModel: FlashcardViewModel = hiltViewModel()
                val context = LocalContext.current
                val audioPlayer = remember { AudioPlayer(context) }
                val audioCache = remember { AudioCache(context) }
                val ttsManager = remember { TTSManager(context) }

                DisposableEffect(Unit) {
                    onDispose {
                        audioPlayer.stop()
                        ttsManager.release()
                    }
                }

                FlashcardScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onPlayAudio = { text, audioUrl, slow ->
                        val resolved = audioCache.getCachedPath(audioUrl) ?: audioUrl
                        if (!resolved.isNullOrBlank()) {
                            audioPlayer.play(resolved)
                        } else {
                            ttsManager.speak(text, speed = if (slow) 0.7f else 1.0f)
                        }
                    }
                )
            }

            composable(
                route = "lesson/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.IntType; defaultValue = 1 })
            ) {
                val viewModel: LessonViewModel = hiltViewModel()
                val context = LocalContext.current
                val audioPlayer = remember { AudioPlayer(context) }
                val audioCache = remember { AudioCache(context) }
                val ttsManager = remember { TTSManager(context) }

                var pendingSpeechPrompt by remember { mutableStateOf<String?>(null) }

                val speechRecognizerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val transcript = matches?.firstOrNull()
                        if (transcript != null) {
                            viewModel.onEvent(LessonEvent.SpeakingAnswerCaptured(transcript))
                        } else {
                            Toast.makeText(context, "No speech detected", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        val prompt = pendingSpeechPrompt
                        launchSpeechRecognizer(prompt, speechRecognizerLauncher)
                    } else {
                        Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        audioPlayer.stop()
                        ttsManager.release()
                    }
                }

                LessonScreen(
                    viewModel = viewModel,
                    onLessonComplete = { result ->
                        val message = if (result.isPassed) {
                            "Passed lesson. +${result.xpEarned} XP, +${result.coinsEarned} Coins"
                        } else {
                            "Not passed. Score ${result.correctAnswers}/${result.totalExercises}"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                    onExit = { navController.popBackStack() },
                    onPlayAudio = { text, audioUrl, slow ->
                        val resolved = audioCache.getCachedPath(audioUrl) ?: audioUrl
                        if (!resolved.isNullOrBlank()) {
                            audioPlayer.play(resolved)
                        } else {
                            ttsManager.speak(text, speed = if (slow) 0.7f else 1.0f)
                        }
                    },
                    onRequestSpeechRecognition = { prompt ->
                        pendingSpeechPrompt = prompt
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
            }
        }
    }
}

private fun launchSpeechRecognizer(
    prompt: String?,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        if (!prompt.isNullOrBlank()) {
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }
    launcher.launch(intent)
}

private fun shouldShowBottomBar(currentDestination: NavDestination?): Boolean {
    val route = currentDestination?.route ?: return true
    if (route in setOf("auth_gate", "login", "register")) return false
    if (route.startsWith("lesson") || route.startsWith("flashcards")) return false
    return true
}
