package com.dots.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dots.app.container

/**
 * Timezone / clock changes shift which calendar day it is, and after a reboot the midnight
 * job should be re-aligned. These broadcasts are exempt from implicit-broadcast limits.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> context.container.scheduler.refreshNow()
        }
    }
}
