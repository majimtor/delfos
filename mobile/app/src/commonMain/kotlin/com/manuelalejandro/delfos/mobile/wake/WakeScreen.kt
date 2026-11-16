package com.manuelalejandro.delfos.mobile.wake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.answer_retry
import com.manuelalejandro.delfos.mobile.resources.wake_body
import com.manuelalejandro.delfos.mobile.resources.wake_elapsed
import com.manuelalejandro.delfos.mobile.resources.wake_failed
import com.manuelalejandro.delfos.mobile.resources.wake_title
import org.jetbrains.compose.resources.stringResource

/** Shows [content] once the API answers; until then an honest waiting screen, as the web does. */
@Composable
internal fun WakeScreen(viewModel: WakeViewModel, content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        WakeUiState.Ready -> content()
        is WakeUiState.Waking -> Waiting(current)
        WakeUiState.Failed -> Centered {
            Text(
                stringResource(Res.string.wake_failed),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = viewModel::retry) { Text(stringResource(Res.string.answer_retry)) }
        }
    }
}

@Composable
private fun Waiting(state: WakeUiState.Waking) {
    Centered {
        // A plain spinner until the first probe fails; then the explanation, announced once. The counter is visual only.
        if (state.attempts > 0) {
            Text(
                stringResource(Res.string.wake_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.wake_body), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
        CircularProgressIndicator()
        if (state.elapsedSeconds > 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.wake_elapsed, state.elapsedSeconds.toString()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
        }
    }
}
