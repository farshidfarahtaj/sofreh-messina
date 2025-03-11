package com.example.sofrehmessina

import android.app.Application
import android.content.Context
import android.content.res.Configuration as AndroidConfiguration
import android.util.Log
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.util.FirebaseTokenManager
import com.example.sofrehmessina.util.ImageCacheManager
import com.example.sofrehmessina.util.LocaleHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.example.sofrehmessina.di.LanguageHelper
import com.example.sofrehmessina.utils.HiltComponentManager
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModelFactory
import com.google.firebase.messaging.FirebaseMessaging
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkManager
import com.example.sofrehmessina.worker.DataSyncWorker
import com.example.sofrehmessina.worker.NotificationSyncWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import kotlinx.coroutines.cancel
import java.util.concurrent.TimeUnit
import java.io.File
import androidx.work.NetworkType
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Configuration as WorkConfiguration
import androidx.work.Constraints

/**
 * Custom Application class for SofrehMessina with Hilt integration
 */
@HiltAndroidApp
class SofrehMessinaApp : Application(), HiltComponentManager, ViewModelStoreOwner, androidx.work.Configuration.Provider {
    companion object {
        private const val TAG = "SofrehMessinaApp"
        
        // WorkManager configuration
        private object WorkConfig {
            const val DATA_SYNC_WORK_NAME = "data_sync_work"
            const val NOTIFICATION_SYNC_WORK_NAME = "notification_sync_work"
            const val DATA_SYNC_INTERVAL_HOURS = 2L
            const val NOTIFICATION_SYNC_INTERVAL_MINUTES = 15L
        }

        // Global reference to the app instance
        private lateinit var instance: SofrehMessinaApp

        fun getInstance(): SofrehMessinaApp {
            return instance
        }
    }

    //region Firebase Services
    @Inject
    lateinit var firebaseRepository: FirebaseRepository

    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    //endregion

    //region App Services
    @Inject
    lateinit var tokenManager: FirebaseTokenManager

    @Inject
    override lateinit var imageCacheManager: ImageCacheManager

    @Inject
    lateinit var languageHelper: LanguageHelper
    //endregion

    //region Hilt & WorkManager
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    //endregion

    //region ViewModels & Factories
    @Inject
    lateinit var sharedImageViewModelFactory: SharedImageViewModelFactory
    
    // ViewModel store for application-level ViewModels
    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    // Implementation of ViewModelStoreOwner interface
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
    //endregion

    // Application-level coroutine scope
    private lateinit var appScope: CoroutineScope

    override fun attachBaseContext(base: Context) {
        // Cannot use languageHelper here as it's not initialized yet
        // Apply stored language before the app context is attached using static method
        val languageCode = LocaleHelper.getSelectedLanguageCode(base)
        val contextWrapper = LocaleHelper.updateLocale(base, languageCode)
        super.attachBaseContext(contextWrapper)
    }

    override fun onConfigurationChanged(newConfig: AndroidConfiguration) {
        super.onConfigurationChanged(newConfig)
        // Apply stored language when system configuration changes
        if (::languageHelper.isInitialized) {
            languageHelper.onConfigurationChanged()
        } else {
            // Fallback if languageHelper is not initialized yet
            val languageCode = LocaleHelper.getSelectedLanguageCode(this)
            LocaleHelper.updateLocale(this, languageCode)
        }
    }

    override fun onCreate() {
        val startTime = System.currentTimeMillis()
        super.onCreate()

        // Store instance reference
        instance = this

        // Create app scope
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        // Setup Bluetooth crash handling mechanism
        setupBluetoothCrashHandler()

        // CRITICAL: Initialize language first (affects UI)
        if (::languageHelper.isInitialized) {
            languageHelper.applyLanguage()
        } else {
            // Fallback if languageHelper is not initialized yet
            LocaleHelper.applyLanguage(this)
        }
        
        // CRITICAL: Initialize Firebase (authentication & data)
        initializeFirebase()
        
        // CRITICAL: Set up Firebase Crashlytics early to catch any initialization errors
        initializeCrashlytics()

        // IMPORTANT BUT ASYNC: Initialize Firebase App Check in a coroutine
        appScope.launch {
            initializeAppCheck()
        }

        // IMPORTANT: Set up WorkManager configuration
        setupWorkManager()

        // NON-CRITICAL: Initialize these in parallel
        appScope.launch(Dispatchers.IO) {
            // Initialize cache management
            initializeImageCache()
            
            // Prefetch important images with a delay to not interfere with startup
            delay(1000)
            prefetchImportantImages()
        }

        // NON-CRITICAL: Initialize theme
        appScope.launch {
            initializeTheme()
        }

        // NON-CRITICAL: Restore shader cache
        appScope.launch(Dispatchers.IO) {
            restoreShaderCache()
        }

        val initTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Application initialized in $initTime ms")
    }

    /**
     * Sets up a global exception handler to catch and handle Bluetooth-related crashes
     * This prevents the app from crashing when system Bluetooth issues occur
     */
    private fun setupBluetoothCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Check if exception is Bluetooth-related
            val isBluetoothIssue = throwable.stackTraceToString().let { stackTrace ->
                stackTrace.contains("bluetooth", ignoreCase = true) ||
                stackTrace.contains("bt_", ignoreCase = true) ||
                stackTrace.contains("bt.", ignoreCase = true) ||
                stackTrace.contains("Hardware Error Event with code 0x42")
            }
            
            if (isBluetoothIssue) {
                // Log the Bluetooth error but don't crash
                Log.e(TAG, "Caught Bluetooth system exception", throwable)
                FirebaseCrashlytics.getInstance().recordException(throwable)
                
                // Only suppress certain Bluetooth errors
                if (throwable.message?.contains("Hardware Error Event with code 0x42") == true) {
                    Log.w(TAG, "Suppressing common Bluetooth hardware error")
                    // Don't propagate this specific error
                    return@setDefaultUncaughtExceptionHandler
                }
            }
            
            // For all other exceptions or Bluetooth errors we don't specifically handle,
            // pass to the default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Initializes the image cache manager and ensures cache directories exist.
     */
    private suspend fun initializeImageCache() {
        try {
            // Ensure cache directories exist and are writable
            imageCacheManager.ensureCacheDirectoriesExist()
            
            // Clear old cache files if needed (older than 7 days)
            imageCacheManager.cleanupOldCacheFiles(maxAgeMillis = 7 * 24 * 60 * 60 * 1000L)

            Log.d(TAG, "Image cache initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing image cache: ${e.message}")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Prefetches important images for the app to ensure they're available offline
     */
    private suspend fun prefetchImportantImages() {
        try {
            Log.d(TAG, "Starting prefetch of important images")

            // Get paths to prefetch from Firebase or fallback to defaults
            val paths = getCommonImagePaths()
            
            if (paths.isEmpty()) {
                Log.w(TAG, "No images to prefetch")
                return
            }

            Log.d(TAG, "Found ${paths.size} images to prefetch")

            // Convert paths to storage references
            val storage = FirebaseStorage.getInstance()
            val imageRefs = paths.map { path -> storage.reference.child(path) }

            // Use a timeout to prevent hanging if Firebase is slow (30 seconds is more reasonable)
            withTimeoutOrNull(30000) {
                // Try first approach with ViewModel
                try {
                    val viewModel = provideSharedImageViewModel()
                    viewModel.prefetchImages(imageRefs)
                } catch (e: Exception) {
                    // Fallback to direct prefetching if ViewModel approach fails
                    Log.w(TAG, "Falling back to direct image prefetching: ${e.message}")
                    imageCacheManager.prefetchImages(imageRefs)
                }
                
                Log.d(TAG, "Image prefetching completed successfully")
            } ?: Log.w(TAG, "Image prefetching timed out")
        } catch (e: Exception) {
            Log.e(TAG, "Error prefetching images: ${e.message}")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Gets a list of common image paths to prefetch
     * Tries to fetch from Firebase or falls back to hardcoded common paths
     */
    private suspend fun getCommonImagePaths(): List<String> {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Try to get image paths from a config document with a timeout
            val configDoc = withTimeoutOrNull(5000) {
                firestore.collection("config")
                    .document("imagePrefetch")
                    .get()
                    .await()
            }

            if (configDoc != null) {
                // Use safer cast with explicit type check
                val pathsObj = configDoc.get("paths")
                val configPaths = when {
                    pathsObj is List<*> && pathsObj.all { it is String } -> pathsObj.map { it as String }
                    else -> null
                }

                if (!configPaths.isNullOrEmpty()) {
                    Log.d(TAG, "Using ${configPaths.size} image paths from Firebase config")
                    return configPaths
                }
            }

            // Try to get actual category images with a timeout
            val categories = withTimeoutOrNull(5000) {
                firestore.collection("categories")
                    .limit(10) // Limit to 10 categories for performance
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("imageUrl") }
                    .filter { it.isNotEmpty() }
            } ?: emptyList()

            if (categories.isNotEmpty()) {
                // Add essential UI elements
                val combinedPaths = categories + listOf(
                    "logo/logo.png",
                    "icons/placeholder.png"
                )

                Log.d(TAG, "Using ${categories.size} category images + 2 essential images")
                return combinedPaths
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching image paths from Firebase: ${e.message}")
        }

        // Minimal fallback list with essential images
        return listOf(
            "logo/logo.png",
            "icons/placeholder.png"
        )
    }

    /**
     * Initializes Firebase services
     */
    private fun initializeFirebase() {
        try {
            // Initialize Firebase once
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
            
            // Log Firebase project information
            val firebaseApp = FirebaseApp.getInstance()
            Log.d(TAG, "Firebase app: ${firebaseApp.name}, project: ${firebaseApp.options.projectId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Initializes Firebase App Check for security
     */
    private fun initializeAppCheck() {
        try {
            // Don't re-initialize Firebase app here since it's already done in initializeFirebase()
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Firebase App Check initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase App Check: ${e.message}")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Provides the SharedImageViewModel for non-Hilt contexts
     * Uses our custom factory
     */
    override fun provideSharedImageViewModel(): SharedImageViewModel {
        // Create a ViewModelStore
        val vmStore = ViewModelStore()

        // Create a ViewModelStoreOwner
        val vmStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = vmStore
        }

        // Use our injected factory to create the ViewModel using indexing
        return ViewModelProvider(vmStoreOwner, sharedImageViewModelFactory)[SharedImageViewModel::class.java]
    }

    /**
     * Initializes Firebase Crashlytics
     */
    private fun initializeCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            
            // Set user identifier for better crash reporting
            firebaseAuth.currentUser?.uid?.let { userId ->
                crashlytics.setUserId(userId)
            }
            
            // Set common keys for better debugging
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            
            Log.d(TAG, "Crashlytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Crashlytics: ${e.message}")
        }
    }

    /**
     * Initializes the application theme from saved settings
     */
    private fun initializeTheme() {
        // Code to initialize theme goes here
    }

    /**
     * Configures and schedules periodic work with WorkManager
     */
    private fun setupWorkManager() {
        try {
            // Schedule data sync worker with NetworkType.CONNECTED constraint
            // This ensures it runs only when there's a network connection
            val dataSyncConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true) // Don't run when battery is low
                .build()
                
            // Schedule data sync worker to run every 2 hours
            val dataSyncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                WorkConfig.DATA_SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
            .setConstraints(dataSyncConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .addTag("sync")
            .addTag("data_sync")
            .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WorkConfig.DATA_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dataSyncRequest
            )
            
            // Also schedule a one-time immediate sync to ensure data is cached right away
            val immediateDataSyncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setConstraints(dataSyncConstraints)
                .addTag("initial_sync")
                .build()
                
            WorkManager.getInstance(this).enqueue(immediateDataSyncRequest)

            // Schedule notification sync worker
            val notificationSyncRequest = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                WorkConfig.NOTIFICATION_SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
            .addTag("sync")
            .addTag("notification_sync")
            .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WorkConfig.NOTIFICATION_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                notificationSyncRequest
            )

            Log.d(TAG, "WorkManager background tasks scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling WorkManager tasks: ${e.message}")
        }
    }

    /**
     * Restores shader cache from persistent storage on app startup
     * This ensures that GPU shaders are available immediately after app restart
     */
    private fun restoreShaderCache() {
        // Only perform this on devices with API level < 28, as newer Android versions handle this automatically
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Log.d(TAG, "Skipping shader cache restoration on Android P+ (handled by system)")
            return
        }
        
        appScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to restore shader cache")
                
                // Source directory in persistent storage
                val persistentRoot = File(this@SofrehMessinaApp.filesDir, "persistent_cache")
                val shaderCacheSource = File(persistentRoot, "shader_cache")
                
                if (!shaderCacheSource.exists() || !shaderCacheSource.isDirectory) {
                    Log.d(TAG, "No shader cache found in persistent storage")
                    return@launch
                }
                
                // Primary destination - most common location
                val primaryDestination = File(cacheDir, "com.android.opengl.shaders_cache")
                
                try {
                    if (!primaryDestination.exists()) {
                        primaryDestination.mkdirs()
                    }
                    
                    // Only copy files that are newer in the source
                    val sourceFiles = shaderCacheSource.listFiles() ?: emptyArray()
                    var copiedCount = 0
                    
                    for (sourceFile in sourceFiles) {
                        val destFile = File(primaryDestination, sourceFile.name)
                        if (!destFile.exists() || sourceFile.lastModified() > destFile.lastModified()) {
                            sourceFile.copyTo(destFile, overwrite = true)
                            copiedCount++
                        }
                    }
                    
                    Log.d(TAG, "Restored $copiedCount shader cache files")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore shader cache: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in shader cache restoration: ${e.message}")
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        Log.d(TAG, "Application terminating, cleaning up resources")
        
        // Create a new scope for cleanup to avoid using the cancelled appScope
        val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        try {
            // Clean up FCM resources on app termination
            if (::tokenManager.isInitialized) {
                tokenManager.cleanupFcmResources()
            }
            
            // Clean up ViewModel store
            appViewModelStore.clear()
            
            // Clean up Firebase Messaging token
            cleanupScope.launch {
                try {
                    FirebaseMessaging.getInstance().deleteToken().await()
                    Log.d(TAG, "FCM token deleted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting FCM token: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        } finally {
            // Cancel app scope
            if (::appScope.isInitialized) {
                appScope.cancel("Application terminated")
            }
            
            // Give a little time for cleanup tasks to complete
            cleanupScope.launch {
                delay(500)
                cleanupScope.cancel()
            }
        }
    }

    /**
     * Implementation for Configuration.Provider to provide WorkManager configuration
     */
    override fun getWorkManagerConfiguration(): WorkConfiguration {
        return WorkConfiguration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
    }
}