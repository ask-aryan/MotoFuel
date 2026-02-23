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

data class TourStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val tip: String? = null
)

val tourSteps = listOf(
    TourStep(
        icon = Icons.Default.Check,
        title = "You're All Set! 🎉",
        description = "Great job! Your fuel price and vehicle are configured. Let's explore what MotoFuel can do.",
        tip = null
    ),
    TourStep(
        icon = Icons.Default.LocalGasStation,
        title = "Log Your Fill-ups",
        description = "Tap the fuel pump button on the Dashboard to add a fill-up. Enter your odometer reading and fuel amount.",
        tip = "💡 Tip: Always fill to a full tank for the most accurate efficiency calculation!"
    ),
    TourStep(
        icon = Icons.Default.BarChart,
        title = "Track Your Stats",
        description = "The Stats tab shows your mileage trend, fuel price history, and monthly expenses as beautiful charts.",
        tip = "💡 Tip: You need at least 2 full-tank entries to see efficiency charts."
    ),
    TourStep(
        icon = Icons.Default.Route,
        title = "Trip Mode",
        description = "Use Trip Mode to track specific journeys. Start a named trip like 'Jaipur to Delhi' or use quick Trip A/B counters.",
        tip = "💡 Tip: Trip stats show distance, fuel used, and cost for each journey."
    ),
    TourStep(
        icon = Icons.Default.Lightbulb,
        title = "Smart Insights",
        description = "MotoFuel analyzes your data and shows insights on the Dashboard — like mileage drops, price changes, and your best fill-up ever!",
        tip = null
    ),
    TourStep(
        icon = Icons.Default.Settings,
        title = "Settings",
        description = "Manage your garage and update fuel prices anytime from the Settings tab.",
        tip = "💡 Tip: Price history is tracked automatically from your fill-up entries."
    ),
    TourStep(
        icon = Icons.Default.Rocket,
        title = "Ready to Roll! 🚀",
        description = "You know everything! Start logging fill-ups and watch MotoFuel learn your vehicle's patterns.",
        tip = null
    )
)

@Composable
fun AppTour(onFinish: () -> Unit) {
    var currentStep by remember { mutableStateOf(0) }
    val step = tourSteps[currentStep]
    val isLast = currentStep == tourSteps.lastIndex

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
                step.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Description
            Text(
                step.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            // Tip
            if (step.tip != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        step.tip,
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
                        Text("Skip Tour", color = MaterialTheme.colorScheme.outline)
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
                        if (isLast) "Get Started!" else "Next →",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}