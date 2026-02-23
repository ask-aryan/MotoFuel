package com.example.fuletracker.ux

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val stats = viewModel.computeStats(entries)
    val sorted = entries.sortedBy { it.odometer }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Title ──────────────────────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Statistics", style = MaterialTheme.typography.headlineMedium)
                if (selectedVehicle != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, Modifier.size(14.dp))
                            Text(selectedVehicle!!.name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // ── Summary cards ──────────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatRow("Avg Efficiency", stats?.let { "${"%.1f".format(it.avgEfficiency)} km/L" } ?: "—")
                    HorizontalDivider()
                    StatRow("Best Efficiency", stats?.let { "${"%.1f".format(it.bestEfficiency)} km/L" } ?: "—")
                    HorizontalDivider()
                    StatRow("Worst Efficiency", stats?.let { "${"%.1f".format(it.worstEfficiency)} km/L" } ?: "—")
                    HorizontalDivider()
                    StatRow("Total Distance", stats?.let { "${"%.0f".format(it.totalDistance)} km" } ?: "—")
                    HorizontalDivider()
                    StatRow("Total Fuel", stats?.let { "${"%.1f".format(it.totalFuel)} L" } ?: "—")
                    HorizontalDivider()
                    StatRow("Total Cost", stats?.let { "₹${"%.0f".format(it.totalCost)}" } ?: "—")
                    HorizontalDivider()
                    StatRow("Fill-ups", stats?.entryCount?.toString() ?: "0")
                }
            }
        }

        // ── Chart 1: Mileage over time ─────────────────────────────────────
        item {
            ChartCard(title = "⚡ Mileage Over Time (km/L)") {
                val fullTankEntries = sorted.filter { it.fullTank }
                if (fullTankEntries.size >= 2) {
                    val dataPoints = mutableListOf<Entry>()
                    val labels = mutableListOf<String>()
                    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

                    for (i in 1 until fullTankEntries.size) {
                        val dist = fullTankEntries[i].odometer - fullTankEntries[i - 1].odometer
                        val fuel = fullTankEntries[i].fuelAmount
                        if (dist > 0 && fuel > 0) {
                            dataPoints.add(Entry((i - 1).toFloat(), (dist / fuel).toFloat()))
                            labels.add(dateFmt.format(Date(fullTankEntries[i].date)))
                        }
                    }

                    AndroidView(
                        factory = { ctx -> buildLineChart(ctx) },
                        update = { chart ->
                            val dataset = LineDataSet(dataPoints, "km/L").applyEfficiencyStyle()
                            chart.data = LineData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    ChartPlaceholder("Need at least 2 full-tank entries")
                }
            }
        }

        // ── Chart 2: Fuel price trend ──────────────────────────────────────
        item {
            ChartCard(title = "📈 Fuel Price Trend (₹/L)") {
                if (sorted.size >= 2) {
                    val dataPoints = sorted.mapIndexed { i, e ->
                        Entry(i.toFloat(), e.pricePerLiter.toFloat())
                    }
                    val labels = sorted.map {
                        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it.date))
                    }

                    AndroidView(
                        factory = { ctx -> buildLineChart(ctx) },
                        update = { chart ->
                            val dataset = LineDataSet(dataPoints, "₹/L").applyPriceStyle()
                            chart.data = LineData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    ChartPlaceholder("Need at least 2 entries")
                }
            }
        }

        // ── Chart 3: Monthly expenses ──────────────────────────────────────
        item {
            ChartCard(title = "💰 Monthly Expenses (₹)") {
                val monthlyData = entries
                    .groupBy {
                        SimpleDateFormat("MMM yy", Locale.getDefault()).format(Date(it.date))
                    }
                    .entries
                    .sortedBy { it.key }

                if (monthlyData.size >= 1) {
                    val barEntries = monthlyData.mapIndexed { i, (_, v) ->
                        BarEntry(i.toFloat(), v.sumOf { it.fuelAmount * it.pricePerLiter }.toFloat())
                    }
                    val labels = monthlyData.map { it.key }

                    AndroidView(
                        factory = { ctx -> buildBarChart(ctx) },
                        update = { chart ->
                            val dataset = BarDataSet(barEntries, "₹").applyExpenseStyle()
                            chart.data = BarData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    ChartPlaceholder("No expense data yet")
                }
            }
        }

        // ── History ────────────────────────────────────────────────────────
        item {
            Text("History", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp))
        }

        if (entries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text(
                        if (selectedVehicle == null) "No vehicle selected"
                        else "No entries for this vehicle",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(entries.sortedByDescending { it.date }) { entry ->
                EntryCard(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
            }
        }
    }
}

// ── Chart builders ─────────────────────────────────────────────────────────────

fun buildLineChart(ctx: android.content.Context): LineChart {
    return LineChart(ctx).apply {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(false)
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = AndroidColor.parseColor("#49454F")
            textSize = 9f
        }
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = AndroidColor.parseColor("#E6E1E5")
            textColor = AndroidColor.parseColor("#49454F")
        }
        axisRight.isEnabled = false
        setExtraOffsets(8f, 8f, 8f, 8f)
    }
}

fun buildBarChart(ctx: android.content.Context): BarChart {
    return BarChart(ctx).apply {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(true)
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = AndroidColor.parseColor("#49454F")
            textSize = 9f
        }
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = AndroidColor.parseColor("#E6E1E5")
            textColor = AndroidColor.parseColor("#49454F")
        }
        axisRight.isEnabled = false
        setExtraOffsets(8f, 8f, 8f, 8f)
    }
}

fun LineDataSet.applyEfficiencyStyle(): LineDataSet {
    color = AndroidColor.parseColor("#6750A4")
    setCircleColor(AndroidColor.parseColor("#6750A4"))
    lineWidth = 2.5f
    circleRadius = 4f
    setDrawFilled(true)
    fillColor = AndroidColor.parseColor("#EADDFF")
    fillAlpha = 80
    valueTextColor = AndroidColor.parseColor("#49454F")
    valueTextSize = 9f
    mode = LineDataSet.Mode.CUBIC_BEZIER
    return this
}

fun LineDataSet.applyPriceStyle(): LineDataSet {
    color = AndroidColor.parseColor("#B3261E")
    setCircleColor(AndroidColor.parseColor("#B3261E"))
    lineWidth = 2.5f
    circleRadius = 4f
    setDrawFilled(true)
    fillColor = AndroidColor.parseColor("#F9DEDC")
    fillAlpha = 80
    valueTextColor = AndroidColor.parseColor("#49454F")
    valueTextSize = 9f
    mode = LineDataSet.Mode.CUBIC_BEZIER
    return this
}

fun BarDataSet.applyExpenseStyle(): BarDataSet {
    color = AndroidColor.parseColor("#D0BCFF")
    valueTextColor = AndroidColor.parseColor("#49454F")
    valueTextSize = 9f
    return this
}

// ── Shared composables ─────────────────────────────────────────────────────────
