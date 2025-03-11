package com.example.sofrehmessina.ui.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.compose.animation.core.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animation utilities for smooth page transitions in the app
 */
object AnimationUtils {
    private const val ANIMATION_DURATION = 400
    private const val FAST_ANIMATION_DURATION = 300
    private const val SLOW_ANIMATION_DURATION = 600

    /**
     * Creates a slide and fade entrance transition
     */
    fun enterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            initialOffsetX = { fullWidth -> fullWidth }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }

    /**
     * Creates a slide and fade exit transition
     */
    fun exitTransition(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            targetOffsetX = { fullWidth -> -fullWidth }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }

    /**
     * Creates a slide and fade "pop" entrance transition (when going back)
     */
    fun popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            initialOffsetX = { fullWidth -> -fullWidth }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }

    /**
     * Creates a slide and fade "pop" exit transition (when going back)
     */
    fun popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            targetOffsetX = { fullWidth -> fullWidth }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }

    // Alternative animations for special screens
    
    /**
     * Scale and fade entrance for special screens like splash or modals
     */
    fun specialEnterTransition(): EnterTransition {
        return scaleIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            initialScale = 0.8f
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    /**
     * Scale and fade exit for special screens like splash or modals
     */
    fun specialExitTransition(): ExitTransition {
        return scaleOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            targetScale = 1.1f
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    /**
     * Vertical slide for fullscreen dialogs or bottom sheets
     */
    fun verticalEnterTransition(): EnterTransition {
        return slideInVertically(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    /**
     * Vertical exit for fullscreen dialogs or bottom sheets
     */
    fun verticalExitTransition(): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    // New animation transitions
    
    /**
     * Spring-based bouncy entrance transition
     */
    fun springEnterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { fullWidth -> fullWidth }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    /**
     * Zoom entrance transition for food details
     */
    fun zoomEnterTransition(): EnterTransition {
        return scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialScale = 0.7f
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FAST_ANIMATION_DURATION,
                easing = LinearOutSlowInEasing
            )
        )
    }
    
    /**
     * Quick zoom out transition
     */
    fun zoomExitTransition(): ExitTransition {
        return scaleOut(
            animationSpec = tween(
                durationMillis = FAST_ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            ),
            targetScale = 0.9f
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FAST_ANIMATION_DURATION,
                easing = LinearOutSlowInEasing
            )
        )
    }
    
    /**
     * Bottom sheet entrance transition
     */
    fun bottomSheetEnterTransition(): EnterTransition {
        return slideInVertically(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    /**
     * Bottom sheet exit transition
     */
    fun bottomSheetExitTransition(): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            ),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        )
    }
    
    /**
     * Creates a bouncing effect that can be applied to any composable
     */
    fun Modifier.bounceEffect(
        enabled: Boolean = true,
        bounceHeight: Float = 4f
    ): Modifier = composed {
        if (!enabled) return@composed this
        
        val infiniteTransition = rememberInfiniteTransition(label = "bounce")
        val bounce by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800,
                    easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )
        
        // Calculate bounce using sine function for smoother effect
        val bounceDelta = sin(bounce * PI.toFloat()) * bounceHeight
        
        this.offset(y = (-bounceDelta).dp)
    }
    
    /**
     * Creates a pulse effect that scales the component up and down
     */
    fun Modifier.pulseEffect(
        isPulsing: Boolean = true
    ): Modifier = composed {
        if (!isPulsing) return@composed this
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        this.scale(scale)
    }
    
    /**
     * Creates a floating effect that makes the component hover
     */
    fun Modifier.floatingEffect(
        enabled: Boolean = true,
        floatHeight: Float = 3f
    ): Modifier = composed {
        if (!enabled) return@composed this
        
        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val translation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float"
        )
        
        val floatDelta = sin(translation * PI.toFloat()) * floatHeight
        
        this.offset(y = (-floatDelta).dp)
    }
    
    /**
     * Creates a rotation effect in 3D space
     */
    fun Modifier.rotationEffect(
        enabled: Boolean = true,
        maxDegrees: Float = 10f
    ): Modifier = composed {
        if (!enabled) return@composed this
        
        val infiniteTransition = rememberInfiniteTransition(label = "rotate")
        val rotation by infiniteTransition.animateFloat(
            initialValue = -maxDegrees,
            targetValue = maxDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotate"
        )
        
        this.graphicsLayer {
            rotationZ = rotation
        }
    }
}

/**
 * Enum for flip direction
 */
enum class FlipDirection {
    HORIZONTAL,
    VERTICAL
} 