package com.example.fueltracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelStatsCalculatorTest {

    private fun entry(
        id: Int = 0,
        odometer: Double,
        fuelAmount: Double,
        fullTank: Boolean = true,
        pricePerLiter: Double = 0.0
    ) = FuelEntry(
        id = id,
        vehicleId = 1,
        odometer = odometer,
        fuelAmount = fuelAmount,
        pricePerLiter = pricePerLiter,
        fullTank = fullTank
    )

    @Test
    fun `empty list produces null stats`() {
        assertNull(computeFuelStats(emptyList()))
    }

    @Test
    fun `single entry produces zeroed distance and efficiency but preserves fuel and cost`() {
        val stats = computeFuelStats(listOf(entry(odometer = 100.0, fuelAmount = 5.0, pricePerLiter = 100.0)))!!
        assertEquals(0.0, stats.avgEfficiency, 0.0001)
        assertEquals(0.0, stats.bestEfficiency, 0.0001)
        assertEquals(0.0, stats.worstEfficiency, 0.0001)
        assertEquals(0.0, stats.totalDistance, 0.0001)
        assertEquals(5.0, stats.totalFuel, 0.0001)
        assertEquals(500.0, stats.totalCost, 0.0001)
        assertEquals(100.0, stats.lastOdometer, 0.0001)
        assertEquals(0.0, stats.costPerKm, 0.0001)
        assertEquals(1, stats.entryCount)
    }

    @Test
    fun `two fills aggregate distance, fuel, cost and efficiency correctly`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0, pricePerLiter = 100.0),
            entry(id = 2, odometer = 200.0, fuelAmount = 10.0, pricePerLiter = 100.0)
        )
        val stats = computeFuelStats(entries)!!
        assertEquals(200.0, stats.totalDistance, 0.0001)
        assertEquals(15.0, stats.totalFuel, 0.0001)
        assertEquals(1500.0, stats.totalCost, 0.0001)
        assertEquals(20.0, stats.avgEfficiency, 0.0001)
        assertEquals(20.0, stats.bestEfficiency, 0.0001)
        assertEquals(20.0, stats.worstEfficiency, 0.0001)
        assertEquals(200.0, stats.lastOdometer, 0.0001)
        assertEquals(7.5, stats.costPerKm, 0.0001)
        assertEquals(2, stats.entryCount)
    }

    @Test
    fun `best and worst efficiency reflect the extremes across multiple segments`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0),
            entry(id = 2, odometer = 100.0, fuelAmount = 10.0),  // segment: 100/10 = 10
            entry(id = 3, odometer = 300.0, fuelAmount = 5.0)    // segment: 200/5 = 40
        )
        val stats = computeFuelStats(entries)!!
        assertEquals(10.0, stats.worstEfficiency, 0.0001)
        assertEquals(40.0, stats.bestEfficiency, 0.0001)
        assertEquals(25.0, stats.avgEfficiency, 0.0001)
    }

    @Test
    fun `no full-tank segments yields zero efficiency but non-zero distance and cost`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0, fullTank = false, pricePerLiter = 100.0),
            entry(id = 2, odometer = 150.0, fuelAmount = 5.0, fullTank = false, pricePerLiter = 100.0)
        )
        val stats = computeFuelStats(entries)!!
        assertEquals(0.0, stats.avgEfficiency, 0.0001)
        assertEquals(0.0, stats.bestEfficiency, 0.0001)
        assertEquals(0.0, stats.worstEfficiency, 0.0001)
        assertEquals(150.0, stats.totalDistance, 0.0001)
        assertEquals(1000.0, stats.totalCost, 0.0001)
        assertEquals(1000.0 / 150.0, stats.costPerKm, 0.0001)
    }

    @Test
    fun `entries out of order are still aggregated using sorted odometer for distance and last odometer`() {
        val entries = listOf(
            entry(id = 2, odometer = 200.0, fuelAmount = 10.0, pricePerLiter = 100.0),
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0, pricePerLiter = 100.0)
        )
        val stats = computeFuelStats(entries)!!
        assertEquals(200.0, stats.totalDistance, 0.0001)
        assertEquals(200.0, stats.lastOdometer, 0.0001)
    }

    @Test
    fun `zero total distance yields zero cost per km instead of dividing by zero`() {
        val entries = listOf(
            entry(id = 1, odometer = 100.0, fuelAmount = 5.0, pricePerLiter = 100.0),
            entry(id = 2, odometer = 100.0, fuelAmount = 5.0, pricePerLiter = 100.0)
        )
        val stats = computeFuelStats(entries)!!
        assertEquals(0.0, stats.totalDistance, 0.0001)
        assertEquals(0.0, stats.costPerKm, 0.0001)
        assertEquals(1000.0, stats.totalCost, 0.0001)
    }
}
