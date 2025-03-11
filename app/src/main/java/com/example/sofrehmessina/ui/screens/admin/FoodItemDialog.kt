package com.example.sofrehmessina.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.data.model.FoodItem
import com.example.sofrehmessina.data.model.FoodTranslation
import com.example.sofrehmessina.data.model.Languages
import com.example.sofrehmessina.ui.components.MultilingualTextField
import com.example.sofrehmessina.util.TranslationManager
import com.example.sofrehmessina.util.LocaleHelper
import java.util.*

/**
 * Dialog for adding or editing a food item with multilingual support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodItemDialog(
    foodItem: FoodItem? = null,
    categories: List<Map<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit,
    translationManager: TranslationManager? = null
) {
    val isEditing = foodItem != null
    val context = LocalContext.current
    // Get current language directly if TranslationManager is not provided
    val currentLang = translationManager?.getCurrentLanguage() ?: LocaleHelper.getSelectedLanguageCode(context)
    
    // State for all form fields
    val nameTranslations = remember {
        mutableStateOf(
            foodItem?.translations?.mapValues { it.value.name }?.toMutableMap()
                ?: Languages.SUPPORTED_LANGUAGES.associateWith { "" }.toMutableMap()
        )
    }
    
    val descriptionTranslations = remember {
        mutableStateOf(
            foodItem?.translations?.mapValues { it.value.description }?.toMutableMap()
                ?: Languages.SUPPORTED_LANGUAGES.associateWith { "" }.toMutableMap()
        )
    }
    
    var price by remember { mutableStateOf(foodItem?.price?.toString() ?: "") }
    var priceError by remember { mutableStateOf<String?>(null) }
    
    var discountPrice by remember { mutableStateOf(foodItem?.discountPrice?.toString() ?: "") }
    
    var categoryId by remember { mutableStateOf(foodItem?.categoryId ?: "") }
    var categoryError by remember { mutableStateOf<String?>(null) }
    
    var imageUrl by remember { mutableStateOf(foodItem?.imageUrl ?: "") }
    var imageError by remember { mutableStateOf<String?>(null) }
    
    var featured by remember { mutableStateOf(foodItem?.featured ?: false) }
    var available by remember { mutableStateOf(foodItem?.available ?: true) }
    
    // Validation function
    fun validate(): Boolean {
        var isValid = true
        
        // Check that at least one language has a name
        if (nameTranslations.value.values.all { it.isBlank() }) {
            isValid = false
        }
        
        if (price.isBlank() || price.toDoubleOrNull() == null) {
            priceError = "Valid price is required"
            isValid = false
        } else {
            priceError = null
        }
        
        if (categoryId.isBlank()) {
            categoryError = "Category is required"
            isValid = false
        } else {
            categoryError = null
        }
        
        if (imageUrl.isBlank()) {
            imageError = "Image URL is required"
            isValid = false
        } else {
            imageError = null
        }
        
        return isValid
    }
    
    // Build the food item from form data
    fun buildFoodItem(): FoodItem {
        // Create translations map
        val translations = Languages.SUPPORTED_LANGUAGES.associateWith { langCode ->
            FoodTranslation(
                name = nameTranslations.value[langCode] ?: "",
                description = descriptionTranslations.value[langCode] ?: "",
                ingredients = foodItem?.translations?.get(langCode)?.ingredients ?: listOf()
            )
        }
        
        val now = System.currentTimeMillis()
        
        return FoodItem(
            id = foodItem?.id ?: UUID.randomUUID().toString(),
            translations = translations,
            price = price.toDoubleOrNull() ?: 0.0,
            discountPrice = if (discountPrice.isNotBlank()) discountPrice.toDoubleOrNull() else null,
            categoryId = categoryId,
            imageUrl = imageUrl,
            featured = featured,
            available = available,
            createdAt = foodItem?.createdAt ?: now,
            updatedAt = now
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Food Item" else "Add Food Item")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Multilingual name field
                MultilingualTextField(
                    translations = nameTranslations.value,
                    onTranslationsChange = { nameTranslations.value = it.toMutableMap() },
                    label = "Name",
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameTranslations.value.values.all { it.isBlank() },
                    supportingText = {
                        if (nameTranslations.value.values.all { it.isBlank() }) {
                            Text("Name is required in at least one language")
                        }
                    }
                )
                
                // Translation helper buttons for name
                nameTranslations.value[Languages.PERSIAN]?.takeIf { it.isNotBlank() }?.let { persianText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Translate from Persian:", 
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        
                        // Button to translate to English
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://translate.google.com/?sl=fa&tl=en&text=${Uri.encode(persianText)}&op=translate"
                                ))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🇬🇧", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        // Button to translate to Italian
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://translate.google.com/?sl=fa&tl=it&text=${Uri.encode(persianText)}&op=translate"
                                ))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🇮🇹", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                // Multilingual description field
                MultilingualTextField(
                    translations = descriptionTranslations.value,
                    onTranslationsChange = { descriptionTranslations.value = it.toMutableMap() },
                    label = "Description",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 5
                )
                
                // Translation helper buttons for description
                descriptionTranslations.value[Languages.PERSIAN]?.takeIf { it.isNotBlank() }?.let { persianText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Translate from Persian:", 
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        
                        // Button to translate to English
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://translate.google.com/?sl=fa&tl=en&text=${Uri.encode(persianText)}&op=translate"
                                ))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🇬🇧", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        // Button to translate to Italian
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://translate.google.com/?sl=fa&tl=it&text=${Uri.encode(persianText)}&op=translate"
                                ))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🇮🇹", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                // Price fields (non-multilingual)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { 
                            price = it 
                            priceError = null
                        },
                        label = { Text("Price") },
                        modifier = Modifier.weight(1f),
                        isError = priceError != null,
                        supportingText = priceError?.let { { Text(it) } }
                    )
                    
                    OutlinedTextField(
                        value = discountPrice,
                        onValueChange = { discountPrice = it },
                        label = { Text("Discount Price (Optional)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = categories.find { it["id"] == categoryId }
                            ?.get(currentLang)
                            ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = categoryError != null,
                        supportingText = categoryError?.let { { Text(it) } }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = false,
                        onDismissRequest = { }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Text(category[currentLang] ?: category.values.firstOrNull() ?: "") 
                                },
                                onClick = { 
                                    categoryId = category["id"] ?: ""
                                    categoryError = null
                                }
                            )
                        }
                    }
                }
                
                // Image URL
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { 
                        imageUrl = it 
                        imageError = null
                    },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = imageError != null,
                    supportingText = imageError?.let { { Text(it) } }
                )
                
                // Featured and available toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = featured,
                            onCheckedChange = { featured = it }
                        )
                        Text("Featured")
                    }
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = available,
                            onCheckedChange = { available = it }
                        )
                        Text("Available")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (validate()) {
                        onSave(buildFoodItem())
                    }
                }
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 