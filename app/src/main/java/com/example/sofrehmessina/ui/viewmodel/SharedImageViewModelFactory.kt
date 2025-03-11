package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sofrehmessina.util.ImageCacheManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating SharedImageViewModel instances.
 * This factory ensures proper dependency injection without directly injecting
 * the @HiltViewModel into the Application class.
 */
@Singleton
class SharedImageViewModelFactory @Inject constructor(
    private val imageCacheManager: ImageCacheManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedImageViewModel::class.java)) {
            return SharedImageViewModel(imageCacheManager) as T
        }
        
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
} 