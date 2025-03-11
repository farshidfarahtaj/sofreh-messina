package com.example.sofrehmessina.util

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Adds keyboard-aware modifiers to ensure content remains visible when the keyboard is shown.
 * Use this for any screen with input fields.
 * 
 * @param enableScroll Whether to enable vertical scrolling (usually true for forms)
 * @return Modifier with keyboard handling capabilities
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.keyboardAware(enableScroll: Boolean = true): Modifier {
    return if (enableScroll) {
        this
            .verticalScroll(rememberScrollState())
            .imeNestedScroll()
            .imePadding()
    } else {
        this
            .imeNestedScroll()
            .imePadding()
    }
}

/**
 * Adds keyboard-aware modifiers to an existing scrollable content.
 * Use this when you already have scrolling set up.
 * 
 * @return Modifier with keyboard handling for scrollable content
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.keyboardAwareScrollable(): Modifier {
    return this
        .imeNestedScroll()
        .imePadding()
} 