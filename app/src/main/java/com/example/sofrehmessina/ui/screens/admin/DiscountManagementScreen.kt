package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.Discount
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.ui.components.DatePickerDialog
import com.example.sofrehmessina.ui.components.formatDate
import com.example.sofrehmessina.ui.components.isValidDateRange
import com.example.sofrehmessina.ui.viewmodel.DiscountViewModel
import com.example.sofrehmessina.ui.viewmodel.FoodViewModel
import com.example.sofrehmessina.util.orEmpty
import com.example.sofrehmessina.util.toDoubleSafely
import com.example.sofrehmessina.util.toIntSafely
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountManagementScreen(
    onBackClick: () -> Unit,
    viewModel: DiscountViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val discounts by viewModel.discounts.collectAsState()
        val categories by viewModel.categories.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        
        var showAddDialog by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }
        var selectedDiscount by remember { mutableStateOf<Discount?>(null) }
        
        LaunchedEffect(key1 = true) {
            viewModel.loadDiscounts()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Discount Management") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Discount")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                    )
                } else if (discounts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No discounts found. Create one by tapping the + button.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(discounts) { discount ->
                            DiscountCard(
                                discount = discount,
                                categoryName = viewModel.getCategoryName(discount.categoryId),
                                onEditClick = {
                                    selectedDiscount = discount
                                    showEditDialog = true
                                },
                                onToggleStatus = {
                                    viewModel.toggleDiscountStatus(discount)
                                },
                                onDeleteClick = {
                                    viewModel.deleteDiscount(discount.id)
                                }
                            )
                        }
                    }
                }
                
                if (error != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearError() },
                        title = { Text("Error") },
                        text = { Text(error ?: "Unknown error") },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
                
                if (showAddDialog) {
                    DiscountFormDialog(
                        discount = null,
                        categories = categories,
                        onDismiss = { showAddDialog = false },
                        onSave = { name, description, categoryId, minQuantity, percentOff, active, startDate, endDate, specificFoodIds ->
                            viewModel.createDiscount(
                                name = name,
                                description = description,
                                categoryId = categoryId,
                                minQuantity = minQuantity,
                                percentOff = percentOff,
                                active = active,
                                startDate = startDate,
                                endDate = endDate,
                                specificFoodIds = specificFoodIds
                            )
                            showAddDialog = false
                        }
                    )
                }
                
                if (showEditDialog && selectedDiscount != null) {
                    DiscountFormDialog(
                        discount = selectedDiscount,
                        categories = categories,
                        onDismiss = {
                            showEditDialog = false
                            selectedDiscount = null
                        },
                        onSave = { name, description, categoryId, minQuantity, percentOff, active, startDate, endDate, specificFoodIds ->
                            val updatedDiscount = selectedDiscount!!.copy(
                                name = name,
                                description = description,
                                categoryId = categoryId,
                                minQuantity = minQuantity,
                                percentOff = percentOff,
                                active = active,
                                startDate = startDate,
                                endDate = endDate,
                                specificFoodIds = specificFoodIds
                            )
                            viewModel.updateDiscount(updatedDiscount)
                            showEditDialog = false
                            selectedDiscount = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscountCard(
    discount: Discount,
    categoryName: String,
    onEditClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = discount.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    Switch(
                        checked = discount.active,
                        onCheckedChange = { onToggleStatus() }
                    )
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = discount.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (!discount.specificFoodIds.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${discount.specificFoodIds.size} specific items)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${discount.percentOff}% off",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (discount.minQuantity > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Min Quantity: ${discount.minQuantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (discount.startDate != null || discount.endDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            if (discount.startDate != null) {
                                append("From: ${dateFormat.format(discount.startDate)}")
                            }
                            
                            if (discount.endDate != null) {
                                if (discount.startDate != null) {
                                    append(" ")
                                }
                                append("To: ${dateFormat.format(discount.endDate)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .background(
                        color = if (discount.active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (discount.active) "Active" else "Inactive",
                    color = if (discount.active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountFormDialog(
    discount: Discount?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Double, Boolean, Date?, Date?, List<String>?) -> Unit
) {
    val isNewDiscount = discount == null
    val title = if (isNewDiscount) "Create Discount" else "Edit Discount"
    
    var name by remember { mutableStateOf(discount?.name ?: "") }
    var description by remember { mutableStateOf(discount?.description ?: "") }
    var categoryId by remember { mutableStateOf(discount?.categoryId.orEmpty()) }
    var minQuantity by remember { mutableStateOf(discount?.minQuantity?.toString() ?: "1") }
    var percentOff by remember { mutableStateOf(discount?.percentOff?.toString() ?: "10") }
    var active by remember { mutableStateOf(discount?.active ?: true) }
    
    // Time-limited discount options
    var isTimeLimited by remember { mutableStateOf(discount?.startDate != null || discount?.endDate != null) }
    var startDate by remember { mutableStateOf(discount?.startDate) }
    var endDate by remember { mutableStateOf(discount?.endDate) }
    
    // Specific food items selection
    var isItemSpecific by remember { mutableStateOf(discount?.specificFoodIds != null && discount.specificFoodIds.isNotEmpty()) }
    var selectedFoodItems by remember { mutableStateOf<List<Food>>(emptyList()) }
    var specificFoodIds by remember { mutableStateOf<List<String>>(discount?.specificFoodIds ?: emptyList()) }
    var showFoodSelection by remember { mutableStateOf(false) }
    
    // Load food items for the selected category
    val foodViewModel = hiltViewModel<FoodViewModel>()
    val foodItems by foodViewModel.foodItems.collectAsState()
    val isLoading by foodViewModel.isLoading.collectAsState()
    
    LaunchedEffect(categoryId) {
        if (categoryId.isNotEmpty() && isItemSpecific) {
            foodViewModel.loadFoodItems(categoryId)
        }
    }
    
    // Date selection dialog state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Validate dates
    val isDateRangeValid = !isTimeLimited || isValidDateRange(startDate, endDate)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Discount Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (categories.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedCategory = categories.find { it.id == categoryId }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categoryId = category.id
                                        expanded = false
                                        // Reset specific food selections when category changes
                                        if (isItemSpecific) {
                                            specificFoodIds = emptyList()
                                            selectedFoodItems = emptyList()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No categories available",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Discount scope selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apply discount to:", modifier = Modifier.weight(1f))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isItemSpecific,
                            onClick = { isItemSpecific = false }
                        )
                        Text("All items in category")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isItemSpecific,
                            onClick = { isItemSpecific = true }
                        )
                        Text("Specific items")
                    }
                }
                
                // Show specific food item selection when that option is selected
                if (isItemSpecific) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            showFoodSelection = true
                            // Load items for the selected category if not already loaded
                            if (categoryId.isNotEmpty()) {
                                foodViewModel.loadFoodItems(categoryId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = categoryId.isNotEmpty()
                    ) {
                        Text(
                            if (specificFoodIds.isEmpty()) "Select Food Items"
                            else "${specificFoodIds.size} Items Selected"
                        )
                    }
                    
                    if (categoryId.isEmpty()) {
                        Text(
                            "Please select a category first",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = minQuantity,
                    onValueChange = { minQuantity = it },
                    label = { Text("Minimum Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = percentOff,
                    onValueChange = { percentOff = it },
                    label = { Text("Discount Percentage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = active,
                        onCheckedChange = { active = it }
                    )
                    
                    Text("Active")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time-limited discount section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTimeLimited,
                        onCheckedChange = { 
                            isTimeLimited = it 
                            if (!it) {
                                startDate = null
                                endDate = null
                            }
                        }
                    )
                    
                    Text("Time-limited discount")
                }
                
                if (isTimeLimited) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Start date selection
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val startDateText = formatDate(startDate) 
                                .ifEmpty { "Select start date" }
                            Text(startDateText)
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // End date selection
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val endDateText = formatDate(endDate)
                                .ifEmpty { "Select end date" }
                            Text(endDateText)
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    }
                    
                    if (!isDateRangeValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "End date must be after start date",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Basic validation
                            if (name.isBlank() || categoryId.isBlank() || 
                                minQuantity.toIntOrNull() == null || percentOff.toDoubleOrNull() == null ||
                                !isDateRangeValid || (isItemSpecific && specificFoodIds.isEmpty())) {
                                return@Button
                            }
                            
                            onSave(
                                name,
                                description,
                                categoryId,
                                minQuantity.toIntSafely(1),
                                percentOff.toDoubleSafely(10.0),
                                active,
                                if (isTimeLimited) startDate else null,
                                if (isTimeLimited) endDate else null,
                                if (isItemSpecific) specificFoodIds else null
                            )
                        },
                        enabled = categoryId.isNotEmpty() && percentOff.isNotEmpty() && isDateRangeValid && 
                            (!isItemSpecific || specificFoodIds.isNotEmpty())
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
    
    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { date -> 
                startDate = date
                showStartDatePicker = false
            },
            initialDate = startDate
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { date -> 
                endDate = date
                showEndDatePicker = false
            },
            initialDate = endDate
        )
    }
    
    // Food item selection dialog
    if (showFoodSelection) {
        FoodSelectionDialog(
            foodItems = foodItems,
            selectedFoodIds = specificFoodIds,
            isLoading = isLoading,
            onDismiss = { showFoodSelection = false },
            onSelectionConfirmed = { selectedIds ->
                specificFoodIds = selectedIds
                showFoodSelection = false
            }
        )
    }
}

@Composable
fun FoodSelectionDialog(
    foodItems: List<Food>,
    selectedFoodIds: List<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectionConfirmed: (List<String>) -> Unit
) {
    var tempSelectedIds by remember { mutableStateOf(selectedFoodIds) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Food Items",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (foodItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No food items found in this category")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(foodItems) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelectedIds = if (tempSelectedIds.contains(food.id)) {
                                            tempSelectedIds - food.id
                                        } else {
                                            tempSelectedIds + food.id
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tempSelectedIds.contains(food.id),
                                    onCheckedChange = { checked ->
                                        tempSelectedIds = if (checked) {
                                            tempSelectedIds + food.id
                                        } else {
                                            tempSelectedIds - food.id
                                        }
                                    }
                                )
                                
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "$${food.price}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onSelectionConfirmed(tempSelectedIds) }
                    ) {
                        Text("Confirm (${tempSelectedIds.size} selected)")
                    }
                }
            }
        }
    }
} 