package com.manuelalejandro.delfos.mobile.manager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.mobile.answer.display
import com.manuelalejandro.delfos.mobile.api.NumberStats
import com.manuelalejandro.delfos.mobile.api.QuestionResult
import com.manuelalejandro.delfos.mobile.api.VersionResults
import com.manuelalejandro.delfos.mobile.platform.formatTime
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.answer_back
import com.manuelalejandro.delfos.mobile.resources.answer_retry
import com.manuelalejandro.delfos.mobile.resources.results_answers
import com.manuelalejandro.delfos.mobile.resources.results_load_failed
import com.manuelalejandro.delfos.mobile.resources.results_max
import com.manuelalejandro.delfos.mobile.resources.results_mean
import com.manuelalejandro.delfos.mobile.resources.results_min
import com.manuelalejandro.delfos.mobile.resources.results_refresh
import com.manuelalejandro.delfos.mobile.resources.results_responses
import com.manuelalejandro.delfos.mobile.resources.results_updated
import com.manuelalejandro.delfos.mobile.resources.results_version
import com.manuelalejandro.delfos.mobile.session.SessionManager
import com.manuelalejandro.delfos.mobile.session.SessionState
import com.manuelalejandro.delfos.mobile.ui.BackArrow
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The aggregates of one published version, question by question. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultsScreen(
    viewModel: ResultsViewModel,
    session: SessionManager,
    title: String,
    onBack: () -> Unit,
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
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(BackArrow, contentDescription = stringResource(Res.string.answer_back)) }
                },
                actions = {
                    val loaded = state as? ResultsUiState.Loaded
                    TextButton(onClick = viewModel::refresh, enabled = loaded != null && !loaded.refreshing) {
                        Text(stringResource(Res.string.results_refresh))
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            ResultsUiState.Loading -> Centered(padding) { CircularProgressIndicator() }
            ResultsUiState.Failed -> Centered(padding) {
                Text(stringResource(Res.string.results_load_failed), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = viewModel::load) { Text(stringResource(Res.string.answer_retry)) }
            }
            is ResultsUiState.Loaded -> Results(current.results, padding)
        }
    }
}

@Composable
private fun Results(results: VersionResults, padding: androidx.compose.foundation.layout.PaddingValues) {
    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    stringResource(Res.string.results_version, results.versionNumber.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                val responses = pluralStringResource(Res.plurals.results_responses, results.responseCount, results.responseCount.toString())
                Text(
                    "$responses · ${stringResource(Res.string.results_updated, formatTime(results.computedAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(results.questions, key = { it.key }) { question ->
            QuestionCard(question)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuestionCard(question: QuestionResult) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(question.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(
                pluralStringResource(Res.plurals.results_answers, question.answerCount, question.answerCount.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (question.type) {
                QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE -> question.options.orEmpty().forEach { option ->
                    CountBar(option.label, option.count, question.answerCount)
                }
                QuestionType.NUMBER -> question.number?.let { Stats(it) }
                QuestionType.SCALE -> {
                    question.distribution.orEmpty().forEach { value -> CountBar(value.value.toString(), value.count, question.answerCount) }
                    question.number?.let { Stats(it) }
                }
                QuestionType.TEXT -> Unit
            }
        }
    }
}

/** One count with its share of the answers as a bar; the bar repeats the numbers, so it stays out of the accessibility tree. */
@Composable
private fun CountBar(label: String, count: Int, total: Int) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f))
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { if (total > 0) count.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clearAndSetSemantics { },
        )
    }
}

@Composable
private fun Stats(stats: NumberStats) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Stat(stringResource(Res.string.results_mean), stats.mean.rounded(), Modifier.weight(1f))
        Stat(stringResource(Res.string.results_min), stats.min.display(), Modifier.weight(1f))
        Stat(stringResource(Res.string.results_max), stats.max.display(), Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** A mean with two decimals at most. */
private fun Double.rounded(): String = ((this * 100).roundToLong() / 100.0).display()
