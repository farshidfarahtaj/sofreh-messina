package com.example.sofrehmessina.utils

import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import com.example.sofrehmessina.util.ImageCacheManager

/**
 * Interface for providing Hilt-managed components to non-Hilt contexts
 * This allows accessing dependency-injected components from places where
 * Hilt injection is not directly possible, like the Application class
 */
interface HiltComponentManager {
    /**
     * Provides access to the ImageCacheManager
     */
    val imageCacheManager: ImageCacheManager
    
    /**
     * Provides a properly-initialized SharedImageViewModel
     */
    fun provideSharedImageViewModel(): SharedImageViewModel
} 