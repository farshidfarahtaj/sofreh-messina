package com.example.sofrehmessina.ui.screens.admin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.sofrehmessina.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.CategoryViewModel
import com.example.sofrehmessina.ui.viewmodel.FoodViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import kotlinx.coroutines.launch
import com.example.sofrehmessina.ui.components.MultilingualTextField
import com.example.sofrehmessina.data.model.FoodTranslation
import com.example.sofrehmessina.data.model.Languages
import com.example.sofrehmessina.util.LocaleHelper
import androidx.compose.ui.graphics.Color
import com.example.sofrehmessina.ui.components.FoodAvailabilityToggle
import android.net.Uri
import com.example.sofrehmessina.ui.components.ImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFoodScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    foodViewModel: FoodViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        var showAddDialog by remember { mutableStateOf(false) }
        var selectedFood by remember { mutableStateOf<Food?>(null) }
        var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    
        val foodItems by foodViewModel.foodItems.collectAsState()
        val categories by categoryViewModel.categories.collectAsState()
        val isLoading by foodViewModel.isLoading.collectAsState()
        val error by foodViewModel.error.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        val context = LocalContext.current
        val currentLang = LocaleHelper.getSelectedLanguageCode(context)
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Handle errors
        LaunchedEffect(error) {
            error?.let {
                // You could add a snackbar or other error notification here
                foodViewModel.clearError()
            }
        }

        // Load data when the screen is first displayed or when the selected category changes
        LaunchedEffect(selectedCategoryId) {
            try {
                // Load categories first
                categoryViewModel.loadCategories()
                
                // Then load food items based on the selected category
                if (selectedCategoryId != null) {
                    foodViewModel.loadFoodItems(selectedCategoryId)
                } else {
                    foodViewModel.loadAllFoodItems()
                }
            } catch (e: Exception) {
                // Log the error or handle it properly
                println("Error loading data: ${e.message}")
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AdminDrawer(
                    currentUser = currentUser,
                    navController = navController,
                    onLogout = { authViewModel.signOut() },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Manage Food Items") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            // Filter by category
                            IconButton(onClick = { /* Show category filter */ }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter by Category")
                            }
                            // Add food item
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Food Item")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Food Item")
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Category Filter
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOfFirst { it.id == selectedCategoryId } + 1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null }
                        ) {
                            Text(
                                text = "All",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        categories.forEach { category ->
                            Tab(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id }
                            ) {
                                Text(
                                    text = category.getName(currentLang),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            error != null -> {
                                Text(
                                    text = error?.message ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp)
                                )
                            }
                            else -> {
                                val filteredItems = if (selectedCategoryId != null) {
                                    foodItems.filter { it.categoryId == selectedCategoryId }
                                } else {
                                    foodItems
                                }

                                if (filteredItems.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.no_food_items_found),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(
                                            items = filteredItems,
                                            key = { it.id }
                                        ) { food ->
                                            AdminFoodItemCard(
                                                food = food,
                                                category = categories.find { it.id == food.categoryId },
                                                onEdit = { 
                                                    Log.d("AdminFoodItemCard", "Editing food item: ${food.id}, Name: ${food.getName(currentLang)}")
                                                    Log.d("AdminFoodItemCard", "Food availability status before edit: ${food.foodAvailable}")
                                                    selectedFood = food 
                                                },
                                                onDelete = { foodViewModel.deleteFoodItem(food.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            FoodFormDialog(
                food = null,
                categories = categories,
                onDismiss = { showAddDialog = false },
                onSave = { newFood, selectedImageUri ->
                    foodViewModel.addFoodWithImage(newFood, selectedImageUri)
                    showAddDialog = false
                },
                isImageUploading = foodViewModel.isImageUploading.collectAsState().value,
                imageUploadError = foodViewModel.imageUploadError.collectAsState().value
            )
        }

        selectedFood?.let { food ->
            FoodFormDialog(
                food = food,
                categories = categories,
                onDismiss = { selectedFood = null },
                onSave = { updatedFood, selectedImageUri ->
                    Log.d("ManageFoodScreen", "Saving updated food item: ${updatedFood.id}")
                    Log.d("ManageFoodScreen", "Food availability status before save: ${food.foodAvailable}")
                    Log.d("ManageFoodScreen", "Updated food availability status: ${updatedFood.foodAvailable}")
                    
                    foodViewModel.updateFoodWithImage(updatedFood, selectedImageUri)
                    Log.d("ManageFoodScreen", "Called updateFoodItem on ViewModel")
                    
                    selectedFood = null
                },
                isImageUploading = foodViewModel.isImageUploading.collectAsState().value,
                imageUploadError = foodViewModel.imageUploadError.collectAsState().value
            )
        }
    }
}

@Composable
fun AdminFoodItemCard(
    food: Food,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguageCode(context)

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Box {
                AsyncImage(
                    model = food.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
                
                // Show unavailable overlay if the food item is not available
                if (!food.foodAvailable) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.out_of_stock),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = food.getName(currentLang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Add availability badge
                            if (!food.foodAvailable) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = stringResource(R.string.out_of_stock),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        category?.let {
                            Text(
                                text = it.getName(currentLang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val description = food.getDescription(currentLang)
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = currencyManager.formatPrice(food.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Food Item") },
            text = { Text("Are you sure you want to delete this food item?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodFormDialog(
    food: Food?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Food, Uri?) -> Unit,
    isImageUploading: Boolean = false,
    imageUploadError: String? = null,
    foodViewModel: FoodViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguageCode(context)
    
    // Log critical information about the food item when dialog is opened
    DisposableEffect(food) {
        Log.d("FoodDialog", "======= FOOD DIALOG OPENED =======")
        Log.d("FoodDialog", "Food: ${food?.id}, Name: ${food?.getName(currentLang)}")
        Log.d("FoodDialog", "foodAvailable direct from database: ${food?.foodAvailable}")
        Log.d("FoodDialog", "Full Food object: $food")
        Log.d("FoodDialog", "=================================")
        onDispose { 
            Log.d("FoodDialog", "Food dialog closed")
        }
    }
    
    // Translations state - deep copy the translations map to ensure we're not modifying the original object
    // and convert to immutable map for each language
    val nameTranslations = remember {
        mutableStateOf(
            food?.translations?.mapValues { it.value.name }?.toMutableMap()
                ?: Languages.SUPPORTED_LANGUAGES.associateWith { "" }.toMutableMap()
        )
    }
    
    val descriptionTranslations = remember {
        mutableStateOf(
            food?.translations?.mapValues { it.value.description }?.toMutableMap()
                ?: Languages.SUPPORTED_LANGUAGES.associateWith { "" }.toMutableMap()
        )
    }
    
    // Log translations for debugging
    LaunchedEffect(Unit) {
        Log.d("FoodDialog", "Initial name translations: ${nameTranslations.value}")
        Log.d("FoodDialog", "Initial description translations: ${descriptionTranslations.value}")
        Log.d("FoodDialog", "Initial food item: $food")
        Log.d("FoodDialog", "Food foodAvailable status from database: ${food?.foodAvailable}")
    }
    
    // Other fields
    var imageUrl by remember { mutableStateOf(food?.imageUrl ?: "") }
    var price by remember { mutableStateOf(food?.price?.toString() ?: "") }
    var priceError by remember { mutableStateOf<String?>(null) }
    
    // Initialize categoryId properly when categories are updated or dialog is opened
    var categoryId by remember { mutableStateOf(food?.categoryId ?: (categories.firstOrNull()?.id ?: "")) }
    
    // Save the initial availability state from the food item and use that to initialize the state
    var foodAvailable by remember { mutableStateOf(food?.foodAvailable ?: true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Debug log for initial state
    LaunchedEffect(Unit) {
        Log.d("FoodDialog", "Dialog initialized with foodAvailable: ${food?.foodAvailable}")
        Log.d("FoodDialog", "Initial state variable value: $foodAvailable")
    }

    // Make sure the foodAvailable state is correctly initialized when food changes
    LaunchedEffect(food) {
        food?.let {
            Log.d("FoodDialog", "Food item changed, updating foodAvailable to: ${it.foodAvailable}")
            foodAvailable = it.foodAvailable
            Log.d("FoodDialog", "After update, foodAvailable is now: $foodAvailable")
        }
    }

    // Update categoryId if categories change after dialog is opened
    LaunchedEffect(categories) {
        if ((categoryId.isEmpty() || !categories.any { it.id == categoryId }) && categories.isNotEmpty()) {
            categoryId = categories.first().id
        }
    }
    
    // Validation function
    fun validate(): Boolean {
        var isValid = true
        
        // Check that at least one language has a name
        if (nameTranslations.value.values.all { it.isBlank() }) {
            isValid = false
        }
        
        if (price.isBlank() || price.toDoubleOrNull() == null) {
            priceError = "Valid price is required"
            isValid = false
        } else {
            priceError = null
        }
        
        if (imageUrl.isBlank()) {
            isValid = false
        }
        
        if (categoryId.isBlank() || categories.isEmpty()) {
            isValid = false
        }
        
        return isValid
    }
    
    // Build the food item from form data
    fun buildFoodItem(): Food {
        // Create translations map - ensure we have entries for all supported languages
        val translations = Languages.SUPPORTED_LANGUAGES.associateWith { langCode ->
            FoodTranslation(
                name = nameTranslations.value[langCode] ?: "",
                description = descriptionTranslations.value[langCode] ?: ""
            )
        }
        
        // Log the translations before saving
        Log.d("FoodDialog", "Building food item with translations: $translations")
        Log.d("FoodDialog", "Building food item with foodAvailable: $foodAvailable")
        
        return Food(
            id = food?.id ?: "",
            translations = translations,
            price = price.toDoubleOrNull() ?: 0.0,
            imageUrl = imageUrl,
            categoryId = categoryId,
            foodAvailable = foodAvailable,
            // Keep original values for other fields if editing
            discountedPrice = food?.discountedPrice,
            discountPercentage = food?.discountPercentage,
            discountEndDate = food?.discountEndDate,
            discountMessage = food?.discountMessage,
            // Set legacy fields for backward compatibility
            name = nameTranslations.value[currentLang] ?: nameTranslations.value["en"] 
                ?: nameTranslations.value.values.firstOrNull() ?: "",
            description = descriptionTranslations.value[currentLang] ?: descriptionTranslations.value["en"]
                ?: descriptionTranslations.value.values.firstOrNull() ?: "",
            available = foodAvailable
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (food == null) "Add Food Item" else "Edit Food Item")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image picker
                ImagePicker(
                    currentImageUrl = imageUrl,
                    onImageSelected = { uri -> selectedImageUri = uri },
                    label = "Food Image",
                    isUploading = isImageUploading,
                    error = imageUploadError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Multilingual name field
                MultilingualTextField(
                    translations = nameTranslations.value,
                    onTranslationsChange = { updatedTranslations ->
                        Log.d("FoodDialog", "Name translations updated: $updatedTranslations")
                        nameTranslations.value = updatedTranslations.toMutableMap()
                    },
                    label = "Name",
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameTranslations.value.values.all { it.isBlank() },
                    supportingText = {
                        if (nameTranslations.value.values.all { it.isBlank() }) {
                            Text("Name is required in at least one language")
                        }
                    }
                )
                
                // Multilingual description field
                MultilingualTextField(
                    translations = descriptionTranslations.value,
                    onTranslationsChange = { updatedTranslations ->
                        Log.d("FoodDialog", "Description translations updated: $updatedTranslations")
                        descriptionTranslations.value = updatedTranslations.toMutableMap()
                    },
                    label = "Description",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 5
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { 
                        price = it.filter { char -> char.isDigit() || char == '.' }
                        priceError = null
                    },
                    label = { Text("Price (${currencyManager.getSymbol()})") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } }
                )
                
                // Only show category dropdown if there are categories
                if (categories.isNotEmpty()) {
                    var categoryDropdownExpanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        val selectedCategory = categories.find { it.id == categoryId }
                        val selectedCategoryName = selectedCategory?.getName(currentLang) ?: "Select Category"
                        
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.getName(currentLang)) },
                                    onClick = {
                                        categoryId = category.id
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "No categories available. Please create a category first.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Move the foodAvailable checkbox higher in the form and make it more prominent
                FoodAvailabilityToggle(
                    available = foodAvailable,
                    onAvailabilityChanged = { newValue ->
                        Log.d("FoodDialog", "FoodAvailable toggle changed: $foodAvailable -> $newValue")
                        
                        // If we're editing an existing food item, use the specialized toggle method
                        // This ensures both foodAvailable and available fields are updated consistently
                        if (food != null) {
                            foodViewModel.toggleFoodAvailability(food.id, newValue)
                            // Important: Update the local state right away for UI responsiveness
                            // This ensures the toggle reflects the current state immediately
                            foodAvailable = newValue
                            Log.d("FoodDialog", "Updated local foodAvailable state to: $foodAvailable")
                        } else {
                            // For new food items, just update the local state
                            foodAvailable = newValue
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate()) {
                        try {
                            // Log before saving
                            Log.d("FoodDialog", "======= SAVING FOOD ITEM =======")
                            Log.d("FoodDialog", "Food ID: ${food?.id}")
                            Log.d("FoodDialog", "foodAvailable FINAL value before save: $foodAvailable")
                            Log.d("FoodDialog", "Name translations: ${nameTranslations.value}")
                            Log.d("FoodDialog", "Description translations: ${descriptionTranslations.value}")
                            Log.d("FoodDialog", "Saving food with foodAvailable: $foodAvailable")
                            
                            val foodItem = buildFoodItem()
                            Log.d("FoodDialog", "Built food item with foodAvailable: ${foodItem.foodAvailable}")
                            Log.d("FoodDialog", "Food item dump: $foodItem")
                            
                            onSave(foodItem, selectedImageUri)
                            
                            // Log after save is triggered
                            Log.d("FoodDialog", "Save action triggered. FoodAvailable status: ${foodItem.foodAvailable}")
                            Log.d("FoodDialog", "======= SAVE COMPLETE =======")
                            
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("FoodDialog", "Error saving food item: ${e.message}", e)
                        }
                    }
                },
                enabled = validate()
            ) {
                Text(if (food == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 
