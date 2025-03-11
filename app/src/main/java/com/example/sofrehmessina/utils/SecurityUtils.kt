package com.example.sofrehmessina.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.MessageDigest

/**
 * Utility class for handling app security operations including
 * encryption, decryption, secure hashing, and token validation.
 */
object SecurityUtils {
    // Encryption constants
    private const val KEY_ALIAS = "SofrehMessinaEncryptionKey"
    private const val KEY_STORE_TYPE = "AndroidKeyStore"
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
    private const val GCM_TAG_LENGTH = 128
    
    // Cache encryption key for performance
    private var encryptionKey: SecretKey? = null
    
    /**
     * Gets or creates the encryption key using Android KeyStore
     */
    @Synchronized
    private fun getOrCreateEncryptionKey(): SecretKey {
        try {
            // Return cached key if already initialized
            encryptionKey?.let { return it }
            
            val keyStore = KeyStore.getInstance(KEY_STORE_TYPE)
            keyStore.load(null)
            
            // Check if key already exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                encryptionKey = entry.secretKey
                return entry.secretKey
            }
            
            // Otherwise create a new key
            val keyGenerator = KeyGenerator.getInstance(
                ENCRYPTION_ALGORITHM,
                KEY_STORE_TYPE
            )
            
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false) // Change to true for higher security on supported devices
            .setKeySize(256) // Use 256-bit key for stronger encryption
            .build()
            
            keyGenerator.init(keyGenSpec)
            encryptionKey = keyGenerator.generateKey()
            return encryptionKey!!
            
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Error in getOrCreateEncryptionKey: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Encrypts sensitive data
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext
        
        try {
            val key = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // Concatenate IV and encrypted bytes
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            return Base64.encodeToString(combined, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Encryption error: ${e.message}", e)
            // On encryption failure, return the original text but log the error
            return plaintext
        }
    }
    
    /**
     * Decrypts sensitive data
     * @param ciphertext Base64-encoded encrypted data
     * @return Decrypted plain text
     */
    fun decrypt(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ciphertext
        
        try {
            // Quick check to see if the input is likely encrypted
            // (avoid trying to decrypt plaintext that was never encrypted)
            if (!looksEncrypted(ciphertext)) {
                return ciphertext
            }
            
            val key = getOrCreateEncryptionKey()
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            
            // Extract IV (first 12 bytes for GCM)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(encrypted)
            
            return String(decryptedBytes, Charsets.UTF_8)
            
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Decryption error: ${e.message}", e)
            // On decryption failure, return the original text but log the error
            return ciphertext
        }
    }
    
    /**
     * Check if the string appears to be an encrypted value
     * @param text The text to check
     * @return true if the text appears to be encrypted
     */
    private fun looksEncrypted(text: String): Boolean {
        // Base64 strings typically have a specific character set and length pattern
        val base64Pattern = Regex("^[A-Za-z0-9+/=]+$")
        return text.length >= 24 && base64Pattern.matches(text)
    }
    
    /**
     * Generates a secure hash of the input using SHA-256
     * Useful for creating secure references without storing actual values
     */
    fun secureHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Generates a cryptographically secure random token
     * @param length Length of the token
     * @return Random token string
     */
    fun generateSecureToken(length: Int = 32): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
            .substring(0, length)
    }
    
    /**
     * Securely overwrites a string in memory
     * Helps prevent memory-based data leaks
     */
    fun secureOverwrite(stringBuilder: StringBuilder) {
        val secureRandom = SecureRandom()
        val randomChars = CharArray(stringBuilder.length)
        
        // Fill with random characters
        for (i in randomChars.indices) {
            randomChars[i] = (secureRandom.nextInt(26) + 'a'.code).toChar()
        }
        
        // Overwrite the original content multiple times (3 passes)
        for (pass in 0 until 3) {
            for (i in 0 until stringBuilder.length) {
                stringBuilder.setCharAt(i, randomChars[i])
            }
        }
        
        // Clear to zero length
        stringBuilder.setLength(0)
    }
    
    /**
     * Securely validates if a token has the correct format and characteristics
     * @param token The token to validate
     * @return True if token appears valid
     */
    fun isValidAuthToken(token: String): Boolean {
        // Basic validation for Firebase JWT token format
        val jwtPattern = Regex("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]+$")
        return token.isNotEmpty() && jwtPattern.matches(token)
    }
} 