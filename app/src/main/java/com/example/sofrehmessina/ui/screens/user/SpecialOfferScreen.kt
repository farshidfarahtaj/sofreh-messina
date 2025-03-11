package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sofrehmessina.ui.components.SpecialOfferDisplay

/**
 * Screen to display special offers to users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialOfferScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Special Offers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Display the special offer with the exact title and values from the user's request
                SpecialOfferDisplay(
                    title = "Special Item Offer: 5 for 35 percent",
                    quantity = 5,
                    percentOff = 35,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Add a button to navigate to the items
                Button(
                    onClick = { /* Navigate to the items with this offer */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Shop Now")
                }
            }
        }
    }
} 