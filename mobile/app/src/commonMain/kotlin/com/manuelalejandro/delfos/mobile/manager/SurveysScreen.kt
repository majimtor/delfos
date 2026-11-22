package com.manuelalejandro.delfos.mobile.manager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manuelalejandro.delfos.mobile.ResultsRoute
import com.manuelalejandro.delfos.mobile.api.Survey
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.answer_back
import com.manuelalejandro.delfos.mobile.resources.answer_retry
import com.manuelalejandro.delfos.mobile.resources.surveys_empty
import com.manuelalejandro.delfos.mobile.resources.surveys_load_failed
import com.manuelalejandro.delfos.mobile.resources.surveys_load_more
import com.manuelalejandro.delfos.mobile.resources.surveys_logout
import com.manuelalejandro.delfos.mobile.resources.surveys_not_published
import com.manuelalejandro.delfos.mobile.resources.surveys_open_results
import com.manuelalejandro.delfos.mobile.resources.surveys_responses
import com.manuelalejandro.delfos.mobile.resources.surveys_status_closed
import com.manuelalejandro.delfos.mobile.resources.surveys_status_draft
import com.manuelalejandro.delfos.mobile.resources.surveys_status_published
import com.manuelalejandro.delfos.mobile.resources.surveys_title
import com.manuelalejandro.delfos.mobile.session.SessionManager
import com.manuelalejandro.delfos.mobile.session.SessionState
import com.manuelalejandro.delfos.mobile.ui.BackArrow
import com.manuelalejandro.delfos.mobile.ui.delfosColors
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The manager's surveys; a published one opens its results. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SurveysScreen(
    viewModel: SurveysViewModel,
    session: SessionManager,
    onBack: () -> Unit,
    onOpenResults: (ResultsRoute) -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val sessionState by session.state.collectAsState()
    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.SignedOut) onSignedOut()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.surveys_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(BackArrow, contentDescription = stringResource(Res.string.answer_back)) }
                },
                actions = {
                    TextButton(onClick = viewModel::logout) { Text(stringResource(Res.string.surveys_logout)) }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Centered(padding) { CircularProgressIndicator() }
            state.failed && state.surveys.isEmpty() -> Centered(padding) {
                Text(stringResource(Res.string.surveys_load_failed), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = viewModel::load) { Text(stringResource(Res.string.answer_retry)) }
            }
            state.surveys.isEmpty() -> Centered(padding) { Text(stringResource(Res.string.surveys_empty), textAlign = TextAlign.Center) }
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.surveys, key = { it.id }) { survey -> SurveyCard(survey, onOpenResults) }
                if (state.nextCursor != null) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = viewModel::loadMore, enabled = !state.loadingMore) {
                                Text(stringResource(Res.string.surveys_load_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One survey: its title, its state as a chip, its figures, and the way to its results when it has some. */
@Composable
private fun SurveyCard(survey: Survey, onOpenResults: (ResultsRoute) -> Unit) {
    val version = survey.publishedVersionNumber
    val responses = pluralStringResource(Res.plurals.surveys_responses, survey.responseCount, survey.responseCount.toString())
    val openLabel = stringResource(Res.string.surveys_open_results)
    OutlinedCard(
        modifier = if (version != null) {
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = openLabel) {
                onOpenResults(ResultsRoute(survey.id, version, survey.title))
            }
        } else {
            Modifier.fillMaxWidth()
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(survey.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                StatusChip(survey.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(responses, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                if (version != null) openLabel else stringResource(Res.string.surveys_not_published),
                color = if (version != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** The state of a survey in the colours the web gives it: published went well, a draft waits, closed rests. */
@Composable
private fun StatusChip(status: String) {
    val colours = MaterialTheme.colorScheme
    val extra = MaterialTheme.delfosColors
    val (label, container, content) = when (status) {
        "PUBLISHED" -> Triple(stringResource(Res.string.surveys_status_published), extra.successContainer, extra.onSuccessContainer)
        "CLOSED" -> Triple(stringResource(Res.string.surveys_status_closed), colours.surfaceContainerHigh, colours.onSurfaceVariant)
        else -> Triple(stringResource(Res.string.surveys_status_draft), colours.tertiaryContainer, colours.onTertiaryContainer)
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
internal fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        content()
    }
}
