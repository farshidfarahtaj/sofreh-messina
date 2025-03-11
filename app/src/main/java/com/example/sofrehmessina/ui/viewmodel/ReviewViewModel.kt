package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Review
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {
    private val _reviews = MutableStateFlow<List<ReviewWithUserName>>(emptyList())
    val reviews: StateFlow<List<ReviewWithUserName>> = _reviews

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    init {
        viewModelScope.launch {
            _currentUser.value = null
        }
    }

    fun loadReviews(foodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getFoodReviews(foodId)
                    .onSuccess { reviews ->
                        val reviewsWithUserNames = reviews.map { review ->
                            val userName = "Unknown User"
                            ReviewWithUserName(review, userName)
                        }
                        _reviews.value = reviewsWithUserNames
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addReview(foodId: String, rating: Float, comment: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = _currentUser.value ?: return@launch
                
                val review = Review(
                    id = "",
                    userId = currentUser.id,
                    foodId = foodId,
                    rating = rating,
                    comment = comment,
                    createdAt = Timestamp.now()
                )
                
                repository.createReview(review)
                    .onSuccess { newReview ->
                        // Reload reviews to get the updated list with user names
                        _reviews.value = _reviews.value + ReviewWithUserName(newReview, currentUser.name)
                        loadReviews(foodId)
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _reviews.value = _reviews.value.filter { it.review.id != reviewId }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class ReviewWithUserName(
    val review: Review,
    val userName: String
) 