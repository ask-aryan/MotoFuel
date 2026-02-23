package com.example.fuletracker.ux

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingTooltip(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    message: String,
    arrowDown: Boolean = true,     // arrow points down toward FAB
    onDismiss: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { if (arrowDown) it else -it },
        exit = fadeOut() + slideOutVertically { if (arrowDown) it else -it }
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 24.dp)
        ) {
            if (!arrowDown) {
                // Arrow pointing up
                Box(
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(12.dp, 8.dp)
                        .clip(ArrowShape(pointUp = true))
                        .background(MaterialTheme.colorScheme.inverseSurface)
                )
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon, null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.widthIn(max = 200.dp)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (arrowDown) {
                // Arrow pointing down toward FAB
                Box(
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(12.dp, 8.dp)
                        .clip(ArrowShape(pointUp = false))
                        .background(MaterialTheme.colorScheme.inverseSurface)
                )
            }
        }
    }
}

// Custom arrow shape
class ArrowShape(private val pointUp: Boolean) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            if (pointUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}