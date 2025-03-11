package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.sofrehmessina.data.model.Languages

/**
 * A text field component that supports entering text in multiple languages
 * with appropriate text direction for each language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultilingualTextField(
    translations: Map<String, String>,
    onTranslationsChange: (Map<String, String>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    var selectedLanguage by remember { mutableStateOf(Languages.PERSIAN) } // Default to Persian
    
    // Debug the current translations
    LaunchedEffect(translations) {
        android.util.Log.d("MultilingualTextField", "Current translations: $translations")
    }
    
    Column(modifier = modifier) {
        // Simplified language tabs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Languages.SUPPORTED_LANGUAGES.forEach { langCode ->
                    val langInfo = when (langCode) {
                        Languages.PERSIAN -> Triple("🇮🇷", "فارسی", "Persian")
                        Languages.ENGLISH -> Triple("🇬🇧", "English", "English")
                        Languages.ITALIAN -> Triple("🇮🇹", "Italiano", "Italian")
                        else -> Triple("🌐", "Other", "Other")
                    }
                    
                    val isSelected = langCode == selectedLanguage
                    val hasContent = translations[langCode]?.isNotBlank() == true
                    
                    Surface(
                        onClick = { selectedLanguage = langCode },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else if (hasContent) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            Color.Transparent,
                        contentColor = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = langInfo.first, // Flag
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = langInfo.second, // Native name
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (hasContent) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    
                    if (langCode != Languages.SUPPORTED_LANGUAGES.last()) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Text field for the current language
        OutlinedTextField(
            value = translations[selectedLanguage] ?: "",
            onValueChange = { newValue ->
                val updatedTranslations = translations.toMap() + (selectedLanguage to newValue)
                android.util.Log.d("MultilingualTextField", "Updating $selectedLanguage to: $newValue")
                android.util.Log.d("MultilingualTextField", "New translations map: $updatedTranslations")
                onTranslationsChange(updatedTranslations)
            },
            label = { 
                val langInfo = when (selectedLanguage) {
                    Languages.PERSIAN -> "$label (Persian 🇮🇷)"
                    Languages.ENGLISH -> "$label (English 🇬🇧)"
                    Languages.ITALIAN -> "$label (Italian 🇮🇹)"
                    else -> label
                }
                Text(langInfo)
            },
            textStyle = getTextStyleForLanguage(selectedLanguage),
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            supportingText = supportingText,
            singleLine = singleLine,
            maxLines = maxLines
        )
    }
}

/**
 * Gets the appropriate text style for a given language code,
 * with proper text direction settings.
 */
@Composable
private fun getTextStyleForLanguage(langCode: String): TextStyle {
    val baseStyle = MaterialTheme.typography.bodyLarge
    
    return when (langCode) {
        Languages.PERSIAN -> baseStyle.copy(textDirection = TextDirection.Rtl)
        else -> baseStyle.copy(textDirection = TextDirection.Ltr)
    }
} 