package com.dots.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Scheduling strategy (no services running in the background):
 *
 *  • MIDNIGHT – a one-time job at 00:01 local time. The worker re-schedules the next one,
 *    so the dot fills right after the day changes.
 *  • SAFETY_NET – a 6-hourly periodic job in case the midnight job was deferred by Doze,
 *    or the device was off. The worker exits immediately when nothing changed.
 *  • NOW – one-off refresh on app start, reboot, time or timezone change.
 *
 * WorkManager persists all of it across reboots by itself.
 */
class UpdateScheduler(private val context: Context) {

    private val wm get() = WorkManager.getInstance(context)

    private val constraints = Constraints.Builder().setRequiresBatteryNotLow(false).build()

    fun schedule() {
        scheduleMidnight()
        val periodic = PeriodicWorkRequestBuilder<WallpaperUpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(SAFETY_NET, ExistingPeriodicWorkPolicy.KEEP, periodic)
    }

    fun scheduleMidnight() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val next = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(1)
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<WallpaperUpdateWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .addTag(MIDNIGHT)
            .build()
        wm.enqueueUniqueWork(MIDNIGHT, ExistingWorkPolicy.REPLACE, request)
    }

    /** Immediate refresh, e.g. after a timezone change; also re-aligns the midnight job. */
    fun refreshNow() {
        wm.enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WallpaperUpdateWorker>().build())
        scheduleMidnight()
    }

    companion object {
        const val MIDNIGHT = "dots_midnight_update"
        const val SAFETY_NET = "dots_periodic_update"
        const val NOW = "dots_update_now"
    }
}
