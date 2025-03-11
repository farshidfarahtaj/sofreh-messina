package com.example.sofrehmessina.util

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast

/**
 * Helper class for improving Persian text input in the app
 */
class InputHelper {
    companion object {
        /**
         * Configure an EditText for Persian input
         */
        fun configurePersianEditText(editText: EditText) {
            // Set text direction to match locale (important for mixed RTL/LTR content)
            editText.textDirection = View.TEXT_DIRECTION_LTR
            
            // Set text alignment to the start (respects the layout direction)
            editText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            
            // Use Persian font if available
            try {
                val persianTypeface = Typeface.createFromAsset(editText.context.assets, "font/vazirbold.ttf")
                editText.typeface = persianTypeface
            } catch (e: Exception) {
                // Fall back to system font if Persian font isn't available
            }
            
            // Add TextWatcher to ensure proper text direction for Persian
            configurePersianTextWatcher(editText)
        }
        
        /**
         * Configure a TextWatcher for proper Persian text handling
         */
        private fun configurePersianTextWatcher(editText: EditText) {
            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s == null) return
                    
                    // Remove any explicit direction markers that might be added
                    var text = s.toString()
                    if (text.contains("\u200E") || text.contains("\u200F")) {
                        text = text.replace("\u200E", "").replace("\u200F", "")
                        val wasEmpty = s.isEmpty()
                        s.clear()
                        s.append(text)
                        
                        // Reset cursor position if needed
                        if (!wasEmpty) {
                            editText.setSelection(text.length)
                        }
                    }
                }
            })
        }

        /**
         * Show the keyboard for the given EditText
         */
        fun showKeyboard(context: Context, editText: EditText) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            editText.requestFocus()
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        
        /**
         * Check if a Persian keyboard is available
         */
        fun isPersianKeyboardAvailable(context: Context): Boolean {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val inputMethodSubtype = imm.currentInputMethodSubtype ?: return false
            
            // Check if the keyboard is set to Persian
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                inputMethodSubtype.languageTag
            } else {
                @Suppress("DEPRECATION")
                inputMethodSubtype.locale
            }
            
            return locale.contains("fa") || locale.contains("IR") || locale.contains("ir")
        }
        
        /**
         * Prompt user to switch to a Persian keyboard
         */
        fun promptForPersianKeyboard(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to app settings if input method settings aren't available
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }
        }
        
        /**
         * Show a toast suggesting to switch to Persian keyboard
         */
        fun showPersianKeyboardSuggestion(context: Context) {
            Toast.makeText(
                context,
                "برای تایپ فارسی، لطفاً صفحه‌کلید فارسی را فعال کنید",
                Toast.LENGTH_LONG
            ).show()
        }
    }
} 