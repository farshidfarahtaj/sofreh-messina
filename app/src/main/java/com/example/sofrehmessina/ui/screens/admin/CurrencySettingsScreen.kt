package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.viewmodel.CurrencySettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    navController: NavController,
    viewModel: CurrencySettingsViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val isLoading by viewModel.isLoading.collectAsState()
        val selectedCurrency by viewModel.selectedCurrency.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val successMessage by viewModel.successMessage.collectAsState()
        val isAdmin by viewModel.isAdmin.collectAsState()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(errorMessage, successMessage) {
            errorMessage?.let {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = it,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearMessages()
                }
            }
            
            successMessage?.let {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = it,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearMessages()
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Currency Settings") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Current App Currency",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = viewModel.currencyNames[selectedCurrency] ?: selectedCurrency,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            if (isAdmin) {
                                Text(
                                    text = "Select Currency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "This will change the currency for all users of the app.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Currency selection list
                                viewModel.availableCurrencies.forEach { currency ->
                                    val isSelected = currency == selectedCurrency
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .selectable(
                                                selected = isSelected,
                                                onClick = { 
                                                    if (!isSelected) {
                                                        viewModel.updateCurrency(currency)
                                                    }
                                                }
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = viewModel.currencyNames[currency] ?: currency,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                
                                                // Show sample of formatted price
                                                CurrencySample(currency)
                                            }
                                            
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = { viewModel.refreshCurrency() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Refresh Currency Settings")
                                }
                            } else {
                                // Not an admin
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = "You need admin permissions to change the currency",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencySample(currencyCode: String) {
    val sampleAmount = 1234.56
    
    // Sample formatting based on currency code
    val formattedPrice = when(currencyCode) {
        "EUR" -> "${sampleAmount.toString().replace(".", ",")} €"
        "USD" -> "$ ${sampleAmount}"
        "IRR" -> "${sampleAmount.toInt()} ﷼"
        else -> "${sampleAmount.toString().replace(".", ",")} €"
    }
    
    Text(
        text = "Example: $formattedPrice",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp
    )
} 