package com.dots.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.dots.app.data.ThemePreference
import com.dots.app.domain.model.WallpaperTarget
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiState
import com.dots.app.ui.components.SectionCard
import com.dots.app.ui.components.Segmented
import com.dots.app.ui.components.ToggleRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: UiState, vm: DotsViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s = state.settings
    val context = LocalContext.current
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("Updates") {
                ToggleRow(
                    "Update automatically",
                    s.autoUpdate,
                    vm::setAutoUpdate,
                    "Fills the next dot shortly after midnight. Runs only when something changed, so it uses almost no battery.",
                )
            }
            SectionCard("Apply to") {
                Segmented(WallpaperTarget.entries, s.target, vm::setTarget, { it.title })
            }
            SectionCard("Week starts on") {
                Segmented(listOf(1, 7, 6), state.config.countdown.weekStartsOn, { d ->
                    vm.edit { it.copy(countdown = it.countdown.copy(weekStartsOn = d)) }
                }, { when (it) { 1 -> "Monday"; 7 -> "Sunday"; else -> "Saturday" } })
            }
            SectionCard("Appearance") {
                Segmented(ThemePreference.entries, s.theme, vm::setTheme, { it.name.lowercase().replaceFirstChar(Char::uppercase) })
            }
            SectionCard("About") {
                Text("DOTS $version", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Every dot is a unit of time. Filled dots have passed, empty dots are still ahead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
