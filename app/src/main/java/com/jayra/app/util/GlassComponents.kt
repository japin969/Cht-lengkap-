package com.jayra.app.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.glassSurfaceV2
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier

/**
 * Common Glass Components used across all features.
 * Single source of truth for GlassCard to avoid duplicate definitions.
 */
@Composable
fun GlassCard(
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 16.dp)
    ) {
        content()
    }
}