package com.manuelalejandro.delfos.mobile.manager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.answer_back
import com.manuelalejandro.delfos.mobile.resources.login_demo_hint
import com.manuelalejandro.delfos.mobile.resources.login_error_account_disabled
import com.manuelalejandro.delfos.mobile.resources.login_error_generic
import com.manuelalejandro.delfos.mobile.resources.login_error_invalid_credentials
import com.manuelalejandro.delfos.mobile.resources.login_error_rate_limited
import com.manuelalejandro.delfos.mobile.resources.login_password
import com.manuelalejandro.delfos.mobile.resources.login_required
import com.manuelalejandro.delfos.mobile.resources.login_submit
import com.manuelalejandro.delfos.mobile.resources.login_title
import com.manuelalejandro.delfos.mobile.resources.login_username
import com.manuelalejandro.delfos.mobile.ui.BackArrow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The manager's credentials; the same wording as the web client. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginScreen(viewModel: LoginViewModel, onBack: () -> Unit, onSignedIn: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.done) {
        if (state.done) onSignedIn()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(BackArrow, contentDescription = stringResource(Res.string.answer_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::setUsername,
                label = { Text(stringResource(Res.string.login_username)) },
                singleLine = true,
                isError = state.missing && state.username.isBlank(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text(stringResource(Res.string.login_password)) },
                singleLine = true,
                isError = state.missing && state.password.isBlank(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.missing) {
                Text(
                    stringResource(Res.string.login_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val error = state.error
            if (error != null) {
                Text(
                    stringResource(error.message),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = viewModel::submit, enabled = !state.submitting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.login_submit))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.login_demo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val LoginError.message: StringResource
    get() = when (this) {
        LoginError.INVALID_CREDENTIALS -> Res.string.login_error_invalid_credentials
        LoginError.ACCOUNT_DISABLED -> Res.string.login_error_account_disabled
        LoginError.RATE_LIMITED -> Res.string.login_error_rate_limited
        LoginError.GENERIC -> Res.string.login_error_generic
    }
