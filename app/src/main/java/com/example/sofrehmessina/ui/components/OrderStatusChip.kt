package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sofrehmessina.data.model.OrderStatus

/**
 * A chip component that displays the order status with an appropriate icon and color.
 * 
 * @param status The order status to display
 * @param modifier Optional modifier for the component
 * @param showIcon Whether to show the status icon (default: true)
 */
@Composable
fun OrderStatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val (color, icon) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.primary to Icons.Default.Schedule
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.tertiary to Icons.Default.ThumbUp
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.secondary to Icons.Default.Restaurant
        OrderStatus.READY -> MaterialTheme.colorScheme.primary to Icons.Default.Done
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondary to Icons.Default.DeliveryDining
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to Icons.Default.Cancel
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = status.name.lowercase()
                    .replaceFirstChar { it.uppercase() },
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * A simplified version of OrderStatusChip that only shows the status text with a colored background.
 * Used in admin screens where a more compact representation is needed.
 * 
 * @param status The order status to display
 * @param modifier Optional modifier for the component
 */
@Composable
fun AdminOrderStatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.errorContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.secondaryContainer
        OrderStatus.READY -> MaterialTheme.colorScheme.tertiaryContainer
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.surfaceVariant
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }
    
    val textColor = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.onErrorContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.READY -> MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.onSurfaceVariant
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = backgroundColor
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
} 