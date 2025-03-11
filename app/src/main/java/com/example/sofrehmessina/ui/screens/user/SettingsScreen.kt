package com.example.sofrehmessina.ui.screens.user

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.ThemeOption
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.ui.components.ErrorDialog
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.screens.debug.AdminDebugActivity
import com.example.sofrehmessina.ui.screens.user.SettingsViewModel
import androidx.compose.material3.HorizontalDivider
import com.example.sofrehmessina.util.LocaleHelper
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN
    
    // State for language dialog
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("") }
    
    // State for Terms of Service and Privacy Policy dialogs
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    
    // Get current app language
    val currentLanguage = LocaleHelper.getSelectedLanguageCode(context)
    
    // Listen for activity recreation events (for theme changes)
    LaunchedEffect(Unit) {
        viewModel.activityRecreationNeeded.collect {
            Log.d("SettingsScreen", "Fully restarting app to apply theme changes")
            (context as? Activity)?.let { activity ->
                // Show a brief toast message to indicate theme is changing
                android.widget.Toast.makeText(
                    context,
                    "Applying theme...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Forcefully restart the entire app - similar to language change restart
                try {
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    if (intent != null) {
                        // Clear all previous activities and start fresh
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        
                        context.startActivity(intent)
                        
                        // Finish current activity
                        activity.finishAffinity()
                    } else {
                        // Fallback restart
                        val restartIntent = Intent(context, context.javaClass)
                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(restartIntent)
                        activity.finishAffinity()
                    }
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Failed to restart app: ${e.message}")
                    // Last resort - just recreate the current activity
                    activity.recreate()
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }
    
    // Initialize display name from current user if needed
    LaunchedEffect(currentUser) {
        viewModel.initializeDisplayNameFromUser(currentUser)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .imeNestedScroll()
                .imePadding(),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // User info section
            Text(
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // User profile settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Display name setting
                SettingsTextItem(
                    title = stringResource(R.string.display_name),
                    description = stringResource(R.string.your_name_shown_to_others),
                    value = settings.displayName,
                    onValueChange = { viewModel.updateDisplayName(it) }
                )
                
                HorizontalDivider()
                
                // Email notification settings
                SettingsSwitchItem(
                    title = stringResource(R.string.email_notifications),
                    description = stringResource(R.string.receive_emails_about_account),
                    checked = settings.emailNotifications,
                    onCheckedChange = { viewModel.updateEmailNotifications(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance section
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Appearance settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Theme setting
                SettingsThemeItem(
                    title = stringResource(R.string.theme),
                    description = stringResource(R.string.app_appearance_theme),
                    currentTheme = settings.theme,
                    onThemeSelected = { viewModel.updateTheme(it) }
                )
                
                HorizontalDivider()
                
                // Language setting
                SettingsLanguageSelector(
                    title = stringResource(R.string.language),
                    description = stringResource(R.string.app_language),
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { language ->
                        // Only show dialog if language is changing
                        if (language != currentLanguage) {
                            selectedLanguage = language
                            showLanguageDialog = true
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Notification section
            Text(
                text = stringResource(R.string.notifications),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Notification settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Order updates
                SettingsSwitchItem(
                    title = stringResource(R.string.order_updates),
                    description = stringResource(R.string.notifications_about_orders),
                    checked = settings.orderNotifications,
                    onCheckedChange = { viewModel.updateOrderNotifications(it) }
                )
                
                HorizontalDivider()
                
                // Promotional notifications
                SettingsSwitchItem(
                    title = stringResource(R.string.promotional_offers),
                    description = stringResource(R.string.notifications_about_offers),
                    checked = settings.promotionalNotifications,
                    onCheckedChange = { viewModel.updatePromotionalNotifications(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Security section
            Text(
                text = stringResource(R.string.security),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Auto Logout
                SettingsSwitchItem(
                    title = stringResource(R.string.auto_logout),
                    description = stringResource(R.string.auto_logout_description),
                    checked = settings.autoLogout,
                    onCheckedChange = { viewModel.updateAutoLogout(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // About section
            Text(
                text = stringResource(R.string.about),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // About app
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SettingsClickableItem(
                    title = stringResource(R.string.terms_of_service),
                    description = stringResource(R.string.read_terms_of_service),
                    onClick = { showTermsDialog = true }
                )
                
                HorizontalDivider()
                
                SettingsClickableItem(
                    title = stringResource(R.string.privacy_policy),
                    description = stringResource(R.string.read_privacy_policy),
                    onClick = { showPrivacyDialog = true }
                )
                
                HorizontalDivider()
                
                SettingsClickableItem(
                    title = stringResource(R.string.about),
                    description = stringResource(R.string.version_and_information, "1.0.0"),
                    onClick = { /* Show about dialog */ }
                )
            }
            
            // Admin Debug section - only visible to admins
            if (isAdmin) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Admin Debug Tools",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Admin tools item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                context.startActivity(Intent(context, AdminDebugActivity::class.java))
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Debug Tools",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Access admin tools to fix permissions and user roles",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            // Language Restart Dialog
            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { 
                        Text(
                            text = stringResource(R.string.language_selection),
                            style = MaterialTheme.typography.headlineSmall
                        ) 
                    },
                    text = { 
                        Text(
                            text = stringResource(R.string.language_change_restart),
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                Log.d("SettingsScreen", "Language changed to $selectedLanguage, restarting app")
                                
                                // Update settings
                                viewModel.setLanguage(selectedLanguage)
                                
                                // Save new language preference
                                LocaleHelper.setSelectedLanguageCode(context, selectedLanguage)
                                
                                showLanguageDialog = false
                                
                                // Restart app without redirecting to login
                                try {
                                    val packageManager = context.packageManager
                                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                    if (intent != null) {
                                        // Clear all previous activities and start fresh
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        
                                        // Add flag to indicate language change (for faster splash screen)
                                        intent.putExtra("LANGUAGE_CHANGED", true)
                                        
                                        context.startActivity(intent)
                                        
                                        // Finish current activity without exiting the app
                                        (context as? Activity)?.finishAffinity()
                                    } else {
                                        // Fallback restart
                                        val restartIntent = Intent(context, context.javaClass)
                                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        restartIntent.putExtra("LANGUAGE_CHANGED", true)
                                        context.startActivity(restartIntent)
                                        (context as? Activity)?.finishAffinity()
                                    }
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Failed to restart app: ${e.message}")
                                    
                                    // Last resort fallback
                                    val intent = Intent(context, context.javaClass)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    intent.putExtra("LANGUAGE_CHANGED", true)
                                    context.startActivity(intent)
                                    (context as? Activity)?.finishAffinity()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.restart_now))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { 
                                // Just change the language but don't restart
                                viewModel.setLanguage(selectedLanguage)
                                LocaleHelper.setSelectedLanguageCode(context, selectedLanguage)
                                
                                // Notify the app that the language has changed
                                viewModel.notifyLanguageChanged()
                                
                                showLanguageDialog = false 
                            }
                        ) {
                            Text(stringResource(R.string.restart_later))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                )
            }
            
            // Terms of Service Dialog
            if (showTermsDialog) {
                LegalContentDialog(
                    title = stringResource(R.string.terms_title),
                    content = stringResource(R.string.terms_content),
                    lastUpdated = stringResource(R.string.legal_last_updated),
                    onDismiss = { showTermsDialog = false }
                )
            }
            
            // Privacy Policy Dialog
            if (showPrivacyDialog) {
                LegalContentDialog(
                    title = stringResource(R.string.privacy_title),
                    content = stringResource(R.string.privacy_content),
                    lastUpdated = stringResource(R.string.legal_last_updated),
                    onDismiss = { showPrivacyDialog = false }
                )
            }
        }
    }
}

@Composable
fun SettingsThemeItem(
    title: String,
    description: String,
    currentTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showThemeDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when (currentTheme) {
                    ThemeOption.LIGHT -> stringResource(R.string.theme_light)
                    ThemeOption.DARK -> stringResource(R.string.theme_dark)
                    ThemeOption.SYSTEM -> stringResource(R.string.theme_system)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_selection)) },
            text = {
                Column {
                    // Light theme option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(ThemeOption.LIGHT)
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == ThemeOption.LIGHT,
                            onClick = {
                                onThemeSelected(ThemeOption.LIGHT)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_light))
                        
                        if (currentTheme == ThemeOption.LIGHT) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Dark theme option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(ThemeOption.DARK)
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == ThemeOption.DARK,
                            onClick = {
                                onThemeSelected(ThemeOption.DARK)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_dark))
                        
                        if (currentTheme == ThemeOption.DARK) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // System theme option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(ThemeOption.SYSTEM)
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == ThemeOption.SYSTEM,
                            onClick = {
                                onThemeSelected(ThemeOption.SYSTEM)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_system))
                        
                        if (currentTheme == ThemeOption.SYSTEM) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsLanguageSelector(
    title: String,
    description: String,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val actualCurrentLanguage = currentLanguage

    // Available languages
    val languages = mapOf(
        "en" to stringResource(R.string.english),
        "fa" to stringResource(R.string.persian),
        "it" to stringResource(R.string.italian)
    )

    // For dropdown menu positioning
    val density = LocalDensity.current
    val itemHeight = 48.dp
    val totalHeight = itemHeight * languages.size
    val offsetY = with(density) { -totalHeight / 2 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(16.dp)
    ) {
        Column {
            // Title and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = languages[currentLanguage] ?: currentLanguage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, offsetY)
            ) {
                languages.forEach { (code, name) ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            Log.d("SettingsLanguageSelector", "Language selected: $code (current: $actualCurrentLanguage)")
                            
                            // Only process if language is actually changing
                            if (actualCurrentLanguage != code) {
                                Log.d("SettingsLanguageSelector", "Language changed, triggering dialog")
                                onLanguageSelected(code)
                            }
                        },
                        text = { Text(text = name) },
                        trailingIcon = {
                            if (code == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTextItem(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            singleLine = true
        )
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
@Suppress("unused")
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
@Suppress("unused")
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke() ?: run {
                if (onClick != null) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LegalContentDialog(
    title: String,
    content: String,
    lastUpdated: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = lastUpdated,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentAlignment = Alignment.Center
            ) {
                val scrollState = rememberScrollState()
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.legal_close))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
} 