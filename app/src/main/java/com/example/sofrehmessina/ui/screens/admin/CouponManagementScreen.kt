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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponManagementScreen(
    onBackClick: () -> Unit,
    viewModel: DiscountViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val discounts by viewModel.discounts.collectAsState()
        val categories by viewModel.categories.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        
        // Filter to only show coupon-based discounts
        val couponDiscounts = discounts.filter { it.couponCode != null }
        
        var showAddDialog by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }
        var selectedDiscount by remember { mutableStateOf<Discount?>(null) }
        
        LaunchedEffect(key1 = true) {
            viewModel.loadDiscounts()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Coupon Management") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Coupon")
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
                } else if (couponDiscounts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No coupons found. Create one by tapping the + button.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(couponDiscounts) { discount ->
                            CouponCard(
                                discount = discount,
                                categories = categories,
                                onEditClick = {
                                    selectedDiscount = discount
                                    showEditDialog = true
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
                        text = { Text(error.orEmpty()) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("OK")
                            }
                        }
                    )
                }
                
                if (showAddDialog) {
                    CouponDialog(
                        onDismiss = { showAddDialog = false },
                        onConfirm = { coupon ->
                            viewModel.createDiscount(
                                name = coupon.name,
                                description = coupon.description,
                                categoryId = coupon.categoryId,
                                minQuantity = coupon.minQuantity,
                                percentOff = coupon.percentOff,
                                active = coupon.active,
                                startDate = coupon.startDate,
                                endDate = coupon.endDate,
                                specificFoodIds = coupon.specificFoodIds,
                                couponCode = coupon.couponCode,
                                isCustomerSpecific = coupon.isCustomerSpecific
                            )
                            showAddDialog = false
                        },
                        categories = categories,
                        discount = null
                    )
                }
                
                if (showEditDialog && selectedDiscount != null) {
                    CouponDialog(
                        onDismiss = {
                            showEditDialog = false
                            selectedDiscount = null
                        },
                        onConfirm = { updatedCoupon ->
                            viewModel.updateDiscount(
                                id = selectedDiscount!!.id,
                                name = updatedCoupon.name,
                                description = updatedCoupon.description,
                                categoryId = updatedCoupon.categoryId,
                                minQuantity = updatedCoupon.minQuantity,
                                percentOff = updatedCoupon.percentOff,
                                active = updatedCoupon.active,
                                startDate = updatedCoupon.startDate,
                                endDate = updatedCoupon.endDate,
                                specificFoodIds = updatedCoupon.specificFoodIds,
                                couponCode = updatedCoupon.couponCode,
                                isCustomerSpecific = updatedCoupon.isCustomerSpecific
                            )
                            showEditDialog = false
                            selectedDiscount = null
                        },
                        categories = categories,
                        discount = selectedDiscount
                    )
                }
            }
        }
    }
}

@Composable
fun CouponCard(
    discount: Discount,
    categories: List<Category>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val category = categories.find { it.id == discount.categoryId }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Coupon Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = discount.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Coupon code in highlighted box
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CODE:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = discount.couponCode ?: "N/A",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (discount.active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (discount.active) "ACTIVE" else "INACTIVE",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (discount.active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Discount details
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = discount.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${discount.percentOff.toInt()}% off",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                    
                    if (category != null) {
                        Text(
                            text = "Category: ${category.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (discount.categoryId.isBlank()) {
                        Text(
                            text = "Applies to: All Categories",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (discount.isCustomerSpecific) {
                        Text(
                            text = "Customer-specific coupon",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                    
                    // Date validity
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val now = Date()
                    val isValid = (discount.startDate == null || discount.startDate <= now) &&
                                 (discount.endDate == null || discount.endDate >= now)
                    
                    val dateRangeText = when {
                        discount.startDate != null && discount.endDate != null ->
                            "Valid: ${dateFormat.format(discount.startDate)} - ${dateFormat.format(discount.endDate)}"
                        discount.startDate != null ->
                            "Valid from: ${dateFormat.format(discount.startDate)}"
                        discount.endDate != null ->
                            "Valid until: ${dateFormat.format(discount.endDate)}"
                        else -> "No expiration date"
                    }
                    
                    Text(
                        text = dateRangeText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isValid) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error,
                            textDecoration = if (!isValid) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                }
                
                // Actions
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Coupon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Coupon",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponDialog(
    onDismiss: () -> Unit,
    onConfirm: (Discount) -> Unit,
    categories: List<Category>,
    discount: Discount?
) {
    // State for form fields
    var name by remember { mutableStateOf(discount?.name ?: "") }
    var description by remember { mutableStateOf(discount?.description ?: "") }
    var couponCode by remember { mutableStateOf(discount?.couponCode ?: "") }
    var selectedCategoryId by remember { mutableStateOf(discount?.categoryId ?: "") }
    var percentOffText by remember { mutableStateOf(discount?.percentOff?.toString() ?: "") }
    var minQuantityText by remember { mutableStateOf(discount?.minQuantity?.toString() ?: "0") }
    var isActive by remember { mutableStateOf(discount?.active ?: true) }
    var isCustomerSpecific by remember { mutableStateOf(discount?.isCustomerSpecific ?: false) }
    
    var startDate by remember { mutableStateOf(discount?.startDate) }
    var endDate by remember { mutableStateOf(discount?.endDate) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    var expandCategoryDropdown by remember { mutableStateOf(false) }

    // Validation
    val isValid = name.isNotBlank() && couponCode.isNotBlank() && 
                   percentOffText.toDoubleSafely() in 0.01..100.0 &&
                   isValidDateRange(startDate, endDate)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (discount == null) "Add New Coupon" else "Edit Coupon",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Coupon Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Coupon Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Coupon Code
                OutlinedTextField(
                    value = couponCode,
                    onValueChange = { couponCode = it.uppercase() },
                    label = { Text("Coupon Code") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = couponCode.isBlank()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Discount percentage
                OutlinedTextField(
                    value = percentOffText,
                    onValueChange = { percentOffText = it },
                    label = { Text("Discount Percentage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = percentOffText.toDoubleSafely() !in 0.01..100.0,
                    trailingIcon = { Text("%") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category selection
                ExposedDropdownMenuBox(
                    expanded = expandCategoryDropdown,
                    onExpandedChange = { expandCategoryDropdown = !expandCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "All Categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Apply to Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCategoryDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandCategoryDropdown,
                        onDismissRequest = { expandCategoryDropdown = false }
                    ) {
                        // Add "All Categories" option
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                selectedCategoryId = ""
                                expandCategoryDropdown = false
                            }
                        )
                        
                        // Add category options
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expandCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Min quantity (optional for coupons)
                OutlinedTextField(
                    value = minQuantityText,
                    onValueChange = { minQuantityText = it },
                    label = { Text("Minimum Quantity (0 for no minimum)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Start date
                    OutlinedTextField(
                        value = startDate?.let { formatDate(it) } ?: "No start date",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartDatePicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(Icons.Default.DateRange, "Select start date")
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // End date
                    OutlinedTextField(
                        value = endDate?.let { formatDate(it) } ?: "No end date",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndDatePicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Default.DateRange, "Select end date")
                            }
                        },
                        isError = !isValidDateRange(startDate, endDate)
                    )
                }
                
                if (!isValidDateRange(startDate, endDate)) {
                    Text(
                        text = "End date must be after start date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status and customer-specific toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active")
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isCustomerSpecific,
                            onCheckedChange = { isCustomerSpecific = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customer-specific")
                    }
                }
                
                if (isCustomerSpecific) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This coupon will be intended for specific customers only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
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
                            // Create discount object with coupon code
                            val newDiscount = Discount(
                                id = discount?.id ?: "",
                                name = name,
                                description = description,
                                categoryId = selectedCategoryId,
                                percentOff = percentOffText.toDoubleSafely(),
                                minQuantity = minQuantityText.toIntSafely(),
                                active = isActive,
                                startDate = startDate,
                                endDate = endDate,
                                couponCode = couponCode,
                                isCustomerSpecific = isCustomerSpecific
                            )
                            
                            onConfirm(newDiscount)
                        },
                        enabled = isValid
                    ) {
                        Text(if (discount == null) "Create Coupon" else "Update Coupon")
                    }
                }
            }
        }
    }
    
    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = {
                endDate = it
                showEndDatePicker = false
            }
        )
    }
} 