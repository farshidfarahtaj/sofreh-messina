package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuantitySelector(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    maxQuantity: Int = 100
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Decrement button
        OutlinedIconButton(
            onClick = { 
                if (quantity > minQuantity) {
                    onQuantityChange(quantity - 1)
                }
            },
            enabled = quantity > minQuantity,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Decrease quantity",
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Quantity value
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.bodyLarge
        )
        
        // Increment button
        OutlinedIconButton(
            onClick = { 
                if (quantity < maxQuantity) {
                    onQuantityChange(quantity + 1)
                }
            },
            enabled = quantity < maxQuantity,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase quantity",
                modifier = Modifier.size(16.dp)
            )
        }
    }
} 