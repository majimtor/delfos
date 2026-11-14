package com.manuelalejandro.delfos.mobile.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.manuelalejandro.delfos.mobile.AnswerRoute
import com.manuelalejandro.delfos.mobile.parseSurveyLink
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.home_code_invalid
import com.manuelalejandro.delfos.mobile.resources.home_code_label
import com.manuelalejandro.delfos.mobile.resources.home_demo_hint
import com.manuelalejandro.delfos.mobile.resources.home_intro
import com.manuelalejandro.delfos.mobile.resources.home_open
import com.manuelalejandro.delfos.mobile.resources.home_title
import org.jetbrains.compose.resources.stringResource

/** Where a respondent types the survey code or pastes the link received. */
@Composable
internal fun HomeScreen(onOpen: (AnswerRoute) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val open = {
        val route = parseSurveyLink(input)
        if (route == null) invalid = true else onOpen(route)
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = stringResource(Res.string.home_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.home_intro), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    invalid = false
                },
                label = { Text(stringResource(Res.string.home_code_label)) },
                isError = invalid,
                supportingText = {
                    Text(if (invalid) stringResource(Res.string.home_code_invalid) else stringResource(Res.string.home_demo_hint))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { open() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = open, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.home_open))
            }
        }
    }
}
