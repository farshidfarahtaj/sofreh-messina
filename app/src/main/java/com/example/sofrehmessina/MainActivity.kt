package com.example.sofrehmessina

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sofrehmessina.di.FirebaseRepositoryEntryPoint
import com.example.sofrehmessina.navigation.AppNavGraph
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.BottomNavItem
import com.example.sofrehmessina.ui.theme.SofrehMessinaTheme
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import com.example.sofrehmessina.ui.viewmodel.LocalCartViewModel
import com.example.sofrehmessina.ui.screens.user.SettingsViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import android.content.ComponentCallbacks2
import android.content.Context
import com.example.sofrehmessina.util.LocaleHelper
import com.example.sofrehmessina.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import com.example.sofrehmessina.util.AutoLogoutManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val cartViewModel: CartViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val categoryViewModel: com.example.sofrehmessina.ui.viewmodel.CategoryViewModel by viewModels()
    private val foodViewModel: com.example.sofrehmessina.ui.viewmodel.FoodViewModel by viewModels()
    private val TAG = "MainActivity"
    
    @Inject
    lateinit var firebaseRepository: FirebaseRepository
    
    @Inject
    lateinit var autoLogoutManager: AutoLogoutManager
    
    // Get the repository directly
    private val entryPoint by lazy {
        EntryPointAccessors.fromActivity(
            this,
            FirebaseRepositoryEntryPoint::class.java
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sync auto-logout settings on app start to ensure we have the latest
        lifecycleScope.launch {
            try {
                autoLogoutManager.syncAutoLogoutSettings()
                Log.d(TAG, "Auto-logout settings synced on app start")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing auto-logout settings: ${e.message}", e)
            }
        }
        
        // Set up back handler
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    finishAfterTransition()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        
        // Ensure proper window configuration
        window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Initialize Firebase if not already initialized
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            
            if (isGooglePlayServicesAvailable()) {
                askNotificationPermission()
            }
            
            // Handle if app was opened from a notification
            handleNotificationIntent()
        } catch (e: Exception) {
            Log.e(TAG, "Error during app initialization", e)
        }
        
        setContent {
            SofrehMessinaTheme {
                AppContent()
            }
        }
    }
    
    @Composable
    private fun AppContent() {
        // A surface container using the 'background' color from the theme
        LifecycleObserver()
        MainScreen()
    }
    
    @Composable
    private fun LifecycleObserver() {
        val lifecycleOwner = LocalLifecycleOwner.current
        
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        Log.d(TAG, "Lifecycle: onCreate")
                    }
                    Lifecycle.Event.ON_STOP -> {
                        Log.d(TAG, "Lifecycle: onStop")
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        Log.d(TAG, "Lifecycle: onDestroy")
                    }
                    else -> { /* Handle other lifecycle events if needed */ }
                }
            }
            
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
    
    // Request notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            // Get FCM token now that we have permission
            getFCMToken()
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Apply the saved language preference
        val context = LocaleHelper.applyLanguage(newBase)
        super.attachBaseContext(context)
        Log.d(TAG, "MainActivity attachBaseContext with language: ${LocaleHelper.getSelectedLanguageCode(context)}")
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if language has changed and needs to be refreshed
        val currentLanguage = LocaleHelper.getSelectedLanguageCode(this)
        Log.d(TAG, "MainActivity onResume with language: $currentLanguage")
        
        // Check if auto-logout should be performed based on time in background
        lifecycleScope.launch {
            try {
                val wasLoggedOut = autoLogoutManager.checkAndPerformAutoLogoutIfNeeded()
                if (wasLoggedOut) {
                    // Restart the app to go to login screen
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finishAffinity()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auto-logout on resume: ${e.message}", e)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        
        // We don't want to log out when the app is just paused temporarily
        // This is just for logging purposes
        lifecycleScope.launch {
            try {
                val settingsResult = firebaseRepository.getUserSettings()
                val autoLogoutEnabled = settingsResult.getOrNull()?.autoLogout ?: false
                
                if (autoLogoutEnabled) {
                    Log.d(TAG, "Auto-logout is enabled, will log out when app is closed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auto-logout setting in onPause: ${e.message}")
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
        
        // Record when the app goes to background
        if (!isChangingConfigurations) {
            autoLogoutManager.recordBackgroundTime()
        }
    }
    
    override fun onDestroy() {
        try {
            // Ensure any Compose animations are cancelled
            window?.decorView?.cancelPendingInputEvents()
            
            // Clean up the window to prevent white screen
            window?.decorView?.visibility = View.GONE
            window?.setDimAmount(0f)
            
            // Check if auto-logout should be performed when the app is explicitly closed
            // Only check if the app is actually finishing and not just recreating for configuration changes
            if (isFinishing && !isChangingConfigurations) {
                lifecycleScope.launch(Dispatchers.Main.immediate) {
                    try {
                        // Check if auto logout is enabled in settings before attempting to log out
                        val prefs = getSharedPreferences("AutoLogoutPrefs", Context.MODE_PRIVATE)
                        val autoLogoutEnabled = prefs.getBoolean("auto_logout_enabled", false)
                        
                        if (autoLogoutEnabled) {
                            Log.d(TAG, "Auto logout is enabled, checking if we should log out on close")
                            autoLogoutManager.checkAndPerformAutoLogoutOnClose()
                        } else {
                            Log.d(TAG, "Auto logout is disabled, NOT logging out on app close")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during auto-logout on destroy: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during onDestroy cleanup: ${e.message}", e)
        } finally {
            super.onDestroy()
            Log.d(TAG, "onDestroy called and cleanup complete")
        }
    }

    /**
     * Checks if Google Play Services are available on the device
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        try {
            // Here we should normally use GoogleApiAvailability to check, but to avoid
            // adding another dependency, we'll use a simple check and catch approach
            FirebaseApp.getInstance().applicationContext
            return true  // If we got here without exception, services are available
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services availability", e)
            return false
        }
    }
    
    private fun askNotificationPermission() {
        try {
            // This is only necessary for API level >= 33 (TIRAMISU)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted, get FCM token
                    getFCMToken()
                } else {
                    // Request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // For API < 33, notification permissions are granted during app installation
                getFCMToken()
            }
        } catch (e: Exception) {
            // Log any errors but don't crash the app
            Log.e(TAG, "Error handling notification permissions", e)
        }
    }
    
    private fun getFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    // Don't try to proceed with token handling if there was an error
                    return@addOnCompleteListener
                }
                
                // Get new FCM registration token
                val token = task.result
                
                // Log and send to server
                Log.d(TAG, "FCM Token: $token")
                
                // Save the token in Firebase for the current user
                lifecycleScope.launch {
                    try {
                        // Get repository instance using Hilt
                        val repository = entryPoint.firebaseRepository
                        repository.saveFCMToken(token)
                        Log.d(TAG, "FCM token saved successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving FCM token", e)
                    }
                }
            }
        } catch (e: Exception) {
            // Catch any exceptions that might occur when initializing Firebase Messaging
            Log.e(TAG, "Error initializing Firebase Messaging", e)
        }
    }
    
    private fun handleNotificationIntent() {
        // Check if app was opened from a notification
        intent.extras?.let { extras ->
            if (extras.containsKey("notification_type")) {
                val notificationType = extras.getString("notification_type")
                when (notificationType) {
                    "order_status" -> {
                        val orderId = extras.getString("orderId")
                        Log.d(TAG, "App opened from order notification: $orderId")
                        // TODO: Navigate to order details screen
                    }
                    else -> {
                        Log.d(TAG, "App opened from notification: $notificationType")
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val currentRoute = currentDestination?.route ?: Screen.Splash.route
    
    // Get the CartViewModel
    val cartViewModel = hiltViewModel<CartViewModel>()
    
    val isBottomBarVisible = remember(currentRoute) {
        !currentRoute.contains(Screen.Splash.route) &&
        !currentRoute.contains(Screen.Login.route) &&
        !currentRoute.contains(Screen.Register.route) &&
        !currentRoute.contains("admin") &&
        !currentRoute.contains(Screen.FoodDetails.route) &&
        !currentRoute.contains(Screen.CategoryDetails.route) &&
        !currentRoute.contains(Screen.Checkout.route) &&
        !currentRoute.contains(Screen.OrderTracking.route)
    }
    
    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                BottomAppBar {
                    NavigationBar(
                        modifier = Modifier.shadow(8.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 16.dp
                    ) {
                        BottomNavItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_home),
                            label = stringResource(R.string.home),
                            selected = currentRoute == Screen.Home.route,
                            onClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                        BottomNavItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_restaurant),
                            label = stringResource(R.string.menu),
                            selected = currentRoute == Screen.Menu.route,
                            onClick = {
                                navController.navigate(Screen.Menu.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                        BottomNavItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_shopping_cart),
                            label = stringResource(R.string.cart),
                            selected = currentRoute == Screen.Cart.route,
                            onClick = {
                                navController.navigate(Screen.Cart.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Provide the CartViewModel to all screens
            CompositionLocalProvider(LocalCartViewModel provides cartViewModel) {
                AppNavGraph(navController = navController)
            }
        }
    }
}