package com.example.sofrehmessina.util

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

/**
 * Extension function to easily get a storage reference from a path
 */
fun FirebaseStorage.getRef(path: String): StorageReference {
    return this.reference.child(path)
} 