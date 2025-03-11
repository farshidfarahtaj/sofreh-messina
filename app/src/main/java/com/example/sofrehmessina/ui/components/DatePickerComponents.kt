package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*
import com.example.sofrehmessina.util.isSafelyBefore

/**
 * Format a date to a readable string
 * @param date The date to format
 * @return Formatted date string or empty string if date is null
 */
fun formatDate(date: Date?): String {
    return if (date != null) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateFormat.format(date)
    } else {
        ""
    }
}

/**
 * Check if a date range is valid
 * @param startDate The start date
 * @param endDate The end date
 * @return true if valid date range (including if either date is null), false otherwise
 */
fun isValidDateRange(startDate: Date?, endDate: Date?): Boolean {
    return startDate.isSafelyBefore(endDate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Date) -> Unit,
    initialDate: Date? = null
) {
    val calendar = Calendar.getInstance()
    if (initialDate != null) {
        calendar.time = initialDate
    }
    
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Year picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Year:", modifier = Modifier.width(80.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    var showYearPicker by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { showYearPicker = true }) {
                        Text(selectedYear.toString())
                    }
                    
                    if (showYearPicker) {
                        YearPickerDialog(
                            onDismissRequest = { showYearPicker = false },
                            onYearSelected = { year -> 
                                selectedYear = year
                                showYearPicker = false
                            },
                            initialYear = selectedYear
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Month picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Month:", modifier = Modifier.width(80.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val months = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true }
                        ) {
                            Text(months[selectedMonth])
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            months.forEachIndexed { index, month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        selectedMonth = index
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Day picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Day:", modifier = Modifier.width(80.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    var dayExpanded by remember { mutableStateOf(false) }
                    
                    // Calculate days in month
                    val daysInMonth = when (selectedMonth + 1) {
                        2 -> if (selectedYear % 4 == 0 && (selectedYear % 100 != 0 || selectedYear % 400 == 0)) 29 else 28
                        4, 6, 9, 11 -> 30
                        else -> 31
                    }
                    
                    // Adjust selected day if necessary
                    if (selectedDay > daysInMonth) {
                        selectedDay = daysInMonth
                    }
                    
                    Box {
                        OutlinedButton(
                            onClick = { dayExpanded = true }
                        ) {
                            Text(selectedDay.toString())
                        }
                        
                        DropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            for (day in 1..daysInMonth) {
                                DropdownMenuItem(
                                    text = { Text(day.toString()) },
                                    onClick = {
                                        selectedDay = day
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            calendar.set(selectedYear, selectedMonth, selectedDay)
                            onDateSelected(calendar.time)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun YearPickerDialog(
    onDismissRequest: () -> Unit,
    onYearSelected: (Int) -> Unit,
    initialYear: Int
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 100..currentYear + 10).toList()
    
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Year",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(years) { year ->
                        Text(
                            text = year.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onYearSelected(year) }
                                .padding(16.dp),
                            fontWeight = if (year == initialYear) FontWeight.Bold else FontWeight.Normal,
                            color = if (year == initialYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    initialDate: Date? = null,
    onDateSelected: (Date) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(initialDate ?: Date()) }
    
    Column {
        OutlinedButton(
            onClick = { showDialog = true }
        ) {
            Text(formatDate(selectedDate))
        }
        
        if (showDialog) {
            DatePickerDialog(
                onDismissRequest = { showDialog = false },
                onDateSelected = { date ->
                    selectedDate = date
                    onDateSelected(date)
                },
                initialDate = selectedDate
            )
        }
    }
} 