package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.BannerItem
import com.example.sofrehmessina.data.repository.BannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri
import android.util.Log
import com.example.sofrehmessina.util.FirebaseStorageManager

@HiltViewModel
class BannerViewModel @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val storageManager: FirebaseStorageManager
) : ViewModel() {

    private val TAG = "BannerViewModel"
    
    // Banner data
    private val _banners = MutableStateFlow<List<BannerItem>>(emptyList())
    val banners: StateFlow<List<BannerItem>> = _banners
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Operation success state
    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess
    
    // Image upload status
    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading
    
    private val _imageUploadError = MutableStateFlow<String?>(null)
    val imageUploadError: StateFlow<String?> = _imageUploadError
    
    init {
        loadBanners()
    }
    
    /**
     * Load banners from repository
     */
    fun loadBanners() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            bannerRepository.getAllBanners() // Use getAllBanners for admin view to show all banners
                .catch { e ->
                    Log.e(TAG, "Error loading banners", e)
                    _error.value = e.message ?: "Unknown error occurred"
                    _isLoading.value = false
                    
                    // If no banners loaded, use sample data as fallback
                    if (_banners.value.isEmpty()) {
                        _banners.value = getSampleBanners()
                    }
                }
                .collect { bannerList ->
                    _banners.value = bannerList.ifEmpty { getSampleBanners() }
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Add a new banner
     */
    fun addBanner(imageUrl: String, title: String, subtitle: String, actionUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            
            try {
                // Create a temporary banner object with a placeholder ID (will be replaced by Firestore)
                val newBanner = BannerItem(
                    id = "temp_${System.currentTimeMillis()}",
                    imageUrl = imageUrl,
                    title = title,
                    subtitle = subtitle,
                    actionUrl = actionUrl
                )
                
                val result = bannerRepository.createBanner(newBanner)
                
                if (result.isSuccess) {
                    // Reload banners to get the updated list with the new banner
                    loadBanners()
                    _operationSuccess.value = "Banner added successfully"
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to add banner"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding banner", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update an existing banner
     */
    fun updateBanner(banner: BannerItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            
            try {
                val result = bannerRepository.updateBanner(banner)
                
                if (result.isSuccess) {
                    // Reload banners to get the updated list
                    loadBanners()
                    _operationSuccess.value = "Banner updated successfully"
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update banner"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating banner", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete a banner
     */
    fun deleteBanner(bannerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            
            try {
                val result = bannerRepository.deleteBanner(bannerId)
                
                if (result.isSuccess) {
                    // Remove the banner from the current list instead of reloading
                    _banners.value = _banners.value.filter { it.id != bannerId }
                    _operationSuccess.value = "Banner deleted successfully"
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to delete banner"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting banner", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle a banner's active status
     */
    fun toggleBannerActive(bannerId: String, active: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            
            try {
                Log.d(TAG, "Toggling banner $bannerId to active=$active")
                val result = bannerRepository.toggleBannerActive(bannerId, active)
                
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully toggled banner active status: $bannerId to $active")
                    // Update the banner in the current list
                    _banners.value = _banners.value.map { 
                        if (it.id == bannerId) {
                            it.copy(active = active)
                        } else {
                            it
                        }
                    }
                    _operationSuccess.value = if (active) "Banner activated" else "Banner deactivated"
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update banner status"
                    Log.e(TAG, "Error toggling banner: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling banner active status", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Reorder a banner
     */
    fun reorderBanner(bannerId: String, newOrder: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            
            try {
                val result = bannerRepository.reorderBanner(bannerId, newOrder)
                
                if (result.isSuccess) {
                    // Reload banners to get the updated order
                    loadBanners()
                    _operationSuccess.value = "Banner order updated"
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to reorder banner"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reordering banner", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _operationSuccess.value = null
    }
    
    /**
     * Provide sample banners as fallback
     */
    private fun getSampleBanners(): List<BannerItem> {
        return listOf(
            BannerItem(
                id = "sample1",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/sofrehmessina.appspot.com/o/banners%2Fsample_banner1.jpg?alt=media",
                title = "Special Offers",
                subtitle = "Enjoy our limited time dishes and discounts",
                actionUrl = ""
            ),
            BannerItem(
                id = "sample2",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/sofrehmessina.appspot.com/o/banners%2Fsample_banner2.jpg?alt=media",
                title = "New Menu Items",
                subtitle = "Try our freshly added Persian delicacies",
                actionUrl = ""
            ),
            BannerItem(
                id = "sample3",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/sofrehmessina.appspot.com/o/banners%2Fsample_banner3.jpg?alt=media",
                title = "Persian Cuisine",
                subtitle = "Authentic flavors from the heart of Iran",
                actionUrl = ""
            )
        )
    }
    
    /**
     * Upload a banner image to Firebase Storage
     * 
     * @param uri URI of the image to upload
     * @param bannerId ID of the banner
     * @return URL of the uploaded image or null if upload failed
     */
    suspend fun uploadBannerImage(uri: Uri, bannerId: String): String? {
        _isImageUploading.value = true
        _imageUploadError.value = null
        
        return try {
            val imageUrl = storageManager.uploadBannerImage(uri, bannerId)
            
            if (imageUrl == null) {
                _imageUploadError.value = "Failed to upload image"
                null
            } else {
                // Update the banner with the new image URL
                val banner = _banners.value.find { it.id == bannerId }
                if (banner != null) {
                    val updatedBanner = banner.copy(imageUrl = imageUrl)
                    val result = bannerRepository.updateBanner(updatedBanner)
                    
                    if (result.isSuccess) {
                        // Update the local state
                        _banners.value = _banners.value.map { 
                            if (it.id == bannerId) updatedBanner else it 
                        }
                    }
                }
                
                imageUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading banner image: ${e.message}", e)
            _imageUploadError.value = e.message ?: "Unknown error occurred"
            null
        } finally {
            _isImageUploading.value = false
        }
    }
    
    /**
     * Add a banner with image upload
     * 
     * @param title Banner title
     * @param subtitle Banner subtitle
     * @param actionUrl Action URL for the banner
     * @param imageUri URI of the image to upload
     */
    fun addBannerWithImage(title: String, subtitle: String, actionUrl: String, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            _isImageUploading.value = imageUri != null
            
            try {
                // Create a temporary banner object with a placeholder ID and image URL
                val tempBanner = BannerItem(
                    id = "temp_${System.currentTimeMillis()}",
                    imageUrl = "",
                    title = title,
                    subtitle = subtitle,
                    actionUrl = actionUrl
                )
                
                // Create the banner in Firestore
                val result = bannerRepository.createBanner(tempBanner)
                
                if (result.isSuccess) {
                    val bannerId = result.getOrThrow()
                    
                    // If there's an image URI, upload it
                    if (imageUri != null) {
                        val imageUrl = uploadBannerImage(imageUri, bannerId)
                        
                        if (imageUrl != null) {
                            // Update the banner with the real image URL
                            val updatedBanner = tempBanner.copy(id = bannerId, imageUrl = imageUrl)
                            bannerRepository.updateBanner(updatedBanner)
                        }
                    }
                    
                    // Reload banners to get the updated list
                    loadBanners()
                    _operationSuccess.value = "Banner added successfully"
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to add banner"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding banner with image: ${e.message}", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                _isImageUploading.value = false
            }
        }
    }
    
    /**
     * Update a banner with image upload
     * 
     * @param banner Banner to update
     * @param imageUri Optional URI of the image to upload
     */
    fun updateBannerWithImage(banner: BannerItem, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _operationSuccess.value = null
            _isImageUploading.value = imageUri != null
            
            try {
                var updatedBanner = banner
                
                // If there's an image URI, upload it
                if (imageUri != null) {
                    val imageUrl = uploadBannerImage(imageUri, banner.id)
                    
                    if (imageUrl != null) {
                        updatedBanner = banner.copy(imageUrl = imageUrl)
                    }
                }
                
                // Update the banner in Firestore
                val result = bannerRepository.updateBanner(updatedBanner)
                
                if (result.isSuccess) {
                    // Update local state
                    _banners.value = _banners.value.map { 
                        if (it.id == banner.id) updatedBanner else it 
                    }
                    _operationSuccess.value = "Banner updated successfully"
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update banner"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating banner with image: ${e.message}", e)
                _error.value = e.message ?: "Unknown error occurred"
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