package com.example.sofrehmessina.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sofrehmessina.R
import kotlinx.coroutines.delay

/**
 * A dialog that shows an animation when an item is added to the cart
 * 
 * @param visible Whether the dialog is visible
 * @param foodName The name of the food item that was added to the cart
 * @param quantity The quantity of the food item that was added
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun AddToCartSuccessDialog(
    visible: Boolean,
    foodName: String,
    quantity: Int = 1,
    onDismiss: () -> Unit
) {
    // Create a transition state that starts as visible if the dialog is visible
    val transitionState = remember {
        MutableTransitionState(false).apply {
            targetState = visible
        }
    }
    
    // Auto-dismiss after a delay
    LaunchedEffect(visible) {
        if (visible) {
            delay(1800) // Show for 1.8 seconds
            onDismiss()
        }
    }
    
    // Only show the dialog if the transition is happening or the dialog is visible
    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
                usePlatformDefaultWidth = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(animationSpec = tween(220)) + 
                       scaleIn(initialScale = 0.8f, animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(220)) + 
                       scaleOut(targetScale = 0.8f, animationSpec = tween(220))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Success icon with animation
                        SuccessCheckmark()
                        
                        // Success message
                        Text(
                            text = stringResource(R.string.added_to_cart_success),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Item details
                        Text(
                            text = if (quantity > 1) {
                                "$quantity × $foodName"
                            } else {
                                foodName
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * A bouncing checkmark animation that indicates success
 */
@Composable
private fun SuccessCheckmark() {
    var animationPlayed by remember { mutableStateOf(false) }
    
    // Start the animation when the composable is first displayed
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    // Create a transition that animates the scale of the checkmark
    val transition = updateTransition(animationPlayed, label = "checkmark_transition")
    
    // Animate the scale of the checkmark
    val scale by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "scale"
    ) { played ->
        if (played) 1f else 0f
    }
    
    // Background circle
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Checkmark icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * A simpler animation that slides in from the bottom of the screen
 * This is an alternative to the dialog if you prefer a less intrusive notification
 */
@Composable
fun AddToCartSnackbarAnimation(
    visible: Boolean,
    foodName: String,
    quantity: Int = 1,
    onDismiss: () -> Unit
) {
    // Create a transition state that starts as visible if the animation is visible
    val transitionState = remember {
        MutableTransitionState(false).apply {
            targetState = visible
        }
    }
    
    // Auto-dismiss after a delay
    LaunchedEffect(visible) {
        if (visible) {
            delay(2000) // Show for 2 seconds
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column {
                        Text(
                            text = stringResource(R.string.added_to_cart_success),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = if (quantity > 1) {
                                "$quantity × $foodName"
                            } else {
                                foodName
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
} 