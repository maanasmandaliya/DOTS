package com.dots.app.domain.countdown

import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.CountdownSettings
import com.dots.app.domain.model.TimeUnitKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Result of a countdown calculation.
 *
 * Dots are indexed 0 until [total]. Dots with index < [elapsed] are fully elapsed (filled),
 * the dot at index [currentIndex] is the unit in progress (today / this week) and is
 * highlighted, everything after it is remaining.
 */
data class CountdownProgress(
    val mode: CountdownMode,
    val total: Int,
    val elapsed: Int,
    val unitLabel: String,
    val title: String,
    /** Precise progress 0..1 (includes the fraction of the current unit for LIFE/CUSTOM-week). */
    val fraction: Double,
    val isConfigured: Boolean = true,
) {
    val remaining: Int get() = (total - elapsed).coerceAtLeast(0)
    val currentIndex: Int get() = if (elapsed in 0 until total) elapsed else -1
    val percent: Double get() = fraction * 100.0

    /** Stable key: if this is unchanged, the rendered wallpaper is unchanged. */
    val renderKey: String get() = "$mode:$total:$elapsed"
}

/**
 * Pure date math for all countdown modes. All calculations happen on [LocalDate]s in the
 * caller's zone, so they are immune to DST and only depend on the local calendar day —
 * a timezone change simply changes which "today" is passed in.
 */
object CountdownCalculator {

    fun calculate(settings: CountdownSettings, today: LocalDate): CountdownProgress = when (settings.mode) {
        CountdownMode.YEAR -> year(today)
        CountdownMode.MONTH -> month(today)
        CountdownMode.WEEK -> week(today, settings.weekStartsOn)
        CountdownMode.LIFE -> life(today, parse(settings.birthDate), settings.lifeExpectancyYears)
        CountdownMode.CUSTOM -> custom(
            today,
            parse(settings.customStart),
            parse(settings.customEnd),
            settings.customUnit,
            settings.customTitle,
        )
    }

    fun year(today: LocalDate): CountdownProgress {
        val total = today.lengthOfYear() // 365 or 366
        val elapsed = today.dayOfYear - 1
        return CountdownProgress(
            mode = CountdownMode.YEAR,
            total = total,
            elapsed = elapsed,
            unitLabel = "days",
            title = today.year.toString(),
            fraction = elapsed.toDouble() / total,
        )
    }

    fun month(today: LocalDate): CountdownProgress {
        val total = today.lengthOfMonth() // 28..31
        val elapsed = today.dayOfMonth - 1
        return CountdownProgress(
            mode = CountdownMode.MONTH,
            total = total,
            elapsed = elapsed,
            unitLabel = "days",
            title = today.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
            fraction = elapsed.toDouble() / total,
        )
    }

    fun week(today: LocalDate, weekStartsOn: Int): CountdownProgress {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(weekStartsOn.coerceIn(1, 7))))
        val elapsed = ChronoUnit.DAYS.between(start, today).toInt()
        return CountdownProgress(
            mode = CountdownMode.WEEK,
            total = 7,
            elapsed = elapsed,
            unitLabel = "days",
            title = "This week",
            fraction = elapsed / 7.0,
        )
    }

    fun life(today: LocalDate, birth: LocalDate?, expectancyYears: Int): CountdownProgress {
        if (birth == null || birth.isAfter(today)) {
            return CountdownProgress(CountdownMode.LIFE, 0, 0, "weeks", "Life", 0.0, isConfigured = false)
        }
        val end = birth.plusYears(expectancyYears.coerceIn(1, 150).toLong())
        val totalDays = ChronoUnit.DAYS.between(birth, end)
        val total = Math.ceil(totalDays / 7.0).toInt()
        val livedDays = ChronoUnit.DAYS.between(birth, today).coerceAtMost(totalDays)
        // total rounds the final partial week up, so a completed span must fill every dot.
        val elapsed = if (livedDays >= totalDays) total else (livedDays / 7).toInt()
        return CountdownProgress(
            mode = CountdownMode.LIFE,
            total = total,
            elapsed = elapsed,
            unitLabel = "weeks",
            title = "Life",
            fraction = livedDays.toDouble() / totalDays,
        )
    }

    fun custom(
        today: LocalDate,
        start: LocalDate?,
        end: LocalDate?,
        unit: TimeUnitKind,
        title: String,
    ): CountdownProgress {
        if (start == null || end == null || !end.isAfter(start)) {
            return CountdownProgress(CountdownMode.CUSTOM, 0, 0, "days", title, 0.0, isConfigured = false)
        }
        // The goal day itself is the last dot, so the span is inclusive.
        val spanDays = ChronoUnit.DAYS.between(start, end) + 1
        val passedDays = ChronoUnit.DAYS.between(start, today).coerceIn(0, spanDays)
        val (total, elapsed, label) = when (unit) {
            TimeUnitKind.DAY -> Triple(spanDays.toInt(), passedDays.toInt(), "days")
            TimeUnitKind.WEEK -> Triple(Math.ceil(spanDays / 7.0).toInt(), (passedDays / 7).toInt(), "weeks")
        }
        return CountdownProgress(
            mode = CountdownMode.CUSTOM,
            total = total,
            elapsed = if (passedDays >= spanDays) total else elapsed,
            unitLabel = label,
            title = title.ifBlank { "Goal" },
            fraction = passedDays.toDouble() / spanDays,
        )
    }

    fun parse(iso: String?): LocalDate? = try {
        iso?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) {
        null
    }
}
