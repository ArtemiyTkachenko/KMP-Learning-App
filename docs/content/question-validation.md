# Question Validation

This is the canonical acceptance standard for the interview question bank. Every new
question, and every materially modified one, passes through it before it ships.

Read it together with its two neighbours, and do not restate their content here:

- `docs/content/content-authoring.md` is the editorial **contract** — the rules a question
  must satisfy. Where this document and the contract disagree, the contract wins.
- `docs/content/question-authoring-playbook.md` is the **method** — where plausible
  distractors come from, how to classify interview level, how to write an explanation.

This document answers a narrower question: **how do we know a finished question is
correct, and what stops a defective one from shipping?**

## The lifecycle

Generation and validation are separate stages. A question is not finished when it is
written.

```text
Generate → Independently solve → Validate answer uniqueness → Validate distractors
        → Verify source → Validate explanation → Deterministic checks → Semantic rubric
        → Fix failures → Re-run validation → Accept
```

Two rules make the difference between this being real and being ceremony.

**Independently solve before looking at the key.** Read the finished stem and its options
and decide which one you would pick, using only what is written. Then compare with
`correctAnswerIds`. Reasoning of the form *"I generated option B as the correct one,
therefore option B is correct"* is not validation — generation intent proves nothing. Most
of the defects this process has caught were questions whose author was certain when they
wrote them.

**Re-validate after the final edit, not only before it.** A fix in one field routinely
breaks another: tightening a stem can make a distractor correct, replacing a keyed answer
can invalidate the explanation, narrowing the context can make the cited source
inapplicable, and rewording an option can create overlap with another. The version that
ships is the version that must have passed.

## Deterministic checks

Run automatically. They catch structure, never meaning.

| Where | What it enforces |
| --- | --- |
| `CurriculumValidator` (`shared/.../curriculum/validation/`) | Required fields present and non-blank; unique topic, subtopic, question, and answer IDs; valid topic/subtopic references and hierarchy consistency; at least two answers; `correctAnswerIds` referencing real options; `SINGLE` not carrying several correct answers; answer options within a question not repeating the same text; at least one source with a non-blank title and a syntactically valid `http(s)` URL; no authoring placeholder (`TODO`, `TBD`, `FIXME`, `XXX`, `lorem ipsum`) in any authored text, and no unreachable host in a source URL. It runs at import time, so a malformed bundle is rejected rather than persisted. |
| `InitialCurriculumSmokeTest` | The bundled bank's shape: topic taxonomy, question counts per topic, level distribution, and structural validity. |
| `InitialCurriculumContentQualityTest` | Editorial invariants of the bundled bank: no duplicated stems; every `MULTIPLE` question says "Select all that apply."; every source cites an approved primary documentation host; no keyed answer exceeds its longest distractor by more than 10%; absolute words appear in keyed answers and not only in distractors. |

```sh
./gradlew :shared:jvmTest --tests '*InitialCurriculum*' --tests '*CurriculumValidator*'
```

Two limits are deliberate.

`example.com` is **not** rejected by `CurriculumValidator`, because this repository uses
reserved documentation domains throughout its test fixtures, where they are correct.
Placeholder hosts in shipped content are caught instead by the approved-host check on the
bundled bank.

Nothing here judges whether a distractor is plausible, whether a stem is ambiguous, or
whether a source substantiates its claim. Do not try to automate those with keyword
heuristics; a heuristic that pretends to measure pedagogical plausibility is worse than no
check, because it converts a judgement into a green tick. They belong to the rubric below.

The remaining mechanical check has no automation because it needs the network — confirm
every source URL still resolves before opening a PR. The script is in
`docs/content/question-authoring-playbook.md`, Part 7.

## Semantic rubric

Reasoning-based. Every applicable criterion gets PASS or FAIL.

**Question quality**

- `Q1_CLEAR` — written in clear, natural language.
- `Q2_SELF_CONTAINED` — carries enough context to answer without inferring intent.
- `Q3_UNAMBIGUOUS` — a knowledgeable reader cannot reasonably read it two ways.

**Correct answer**

- `Q4_SINGLE_CORRECT` — exactly one option is objectively correct in the stated context.
- `Q5_CORRECT_VERIFIED` — the key was independently derived, not assumed from metadata.

**Distractors**

- `Q6_DISTRACTORS_PLAUSIBLE` — each could attract someone with incomplete knowledge.
- `Q7_DISTRACTORS_FALSE` — each is definitively wrong under the stem as written.

**Options**

- `Q8_OPTIONS_PARALLEL` — consistent in grammatical form, specificity, and style.
- `Q9_NO_ANSWER_CLUES` — nothing in wording, length, or grammar reveals the key.
- `Q10_NO_OPTION_OVERLAP` — no two options overlap enough to make both defensible.

**Sources**

- `Q11_SOURCE_PRESENT` — an identifiable source exists.
- `Q12_SOURCE_RELEVANT` — it concerns the claim being tested.
- `Q13_SOURCE_SUPPORTS_ANSWER` — it substantiates the keyed answer.
- `Q14_SOURCE_AUTHORITY` — it is authoritative, and primary where one is practical.

**Explanation**

- `Q15_EXPLANATION_CORRECT` — factually correct in every sentence, including the ones
  about distractors.
- `Q16_EXPLANATION_JUSTIFIES` — explains why, rather than restating the answer.
- `Q17_EXPLANATION_CONSISTENT` — explanation, key, stem, and source do not contradict.

**Longevity and usefulness**

- `Q18_CURRENT` — not resting on obsolete behaviour unless the version context is stated.
- `Q19_EDUCATIONALLY_USEFUL` — tests knowledge worth having, not trivia or wording.
- `Q20_NO_DUPLICATION` — does not substantially repeat another question.

### Working the rubric

`Q4` is the one that needs an explicit pass over **every** option, not just the key. For
each, ask: is it factually correct; could it be correct under a reasonable reading; is it
partially correct; does it rely on an unstated assumption; does it overlap the key? Exactly
one option may survive. If two do, rewrite the stem or the options — never resolve the
ambiguity by editing `correctAnswerIds`.

`Q7` has a mirror the playbook calls out and this rubric assumes: **no keyed answer may be
defensibly wrong either.** Marking a knowledgeable candidate incorrect for declining an
over-broad key is worse than an easy question.

## Source verification status

Tracked separately from question validity, because a question can be well written and
badly sourced.

| Status | Meaning |
| --- | --- |
| `VERIFIED` | The source was opened and directly supports the keyed answer. |
| `PARTIALLY_VERIFIED` | Opened, but support is indirect or incomplete. |
| `UNVERIFIED` | Appears legitimate; could not actually be inspected. |
| `INVALID` | Broken, irrelevant, contradictory, or does not support the answer. |

Never record `VERIFIED` without having read the source. A resolving URL is evidence that a
page exists, not that it says what the question claims. Never invent a URL, section number,
or quotation: if a claim cannot be sourced, change the claim or flag the question — the
playbook's rule is *delete what you cannot cite*.

## Severity

- **BLOCKER** — wrong key, several correct answers, no correct answer, false premise,
  fabricated source, source contradicting the answer, fundamentally ambiguous wording.
- **MAJOR** — misleading distractor, missing context, factual problem in the explanation,
  source that does not adequately support the claim, outdated context, partially correct
  distractor.
- **MINOR** — awkward wording, punctuation, verbosity, small explanation improvements.

A substantive correctness problem is never MINOR.

## Final status and confidence

Each reviewed question ends with one status — `PASS`, `PASS_WITH_MINOR_CHANGES`, `FIXED`,
`REVIEW_REQUIRED`, or `REPLACE` — and a confidence of `HIGH`, `MEDIUM`, or `LOW`.

Use `REVIEW_REQUIRED` when correctness needs subject-matter input or source access you do
not have, and `REPLACE` when the premise is false, unverifiable, obsolete, inherently
ambiguous, or educationally useless. Do not optimise for a high PASS rate; a forced PASS
is the failure this process exists to prevent.

## Acceptance gate

A question ships only when **all** of the following hold:

- deterministic validation passes;
- `Q1`, `Q2`, `Q3`, `Q4`, `Q5`, `Q7`, `Q11`, `Q12`, `Q15`, and `Q17` pass;
- `Q13` passes wherever verification is possible;
- no unresolved BLOCKER remains;
- confidence is not `LOW`.

The other criteria are not hard gates, but a failure in any of them should normally be
fixed before merge rather than deferred.

## Recording an audit

Bank-wide reviews record their results in a machine-readable log so a later session reads
the verdict instead of re-deriving it — see `docs/content/question-audit-log.yml` for the
format and the current entries. Keep audit metadata there. Production `Question` objects
carry no review-only fields.
