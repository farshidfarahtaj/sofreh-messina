package com.example.sofrehmessina.di

import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface FirebaseRepositoryEntryPoint {
    val firebaseRepository: FirebaseRepository
} 