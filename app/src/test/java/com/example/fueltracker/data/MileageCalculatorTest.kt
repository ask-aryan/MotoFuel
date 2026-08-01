package com.example.fueltracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MileageCalculatorTest {

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
    fun `empty list produces no segments`() {
        assertTrue(computeSegmentEfficiencies(emptyList()).isEmpty())
    }

    @Test
    fun `single full-tank entry produces no segments`() {
        val entries = listOf(entry(odometer = 100.0, fuelAmount = 5.0))
        assertTrue(computeSegmentEfficiencies(entries).isEmpty())
    }

    @Test
    fun `no full-tank entries produces no segments`() {
        val entries = listOf(
            entry(odometer = 100.0, fuelAmount = 5.0, fullTank = false),
            entry(odometer = 200.0, fuelAmount = 5.0, fullTank = false)
        )
        assertTrue(computeSegmentEfficiencies(entries).isEmpty())
    }

    @Test
    fun `two full-tank fills produce one segment with simple distance over fuel`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0),
            entry(id = 2, odometer = 200.0, fuelAmount = 10.0)
        )
        val segments = computeSegmentEfficiencies(entries)
        assertEquals(1, segments.size)
        assertEquals(2, segments[0].first.id)
        assertEquals(20.0, segments[0].second, 0.0001)
    }

    @Test
    fun `partial fill between two full tanks is included in segment fuel total`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0),
            entry(id = 2, odometer = 100.0, fuelAmount = 3.0, fullTank = false),
            entry(id = 3, odometer = 300.0, fuelAmount = 7.0)
        )
        val segments = computeSegmentEfficiencies(entries)
        assertEquals(1, segments.size)
        assertEquals(3, segments[0].first.id)
        // distance 300, fuel = partial(3) + final full(7) = 10
        assertEquals(30.0, segments[0].second, 0.0001)
    }

    @Test
    fun `multiple full-tank fills produce independent consecutive segments`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0),
            entry(id = 2, odometer = 100.0, fuelAmount = 5.0),
            entry(id = 3, odometer = 300.0, fuelAmount = 10.0)
        )
        val segments = computeSegmentEfficiencies(entries)
        assertEquals(2, segments.size)
        assertEquals(2, segments[0].first.id)
        assertEquals(20.0, segments[0].second, 0.0001)
        assertEquals(3, segments[1].first.id)
        assertEquals(20.0, segments[1].second, 0.0001)
    }

    @Test
    fun `entries out of order are sorted by odometer before segmenting`() {
        val entries = listOf(
            entry(id = 2, odometer = 200.0, fuelAmount = 10.0),
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0)
        )
        val segments = computeSegmentEfficiencies(entries)
        assertEquals(1, segments.size)
        assertEquals(2, segments[0].first.id)
        assertEquals(20.0, segments[0].second, 0.0001)
    }

    @Test
    fun `zero distance between full tanks at same odometer produces no segment`() {
        val entries = listOf(
            entry(id = 1, odometer = 100.0, fuelAmount = 5.0),
            entry(id = 2, odometer = 100.0, fuelAmount = 5.0)
        )
        assertTrue(computeSegmentEfficiencies(entries).isEmpty())
    }

    @Test
    fun `full tank entry with zero fuel amount produces no segment for that anchor transition`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 0.0),
            entry(id = 2, odometer = 100.0, fuelAmount = 0.0)
        )
        assertTrue(computeSegmentEfficiencies(entries).isEmpty())
    }

    @Test
    fun `negative odometer values are sorted and segmented like any other reading`() {
        val entries = listOf(
            entry(id = 1, odometer = 0.0, fuelAmount = 5.0),
            entry(id = 2, odometer = -50.0, fuelAmount = 5.0)
        )
        val segments = computeSegmentEfficiencies(entries)
        assertEquals(1, segments.size)
        assertEquals(1, segments[0].first.id)
        // sorted order: -50 (anchor) -> 0 (dist 50, fuel 5)
        assertEquals(10.0, segments[0].second, 0.0001)
    }
}
