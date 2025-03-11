package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ErrorDialog(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFirestoreIndexError = error.contains("FAILED_PRECONDITION") && error.contains("requires an index")
    
    if (isFirestoreIndexError) {
        FirestoreIndexErrorDialog(error = error, onDismiss = onDismiss)
    } else {
        StandardErrorDialog(error = error, onDismiss = onDismiss, modifier = modifier)
    }
}

@Composable
fun StandardErrorDialog(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = simplifyErrorMessage(error),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun FirestoreIndexErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val indexUrl = extractFirestoreIndexUrl(error)
    
    LaunchedEffect(key1 = indexUrl) {
        // Log URL for developer
        println("Firestore index URL: $indexUrl")
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .widthIn(max = 300.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Database Setup Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This feature requires additional setup. Please contact the app developer.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (indexUrl != null) {
                    Button(
                        onClick = {
                            uriHandler.openUri(indexUrl)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Create Index")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

private fun simplifyErrorMessage(errorMessage: String): String {
    return when {
        errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("requires an index") ->
            "This feature requires database setup. Please try again later or contact support."
        errorMessage.contains("PERMISSION_DENIED") ->
            "You don't have permission to access this feature."
        errorMessage.contains("RESOURCE_EXHAUSTED") ->
            "Service temporarily unavailable. Please try again later."
        errorMessage.contains("UNAVAILABLE") ->
            "Service currently unavailable. Please check your internet connection."
        errorMessage.contains("UNAUTHENTICATED") ->
            "Authentication required. Please sign in again."
        else -> errorMessage
    }
}

private fun extractFirestoreIndexUrl(errorMessage: String): String? {
    return errorMessage.let {
        val startIndex = it.indexOf("https://")
        val endIndex = it.indexOf("\n", startIndex).takeIf { it != -1 } ?: it.length
        if (startIndex != -1) it.substring(startIndex, endIndex) else null
    }
} 