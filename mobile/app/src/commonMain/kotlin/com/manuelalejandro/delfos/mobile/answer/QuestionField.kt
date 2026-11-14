package com.manuelalejandro.delfos.mobile.answer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.answer_required
import com.manuelalejandro.delfos.mobile.resources.error_missing_required
import com.manuelalejandro.delfos.mobile.resources.scale_labels
import org.jetbrains.compose.resources.stringResource

/** One question of the survey, rendered by type with its hint and its errors; same semantics as the web client. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuestionField(
    question: Question,
    value: AnswerValue,
    errors: List<QuestionError>,
    missing: Boolean,
    enabled: Boolean,
    onChange: (AnswerValue) -> Unit,
) {
    val messages = errors.map { errorMessage(it) } +
        (if (missing && errors.isEmpty()) listOf(stringResource(Res.string.error_missing_required)) else emptyList())
    val hint = hintFor(question)
    val hasError = messages.isNotEmpty()
    val message = messages.joinToString(" ")
    val label = if (question.required) "${question.text} ${stringResource(Res.string.answer_required)}" else question.text
    Column(
        modifier = Modifier.fillMaxWidth().semantics { if (hasError) error(message) },
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.padding(top = 8.dp))
        when (question.type) {
            QuestionType.SINGLE_CHOICE -> Column(Modifier.selectableGroup()) {
                question.options.orEmpty().forEach { option ->
                    val selected = value.optionKey == option.key
                    ChoiceRow(
                        modifier = Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton) {
                            onChange(AnswerValue(optionKey = option.key))
                        },
                        label = option.label,
                    ) { RadioButton(selected = selected, onClick = null, enabled = enabled) }
                }
            }
            QuestionType.MULTIPLE_CHOICE -> Column {
                question.options.orEmpty().forEach { option ->
                    val checked = option.key in value.optionKeys
                    ChoiceRow(
                        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox) { on ->
                            onChange(value.copy(optionKeys = if (on) value.optionKeys + option.key else value.optionKeys - option.key))
                        },
                        label = option.label,
                    ) { Checkbox(checked = checked, onCheckedChange = null, enabled = enabled) }
                }
            }
            QuestionType.TEXT -> OutlinedTextField(
                value = value.text,
                onValueChange = { onChange(value.copy(text = it)) },
                singleLine = question.multiline != true,
                minLines = if (question.multiline == true) 3 else 1,
                isError = hasError,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            QuestionType.NUMBER -> OutlinedTextField(
                value = value.numberText,
                onValueChange = { onChange(value.copy(numberText = it)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (question.integerOnly == true) KeyboardType.Number else KeyboardType.Decimal,
                ),
                isError = hasError,
                enabled = enabled,
                modifier = Modifier.widthIn(max = 240.dp),
            )
            QuestionType.SCALE -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.selectableGroup(),
                ) {
                    scaleValues(question).forEach { n ->
                        val selected = value.number == n.toDouble()
                        FilterChip(
                            selected = selected,
                            onClick = { onChange(AnswerValue(numberText = n.toString())) },
                            label = { Text(n.toString()) },
                            enabled = enabled,
                            modifier = Modifier.semantics { role = Role.RadioButton },
                        )
                    }
                }
                val minLabel = question.minLabel
                val maxLabel = question.maxLabel
                if (!minLabel.isNullOrBlank() || !maxLabel.isNullOrBlank()) {
                    Text(
                        stringResource(Res.string.scale_labels, minLabel.orEmpty(), maxLabel.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (hasError) {
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else if (hint != null) {
            Text(
                hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** A choice with a 48 dp touch target where the whole row toggles the control. */
@Composable
private fun ChoiceRow(modifier: Modifier, label: String, control: @Composable () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun scaleValues(question: Question): List<Int> {
    val min = question.effectiveScaleMin.toInt()
    val max = question.max?.toInt() ?: min
    return (min..max).toList()
}
