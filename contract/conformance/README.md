# Conformance suite for visibility rules

These JSON files are the oracle for every implementation of the rules
evaluator and the definition validator: the server, the shared Kotlin module
and any client that evaluates locally. An implementation conforms when it
produces the expected output for every case in every file. The suite is
versioned with the contract; a change in semantics is a change here first.

## Files

| File | Covers |
| --- | --- |
| `01-visibility-basics.json` | `ALL` and `ANY`, chained rules, hidden questions as unanswered, required only when visible |
| `02-operators-choice.json` | Every operator on `SINGLE_CHOICE` and `MULTIPLE_CHOICE` sources |
| `03-operators-number-scale.json` | Every operator on `NUMBER` and `SCALE` sources, `IS_ANSWERED` on `TEXT`, numeric answer checks |
| `04-answer-validation.json` | Shape and limit errors of answers, unknown and duplicate questions, missing required answers |
| `05-definition-validation.json` | Errors of a draft definition, on save and on publish |

## Format

Evaluation files hold one `definition` (the `questions` and `rules` of a
version, as in the `DraftDefinition` schema of `openapi.yaml`) and a list
of `cases`. Each case gives `answers` (as in the `Answer` schema) and the
`expected` result, with the three fields of `EvaluationResult`:
`visibleQuestionKeys` in survey order, `errors`, and
`missingRequiredQuestionKeys` in survey order.

The definition-validation file holds `cases` with their own `definition`,
a `mode` (`save`, the default, or `publish`) and `expectedErrors`.

Errors are compared as a set: order does not matter, and every expected
error must appear exactly once with the same `path` and `code`. When an
expected error lists `params`, the implementation must return at least
those entries with the same values; it may add more. Paths use zero-based
indexes; positions inside `params` are one-based.

## Semantics fixed by the suite

- Questions are evaluated in survey order. A question without a rule is
  visible. A question with a rule is visible when its conditions hold under
  `ALL` or `ANY`.
- A condition reads the answer of its source question only when that
  question is visible and its answer is valid; otherwise the source counts
  as unanswered. Every comparison on an unanswered source is false, including
  `NOT_EQUALS` and `EXCLUDES`; `IS_NOT_ANSWERED` is true.
- An answer to a hidden question yields `HIDDEN_QUESTION_ANSWERED` and no
  other error for that answer, and it never influences a rule.
- An answer to a visible question is checked for shape and limits; an
  invalid answer is reported and treated as unanswered by the rules.
- A visible required question without a valid answer is listed in
  `missingRequiredQuestionKeys`, even when it also has an error.
- Text length is counted in Unicode code points. `integerOnly` numbers and
  `SCALE` values must have no fractional part; `17.0` is an integer.
- `MULTIPLE_CHOICE` selection counts are checked on distinct options after
  unknown and duplicate options have been reported.
