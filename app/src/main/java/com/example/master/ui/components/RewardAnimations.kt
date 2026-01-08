package com.example.master.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animated coin gain effect
 * Shows coins flying up and fading out
 */
@Composable
fun CoinGainAnimation(
    amount: Int,
    onAnimationEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }
    
    LaunchedEffect(amount) {
        animate(
            initialValue = 0f,
            targetValue = -100f,
            animationSpec = tween(1000, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            offsetY = value
            alpha = 1f - (value / -100f)
        }
        delay(100)
        onAnimationEnd()
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .offset(y = offsetY.dp)
            .alpha(alpha)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Coins",
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "+$amount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )
    }
}

/**
 * XP gain animation with level up effect
 */
@Composable
fun XPGainAnimation(
    amount: Int,
    showLevelUp: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    var offsetY by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }
    var scale by remember { mutableStateOf(1f) }
    
    LaunchedEffect(amount) {
        if (showLevelUp) {
            // Bounce animation for level up
            animate(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) { value, _ ->
                scale = value
            }
        }
        
        animate(
            initialValue = 0f,
            targetValue = -80f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        ) { value, _ ->
            offsetY = value
            alpha = 1f - (value / -80f)
        }
        delay(100)
        onAnimationEnd()
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = offsetY.dp)
            .alpha(alpha)
    ) {
        if (showLevelUp) {
            Text(
                text = "🎉 LEVEL UP! 🎉",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = 0.dp, y = scale.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Text(
            text = "+${amount} XP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

/**
 * Success confetti animation
 */
@Composable
fun SuccessAnimation(
    show: Boolean,
    onAnimationEnd: () -> Unit = {}
) {
    if (!show) return
    
    LaunchedEffect(show) {
        delay(2000)
        onAnimationEnd()
    }
    
    // Confetti particles animation
    // Implementation would use Canvas to draw animated particles
}
