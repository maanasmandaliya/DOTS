package com.dots.app

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiEvent
import com.dots.app.ui.UiState
import com.dots.app.ui.crop.CropScreen
import com.dots.app.ui.dashboard.DashboardScreen
import com.dots.app.ui.editor.EditorScreen
import com.dots.app.ui.history.HistoryScreen
import com.dots.app.ui.settings.SettingsScreen
import com.dots.app.ui.theme.DotsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: DotsViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            DotsTheme(state.settings.theme) {
                DotsApp(vm, state)
            }
        }
    }
}

@Composable
private fun DotsApp(vm: DotsViewModel, state: UiState) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Recalculate the date (and catch up the wallpaper) every time the app comes forward.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResume() }

    LaunchedEffect(vm) {
        vm.uiEvents.collect { e ->
            when (e) {
                is UiEvent.Message -> snackbar.showSnackbar(e.text)
                is UiEvent.Launch -> try {
                    context.startActivity(e.intent)
                } catch (_: ActivityNotFoundException) {
                    snackbar.showSnackbar("No app available to share with")
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.loaded) {
            NavHost(
                navController = nav,
                startDestination = "dashboard",
                enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        state, vm, snackbar,
                        onCustomize = { tab -> nav.navigate("editor/$tab") },
                        onHistory = { nav.navigate("history") },
                        onSettings = { nav.navigate("settings") },
                    )
                }
                composable("editor/{tab}", arguments = listOf(navArgument("tab") { type = NavType.IntType })) { entry ->
                    EditorScreen(
                        state, vm, snackbar,
                        initialTab = entry.arguments?.getInt("tab") ?: 1,
                        onBack = { nav.popBackStack() },
                        onCrop = { nav.navigate("crop") },
                    )
                }
                composable("crop") { CropScreen(state, vm, onDone = { nav.popBackStack() }) }
                composable("history") { HistoryScreen(state, vm, snackbar, onBack = { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(state, vm, snackbar, onBack = { nav.popBackStack() }) }
            }
        }

        state.busy?.let { label ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    // Swallow touches while a long operation runs.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(36.dp), color = Color.White, strokeWidth = 2.dp)
                    Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
