package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.CategoryTranslation
import com.example.sofrehmessina.data.model.Languages
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.components.CategoryCard
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.CategoryViewModel
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.sofrehmessina.ui.components.MultilingualTextField
import com.example.sofrehmessina.ui.theme.SofrehMessinaTheme
import com.example.sofrehmessina.util.LocaleHelper
import com.example.sofrehmessina.util.Resource
import com.example.sofrehmessina.util.TranslationManager
import android.util.Log
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import com.example.sofrehmessina.ui.components.KeyboardAwareDialogContent
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import android.net.Uri
import com.example.sofrehmessina.ui.components.ImagePicker

sealed interface CategoryState {
    data object Loading : CategoryState
    data class Success(val categories: List<Category>) : CategoryState
    data class Error(val message: String) : CategoryState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        var showAddDialog by remember { mutableStateOf(false) }
        var selectedCategory by remember { mutableStateOf<Category?>(null) }
        var showErrorSnackbar by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val categories by viewModel.categories.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        val categoryState = when {
            isLoading -> CategoryState.Loading
            error != null -> CategoryState.Error(error ?: "Unknown error")
            else -> CategoryState.Success(categories)
        }

        LaunchedEffect(error) {
            if (error != null) {
                showErrorSnackbar = true
                errorMessage = error
            }
        }

        LaunchedEffect(Unit) {
            try {
                viewModel.loadCategories()
            } catch (e: Exception) {
                Log.e("ManageCategoriesScreen", "Error loading categories: ${e.message}", e)
                errorMessage = "Failed to load categories: ${e.message}"
                showErrorSnackbar = true
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
                        title = { Text("Manage Categories") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Category")
                            }
                        }
                    )
                },
                snackbarHost = {
                    if (showErrorSnackbar && errorMessage != null) {
                        SnackbarHost(
                            hostState = remember { SnackbarHostState() }
                        ) {
                            Snackbar(
                                action = {
                                    TextButton(onClick = {
                                        showErrorSnackbar = false
                                        viewModel.clearError()
                                    }) {
                                        Text("Dismiss")
                                    }
                                },
                                dismissAction = {
                                    IconButton(onClick = {
                                        showErrorSnackbar = false
                                        viewModel.clearError()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                    }
                                }
                            ) {
                                Text(errorMessage ?: "Unknown error")
                            }
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (categoryState) {
                        is CategoryState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is CategoryState.Error -> {
                            Text(
                                text = categoryState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }
                        is CategoryState.Success -> {
                            if (categories.isEmpty()) {
                                Text(
                                    text = "No categories found",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp)
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(
                                        items = categories,
                                        key = { it.id }
                                    ) { category ->
                                        CategoryCardWithErrorHandling(
                                            category = category,
                                            onClick = { selectedCategory = category }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Move the CategoryDialog inside the CompositionLocalProvider
        if (showAddDialog || selectedCategory != null) {
            CategoryDialog(
                category = selectedCategory,
                onDismiss = {
                    showAddDialog = false
                    selectedCategory = null
                },
                onSave = { newCategory, imageUri ->
                    if (selectedCategory != null) {
                        viewModel.updateCategoryWithImage(newCategory, imageUri)
                    } else {
                        viewModel.addCategoryWithImage(newCategory, imageUri)
                    }
                    showAddDialog = false
                    selectedCategory = null
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (category: Category, imageUri: Uri?) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguageCode(context)
    
    // Translations state
    val nameTranslations = remember {
        mutableStateOf(
            category?.translations?.mapValues { it.value.name }?.toMutableMap()
                ?: Languages.SUPPORTED_LANGUAGES.associateWith { "" }.toMutableMap()
        )
    }
    
    val descriptionTranslations = remember {
        mutableStateOf(
            category?.translations?.mapValues { it.value.description }?.toMutableMap()
                ?: Languages.SUPPORTED_LANGUAGES.associateWith { "" }.toMutableMap()
        )
    }
    
    var imageUrl by remember { mutableStateOf(category?.imageUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // View model states
    val isImageUploading by viewModel.isImageUploading.collectAsState()
    val imageUploadError by viewModel.imageUploadError.collectAsState()
    
    // Check if the form is valid
    val isFormValid = nameTranslations.value[currentLang]?.isNotEmpty() == true
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
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
                    label = "Category Image",
                    isUploading = isImageUploading,
                    error = imageUploadError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Multilingual name field
                MultilingualTextField(
                    translations = nameTranslations.value,
                    onTranslationsChange = { updatedTranslations ->
                        nameTranslations.value = updatedTranslations.toMutableMap()
                    },
                    label = "Name",
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameTranslations.value[currentLang]?.isEmpty() == true
                )
                
                // Multilingual description field
                MultilingualTextField(
                    translations = descriptionTranslations.value,
                    onTranslationsChange = { updatedTranslations ->
                        descriptionTranslations.value = updatedTranslations.toMutableMap()
                    },
                    label = "Description",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newCategory = (category ?: Category(
                        id = "",
                        name = nameTranslations.value[currentLang] ?: "",
                        description = descriptionTranslations.value[currentLang] ?: "",
                        imageUrl = imageUrl,
                        translations = createCategoryTranslations(nameTranslations.value, descriptionTranslations.value)
                    )).copy(
                        name = nameTranslations.value[currentLang] ?: "",
                        description = descriptionTranslations.value[currentLang] ?: "",
                        imageUrl = imageUrl,
                        translations = createCategoryTranslations(nameTranslations.value, descriptionTranslations.value)
                    )
                    
                    onSave(newCategory, selectedImageUri)
                },
                enabled = isFormValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Helper function to create category translations
 */
private fun createCategoryTranslations(
    nameTranslations: Map<String, String>,
    descriptionTranslations: Map<String, String>
): Map<String, CategoryTranslation> {
    return Languages.SUPPORTED_LANGUAGES.associateWith { langCode ->
        CategoryTranslation(
            name = nameTranslations[langCode] ?: "",
            description = descriptionTranslations[langCode] ?: ""
        )
    }
}

@Composable
fun CategoryCardWithErrorHandling(
    category: Category,
    onClick: () -> Unit
) {
    // Use remember and mutableStateOf to track if there was an error
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Simple logging, no need for try-catch here
    LaunchedEffect(category.id) {
        Log.d("CategoryCardWithErrorHandling", "Preparing to render category with ID: ${category.id}")
    }
    
    // Handle extreme cases where we can detect issues before rendering
    LaunchedEffect(category.imageUrl) {
        if (category.imageUrl.isBlank()) {
            Log.w("CategoryCardWithErrorHandling", "Empty image URL for category: ${category.id}")
            // We don't set hasError here because the CategoryCard will handle this gracefully
        }
    }
    
    // Use a proper function reference instead of a lambda with implicit unit conversion
    val handleCardClick = onClick
    
    // Handler for errors reported by CategoryCard
    val handleCardError = { error: Throwable ->
        Log.e("CategoryCardWithErrorHandling", "Error reported by CategoryCard: ${error.message}", error)
        errorMessage = error.message
        hasError = true
    }
    
    // Render the appropriate UI based on the error state
    if (hasError) {
        // Render error placeholder card
        Card(
            onClick = { },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "Error loading category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    } else {
        // Use the CategoryCard with error reporting
        CategoryCard(
            category = category,
            onClick = handleCardClick,
            modifier = Modifier.padding(8.dp),
            onError = handleCardError
        )
    }
} 