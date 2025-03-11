package com.example.sofrehmessina.util

/**
 * A generic class that holds a value with its loading status.
 * @param <T> Type of the resource data
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Exception? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    
    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(message: String, exception: Exception? = null): Resource<Nothing> = Error(message, exception)
        fun loading(): Resource<Nothing> = Loading
    }
} 