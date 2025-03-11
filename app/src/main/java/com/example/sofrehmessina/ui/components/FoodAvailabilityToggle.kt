package com.example.sofrehmessina.ui.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * A specialized component for toggling food availability with a switch
 * that provides clear visual feedback and prevents accidental toggles.
 * 
 * This component is fully controlled by its parent - it doesn't maintain any internal state
 * to ensure it always reflects the current availability status correctly.
 */
@Composable
fun FoodAvailabilityToggle(
    available: Boolean,
    onAvailabilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tag = "FoodAvailabilityToggle"
    
    // Debug logging - use only the external prop value
    Log.d(tag, "FoodAvailabilityToggle composing with available: $available")
    
    // Animation states - use the external state directly
    val backgroundColor by animateColorAsState(
        targetValue = if (available) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.errorContainer,
        label = "bgColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (available) 
            MaterialTheme.colorScheme.onPrimaryContainer 
        else 
            MaterialTheme.colorScheme.onErrorContainer,
        label = "contentColor"
    )
    
    // Pulse animation for the title when changing state
    var isPulsing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPulsing) 1.05f else 1f,
        label = "pulseAnimation"
    )
    
    // Trigger the pulse animation when available changes
    LaunchedEffect(available) {
        Log.d(tag, "Availability state changed: $available")
        isPulsing = true
        delay(300)
        isPulsing = false
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title with animation
            Text(
                text = "Item Availability Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.scale(scale)
            )
            
            HorizontalDivider(
                color = contentColor.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Switch and label in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (available) "Available for Order" else "Unavailable (Out of Stock)",
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    
                    Text(
                        text = if (available)
                            "Customers can view and add this item to cart"
                        else
                            "Item will be shown as 'Out of Stock' and cannot be ordered",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
                
                // Add the switch with a slight delay to prevent accidental toggles
                var switchEnabled by remember { mutableStateOf(true) }
                
                Switch(
                    checked = available,
                    onCheckedChange = { newValue ->
                        Log.d(tag, "Switch toggled: $available -> $newValue")
                        
                        // Disable the switch temporarily to prevent rapid toggling
                        switchEnabled = false
                        
                        // Notify parent component of the change - parent is responsible for updating state
                        onAvailabilityChanged(newValue)
                        
                        // Re-enable the switch after a short delay
                        MainScope().launch {
                            delay(500) // 500ms delay
                            switchEnabled = true
                        }
                    },
                    enabled = switchEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.error,
                        uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer,
                        uncheckedBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.scale(1.2f) // Make the switch slightly larger
                )
            }
            
            // Status indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (available) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                else 
                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = if (available) "Available for Order" else "Out of Stock",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (available) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Preview for FoodAvailabilityToggle
 */
/*
@Composable
@androidx.compose.ui.tooling.preview.Preview
fun FoodAvailabilityTogglePreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            FoodAvailabilityToggle(
                available = true,
                onAvailabilityChanged = {}
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FoodAvailabilityToggle(
                available = false,
                onAvailabilityChanged = {}
            )
        }
    }
}
*/ 