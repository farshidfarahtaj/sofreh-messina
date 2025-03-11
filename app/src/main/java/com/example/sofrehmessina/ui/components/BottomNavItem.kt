package com.example.sofrehmessina.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sofrehmessina.ui.utils.AnimationUtils.bounceEffect
import kotlinx.coroutines.delay

@Composable
fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    // Create selection animation
    LaunchedEffect(selected) {
        if (selected) {
            // Bounce animation when selected
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    
    NavigationBarItem(
        icon = { 
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = label,
                    modifier = Modifier
                        .scale(scale.value)
                        .bounceEffect(enabled = selected, bounceHeight = 2f)
                )
                
                // Badge code removed - no longer showing cart badge
            }
        },
        label = { 
            Text(
                text = label,
                style = LocalTextStyle.current.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            ) 
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    )
} 