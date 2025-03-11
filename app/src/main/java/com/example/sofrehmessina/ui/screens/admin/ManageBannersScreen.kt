package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sofrehmessina.data.model.BannerItem
import com.example.sofrehmessina.ui.components.ErrorDialog
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.viewmodel.BannerViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import android.net.Uri
import com.example.sofrehmessina.ui.components.ImagePicker
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBannersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: BannerViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val banners by viewModel.banners.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        val successMessage by viewModel.operationSuccess.collectAsState()
        
        var showAddBannerDialog by remember { mutableStateOf(false) }
        var showEditBannerDialog by remember { mutableStateOf(false) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var currentBanner by remember { mutableStateOf<BannerItem?>(null) }
        
        // Show success message as a Snackbar
        val snackbarHostState = remember { SnackbarHostState() }
        
        LaunchedEffect(successMessage) {
            successMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSuccessMessage()
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Manage Banners") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddBannerDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Banner")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddBannerDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Banner")
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                } else if (error != null) {
                    ErrorDialog(
                        error = error.toString(),
                        onDismiss = { viewModel.clearError() }
                    )
                } else {
                    if (banners.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No Banners Added Yet",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Add banners to display in the slideshow on the home screen",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(onClick = { showAddBannerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add First Banner")
                            }
                        }
                    } else {
                        // List of banners
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(banners) { banner ->
                                BannerCard(
                                    banner = banner,
                                    onEdit = {
                                        currentBanner = banner
                                        showEditBannerDialog = true
                                    },
                                    onDelete = {
                                        currentBanner = banner
                                        showDeleteConfirmation = true
                                    },
                                    onToggleActive = { isActive ->
                                        viewModel.toggleBannerActive(banner.id, isActive)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Add banner dialog
        if (showAddBannerDialog) {
            BannerFormDialog(
                title = "Add Banner",
                imageUrl = "",
                bannerTitle = "",
                subtitle = "",
                actionUrl = "",
                onDismiss = { showAddBannerDialog = false },
                onSave = { _, title, subtitle, actionUrl, selectedImageUri ->
                    viewModel.addBannerWithImage(title, subtitle, actionUrl, selectedImageUri)
                    showAddBannerDialog = false
                },
                viewModel = viewModel
            )
        }
        
        // Edit banner dialog
        if (showEditBannerDialog && currentBanner != null) {
            BannerFormDialog(
                title = "Edit Banner",
                imageUrl = currentBanner!!.imageUrl,
                bannerTitle = currentBanner!!.title,
                subtitle = currentBanner!!.subtitle,
                actionUrl = currentBanner!!.actionUrl,
                onDismiss = { showEditBannerDialog = false },
                onSave = { imageUrl, title, subtitle, actionUrl, selectedImageUri ->
                    val updatedBanner = currentBanner!!.copy(
                        imageUrl = imageUrl,
                        title = title,
                        subtitle = subtitle,
                        actionUrl = actionUrl
                    )
                    viewModel.updateBannerWithImage(updatedBanner, selectedImageUri)
                    showEditBannerDialog = false
                },
                viewModel = viewModel
            )
        }
        
        // Delete confirmation dialog
        if (showDeleteConfirmation && currentBanner != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Banner") },
                text = { Text("Are you sure you want to delete this banner? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteBanner(currentBanner!!.id)
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BannerFormDialog(
    title: String,
    imageUrl: String,
    bannerTitle: String,
    subtitle: String,
    actionUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Uri?) -> Unit,
    viewModel: BannerViewModel = hiltViewModel()
) {
    var imageUrlState by remember { mutableStateOf(imageUrl) }
    var titleState by remember { mutableStateOf(bannerTitle) }
    var subtitleState by remember { mutableStateOf(subtitle) }
    var actionUrlState by remember { mutableStateOf(actionUrl) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // View model states
    val isImageUploading by viewModel.isImageUploading.collectAsState()
    val imageUploadError by viewModel.imageUploadError.collectAsState()
    
    // Check if form is valid
    val isFormValid = titleState.isNotEmpty()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image picker
                ImagePicker(
                    currentImageUrl = imageUrlState,
                    onImageSelected = { uri -> selectedImageUri = uri },
                    label = "Banner Image",
                    isUploading = isImageUploading,
                    error = imageUploadError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Title field
                OutlinedTextField(
                    value = titleState,
                    onValueChange = { titleState = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Banner Title") },
                    singleLine = true,
                    isError = titleState.isEmpty()
                )
                
                // Subtitle field
                OutlinedTextField(
                    value = subtitleState,
                    onValueChange = { subtitleState = it },
                    label = { Text("Subtitle (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Banner Subtitle") },
                    singleLine = true
                )
                
                // Action URL field
                OutlinedTextField(
                    value = actionUrlState,
                    onValueChange = { actionUrlState = it },
                    label = { Text("Action URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("URL or deep link when banner is clicked") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(imageUrlState, titleState, subtitleState, actionUrlState, selectedImageUri) 
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

@Composable
fun BannerCard(
    banner: BannerItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleActive: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box {
                // Image with fallback
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = banner.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // We'll handle the error state with our own composable instead of the error parameter
                    if (banner.imageUrl.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    // Status indicator (active/inactive)
                    if (!banner.active) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "INACTIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Banner",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Banner",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (onToggleActive != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                // Toggle the opposite of current state
                                onToggleActive(!banner.active)
                            }
                        ) {
                            Text(
                                text = if (banner.active) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (banner.active) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            
                            Switch(
                                checked = banner.active,
                                onCheckedChange = { onToggleActive(it) },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(width = 40.dp, height = 24.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                        }
                    }
                }
                
                if (banner.subtitle.isNotEmpty()) {
                    Text(
                        text = banner.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (banner.actionUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = banner.actionUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Display order number
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Order: ${banner.order}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Image URL display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = banner.imageUrl,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 