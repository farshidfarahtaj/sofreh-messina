package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.sofrehmessina.util.FirebaseStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val storageManager: FirebaseStorageManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val TAG = "CategoryViewModel"
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Image upload status
    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading
    
    private val _imageUploadError = MutableStateFlow<String?>(null)
    val imageUploadError: StateFlow<String?> = _imageUploadError
    
    // Track the language in use to detect changes
    private var currentLanguage = LocaleHelper.getSelectedLanguageCode(context)

    init {
        Log.d(TAG, "Initializing CategoryViewModel with language: $currentLanguage")
        // Force refresh categories on init to ensure we have current language data
        forceRefreshCategories()
    }
    
    fun forceRefreshCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Force repository to clear cache and reload from server
                repository.refreshCategories()
                loadCategories()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val category = _categories.value.find { it.id == categoryId }
                if (category != null) {
                    _selectedCategory.value = category
                } else {
                    _error.value = "Category not found"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if language has changed since last load
                val currentLocale = LocaleHelper.getSelectedLanguageCode(context)
                if (currentLocale != currentLanguage) {
                    Log.d(TAG, "Language changed from $currentLanguage to $currentLocale, forcing refresh")
                    currentLanguage = currentLocale
                    repository.refreshCategories()
                }
                
                var firstLoad = true
                repository.getCategories().collect { categories ->
                    _categories.value = categories
                    
                    // Set loading to false after receiving the first data
                    if (firstLoad) {
                        _isLoading.value = false
                        firstLoad = false
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    // Call this method when the language changes to refresh the category data
    fun refreshCategoriesForLanguageChange() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing categories for language change")
                // Update the tracked language
                currentLanguage = LocaleHelper.getSelectedLanguageCode(context)
                
                // First, refresh the categories in the repository
                repository.refreshCategories()
                
                // Then reload the categories to update the UI
                repository.getCategories().collect { categories ->
                    _categories.value = categories
                    
                    // If we have a selected category, refresh it too
                    _selectedCategory.value?.let { currentCategory ->
                        val updatedCategory = categories.find { it.id == currentCategory.id }
                        if (updatedCategory != null) {
                            _selectedCategory.value = updatedCategory
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e(TAG, "Error refreshing categories for language change: ${e.message}", e)
            }
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addCategory(category)
                    .onSuccess { categoryId ->
                        val newCategory = category.copy(id = categoryId)
                        _categories.value = _categories.value + newCategory
                        loadCategories()
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateCategory(category)
                    .onSuccess {
                        loadCategories()
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteCategory(categoryId)
                    .onSuccess {
                        loadCategories()
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Upload a category image to Firebase Storage
     * 
     * @param uri URI of the image to upload
     * @param categoryId ID of the category
     * @return URL of the uploaded image or null if upload failed
     */
    suspend fun uploadCategoryImage(uri: Uri, categoryId: String): String? {
        _isImageUploading.value = true
        _imageUploadError.value = null
        
        return try {
            val imageUrl = storageManager.uploadCategoryImage(uri, categoryId)
            
            if (imageUrl == null) {
                _imageUploadError.value = "Failed to upload image"
                null
            } else {
                // Update the category with the new image URL
                val category = _categories.value.find { it.id == categoryId }
                if (category != null) {
                    val updatedCategory = category.copy(imageUrl = imageUrl)
                    repository.updateCategory(updatedCategory)
                    
                    // Update the local state
                    _selectedCategory.value = updatedCategory
                    _categories.value = _categories.value.map { 
                        if (it.id == categoryId) updatedCategory else it 
                    }
                }
                
                imageUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading category image: ${e.message}", e)
            _imageUploadError.value = e.message ?: "Unknown error occurred"
            null
        } finally {
            _isImageUploading.value = false
        }
    }
    
    /**
     * Add a category with image upload
     * 
     * @param category Category to add
     * @param imageUri Optional URI of the image to upload
     */
    fun addCategoryWithImage(category: Category, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isImageUploading.value = imageUri != null
            
            try {
                // First add the category without an image
                val result = repository.addCategory(category)
                
                if (result.isSuccess) {
                    val categoryId = result.getOrThrow()
                    var newCategory = category.copy(id = categoryId)
                    
                    // If there's an image URI, upload it
                    if (imageUri != null) {
                        val imageUrl = uploadCategoryImage(imageUri, categoryId)
                        if (imageUrl != null) {
                            // Update the category with the image URL
                            newCategory = newCategory.copy(imageUrl = imageUrl)
                            repository.updateCategory(newCategory)
                        }
                    }
                    
                    // Refresh the categories to update the UI
                    loadCategories()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to add category"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e(TAG, "Error adding category with image: ${e.message}", e)
            } finally {
                _isLoading.value = false
                _isImageUploading.value = false
            }
        }
    }
    
    /**
     * Update a category with image upload
     * 
     * @param category Category to update
     * @param imageUri Optional URI of the image to upload
     */
    fun updateCategoryWithImage(category: Category, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isImageUploading.value = imageUri != null
            
            try {
                var updatedCategory = category
                
                // If there's an image URI, upload it
                if (imageUri != null) {
                    val imageUrl = uploadCategoryImage(imageUri, category.id)
                    if (imageUrl != null) {
                        updatedCategory = category.copy(imageUrl = imageUrl)
                    }
                }
                
                // Update the category in Firestore
                val result = repository.updateCategory(updatedCategory)
                
                if (result.isSuccess) {
                    // Update local state and refresh
                    _selectedCategory.value = updatedCategory
                    loadCategories()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update category"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e(TAG, "Error updating category with image: ${e.message}", e)
            } finally {
                _isLoading.value = false
                _isImageUploading.value = false
            }
        }
    }
    
    /**
     * Clear image upload error
     */
    fun clearImageUploadError() {
        _imageUploadError.value = null
    }
} 