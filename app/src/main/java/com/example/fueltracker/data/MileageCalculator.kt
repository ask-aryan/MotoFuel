package com.example.fueltracker.data

/**
 * Computes mileage per full-tank-to-full-tank segment. A segment spans from one full
 * tank fill to the next; fuel from any partial fills in between is included in the
 * segment's fuel total, since it was consumed over that same distance.
 */
fun computeSegmentEfficiencies(entries: List<FuelEntry>): List<Pair<FuelEntry, Double>> {
    val sorted = entries.sortedBy { it.odometer }
    val segments = mutableListOf<Pair<FuelEntry, Double>>()
    var lastFullOdometer: Double? = null
    var fuelSinceLastFull = 0.0
    for (entry in sorted) {
        fuelSinceLastFull += entry.fuelAmount
        if (entry.fullTank) {
            val anchor = lastFullOdometer
            if (anchor != null) {
                val dist = entry.odometer - anchor
                if (dist > 0 && fuelSinceLastFull > 0) {
                    segments.add(Pair(entry, dist / fuelSinceLastFull))
                }
            }
            lastFullOdometer = entry.odometer
            fuelSinceLastFull = 0.0
        }
    }
    return segments
}

/**
 * Aggregates fuel entries into overall stats (distance, cost, efficiency). Returns
 * null when there are no entries, since totals/averages are meaningless for an empty log.
 */
fun computeFuelStats(entries: List<FuelEntry>): FuelStats? {
    if (entries.isEmpty()) return null
    val sorted = entries.sortedBy { it.odometer }
    val segments = computeSegmentEfficiencies(entries).map { it.second }
    val totalDistance = if (sorted.size > 1) sorted.last().odometer - sorted.first().odometer else 0.0
    val totalCost = entries.sumOf { it.fuelAmount * it.pricePerLiter }
    return FuelStats(
        avgEfficiency = if (segments.isEmpty()) 0.0 else segments.average(),
        bestEfficiency = segments.maxOrNull() ?: 0.0,
        worstEfficiency = segments.minOrNull() ?: 0.0,
        totalDistance = totalDistance,
        totalFuel = entries.sumOf { it.fuelAmount },
        totalCost = totalCost,
        lastOdometer = sorted.last().odometer,
        costPerKm = if (totalDistance > 0) totalCost / totalDistance else 0.0,
        entryCount = entries.size
    )
}
