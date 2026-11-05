package com.dots.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dots.app.container
import com.dots.app.data.WallpaperException

/**
 * Recalculates the countdown and re-applies the wallpaper when a new dot needs filling.
 * All of the user's design (image, colours, opacity, size, position) comes from the
 * persisted applied config, so it is preserved across updates.
 */
class WallpaperUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext.container
        return try {
            val changed = c.updater.refreshIfNeeded()
            Log.d(TAG, "Wallpaper refresh: ${if (changed) "updated" else "unchanged"}")
            if (tags.contains(UpdateScheduler.MIDNIGHT)) c.scheduler.scheduleMidnight()
            Result.success()
        } catch (e: WallpaperException) {
            Log.w(TAG, "Wallpaper refresh failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected refresh error", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val TAG = "DotsWorker"
    }
}
