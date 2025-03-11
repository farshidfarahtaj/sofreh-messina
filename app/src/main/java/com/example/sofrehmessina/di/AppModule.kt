package com.example.sofrehmessina.di

import android.content.Context
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.util.ImageCacheManager
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.di.LanguageHelper
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModelFactory
import com.example.sofrehmessina.util.AutoLogoutManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCurrencyManager(
        @ApplicationContext context: Context,
        firebaseRepository: FirebaseRepository
    ): CurrencyManager {
        return CurrencyManager(context, firebaseRepository)
    }
    
    @Provides
    @Singleton
    fun provideImageCacheManager(
        @ApplicationContext context: Context
    ): ImageCacheManager {
        return ImageCacheManager(context)
    }
    
    @Provides
    @Singleton
    fun provideLanguageHelper(
        @ApplicationContext context: Context
    ): LanguageHelper {
        return LanguageHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideSharedImageViewModelFactory(
        imageCacheManager: ImageCacheManager
    ): SharedImageViewModelFactory {
        return SharedImageViewModelFactory(imageCacheManager)
    }

    @Provides
    @Singleton
    fun provideAutoLogoutManager(
        @ApplicationContext context: Context,
        firebaseRepository: FirebaseRepository
    ): AutoLogoutManager {
        return AutoLogoutManager(context, firebaseRepository)
    }
} 