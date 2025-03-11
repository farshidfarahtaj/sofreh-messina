package com.example.sofrehmessina.data.repository

import android.util.Log
import com.example.sofrehmessina.data.model.BannerItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "BannerRepository"
    private val BANNERS_COLLECTION = "banners"
    
    /**
     * Fetches all banners from Firestore
     * @return A flow of banner items
     */
    fun getBanners(): Flow<List<BannerItem>> = flow {
        try {
            val snapshot = firestore.collection(BANNERS_COLLECTION)
                .whereEqualTo("active", true)
                .orderBy("order")
                .get()
                .await()
                
            val banners = snapshot.documents.mapNotNull { doc ->
                try {
                    BannerItem(
                        id = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        title = doc.getString("title") ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        actionUrl = doc.getString("actionUrl") ?: "",
                        active = doc.getBoolean("active") ?: true,
                        order = doc.getLong("order")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing banner document: ${doc.id}", e)
                    null
                }
            }
            emit(banners)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting banners", e)
            // Emit default banners in case of failure
            emit(getDefaultBanners())
        }
    }
    
    /**
     * Fetches all banners including inactive ones (for admin management)
     */
    fun getAllBanners(): Flow<List<BannerItem>> = flow {
        try {
            val snapshot = firestore.collection(BANNERS_COLLECTION)
                .orderBy("order")
                .get()
                .await()
                
            val banners = snapshot.documents.mapNotNull { doc ->
                try {
                    BannerItem(
                        id = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        title = doc.getString("title") ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        actionUrl = doc.getString("actionUrl") ?: "",
                        active = doc.getBoolean("active") ?: true,
                        order = doc.getLong("order")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing banner document: ${doc.id}", e)
                    null
                }
            }
            emit(banners)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all banners", e)
            emit(emptyList())
        }
    }
    
    /**
     * Provides default banners in case Firestore fetch fails
     */
    private fun getDefaultBanners(): List<BannerItem> {
        return listOf(
            BannerItem(
                id = "default1",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/sofrehmessina.appspot.com/o/banners%2Fdefault_banner1.jpg?alt=media",
                title = "Welcome to SofrehMessina",
                subtitle = "Authentic Persian Cuisine",
                actionUrl = "",
                active = true,
                order = 0
            ),
            BannerItem(
                id = "default2",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/sofrehmessina.appspot.com/o/banners%2Fdefault_banner2.jpg?alt=media",
                title = "Order Online",
                subtitle = "Fast & convenient delivery",
                actionUrl = "",
                active = true,
                order = 0
            )
        )
    }
    
    /**
     * Create a new banner in Firestore (for admin use)
     */
    suspend fun createBanner(banner: BannerItem): Result<String> = try {
        // Find the highest current order
        val highestOrder = try {
            val snapshot = firestore.collection(BANNERS_COLLECTION)
                .orderBy("order", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                
            if (snapshot.isEmpty) 0 else snapshot.documents[0].getLong("order")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error finding highest order", e)
            0
        }
        
        val bannerMap = mapOf(
            "imageUrl" to banner.imageUrl,
            "title" to banner.title,
            "subtitle" to banner.subtitle,
            "actionUrl" to banner.actionUrl,
            "active" to true,
            "order" to (highestOrder + 1)
        )
        
        val docRef = firestore.collection(BANNERS_COLLECTION)
            .add(bannerMap)
            .await()
            
        Result.success(docRef.id)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating banner", e)
        Result.failure(e)
    }
    
    /**
     * Update an existing banner in Firestore (for admin use)
     */
    suspend fun updateBanner(banner: BannerItem): Result<Unit> = try {
        val updates = mapOf(
            "imageUrl" to banner.imageUrl,
            "title" to banner.title,
            "subtitle" to banner.subtitle,
            "actionUrl" to banner.actionUrl
        )
        
        firestore.collection(BANNERS_COLLECTION)
            .document(banner.id)
            .update(updates)
            .await()
            
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating banner: ${banner.id}", e)
        Result.failure(e)
    }
    
    /**
     * Delete a banner from Firestore (for admin use)
     */
    suspend fun deleteBanner(bannerId: String): Result<Unit> = try {
        firestore.collection(BANNERS_COLLECTION)
            .document(bannerId)
            .delete()
            .await()
            
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting banner: $bannerId", e)
        Result.failure(e)
    }
    
    /**
     * Toggle a banner's active status (for admin use)
     */
    suspend fun toggleBannerActive(bannerId: String, active: Boolean): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting to toggle banner $bannerId to active=$active")
            
            // First check if the banner exists
            val bannerDoc = firestore.collection(BANNERS_COLLECTION)
                .document(bannerId)
                .get()
                .await()
                
            if (!bannerDoc.exists()) {
                Log.e(TAG, "Banner not found: $bannerId")
                return Result.failure(Exception("Banner not found"))
            }
            
            // Then update the active status
            firestore.collection(BANNERS_COLLECTION)
                .document(bannerId)
                .update("active", active)
                .await()
                
            Log.d(TAG, "Successfully updated banner $bannerId active status to $active")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling banner active status: $bannerId to $active", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reorder a banner by changing its order value (for admin use)
     */
    suspend fun reorderBanner(bannerId: String, newOrder: Int): Result<Unit> = try {
        firestore.collection(BANNERS_COLLECTION)
            .document(bannerId)
            .update("order", newOrder)
            .await()
            
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error reordering banner: $bannerId", e)
        Result.failure(e)
    }
} 