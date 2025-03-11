package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sofrehmessina.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    logoSize: Float = 1.0f
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(R.string.app_logo_description),
            modifier = Modifier.size((80 * logoSize).dp)
        )
    }
} 