package com.example.fuletracker.ux

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.fuletracker.R
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val stats = viewModel.computeStats(entries)
    val sorted = entries.sortedBy { it.odometer }

    // Fetch all strings at the top level of the composable
    val unitKmL = stringResource(R.string.unit_km_l)
    val unitRupeePerL = stringResource(R.string.unit_rupee_per_l)
    val avgEfficiencyLabel = stringResource(R.string.stat_avg_efficiency)
    val bestEfficiencyLabel = stringResource(R.string.stat_best_efficiency)
    val worstEfficiencyLabel = stringResource(R.string.stat_worst_efficiency)
    val totalDistanceLabel = stringResource(R.string.stat_total_distance)
    val totalFuelLabel = stringResource(R.string.stat_total_fuel)
    val totalCostLabel = stringResource(R.string.stat_total_cost)
    val fillUpsLabel = stringResource(R.string.stat_fill_ups)
    val chartMileageTitle = stringResource(R.string.chart_mileage_title)
    val chartPriceTitle = stringResource(R.string.chart_price_title)
    val chartExpensesTitle = stringResource(R.string.chart_expenses_title)
    val needFullTankWarning = stringResource(R.string.need_full_tank_entries_warning)
    val needEntriesWarning = stringResource(R.string.need_entries_warning)
    val noExpenseWarning = stringResource(R.string.no_expense_data_warning)
    val historyHeader = stringResource(R.string.history_header)
    val noVehicleWarning = stringResource(R.string.no_vehicle_selected_warning)
    val noEntriesWarning = stringResource(R.string.no_entries_warning)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatRow(avgEfficiencyLabel, stats?.let { "${"%.1f".format(it.avgEfficiency)} $unitKmL" } ?: "—")
                    HorizontalDivider()
                    StatRow(bestEfficiencyLabel, stats?.let { "${"%.1f".format(it.bestEfficiency)} $unitKmL" } ?: "—")
                    HorizontalDivider()
                    StatRow(worstEfficiencyLabel, stats?.let { "${"%.1f".format(it.worstEfficiency)} $unitKmL" } ?: "—")
                    HorizontalDivider()
                    StatRow(totalDistanceLabel, stats?.let { "${"%.0f".format(it.totalDistance)} km" } ?: "—")
                    HorizontalDivider()
                    StatRow(totalFuelLabel, stats?.let { "${"%.1f".format(it.totalFuel)} L" } ?: "—")
                    HorizontalDivider()
                    StatRow(totalCostLabel, stats?.let { "₹${"%.0f".format(it.totalCost)}" } ?: "—")
                    HorizontalDivider()
                    StatRow(fillUpsLabel, stats?.entryCount?.toString() ?: "0")
                }
            }
        }

        item {
            ChartCard(title = chartMileageTitle) {
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
                            val dataset = LineDataSet(dataPoints, unitKmL).applyEfficiencyStyle()
                            chart.data = LineData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    ChartPlaceholder(needFullTankWarning)
                }
            }
        }

        item {
            ChartCard(title = chartPriceTitle) {
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
                            val dataset = LineDataSet(dataPoints, unitRupeePerL).applyPriceStyle()
                            chart.data = LineData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    ChartPlaceholder(needEntriesWarning)
                }
            }
        }

        item {
            ChartCard(title = chartExpensesTitle) {
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
                    ChartPlaceholder(noExpenseWarning)
                }
            }
        }

        item {
            Text(historyHeader, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp))
        }

        if (entries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text(
                        if (selectedVehicle == null) noVehicleWarning
                        else noEntriesWarning,
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
