package com.dots.app

import android.content.Context
import com.dots.app.data.ImageRepository
import com.dots.app.data.SettingsRepository
import com.dots.app.data.WallpaperExporter
import com.dots.app.data.WallpaperGenerator
import com.dots.app.data.WallpaperUpdater
import com.dots.app.work.UpdateScheduler

/** Manual dependency container; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val settings = SettingsRepository(app)
    val images = ImageRepository(app)
    val generator = WallpaperGenerator(app, images)
    val exporter = WallpaperExporter(app)
    val updater = WallpaperUpdater(settings, generator, exporter, images)
    val scheduler = UpdateScheduler(app)
}
