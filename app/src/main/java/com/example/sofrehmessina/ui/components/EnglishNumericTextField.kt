package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.util.Locale

/**
 * A custom TextField that ensures numeric input is always displayed in English format
 * regardless of the app's selected language.
 */
@Composable
fun EnglishNumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Number,
    isOutlined: Boolean = false
) {
    val textField = @Composable {
        if (isOutlined) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    // Only allow numeric input (and decimal point for some keyboard types)
                    val filteredValue = when (keyboardType) {
                        KeyboardType.Number -> newValue.filter { it.isDigit() }
                        KeyboardType.Decimal -> newValue.filter { it.isDigit() || it == '.' }
                        else -> newValue
                    }
                    onValueChange(filteredValue)
                },
                modifier = modifier,
                label = label,
                placeholder = placeholder,
                isError = isError,
                supportingText = supportingText,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                readOnly = readOnly,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = EnglishNumericTransformation()
            )
        } else {
            TextField(
                value = value,
                onValueChange = { newValue ->
                    // Only allow numeric input (and decimal point for some keyboard types)
                    val filteredValue = when (keyboardType) {
                        KeyboardType.Number -> newValue.filter { it.isDigit() }
                        KeyboardType.Decimal -> newValue.filter { it.isDigit() || it == '.' }
                        else -> newValue
                    }
                    onValueChange(filteredValue)
                },
                modifier = modifier,
                label = label,
                placeholder = placeholder,
                isError = isError,
                supportingText = supportingText,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                readOnly = readOnly,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = EnglishNumericTransformation()
            )
        }
    }
    
    textField()
}

/**
 * Visual transformation that ensures numeric input is displayed in English format.
 */
class EnglishNumericTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // If the text is empty, return as is
        if (text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        // Convert any non-English digits to English digits
        val englishDigits = text.text.map { char ->
            when {
                char.isDigit() -> {
                    // Convert any non-English digit to its English equivalent
                    val digitValue = Character.getNumericValue(char)
                    if (digitValue in 0..9) {
                        '0' + digitValue
                    } else {
                        char
                    }
                }
                else -> char
            }
        }.joinToString("")
        
        val annotatedString = AnnotatedString(englishDigits)
        
        return TransformedText(annotatedString, object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        })
    }
} 