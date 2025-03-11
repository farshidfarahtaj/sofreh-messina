package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.ui.viewmodel.ReviewViewModel
import com.example.sofrehmessina.ui.viewmodel.ReviewWithUserName
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun ReviewsScreen(
    foodId: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val reviews by viewModel.reviews.collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val averageRating = remember(reviews) {
        if (reviews.isEmpty()) 0f
        else reviews.sumOf { it.review.rating.toDouble() }.toFloat() / reviews.size
    }

    LaunchedEffect(foodId) {
        viewModel.loadReviews(foodId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddReviewDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Review")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Rating Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", averageRating),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < averageRating) 
                                    Icons.Default.Star 
                                else 
                                    Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = "${reviews.size} ${if (reviews.size == 1) "review" else "reviews"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reviews yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reviews) { reviewWithUserName ->
                        ReviewItem(
                            reviewWithUserName = reviewWithUserName,
                            currentUserId = currentUser?.id,
                            onDeleteClick = { showDeleteDialog = reviewWithUserName.review.id }
                        )
                    }
                }
            }
        }
    }

    if (showAddReviewDialog) {
        AddReviewDialog(
            onDismiss = { showAddReviewDialog = false },
            onSubmit = { rating, comment ->
                viewModel.addReview(foodId, rating, comment)
                showAddReviewDialog = false
            }
        )
    }

    showDeleteDialog?.let { reviewId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete this review?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReview(reviewId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReviewItem(
    reviewWithUserName: ReviewWithUserName,
    currentUserId: String?,
    onDeleteClick: () -> Unit
) {
    val review = reviewWithUserName.review
    val canDelete = currentUserId == review.userId
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reviewWithUserName.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(review.createdAt.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canDelete) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Review",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < review.rating) 
                            Icons.Default.Star 
                        else 
                            Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (review.comment.isNotEmpty()) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AddReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (rating: Float, comment: String) -> Unit
) {
    var rating by remember { mutableStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Review") },
        text = {
            Column {
                Text(
                    text = "Rating",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { rating = (index + 1).toFloat() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (index < rating) 
                                    Icons.Default.Star 
                                else 
                                    Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 