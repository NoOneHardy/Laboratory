package ch.no1hardy.orbit7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.navigation.Orbit7App
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity. Everything above it is Compose (`docs/05-architecture.md` §3).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash until we know whether to open onboarding or the Bridge, so the first
        // frame is never the wrong screen.
        splash.setKeepOnScreenCondition { viewModel.state.value is MainUiState.Loading }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            Orbit7Theme(reduceEffects = (state as? MainUiState.Ready)?.reduceEffects == true) {
                when (val current = state) {
                    MainUiState.Loading -> Unit
                    is MainUiState.Ready ->
                        Orbit7App(
                            startWithOnboarding = !current.onboardingCompleted,
                            pendingSettlementWeekId = current.pendingSettlementWeekId,
                            deepLink = intent?.getStringExtra(EXTRA_DESTINATION),
                        )
                }
            }
        }
    }

    companion object {
        /** Set by the widget and by notification actions to open a specific screen. */
        const val EXTRA_DESTINATION = "orbit7.destination"
        const val DESTINATION_QUICK_ADD = "quick_add"
        const val DESTINATION_DRAINS = "drains"
    }
}
