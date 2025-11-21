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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.master.R
import com.example.master.auth.AuthViewModel
import com.example.master.auth.LoginScreen
import com.example.master.auth.RegisterScreen
import com.example.master.core.audio.AudioPlayer
import com.example.master.core.audio.TTSManager
import com.example.master.ui.dashboard.DashboardRoute
import com.example.master.ui.dashboard.DashboardViewModel
import com.example.master.ui.home.HomeNavigationEvent
import com.example.master.ui.home.HomeRoute
import com.example.master.ui.home.HomeViewModel
import com.example.master.ui.lesson.LessonEvent
import com.example.master.ui.lesson.LessonScreen
import com.example.master.ui.lesson.LessonViewModel
import com.example.master.ui.notifications.NotificationsRoute
import com.example.master.ui.notifications.NotificationsViewModel
import com.example.master.ui.store.DailyChallengeScreen
import com.example.master.ui.store.StoreRoute
import com.example.master.ui.store.StoreViewModel
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

    val bottomDestinations = listOf(
        BottomDestination("home", "Home", Icons.Filled.Home),
        BottomDestination("dashboard", "Dashboard", Icons.Filled.Dashboard),
        BottomDestination("notifications", "Alerts", Icons.Filled.Notifications),
        BottomDestination("store", "Store", Icons.Filled.Store)
    )

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentDestination)) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
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
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                val viewModel: AuthViewModel = hiltViewModel()
                val context = LocalContext.current
                val credentialManager = remember { CredentialManager.create(context) }
                val scope = rememberCoroutineScope()

                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onGoogleSignIn = {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(context.getString(R.string.default_web_client_id))
                            .setAutoSelectEnabled(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        scope.launch {
                            runCatching { credentialManager.getCredential(context, request) }
                                .onSuccess { credResult ->
                                    runCatching {
                                        GoogleIdTokenCredential.createFrom(credResult.credential.data).idToken
                                    }.onSuccess { token ->
                                        if (!token.isNullOrEmpty()) {
                                            viewModel.signInWithGoogle(token)
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
                )
            }

            composable("register") {
                val viewModel: AuthViewModel = hiltViewModel()
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                val viewModel: HomeViewModel = hiltViewModel()
                val context = LocalContext.current

                LaunchedEffect(viewModel) {
                    viewModel.navigationEvents.collect { event ->
                        when (event) {
                            is HomeNavigationEvent.NavigateToPlay -> navController.navigate("lesson/${event.level}")
                            is HomeNavigationEvent.NavigateToDailyChallenge -> navController.navigate("daily")
                            HomeNavigationEvent.NavigateToAchievements -> navController.navigate("dashboard")
                            HomeNavigationEvent.NavigateToStore -> navController.navigate("store")
                            is HomeNavigationEvent.NavigateToQuest -> navController.navigate("store")
                            is HomeNavigationEvent.NavigateToBooster -> navController.navigate("store")
                            is HomeNavigationEvent.ThemeApplied -> {
                                Toast.makeText(context, "Theme applied: ${event.themeName}", Toast.LENGTH_SHORT).show()
                            }
                            is HomeNavigationEvent.ShowMessage -> {
                                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                HomeRoute(homeViewModel = viewModel)
            }

            composable("dashboard") {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardRoute(viewModel = viewModel)
            }

            composable("notifications") {
                val viewModel: NotificationsViewModel = hiltViewModel()
                NotificationsRoute(viewModel = viewModel)
            }

            composable("store") {
                val viewModel: StoreViewModel = hiltViewModel()
                StoreRoute(viewModel = viewModel)
            }

            composable("daily") {
                val viewModel: StoreViewModel = hiltViewModel()
                DailyChallengeScreen(
                    stateFlow = viewModel.uiState,
                    onStart = viewModel::startDailyChallenge,
                    onSubmit = { viewModel.submitDailyChallenge(score = 50) }
                )
            }

            composable(
                route = "lesson/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.IntType; defaultValue = 1 })
            ) {
                val viewModel: LessonViewModel = hiltViewModel()
                val context = LocalContext.current
                val audioPlayer = remember { AudioPlayer(context) }
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
                            "Congratulations! +${result.xpEarned} XP, +${result.coinsEarned} Coins"
                        } else {
                            "Lesson ended. Keep practicing!"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                    onExit = { navController.popBackStack() },
                    onPlayAudio = { text, audioUrl, slow ->
                        if (!audioUrl.isNullOrBlank()) {
                            audioPlayer.play(audioUrl)
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
    // Hide on auth screens, show everywhere else so users always have quick navigation
    val hidden = setOf("login", "register")
    return currentDestination?.route !in hidden
}
