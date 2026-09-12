package ch.no1hardy.orbit7.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.R
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The debug launcher: the component gallery and the time-travel screen, side by side.
 *
 * It is a separate activity in the debug source set rather than a hidden route in the app, so there
 * is no way for any of it to reach a release build.
 */
@AndroidEntryPoint
class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Orbit7Theme {
                DebugHome()
            }
        }
    }
}

private enum class DebugTab {
    GALLERY,
    TIME_TRAVEL,
}

@Composable
private fun DebugHome() {
    var tab by rememberSaveable { mutableStateOf(DebugTab.GALLERY) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { tab = DebugTab.GALLERY }) {
                Text(stringResource(R.string.debug_gallery))
            }
            TextButton(onClick = { tab = DebugTab.TIME_TRAVEL }) {
                Text(stringResource(R.string.debug_title))
            }
        }
        when (tab) {
            DebugTab.GALLERY -> ComponentGalleryScreen()
            DebugTab.TIME_TRAVEL -> TimeTravelScreen()
        }
    }
}
