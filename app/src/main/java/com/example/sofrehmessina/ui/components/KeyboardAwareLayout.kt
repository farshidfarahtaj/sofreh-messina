package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A composable that handles keyboard behavior consistently across the app.
 * This layout ensures that:
 * 1. The content is scrollable when the keyboard appears
 * 2. The focused input field remains visible above the keyboard
 * 3. The content is properly padded to avoid being covered by the keyboard
 * 4. Automatically scrolls to the focused input field
 * 5. Maintains stable layout during typing to prevent content from "jumping"
 *
 * @param modifier The modifier to be applied to the layout
 * @param horizontalAlignment How to align the content horizontally
 * @param verticalArrangement How to arrange the content vertically
 * @param scrollState Optional ScrollState to control scrolling. If not provided, a new one will be created.
 * @param contentAlignment The alignment of the content within the layout
 * @param content The content to be displayed inside the keyboard-aware layout
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardAwareLayout(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    scrollState: ScrollState = rememberScrollState(),
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scrollState)
                // With adjustPan mode, we don't need imeNestedScroll
                .padding(horizontal = 8.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * A keyboard-aware layout specifically for scrollable dialog content.
 * Prevents the content from shifting during text input.
 *
 * @param modifier The modifier to be applied to the layout
 * @param scrollState The ScrollState to control scrolling
 * @param content The content to be displayed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardAwareDialogContent(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/**
 * Extension function to add automatic scrolling behavior to any Modifier.
 * Apply this to input fields to make them scroll into view when focused.
 * 
 * Modified to work with adjustPan mode with more aggressive positioning.
 */
@Composable
fun Modifier.autoScrollOnFocus(scrollState: ScrollState): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var yPosition by remember { mutableFloatStateOf(0f) }
    
    return this
        .onGloballyPositioned { layoutCoordinates ->
            yPosition = layoutCoordinates.positionInWindow().y
        }
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch {
                    // More aggressive scrolling - move field to near the top of the screen
                    // This ensures it's well above the keyboard regardless of screen size
                    scrollState.animateScrollTo((scrollState.value + yPosition.toInt() - 150).coerceAtLeast(0))
                    
                    // Add a brief delay then scroll again to ensure proper positioning
                    kotlinx.coroutines.delay(100)
                    scrollState.animateScrollTo((scrollState.value + yPosition.toInt() - 150).coerceAtLeast(0))
                }
            }
        }
} 