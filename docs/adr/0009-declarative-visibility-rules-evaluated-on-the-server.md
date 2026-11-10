# 0009. Declarative visibility rules evaluated on the server

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

Conditional logic between questions is the product requirement that makes
Delfos more than a form builder. The decision has two parts: the model of a
rule, which becomes part of the contract and of every stored survey and is
therefore expensive to change, and where rules are evaluated, which can
change per client. Offline response is not required at launch.

## Decision

A question has at most one visibility rule: a list of conditions over
earlier questions, combined with `ALL` or `ANY`, each condition being a
source question, an operator valid for that question's type and a typed
value. A question with a rule is visible only when the rule holds; a
question without a rule is always visible. Rules may reference only
questions that precede their target, which makes cycles impossible by
construction and reduces validation to broken references, forward
references, operator and value type checks. The rule schema is part of the
contract.

The server always evaluates: when validating a submitted response, and
through a stateless evaluation endpoint that takes a version and partial
answers and returns the visible questions and accumulated validation
errors. The web client calls that endpoint as answers change. The evaluator
lives in a pure Kotlin module shared with the mobile client, which evaluates
locally; a conformance suite of JSON cases (definition, answers, expected
visibility and errors) is kept with the contract and run by every
implementation. Answers to hidden questions are rejected, not ignored.

## Alternatives considered

- Evaluation only in clients. Faster interaction, and the server would
  have to trust the client's idea of what was visible; rejected on
  principle.
- Show and hide actions with several rules per question, or skip-to-question
  jumps. More expressive and harder to validate and to render in an editor;
  a jump backwards would reintroduce cycles. A skip is expressed as
  visibility rules on the skipped questions.
- A generic expression language (JSON Logic, CEL). Maximum power, hardest
  to validate, to explain in an editor and to keep consistent across
  implementations.
- One implementation per client in each client's language from the first
  slice. Three evaluators to keep honest; the shared module and the
  server endpoint avoid two of them until a real need appears.

## Consequences

- Positive: cycles cannot exist; validation errors are precise and listed
  together; one implementation serves server and Android; the web pays
  only a request per change.
- Negative: no offline response on the web; nested condition groups and
  cross-question arithmetic are not expressible; the evaluation endpoint is
  a public surface that needs rate limiting.
- Strongest argument against: forward-only references rule out legitimate
  designs such as a summary question that depends on later answers. Such a
  question can be placed last; the limitation is documented.

## Reopen when

Offline response becomes a requirement, a seeded survey cannot be
expressed with flat rules, or a second client needs an evaluator in a
language the shared module cannot target.

## Links

ADR 0001, ADR 0002, ADR 0005, ADR 0007, PLAN.md section 3.
