package com.manuelalejandro.delfos.mobile.answer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manuelalejandro.delfos.mobile.AnswerRoute
import com.manuelalejandro.delfos.mobile.api.PublicApi
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.answer_another
import com.manuelalejandro.delfos.mobile.resources.answer_back
import com.manuelalejandro.delfos.mobile.resources.answer_closed
import com.manuelalejandro.delfos.mobile.resources.answer_failed
import com.manuelalejandro.delfos.mobile.resources.answer_invitation_invalid
import com.manuelalejandro.delfos.mobile.resources.answer_invitation_required
import com.manuelalejandro.delfos.mobile.resources.answer_invitation_used
import com.manuelalejandro.delfos.mobile.resources.answer_loading
import com.manuelalejandro.delfos.mobile.resources.answer_not_found
import com.manuelalejandro.delfos.mobile.resources.answer_retry
import com.manuelalejandro.delfos.mobile.resources.answer_submit
import com.manuelalejandro.delfos.mobile.resources.answer_submit_failed
import com.manuelalejandro.delfos.mobile.resources.answer_submitting
import com.manuelalejandro.delfos.mobile.resources.answer_thanks_body
import com.manuelalejandro.delfos.mobile.resources.answer_thanks_title
import com.manuelalejandro.delfos.mobile.resources.home_title
import com.manuelalejandro.delfos.mobile.ui.BackArrow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The answer flow of one survey: loading, the form, and what the server replied. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnswerScreen(
    route: AnswerRoute,
    api: PublicApi,
    onBack: () -> Unit,
    onAnswerAnother: () -> Unit,
) {
    val viewModel = viewModel(key = "answer/${route.publicId}/${route.invitation}") {
        AnswerViewModel(api, route.publicId, route.invitation)
    }
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(BackArrow, contentDescription = stringResource(Res.string.answer_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                AnswerUiState.Loading -> Centered {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(Res.string.answer_loading), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                AnswerUiState.NotFound -> Centered { Text(stringResource(Res.string.answer_not_found), textAlign = TextAlign.Center) }
                AnswerUiState.LoadFailed -> Centered {
                    Text(stringResource(Res.string.answer_failed), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = viewModel::load) { Text(stringResource(Res.string.answer_retry)) }
                }
                is AnswerUiState.Refused -> Centered { Text(stringResource(current.reason.message), textAlign = TextAlign.Center) }
                AnswerUiState.Done -> Centered {
                    Text(
                        stringResource(Res.string.answer_thanks_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.answer_thanks_body), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onAnswerAnother) { Text(stringResource(Res.string.answer_another)) }
                }
                is AnswerUiState.Form -> SurveyForm(current, onChange = viewModel::change, onSubmit = viewModel::submit)
            }
        }
    }
}

private val Refusal.message: StringResource
    get() = when (this) {
        Refusal.INVITATION_REQUIRED -> Res.string.answer_invitation_required
        Refusal.INVITATION_INVALID -> Res.string.answer_invitation_invalid
        Refusal.INVITATION_USED -> Res.string.answer_invitation_used
    }

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun SurveyForm(
    state: AnswerUiState.Form,
    onChange: (String, AnswerValue) -> Unit,
    onSubmit: () -> Unit,
) {
    val survey = state.survey
    val enabled = !state.closed && !state.submitting
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            survey.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (!survey.description.isNullOrBlank()) {
            Text(
                survey.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (state.closed) Notice(stringResource(Res.string.answer_closed), error = false)
        if (state.sendFailed) Notice(stringResource(Res.string.answer_submit_failed), error = true)
        Spacer(Modifier.height(24.dp))
        for (question in state.evaluation.visible) {
            QuestionField(
                question = question,
                value = state.values[question.key] ?: AnswerValue(),
                errors = state.errorsOf(question.key),
                missing = question.key in state.missing,
                enabled = enabled,
                onChange = { onChange(question.key, it) },
            )
            Spacer(Modifier.height(24.dp))
        }
        Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.submitting) Res.string.answer_submitting else Res.string.answer_submit))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Notice(text: String, error: Boolean) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(
            text,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
