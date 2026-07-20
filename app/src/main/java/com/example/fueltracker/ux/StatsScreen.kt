package com.example.fueltracker.ux

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.fueltracker.R
import com.example.fueltracker.data.FuelEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val stats = viewModel.computeStats(entries)
    val sorted = entries.sortedBy { it.odometer }
    var editingEntry by remember { mutableStateOf<FuelEntry?>(null) }

    val accentColor = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val mutedColor = AndroidColor.parseColor("#6B7280")
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val unitKmL = stringResource(R.string.unit_km_l)
    val needFullTankWarning = stringResource(R.string.need_full_tank_entries_warning)
    val noExpenseWarning = stringResource(R.string.no_expense_data_warning)
    val historyHeader = stringResource(R.string.history_header)
    val noVehicleWarning = stringResource(R.string.no_vehicle_selected_warning)
    val noEntriesWarning = stringResource(R.string.no_entries_warning)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Page Header ───────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Stats",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (stats != null) {
                    Text(
                        "${stats.entryCount} fill-ups",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 3-chip metric strip ──────────────────────────────────────────────
        if (stats != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(
                        label = "BEST",
                        value = "${"%.1f".format(stats.bestEfficiency)}",
                        unit = unitKmL,
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "AVG",
                        value = "${"%.1f".format(stats.avgEfficiency)}",
                        unit = unitKmL,
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "WORST",
                        value = "${"%.1f".format(stats.worstEfficiency)}",
                        unit = unitKmL,
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Totals row ────────────────────────────────────────────────────────
        if (stats != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(
                        label = "DISTANCE",
                        value = "${"%.0f".format(stats.totalDistance)}",
                        unit = "km",
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "FUEL",
                        value = "${"%.1f".format(stats.totalFuel)}",
                        unit = "L",
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "COST",
                        value = "₹${"%.0f".format(stats.totalCost)}",
                        unit = "",
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Mileage chart ─────────────────────────────────────────────────────
        item {
            StyledChartCard(title = "Mileage over time", subtitle = unitKmL) {
                val segmentEfficiencies = viewModel.computeSegmentEfficiencies(entries)
                if (segmentEfficiencies.size >= 1) {
                    val dataPoints = mutableListOf<Entry>()
                    val labels = mutableListOf<String>()
                    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
                    segmentEfficiencies.forEachIndexed { i, (entry, efficiency) ->
                        dataPoints.add(Entry(i.toFloat(), efficiency.toFloat()))
                        labels.add(dateFmt.format(Date(entry.date)))
                    }
                    AndroidView(
                        factory = { ctx -> buildLineChart(ctx, surfaceColor, mutedColor) },
                        update = { chart ->
                            val dataset = LineDataSet(dataPoints, unitKmL).applyAccentLineStyle(accentColor, labelTextColor)
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

        // ── Monthly expenses chart ────────────────────────────────────────────
        item {
            StyledChartCard(title = "Monthly expenses", subtitle = "₹") {
                val monthlyData = entries
                    .groupBy { SimpleDateFormat("MMM yy", Locale.getDefault()).format(Date(it.date)) }
                    .entries
                    .sortedBy { (_, v) -> v.minOf { it.date } }  // chronological, not alphabetical

                if (monthlyData.isNotEmpty()) {
                    val barEntries = monthlyData.mapIndexed { i, (_, v) ->
                        BarEntry(i.toFloat(), v.sumOf { it.fuelAmount * it.pricePerLiter }.toFloat())
                    }
                    val monthKeys = monthlyData.map { it.key }
                    AndroidView(
                        factory = { ctx -> buildBarChart(ctx, surfaceColor, mutedColor) },
                        update = { chart ->
                            val dataset = BarDataSet(barEntries, "₹")
                                .applyAccentBarStyle(accentColor, labelTextColor)
                            chart.data = BarData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthKeys)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    ChartPlaceholder(noExpenseWarning)
                }
            }
        }

        // ── Fuel Price Trend chart ────────────────────────────────────────────
        item {
            StyledChartCard(title = "Fuel Price Trend", subtitle = "₹/L") {
                val pricePoints = sorted
                    .filter { it.pricePerLiter > 0 }
                    .sortedBy { it.date }
                if (pricePoints.size >= 2) {
                    val dataPoints = pricePoints.mapIndexed { i, e ->
                        Entry(i.toFloat(), e.pricePerLiter.toFloat())
                    }
                    val labels = pricePoints.map { e ->
                        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(e.date))
                    }
                    AndroidView(
                        factory = { ctx -> buildLineChart(ctx, surfaceColor, mutedColor) },
                        update = { chart ->
                            val dataset = LineDataSet(dataPoints, "₹/L")
                                .applyAccentLineStyle(accentColor, labelTextColor)
                            chart.data = LineData(dataset)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                } else {
                    ChartPlaceholder("Need at least 2 entries with price data")
                }
            }
        }

        // ── History header ────────────────────────────────────────────────────
        item {
            Text(
                historyHeader.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (entries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text(
                        if (selectedVehicle == null) noVehicleWarning else noEntriesWarning,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(entries.sortedByDescending { it.date }) { entry ->
                EntryCard(
                    entry = entry,
                    onEdit = { editingEntry = entry },
                    onDelete = { viewModel.deleteEntry(entry) }
                )
            }
        }

        item { Spacer(Modifier.height(140.dp)) }
    }

    editingEntry?.let { entry ->
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingEntry = null },
            sheetState = editSheetState
        ) {
            AddEntryScreen(
                viewModel = viewModel,
                editingEntry = entry,
                onEntryAdded = { editingEntry = null },
                onDismiss = { editingEntry = null },
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    unit: String,
    accent: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bg = if (accent) primary else MaterialTheme.colorScheme.surface
    val borderColor = if (accent) primary else MaterialTheme.colorScheme.outlineVariant
    val labelColor = if (accent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                     else MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = if (accent) MaterialTheme.colorScheme.onPrimary
                     else MaterialTheme.colorScheme.onSurface
    val unitColor = if (accent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor, letterSpacing = 1.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = unitColor)
    }
}

@Composable
private fun StyledChartCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

fun buildLineChart(ctx: android.content.Context, surfaceColor: Int, axisTextColor: Int): LineChart {
    return LineChart(ctx).apply {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(false)
        setBackgroundColor(surfaceColor)
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = axisTextColor
            textSize = 9f
        }
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = AndroidColor.parseColor("#23272c")
            textColor = axisTextColor
        }
        axisRight.isEnabled = false
        setExtraOffsets(8f, 8f, 8f, 8f)
    }
}

fun buildBarChart(ctx: android.content.Context, surfaceColor: Int, axisTextColor: Int): BarChart {
    return BarChart(ctx).apply {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(true)
        setBackgroundColor(surfaceColor)
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = axisTextColor
            textSize = 9f
        }
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = AndroidColor.parseColor("#23272c")
            textColor = axisTextColor
        }
        axisRight.isEnabled = false
        setExtraOffsets(8f, 8f, 8f, 8f)
    }
}

fun LineDataSet.applyAccentLineStyle(accentColor: Int, labelColor: Int = AndroidColor.TRANSPARENT): LineDataSet {
    color = accentColor
    setCircleColor(accentColor)
    lineWidth = 2.5f
    circleRadius = 4f
    setDrawFilled(true)
    fillColor = accentColor
    fillAlpha = 40
    valueTextColor = labelColor
    valueTextSize = 9f
    valueFormatter = object : ValueFormatter() {
        override fun getPointLabel(entry: Entry) = "%.1f".format(entry.y)
    }
    mode = LineDataSet.Mode.CUBIC_BEZIER
    return this
}

fun BarDataSet.applyAccentBarStyle(
    accentColor: Int,
    labelColor: Int = AndroidColor.TRANSPARENT
): BarDataSet {
    color = accentColor
    valueTextColor = labelColor
    valueTextSize = 9f
    valueFormatter = object : ValueFormatter() {
        override fun getBarLabel(barEntry: BarEntry) = "₹${barEntry.y.toInt()}"
    }
    return this
}
