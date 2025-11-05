package com.example.master.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.master.MasterApplication

class LessonFragment : Fragment() {
    
    private lateinit var viewModel: LessonViewModel
    private var lessonId: Int = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get lessonId from arguments
        lessonId = arguments?.getInt("lessonId") ?: 1
        
        val app = requireActivity().application as MasterApplication
        viewModel = LessonViewModel(app.repository, lessonId)
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
                            // Show completion message
                            val message = if (result.isPassed) {
                                "Congratulations! +${result.xpEarned} XP, +${result.coinsEarned} Coins"
                            } else {
                                "Try again! You can do better!"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            
                            // Navigate back to home
                            findNavController().popBackStack()
                        },
                        onExit = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }
}
