package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.ui.components.FoodItemCard
import com.example.sofrehmessina.ui.viewmodel.FoodViewModel
import com.example.sofrehmessina.ui.viewmodel.CategoryViewModel
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("unused")
fun SearchScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    foodViewModel: FoodViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var priceRange by remember { mutableStateOf(0f..100f) }
    var showPriceRangeDialog by remember { mutableStateOf(false) }
    var tempPriceRange by remember { mutableStateOf(priceRange) }

    val foodItems by foodViewModel.foodItems.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()

    LaunchedEffect(Unit) {
        foodViewModel.loadAllFoodItems()
        categoryViewModel.loadCategories()
    }

    LaunchedEffect(showPriceRangeDialog) {
        if (showPriceRangeDialog) {
            tempPriceRange = priceRange
        }
    }

    val filteredItems = remember(foodItems, searchQuery, selectedCategoryId, priceRange) {
        foodItems.filter { food ->
            val matchesQuery = food.name.contains(searchQuery, ignoreCase = true) ||
                food.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryId == null || food.categoryId == selectedCategoryId
            val matchesPriceRange = food.price >= priceRange.start && food.price <= priceRange.endInclusive
            matchesQuery && matchesCategory && matchesPriceRange
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search food items...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Active Filters
            if (selectedCategoryId != null || priceRange != 0f..100f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCategoryId?.let { categoryId ->
                        categories.find { it.id == categoryId }?.let { category ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedCategoryId = null },
                                label = { Text(category.name) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear category filter"
                                    )
                                }
                            )
                        }
                    }
                    if (priceRange != 0f..100f) {
                        FilterChip(
                            selected = true,
                            onClick = { showPriceRangeDialog = true },
                            label = { Text("$${priceRange.start.toInt()} - $${priceRange.endInclusive.toInt()}") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit price range"
                                )
                            }
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Start typing to search..."
                        else "No items found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = filteredItems,
                        key = { it.id }
                    ) { food ->
                        FoodItemCard(
                            food = food,
                            onClick = { navController.navigate("food/${food.id}") }
                        )
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter By Category") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedCategoryId == null,
                            onClick = {
                                selectedCategoryId = null
                                showFilterDialog = false
                            }
                        )
                        Text(
                            text = "All Categories",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == category.id,
                                onClick = {
                                    selectedCategoryId = category.id
                                    showFilterDialog = false
                                }
                            )
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Button(
                        onClick = {
                            showFilterDialog = false
                            showPriceRangeDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set Price Range")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPriceRangeDialog) {
        AlertDialog(
            onDismissRequest = { showPriceRangeDialog = false },
            title = { Text("Set Price Range") },
            text = {
                Column {
                    Text(
                        text = "$${tempPriceRange.start.toInt()} - $${tempPriceRange.endInclusive.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    RangeSlider(
                        value = tempPriceRange,
                        onValueChange = { tempPriceRange = it },
                        valueRange = 0f..100f,
                        steps = 19
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$0")
                        Text("$100")
                    }
                    TextButton(
                        onClick = {
                            priceRange = 0f..100f
                            tempPriceRange = 0f..100f
                            showPriceRangeDialog = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        priceRange = tempPriceRange
                        showPriceRangeDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceRangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 