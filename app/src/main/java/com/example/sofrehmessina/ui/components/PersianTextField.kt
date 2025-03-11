package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import com.example.sofrehmessina.ui.theme.PersianTypography
import com.example.sofrehmessina.util.InputHelper
import com.example.sofrehmessina.util.LocaleHelper

/**
 * A custom TextField that properly handles Persian text input
 */
@Composable
fun PersianTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    isError: Boolean = false,
    textStyle: TextStyle = PersianTypography.bodyLarge,
    outlined: Boolean = false
) {
    val context = LocalContext.current
    var hasShownKeyboardSuggestion by remember { mutableStateOf(false) }
    
    // Check Persian keyboard once
    LaunchedEffect(Unit) {
        if (!hasShownKeyboardSuggestion && !InputHelper.isPersianKeyboardAvailable(context)) {
            InputHelper.showPersianKeyboardSuggestion(context)
            hasShownKeyboardSuggestion = true
        }
    }
    
    // Custom text style with forced LTR direction
    val persianTextStyle = textStyle.copy(textDirection = TextDirection.Ltr)
    
    if (outlined) {
        OutlinedTextField(
            value = value,
            onValueChange = { 
                // Clean the text from any unwanted direction marks
                val cleaned = it.replace("\u200E", "").replace("\u200F", "")
                onValueChange(cleaned) 
            },
            modifier = modifier.fillMaxWidth(),
            label = label,
            placeholder = placeholder,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            isError = isError,
            textStyle = persianTextStyle
        )
    } else {
        TextField(
            value = value,
            onValueChange = { 
                // Clean the text from any unwanted direction marks
                val cleaned = it.replace("\u200E", "").replace("\u200F", "")
                onValueChange(cleaned) 
            },
            modifier = modifier.fillMaxWidth(),
            label = label,
            placeholder = placeholder,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            isError = isError,
            textStyle = persianTextStyle
        )
    }
}

/**
 * Creates a label for a TextField with Persian text
 */
@Composable
fun PersianTextFieldLabel(text: String) {
    Text(
        text = text,
        style = PersianTypography.bodyMedium.copy(textDirection = TextDirection.Ltr)
    )
}

/**
 * Creates a placeholder for a TextField with Persian text
 */
@Composable
fun PersianTextFieldPlaceholder(text: String) {
    Text(
        text = text,
        style = PersianTypography.bodyMedium.copy(textDirection = TextDirection.Ltr)
    )
} 