package com.example.sofrehmessina.util

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.sofrehmessina.R

/**
 * Extension functions for Views to support Persian text
 */

/**
 * Configure a TextView for Persian text
 */
fun TextView.configurePersian() {
    // Set text direction to LTR
    this.textDirection = View.TEXT_DIRECTION_LTR
    
    // Set text alignment to start
    this.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    
    // Use Persian font
    try {
        val persianTypeface = ResourcesCompat.getFont(context, R.font.vazirbold)
        this.typeface = persianTypeface
    } catch (e: Exception) {
        // Fall back to system font
    }
}

/**
 * Configure an EditText for Persian text input
 */
fun EditText.configurePersianInput() {
    // Configure basic TextView properties
    this.configurePersian()
    
    // Configure input-specific properties
    InputHelper.configurePersianEditText(this)
}

/**
 * Inflate a layout and configure all TextViews and EditTexts for Persian text
 */
fun Context.inflatePersianLayout(resource: Int, root: ViewGroup?, attachToRoot: Boolean = false): View {
    val inflater = LayoutInflater.from(this)
    val view = inflater.inflate(resource, root, attachToRoot)
    
    // Find all TextViews and configure them
    findAndConfigureAllTextViews(view)
    
    return view
}

/**
 * Recursively find and configure all TextViews and EditTexts in a view hierarchy
 */
private fun findAndConfigureAllTextViews(view: View) {
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            findAndConfigureAllTextViews(view.getChildAt(i))
        }
    } else if (view is EditText) {
        view.configurePersianInput()
    } else if (view is TextView) {
        view.configurePersian()
    }
} 