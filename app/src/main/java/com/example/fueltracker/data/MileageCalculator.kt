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
