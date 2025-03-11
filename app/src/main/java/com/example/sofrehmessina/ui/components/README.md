# Keyboard Handling in Sofreh Messina

This document describes the keyboard handling approach used in the Sofreh Messina app.

## KeyboardAwareLayout

The `KeyboardAwareLayout` is a composable component that centralizes the keyboard handling behavior across the app. It ensures that input fields remain visible when the keyboard appears and provides consistent behavior for all screens with input fields.

### Features

- Automatically scrolls to show the focused input field
- Properly adjusts content when the keyboard appears
- Maintains consistent padding and layout behavior
- Works in conjunction with the `android:windowSoftInputMode="adjustResize"` setting in the AndroidManifest.xml

### How to Use

#### Basic Setup

Simply wrap your content in the `KeyboardAwareLayout` composable:

```kotlin
KeyboardAwareLayout(
    modifier = Modifier.padding(paddingValues),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    // Your input fields and other content here
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Input") }
    )
    
    // More composables...
}
```

#### Enhanced Auto-Scrolling

For better auto-scrolling to focused input fields, use the `autoScrollOnFocus` modifier extension:

```kotlin
// Create a ScrollState to share between KeyboardAwareLayout and input fields
val scrollState = rememberScrollState()

KeyboardAwareLayout(
    modifier = Modifier.padding(paddingValues),
    scrollState = scrollState // Pass the shared ScrollState
) {
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Input") },
        modifier = Modifier
            .fillMaxWidth()
            .autoScrollOnFocus(scrollState) // Apply auto-scroll behavior
    )
    
    // More input fields...
}
```

This approach ensures that when an input field is focused:
1. The screen automatically scrolls to position the field in view
2. The keyboard appears below the focused field
3. The user can see what they're typing without the keyboard obscuring the field

### Implementation Details

The KeyboardAwareLayout combines several Compose modifiers that work together to handle keyboard behavior:

1. `verticalScroll(scrollState)` - Makes the content scrollable
2. `imeNestedScroll()` - Enables nested scroll handling with the IME (Input Method Editor)
3. `imePadding()` - Adds appropriate padding to avoid content being covered by the keyboard
4. `autoScrollOnFocus(scrollState)` - Custom modifier that automatically scrolls to the focused field

### Screens Using KeyboardAwareLayout

- LoginScreen
- RegisterScreen
- UserProfileScreen
- ProfileScreen
- CheckoutScreen

## Troubleshooting

If you encounter keyboard handling issues:

1. Make sure your screen uses the `KeyboardAwareLayout`
2. Verify that your Activity has `android:windowSoftInputMode="adjustResize"` in the AndroidManifest.xml
3. Apply the `autoScrollOnFocus` modifier to your input fields for improved scrolling behavior
4. Ensure you're using a shared `ScrollState` between KeyboardAwareLayout and input fields 