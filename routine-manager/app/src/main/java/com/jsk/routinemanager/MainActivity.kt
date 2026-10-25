package com.jsk.routinemanager

import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jsk.routinemanager.debug.DebugEntry
import com.jsk.routinemanager.screen.ScreenPolicy
import com.jsk.routinemanager.ui.MainViewModel
import com.jsk.routinemanager.ui.Screen
import com.jsk.routinemanager.ui.home.HomeScreen
import com.jsk.routinemanager.ui.theme.RoutineManagerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val screenPolicy = ScreenPolicy(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Draw under the notch in every orientation; screens pad that side themselves (SPEC D18).
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        // Bars hidden on every screen; an edge swipe shows them briefly (SPEC D3).
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        // Keep-screen-on and the Bedtime dim, checked at every minute tick while visible (SPEC §3.2, §3.3).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.routine.collect(screenPolicy::update)
            }
        }
        val container = (application as RoutineApp).container
        setContent {
            val dark by viewModel.darkTheme.collectAsStateWithLifecycle()
            RoutineManagerTheme(dark = dark) {
                val screen by viewModel.screen.collectAsStateWithLifecycle()
                when (screen) {
                    Screen.Home -> HomeScreen(
                        viewModel.dashboard,
                        onSettings = {}, // Settings comes in M8.
                        onDebug = if (DebugEntry.AVAILABLE) ({ viewModel.show(Screen.Debug) }) else null,
                    )
                    Screen.Debug -> DebugEntry.Screen(container, onBack = { viewModel.show(Screen.Home) })
                }
            }
        }
    }

    /** The touch that wakes the dimmed Bedtime screen does nothing else (SPEC §3.2). */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean =
        screenPolicy.swallows(ev) || super.dispatchTouchEvent(ev)

    override fun onStop() {
        super.onStop()
        screenPolicy.onHidden()
        // Pending changes are written when the app goes to the background (SPEC §3.5).
        (application as RoutineApp).container.repository.flush()
    }
}
