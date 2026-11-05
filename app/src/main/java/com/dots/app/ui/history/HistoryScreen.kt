package com.dots.app.ui.history

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dots.app.domain.model.HistoryEntry
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiState
import com.dots.app.ui.components.Caption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: UiState, vm: DotsViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<HistoryEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val history = state.settings.history

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (history.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Clear") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Wallpapers you apply will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(history, key = { it.id }) { entry ->
                    Column(Modifier.clickable { selected = entry }) {
                        val thumb by produceState<ImageBitmap?>(null, entry.thumbnailPath) {
                            value = withContext(Dispatchers.IO) {
                                entry.thumbnailPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(state.screenAspect)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            thumb?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Caption(
                            Instant.ofEpochMilli(entry.appliedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        )
                        Text(entry.config.countdown.mode.title, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Use this wallpaper again?") },
            text = { Text("${entry.summary}\n\nIts design will be loaded and applied with today's progress.") },
            confirmButton = { TextButton(onClick = { vm.restoreHistory(entry); selected = null }) { Text("Apply") } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel") } },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes the list of past wallpapers. Your current wallpaper is not affected.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
