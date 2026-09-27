# Learning-Content Editorial Audit

## Purpose

This ledger is the durable record of an editorial audit of the learner-facing learning
curriculum. It judges how the content reads to a learner: whether prose exposes internal
metadata or curriculum structure, whether references and explanations stand on their own,
and whether vocabulary, voice and tone serve the explanation.

The technical content is generally strong. The problems that opened this audit are about
presentation, and so is the audit.

It does **not** primarily audit:

| Concern | Governed by |
| --- | --- |
| Technical correctness of claims | Lesson authoring and review under the authoring contract |
| Source authority and freshness | Rule 9 of the authoring contract; `LearningCurriculumValidator` |
| Question-bank semantic coverage | Rule 10; [learning-question coverage](../content/learning-question-coverage.md) |
| Content architecture, Unit and Lesson boundaries | Rule 1 and the Topic blueprints |
| UI rendering of lessons | [final visual QA](final-visual-qa.md) |

A technical error noticed during editorial review is still recorded — as a note routed to
the right process, not as an `LC-*` finding.

## Source of Truth

Findings are judged against
[`docs/content/learning-content-authoring.md`](../content/learning-content-authoring.md),
principally Rule 7 (voice, vocabulary, framing, tone), Rule 8 (concise, not cryptic),
Rule 11 (project-agnostic) and Rule 12 (written for learners, not the curriculum graph).

The contract was updated with those editorial rules **before** the corpus audit began, so
that every finding cites a written rule rather than a reviewer's taste. If the audit
exposes a gap in the contract, amend the contract first, then record findings against it.

## Corpus

| Field | Value |
| --- | --- |
| Production source | `shared/src/commonMain/composeResources/files/curriculum/learning_curriculum.json` |
| Baseline commit | `5693061` (branch `task/question-learning-audit`) |
| Units at baseline | 30, all `ACTIVE` |
| Lessons at baseline | 135, all `ACTIVE` |
| Learner-visible prose strings at baseline | 4 983 (definition under [What is reviewed](#what-is-reviewed)) |
| Code blocks at baseline | 377 |

These numbers describe the corpus the audit starts from, not invariants. A pass that runs
against a later commit records that commit and reconciles any Units or Lessons added,
removed or re-authored since.

## What Is Reviewed

Learner-visible text:

- Unit title and summary;
- Lesson title and summary;
- `paragraph` blocks;
- `bullet_list` items;
- `comparison` headers and cells;
- `callout` text.

`code` blocks are reviewed separately and only for editorial problems in comments and
prose-like strings. Identifiers inside code are part of the subject.

IDs, `primarySubtopicIds`, `supportingSubtopicIds`, `relatedLessonIds`, `sources` and
`status` are metadata and are not reviewed as prose. A normal technical identifier —
a package path, a column, an API name — is never an `LC-LEAK` by virtue of its spelling.

## Audit Categories

Each finding carries exactly one primary category. Categories overlap; choose the one
whose fix best describes the rewrite.

| Category | Meaning |
| --- | --- |
| `LC-LEAK` | Internal curriculum or product metadata exposed to the learner: Unit, Lesson, Topic, Subtopic or Question IDs, backlog keys, status values. |
| `LC-XREF` | A cross-reference that is opaque (IDs, Unit numbers, "the next Lesson"), fragile under reordering, or substitutes for explanation the current argument needs. |
| `LC-META` | Curriculum-design or authoring scaffolding exposed unnecessarily: ownership of concepts by Units or Topics, "this Unit deliberately stops…", "we do not re-teach…", curriculum boundaries. |
| `LC-VOCAB` | Vague, habitual or imprecise wording where a specific term exists — the recurring case is "shape". |
| `LC-VOICE` | Robotic or formulaic prose: repeated rhetorical framing, synthetic balanced contrasts, documentation-paraphrase voice. |
| `LC-TONE` | Unnecessarily preachy or prescriptive wording, or an absolute not justified by the technical contract. |
| `LC-CLARITY` | Technically correct but cryptic, underspecified, or unnecessarily hard to follow — including brevity that depends on omitted reasoning. |
| `LC-VERBOSE` | Redundant or meta prose that can be shortened without losing technical substance. |

## Severity

| Severity | Use for |
| --- | --- |
| **High** | An internal metadata leak; missing context that materially obstructs understanding; prose that meaningfully misrepresents a technical decision. |
| **Medium** | Repeated, meta or formulaic language that materially degrades the Lesson; an opaque reference that requires unnecessary curriculum knowledge; a substantial clarity or tone problem. |
| **Low** | A real, local editorial issue suitable for cleanup. |
| **Observation** | A pattern that was reviewed and intentionally retained, recorded so a later pass does not re-litigate it. |

Style preference without evidence is not a High or Medium finding. A Medium finding
should be able to say what the learner loses; a High finding should be able to say what
the learner cannot do.

## Audit Method

The audit runs in two stages.

1. **Automated corpus sweep** — a script walks the learner-visible text defined above and
   records suspicious locations. It produces candidates, not findings.
2. **Human contextual review** — every candidate is read in its paragraph and Lesson and
   either becomes a finding, becomes an Observation, or is dismissed.

> **Match ≠ finding.** No regular-expression hit is a defect until a reviewer has read it
> in context. This audit does not create a style linter based on word occurrence, and no
> sweep pattern should be promoted into CI.

Sweep signals:

| Signal | Why it is suspicious | Typical false positive |
| --- | --- | --- |
| `lesson_*`, `unit_*`, exact Unit/Lesson IDs, Topic/Subtopic IDs | Possible `LC-LEAK` | A real package path in a worked example |
| `Unit N`, `Lesson N` | Possible `LC-XREF` | A numbered step in an example |
| earlier / next / previous / later / following / prior + Unit or Lesson | Possible `LC-XREF` or `LC-META` | — |
| "this Unit", "this Lesson" | Possible `LC-META` | Stating the scope of a worked example |
| owns / ownership / belongs to / lives in / full treatment | Possible `LC-META` | Ownership is a genuine technical subject: state, scope, lifecycle and data ownership |
| boundary / boundaries | Possible `LC-META` | Architecture, module, layer and DI boundaries |
| shape / shapes / shaped | Possible `LC-VOCAB` | "API shape" or "state shape" meaning broad structural form |
| "the point", "the rule", "what matters", "the important part", "worth …", "the honest answer", "deliberately", "not X, but Y" | Possible `LC-VOICE` | A single framing that genuinely directs attention |
| always / never / must / should / only / wrong / correct / obviously / simply / clearly | Possible `LC-TONE` | A real invariant or API guarantee |

The sweep script is a working tool, not a repository artifact; record the patterns and the
commit with each pass so its counts can be reproduced.

## Required Review of "Shape"

Every occurrence of "shape" in learner prose is reviewed and classified:

| Decision | When |
| --- | --- |
| **Keep** | It is genuinely useful shorthand for broad structural form, and a specific term would be less accurate. |
| **Replace** | A precise term exists — signature, parameter list, state model, data representation, type structure, object graph, API contract, call pattern, control flow, layout, and so on. |
| **Remove** | It is rhetorical filler ("the shape worth noticing is…") and the sentence is better stating its fact. |

There is no single replacement word. The rewrite direction names the specific term, or
says to delete the framing.

## Cross-Reference Review

Every prose reference to another Lesson or Unit — by ID, number, position or title — is
classified:

| Decision | When |
| --- | --- |
| **Keep** | The local explanation is sufficient, the reference uses the human-facing title, and it genuinely helps the learner continue. |
| **Rewrite using human title** | The reference is useful but uses an ID, a Unit or Lesson number, or a relative position. |
| **Remove** | The reference does not help the current explanation; `relatedLessonIds` alone does not justify prose. |
| **Bring required context local** | The reference substitutes for a fact the current argument needs. State the fact; keep or drop the reference as a separate decision. |

## Finding Ledger

IDs are stable and sequential (`LC-001`, `LC-002`, …) and are never reused. Location
names the section depth (`CORE`, `PRACTICAL`, `SENIOR`), the block type and its ordinal,
for example `PRACTICAL / paragraph 3`, or `Unit summary`.

Status values: **Open**, **Resolved** (rewritten), **Accepted as-is** (retained after
review, usually with an Observation), **Superseded** (the text was removed or re-authored
for another reason).

| ID | Category | Severity | Unit | Lesson | Location | Short evidence | Problem | Rewrite direction | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| — | — | — | — | — | — | — | — | — | — |

*No findings recorded. The corpus audit has not begun.*

## Pre-Audit Observations

> These are the reasons the audit was opened. They are **not** completed findings, they
> have not been reviewed in context, and none of them is evidence that a specific Lesson
> is defective.

Exploratory reading of the curriculum during interview preparation surfaced repeated
examples of:

- internal `lesson_*` identifiers in learner prose;
- repeated curriculum-ownership language;
- heavy use of "shape";
- frequent previous / next Unit and Lesson narration;
- repeated formulaic phrases such as "worth …", "the rule …" and "the point …";
- unnecessarily absolute or prescriptive constructions.

### Baseline raw match counts

Recomputed deterministically at commit `5693061` over the 4 983 learner-visible prose
strings defined in [What is reviewed](#what-is-reviewed) (Unit and Lesson titles and
summaries; paragraph, callout and bullet text; comparison headers and cells), excluding
code blocks. Matching is case-insensitive on word boundaries. These are **raw matches**;
the confirmed-finding count for every row is currently zero because nothing has been
reviewed.

| Pattern | Raw matches | Notes |
| --- | --- | --- |
| `lesson_[a-z0-9_]+` | 29 | 25 are exact Lesson IDs (18 distinct); 4 are `lesson_study` package paths in a worked example, a likely false positive for `LC-LEAK` |
| `unit_[a-z0-9_]+` | 0 | |
| `(Unit\|Lesson)s? \d+` | 77 | |
| `(earlier\|next\|previous\|later\|preceding\|following\|last\|prior) (unit\|lesson)s?` | 196 | |
| `this (unit\|lesson)` | 324 | |
| `own(s\|ed\|ership)?` | 843 | Dominated by legitimate technical ownership; not a proxy for `LC-META` |
| `shap(e\|es\|ed)` | 188 | |
| `worth` | 247 | |
| `the point` | 30 | |
| `the rule` | 86 | |
| `deliberately` | 79 | |
| `always\|never\|must` | 664 | Includes every real invariant |

## Per-Unit Audit Status

Audit status: **Not reviewed** → **Reviewed** → **Rewrite complete** → **Re-verified**.
Rewrite scope, assigned during review: **None**, **Light copy edit**, **Substantive
editorial rewrite**.

| Unit ID | Human title | Lessons | Audit status | Rewrite scope | Notes |
| --- | --- | --- | --- | --- | --- |
| `unit_thinking_in_compose` | Thinking in Compose | 3 | Not reviewed | — | |
| `unit_state_and_state_ownership` | State and State Ownership | 5 | Not reviewed | — | |
| `unit_recomposition` | Recomposition | 3 | Not reviewed | — | |
| `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | 5 | Not reviewed | — | |
| `unit_derived_state_and_expensive_work` | Derived State and Expensive Work | 3 | Not reviewed | — | |
| `unit_snapshot_fundamentals` | Snapshot Fundamentals | 2 | Not reviewed | — | |
| `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow | 4 | Not reviewed | — | |
| `unit_observable_state_collection` | Observable State Collection and Lifecycle | 4 | Not reviewed | — | |
| `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and LaunchedEffect | 4 | Not reviewed | — | |
| `unit_latest_values_and_event_driven_work` | Latest-Value Effects and Event-Driven Coroutine Work | 3 | Not reviewed | — | |
| `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers | 4 | Not reviewed | — | |
| `unit_production_ui_effects_and_selection` | Production UI Effects and Mechanism Selection | 3 | Not reviewed | — | |
| `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | 5 | Not reviewed | — | |
| `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work | 4 | Not reviewed | — | |
| `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | 5 | Not reviewed | — | |
| `unit_flow_fundamentals` | Flow Fundamentals | 5 | Not reviewed | — | |
| `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | 5 | Not reviewed | — | |
| `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | 5 | Not reviewed | — | |
| `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | 5 | Not reviewed | — | |
| `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | 5 | Not reviewed | — | |
| `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | 5 | Not reviewed | — | |
| `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | 5 | Not reviewed | — | |
| `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | 5 | Not reviewed | — | |
| `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection | 4 | Not reviewed | — | |
| `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | 6 | Not reviewed | — | |
| `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | 6 | Not reviewed | — | |
| `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | 7 | Not reviewed | — | |
| `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | 6 | Not reviewed | — | |
| `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | 5 | Not reviewed | — | |
| `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy | 4 | Not reviewed | — | |

## Rewrite Workflow

After the audit, production content is rewritten in small, reviewable batches: one Unit
at a time, or a tightly related cluster of Units when terminology must stay consistent
across them.

For each batch:

1. Read the batch's findings in this ledger.
2. Preserve technical meaning. An editorial rewrite that changes a claim is a content
   change and needs its sources rechecked under Rule 9.
3. Preserve stable Unit and Lesson IDs.
4. Preserve Subtopic mappings and `relatedLessonIds` unless a separate semantic issue
   requires a change, recorded as such.
5. Change only learner-facing prose.
6. Parse and validate the curriculum: `./gradlew :shared:jvmTest` runs
   `BundledLearningCurriculumTest`, which loads the bundled file and applies
   `LearningCurriculumValidator`.
7. Regenerate the learning-question coverage snapshot — see below.
8. Review the resulting diff.
9. Mark the batch's findings **Resolved** and move the Unit to **Rewrite complete**.
10. Re-read the whole Unit for voice consistency, then mark it **Re-verified**.

### Coverage snapshot and prose-only edits

The coverage *analysis* does not change for a prose-only edit, but the snapshot must
still be regenerated. `tools/learning_question_coverage.py` fingerprints the **whole**
learning document — deliberately, because editing Lesson prose can change whether the
material still teaches an associated question — and CI runs `--check` against that
fingerprint (`.github/workflows/main.yml`). Any edit to `learning_curriculum.json`,
including a single-word copy edit, makes the committed snapshot stale:

```sh
python3 tools/learning_question_coverage.py --write
python3 tools/learning_question_coverage.py --check
```

Expect the resulting diff in `docs/content/learning-question-coverage.md` to be the
fingerprint line only. Anything more means the batch changed mappings or structure and
needs review as such.

## Audit Completion Criteria

The corpus audit is complete only when:

- every active Lesson has been read in full;
- every automated sweep candidate has been reviewed in context and either recorded or
  dismissed;
- every Unit has an audit status other than **Not reviewed**;
- the internal-identifier inventory is complete: every ID-like token in learner prose is
  classified as a leak or as a real technical identifier;
- the cross-reference review is complete, with every prose reference classified;
- corpus-level voice findings — patterns that span Units rather than live in one
  paragraph — are documented;
- every Unit has an assigned rewrite scope;
- no regular-expression hit has been treated as a defect without contextual review.
