package com.dots.app

import com.dots.app.domain.countdown.CountdownCalculator
import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.CountdownSettings
import com.dots.app.domain.model.TimeUnitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CountdownCalculatorTest {

    @Test fun `year has 365 dots in a common year and 366 in a leap year`() {
        assertEquals(365, CountdownCalculator.year(LocalDate.of(2026, 6, 1)).total)
        assertEquals(366, CountdownCalculator.year(LocalDate.of(2028, 6, 1)).total)
        assertEquals(366, CountdownCalculator.year(LocalDate.of(2000, 1, 1)).total)
        assertEquals(365, CountdownCalculator.year(LocalDate.of(2100, 1, 1)).total)
    }

    @Test fun `year boundaries`() {
        val first = CountdownCalculator.year(LocalDate.of(2026, 1, 1))
        assertEquals(0, first.elapsed)
        assertEquals(0, first.currentIndex)
        val last = CountdownCalculator.year(LocalDate.of(2028, 12, 31))
        assertEquals(365, last.elapsed)
        assertEquals(365, last.currentIndex)
        assertEquals(1, last.remaining)
    }

    @Test fun `leap day is counted`() {
        val p = CountdownCalculator.year(LocalDate.of(2028, 3, 1))
        assertEquals(31 + 29, p.elapsed)
    }

    @Test fun `month lengths`() {
        assertEquals(28, CountdownCalculator.month(LocalDate.of(2026, 2, 10)).total)
        assertEquals(29, CountdownCalculator.month(LocalDate.of(2028, 2, 10)).total)
        assertEquals(30, CountdownCalculator.month(LocalDate.of(2026, 4, 30)).total)
        assertEquals(31, CountdownCalculator.month(LocalDate.of(2026, 12, 31)).total)
        assertEquals(30, CountdownCalculator.month(LocalDate.of(2026, 12, 31)).elapsed)
    }

    @Test fun `week respects first day of week`() {
        val thursday = LocalDate.of(2026, 11, 5)
        assertEquals(3, CountdownCalculator.week(thursday, 1).elapsed) // Mon start
        assertEquals(4, CountdownCalculator.week(thursday, 7).elapsed) // Sun start
        assertEquals(0, CountdownCalculator.week(LocalDate.of(2026, 11, 2), 1).elapsed)
        assertEquals(6, CountdownCalculator.week(LocalDate.of(2026, 11, 8), 1).elapsed)
    }

    @Test fun `life calendar in weeks`() {
        val birth = LocalDate.of(2000, 1, 1)
        val p = CountdownCalculator.life(LocalDate.of(2000, 1, 15), birth, 80)
        assertEquals(2, p.elapsed)
        assertEquals(Math.ceil(java.time.temporal.ChronoUnit.DAYS.between(birth, birth.plusYears(80)) / 7.0).toInt(), p.total)
        assertTrue(p.total in 4170..4176)
    }

    @Test fun `life not configured without birth date or future birth date`() {
        assertFalse(CountdownCalculator.life(LocalDate.of(2026, 1, 1), null, 80).isConfigured)
        assertFalse(CountdownCalculator.life(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 80).isConfigured)
    }

    @Test fun `life past expectancy is complete`() {
        val p = CountdownCalculator.life(LocalDate.of(2100, 1, 1), LocalDate.of(1990, 1, 1), 80)
        assertEquals(p.total, p.elapsed)
        assertEquals(0, p.remaining)
        assertEquals(-1, p.currentIndex)
    }

    @Test fun `custom goal inclusive days and clamping`() {
        val start = LocalDate.of(2026, 11, 1)
        val end = LocalDate.of(2026, 11, 10)
        val mid = CountdownCalculator.custom(LocalDate.of(2026, 11, 5), start, end, TimeUnitKind.DAY, "Trip")
        assertEquals(10, mid.total)
        assertEquals(4, mid.elapsed)
        val before = CountdownCalculator.custom(LocalDate.of(2026, 10, 1), start, end, TimeUnitKind.DAY, "Trip")
        assertEquals(0, before.elapsed)
        val after = CountdownCalculator.custom(LocalDate.of(2027, 1, 1), start, end, TimeUnitKind.DAY, "Trip")
        assertEquals(10, after.elapsed)
        val weeks = CountdownCalculator.custom(LocalDate.of(2026, 11, 9), start, end, TimeUnitKind.WEEK, "Trip")
        assertEquals(2, weeks.total)
        assertEquals(1, weeks.elapsed)
        val weeksDone = CountdownCalculator.custom(LocalDate.of(2027, 1, 1), start, end, TimeUnitKind.WEEK, "Trip")
        assertEquals(weeksDone.total, weeksDone.elapsed)
    }

    @Test fun `custom with invalid range is not configured`() {
        val d = LocalDate.of(2026, 1, 1)
        assertFalse(CountdownCalculator.custom(d, d, d, TimeUnitKind.DAY, "x").isConfigured)
        assertFalse(CountdownCalculator.custom(d, null, d, TimeUnitKind.DAY, "x").isConfigured)
    }

    @Test fun `settings dispatch and bad dates`() {
        val s = CountdownSettings(mode = CountdownMode.LIFE, birthDate = "not-a-date")
        assertFalse(CountdownCalculator.calculate(s, LocalDate.of(2026, 1, 1)).isConfigured)
        val y = CountdownCalculator.calculate(CountdownSettings(), LocalDate.of(2026, 11, 5))
        assertEquals(CountdownMode.YEAR, y.mode)
        assertEquals(308, y.elapsed)
    }

    @Test fun `render key changes only when the day changes`() {
        val a = CountdownCalculator.year(LocalDate.of(2026, 5, 5)).renderKey
        val b = CountdownCalculator.year(LocalDate.of(2026, 5, 5)).renderKey
        val c = CountdownCalculator.year(LocalDate.of(2026, 5, 6)).renderKey
        assertEquals(a, b)
        assertTrue(a != c)
    }
}
