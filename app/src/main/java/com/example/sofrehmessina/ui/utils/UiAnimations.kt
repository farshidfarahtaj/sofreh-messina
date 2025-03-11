package com.example.sofrehmessina.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import com.example.sofrehmessina.ui.utils.FlipDirection

/**
 * Collection of UI animation components and modifiers
 */
object UiAnimations {

    /**
     * Creates a shimmer effect for loading UI elements
     */
    @Composable
    fun ShimmerBox(
        modifier: Modifier = Modifier,
        shape: Shape = RectangleShape,
        isLoading: Boolean = true,
        content: @Composable () -> Unit = {}
    ) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val transition = rememberInfiniteTransition(label = "shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerOffset"
        )

        Box(
            modifier = modifier.onGloballyPositioned { size = it.size }
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.LightGray.copy(alpha = 0.6f),
                                        Color.LightGray.copy(alpha = 0.2f),
                                        Color.LightGray.copy(alpha = 0.6f),
                                    ),
                                    start = Offset(startOffsetX, 0f),
                                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
                                ),
                                size = Size(size.width.toFloat(), size.height.toFloat())
                            )
                        }
                )
            } else {
                content()
            }
        }
    }

    /**
     * Animated notification that slides in from the top and auto-dismisses
     */
    @Composable
    fun AnimatedNotification(
        visible: Boolean,
        message: String,
        color: Color = MaterialTheme.colorScheme.primary,
        modifier: Modifier = Modifier,
        autoDismiss: Boolean = true,
        dismissDelay: Long = 3000,
        onDismiss: () -> Unit = {}
    ) {
        val density = LocalDensity.current
        
        LaunchedEffect(visible) {
            if (visible && autoDismiss) {
                delay(dismissDelay)
                onDismiss()
            }
        }
        
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { with(density) { -40.dp.roundToPx() } } + expandVertically() + fadeIn(),
            exit = slideOutVertically { with(density) { -40.dp.roundToPx() } } + shrinkVertically() + fadeOut(),
            modifier = modifier
        ) {
            Surface(
                color = color,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    /**
     * Modifier extension that adds a pulsating border effect
     */
    fun Modifier.pulsatingBorder(
        borderColor: Color,
        pulseColor: Color,
        isActive: Boolean = true
    ): Modifier = composed {
        if (!isActive) return@composed this
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        val animatedColor by animateColorAsState(
            targetValue = if (isActive) pulseColor else borderColor,
            animationSpec = tween(300), 
            label = "color"
        )
        
        this.drawBehind {
            if (isActive) {
                drawCircle(
                    color = animatedColor.copy(alpha = 0.5f),
                    radius = size.minDimension / 2 * pulseScale
                )
            }
            
            drawCircle(
                color = borderColor,
                radius = size.minDimension / 2
            )
        }
    }
    
    /**
     * Creates a bouncing effect that can be applied to any composable
     */
    fun Modifier.bounceEffect(
        enabled: Boolean = true,
        bounceHeight: Float = 20f
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
     * Creates a wave-like animation that moves the component left to right
     */
    fun Modifier.waveEffect(
        enabled: Boolean = true,
        waveWidth: Float = 10f
    ): Modifier = composed {
        if (!enabled) return@composed this
        
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val wave by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave"
        )
        
        // Calculate wave using sine function
        val waveDelta = sin(wave * 2 * PI.toFloat()) * waveWidth
        
        this.offset(x = waveDelta.dp)
    }
    
    /**
     * Creates a folding/flipping effect (like a card flip)
     */
    fun Modifier.flipEffect(
        flipProgress: Float,
        flipDirection: FlipDirection = FlipDirection.HORIZONTAL
    ): Modifier = this.graphicsLayer {
        when (flipDirection) {
            FlipDirection.HORIZONTAL -> {
                rotationY = 180f * flipProgress
                cameraDistance = 12f * density
                alpha = if (flipProgress > 0.5f) 0f else 1f
            }
            FlipDirection.VERTICAL -> {
                rotationX = 180f * flipProgress
                cameraDistance = 12f * density
                alpha = if (flipProgress > 0.5f) 0f else 1f
            }
        }
        transformOrigin = TransformOrigin.Center
    }
    
    /**
     * Expands/Collapses a composable with animation
     */
    fun Modifier.expandCollapseEffect(
        expanded: Boolean,
        expandDuration: Int = 300
    ): Modifier = composed {
        val scale by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(
                durationMillis = expandDuration,
                easing = FastOutSlowInEasing
            ),
            label = "expand"
        )
        
        this
            .scale(scale)
            .alpha(scale)
    }
    
    /**
     * Creates a gradient shimmer effect that moves across the component
     */
    fun Modifier.gradientShimmer(
        colors: List<Color> = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        enabled: Boolean = true,
        durationMillis: Int = 1500
    ): Modifier = composed {
        if (!enabled) return@composed this
        
        var size by remember { mutableStateOf(IntSize.Zero) }
        val transition = rememberInfiniteTransition(label = "shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerOffset"
        )
        
        this
            .onGloballyPositioned { size = it.size }
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(startOffsetX, 0f),
                        end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
                    ),
                    size = Size(size.width.toFloat(), size.height.toFloat())
                )
            }
    }
} 