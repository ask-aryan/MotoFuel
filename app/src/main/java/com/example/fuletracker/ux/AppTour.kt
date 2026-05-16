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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fuletracker.R
import androidx.compose.ui.res.stringResource

data class TourStep(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val tipRes: Int? = null
)

val tourSteps = listOf(
    TourStep(
        icon = Icons.Default.Check,
        titleRes = R.string.tour_welcome_title,
        descriptionRes = R.string.tour_welcome_desc,
        tipRes = null
    ),
    TourStep(
        icon = Icons.Default.LocalGasStation,
        titleRes = R.string.tour_log_fillups_title,
        descriptionRes = R.string.tour_log_fillups_desc,
        tipRes = R.string.tour_log_fillups_tip
    ),
    TourStep(
        icon = Icons.Default.BarChart,
        titleRes = R.string.tour_track_stats_title,
        descriptionRes = R.string.tour_track_stats_desc,
        tipRes = R.string.tour_track_stats_tip
    ),
    TourStep(
        icon = Icons.Default.Settings,
        titleRes = R.string.tour_settings_title,
        descriptionRes = R.string.tour_settings_desc,
        tipRes = R.string.tour_settings_tip
    ),
    TourStep(
        icon = Icons.Default.Rocket,
        titleRes = R.string.tour_ready_title,
        descriptionRes = R.string.tour_ready_desc,
        tipRes = null
    )
)

@Composable
fun AppTour(onFinish: () -> Unit) {
    var currentStep by remember { mutableStateOf(0) }
    
    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "tour"
            ) { stepIndex ->
                val s = tourSteps[stepIndex]
                TourCard(
                    step = s,
                    currentStep = stepIndex,
                    totalSteps = tourSteps.size,
                    isLast = stepIndex == tourSteps.lastIndex,
                    onNext = { currentStep++ },
                    onSkip = onFinish,
                    onFinish = onFinish
                )
            }
        }
    }
}

@Composable
fun TourCard(
    step: TourStep,
    currentStep: Int,
    totalSteps: Int,
    isLast: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index == currentStep) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title
            Text(
                stringResource(step.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Description
            Text(
                stringResource(step.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            // Tip
            step.tipRes?.let {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(it),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.skip_tour), color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Button(
                    onClick = if (isLast) onFinish else onNext,
                    modifier = Modifier.then(
                        if (isLast) Modifier.fillMaxWidth() else Modifier
                    )
                ) {
                    Text(
                        if (isLast) stringResource(R.string.get_started) else stringResource(R.string.next_arrow),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}