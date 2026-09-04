---
name: question-bank-change
description: Author, review, or fix interview questions in the bundled question bank. Use whenever initial_curriculum.json questions are added, edited, or audited, including source, explanation, distractor, level, and status changes; not for application code that merely reads curriculum data.
---

# Question Bank Change

## Use When

Adding, rewriting, auditing, or fixing any question in
`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json` — a new
batch, a wording fix, a distractor replacement, a source correction, an explanation edit, a
level reclassification, or a status change.

## Do Not Use When

Changing application code that reads curriculum data (the persistence layer, selection
policies, review screens) without changing question content, or editing the curriculum
taxonomy alone.

## The one rule that makes this skill necessary

**Generation is not validation.** A question is not finished when it is written. Every new
or materially modified question passes the lifecycle below before it ships, and the stage
that catches the most defects is the one that is easiest to skip: solving the finished
question yourself, from the stem and options alone, before reading `correctAnswerIds`.

Reasoning of the form *"I wrote option B as the correct one, so option B is correct"* has
never validated anything.

## Workflow

1. **Read the rules.** `docs/content/content-authoring.md` is the contract;
   `docs/content/question-authoring-playbook.md` is the method for meeting it;
   `docs/content/question-validation.md` is the acceptance standard. Load the one the task
   needs rather than all three.
2. **Check what the bank already holds.** `docs/content/question-bank-coverage.md` records
   per-subtopic counts, the concept index, and the audit baselines a new batch must not
   degrade. Read it instead of re-deriving coverage from the JSON.
3. **Write or edit the question**, following an existing question in the same subtopic for
   shape. Budget most of the effort on the three wrong answers, not the right one.
4. **Validate it**, following the full lifecycle in
   `docs/content/question-validation.md`: solve independently → test every option for
   defensibility → verify each distractor is both plausible and definitively false → open
   the source and confirm it states the claim → check the explanation against the source →
   run the deterministic checks → work the semantic rubric → fix → re-validate the final
   text.
5. **Respect stable identity.** Keep `Question.id` for wording, clarity, distractor,
   explanation, and source changes. Keep an `AnswerOption.id` when only its wording
   changes, and issue a new one when the claim itself changes — historical attempts store
   selected answer IDs, so reusing an ID for a different assertion corrupts them.
6. **Update the pinned counts** the tests assert (question totals per topic, level
   distribution, `questionSources`) in the same change, and regenerate the coverage tables
   when the bank's shape changes.
7. **Record a bank-wide audit** in `docs/content/question-audit-log.yml`. A single new
   question does not need an entry; a review or remediation pass does.

## Validation Commands

```sh
# Deterministic: structural validation plus the bundled-bank content gates.
./gradlew :shared:jvmTest --tests '*InitialCurriculum*' --tests '*CurriculumValidator*'

# Everything that touches curriculum data.
./gradlew :shared:jvmTest
```

Source liveness and anchor resolution need the network and are not automated — run both
scripts in `docs/content/question-authoring-playbook.md`, Part 7, before opening a PR. A URL
whose `#section` has been deleted still returns 200, so the liveness loop alone will pass it.

**A resolving URL is not a verified source.** Pages return HTTP 200 while rendering nothing,
and vendor guides are restructured with sections silently removed. Open the page and find
the sentence. Where a question turns on a named parameter or a specific contract, cite the
API reference rather than the narrative guide — guides drift, reference pages do not.

Record source status honestly as `VERIFIED`, `PARTIALLY_VERIFIED`, `UNVERIFIED`, or
`INVALID`. Never claim `VERIFIED` for a page you did not read, and never invent a URL,
section number, or quotation. If a claim cannot be cited, change the claim.

## Project References

- [Question validation](../../../docs/content/question-validation.md) — the lifecycle,
  rubric, severity scale, and acceptance gate. The canonical definition of question quality.
- [Content authoring](../../../docs/content/content-authoring.md) — the editorial contract.
- [Authoring playbook](../../../docs/content/question-authoring-playbook.md) — distractor
  sourcing, level classification, anti-cue audits, stable identity.
- [Question bank coverage](../../../docs/content/question-bank-coverage.md) — what the bank
  already covers and the baselines to hold.
- [Audit log](../../../docs/content/question-audit-log.yml) — prior review verdicts.

## Output

Report the questions changed by ID, what each fix was, the commands run, and the source
verification status of every citation touched. Name any question left
`REVIEW_REQUIRED` or `REPLACE` rather than forcing it to pass.
