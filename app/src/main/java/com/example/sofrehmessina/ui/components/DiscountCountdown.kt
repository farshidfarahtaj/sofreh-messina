package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sofrehmessina.R
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Displays a countdown timer for time-limited discounts
 */
@Composable
fun DiscountCountdown(endDate: Date?) {
    if (endDate == null) return
    
    var remainingTime by remember { mutableStateOf(endDate.time - System.currentTimeMillis()) }
    
    LaunchedEffect(key1 = endDate) {
        while (remainingTime > 0) {
            delay(60000) // Update every minute
            remainingTime = endDate.time - System.currentTimeMillis()
        }
    }
    
    if (remainingTime > 0) {
        val hours = TimeUnit.MILLISECONDS.toHours(remainingTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = DiscountDesignSystem.Colors.timedDiscountBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = stringResource(R.string.discount_countdown),
                    tint = DiscountDesignSystem.Colors.discountPrimary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = stringResource(
                        R.string.ends_in, 
                        stringResource(R.string.hours_minutes, hours.toInt(), minutes.toInt())
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = DiscountDesignSystem.Colors.discountPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
} 