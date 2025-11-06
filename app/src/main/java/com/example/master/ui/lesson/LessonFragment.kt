package com.example.master.ui.lesson

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.master.MasterApplication
import com.example.master.core.audio.AudioPlayer
import com.example.master.core.audio.TTSManager
import java.util.Locale

class LessonFragment : Fragment() {
    
    private lateinit var viewModel: LessonViewModel
    private var lessonId: Int = 1
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var ttsManager: TTSManager
    private var pendingSpeechPrompt: String? = null
    
    private val speechPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer(pendingSpeechPrompt)
        } else {
            Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val transcript = matches?.firstOrNull()
            if (transcript != null) {
                viewModel.onEvent(LessonEvent.SpeakingAnswerCaptured(transcript))
            } else {
                Toast.makeText(requireContext(), "No speech detected", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lessonId = arguments?.getInt("lessonId") ?: 1
        
        val app = requireActivity().application as MasterApplication
        viewModel = LessonViewModel(app.repository, lessonId)
        audioPlayer = AudioPlayer(requireContext())
        ttsManager = TTSManager(requireContext())
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
                    LessonScreen(
                        viewModel = viewModel,
                        onLessonComplete = { result ->
                            val message = if (result.isPassed) {
                                "Congratulations! +${result.xpEarned} XP, +${result.coinsEarned} Coins"
                            } else {
                                "Lesson ended. Keep practicing!"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            findNavController().popBackStack()
                        },
                        onExit = {
                            findNavController().popBackStack()
                        },
                        onPlayAudio = { text, audioUrl, slow ->
                            playAudio(text, audioUrl, slow)
                        },
                        onRequestSpeechRecognition = { prompt ->
                            requestSpeechRecognition(prompt)
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        audioPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.release()
    }
    
    private fun requestSpeechRecognition(prompt: String) {
        pendingSpeechPrompt = prompt
        val context = requireContext()
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognizer(prompt)
        } else {
            speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    private fun launchSpeechRecognizer(prompt: String?) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt ?: "Speak now")
        }
        speechRecognizerLauncher.launch(intent)
    }
    
    private fun playAudio(text: String, audioUrl: String?, slow: Boolean) {
        val normalizedText = text.ifBlank { return }
        if (!slow && !audioUrl.isNullOrBlank()) {
            audioPlayer.play(audioUrl) {
                Toast.makeText(requireContext(), "Unable to play audio", Toast.LENGTH_SHORT).show()
            }
            return
        }

        audioPlayer.stop()
        val locale = detectLocale(normalizedText)
        val speed = if (slow) 0.7f else 1.0f
        ttsManager.speak(
            text = normalizedText,
            language = locale,
            speed = speed
        )
    }

    private fun detectLocale(text: String): Locale {
        val containsCJK = text.any { Character.UnicodeBlock.of(it)?.toString()?.contains("CJK") == true }
        val containsVietnameseDiacritics = text.any { it.code > 127 }
        return when {
            containsCJK -> Locale.CHINESE
            containsVietnameseDiacritics -> Locale("vi")
            else -> Locale.US
        }
    }
}
