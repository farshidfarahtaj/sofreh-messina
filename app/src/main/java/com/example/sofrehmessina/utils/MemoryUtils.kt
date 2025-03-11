package com.example.sofrehmessina.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Utility class for memory management to help prevent app freezing
 * and improve performance when memory pressure is detected.
 */
object MemoryUtils {
    private const val TAG = "MemoryUtils"
    private const val LOW_MEMORY_THRESHOLD_PERCENTAGE = 15 // 15% of available memory
    private const val DEFAULT_CLEANUP_THRESHOLD_MB = 50L // 50MB
    
    // Scheduler for periodic memory checks
    private val memoryCheckExecutor = Executors.newSingleThreadScheduledExecutor()
    private var isMonitoringActive = false

    /**
     * Suggests to the system that it's a good time to run garbage collection
     * Runs on a background thread to avoid blocking UI
     */
    fun releaseMemoryHint() {
        Thread {
            try {
                // Request garbage collection
                System.gc()
                // Allow finalization of objects
                System.runFinalization()
                Log.d(TAG, "Memory hint released")
            } catch (e: Exception) {
                Log.e(TAG, "Error during memory hint release: ${e.message}", e)
            }
        }.start()
    }

    /**
     * Reports current memory usage of the app and system
     * @return A string with memory usage details
     */
    fun getMemoryUsageReport(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val usedMemInMB = (memoryInfo.totalMem - memoryInfo.availMem) / (1024L * 1024L)
        val totalMemInMB = memoryInfo.totalMem / (1024L * 1024L)
        val freeMemInMB = memoryInfo.availMem / (1024L * 1024L)
        val percentUsed = 100.0 * (memoryInfo.totalMem - memoryInfo.availMem) / memoryInfo.totalMem
        
        // Get app-specific memory info
        val rt = Runtime.getRuntime()
        val appUsedMem = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)
        val appMaxMem = rt.maxMemory() / (1024L * 1024L)
        
        // Debug level memory info
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / (1024L * 1024L)
        
        val report = StringBuilder()
        report.append("System Memory: ${usedMemInMB}MB used of ${totalMemInMB}MB (${String.format("%.1f", percentUsed)}%)\n")
        report.append("Free Memory: ${freeMemInMB}MB\n")
        report.append("Low Memory: ${memoryInfo.lowMemory}\n")
        report.append("App Memory: ${appUsedMem}MB used of ${appMaxMem}MB max\n")
        report.append("Native Heap: ${nativeHeap}MB\n")
        
        Log.d(TAG, report.toString())
        return report.toString()
    }

    /**
     * Checks if the device is in a low memory condition
     * @return True if memory is low, false otherwise
     */
    fun isLowMemory(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Check official low memory flag first
        if (memoryInfo.lowMemory) {
            return true
        }
        
        // Then calculate percentage of free memory
        val percentFree = 100.0 * memoryInfo.availMem / memoryInfo.totalMem
        val isLow = percentFree < LOW_MEMORY_THRESHOLD_PERCENTAGE
        
        if (isLow) {
            Log.w(TAG, "Low memory condition detected: ${String.format("%.1f", percentFree)}% free")
        }
        
        return isLow
    }

    /**
     * Schedules a memory cleanup task if available memory is below a certain threshold
     * @param cleanupThreshold Amount of memory in MB below which cleaning should occur
     */
    fun scheduleMemoryCleanupIfNeeded(context: Context, cleanupThreshold: Long = DEFAULT_CLEANUP_THRESHOLD_MB) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemInMB = memoryInfo.availMem / (1024L * 1024L)
        
        if (availableMemInMB < cleanupThreshold || memoryInfo.lowMemory) {
            Log.w(TAG, "Low memory detected (${availableMemInMB}MB available), scheduling cleanup")
            
            // Schedule memory cleanup on a background thread
            Thread {
                try {
                    val beforeCleanup = memoryInfo.availMem / (1024L * 1024L)
                    
                    // Release memory
                    System.gc()
                    System.runFinalization()
                    
                    // Check memory after cleanup
                    activityManager.getMemoryInfo(memoryInfo)
                    val afterCleanup = memoryInfo.availMem / (1024L * 1024L)
                    val freed = afterCleanup - beforeCleanup
                    
                    Log.d(TAG, "Memory cleanup complete: ${freed}MB freed, now at ${afterCleanup}MB available")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during memory cleanup: ${e.message}", e)
                }
            }.start()
        }
    }
    
    /**
     * Starts periodic memory monitoring to proactively detect memory issues
     * @param context Application context
     * @param intervalMs How often to check memory (default: 30 seconds)
     */
    fun startMemoryMonitoring(context: Context, intervalMs: Long = 30000) {
        if (isMonitoringActive) return
        
        isMonitoringActive = true
        Log.d(TAG, "Starting periodic memory monitoring (interval: ${intervalMs}ms)")
        
        memoryCheckExecutor.scheduleAtFixedRate({
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                
                val availableMemInMB = memoryInfo.availMem / (1024L * 1024L)
                val percentFree = 100.0 * memoryInfo.availMem / memoryInfo.totalMem
                
                // Log detailed metrics only when memory is low
                if (percentFree < LOW_MEMORY_THRESHOLD_PERCENTAGE || memoryInfo.lowMemory) {
                    Log.w(TAG, "Memory monitor: LOW MEMORY DETECTED")
                    getMemoryUsageReport(context) // This will log the full report
                    
                    // Perform cleanup on main thread to avoid interfering with UI operations
                    Handler(Looper.getMainLooper()).post {
                        scheduleMemoryCleanupIfNeeded(context)
                    }
                } else {
                    // Brief log for normal operation
                    Log.d(TAG, "Memory monitor: ${availableMemInMB}MB available (${String.format("%.1f", percentFree)}% free)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in memory monitoring: ${e.message}", e)
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Stops periodic memory monitoring
     */
    fun stopMemoryMonitoring() {
        if (!isMonitoringActive) return
        
        try {
            memoryCheckExecutor.shutdown()
            isMonitoringActive = false
            Log.d(TAG, "Stopped memory monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping memory monitoring: ${e.message}", e)
        }
    }
    
    /**
     * Checks if we are experiencing memory pressure and logs the state
     * Call this when operations are slow or the app seems unresponsive
     */
    fun checkAndLogMemoryPressure(context: Context): Boolean {
        try {
            val isLow = isLowMemory(context)
            if (isLow) {
                Log.w(TAG, "⚠️ MEMORY PRESSURE DETECTED ⚠️")
                getMemoryUsageReport(context)
                scheduleMemoryCleanupIfNeeded(context)
            }
            return isLow
        } catch (e: Exception) {
            Log.e(TAG, "Error checking memory pressure: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Call this to release memory before expensive operations
     * like loading large images or complex database queries
     */
    suspend fun prepareForExpensiveOperation(context: Context) = withContext(Dispatchers.IO) {
        try {
            val before = Runtime.getRuntime().freeMemory() / (1024L * 1024L)
            System.gc()
            System.runFinalization()
            val after = Runtime.getRuntime().freeMemory() / (1024L * 1024L)
            Log.d(TAG, "Prepared for expensive operation, freed ${after - before}MB")
            
            checkAndLogMemoryPressure(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing for expensive operation: ${e.message}", e)
        }
    }
} 