package com.example.sofrehmessina.di

import android.content.Context
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for properly injecting Google Play Services dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleServicesModule {
    
    private const val TAG = "GoogleServicesModule"
    
    /**
     * Checks if Google Play Services are available and properly configured
     */
    @Provides
    @Singleton
    fun provideGoogleApiAvailability(): GoogleApiAvailability {
        return GoogleApiAvailability.getInstance()
    }
    
    /**
     * Checks if Google Play Services are available
     */
    @Provides
    @Singleton
    fun provideGooglePlayServicesAvailability(
        @ApplicationContext context: Context,
        googleApiAvailability: GoogleApiAvailability
    ): Boolean {
        try {
            val result = googleApiAvailability.isGooglePlayServicesAvailable(context)
            val isAvailable = result == com.google.android.gms.common.ConnectionResult.SUCCESS
            
            if (!isAvailable) {
                Log.w(TAG, "Google Play Services not available, status code: $result")
                
                // Check if it's resolvable
                if (googleApiAvailability.isUserResolvableError(result)) {
                    Log.i(TAG, "Error is user resolvable")
                } else {
                    Log.e(TAG, "Error is not user resolvable")
                }
            } else {
                Log.i(TAG, "Google Play Services available")
            }
            
            return isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services availability: ${e.message}", e)
            return false
        }
    }
} 