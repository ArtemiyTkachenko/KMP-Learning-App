# Learning-Content Editorial Audit

## Purpose

This ledger is the durable record of an editorial audit of the learner-facing learning
curriculum. It judges how the content reads to a learner: whether prose exposes internal
metadata or curriculum structure, whether references and explanations stand on their own,
and whether vocabulary, voice and tone serve the explanation.

The technical content is generally strong, and the full read confirmed it. The problems
this audit records are about presentation, and so is the audit. It is an audit, not a
rewrite: every finding names a problem and a rewrite direction, and no production content
was changed.

It does **not** primarily audit:

| Concern | Governed by |
| --- | --- |
| Technical correctness of claims | Lesson authoring and review under the authoring contract |
| Source authority and freshness | Rule 9 of the authoring contract; `LearningCurriculumValidator` |
| Question-bank semantic coverage | Rule 10; [learning-question coverage](../content/learning-question-coverage.md) |
| Content architecture, Unit and Lesson boundaries | Rule 1 and the Topic blueprints |
| UI rendering of lessons | [final visual QA](final-visual-qa.md) |

A technical error noticed during editorial review is still recorded — as a note routed to
the right process, not as an `LC-*` finding. None was found that needed routing; two
structural observations are recorded in the Lesson matrix (a Lesson with two `CORE`
sections, and two near-identical Lesson titles in different Topics).

**Source of truth.** Findings are judged against
[`docs/content/learning-content-authoring.md`](../content/learning-content-authoring.md),
principally Rule 7 (voice, vocabulary, framing, tone), Rule 8 (concise, not cryptic),
Rule 11 (project-agnostic) and Rule 12 (written for learners, not the curriculum graph).

## Audit Baseline

| Field | Value |
| --- | --- |
| Production source | `shared/src/commonMain/composeResources/files/curriculum/learning_curriculum.json` |
| Audited commit | `3ec5225` (branch `task/question-learning-audit`), local HEAD on 2026-09-27 |
| Relation to the pre-audit baseline | The curriculum file is byte-identical to `5693061`, the commit the pre-audit counts were taken at (`git diff --quiet 5693061 HEAD -- …/curriculum/` succeeds) |
| Units | 30, all `ACTIVE` |
| Lessons | 135, all `ACTIVE` |
| Learner-visible prose strings | 4 983 (definition under [What is reviewed](#what-is-reviewed)) |
| Learner-facing prose words | ≈ 192 400 — 189 151 in Lessons, 3 243 in Unit titles and summaries |
| Code blocks | 377 (≈ 12 000 words), reviewed separately for comments and prose-like strings |
| Coverage of the read | Every block of every active Lesson, every Unit title and summary |

These numbers describe the corpus this pass audited, not invariants. A later pass
records its own commit and reconciles Units or Lessons added, removed or re-authored.

## Method

### What Is Reviewed

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

### Two Stages

1. **Automated sweep (Stage A).** A throw-away script walked the 4 983 prose strings and the
   377 code blocks and listed every candidate below, sentence by sentence, with its
   Lesson and block location. The sweep located places to read; it decided nothing.
2. **Close reading (Stage B).** Every Lesson was then read in full, Unit by Unit, in
   authored order, with the sweep candidates for that Unit beside it. Every candidate was
   classified in context; findings, cross-reference decisions and "shape" decisions were
   recorded while reading, not afterwards. Problems the sweep could not see — voice,
   summaries written as objectives, undefined labels, examples drawn from this
   application — were found by reading.

> **Match ≠ finding.** No regular-expression hit is a defect until a reviewer has read it
> in context. No sweep pattern should be promoted into CI.

Sweep signals and raw counts over the 4 983 prose strings at the audited commit
(case-insensitive, word boundaries; code blocks excluded unless stated):

| Signal | Raw matches | What contextual review made of it |
| --- | ---: | --- |
| Tokens matching a real Unit, Lesson, Topic, Subtopic or Question ID | 26 | 25 Lesson IDs (18 distinct) and 1 Question ID — all confirmed leaks |
| `lesson_[a-z0-9_]+` | 29 | 25 Lesson IDs plus 4 `lesson_study` package paths (dismissed: real technical identifiers) |
| `unit_[a-z0-9_]+` | 0 | — |
| Backlog keys (`E\d\d`) | 1 | `E24` — confirmed leak |
| `(Unit\|Lesson)s? \d+` (incl. `unit-1`) | 78 | All course coordinates; see [Cross-reference audit](#cross-reference-audit) |
| earlier / next / previous / later / … + Unit or Lesson | 223 | Nearly all positional references; see cross-reference audit |
| `this (unit\|lesson)` | 324 | Mostly scope narration (`LC-META`); a minority harmless ("this lesson's example") |
| `curricul(um\|a)` | 151 | Almost always curriculum-ownership narration; none needed |
| `own(s\|ed\|ership)?` | 843 | Dominated by legitimate technical ownership; read, not counted |
| `boundary` / `boundaries` | — | Read in context; curriculum boundaries recorded, engineering boundaries kept |
| `shap(e\|es\|ed)` | 188 (+ 6 in code comments) | All 194 classified; see [Shape audit](#shape-audit) |
| `worth` | 247 | Recurring framing; see [Voice and tone](#voice-and-tone-findings) |
| `honest(ly)` | 79 | Recurring framing |
| `deliberate(ly)` | 101 | Mostly scope defence ("deliberately not taught here") |
| `the point` / `the rule` | 30 / 86 | Mostly acceptable; framed uses folded into voice findings |
| `is not … . It/That/This is` | 40 | Corrective-contrast template |
| Callouts beginning "Do not" | 11 | Concentrated in two Units |
| `always\|never\|must` | 664 | Read against the contract; overwhelmingly real invariants |
| `should( not)?` / `wrong\|correct` | 311 / 328 | Mostly calibrated; see tone section |
| "this project / repository / app / application / codebase / build", "the configured / resolved / verified" | 25 | Rule 11 candidates; several confirmed |
| "learner(s)" in prose | 15 | Third-person "learner" in tables; learning-app example domain leaking |
| Verification narration ("was run", "checked against", "measured here") | 8 | Author-process narration; several confirmed |

### Categories

Each finding carries exactly one primary category. Categories overlap; the one chosen is the
one whose fix best describes the rewrite.

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
| `LC-PROJECT` | Learner-facing content that depends on, or reports facts about, this repository's or product's implementation (Rule 11): its real classes, modules, package structure, file counts, KDoc, dependency setup, object graph or current source-tree state used as evidence, statements about what the codebase "currently" does, and results attributed to this build rather than to a named version. *Added after the audit; used for findings filed from then on.* |

`LC-PROJECT` and `LC-META` split on what the prose exposes. `LC-PROJECT` is about the
**software** the curriculum ships inside — a sentence a learner could only verify by
reading this repository. `LC-META` is about the **curriculum itself** — how the course is
divided, ordered, owned, authored or maintained. A sentence reporting this app's Koin
modules is `LC-PROJECT`; a sentence saying another Unit owns Koin scopes is `LC-META`; a
sentence exposing a curriculum ID or backlog key remains `LC-LEAK`.

Two application notes. **Rule 11 problems** — Lessons that use this learning application's
own types, module names, counts or KDoc as evidence — had no category of their own during
this audit; they were filed as `LC-META` (authoring material exposed to the learner),
except where they materially obstruct understanding, when they were filed as `LC-CLARITY`.
Those historical findings keep their original category and number; `LC-PROJECT` applies to
findings filed after it was added (see
[Authoring-contract follow-ups](#authoring-contract-follow-ups)).
**Leaks** get one finding per Lesson; every leaked identifier is listed individually in the
[inventory](#internal-identifier-inventory).

### Severity

| Severity | Use for |
| --- | --- |
| **High** | An internal metadata leak; missing context that materially obstructs understanding; prose that meaningfully misrepresents a technical decision. |
| **Medium** | Repeated, meta or formulaic language that materially degrades the Lesson; an opaque reference that requires unnecessary curriculum knowledge; a substantial clarity or tone problem. |
| **Low** | A real, local editorial issue suitable for cleanup. |
| **Observation** | A pattern that was reviewed and intentionally retained, recorded so a later pass does not re-litigate it. |

Severity was assigned conservatively. Every leak is High by definition; beyond leaks, High
was used only where a learner meets authoring vocabulary they cannot decode (a Lesson title),
or where the learner is asked to accept evidence that exists only in this repository.

### Finding Granularity

One finding is one editorial problem in one Lesson — several sentences with the same
problem in the same Lesson are one finding. A pattern that runs through a whole Unit (all
Lesson summaries written as objectives; "Do not…" callouts in every Lesson; Topic-local Unit
numbers) is one Unit-level finding whose location lists the affected Lessons.

### Rewrite Scope

| Scope | Rule applied in this audit |
| --- | --- |
| **None** | No Lesson-specific finding. At most the Unit-wide consistency edits (summary pattern, a positional pointer, one framing word) touch it — a sentence or two. |
| **Light copy edit** | Technical structure and explanation are sound; the work is deleting narration, replacing vague words, re-pointing references, softening, shortening. |
| **Substantive editorial rewrite** | The technical subject stays, but a meaningful share of the prose — a whole section, the summary plus opening plus close, or a load-bearing example — needs restructuring or re-voicing. |

### Cross-Reference and "Shape" Decisions

Every prose reference to another Lesson, another Unit, a "curriculum", or a position in the
course was classified:

| Decision | When |
| --- | --- |
| **Keep** | The local explanation is sufficient, the reference uses the human-facing title, and it genuinely helps the learner continue. |
| **Rewrite using human title** | The reference is useful but uses an ID, a Unit or Lesson number, a relative position or an informal nickname. |
| **Remove** | The reference does not help the current explanation; `relatedLessonIds` alone does not justify prose. |
| **Bring required context local** | The reference substitutes for a fact the current argument needs. State the fact; keep or drop the reference as a separate decision. |

Every occurrence of "shape" was classified **Keep** (broad structural form, genuinely the
best word), **Replace** (a precise term exists — the direction names it) or **Remove**
(rhetorical filler; the sentence states its fact instead).

## Corpus Metrics

Lesson prose length (title, summary and all prose blocks; code excluded):

| Statistic | Words |
| --- | ---: |
| Minimum | 457 (**Which Graph Does This Binding Join?**) |
| Median | 1 422 |
| Mean | 1 401 |
| 75th percentile | 1 687 |
| 90th percentile | 2 052 |
| Maximum | 2 423 (**Is This State, or Is It Something That Happened?**) |
| Lessons over ~1 500 words | 60 |
| Lessons over ~2 000 words | 17 |

By Topic:

| Topic (Units) | Units | Lessons | Prose words | Mean words per Lesson |
| --- | ---: | ---: | ---: | ---: |
| Compose (1–12) | 12 | 43 | 51 889 | 1 207 |
| Coroutines and Flow (13–18) | 6 | 29 | 38 537 | 1 329 |
| Application Architecture (19–24) | 6 | 29 | 56 134 | 1 936 |
| Dependency Injection (25–30) | 6 | 34 | 42 591 | 1 253 |

Of the 17 Lessons over 2 000 words, 13 are in Application Architecture, 3 in Compose
(the two snapshot Lessons and mechanism selection) and 1 in Dependency Injection; none is in
Coroutines and Flow. Length is context, not a quality metric; the over-2 000 Lessons are assessed
individually under [Clarity and verbosity](#clarity-and-verbosity-findings).

## Corpus-Level Findings

1. **The technical teaching is strong and survives the audit intact.** No Lesson needs its
   technical content rewritten. Several Lessons are exemplary as teaching — the snapshot,
   derived-state, flow-buffering, SharedFlow-replay, scope-ownership and Dagger-scope
   Lessons among them — and the audit recommends leaving them close to as they are.
2. **Curriculum narration is the dominant defect.** `LC-META` is 100 of 223 findings. The
   corpus contains 588 prose references to other Lessons, Units, "curricula" or course
   positions; only 10 already meet the contract. 349 should simply be deleted, because the
   sentence around them already restates what they point at.
3. **Metadata leaks are real but contained.** 28 confirmed leaks — 25 Lesson IDs (18
   distinct), one Question ID, one backlog key (`E24`) and one reference to a backlog epic —
   in 16 Lessons across 8 Units. All 25 Lesson IDs sit in Units 7–11 and one sentence of
   Unit 19. They are a signature of one authoring generation, not a corpus-wide habit.
4. **Course coordinates are worse than fragile: some are ambiguous or wrong.** Unit numbers
   are Topic-local, so "Unit 1" means **Thinking in Compose** in a Compose Lesson,
   **Coroutine Fundamentals and Structured Concurrency** in a coroutines Lesson and
   **Dependency Injection as Object Construction** in a Dagger Lesson. Several references
   point at Units or curricula that do not exist ("the performance unit", "the lazy-layout
   unit", and the testing, modularization, persistence, background-work,
   lifecycle-and-navigation and language "curricula"), and "later in this curriculum" is used
   for Compose material that precedes the Lesson in authored order.
5. **This application is used as evidence (Rule 11).** Architecture and Koin Lessons cite
   this learning app's real repositories, state holders, Koin modules, KDoc and even counts
   ("15 ViewModel definitions… 20 `koinViewModel()` resolutions across 15 files"), and one
   Lesson addresses "the application you are reading this in". The Koin Unit is effectively
   a case study of this repository.
6. **"Shape" is a genuine habit, not a false alarm.** Of 188 prose occurrences (plus 6 in code
   comments), 25 are legitimate, 140 should name the precise thing (signature, contract,
   representation, arrangement, pattern, kind of work) and 23 are filler. Use clusters in
   Flow Fundamentals, the Architecture repository and screen-state Units, the MVP/MVVM/MVI
   Unit and the DI Units.
7. **Voice is mostly natural, with identifiable tics.** "worth …" (247), "honest(ly)" (79)
   and "deliberately" (101) recur as framing; summaries follow two templates depending on the
   generation — imperative learning objectives (33 Lesson summaries) or "This lesson …"
   authoring outlines (30). The corrective "is not X. It is Y." appears 40 times.
8. **Tone is generally well calibrated.** Absolute language is overwhelmingly backed by a
   real contract. Architecture and DI Lessons already hedge preferences carefully. Only 4
   tone findings were warranted.
9. **Length is justified by reasoning more often than not.** No over-length Lesson holds two
   mental models, so no split is recommended. Avoidable scaffolding ranges from about 5% to
   about 30% of a Lesson and is concentrated in Architecture.
10. **Problems cluster by authoring generation** (see
    [Authoring-generation observations](#authoring-generation-observations)), which is why
    the rewrite should be batched by Unit and tuned per generation rather than applied
    uniformly.

Findings by category and severity:

| Category | High | Medium | Low | Observation | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| `LC-LEAK` | 16 | 0 | 0 | 0 | 16 |
| `LC-XREF` | 0 | 11 | 23 | 0 | 34 |
| `LC-META` | 3 | 49 | 48 | 0 | 100 |
| `LC-VOCAB` | 0 | 4 | 23 | 0 | 27 |
| `LC-VOICE` | 0 | 5 | 16 | 0 | 21 |
| `LC-TONE` | 0 | 0 | 4 | 0 | 4 |
| `LC-CLARITY` | 0 | 8 | 2 | 1 | 11 |
| `LC-VERBOSE` | 0 | 1 | 9 | 0 | 10 |
| **Total** | 19 | 78 | 125 | 1 | 223 |

Rewrite scope: **7** Lessons None, **114** Light copy edit, **14** Substantive editorial
rewrite. Units: **25** Light copy edit, **5** Substantive editorial rewrite (Units 7, 8, 23,
24 and 29), **0** None.

## Finding Ledger

IDs are stable and sequential (`LC-001` … `LC-223`) and are never reused. Location names the
section depth (`CORE`, `PRACTICAL`, `SENIOR`), the block type and its ordinal within that
section, for example `PRACTICAL / paragraph 3`, or `Unit summary`. Where a Lesson has two
sections of the same depth, the ordinal counts within each section. Unit-level findings
list the affected Lessons as `L<unit>.<lesson>` in authored order.

Status values: **Open**, **Resolved** (rewritten), **Accepted as-is** (retained after
review, usually with an Observation), **Superseded** (the text was removed or re-authored
for another reason).

| ID | Category | Severity | Unit | Lesson | Location | Short evidence | Problem | Rewrite direction | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| LC-001 | `LC-META` | Low | `unit_thinking_in_compose` | — (Unit-level) | Unit summary | "This unit replaces that habit… so it is worth getting right before any Compose API is introduced" | Summary narrates the course sequence rather than the subject; the three ideas are good and stand without it. | State the three ideas and why later Compose behaviour depends on them; drop "this unit" and "before any Compose API is introduced". | Open |
| LC-002 | `LC-META` | Medium | `unit_thinking_in_compose` | `lesson_composable_execution` | CORE / paragraphs 2–3; PRACTICAL / paragraph 3 | "everything later in the Compose path"; "That is deliberately as far as this lesson takes recomposition"; "its own unit later in this path" (×2) | Four sentences narrate the course plan; the local explanation ("the body… may execute again… without anyone in your code calling it") is already sufficient. | Delete the scope narration; if a pointer helps, name **Recomposition** and **Effect Lifecycle and LaunchedEffect** by title once. | Open |
| LC-003 | `LC-VOICE` | Low | `unit_thinking_in_compose` | `lesson_composable_execution` | CORE 2 / paragraph 1; SENIOR / paragraphs 1–2, callout 1; PRACTICAL / paragraph 3 | "The habit worth breaking early"; "It is worth asking why"; "a detail worth getting right"; "The practical point is not really threading. It is that…"; "is not a matter of taste. It is what allows…"; "The correction is not to move the call somewhere vaguer. It is to…" | "worth …" framing four times and the "not X. It is Y." corrective construction three times in one Lesson; the content is excellent but the cadence reads templated. | Lead with the claim ("Composables are not callbacks", "Compose does not run composables in parallel today…"); keep at most one corrective contrast. | Open |
| LC-004 | `LC-XREF` | Low | `unit_thinking_in_compose` | `lesson_state_down_events_up` | CORE / paragraph 3; SENIOR / paragraph 3 | "it has its own lesson later in this path"; "a decision model with its own lesson later in this path" | Positional references; the local text already explains owner choice and hoisting enough for the argument. | Name **State Hoisting and the Lowest Sensible Owner** by title, or drop the pointer. | Open |
| LC-005 | `LC-META` | Low | `unit_thinking_in_compose` | `lesson_state_down_events_up` | SENIOR / paragraph 2 | "Those belong to the architecture topic rather than to Compose, and this path deliberately stops at the direction itself." | Curriculum-ownership narration closing a paragraph that has already taught the point. | Drop the sentence, or replace with an optional pointer to **MVP, MVVM and MVI Responsibility Models**. | Open |
| LC-006 | `LC-META` | Low | `unit_state_and_state_ownership` | — (Unit-level) | Unit summary | "This unit covers…"; "Recomposition, stability and effects all assume this material, so it comes before any of them." | The last sentence justifies the course order to the learner. | Keep the list of four questions as plain subject statements; drop the ordering justification. | Open |
| LC-007 | `LC-XREF` | Medium | `unit_state_and_state_ownership` | `lesson_observable_state` | CORE / paragraphs 1, 7; PRACTICAL / paragraphs 5, 7, callout 1; SENIOR / paragraphs 2–3 | Opens with "The previous unit left a deliberate gap. … What it did not say…"; "It has its own unit later in this path. Everything in this unit and the next…"; "the previous unit's lesson on data flow direction warned about"; "the next unit is built on"; "its own lesson at the end of this unit" | Nine positional or course-narration references in one Lesson, including the opening, which a reader arriving directly cannot anchor. The technical content around them is already local. | Open on the problem itself (a value changes and nothing redraws). Delete the course narration; where a pointer helps, name **Recomposition**, **`remember`: Composition Memory** or **Collections and Observable Mutation** by title. | Open |
| LC-008 | `LC-VOICE` | Low | `unit_state_and_state_ownership` | `lesson_observable_state` | CORE / paragraph 4; PRACTICAL / callout 1; SENIOR / paragraphs 1, 3 | "worth separating from the start"; "The symptom is worth stating carefully"; "It is worth being precise about"; "The equality behaviour is worth knowing" | Four "worth …" framings delay the statements they introduce. | State each fact directly; the precision is carried by the sentences that follow. | Open |
| LC-009 | `LC-XREF` | Low | `unit_state_and_state_ownership` | `lesson_remember_composition_memory` | CORE / paragraph 1; PRACTICAL / paragraph 1, callout 1; SENIOR / paragraphs 2–3 | "The previous lesson ended on a counter…"; "Which mechanism does is the next lesson."; "which is hoisting, two lessons from here"; "it has its own unit later in this path" | Positional references; the local restatement of the counter bug is good, so the references add nothing but coordinates. | Keep the restated counter bug, drop "The previous lesson"; name **`rememberSaveable` and State That Must Survive**, **State Hoisting and the Lowest Sensible Owner** and **Composable Identity** by title where a pointer helps. | Open |
| LC-010 | `LC-META` | Medium | `unit_state_and_state_ownership` | `lesson_remember_saveable` | SENIOR / paragraphs 2, 4 | "The Production Screen State and Unidirectional Data Flow unit later in this path covers the bounded Compose-side screen-owner boundary; detailed state-holder architecture remains later curriculum."; "which is the next Lesson" | Authoring vocabulary ("bounded", "screen-owner boundary", "remains later curriculum") reaches the learner at the end of an otherwise concrete synthesis. | Delete the curriculum sentence; the realistic-screen example already makes the point. Optionally point to **The Screen-Level Owner as a Bounded Bridge** by title. | Open |
| LC-011 | `LC-META` | Medium | `unit_state_and_state_ownership` | `lesson_state_hoisting` | CORE / paragraph 3; PRACTICAL / paragraphs 2–3, 5 | "This lesson is not about the direction, which is settled"; "the decision this lesson is really about"; "the reason the next unit treats read location as a design decision"; "The Production Screen State … unit covers that Compose-side boundary without turning it into application architecture; detailed state-holder design remains later curriculum." | Scope narration and authoring vocabulary interrupt one of the Unit's best decision models. | Remove the scope sentences; state "Plain state holders keep UI logic; business logic goes to a screen-level owner" directly and name **Recomposition Scopes and Selective Execution** only if a pointer is wanted. | Open |
| LC-012 | `LC-TONE` | Low | `unit_state_and_state_ownership` | `lesson_state_hoisting` | PRACTICAL / paragraph 5 | "What it must not hold is business logic or application data" | An architectural placement guideline stated as an invariant. | "Keep business logic and application data out of it; those belong to a screen-level owner" — guidance, with the reason. | Open |
| LC-013 | `LC-META` | Low | `unit_state_and_state_ownership` | `lesson_observable_collections` | SENIOR / paragraph 3 | "This is also the doorway into the next major idea in the Compose path. … the subject of the identity and stability unit; the piece to carry forward from here…" | Course-navigation framing around a good technical bridge (read-only vs immutable → unstable collections). | Keep the stability fact; name **Stability and Skipping** by title or drop the pointer. | Open |
| LC-014 | `LC-META` | Low | `unit_recomposition` | — (Unit-level) | Unit summary | "The units on stability and derived state are answers to problems framed here." | Course-plan sentence in a summary that otherwise states the subject well. | Drop the sentence. | Open |
| LC-015 | `LC-XREF` | Medium | `unit_recomposition` | `lesson_composition_and_recomposition` | CORE / paragraph 1; PRACTICAL / paragraphs 6–7; SENIOR / paragraph 4 | "Two units have now supplied the halves… The first said… The second said…"; "the previous unit's rule about side effects"; "its own unit later in this path"; "a fact worth carrying into the next lesson"; "the technique that exploits it deliberately belongs to the performance unit" | The opening recaps by course coordinate (the facts are restated, so the coordinates add nothing). "The performance unit" names a Unit that does not exist in the curriculum, so the pointer for deferred reads is dangling. | Open with the two facts directly (a composable body may re-run; a read during execution records a dependency). Replace "the performance unit" with one clause naming the technique (reading state inside a layout- or draw-phase lambda) or drop it; name **Effect Lifecycle and LaunchedEffect** by title if needed. | Open |
| LC-016 | `LC-XREF` | Medium | `unit_recomposition` | `lesson_recomposition_scopes` | CORE / paragraph 1; PRACTICAL / paragraph 2, callout 1; SENIOR / paragraph 3 | "The previous lesson said…"; "the honest summary at this stage of the path is that… which is the subject of the next unit, not something to guess at here"; "it belongs to the performance unit" | Same pattern: positional framing, and a second dangling "performance unit" reference. The "stage of the path" sentence is both META and VOICE ("honest summary"). | Quote the invalidation fact without "the previous lesson"; say "whether a call is skipped depends on its argument types and compiler configuration (see **Stability and Skipping**)"; replace the performance-unit pointer with the one-clause technique or delete. | Open |
| LC-017 | `LC-VOICE` | Low | `unit_recomposition` | `lesson_recomposition_scopes` | PRACTICAL / paragraph 6; SENIOR / paragraphs 1, 3 | "is worth having in your hands"; "The question worth being able to answer"; "is worth flagging so it is not reinvented" | Repeated "worth" framing. | State the question and the answer directly. | Open |
| LC-018 | `LC-META` | Medium | `unit_recomposition` | `lesson_recomposition_cost` | Lesson summary; CORE / paragraphs 1, 4; PRACTICAL / paragraph 4, callouts 1–2 | Summary: "A short lesson with one job: … so that the units on stability and derived state are read as tools…"; "it turns the next two units into a checklist"; "The tools for that are the subject of later units, and are named here only so the problem has a visible destination"; "the two levers this unit has actually established" | The Lesson's own summary and several paragraphs describe the Lesson's role in the course instead of its subject; "named here only so the problem has a visible destination" is authoring rationale. | Summary: state the claim (the target is unnecessary expensive work, not recomposition count). Name the tools as tools; drop "later units"/"this unit"; point to **Keeping Work Out of Composition** by title. | Open |
| LC-019 | `LC-TONE` | Low | `unit_recomposition` | `lesson_recomposition_cost` | PRACTICAL / callout 2 | "leading with them, before saying what problem they solve, is the answer that sounds rehearsed" | Judges the reader's hypothetical answer rather than advising. | "Lead with the problem, then name stability and memoization as tools." | Open |
| LC-020 | `LC-VERBOSE` | Low | `unit_identity_keys_and_stability` | — (Unit-level) | Unit summary | "This unit takes them apart — identity from the call site…, the gap between…, and the stability rules… — and ends with the two annotations that let you answer the third question yourself…" | A 100-word summary that walks the Lesson sequence; the first two sentences already carry the subject. | Keep the three questions; compress the Lesson walk-through into one clause. | Open |
| LC-021 | `LC-XREF` | Medium | `unit_identity_keys_and_stability` | `lesson_composable_identity` | CORE / paragraphs 1–3, 5; PRACTICAL / paragraph 5; SENIOR / paragraphs 2–3 | "Three units have built up a working model…"; "The second was Unit 2's subject and returns here only as a contrast; the third is two lessons away."; "a question the `remember` lesson deferred"; "the over-specified-key case from Unit 2"; "which is the next lesson" | Explicit Unit numbers and relative Lesson distances. The comparison table already states the three questions locally, so the coordinates only make the text fragile. | Delete the course coordinates; say "the other two questions are covered in **Collections and Observable Mutation** and **Stability and Skipping**" only if useful; restate the over-specified-key case in a clause ("keys that change every execution"). | Open |
| LC-022 | `LC-META` | Low | `unit_identity_keys_and_stability` | `lesson_composable_identity` | PRACTICAL / callout 1; SENIOR / paragraph 4 | "Both versions of `EditorPanel` above were run rather than reasoned about. Each `NoteField` was given a remembered token…"; "Where the runtime keeps this information is deliberately out of scope… being able to name it adds nothing to an interview answer" | Author verification process and an Exclude decision narrated to the learner. | State the observable behaviour (and, if wanted, how a reader can check it with a remembered token). Drop the out-of-scope paragraph; its last sentence (the contract) can close the previous paragraph. | Open |
| LC-023 | `LC-XREF` | Medium | `unit_identity_keys_and_stability` | `lesson_keys_and_identity_in_lists` | CORE / paragraph 1; PRACTICAL / callout 1; SENIOR / paragraph 4 | "The previous lesson said…"; "Those are the second and third questions of this unit"; "all belong to the lazy-layout unit later in this path; this lesson uses a list as the setting…" | "The lazy-layout unit" does not exist, so the pointer for item reuse and `contentType` is dangling; "second and third questions of this unit" is opaque to a direct reader. | Drop the positional framing; replace "questions of this unit" with the questions themselves (observability, skipping); state the disposal fact and drop or genericise the lazy-layout pointer. | Open |
| LC-024 | `LC-VOCAB` | Low | `unit_identity_keys_and_stability` | `lesson_keys_and_identity_in_lists` | CORE / paragraph 3 | "The last row is the one worth being able to predict out loud, because it is the shape most interview questions take." | "Shape" plus interview-coaching framing where the scenario itself is the point. | "The reordered list is the case to be able to predict:" then the example. | Open |
| LC-025 | `LC-META` | Low | `unit_identity_keys_and_stability` | `lesson_immutability_vs_stability` | CORE / paragraphs 1–2; PRACTICAL / paragraph 3, bullet list 1 | "This lesson asks the question that comes next and matters more."; "Being able to separate them is the whole content of this lesson"; "Path 1 is the failure the earlier lesson covered"; "That is skipping, and it is the next lesson." | Lesson describes its own role; the facts are already local. | Delete the self-description; name **Stability and Skipping** if a pointer is wanted. | Open |
| LC-026 | `LC-META` | Medium | `unit_identity_keys_and_stability` | `lesson_stability_and_skipping` | Lesson summary; PRACTICAL / paragraph 1; SENIOR / paragraphs 1, 5 | Summary opens "The third question of the unit"; "no `composeCompiler { }` block declared anywhere in the build"; "There is an honest complication to record here"; "The previous unit's argument stands unchanged"; "they belong to the performance unit later in this path" | The summary is not self-contained; the toolchain note reports this repository's build (Rule 11) rather than a reproducible configuration; "record" is ledger language; a third dangling "performance unit" reference. | Summary: "When the runtime reaches a call, may it decline to run it?". State the toolchain as "default compiler options" rather than "anywhere in the build". Replace "honest complication to record" with the claim ("the compiler observed here does not follow the documented rule for collections"). Drop the performance-unit pointer. | Open |
| LC-027 | `LC-VOICE` | Low | `unit_identity_keys_and_stability` | `lesson_stability_and_skipping` | PRACTICAL / paragraphs 1, 4, 8; SENIOR / paragraph 5 | "it is worth stating which toolchain"; "Three comparisons are worth being able to run in your head"; "That third condition is worth demonstrating"; "It is also worth saying plainly" | "Worth" framing four times. | Lead with the fact each sentence introduces. | Open |
| LC-028 | `LC-VOICE` | Medium | `unit_identity_keys_and_stability` | `lesson_stability_annotations` | PRACTICAL / callout 1, bullet list 1; SENIOR / paragraphs 5, 7 | "One honest detail"; "the honest claim is"; "may then honestly be `@Stable`"; "the honest routes are…"; "What is not honest is assuming…" | Five uses of "honest" in one Lesson turn a technical contract into moral language and read as a tic. | Use technical words: "accurate claim", "valid routes", "is not safe to assume"; keep "promise/contract", which is the Lesson's real vocabulary. | Open |
| LC-029 | `LC-CLARITY` | Medium | `unit_identity_keys_and_stability` | `lesson_stability_annotations` | CORE / paragraph 1; PRACTICAL / paragraph 3; SENIOR / paragraphs 6–7 | "The previous lesson left one thread open."; "using the entity type from the previous lesson"; "what makes the previous lesson's failures impossible"; "the observation the previous lesson recorded and told you not to design against" | Two Senior sentences depend on remembering which failures and which observation the previous Lesson recorded; a direct reader cannot decode them. | Name the failures (shallow `copy()`, id-only `equals`) and the observation (the measured compiler compared an equal `List` with `equals`; do not rely on either outcome) in a clause each. | Open |
| LC-030 | `LC-XREF` | Low | `unit_derived_state_and_expensive_work` | — (Unit-level) | Unit summary | "The previous two units answered when a composable runs. This one answers what should be happening inside it when it does." | Summary opens on course position. | Open with the question itself: what should happen inside a composable body when it runs. | Open |
| LC-031 | `LC-XREF` | Medium | `unit_derived_state_and_expensive_work` | `lesson_remember_key_memoization` | CORE / paragraphs 1, 4, callout 1; PRACTICAL / paragraph 1, callout 1; SENIOR / paragraphs 2–3 | "Unit 2 introduced `remember(key)`…"; "the comparison rule the previous unit taught"; "Unit 3 left this exact function open as a problem"; "The two directions were named in Unit 2"; "the third lesson of this unit"; "Unit 3's point about counting"; "Unit 2's positional-identity point applies unchanged" | Seven Unit-number or positional references in one Lesson. Each is followed by a local restatement, so the coordinates contribute nothing but fragility. | Keep every restated fact; delete the Unit numbers. Name **Keeping Work Out of Composition** in place of "the third lesson of this unit". | Open |
| LC-032 | `LC-META` | Low | `unit_derived_state_and_expensive_work` | `lesson_remember_key_memoization` | PRACTICAL / paragraph 8, comparison 1 header | "Those three failures and the correct case were measured on a JVM target rather than reasoned about. Each calculation incremented its own counter…"; column header "What the learner gets" | Author-verification narration, and a table header that refers to "the learner" in the third person (authoring voice). | Keep the table; introduce it as "With a counter in each calculation and in the body, the four cases behave like this:". Rename the header to "Result". | Open |
| LC-033 | `LC-XREF` | Low | `unit_derived_state_and_expensive_work` | `lesson_derived_state` | SENIOR / paragraphs 4, 7 | "That is the under-specified key of the first lesson in this unit"; "How the snapshot system records and compares these reads is the next unit's subject; this is as far into it as the decision needs to go." | Positional reference plus scope narration closing an excellent mechanism paragraph. | "— the same under-specified-key failure `remember(key)` has, in a place where it is harder to see." Name **How Compose Observes State** if a pointer is wanted; drop the scope clause. | Open |
| LC-034 | `LC-META` | Medium | `unit_derived_state_and_expensive_work` | `lesson_work_outside_composition` | CORE / paragraphs 1–2, 4; PRACTICAL / paragraph 6; SENIOR / paragraphs 3–4 | "Every tool in this unit so far…"; "a question none of the previous lessons asked"; "Unit 1's execution contract, unchanged"; "this lesson's entire point"; "they belong to the effects and coroutine material later in this path. What this lesson needs from them is only the boundary"; "The costs described throughout this unit are… illustrative, not benchmarked — and nothing here claims…"; "this lesson is the one most likely to cause it" | The Lesson repeatedly positions itself in the course and disclaims its own evidence; the technical argument (placement vs frequency, responsibility vs thread) is strong without it. | Open with the question "whose responsibility is this work?"; drop "this unit/lesson" narration and the benchmarking disclaimer (keep one clause: "this is reasoning about placement, not a benchmark" if needed). Name **Effect Lifecycle and LaunchedEffect** / **Coroutine Context, Dispatchers and Concurrent Work** by title if a pointer is wanted. | Open |
| LC-035 | `LC-META` | Medium | `unit_snapshot_fundamentals` | — (Unit-level) | Unit summary | "Four units have each answered a different question…"; "This unit names that machinery and stops there."; "The second lesson takes the same model…" | The whole summary is framed by course position; a learner browsing Units sees coordinates instead of the subject. | Describe the model directly (state objects report reads and changes; observing contexts record reads; changes invalidate readers) and what it explains; drop "four units", "this unit… stops there", "the second lesson". | Open |
| LC-036 | `LC-XREF` | Medium | `unit_snapshot_fundamentals` | `lesson_snapshot_observation` | Lesson summary; CORE / paragraph 1, bullet list 1; PRACTICAL / paragraphs 3, 5; SENIOR / paragraphs 3–4 | Summary: "The last four units each ended with a rule."; "as Unit 3 explained"; "the failure Unit 2's lesson on collections and observable mutation exists for, and its three fixes live there"; "Those are the identity, stability and derived-state units' subjects"; "this is not the lesson you need"; "The next lesson is built directly on that property."; "That is the conclusion Unit 2 reached" | Summary and opening position the Lesson as a recap of the course; the rules are listed locally, so the framing is removable. Unit numbers recur throughout. | Summary: state the unifying mechanism. Keep the four symptoms as examples, not as "the last four units". Replace Unit numbers with titles only where a pointer earns its place (**Collections and Observable Mutation** for fixes). | Open |
| LC-037 | `LC-META` | Low | `unit_snapshot_fundamentals` | `lesson_snapshot_observation` | PRACTICAL / comparison 1 header | Column header "What the learner sees" | Refers to the reader as "the learner" — authoring vocabulary in a learner-facing table (it also means "the user", not the learner). | "What the user sees" or "Result". | Open |
| LC-038 | `LC-META` | Medium | `unit_snapshot_fundamentals` | `lesson_snapshot_flow` | CORE / paragraph 3, bullet list 2, callout 1; PRACTICAL / paragraph 1 | "Four facts about `Flow` are enough for this lesson to stand on its own. The full subject is taught by the Flow Fundamentals unit of this curriculum: the first two facts are its treatment of cold flows, and the last two its treatment of collection lifetime…, both linked below. Operators, hot streams and sharing, and back pressure follow in the units after it."; "in exactly the sense of the previous lesson"; "the previous lesson's model"; "That is as much as this lesson needs; the effect APIs… are a later unit's subject." | The bridge is well built (four local facts) but then narrated to the learner in authoring vocabulary — the paragraph explains the curriculum's partition of Flow. Clearest single instance of the contract's Rule 12 example. | "`snapshotFlow` returns a cold `Flow`; four facts matter here:" then the bullets; one optional pointer to **Flow Fundamentals**. Replace "the previous lesson" with "an observing context (code that records the state it reads)". | Open |
| LC-039 | `LC-META` | Low | `unit_production_screen_state_and_udf` | — (Unit-level) | Unit summary | "This unit applies the earlier state-hoisting and state-down/events-up foundations to one complete question-library screen…" | Summary describes the Unit as an application of earlier material; the list that follows is a Lesson inventory. | Say what the learner can do: place each kind of state on a real screen with the owner its readers, writers and lifetime need. | Resolved — see [Remediation Log](#remediation-log) |
| LC-040 | `LC-LEAK` | High | `unit_production_screen_state_and_udf` | `lesson_classes_of_screen_state` | CORE / paragraph 2 | "They are applied here, not derived again; `lesson_state_hoisting` contains the full ownership argument." | Internal Lesson ID shown to the learner as the place where the argument lives. | Delete the sentence (the three tests are already stated in the previous sentence); optionally "see **State Hoisting and the Lowest Sensible Owner**". | Resolved — see [Remediation Log](#remediation-log) |
| LC-041 | `LC-VOICE` | Medium | `unit_production_screen_state_and_udf` | `lesson_classes_of_screen_state` | Lesson summary; CORE / paragraph 1; SENIOR / callout 1 | Summary: "Place each value by applying the reader, writer and lifetime tests, then make the costs … concrete."; "The earlier state-hoisting lesson asks…"; "A strong answer should leave at least one meaningful production value local when its requirements say local." | Summary is written as an authoring objective (imperative task list), and the interview callout reads as a grading rubric. | Summary: describe the three kinds of state and what goes wrong when each is placed too low or too high. Callout: state the reasoning an answer needs, not what a "strong answer should" contain. | Resolved — see [Remediation Log](#remediation-log) |
| LC-042 | `LC-META` | Medium | `unit_production_screen_state_and_udf` | `lesson_stateless_screen_content` | CORE / paragraphs 1, 3; PRACTICAL / paragraphs 1–2; code 1 comment | "at the level chosen in the previous lesson"; "not the stateful/stateless overload pair from the earlier hoisting lesson"; "Preview tooling is not the subject here"; "That explains testability without teaching a testing framework."; code comment "// Observable-state conversion is the next unit's subject." | Authoring scope decisions narrated in prose and in a code comment; the paragraph about the overload pair only makes sense to someone who remembers that Lesson. | Delete scope disclaimers. Replace the overload reference with its content in a clause ("unlike a stateful convenience overload, which callers can substitute for the stateless one"). Code comment: "// Convert the owner's observable state here." | Resolved — see [Remediation Log](#remediation-log) |
| LC-043 | `LC-VOICE` | Low | `unit_production_screen_state_and_udf` | `lesson_stateless_screen_content` | CORE / callout 1 | "Screen composable means wiring boundary. Content composable means rendering plus reporting intent." | Telegraphic definition-list voice typical of this Unit. | One natural sentence: "The screen composable wires owners to content; the content composable renders values and reports intent." | Resolved — see [Remediation Log](#remediation-log) |
| LC-044 | `LC-LEAK` | High | `unit_production_screen_state_and_udf` | `lesson_screen_state_and_ui_events` | SENIOR / paragraph 2 | "This connects to `lesson_stability_and_skipping` without re-teaching stability." | Internal Lesson ID, combined with authoring language ("without re-teaching"). | State the equality point directly (it already follows in the next sentence); optionally name **Stability and Skipping**. | Resolved — see [Remediation Log](#remediation-log) |
| LC-045 | `LC-META` | Medium | `unit_production_screen_state_and_udf` | `lesson_screen_state_and_ui_events` | CORE / paragraphs 1–3; SENIOR / paragraphs 2–3 | "The type is an example that makes the contract visible, not a prescription… Detailed state modelling belongs to later architecture curriculum."; "The claim is bounded to the screen contract"; "not a new definition of UDF"; "This lesson needs no `@Stable`…"; "but this unit stops at the Compose boundary… a later design problem" | About a fifth of a short Lesson explains what the Lesson does not claim and where other material lives; the author is defending scope rather than teaching. | Keep the value-and-callback argument; delete the disclaimers and scope statements. If a pointer to state modelling helps, name **Modelling the Current UI State**. | Resolved — see [Remediation Log](#remediation-log) |
| LC-046 | `LC-META` | High | `unit_production_screen_state_and_udf` | `lesson_screen_state_owner_boundary` | Lesson title; Lesson summary; SENIOR / paragraph 3 | Title "The Screen-Level Owner as a Bounded Bridge"; summary "…and stop before application architecture begins"; "This is where the Compose curriculum deliberately stops… The bounded bridge here is only the contract at the Composition edge." | The authoring contract's Teach/Bridge vocabulary ("bounded bridge") is the learner-visible Lesson title, and the Lesson closes by describing where the curriculum stops. A learner cannot tell what "bounded bridge" means. | Retitle around the subject, e.g. "Moving State Outside the Composition: Owner vs Lifetime". Replace the closing paragraph with the ownership/lifetime/persistence distinction it already states in S para 2. | Resolved — see [Remediation Log](#remediation-log) |
| LC-047 | `LC-LEAK` | High | `unit_production_screen_state_and_udf` | `lesson_screen_state_owner_boundary` | CORE / paragraph 3 | "The earlier `lesson_remember_saveable` provides the lifetime ladder; this lesson applies its central question…"; "introduced in their owning curricula" | Internal Lesson ID plus curriculum-ownership language in the same paragraph. | "Process death needs saved state or durable storage (see **`rememberSaveable` and State That Must Survive**)." | Resolved — see [Remediation Log](#remediation-log) |
| LC-048 | `LC-META` | Medium | `unit_production_screen_state_and_udf` | `lesson_screen_state_owner_boundary` | CORE / comparison 1; PRACTICAL / paragraphs 1–2, 4 | Row "The learner navigates forward…"; "remain while their entry remains on the area's back stack"; "The snippet is evidence about ownership, not a Navigation lesson."; "is the next unit's subject, so no collection mechanism belongs here" | "The learner" and "the area's back stack" describe this learning application's own navigation (Rule 11) rather than a generic example; two further scope disclaimers. | "The user navigates forward"; "remain while their entry remains on the back stack". Drop the two disclaimers. | Resolved — see [Remediation Log](#remediation-log) |
| LC-049 | `LC-VERBOSE` | Low | `unit_observable_state_collection` | — (Unit-level) | Unit summary | "This unit follows that outside value across the boundary: collection converts it…, the returned state drives…, the collection has…, and lifecycle-aware collection adds…" | Summary enumerates the four Lessons in one long sentence. | Keep the first sentence (a Flow is observable, but not to Compose); compress the rest to "how to convert it, what the collection costs, and when it should pause". | Resolved — see [Remediation Log](#remediation-log) |
| LC-050 | `LC-LEAK` | High | `unit_observable_state_collection` | `lesson_external_state_in_compose` | CORE / paragraph 3; PRACTICAL / paragraph 2 | "the snapshot observation model from `lesson_snapshot_observation`"; "That contract belongs to `lesson_state_flow`; this lesson does not re-teach hot streams…"; "remains the subject of `lesson_snapshot_flow`" | Three internal Lesson IDs; one paragraph exists only to say where other material lives. | Replace the IDs with the fact needed ("reads of Compose state during composition are recorded as dependencies"); name **How Compose Observes State**, **`StateFlow`: One Current Value** and **`snapshotFlow` and Crossing Into Flow** by title only if a pointer helps; delete "does not re-teach". | Resolved — see [Remediation Log](#remediation-log) |
| LC-051 | `LC-XREF` | Low | `unit_observable_state_collection` | `lesson_external_state_in_compose` | CORE / paragraph 1; PRACTICAL / paragraphs 1, 3 | "Unit 7 ended with a screen-level owner outside the Composition."; "This is the same question-library boundary as Unit 7, one step later."; "the previewable, reusable contract Unit 7 established"; "The next lesson names…" | Unit-number coordinates; the owner and content contract are restated locally anyway. | Start from the owner exposing `StateFlow`; drop Unit numbers. | Resolved — see [Remediation Log](#remediation-log) |
| LC-052 | `LC-LEAK` | High | `unit_observable_state_collection` | `lesson_collect_as_state` | CORE / paragraphs 2, 4; PRACTICAL / paragraph 2 | "already taught in `lesson_state_flow`"; "`lesson_composition_and_recomposition` owns the deeper recomposition model"; "Operator semantics remain E24's subject." | Two internal Lesson IDs and a backlog epic key ("E24") in learner prose. | Delete the three sentences; the argument does not need them. Optionally one pointer to **Flow Composition, Timing and Failure** for operators. | Resolved — see [Remediation Log](#remediation-log) |
| LC-053 | `LC-META` | Medium | `unit_observable_state_collection` | `lesson_collect_as_state` | Lesson summary; PRACTICAL / paragraph 3, callout 1; SENIOR / paragraph 1 | Summary "Use collectAsState as the purpose-built bridge… Compare…, distinguish…, trace…, and treat…"; "This is not a general dispatcher lesson… the Flow curriculum owns context preservation"; "later units make that distinction precise"; "The verified Compose runtime 1.11.2 source… not the mental model learners should lead with: Unit 11 teaches `produceState`…" | Objective-style summary, curriculum-ownership sentences, and author-to-author notes ("learners should lead with") in Senior prose. | Summary as a description; drop ownership and "later units" sentences; Senior: "Internally the Flow overload is built on `produceState`, which is why identity and context restart collection." Name **`produceState`: a Composition-Scoped Producer** only if needed. | Resolved — see [Remediation Log](#remediation-log) |
| LC-054 | `LC-VOICE` | Low | `unit_observable_state_collection` | `lesson_collect_as_state` | CORE / paragraph 2; PRACTICAL / paragraphs 1, 3 | "the resolved Compose runtime's overload"; "The resolved runtime keys its state producer…"; "the resolved implementation collects…" | "Resolved" is dependency-resolution vocabulary from this build; to a learner it is noise or a riddle. | "In Compose runtime 1.11.2, …" or simply state the API contract. | Resolved — see [Remediation Log](#remediation-log) |
| LC-055 | `LC-LEAK` | High | `unit_observable_state_collection` | `lesson_collection_lifetime_and_cost` | CORE / paragraph 3; SENIOR / paragraph 2 | "`lesson_flow_collection_lifetime` and `lesson_cooperative_cancellation` own those mechanics"; "`lesson_sharing_cold_flows` teaches `stateIn`, `shareIn`…"; "That is the whole sharing bridge this unit needs." | Three internal Lesson IDs with ownership phrasing and the authoring term "bridge". The local explanation (cooperative cancellation cannot interrupt blocking work; collection policy vs sharing policy) is already sufficient. | Delete the ID sentences; keep the local facts. Optional pointers by title: **Cancellation Is Cooperative**, **Sharing Cold Flows with `stateIn` and `shareIn`**. | Resolved — see [Remediation Log](#remediation-log) |
| LC-056 | `LC-META` | Medium | `unit_observable_state_collection` | `lesson_lifecycle_aware_collection` | Lesson summary; SENIOR / paragraphs 1, 3 | Summary "…from the actual Android, desktop, iOS and web host mappings used by this KMP project"; "This lesson uses that relationship to verify behavior, while Unit 11 remains responsible for teaching `produceState` itself and lifecycle curriculum remains responsible for `repeatOnLifecycle`…"; "These overloads, defaults and mappings were checked against… A later dependency update should re-read those contracts rather than preserve this table by assumption." | Project coupling in the summary (Rule 11), curriculum-responsibility narration, and a maintenance instruction written for authors. | Summary: "…as each host maps window, scene or page activity onto lifecycle states". Senior: keep the two-gate mechanism, drop responsibility narration; convert the maintenance note to a learner-useful caveat ("the lifecycle artifact is beta at 2.11.0-beta01; host mappings may change"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-057 | `LC-LEAK` | High | `unit_observable_state_collection` | `lesson_lifecycle_aware_collection` | SENIOR / paragraph 2 | "A shared source follows the owner scope and sharing policy taught in `lesson_sharing_cold_flows`." | Internal Lesson ID. | "…follows its owner scope and `SharingStarted` policy." Optionally name **Sharing Cold Flows with `stateIn` and `shareIn`**. | Resolved — see [Remediation Log](#remediation-log) |
| LC-058 | `LC-TONE` | Low | `unit_observable_state_collection` | — (Unit-level) | Callouts across all four Lessons | "Do not overstate the defect…"; "Do not turn that into…"; "Do not make a remembered `MutableState`…"; "Do not use 'navigated away' as proof…"; "Do not reduce the decision to…" | Every Lesson's callouts open with a corrective imperative; together they read as a sequence of corrections to an imaginary learner. | Keep the precise content; open with the fact ("The precise defect is…", "Continuous collection is sometimes the requirement…"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-059 | `LC-LEAK` | High | `unit_effect_lifecycle_and_launched_effect` | `lesson_why_effects_are_controlled` | CORE / paragraph 3 | "The execution rules themselves belong to `lesson_composable_execution` and `lesson_composition_and_recomposition`. This unit applies them." | Two internal Lesson IDs with ownership phrasing; the rules are restated in the very next sentence. | Delete the first two sentences; keep the restated execution rules. | Resolved — see [Remediation Log](#remediation-log) |
| LC-060 | `LC-META` | Low | `unit_effect_lifecycle_and_launched_effect` | `lesson_why_effects_are_controlled` | PRACTICAL / paragraph 1; SENIOR / paragraphs 1–2 | "This is conditional wording on purpose"; "later units provide the remaining mechanisms"; "Choosing the longer-lived owner is architecture material outside this unit." | The author explains their own wording choice and where the curriculum continues. | Drop "on purpose"; end S para 1 on the four problems; replace the last sentence with the engineering point ("then the work needs an owner that outlives the composition, such as a screen-level state holder"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-061 | `LC-LEAK` | High | `unit_effect_lifecycle_and_launched_effect` | `lesson_launched_effect` | PRACTICAL / paragraphs 2–3 | "the ordinary coroutine model from `lesson_coroutine_scope_ownership` and `lesson_cooperative_cancellation`"; "`lesson_composable_identity` contains the full identity model; here it explains…" | Three internal Lesson IDs; the paragraph already explains cooperative cancellation and which call site owns the effect. | Delete the ID clauses; optionally name **Cancellation Is Cooperative** or **Composable Identity** by title. | Resolved — see [Remediation Log](#remediation-log) |
| LC-062 | `LC-META` | Low | `unit_effect_lifecycle_and_launched_effect` | `lesson_launched_effect` | SENIOR / paragraph 1 | "The resolved Compose runtime 1.11.2 contract…"; "That is enough context detail for this unit… dispatcher selection remains coroutine curriculum." | Scope narration and build vocabulary ("resolved"). | "In Compose runtime 1.11.2 the effect runs in the Composition's applying context; which dispatcher that is depends on the host." Drop the curriculum clause. | Resolved — see [Remediation Log](#remediation-log) |
| LC-063 | `LC-LEAK` | High | `unit_effect_lifecycle_and_launched_effect` | `lesson_effect_keys_as_dependencies` | PRACTICAL / paragraphs 1, 3 | "`lesson_remember_key_memoization` owns the memoization half of the model."; "`lesson_stability_and_skipping` owns stability and strong skipping; the point here is…" | Two internal Lesson IDs with ownership phrasing. | Delete the ownership clauses; the comparison table already carries the point. | Resolved — see [Remediation Log](#remediation-log) |
| LC-064 | `LC-META` | Low | `unit_effect_lifecycle_and_launched_effect` | `lesson_effect_keys_as_dependencies` | CORE / paragraph 2 | "A later unit solves that latest-value problem; this unit stops at recognizing it." | Scope narration where a one-clause pointer would help more. | "(`rememberUpdatedState` handles that case; see **Reading the Current Value Without Restarting**.)" | Resolved — see [Remediation Log](#remediation-log) |
| LC-065 | `LC-META` | Low | `unit_effect_lifecycle_and_launched_effect` | `lesson_effect_key_failures` | Lesson summary; SENIOR / paragraph 3 | Summary ends "Diagnose both directions from behavior rather than from key-list style."; "Unit 10 starts from that tension and supplies the appropriate mechanism. This lesson establishes the problem and stops before teaching the solution." | Imperative objective in the summary; Unit number and scope narration where naming the mechanism would be one clause. | Name the mechanism: "`rememberUpdatedState` resolves that tension (see **Reading the Current Value Without Restarting**)." | Resolved — see [Remediation Log](#remediation-log) |
| LC-066 | `LC-TONE` | Low | `unit_effect_lifecycle_and_launched_effect` | — (Unit-level) | Callouts in all four Lessons | "Do not fix this by inventing a `Boolean`…"; "Do not say 'recomposition reruns LaunchedEffect'."; "Say 'the effect enters Composition', not 'the composable was called'."; "Do not report 'a new object caused the restart' until…" | Same corrective-imperative callout pattern as the previous Unit; tells the learner what not to say rather than explaining. | Lead with the fact ("Recomposition can execute the call again while the effect keeps running…"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-067 | `LC-XREF` | Low | `unit_latest_values_and_event_driven_work` | `lesson_remember_updated_state` | CORE / paragraph 1; SENIOR / paragraphs 1–2 | "Unit 9 gave effect keys a lifetime meaning: …"; "Unit 9 explains how equality and current compiler memoization…"; "In the resolved Compose runtime 1.11.2 source… the public purpose is the durable lesson" | Unit-number references (the key rule is restated locally) and build/authoring vocabulary. | Open with the restated key rule, no Unit number; name **What an Effect's Keys Declare** for the equality detail; "In Compose runtime 1.11.2 the implementation is equivalent to…". | Resolved — see [Remediation Log](#remediation-log) |
| LC-068 | `LC-VOCAB` | Low | `unit_latest_values_and_event_driven_work` | `lesson_remember_updated_state` | PRACTICAL / comparison 1 header | Column header "Correct shape" | "Shape" as a header for what is a code pattern; "correct" also overstates two valid options that the paragraph after says are both fine. | "Implementation" or "Code". | Resolved — see [Remediation Log](#remediation-log) |
| LC-069 | `LC-LEAK` | High | `unit_latest_values_and_event_driven_work` | `lesson_remember_coroutine_scope` | CORE / paragraph 2; PRACTICAL / paragraph 3 | "The builder and returned `Job` mechanics are covered by `lesson_coroutine_builders`."; "This lesson does not prescribe a concurrency policy; `lesson_coroutine_scope_ownership` and the structured-concurrency material own the deeper Job rules." | Two internal Lesson IDs with ownership phrasing. | Delete; if helpful, "`launch` returns a `Job` (see **Starting Coroutines: `launch`, `async` and `runBlocking`**)". | Resolved — see [Remediation Log](#remediation-log) |
| LC-070 | `LC-META` | Low | `unit_latest_values_and_event_driven_work` | `lesson_remember_coroutine_scope` | PRACTICAL / paragraph 2; SENIOR / paragraphs 1–2 | "recreates the uncontrolled-body-work defect from Unit 9"; "The resolved runtime 1.11.2 KDoc…"; "In the resolved implementation…" | Unit number (the defect is restated in the same sentence) and build vocabulary. | Drop "from Unit 9"; "In Compose runtime 1.11.2, …". | Resolved — see [Remediation Log](#remediation-log) |
| LC-071 | `LC-META` | Low | `unit_latest_values_and_event_driven_work` | `lesson_who_owns_the_trigger` | PRACTICAL / paragraph 2, callout 1; SENIOR / paragraph 1 | "This comparison is deliberately bounded: it does not define an application-wide transient-event delivery architecture…"; "Choosing and designing that owner belongs to later architecture curriculum; this unit does not turn `viewModelScope`… into a default escape hatch."; "Do not reduce the rule to…" | Scope defence and curriculum-ownership narration at the ends of two strong sections. | Drop the disclaimers; end S para 1 at "Move it to an owner whose lifetime actually matches the requirement." Optionally point to **Work Whose Lifetime Is the Owner's**. | Resolved — see [Remediation Log](#remediation-log) |
| LC-072 | `LC-XREF` | Medium | `unit_cleanup_synchronization_and_producers` | `lesson_disposable_effect` | CORE / paragraph 1; PRACTICAL / callout 1; SENIOR / paragraph 3 | "Unit 9 and Unit 10 asked who owns a coroutine."; "either a publication, which is the next lesson's subject, or a coroutine, which is Unit 9's"; "Unit 10's `rememberUpdatedState` solves the value half" | Unit numbers used as the names of concepts; in the callout the learner must decode "Unit 9's" to know the alternative is `LaunchedEffect`. | Name the mechanisms instead of Units: "…is either a publication (`SideEffect`) or suspend work (`LaunchedEffect`)"; "`rememberUpdatedState` solves the value half". | Resolved — see [Remediation Log](#remediation-log) |
| LC-073 | `LC-LEAK` | High | `unit_cleanup_synchronization_and_producers` | `lesson_disposable_effect` | PRACTICAL / paragraph 5; SENIOR / paragraph 1 | "`lesson_effect_keys_as_dependencies` owns the key-comparison rules; this is what getting them wrong does to a registration."; "`lesson_cancellation_cleanup_and_timeouts` already covers how a cancelled coroutine unwinds" | Two internal Lesson IDs with ownership phrasing. | Delete the first clause; replace the second with "(its `finally` runs as the coroutine unwinds)". | Resolved — see [Remediation Log](#remediation-log) |
| LC-074 | `LC-VOCAB` | Low | `unit_cleanup_synchronization_and_producers` | `lesson_disposable_effect` | CORE / paragraphs 2–3 | "it has a different shape from 'run this suspend work…'"; "`DisposableEffect` is the mechanism whose shape matches that problem" | Two vague "shape" uses where the distinction is precisely the lifetime contract (release vs cancellation). | "It is a different problem from…: the question is who gives the registration back"; "`DisposableEffect` is built for that problem." | Resolved — see [Remediation Log](#remediation-log) |
| LC-075 | `LC-XREF` | Low | `unit_cleanup_synchronization_and_producers` | `lesson_side_effect_publication` | PRACTICAL / paragraphs 1, 3; SENIOR / paragraph 3 | "Unit 9 established that Compose may… discard the result"; "The bridge back is a question about the trigger, which Unit 10 already made the deciding one."; P para 3 ends "Note carefully what that does and does not say." with nothing following | Unit numbers (facts restated locally) and a dangling sentence that promises a distinction the paragraph never states. | Drop Unit numbers and "bridge back"; either state what the observation does not show (it does not show the publication count is tied to state changes) or delete the sentence. | Resolved — see [Remediation Log](#remediation-log) |
| LC-076 | `LC-XREF` | Medium | `unit_cleanup_synchronization_and_producers` | `lesson_produce_state` | CORE / paragraphs 1, 3; PRACTICAL / paragraph 1; SENIOR / paragraph 4 | "Unit 8 answered one half of the observation problem"; "all four come from Unit 9's effect model rather than from anything new"; "the question this Lesson exists for"; "the purpose-built conversion is Unit 8's"; "which is the next Lesson's subject" | Unit numbers substitute for mechanism names ("Unit 9's effect model" = `LaunchedEffect` lifetime), so the claim is only legible with the course map. | "The contract is `LaunchedEffect`'s: launched on entry, cancelled on exit, restarted when a key changes." Replace "Unit 8's" with `collectAsState`. Name **A Flow Below the UI, or a Producer at the Boundary?** if a pointer is wanted. | Resolved — see [Remediation Log](#remediation-log) |
| LC-077 | `LC-LEAK` | High | `unit_cleanup_synchronization_and_producers` | `lesson_flow_adapter_or_compose_producer` | SENIOR / paragraph 2 | "`lesson_flow_builders_and_callback_adapters` already covers how to build that adapter — this Lesson does not repeat it." | Internal Lesson ID plus "does not repeat it". | "(`callbackFlow` builds that adapter; see **Flow Builders and Adapting Callback APIs**.)" | Resolved — see [Remediation Log](#remediation-log) |
| LC-078 | `LC-XREF` | Low | `unit_cleanup_synchronization_and_producers` | `lesson_flow_adapter_or_compose_producer` | CORE / paragraph 1; PRACTICAL / paragraphs 1–3; SENIOR / paragraph 4 | "The previous Lesson's producer suspended"; "the first Lesson's `DisposableEffect`"; "the producer shape is what that code actually wanted"; "the honest mechanism"; "Unit 10's trigger distinction"; "Unit 8's collection"; "the subject of a later unit in this path" | Positional references (all restated locally), one "shape" (the contract's own example of the habit) and one "honest". | Name mechanisms directly (`collectAsState`, `DisposableEffect`); "a state producer is what that code wanted"; "`DisposableEffect` is the right mechanism"; name **Choosing the Smallest Sufficient Mechanism** for the later pointer. | Resolved — see [Remediation Log](#remediation-log) |
| LC-079 | `LC-VOCAB` | Low | `unit_cleanup_synchronization_and_producers` | `lesson_side_effect_publication` | SENIOR / paragraph 4 | "Keep it to assignment-shaped publication." | Vague coinage. | "Keep it to simple assignments." | Resolved — see [Remediation Log](#remediation-log) |
| LC-080 | `LC-META` | Low | `unit_production_ui_effects_and_selection` | — (Unit-level) | Unit summary | "Every mechanism in this path has now been met on its own… This unit answers that…" | Opens on course position. | "Given a requirement, which Compose mechanism does it need?" then the four facts. | Open |
| LC-081 | `LC-META` | Medium | `unit_production_ui_effects_and_selection` | `lesson_choosing_a_compose_mechanism` | CORE / paragraph 1; PRACTICAL / paragraph 7; SENIOR / paragraph 3 | "Five units have introduced these mechanisms… That decision is this lesson, and it introduces no new API."; "the rest of the screens this path has built towards. Every outcome in the table is a mechanism an earlier unit owns"; "What happens next is deliberately not this unit's decision… belong to the architecture curriculum. This unit's contribution is the diagnosis…" | Opening, table introduction and Senior close all narrate the course; the decision model itself is excellent. | Open with the requirement-driven question; introduce the table as "The four facts applied to a practice-question screen"; end S para 3 at "…so the Composition cannot be the owner." | Open |
| LC-082 | `LC-VOICE` | Low | `unit_production_ui_effects_and_selection` | `lesson_choosing_a_compose_mechanism` | PRACTICAL / paragraphs 6, 8 | "the shape worth noticing is what is absent: no remembered flag"; "the sixth and seventh are one countdown… The twelfth row is the eleventh with one fact changed" | The authoring contract's own example of rhetorical "shape"; ordinal row references into a 13-row table force counting. | "There is no remembered flag and nothing for an effect to observe." Refer to rows by requirement ("the two countdown rows", "the shared-source row"). | Open |
| LC-083 | `LC-META` | Low | `unit_production_ui_effects_and_selection` | `lesson_transient_ui_effects` | CORE / paragraph 2; PRACTICAL / paragraphs 3–4, 6; SENIOR / paragraphs 2–3 | "the previous lesson's decision starts with rendering"; "as the effect unit described"; "the one the trigger lesson asked about a similar shape"; "the navigation curriculum's subject"; "That question is the next lesson"; "Where this stops is deliberate… the architecture curriculum owns it." | Positional and ownership narration around a strong condition-vs-occurrence argument. | Drop the narration; name **Composition-Driven or Event-Driven?** for the trigger contrast; replace "a similar shape" with "a similar flag-driven snackbar". | Open |
| LC-084 | `LC-META` | Medium | `unit_production_ui_effects_and_selection` | `lesson_transient_effect_delivery` | PRACTICAL / paragraphs 1–2, 6–7, comparison 1; SENIOR / paragraphs 2–4 | "The coroutines and Flow path already settled the emitter's half… the job here is to apply it rather than rebuild it."; "The stream lesson measured exactly that on the same toolchain, so this lesson cites it rather than re-deriving it."; "the shape the stream curriculum calls current-value semantics"; "an architecture decision this unit does not make"; "and stop, because designing that owner is the architecture curriculum's work and not this unit's"; "That contrast is the reason this lesson exists beside the stream material rather than repeating it."; "is what this unit is for" | Roughly a sixth of the Lesson explains why the Lesson exists and which curriculum owns what; the Senior section in particular is an authoring rationale. Key facts ("a `true` from `tryEmit` is not delivery") are stated but framed as citations of other Lessons, so the learner is told where proof lives rather than given it. | State the `tryEmit`/replay facts in the Lesson's own voice (one sentence each); delete the ownership and existence-justification sentences; end on the three consequences. Optional pointer: **Choosing a Stream Abstraction by Delivery Guarantees**. | Open |
| LC-085 | `LC-CLARITY` | Observation | `unit_production_ui_effects_and_selection` | `lesson_transient_effect_delivery` | Lesson title | "What Delivery Guarantee Does This Occurrence Need?" vs Unit 24 "What Guarantee Does This Occurrence Need?" | Two Lessons in different Topics have near-identical titles; once references use human titles (the recommended fix), these two become easy to confuse. | Differentiate the titles when either Unit is rewritten (e.g. Compose side: "One-Off UI Effects When No Screen Is Present"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-086 | `LC-META` | Low | `unit_coroutines_and_structured_concurrency` | — (Unit-level) | Unit summary | "in the order the ideas depend on one another"; "Where code actually runs, how cancellation works, and what a failure does are deliberately later units — all three assume this material." | Course-order narration in an otherwise excellent summary. | End after the structured-concurrency clause; drop the "deliberately later units" sentence. | Open |
| LC-087 | `LC-XREF` | Medium | `unit_coroutines_and_structured_concurrency` | `lesson_suspension_and_blocking` | PRACTICAL / paragraph 2; SENIOR / paragraphs 2, 4 | "The third question has a unit of its own and is deliberately left there."; "the next unit's subject"; "removes the ground the next unit stands on"; "which is exactly why the dispatchers unit separates the pools"; "which the cancellation unit later builds on"; "the third unit's subject rather than this one's" | Six forward pointers by position ("next", "third", "the dispatchers unit"). The bridging content is local and good ("For now it is enough to know that the dispatcher…"), so the pointers are pure navigation and should use titles if kept. | Keep the local bridges; convert at most one pointer per topic to a title (**Dispatchers and Where Code Actually Runs**, **Cancellation Is Cooperative**) and delete the rest. | Open |
| LC-088 | `LC-VOCAB` | Low | `unit_coroutines_and_structured_concurrency` | `lesson_suspension_and_blocking` | PRACTICAL / paragraph 3 | "Two functions with the same shape can have opposite answers." | The contract's example sentence: the meaning is "the same signature". | "Two functions with the same signature…" | Open |
| LC-089 | `LC-META` | Medium | `unit_coroutines_and_structured_concurrency` | `lesson_coroutine_builders` | CORE / paragraphs 3–4, code 1 comment; PRACTICAL / paragraph 3; SENIOR / paragraphs 2–3 | "which the next lesson is about"; "For this lesson that is the whole of `async`"; code comment "is the subject two lessons from here"; "There is a second half to this that the failure unit owns… it is deliberately not answered here"; "the next unit's job"; "not part of the intent decision this lesson is about" | Scope narration in prose and in a code comment; the dropped-`Deferred` failure point is announced and then withheld, which leaves an unresolved worry. | State the failure point in one clause ("an unawaited `async` that fails still fails its parent scope") and point to **How a Coroutine Failure Travels**; drop "this lesson"/"next unit" phrasing; code comment: "// `scope` is owned elsewhere; see below." | Open |
| LC-090 | `LC-VOCAB` | Low | `unit_coroutines_and_structured_concurrency` | `lesson_coroutine_builders` | CORE / paragraph 6, callout 1 | "`launch(dispatcher) { }` is the same shape with one ordinary argument in front"; "not from the shape of the surrounding code" | "Shape" for call syntax and for "context". | "the same call with one argument in front"; "Pick the builder from the intent, not from habit." | Open |
| LC-091 | `LC-META` | Medium | `unit_coroutines_and_structured_concurrency` | `lesson_job_and_parent_child` | CORE / paragraphs 3, 5; PRACTICAL / paragraphs 4–5; SENIOR / paragraph 3 | "a trap worth knowing about and the context unit's to take apart. Nothing in this unit does it."; "One more fact belongs here in a single sentence, because the next unit builds on it… is the next unit's first lesson."; "deserves naming here and nothing more than naming… is the third unit's entire subject, and nothing in this unit depends on the answer. Until then…"; "that use carries rules this unit does not cover. Keeping the two apart avoids importing an answer before the question has been posed." | The Lesson repeatedly explains what it is not teaching and why; three of these passages are longer than the fact they defer. | State the deferred facts in one clause each (a `Job` is a context element; cancellation travels along parent-child edges and is cooperative) and stop; optional title pointers to **`CoroutineContext` and What Children Inherit** and **Cancellation Is Cooperative**. | Open |
| LC-092 | `LC-VOCAB` | Low | `unit_coroutines_and_structured_concurrency` | `lesson_coroutine_scope_ownership` | Lesson summary; PRACTICAL / paragraphs 1, 6; SENIOR / paragraph 2 | Summary "when the honest answer is 'nothing in particular'"; "Here is the shape with no owner."; "the one worth recognising as the shape to imitate"; "Read the shape of that warning" | Three "shape" uses (one rhetorical) and one "honest" in an otherwise direct Lesson. | "Here is a scope with no owner."; "the model to imitate"; delete "Read the shape of that warning —"; summary "when the answer is 'nothing in particular'". | Open |
| LC-093 | `LC-XREF` | Low | `unit_coroutines_and_structured_concurrency` | `lesson_coroutine_scope_ownership` | CORE / paragraph 2; PRACTICAL / paragraphs 2, 6; SENIOR / paragraph 4 | "the `Job` from the previous lesson"; "the failure unit teaches it properly"; "the architecture curriculum's subject, not this one's"; "are the failure unit's subject; ownership does not settle them, and neither does this lesson" | Positional pointers; the local statements are sufficient. | Drop; one title pointer to **`SupervisorJob`, `supervisorScope` and the Limits of Isolation** if useful. | Open |
| LC-094 | `LC-XREF` | Low | `unit_coroutines_and_structured_concurrency` | `lesson_structured_concurrency` | CORE / paragraphs 1, 3, bullet list 1; PRACTICAL / paragraph 5; SENIOR / paragraph 3, callout 1 | "Each earlier lesson contributed a piece."; "the completing state from the previous lesson"; "the reason the previous lesson spent so long on ownership"; "that is the context unit's subject"; "a separate decision the next unit makes"; "It is the failure unit's subject and nothing in this unit depends on it."; "telling those two apart is the next unit's job"; "the detached-`launch` shape" | Positional pointers throughout; the recap in CORE para 1 is good and stands without "each earlier lesson". | Keep the recap as plain statements; name `withContext` instead of "a close relative… the next unit's job"; drop the rest or use titles. | Open |
| LC-095 | `LC-CLARITY` | Medium | `unit_context_dispatchers_and_concurrency` | — (Unit-level) | Unit summary; all four Lessons | "Unit 1 supplies the lifetime model"; "Unit 1 left two facts side by side"; "what it handles is a Unit 3 question"; "Use Unit 3 for cancellation, failure handling and supervision rules."; "Unit 1 introduced coroutineScope"; "Unit 3 owns the exact propagation and supervision rules" | "Unit 1" and "Unit 3" here are numbered within the Coroutines topic, while Compose Lessons use "Unit 1" for **Thinking in Compose**. The same coordinate names different Units depending on where it is read, so these references are ambiguous, not merely fragile. | Replace every "Unit 1" with the fact or with **Coroutine Fundamentals and Structured Concurrency**, and "Unit 3" with **Cancellation, Failure and Coordination**; most occurrences can simply be deleted because the fact is restated. | Open |
| LC-096 | `LC-VERBOSE` | Medium | `unit_context_dispatchers_and_concurrency` | — (Unit-level) | L14.1 CORE / paragraph 4, PRACTICAL / paragraph 1; L14.2 PRACTICAL / paragraph 1; L14.3 PRACTICAL / paragraphs 1, 4; L14.4 CORE / paragraph 2 | "This is common Kotlin code with kotlinx.coroutines imports and `kotlin.coroutines.ContinuationInterceptor`."; "The blocking call is a placeholder for an existing Android/JVM API; Main and IO here are explicitly platform choices."; "These examples are common Kotlin fragments with application types and main-safe suspending load functions supplied by the surrounding application. The final AccountOverview constructor only combines two values; it is not a Flow operation." | Repeated source-set and provenance disclaimers before code. They record the author's KMP-correctness checks rather than help the reader, and they interrupt the setup of each example. | Keep one short platform note where the platform actually changes the answer (IO availability); move the rest into a code comment such as `// Android/JVM` or delete. | Open |
| LC-097 | `LC-VOICE` | Low | `unit_context_dispatchers_and_concurrency` | — (Unit-level) | Lesson summaries; SENIOR bullet lists in L14.1–L14.4 | Summaries "Read a coroutine's effective context…", "Choose execution policy from the work…", "Decide who should choose…", "Compare sequential and concurrent versions…"; each Senior section ends in an imperative checklist ("Choose Default for…", "Use withContext for…") | Objective-style summaries and checklist endings read like a lesson plan; the preceding prose already makes each point. | Summaries as descriptions of the subject. Keep a checklist only where it adds a genuinely new synthesis (the L14.1 three-line review method does; the L14.2 list repeats the table). | Open |
| LC-098 | `LC-META` | Low | `unit_context_dispatchers_and_concurrency` | `lesson_dispatchers` | CORE / paragraph 4; PRACTICAL / paragraph 5; SENIOR / paragraphs 4–5 | "The performance and Android-platform curricula own the detailed diagnosis and platform deadlines."; "need their own reasoning outside this lesson"; "the testing curriculum owns how to do that"; "none is required to answer this lesson's workload decision" | Ownership claims by curricula that do not exist in the course, plus scope narration. | Delete; the paragraphs are complete without them. | Open |
| LC-099 | `LC-VOCAB` | Low | `unit_context_dispatchers_and_concurrency` | `lesson_dispatchers` | Unit summary; CORE / comparison 1 header | "chooses dispatchers from the shape of the work"; header "Work shape" | "Shape" for "kind of work" (CPU, blocking, UI). | "from the kind of work"; header "Kind of work". | Open |
| LC-100 | `LC-META` | Low | `unit_context_dispatchers_and_concurrency` | `lesson_with_context_and_main_safety` | PRACTICAL / paragraph 3; SENIOR / paragraphs 1, 4–5 | "This is a bounded repository responsibility, not a prescription for a particular application architecture."; "as the previous lesson established"; "Those are separate lessons."; "is beyond this lesson" | Scope defences; the S para 4 pointer to **Keeping Work Out of Composition** already uses a human title and is useful. | Keep the Compose pointer; delete the other scope sentences. | Open |
| LC-101 | `LC-VOCAB` | Low | `unit_context_dispatchers_and_concurrency` | `lesson_sequential_and_concurrent_work` | PRACTICAL / paragraph 6 | "before using this shape" | Vague reference to the `map { async }` + `awaitAll()` pattern just described. | "before starting one child per item". | Open |
| LC-102 | `LC-XREF` | Low | `unit_cancellation_failure_and_coordination` | — (Unit-level) | Unit summary | "Units 1 and 2 built structured work and decided where it runs. This unit asks…"; "The order is one argument rather than five API pages." | Topic-local Unit numbers (which collide with Compose numbering) and a sentence defending the Unit's structure. | Open with the question (what happens when structured work is stopped or fails); drop the numbers and the last sentence. | Open |
| LC-103 | `LC-XREF` | Medium | `unit_cancellation_failure_and_coordination` | — (Unit-level) | L15.1 CORE / paragraph 1, SENIOR / paragraph 4; L15.2 SENIOR / paragraph 3; L15.3 CORE / paragraph 2, SENIOR / paragraphs 2, 4; L15.4 CORE / paragraph 1, PRACTICAL / paragraph 2, SENIOR / paragraph 1; L15.5 SENIOR / paragraph 3 | "Unit 1 introduced cancellation… and stopped there. This Lesson answers the question that was left open"; "the subject of the third Lesson in this Unit"; "Unit 1's ownership argument"; "Unit 1's hierarchy"; "the next Lesson"; "the first Lesson in this Unit"; "The previous Lesson established"; "in the vocabulary of the previous Lesson"; "Unit 2 taught the context half of the launch(SupervisorJob()) trap"; "the hot and cold streams material later in this curriculum" | Every Lesson uses topic-local Unit numbers or Lesson ordinals; each is followed by a local restatement except "in the vocabulary of the previous Lesson" (propagation path), which is defined two Lessons away. | Delete ordinals and numbers; define "propagation path" in a clause where it is used in L15.4; name **`CoroutineContext` and What Children Inherit** and **Hot and Cold: When Production Happens** by title where a pointer helps. | Open |
| LC-104 | `LC-VOICE` | Low | `unit_cancellation_failure_and_coordination` | — (Unit-level) | L15.1 CORE / paragraphs 1–2, 5, SENIOR / paragraph 3; L15.2 PRACTICAL / paragraph 2, SENIOR / paragraph 1; L15.3 SENIOR / paragraphs 1, 4; L15.5 SENIOR / paragraph 2 | "The honest answer is that cancelling is a request" (the contract's own example); "Three boundaries keep it honest"; "The honest sequence is…"; "The sequence is worth holding as a picture"; "It is also worth noticing"; "The rule worth carrying"; "the part of this Lesson worth stating precisely"; "Two limits are worth stating alongside"; "The failure mode worth naming"; "That last claim is worth measuring rather than asserting, so it was." | The strongest-written Topic still leans on "honest" and "worth" framing about once per section; the measurement sentence narrates the author's process. | State facts directly ("Cancelling is a request…"); "The cases below were measured on a JVM target against kotlinx-coroutines-core 1.11.0." | Open |
| LC-105 | `LC-META` | Low | `unit_cancellation_failure_and_coordination` | `lesson_cancellation_cleanup_and_timeouts` | SENIOR / paragraph 4 | "Thread interruption is a JVM mechanism, and this is a Kotlin Multiplatform curriculum, so runInterruptible is not part of the coroutine cancellation model everywhere." | Justifies the caveat by the curriculum's scope rather than the engineering fact. | "Thread interruption is a JVM mechanism, so `runInterruptible` is JVM-only." | Open |
| LC-106 | `LC-META` | Low | `unit_cancellation_failure_and_coordination` | `lesson_exception_propagation` | PRACTICAL / paragraph 4; SENIOR / paragraph 5 | "on the other targets this curriculum builds for"; "Finally, a boundary rather than a topic… the architecture curriculum owns that argument" | Curriculum-ownership language; "boundary" used for a curriculum boundary. | "on JS and Native"; "Expected failures are often better modelled as return values; that does not remove coroutine exceptions." Optional pointer to **Model and Error Boundaries: What May Cross**. | Open |
| LC-107 | `LC-VOCAB` | Low | `unit_cancellation_failure_and_coordination` | `lesson_cancellation_cleanup_and_timeouts` | PRACTICAL / paragraph 2; SENIOR / bullet list 1 | "Keep the block small and cleanup-shaped"; "kept small and cleanup-shaped" | Coined adjective; the list that follows already defines it. | "Keep the block small and limited to cleanup". | Open |
| LC-108 | `LC-VOICE` | Low | `unit_cancellation_failure_and_coordination` | — (Unit-level) | Lesson summaries; Senior bullet lists in all five Lessons | Summaries "Decide whether…", "Release resources correctly…", "Look at a coroutine tree, pick the child…", "Isolate independent failures…", "Recognise state…"; every Lesson closes with a recap bullet list before the interview callout | Objective-style summaries and a recap list plus an interview recap at the end of every Lesson: the takeaways are stated three times. | Describe the subject in summaries; keep either the recap list or the interview callout per Lesson, not both, unless the list adds something new. | Open |
| LC-109 | `LC-VOICE` | Low | `unit_flow_fundamentals` | — (Unit-level) | Lesson summaries of all five Lessons | "Decide whether an operation should return one result…"; "Say exactly what runs, and when, for a given chain."; "Name the coroutine a collection runs in, and what will end it."; "Say which part of a chain runs in which context."; "Turn an existing producer into a flow without leaking it." | Every summary opens with an imperative task, as in the Context and Cancellation Units; the idea follows in the next sentence. | Drop the imperative lead-in; start with the idea ("A cold flow is a description: …"). | Open |
| LC-110 | `LC-XREF` | Low | `unit_flow_fundamentals` | — (Unit-level) | Unit summary | "The first three units built a coroutine…"; "cancelled by the model the cancellation unit already taught"; "The order is one argument: why the shape exists, what cold means…" | Course recap and a Lesson walk-through in the summary; "the shape" is vague. | Keep the one-result-vs-values-over-time question and the cold-flow sentence; drop the recap and order sentence. | Open |
| LC-111 | `LC-VOCAB` | Medium | `unit_flow_fundamentals` | `lesson_why_flow` | CORE / paragraphs 1, 3, callout 1; PRACTICAL / paragraph 3; SENIOR / paragraphs 1, 3 | "had the same shape at its edges"; "That shape is right…"; "the second shape"; "The question that chooses the shape"; "the more powerful shape"; "a repository-shaped API"; "This shape is what makes a single source of truth workable" | Seven uses of "shape" in one Lesson for what is precisely a caller contract (one answer vs values over time). The Lesson's own vocabulary ("contract", "signature") is already better and is used alongside. | Use "contract", "return type" or "API" as the sentence requires; "repository API"; delete "the more powerful shape" framing. | Open |
| LC-112 | `LC-META` | Low | `unit_flow_fundamentals` | `lesson_why_flow` | CORE / paragraph 3; PRACTICAL / paragraph 3; SENIOR / paragraphs 1, 3 | "is the next Lesson's subject"; "So the honest comparison is not that Flow is the more powerful shape. It is that…"; "nothing in this unit needs them, and they are the subject of the hot streams unit later in this curriculum. This unit is about…"; "worth a sentence and no more, because the architecture curriculum owns it… is somebody else's Lesson" | Scope and ownership narration, plus a "not X. It is Y." correction. | State the hot-stream contrast in one sentence with a pointer to **Hot and Cold: When Production Happens**; drop the ownership sentences; "A Flow-returning API buys currency and costs caller-side complexity." | Open |
| LC-113 | `LC-XREF` | Low | `unit_flow_fundamentals` | `lesson_cold_flows` | CORE / paragraphs 1, 5; PRACTICAL / paragraph 5; SENIOR / paragraph 3 | "The previous Lesson called a Flow…"; "the rest of the curriculum uses them precisely"; "is the flow composition unit later in this curriculum"; "exactly the mistake the hot streams unit later in this curriculum exists to correct, and the Compose bridge into flows linked below…" | Positional and curriculum pointers; "the Compose bridge… linked below" uses authoring vocabulary. | Replace with titles (**Flow Composition, Timing and Failure**, **Hot and Cold: When Production Happens**) or delete; drop "the rest of the curriculum uses them precisely". | Open |
| LC-114 | `LC-CLARITY` | Medium | `unit_flow_fundamentals` | `lesson_flow_collection_lifetime` | Lesson summary; CORE / paragraph 1; PRACTICAL / paragraph 4; SENIOR / paragraphs 1, 3 | Summary "…the ordinary cooperative cancellation Unit 3 already taught"; "The reason this Lesson comes fourth in the curriculum rather than first is that both answers are already known — they are the coroutine model of the first three units applied to a new shape"; "the subject of the effects and lifecycle integration material later in this curriculum"; "Unit 3's model is unchanged"; "the one Unit 3 already gave" | "Fourth in the curriculum" is not true of anything a learner can see (it is the third Lesson of its Unit, and the Unit is sixteenth in the course). "Later in this curriculum" points to Compose lifecycle collection, which sits earlier in the authored order. A Unit number appears in the Lesson summary. | Summary: "…cancelling it is ordinary cooperative cancellation". Delete the ordering sentence. Name **Lifecycle-Aware Collection and the Lifecycle a Screen Actually Has** by title instead of "later in this curriculum". | Open |
| LC-115 | `LC-XREF` | Low | `unit_flow_fundamentals` | `lesson_flow_context_and_flow_on` | CORE / paragraphs 1, 3; SENIOR / paragraphs 2–4 | "The previous Lesson settled who owns a collection… Unit 2's context model applies unchanged"; "the same misconception Unit 1 removed about the suspend keyword"; "the flow composition unit later in this curriculum" (×2); "Unit 2's precision about context and threads carries forward" | Topic-local Unit numbers and "later in this curriculum" pointers; the facts are restated in each sentence. | Drop the numbers; name **Flow Composition, Timing and Failure** once. | Open |
| LC-116 | `LC-VOCAB` | Medium | `unit_flow_fundamentals` | `lesson_flow_builders_and_callback_adapters` | Lesson summary; CORE / paragraph 1, callout 1; PRACTICAL / paragraph 5; SENIOR / comparison 1 header | "The producer's shape picks the builder"; "producers of one particular shape"; "The builder follows from the producer's shape"; "exactly the shape the cleanup Lesson taught"; header "Producer shape" | "Producer shape" is the authoring contract's named example of the habit; here the precise idea is "how the producer delivers values" (sequential suspending calls, several coroutines, callbacks). | "How the producer delivers values picks the builder"; header "How values are produced"; "the same guarantee a `finally` block gives". | Open |
| LC-117 | `LC-XREF` | Low | `unit_flow_fundamentals` | `lesson_flow_builders_and_callback_adapters` | CORE / paragraph 2; PRACTICAL / paragraphs 3, 5; SENIOR / paragraphs 1, 3–4 | "the rule the previous Lesson established"; "do something honest with it"; "the subject named at the end of this Lesson"; "the cleanup Lesson"; "the hot streams unit later in this curriculum"; "a separate subject this unit deliberately leaves alone"; "the buffering material in the flow composition unit later in this curriculum" | Positional pointers, one "honest" and one internal forward reference that makes the reader hunt. | Name **Sharing Cold Flows with `stateIn` and `shareIn`** and **When the Collector Cannot Keep Up** by title; replace "the subject named at the end of this Lesson" with the pointer itself; "handle it deliberately: record, count or surface the drop". | Open |
| LC-118 | `LC-VERBOSE` | Low | `unit_flow_composition_timing_and_failure` | — (Unit-level) | Unit summary | "The previous unit settled what a Flow is…"; "…every one of them is a question about semantics rather than about which API exists."; "The order is one argument: transform a stream, then join streams, then…" | At about 130 words the longest Unit summary; it recaps the previous Unit and walks the Lesson order. "The order is one argument" also closes the Cancellation and Flow Fundamentals summaries — a repeated template. | Keep the five decisions as a list of questions; drop the recap and the order sentence. | Open |
| LC-119 | `LC-XREF` | Low | `unit_flow_composition_timing_and_failure` | `lesson_transforming_and_filtering_flows` | CORE / paragraph 1 | "The previous unit established what an operator is… This unit is about choosing between them" | Positional opening; the operator definition is restated locally. | "An intermediate operator returns another Flow and starts nothing; …" then straight into the two questions. | Open |
| LC-120 | `LC-XREF` | Low | `unit_flow_composition_timing_and_failure` | `lesson_combining_flows` | PRACTICAL / paragraphs 5–6; SENIOR / paragraphs 3–4 | "There is a third shape"; "It is a stable API in the configured version, unlike the flattening operators the next Lesson covers."; "the problem needed a different shape"; "the subject of the hot streams and state unit later in this curriculum" | "The configured version" refers to this build rather than a named library version (Rule 11); positional pointers; two "shape". | "`merge` carries no opt-in annotation in kotlinx-coroutines 1.11.0"; name **Flattening: Should New Input Cancel Old Work?** and **`StateFlow`: One Current Value**; "a third operator", "a different stream contract". | Open |
| LC-121 | `LC-VOICE` | Low | `unit_flow_composition_timing_and_failure` | `lesson_flattening_flows` | PRACTICAL / paragraph 4; SENIOR / paragraphs 3–4 | "the coroutine kind the second unit established"; "a latest-shaped operator"; "The honest framing is that…"; "the cooperative-cancellation model the cancellation Lesson established" | Topic-local ordinal, a "shape" coinage and an "honest" framing in an otherwise excellent Senior section. | "concurrency in the coroutine sense, not parallelism"; "a latest-value operator"; "Latest semantics fit when stale work is disposable…". | Open |
| LC-122 | `LC-XREF` | Low | `unit_flow_composition_timing_and_failure` | `lesson_flow_buffering_and_conflation` | CORE / paragraph 5; PRACTICAL / paragraph 6; SENIOR / paragraph 4 | "the previous unit already supplied it"; "exactly as the cancellation unit described"; "the state-holder types the hot streams unit introduces" | Positional pointers; facts are local. | Drop them; "`StateFlow` already conflates, so `conflate()` does nothing to it." | Open |
| LC-123 | `LC-XREF` | Low | `unit_flow_composition_timing_and_failure` | `lesson_flow_failure_and_completion` | PRACTICAL / paragraph 4; SENIOR / paragraphs 2, 5–6 | "the architecture curriculum owns it"; "the same shape of pipeline"; "the coroutine model the cancellation Lesson established"; "Finally, the argument the previous unit left half-finished." | Ownership and positional phrasing; the last one only works for a reader who remembers the `flowOn` Senior section. | "Context preservation (see **Context Preservation and `flowOn`**) and exception transparency are the same idea…"; drop the ownership clause. | Open |
| LC-124 | `LC-VERBOSE` | Low | `unit_stateflow_sharedflow_and_hot_streams` | — (Unit-level) | Unit summary | "The previous two units settled cold Flow… This unit asks… Three questions get answered separately… StateFlow answers…; SharedFlow answers…; stateIn and shareIn… The unit ends by choosing among…" | A 150-word summary that recaps the previous Units and pre-teaches every Lesson. | Keep the three questions (where production lives, what is retained, when a shared upstream runs); drop the recap and the Lesson-by-Lesson preview. | Open |
| LC-125 | `LC-META` | Medium | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_hot_and_cold_streams` | CORE / paragraphs 3, 6, comparison 1; PRACTICAL / paragraphs 5–6; SENIOR / paragraph 3 | Comparison column "Where the answer lives" with cells "This lesson", "The StateFlow and SharedFlow lessons", "The sharing lesson"; "this unit keeps them apart on purpose. Production lifetime is this lesson. Retention… belongs to StateFlow and SharedFlow, which are the next two lessons."; "the fourth lesson of this unit is where they do"; "exactly the axis this lesson owns"; "The last lesson in this unit turns that into a decision procedure" | A learner-facing table spends a whole column on course navigation, and the paragraph above it assigns each axis to a Lesson. The three-axis idea is the Lesson's best contribution and does not need the map. | Replace the third column with what decides each axis (e.g. "the builder / `shareIn`", "`StateFlow` value or `replay`", "`SharingStarted`"). Delete the paragraph assigning axes to Lessons. | Open |
| LC-126 | `LC-XREF` | Low | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_hot_and_cold_streams` | CORE / paragraph 1; SENIOR / paragraphs 1, 3 | "Everything the previous two units taught was cold."; "the Compose curriculum already made it. The snapshot lesson observes…"; "This is also the honest answer to why the choice matters at all." | Positional opening; the Compose pointer restates its idea locally, so it can use the title. | "A cold Flow is a description…"; "The same idea appears in **`snapshotFlow` and Crossing Into Flow**: …"; drop "honest answer". | Open |
| LC-127 | `LC-META` | Low | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_state_flow` | CORE / paragraph 3; PRACTICAL / paragraph 5; SENIOR / paragraphs 1, 4–5 | "Where that state should live, and what shape it should take, is an architecture question this unit deliberately leaves alone."; "the buffering lesson already established the model"; "the shared-state problem from the cancellation and coordination unit"; "the connection the next lesson picks up"; "Finally, the honest boundary… questions later material answers. This lesson's job is what a collector of one observes." | Scope and ownership narration; "later material" is wrong for the Compose collection Lessons, which precede this Unit in authored order. | Delete the scope sentences; replace "honest boundary" paragraph with one line: "`StateFlow` is a plain Kotlin Multiplatform type; nothing in it knows about UI or lifecycle." Name **Shared Mutable State and Choosing a Coordination Mechanism** for the lost-update pointer if wanted. | Open |
| LC-128 | `LC-XREF` | Low | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_shared_flow` | SENIOR / comparison 1, paragraphs 3–5 | Cell "`replay = 0` is honest and cheap"; "the final lesson of this unit says what does"; "the trade the buffering lesson framed for cold flows"; "the sharing policies in the next lesson"; "Neither carries a decision this unit needs." | Positional pointers and one "honest" in a table cell. | Name **Choosing a Stream Abstraction by Delivery Guarantees** and **Sharing Cold Flows with `stateIn` and `shareIn`**; "`replay = 0` is simple and cheap"; drop the last sentence. | Open |
| LC-129 | `LC-XREF` | Low | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_sharing_cold_flows` | CORE / paragraph 3; PRACTICAL / paragraph 1; SENIOR / paragraph 5 | "the current-value contract from the state lesson"; "the broadcast contract from the previous lesson"; "the scope-ownership lesson answers it"; "the same operator-fusion rule from the state lesson" | Positional pointers, each followed by a local restatement. | Delete the "from the … lesson" phrases. | Open |
| LC-130 | `LC-META` | Medium | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_choosing_a_stream_abstraction` | CORE / paragraph 1; PRACTICAL / paragraphs 3–4; SENIOR / paragraphs 2–4 | "This unit ends where the Flow half of this curriculum has been heading"; "it is the distinction this Lesson ends on"; "That is all this curriculum teaches about channels, and the boundary is deliberate."; "the honest options are outside this unit"; "belongs to the effects and architecture material later in this path"; "What this unit's five lessons contribute to the decision is worth recalling as one argument… And from the earlier units…" | The Lesson frames itself as the Unit's finale, narrates an Exclude decision about channels, and closes with a recap of the Unit that repeats the interview callout. "Later in this path" is again wrong for the Compose effects material. | Open with the five questions; replace the channel-scope paragraph with nothing (the decision table already shows what matters); cut S para 4 to one sentence or delete; name **What Delivery Guarantee Does This Occurrence Need?** for the Compose side. | Open |
| LC-131 | `LC-VOCAB` | Low | `unit_stateflow_sharedflow_and_hot_streams` | `lesson_choosing_a_stream_abstraction` | CORE / paragraph 6; PRACTICAL / paragraphs 1–2 | "no Flow shape answers"; "a delivery shape none of the Flow types offer"; "the difference is the **shape** of the delivery" | The Lesson's central distinction (one receiver vs all subscribers) is emphasised with a bold "shape" where the precise term is available. | "delivery model" or "who receives each element"; bold that instead. | Open |
| LC-132 | `LC-META` | Low | `unit_architecture_responsibilities_and_boundaries` | — (Unit-level) | Unit summary | "This unit builds the test every later architecture unit applies…" | Course-positioning clause; the list of questions after it is the real summary. | "Four questions decide an architecture: what changes together, which way a dependency may point…" | Open |
| LC-133 | `LC-META` | Low | `unit_architecture_responsibilities_and_boundaries` | `lesson_what_architecture_decides` | CORE / paragraph 3; PRACTICAL / paragraph 2; SENIOR / paragraph 4 | "which is why the recurring device in this unit is a changing requirement"; "Build boundaries are the modularization curriculum's subject, and the last lesson in this unit says the one thing about them that belongs here."; "which is where the next lesson starts" | References to a modularization curriculum that does not exist, and Unit-structure narration. | "Build boundaries are a separate decision; a module enforces a direction but does not create one." Drop the rest. | Open |
| LC-134 | `LC-LEAK` | High | `unit_architecture_responsibilities_and_boundaries` | `lesson_responsibility_and_change` | PRACTICAL / paragraph 8 | "the Compose curriculum already asked it at a smaller scale. `lesson_state_hoisting` decided where a value belongs by asking who reads it, who writes it…" | Internal Lesson ID in a cross-Topic bridge. The bridge itself is well explained locally. | "State hoisting in Compose asks the same question at a smaller scale: who reads a value, who writes it, how long it must live (see **State Hoisting and the Lowest Sensible Owner**)." | Open |
| LC-135 | `LC-META` | Medium | `unit_architecture_responsibilities_and_boundaries` | `lesson_responsibility_and_change` | CORE / paragraph 1; PRACTICAL / paragraph 7; SENIOR / paragraphs 2, 4 | "The previous lesson ended on a question it could not answer"; "exactly the decision this unit is about"; "it is more honest to treat it as one"; "This curriculum treats SOLID as vocabulary rather than as a structure to teach, because the principles… are better met where an engineer actually encounters them: … the subject of the next two lessons, and … the later unit on domain logic… Walking all five principles in order would be a tour of an acronym" | The Senior paragraph is an authoring rationale for an Exclude decision (why SOLID is not taught as a list), addressed to the learner. | Keep one sentence: "This is the S in SOLID; the others are better understood through the problems they solve (see **Who Defines the Abstraction?**)." Drop "honest" and positional pointers. | Open |
| LC-136 | `LC-META` | Low | `unit_architecture_responsibilities_and_boundaries` | `lesson_dependency_direction_and_boundaries` | CORE / paragraphs 2, 5; PRACTICAL / paragraphs 3, 9; SENIOR / paragraphs 3–4 | "keeping them apart is most of this lesson"; "the later unit on domain logic and dependency direction takes it seriously"; "whether a given feature should pay for it is this unit's last lesson"; "this curriculum answers it in the unit on domain logic and dependency direction. All this lesson needs is the distinction"; "the judgement the next two lessons sharpen"; "the build and modularization curriculum's subject. The claim this unit makes is only…" | Six scope/ownership pointers in one Lesson, one to a non-existent curriculum. | Keep one title pointer to **Who Defines the Abstraction?**; delete the rest. | Open |
| LC-137 | `LC-VOCAB` | Low | `unit_architecture_responsibilities_and_boundaries` | `lesson_dependency_direction_and_boundaries` | PRACTICAL / paragraphs 1, 6; SENIOR / paragraph 3 | "The shape the service sends is declared once"; "the honest outcome is two small functions"; "the answer has the same shape" | "Shape" for a payload format and as rhetorical filler; one "honest". | "The payload format the service sends…"; "the correct outcome"; "the answer is always the same:". | Open |
| LC-138 | `LC-META` | Low | `unit_architecture_responsibilities_and_boundaries` | `lesson_when_an_interface_is_a_boundary` | PRACTICAL / paragraph 6; SENIOR / paragraphs 3–4 | "belong to the testing curriculum, which is why this lesson deliberately shows no test"; "Here is an example of the honest middle"; "One question this lesson deliberately leaves open… is the subject of the unit on domain logic and dependency direction. Keeping the two questions apart is deliberate" | Author explains omissions and scope; the open question itself is useful as a one-line pointer. | Delete the testing-curriculum clause; reduce S para 4 to one sentence naming the next question and **Who Defines the Abstraction?**. | Open |
| LC-139 | `LC-VOCAB` | Low | `unit_architecture_responsibilities_and_boundaries` | `lesson_when_an_interface_is_a_boundary` | CORE / paragraph 4; PRACTICAL / paragraph 3; SENIOR / paragraph 3 | "would not dictate the implementation's shape"; "whenever the file store's shape changes"; "the shape of the contract always was" | "Shape" three times for API/method list/contract content. | "the implementation's API"; "whenever the file store's methods change"; "what the contract says, and who owns it". | Open |
| LC-140 | `LC-META` | Medium | `unit_architecture_responsibilities_and_boundaries` | `lesson_layers_and_their_cost` | CORE / paragraphs 1, 3; SENIOR / paragraphs 2, 4–5 | "Only now is layering worth introducing, because a layer is an answer and the four previous lessons were the question."; "the claim this lesson exists to correct"; "the build and modularization curriculum's subject, and this unit stops here"; "the unit on data ownership owns it"; S para 5 "That closes the unit's argument… What this unit has deliberately not done is name any of the components… That starts next: the screen-level owner the Compose curriculum stopped at…" | The Lesson ends with a Unit recap and a trailer for the next Units, and describes other curricula as owners; S para 2 also reports this codebase's own module layout ("both compile in the same shared module") as evidence (Rule 11). | Delete S para 5 (the interview callout already synthesises); frame the LessonStudyRepository case as a hypothetical ("consider a repository…"); replace ownership sentences with one pointer to **Model and Error Boundaries: What May Cross**. | Open |
| LC-141 | `LC-VOCAB` | Low | `unit_architecture_responsibilities_and_boundaries` | `lesson_layers_and_their_cost` | PRACTICAL / comparison 1 headers | "When the two shapes are the same"; "When the two shapes diverge" | Headers where "representations" (the Lesson's own word) is precise. | "When the two representations are the same / diverge". | Open |
| LC-142 | `LC-META` | Medium | `unit_screen_state_holders_and_ui_state` | — (Unit-level) | Unit summary | "The Compose curriculum takes a screen as far as an owner outside the Composition and then deliberately refuses to design it. This unit designs it: …" | The summary is framed as a hand-off between curricula; a learner browsing the Unit sees the course's internal division first. | "A screen needs an owner for its state: what that owner is responsible for, how long it lives, how its state is modelled, how the UI talks to it, and which work it may own." | Open |
| LC-143 | `LC-META` | Medium | `unit_screen_state_holders_and_ui_state` | `lesson_state_holder_responsibility` | CORE / paragraphs 1–2; PRACTICAL / paragraph 1, comparison 1, paragraphs 5, 8; SENIOR / paragraphs 3–4 | "The borrowed-items feature from the previous unit now needs a screen. The Compose curriculum has already taken this as far as it goes… It then stops, on purpose… This unit designs it, and it starts where the previous unit ended… Ask the question that unit built"; cell "This is the value the whole unit is about"; "the dependency-injection curriculum owns it"; "a different subject with its own curriculum"; "is the closing unit of this curriculum, and the next four lessons do the groundwork"; "the subjects of the two units after this one" | The Lesson opens with two paragraphs of curriculum hand-off before its subject and returns to course navigation in four more places. | Open with the loans screen and the question "what is answerable for this state?"; state the reader/writer/lifetime test in one clause; delete the course navigation; one pointer to **Choosing an Owner From the Lifetime the Requirement Needs** if wanted. | Open |
| LC-144 | `LC-META` | Low | `unit_screen_state_holders_and_ui_state` | `lesson_state_holder_responsibility` | PRACTICAL / paragraph 3 | "Picture an application with several of them — a study-progress holder, a progress holder, a mistake-review holder, an interview-history holder and a saved-question holder — plain classes in shared code…" | Framed as "picture", but the list is this learning application's own holders (Rule 11); a reader cannot tell why these five, and the list adds nothing a single example would not. | "Picture an application where a borrowed-items count is shown on several destinations: its holder is a plain class…" — one generic example. | Open |
| LC-145 | `LC-META` | Low | `unit_screen_state_holders_and_ui_state` | `lesson_viewmodel_lifetime_and_persistence` | CORE / paragraph 4; PRACTICAL / paragraph 1; SENIOR / paragraphs 1, 3–4 | "which is where the Compose curriculum left the argument"; "The Compose curriculum already asked, from the Composition's edge…"; "So the honest form of the claim is"; "this lesson names them and stops… belong to the lifecycle-and-navigation curriculum and to the persistence curriculum respectively"; "Plenty of applications illustrate the boundary without illustrating the mechanism… so nothing in them demonstrates saved state for a screen owner." | Curriculum narration (two curricula that do not exist) and a closing sentence that reads as a note about the example codebase rather than teaching. | Keep the saved-state/persistence distinction in one sentence; delete the curriculum clauses and the last sentence. | Open |
| LC-146 | `LC-VOCAB` | Low | `unit_screen_state_holders_and_ui_state` | `lesson_modelling_ui_state` | Lesson summary; CORE / paragraphs 2–3, callout 1; PRACTICAL / paragraphs 1, 4; SENIOR / paragraphs 1, 5 | "what shape that value takes"; "Several shapes can express all of that"; "the shape follows"; "its shape is a per-screen decision"; "It is the shape most screens start with"; "A second point about the same shape"; "The case the two shapes handle worst"; "The shape is an answer…" | "State shape" is legitimate here (data class vs sealed hierarchy) but eight uses in one Lesson turn it into a refrain. The Lesson's own "model" is the better noun for half of them. | Keep "shape" in the summary and callout; use "model" for the two alternatives elsewhere. | Open |
| LC-147 | `LC-CLARITY` | Medium | `unit_screen_state_holders_and_ui_state` | `lesson_modelling_ui_state` | PRACTICAL / paragraph 4; SENIOR / paragraphs 3, 6 | "A second point about the same shape is the one the next section is built on: such a screen never decides whether a level may be deselected, whether a source may be chosen, or whether Start is allowed, because an invariant a Composable enforces is one that a second Composable can break."; "A worked example does exactly this and records the reason in the code. A `LearningLessonUiState.Content`…"; "the repositories unit owns it… is already taught in the Compose curriculum" | The P para 4 sentence introduces a practice-builder invariant the Lesson never explains and the next section never uses; it reads as a note carried over from this application's own design. The Senior example is this application's type, cited as if the reader could inspect it. | Delete the practice-builder sentence (or turn it into one clear point about where invariants belong, with its own example). Present the nested-region example generically ("a lesson screen whose study record can fail independently…"). Drop the ownership clauses. | Open |
| LC-148 | `LC-VOCAB` | Low | `unit_screen_state_holders_and_ui_state` | `lesson_state_out_intentions_in` | CORE / paragraphs 2–3; PRACTICAL / paragraphs 3, 7 | "The shape that makes the claim true"; "The Compose curriculum already taught this shape inside the composable tree"; "The correct shape costs one line"; "setter-shaped functions"; "a setter is honest" | Rhetorical "shape" and "honest"; "state shape" in P para 7 is fine and kept. | "The API that makes the claim true"; "Inside the composable tree the same direction applies"; "The correct version"; "setters"; "a setter is fine". | Open |
| LC-149 | `LC-META` | Low | `unit_screen_state_holders_and_ui_state` | `lesson_state_out_intentions_in` | PRACTICAL / paragraph 4; SENIOR / paragraphs 3–4 | "the coroutines curriculum owns what those stream types are"; "exactly as the first lesson in this unit said about the help panel"; "One deliberate omission… it belongs to the unit of this curriculum that compares responsibility models rather than to this one." | Scope narration; the MVI pointer is useful but should be a title. | "This is the basis of MVI; see **MVI: Intent, Reduction and One Current State**." Drop the rest. | Open |
| LC-150 | `LC-META` | Medium | `unit_screen_state_holders_and_ui_state` | `lesson_owner_scoped_work` | CORE / paragraph 2; PRACTICAL / paragraphs 2, 4; SENIOR / paragraphs 3–5 | "That much the coroutines curriculum already settled… This lesson asks one architectural question on top of it, and only that one"; "is worth knowing and is not the lesson"; "is the honest end of this lesson rather than something it answers. The conclusion available here — and it is a real conclusion, not an evasion —"; "the closing unit's decision"; "a background-work mechanism, which has its own curriculum"; "The shared state holders from the first lesson of this unit"; "Finally, the boundary that keeps this lesson about architecture. How scopes… is taught in the coroutines curriculum… the lifecycle-and-navigation curriculum's… the background-work curriculum." | Defensive narration about what the Lesson does not answer ("not an evasion") and a closing paragraph that is entirely curriculum partition, including three curricula that do not exist. The `AppCoroutineScope` example is this application's own type presented as a worked example. | End P para 4 on the bold conclusion plus one clause ("if it must survive process death, use a background-work mechanism such as WorkManager"). Delete S para 5. Keep the injected-scope example generic. | Open |
| LC-151 | `LC-VERBOSE` | Low | `unit_repositories_and_data_ownership` | — (Unit-level) | Unit summary | "The previous unit asked… This unit asks… It derives…, works…, chooses…, shapes…, and decides…" | Recap plus a five-verb Lesson walkthrough. | Keep the two questions (who owns the data policy; which copy is authoritative); drop the recap and walkthrough. | Open |
| LC-152 | `LC-META` | Medium | `unit_repositories_and_data_ownership` | — (Unit-level) | Lesson summaries of all five Lessons | "This lesson derives that responsibility, separates…, and shows…"; "This lesson works them through on one feature and separates…"; "This lesson chooses authoritative owners for three facts…"; "This lesson compares both shapes…, and uses a codebase with entirely one-shot repositories…"; "This lesson compares both on the same concept, shows…, and asks…" | Every Lesson summary ends by describing what "this lesson" does — an authoring outline rather than a statement of the idea. One also advertises "a codebase" as evidence. | End each summary on the idea itself; drop the "This lesson …" sentence. | Open |
| LC-153 | `LC-META` | Medium | `unit_repositories_and_data_ownership` | `lesson_what_a_repository_owns` | CORE / paragraphs 1–2, 4; PRACTICAL / paragraphs 4, 8–9; SENIOR / paragraphs 2–4 | "The borrowed-items feature the previous two units worked through"; "the previous unit's test applies here unchanged"; "the question the foundations unit taught" (×3); "the persistence and networking curricula own their APIs, and this unit owns the contract above them"; "the unit after this one is where it belongs"; "The definition this curriculum uses is deliberately responsibility-based"; S paras 3–4 "A small worked codebase is useful evidence for the boundary and poor evidence for the coordination. Take an application with five repository interfaces — `AssessmentRepository`, `CurriculumRepository`, `LessonStudyRepository`, `SavedQuestionRepository` and `LearningContentRepository`… That application is small and entirely local-first… so that lesson says so and builds its example deliberately." | Two Senior paragraphs audit this learning application's own repositories as evidence (Rule 11) and explain what the next Lesson's example can and cannot prove; the rest of the Lesson repeatedly cites "the foundations unit". | Replace S paras 3–4 with one generic example of a repository that owns only a contract and a translation; state the ownership question directly instead of "the question the foundations unit taught"; drop curriculum ownership sentences. | Open |
| LC-154 | `LC-META` | Medium | `unit_repositories_and_data_ownership` | `lesson_coordinating_sources` | CORE / paragraph 1; PRACTICAL / paragraphs 2, 7, 10; SENIOR / paragraphs 3–4 | "From here on this feature is **hypothetical**. The application you are reading this in has no network layer at all, so what follows is a design worked through rather than an example lifted from production code, and the lesson says which is which."; "The point for this unit is…"; "the background-work curriculum owns it"; "the subject of the last lesson in this unit"; "this unit's boundary with the persistence curriculum… it is a clean one"; "A last honest note about scope." | The Lesson explicitly refers to the learning app the reader is using (Rule 11's clearest violation in the corpus) and repeatedly narrates curriculum partition. | Delete the "application you are reading this in" sentences — the example stands as an example. Replace curriculum ownership with nothing, or one pointer to **Model and Error Boundaries: What May Cross**. | Open |
| LC-155 | `LC-META` | Medium | `unit_repositories_and_data_ownership` | `lesson_single_source_of_truth` | PRACTICAL / paragraphs 2, 5, 7–8; SENIOR / paragraphs 4–5 | "the lifetime question the previous unit separated out"; "What this lesson will not do is pick one of those as a default."; "the same argument the previous unit made"; "the recommendation this lesson exists to qualify"; S para 5 "It is also no evidence whatsoever for the reasoning above. With no network layer, no fact in such an application has two candidate authorities… saying which example shows which is part of using a codebase as evidence honestly." | The Senior close is an author's note about the evidential value of this application's `StudyProgressStateHolder` rather than teaching. | Keep S para 4 as a generic example ("a state holder that projects repository data and says it is not the truth"); delete S para 5; drop positional references. | Open |
| LC-156 | `LC-VOCAB` | Medium | `unit_repositories_and_data_ownership` | `lesson_observable_or_one_shot_api` | Lesson summary; CORE / paragraph 1, bullet list 1, callout 1; PRACTICAL / paragraphs 4, 7–8, callout 1; SENIOR / paragraphs 1, 3 | "compares both shapes"; "The shape of a data API"; "'A stream is the modern shape.' A shape is not an argument."; "names two shapes"; "Choose the API shape"; "the shape that expresses that"; "two different shapes"; "choosing the observable shape"; "One shape is wrong"; "not a legacy shape"; "offer both shapes"; "the two shapes are simply two ways" | Thirteen uses in one Lesson. "API shape" is legitimate once; after that the precise words ("suspending read", "stream", "return type") are already in the Lesson and are clearer. | Keep "API shape" in CORE para 1 and the quoted slogan; elsewhere name the thing: "a stream", "a suspending read", "both reads", "the return type". | Open |
| LC-157 | `LC-META` | Low | `unit_repositories_and_data_ownership` | `lesson_observable_or_one_shot_api` | PRACTICAL / paragraphs 7, 9; SENIOR / paragraphs 3–6 | "are the coroutines and Flow curriculum's, and the lessons linked from this one are where their contracts live. What belongs to this lesson…"; "the failure the previous unit named at the screen boundary"; "the previous lesson's definition of authority"; S para 4 "Consider an application in which every repository method is a one-shot `suspend` function… across all five of its interfaces" | Curriculum ownership and a counterexample that is this application's design, counted ("all five of its interfaces"). | Delete ownership sentences; keep the counterexample generic (no counts, no this-app types) — the trade-off table carries the teaching. | Open |
| LC-158 | `LC-VOCAB` | Medium | `unit_repositories_and_data_ownership` | `lesson_model_and_error_boundaries` | CORE / paragraph 3; PRACTICAL / paragraphs 5, 9–10, bullet list 1, callout 1; SENIOR / paragraph 4; code 1 comments | "the received shape does match"; "when the shapes are otherwise aligned"; "identical in shape to the shared-model coupling"; "Three shapes are all defensible"; "is this shape"; "where the stored shape is the application's shape"; "the three shapes above"; code comments "// the service's shape", "// the device's shape", "// the application's shape" | The Lesson's subject is *representations*, and it uses that word well; "shape" competes with it eleven times, including in code comments. | Use "representation" or "format" throughout, "option" for the three error mechanisms; code comments "// wire format", "// stored representation", "// application model". | Open |
| LC-159 | `LC-META` | Low | `unit_repositories_and_data_ownership` | `lesson_model_and_error_boundaries` | CORE / paragraph 2; PRACTICAL / paragraphs 6–7, 11; SENIOR / paragraphs 3–4 | "the one the foundations unit already taught"; "the reminder-time preference from the first lesson of this unit"; "A worked example demonstrates the small version of the boundary. A `LocalLessonStudyRepository` maps a `StudiedLessonEntity`…"; "deliberately not this lesson's argument"; "There is a boundary with the previous unit here that is easy to blur"; "the language curriculum owns how they work… the persistence and networking curricula own everything about what they do" | Positional and ownership narration, and one example drawn from this application's own types. | Name the test directly; make the example generic; drop ownership clauses. | Open |
| LC-160 | `LC-VERBOSE` | Low | `unit_domain_logic_and_dependency_direction` | — (Unit-level) | Unit summary | "The previous unit gave this feature a repository… — and the honest answer is often no… The unit separates…, distinguishes…, traces…, and closes on…" | Recap, "honest" framing and a Lesson walkthrough. | Keep "A domain layer is optional…" and the dependency-rule sentence; drop the recap and walkthrough. | Open |
| LC-161 | `LC-META` | Medium | `unit_domain_logic_and_dependency_direction` | — (Unit-level) | Lesson summaries of all five Lessons; references to "the foundations unit" in every Lesson | "This lesson makes 'add it when you need it' actionable…"; "This lesson puts the two side by side…"; "this lesson traces what it costs…"; "This lesson arranges the same three types twice…"; "This lesson states the rule from its intent…"; "the foundations unit's test", "the foundations unit's cost model", "the one the foundations unit promised" (about ten occurrences across the Unit) | Summaries end in authoring outlines, and "the foundations unit" is used as a proper noun the learner never sees (the Unit is titled **Architecture as Responsibilities and Boundaries**). | End summaries on the idea; state each borrowed test in a clause ("a boundary earns its cost when there is independent responsibility on the other side") and drop "the foundations unit". | Open |
| LC-162 | `LC-META` | Medium | `unit_domain_logic_and_dependency_direction` | `lesson_when_a_domain_layer_earns_its_place` | CORE / paragraphs 1, 3–4; PRACTICAL / paragraphs 1–2, 9; SENIOR / paragraphs 2–4 | "The previous unit left the borrowed-items feature with a repository"; "the first one in this subject whose honest answer is often no"; "which the last lesson of this unit takes up properly"; "the reminder-time preference from the data-ownership unit"; "the build and modularization curriculum"; S para 3 "A small worked codebase is evidence for the same separation. It has no package named `domain`… The next lesson reads those types closely."; S para 4 "Two boundaries, named so neither looks like an omission. 'Domain' in this curriculum means…" | Over 2,000 words for one coherent decision model. The three worked cases carry the teaching; the Senior section spends two of four paragraphs on this application's layout and on scope disclaimers. | Keep the three cases and S para 1; reduce S paras 2–4 to two sentences (layer ≠ module; "domain" here does not mean DDD). Expect roughly 20% shorter. | Open |
| LC-163 | `LC-META` | Medium | `unit_domain_logic_and_dependency_direction` | `lesson_use_cases_and_pass_through_cost` | SENIOR / paragraphs 1–3, bullet list 1 | "That same codebase is useful evidence here precisely because it did not adopt the convention. **There is no class named `UseCase` or `Interactor` anywhere in it**… `MistakeReviewService` reads completed attempt history through `AssessmentRepository`…, called from the app shell, the progress state holder, the mistake-review state holder and the recommendation resolver… `ContinueLearningPolicy`… its own documentation says so…"; "nothing in that codebase is named a use case, and calling it one would be reading the lesson back into the code rather than reading the code" | The entire Senior section (≈450 words) reports verified facts about this learning application's own classes, call sites and KDoc (Rule 11). A learner cannot inspect "that codebase", and the list of ten type names teaches nothing beyond "responsibility earned the class". | Replace the Senior section with the principle and one generic illustration ("a service that orchestrates, a policy that only decides, a derivation that only computes — names chosen by what each owns"). Drop every this-app type name and the KDoc quotation. | Open |
| LC-164 | `LC-VOICE` | Medium | `unit_domain_logic_and_dependency_direction` | `lesson_use_cases_and_pass_through_cost` | Lesson summary; CORE / paragraph 3; PRACTICAL / paragraphs 2, 7; SENIOR / call-out | "treats the team convention… honestly"; "This curriculum treats them as equivalent and says so here"; "the honest way to say so is not 'it is boilerplate'"; "the honest position names both"; "naming the third possibility honestly" | "Honest" five times; with "This curriculum treats…" it makes the Lesson sound like it is defending its own fairness. | State positions directly ("Use case and interactor mean the same thing here"; "Both the benefit and the cost are real:"). | Open |
| LC-165 | `LC-LEAK` | High | `unit_domain_logic_and_dependency_direction` | `lesson_policy_and_framework_detail` | SENIOR / paragraph 1 | "Testability is the argument this subject most often gets backwards, and the epic's own rule is that a testability claim has to name the boundary rather than the layer count." | Refers to a backlog epic's acceptance rule — internal project metadata the learner cannot see or decode. | "A testability claim has to name the boundary, not the layer count:" then the existing statement. | Open |
| LC-166 | `LC-META` | Medium | `unit_domain_logic_and_dependency_direction` | `lesson_policy_and_framework_detail` | CORE / paragraphs 1, 3; PRACTICAL / paragraph 4; SENIOR / paragraphs 1–4 | "The previous two lessons decided where an operation belongs"; "the same instrument the foundations unit used"; "The data-ownership unit already priced this move"; "the testing curriculum's subject, which is why this lesson shows none"; "belong entirely to the Kotlin Multiplatform curriculum"; S para 3 "A `ContinueLearningPolicy`… as its own documentation insists… It is not evidence about a business domain… the renewal scenario above is a worked design rather than something lifted from the same codebase"; "the question the lesson has deliberately left open… That is the next lesson, and it is the one the foundations unit promised." | Curriculum narration, a this-app example cited through its own KDoc (Rule 11), and an evidence disclaimer. | Replace S para 3 with a generic pure-policy example; drop curriculum and KDoc references; end with a title pointer to **Who Defines the Abstraction?**. | Open |
| LC-167 | `LC-META` | Medium | `unit_domain_logic_and_dependency_direction` | `lesson_dependency_inversion_in_practice` | CORE / paragraphs 1–2; PRACTICAL / paragraph 6; SENIOR / paragraphs 3, 5 | "Two earlier lessons stopped deliberately at this question. The foundations unit taught…; the lesson on interfaces established…; and the data-ownership unit deferred the same question… Here is the answer."; "the one the earlier lesson already established"; P para 6 "`LessonStudyRepository` is declared in `lesson_study/repository`… `LocalLessonStudyRepository` sits under `data/local/lesson_study/repository`… All five of that application's repository interfaces are arranged that way, and all of it compiles inside one Gradle module"; "belong entirely to the dependency-injection curriculum" | Opens with a map of where earlier Lessons stopped; P para 6 is an audit of this application's package layout presented as evidence (Rule 11). (The `lesson_study` tokens there are package paths, not curriculum IDs.) | Open with the claim to dismantle. Replace P para 6 with a hypothetical ("in a single-module app, the feature package declares the contract and `data/local` implements it; no module boundary is needed for inversion"). Name **Dependency Injection as Object Construction** by title if a pointer is wanted. | Open |
| LC-168 | `LC-META` | Medium | `unit_domain_logic_and_dependency_direction` | `lesson_clean_architecture_intent` | CORE / paragraphs 1, 4; PRACTICAL / paragraph 7; SENIOR / paragraphs 2, 4–5 | "This unit has now made four decisions… without naming the architecture… That order was deliberate. Meeting the name first is how a set of reasons becomes a template"; "the previous lessons in this unit are where that decision is actually made"; "This curriculum therefore teaches the roles rather than the words"; "That is the foundations unit's cost model, carried all the way to the end of this unit"; "the worked example in this unit holds five consumer-owned repository contracts… compiles all of them in one module… the build and modularization curriculum, which this unit names once and leaves alone"; S para 5 "That closes the unit… reading a real codebase against all three is the next unit's subject." | Opening justifies the Unit's ordering; closing paragraph is a Unit recap plus trailer; a this-app claim is repeated. | Open with the rule; delete the ordering justification and the S para 5 recap/trailer (the call-out already summarises); keep the vocabulary-translation advice without "This curriculum". | Open |
| LC-169 | `LC-VOCAB` | Low | `unit_domain_logic_and_dependency_direction` | `lesson_use_cases_and_pass_through_cost` | Lesson summary; CORE / paragraph 5; PRACTICAL / paragraph 3 | "side by side in the same shape"; "two types deliberately given the same shape"; "Now the same shape with something inside it" | Here "the same structure" is accurate (the text itself says "Structurally the two types are twins"). | "with the same structure" / "the same class with something inside it". | Open |
| LC-170 | `LC-META` | Medium | `unit_responsibility_models_mvp_mvvm_mvi` | — (Unit-level) | Unit summary | "The previous four units taught every decision these pattern names are about… without once using a pattern name. This unit adds the vocabulary… One hypothetical practice-configuration screen runs through the comparison unchanged, so every difference the reader sees is… The unit's purpose is classification rather than prescription: finish it able to…" | A 160-word summary made of course recap, pedagogical design rationale and a learning objective. | "MVP, MVVM and MVI are three sets of answers to the same questions about a screen: where state lives, who may change it, whether the owner knows the UI, how input and output travel. The names vary by source; the responsibilities do not." | Resolved — see [Remediation Log](#remediation-log) |
| LC-171 | `LC-META` | Medium | `unit_responsibility_models_mvp_mvvm_mvi` | `lesson_one_screen_five_questions` | Lesson summary; CORE / paragraphs 1–3; PRACTICAL / paragraphs 1–3, 5–6; SENIOR / paragraphs 3–4 | Summary "Four units taught how to describe… This one adds the names"; "Four units have been building one instrument… this unit adds it. It adds nothing structurally new: every arrangement in the next four lessons is something the previous units already gave you"; "the warning has to come first because it changes how the rest of the unit should be read"; "One hypothetical screen runs through this lesson and the three that follow… That is the point of it: if the example changed with the pattern, every difference the reader saw would be a difference in the product"; "Rich enough to expose every trade-off the unit is about"; "the question the unit exists to make awkward"; "the skill this unit is for"; "this unit deliberately teaches it no further than that. There is no fourth implementation… in this unit"; "One last framing before the pattern lessons. … The next three lessons take the same screen…" | Over 2,000 words; about a third of the prose explains the Unit's design (why one running example, why names come last, what the next lessons will do, why MVC gets no implementation). The five-question instrument itself is excellent and would stand in roughly two-thirds of the length. | Rewrite around the instrument: the terminology warning (one paragraph), the five questions, the practice screen answered, the three-teams table, MVC in two sentences. Remove every sentence about the Unit's structure or purpose. | Resolved — see [Remediation Log](#remediation-log) |
| LC-172 | `LC-META` | Medium | `unit_responsibility_models_mvp_mvvm_mvi` | `lesson_mvp_view_contract` | Lesson summary; CORE / paragraphs 1–3; PRACTICAL / paragraphs 3, 5–6, bullet list 1; SENIOR / paragraph 3 | Summary "…taught here as one, not as a strawman for the lesson that follows"; "the unit's standing rule applies here"; "and what this lesson teaches"; "That is deliberately as little code as the argument needs"; "belong to the lifecycle curriculum and are not this lesson's subject. The *requirement* is."; "the only honest way to teach a pattern that is often dismissed"; "a boundary in the foundations unit's exact sense"; "it matters for how the next lesson is read" | The author defends the Lesson's fairness and scope inside the summary and body. The MVP analysis (derive attach/detach from the held reference; enumerability vs consistency) is among the best in the corpus. | Summary: drop the strawman clause. Delete scope and fairness sentences; replace "the foundations unit's exact sense" with "a consumer-owned contract". | Resolved — see [Remediation Log](#remediation-log) |
| LC-173 | `LC-META` | Medium | `unit_responsibility_models_mvp_mvvm_mvi` | `lesson_mvvm_observed_state` | Lesson summary; CORE / paragraph 3; PRACTICAL / paragraphs 1–2, 4–6; SENIOR / paragraphs 2–3 | Summary "The state-holder unit built this arrangement without naming it, so this lesson adds the vocabulary rather than the design"; "The state-holder unit worked all of that out"; "The most useful thing that can be said about this design to a reader of this curriculum is that **they have already built it.** … So this lesson is not a new design."; "because that is what this curriculum's stack uses"; "The coroutines curriculum owns how `StateFlow` behaves"; "the argument from the foundations unit"; "which is why this curriculum defines the pattern by it"; "this curriculum has been careful about it before" | The Lesson positions itself relative to other Units in six places; a reader arriving directly is told they "have already built it". | State the arrangement and its consequences directly; point once to **What a Screen State Holder Is Responsible For** if useful; "defined here by the dependency direction". | Resolved — see [Remediation Log](#remediation-log) |
| LC-174 | `LC-META` | Low | `unit_responsibility_models_mvp_mvvm_mvi` | `lesson_mvi_intent_and_reduction` | CORE / paragraphs 1, 4; PRACTICAL / paragraphs 2, 6; SENIOR / paragraphs 1–2, 4 | "questions three and five have the same answers as the previous lesson"; "This curriculum's definition requires only…"; "the same invariant argument the state-holder unit made… expressed in a different shape"; "That word is already taken in this curriculum… Naming the design question is as far as this lesson goes… are the subject of the next unit"; "The classification lesson puts designs in front of you"; "which the Compose curriculum owns"; "a proportionality judgement of the kind this subject closes on" | Positional and ownership narration. The "effect" disambiguation is valuable and should stay, re-voiced as a fact about Compose rather than about "this curriculum". | "In Compose, 'effect' already names `LaunchedEffect` and its relatives…"; name **What Guarantee Does This Occurrence Need?** by title; drop the rest. | Resolved — see [Remediation Log](#remediation-log) |
| LC-175 | `LC-VOICE` | Low | `unit_responsibility_models_mvp_mvvm_mvi` | — (Unit-level) | L23.1 PRACTICAL / paragraph 3; L23.2 PRACTICAL / paragraph 6; L23.4 PRACTICAL / paragraph 5, SENIOR / paragraphs 2, 4 | "The honest first answer"; "the only honest way to teach a pattern"; "The ceremony has to be counted honestly"; "The honest statement runs the other way"; "The honest answer is that it scales…" | "Honest" framing recurring across the Unit. | State the claims directly. | Resolved — see [Remediation Log](#remediation-log) |
| LC-176 | `LC-META` | Medium | `unit_responsibility_models_mvp_mvvm_mvi` | `lesson_classifying_a_real_architecture` | Lesson summary; CORE / paragraphs 1–2; SENIOR / paragraphs 1–4 | Summary "This lesson is the unit's payoff"; "the previous four units spent their length establishing"; "the skill this unit is actually for"; S para 1 "A `LessonScrollStateReducer`… in an application nobody would describe as MVI… **This is evidence that techniques travel independently of labels; it is not evidence that the application 'uses MVI'…**"; S para 2 "A practice-configuration screen from the same codebase is a live instance of design A. `PracticeBuilderViewModel` holds one immutable `PracticeBuilderUiState`…"; "Two boundaries keep this unit's conclusions the right size"; "the previous unit is the reason the question arises" | Summary and Senior section frame the Lesson as the Unit's climax and audit this learning application's own classes as evidence (Rule 11), with an evidence-validity disclaimer in bold. | Replace S paras 1–2 with generic illustrations (a scroll-chrome reducer in a non-MVI app; a design-A screen); keep S paras 3–4's two scope facts in one paragraph without "this unit"; summary on the classification order. | Resolved — see [Remediation Log](#remediation-log) |
| LC-177 | `LC-VOCAB` | Low | `unit_responsibility_models_mvp_mvvm_mvi` | — (Unit-level) | L23.1 PRACTICAL / comparison 1; L23.2 CORE / paragraph 2, PRACTICAL / paragraph 3; L23.3 PRACTICAL / paragraph 5, SENIOR / paragraph 1; L23.4 PRACTICAL / paragraph 2; L23.5 CORE / paragraph 1, PRACTICAL / paragraphs 1, 5, callouts | "The shape of question 4's answer"; "the shape is immediately different"; "none of it would change the shape. The shape is what matters"; "MVVM-shaped"; "the shape of the output"; "expressed in a different shape"; "input shape", "output shape"; "the shape of the summary"; "adopted a shape" | Thirteen uses; "input shape/output shape" is an undefined label for questions four and five, and the Unit's own words ("arrangement", "model") are more precise. | "input model / output model" (matching "state model"); "the answers are immediately different"; "arrangement"; delete rhetorical uses. | Resolved — see [Remediation Log](#remediation-log) |
| LC-178 | `LC-META` | Medium | `unit_state_events_lifetime_and_selection` | — (Unit-level) | Unit summary | "The five previous units each started from something that already existed… It adds no new mechanism. It applies the five units, and it finishes an argument the Compose curriculum deliberately left at a negative result… where the answer stops being architecture and becomes somebody else's curriculum. Finish it able to…" | A course-narrative summary (recap, relation to another curriculum, learning objective) rather than a description of the subject. | "Start from a requirement and nothing built: what must the application represent, what guarantee does it need, whose lifetime matches, and how much structure does the feature earn." | Resolved — see [Remediation Log](#remediation-log) |
| LC-179 | `LC-META` | Medium | `unit_state_events_lifetime_and_selection` | `lesson_state_or_occurrence` | CORE / paragraphs 1–2, 4; PRACTICAL / paragraphs 2–3, 10; SENIOR / paragraph 5 | "The five previous units each began with something that already existed… this lesson answers the first"; "The Compose curriculum established that contrast at the screen boundary and this unit applies it rather than re-deriving it."; "the next-but-one lesson's subject"; "which the streams curriculum already characterises"; "the source-of-truth decision the data-ownership unit makes, and the mechanism belongs to the persistence curriculum"; "which is the next lesson" | Over 2,000 words — the longest Lesson in the corpus. One coherent model (two dimensions: happened vs true; required retention). The worked requirement table and the duplicate-celebration failure are the length's justification; the opening recap and six curriculum pointers are not. | Open with the scored-session fact; replace curriculum pointers with titles once (**Choosing an Owner From the Lifetime the Requirement Needs**, **What Guarantee Does This Occurrence Need?**) or delete. Roughly 10–15% shorter. | Resolved — see [Remediation Log](#remediation-log) |
| LC-180 | `LC-CLARITY` | Low | `unit_state_events_lifetime_and_selection` | `lesson_state_or_occurrence` | PRACTICAL / paragraph 4 | "performing it tomorrow when they open the borrowed-items list would be wrong — a celebration for a session they finished yesterday" | The practice-session example suddenly moves into the library application's "borrowed-items list"; two running examples collide in one sentence. | "…when they next open the app" (keep one example domain per Lesson). | Resolved — see [Remediation Log](#remediation-log) |
| LC-181 | `LC-META` | Medium | `unit_state_events_lifetime_and_selection` | `lesson_delivery_guarantees` | Lesson summary; CORE / paragraphs 1, 4; PRACTICAL / paragraphs 2–3; SENIOR / paragraphs 2, 4 | Summary "The Compose curriculum reached a negative result and stopped there… This lesson starts from that result and answers the question it left open."; "The Compose curriculum asked that from the consumer's side and reached a deliberate stopping point… designing the owner on the other side is this curriculum's work. This lesson does that work."; "the order is the whole content of this lesson"; "the correct design for it is the transient one the Compose curriculum already supplies"; "over-architecture in the precise sense the closing lesson of this unit defines"; "Two conclusions from the streams curriculum apply immediately and are not re-derived here."; "is honest and usually sufficient"; "This is also where the unit deliberately stops… none of them is this curriculum's. The decision this lesson exists to teach is complete when…" | Summary, opening and Senior close are all framed as a hand-off between curricula; a reader arriving directly first meets another curriculum's "negative result". The title is also a near-duplicate of a Compose Lesson's (see the Observation on **What Delivery Guarantee Does This Occurrence Need?**). | Summary: "A one-off occurrence needs a stated guarantee before a mechanism…". Open with the five questions; state the replay and emission facts as facts; cut S para 4 to one sentence ("Building a durable queue is a separate project"). Differentiate the title from the Compose Lesson's. | Resolved — see [Remediation Log](#remediation-log) |
| LC-182 | `LC-META` | High | `unit_state_events_lifetime_and_selection` | `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraphs 4–5 | "In that example an `AppCoroutineScope`… Five state holders receive it… **every repository in that application exposes one-shot suspending reads and none of them returns a `Flow`**, across every interface and implementation… That application is also local-first with no network layer." | Rung three is justified by reporting verified facts about this learning application — essentially the sentence the authoring contract's Rule 11 cites as its negative example ("every repository in this application exposes one-shot suspending reads, verified across every interface"). The argument is sound; its evidence is uncheckable by the learner. | State the condition as a hypothetical ("if the data layer exposes only one-shot reads, there is nothing observable for destination owners to subscribe to, so a projection above them…"); drop counts, type names and "that application". | Resolved — see [Remediation Log](#remediation-log) |
| LC-183 | `LC-META` | Medium | `unit_state_events_lifetime_and_selection` | `lesson_choosing_the_owner_by_lifetime` | CORE / paragraphs 1, 4; PRACTICAL / paragraphs 3, 8, call-out, comparison 1; SENIOR / paragraphs 2–3 | "The previous lesson ended on a question it deliberately did not answer"; "the same cost discipline the foundations unit applied"; "exactly as the state-holder unit designed it"; "the next rung down is usually the honest answer"; "The honest conclusion is…"; "are the background-work curriculum's subject, and this lesson stops at naming the rung"; table cell "a mechanism whose curriculum is not this one"; "the lifecycle and navigation curriculum, and this lesson teaches none of it" | Over 2,000 words; the ladder model is coherent and valuable, but curriculum narration appears even inside the comparison table. | Replace the table cell with "a scheduler such as WorkManager"; drop curriculum and positional references; keep the Android guidance quotations. | Resolved — see [Remediation Log](#remediation-log) |
| LC-184 | `LC-META` | Low | `unit_state_events_lifetime_and_selection` | `lesson_smallest_sufficient_architecture` | CORE / paragraphs 1–2; PRACTICAL / paragraphs 1–2, 4; SENIOR / paragraphs 1–2, 5 | "This is the last question in the subject… Every previous unit produced a candidate"; "the discipline is in applying it honestly"; "the reminder-time setting the domain unit already used"; "the boundary the data-ownership unit designed"; "from the guarantee lesson… from the lifetime lesson"; "what the pattern unit taught"; "one honest argument against everything in this lesson"; "So close the loop where the subject opened it. The foundations unit asked four questions… the whole curriculum has been assembling the material" | As the Architecture capstone some synthesis is legitimate, but it is expressed as references to Units by informal names rather than as the decisions themselves. | Keep the five-question closing list; replace "from the guarantee lesson" etc. with the decision ("a durable pending-or-acknowledged record"); drop "the foundations unit"/"the whole curriculum" framing. | Resolved — see [Remediation Log](#remediation-log) |
| LC-185 | `LC-VERBOSE` | Low | `unit_dependency_injection_as_object_construction` | — (Unit-level) | Unit summary | "From there the unit follows one small object graph as it grows: what a constructor parameter makes true…, why injection and dependency inversion…, where construction knowledge belongs…, what a registry lookup hides…, and when wiring the whole graph by hand…" | Lesson-by-Lesson walkthrough in a summary whose first three sentences already carry the idea. (The closing "No Dagger, Hilt or Koin syntax appears…" is informative and can stay.) | Keep the construct/fetch/receive framing and the no-container point; compress the walkthrough to one clause. | Open |
| LC-186 | `LC-META` | Medium | `unit_dependency_injection_as_object_construction` | — (Unit-level) | Lesson summaries of all six Lessons | "This lesson writes one controller all three ways…"; "This lesson makes each of those concrete…"; "this lesson proves it both ways…"; "This lesson assembles the unit's graph by hand…"; "This lesson has the unit's controller look its collaborators up instead…"; "This lesson names those costs precisely, judges the unit's own graph…" | Same authoring-outline summary pattern as the Architecture Units. | End each summary on the idea itself. | Open |
| LC-187 | `LC-META` | Low | `unit_dependency_injection_as_object_construction` | `lesson_who_constructs_this_object` | CORE / paragraph 5; PRACTICAL / paragraphs 5, 8; SENIOR / paragraph 3 | "Everything that comes later in this curriculum — bindings, modules, scopes, generated factories — is machinery…"; "This shape has a name and its own lesson later in this unit."; "later units in this curriculum examine what each of them actually does. None of them appears again in this unit…"; "In the form this curriculum prefers…" | Course narration around an otherwise model opening Lesson. | "Bindings, modules, scopes and generated factories are machinery for arranging that arrival at scale"; name **Asking For It, or Being Given It** for the locator; drop the rest. | Open |
| LC-188 | `LC-META` | Low | `unit_dependency_injection_as_object_construction` | `lesson_a_dependency_should_be_visible` | CORE / paragraph 1; PRACTICAL / paragraphs 4, 6, 10, bullet list 1; SENIOR / paragraph 1 | "The previous lesson settled…"; "re-answering it here would be a worse answer than the one that exists. The architecture curriculum's lesson on when an interface is a boundary gives the test"; "the unit on Hilt meets the one that matters on Android"; "the unit on graphs and lifetimes treats that distinction properly"; "The honest encodings stay explicit… What is not honest…"; "**Substitution, and where this topic stops.**… belongs to the testing curriculum and is deliberately not taught here" | Useful pointers expressed as curriculum narration; the interface test is stated locally (good) but framed as a defence of not re-teaching it. | Keep the one-sentence test and name **When an Interface Is a Boundary, and When It Is Only Indirection**; name **A Value the Graph Cannot Know** and **When Android Owns Construction** by title; "correct/incorrect" for "honest"; drop the testing-curriculum clause and the "where this topic stops" header. | Open |
| LC-189 | `LC-CLARITY` | Medium | `unit_dependency_injection_as_object_construction` | `lesson_injected_inverted_or_both` | CORE / paragraph 3; PRACTICAL / callout 1, comparison 2, paragraph 4; SENIOR / paragraph 1 | "The architecture curriculum already taught the second one in full, and this lesson does not re-derive it… What is missing, and what this lesson supplies…"; "The architecture curriculum already states the consequence exactly: the container decides…, 'it does not change a single import…'"; table cells "Version A of the first lesson…", "Version B of the first lesson…", "pays every cost the fifth lesson prices"; "the test the architecture curriculum set" | The Lesson quotes another Lesson of this curriculum in quotation marks as if it were an external authority (Rule 11 reserves quotation for Rule 9 sources), and its decisive comparison table depends on remembering "version A/B of the first lesson" and what "the fifth lesson prices". | State the imports claim in this Lesson's own voice; make table cells self-contained ("the class constructs its collaborator itself"; "fetches from a registry — see **Asking For It, or Being Given It**"); drop "does not re-derive". | Open |
| LC-190 | `LC-VOCAB` | Low | `unit_dependency_injection_as_object_construction` | `lesson_injected_inverted_or_both` | PRACTICAL / paragraphs 1, 3–4, 8, comparison 2 | "Shape one: injected, and not inverted"; "Shape two: inverted, with nothing but a `main` function"; "Shape one above", "Shape two above" (table); "label shape two…", "shape two sits in…", "What shape two demonstrates"; "when the storage changes shape" | "Shape" used eight times as the label for two example arrangements; the Architecture Lesson this one pairs with calls the same thing "Arrangement A/B". | "Arrangement one / two" (or "Case one / two"); "changes its representation". | Open |
| LC-191 | `LC-META` | Low | `unit_dependency_injection_as_object_construction` | `lesson_one_place_that_knows_how_to_build` | CORE / paragraph 1; PRACTICAL / paragraph 4; SENIOR / paragraphs 2–4 | "Every lesson so far has taken something away from a class"; "is the closing lesson's question"; "the one place where the shape of the program is knowable"; "it is the next lesson's subject"; "which is what the unit on Koin examines" | Positional pointers; one "shape" where "object graph" is exact. | Name **When Wiring It Yourself Is the Right Answer**, **Asking For It, or Being Given It** and **The Container, and the Modules That Fill It** once each, or drop; "the object graph is knowable". | Open |
| LC-192 | `LC-META` | Medium | `unit_dependency_injection_as_object_construction` | `lesson_asking_for_it_or_being_given_it` | PRACTICAL / paragraphs 3, 6, callout 1; SENIOR / paragraph 2 | "**Now the qualification, and it is the reason this lesson exists rather than a caveat on the end of it.**"; "Without it, the later units of this curriculum contradict this one: the unit on Hilt covers… the unit on Koin covers… Taught as 'lookup is bad', this lesson would turn both of those into defects."; "this curriculum compares their mechanisms… rather than awarding them labels"; "they are the criteria the framework units later apply rather than new material" | The integration-boundary qualification is the Lesson's best idea, but it is justified by curriculum consistency ("otherwise later units contradict this one") rather than by engineering. | Justify it by the engineering fact: "Framework-instantiated classes cannot take constructor parameters, so resolving a finished object there is a different design." Mention Hilt and Koin as examples by title; delete the curriculum-consistency argument. | Open |
| LC-193 | `LC-META` | Low | `unit_dependency_injection_as_object_construction` | `lesson_when_wiring_it_yourself_is_enough` | CORE / paragraphs 1, 4; PRACTICAL / paragraph 5; SENIOR / paragraphs 1–3 | "The graph in this unit has been wired by hand from the beginning"; "a requirement in its own right that the next unit teaches properly"; "a shape that has changed three times across this unit"; "The honest framing is a curve"; "which is what the units on Dagger, Hilt and Koin supply, and the closing unit of this curriculum is where the crossing point is estimated"; "The architecture curriculum closes on a test" | Course narration and "honest"; the proportionality test is restated locally (good). | Name **One Instance, or a New One Each Time?** and **The Smallest Sufficient Strategy** by title once; "The graph has changed three times"; "The framing is a curve". | Open |
| LC-194 | `LC-VERBOSE` | Low | `unit_object_graphs_lifetimes_and_scopes` | — (Unit-level) | Unit summary | "This unit turns that structure into reasoning a framework can later encode: the transitive graph…, the requirement that decides…, and the three separate ideas… It closes on two things a graph can fail to express… Every conclusion is reached without a framework, because Dagger, Hilt and Koin are later notations…" | A ~170-word Lesson walkthrough; the first two sentences carry the subject. | Keep the opening two sentences and the three meanings of "scope"; drop the walkthrough. | Open |
| LC-195 | `LC-META` | Low | `unit_object_graphs_lifetimes_and_scopes` | — (Unit-level) | Lesson summaries of all six Lessons | "This lesson traces…"; "This lesson decides node by node…"; "This lesson separates…"; "This lesson separates a graph dependency…"; "This lesson reads the collision…"; "This lesson walks the same two faults…" | Authoring-outline summary pattern (as in Units 21–25). | End on the idea. | Open |
| LC-196 | `LC-META` | Low | `unit_object_graphs_lifetimes_and_scopes` | `lesson_from_one_dependency_to_a_graph` | CORE / paragraphs 1, 3–6; PRACTICAL / paragraphs 1, 6, 11, callout 1; SENIOR / paragraph 2 | "The previous unit ended with a controller…"; "the rest of this unit and the three framework units after it all use them"; "about to be used for two different things in the same curriculum"; "The previous unit defined a composition root…"; "The unit on Dagger introduces a type…"; "the previous unit's lesson on injection against inversion"; "Nothing in this curriculum needs graph theory… past that it is a different subject being taught for its own sake" | Course narration around a good graph model; the graph-theory sentence is an Exclude decision narrated. | Keep the root-vs-root distinction without "in the same curriculum"; name **Dagger: Compile-Time Object Graphs** and **Injected, Inverted, or Both?** by title once; replace the graph-theory sentence with "Five terms are enough:". | Open |
| LC-197 | `LC-VOICE` | Low | `unit_object_graphs_lifetimes_and_scopes` | `lesson_one_instance_or_a_new_one` | PRACTICAL / paragraphs 3–4, comparison 1; SENIOR / paragraph 1, callout 1 | "later units grow it rather than inventing it all at once"; "That is the order this unit is arguing for."; cell "the honest answer is that it does not matter"; header "**Stateless objects, honestly.**"; "refuse the default-shaped answer" | "Honest" framing and Unit narration; one "shape" coinage. | "It does not matter"; header "Stateless objects"; "refuse the default answer"; drop the Unit narration. | Open |
| LC-198 | `LC-META` | Low | `unit_object_graphs_lifetimes_and_scopes` | `lesson_scope_is_a_rule_owner_is_a_lifetime` | CORE / paragraph 1, callout 2; PRACTICAL / paragraphs 2, 6–7; SENIOR / paragraph 2 | "Separate them once, here, and the rest of this curriculum becomes readable"; "The coroutines curriculum owns the first; this unit means only the second"; "The architecture curriculum already taught how to pick an owner… this lesson applies that conclusion instead of re-deriving it. What construction adds is the part that curriculum did not need"; "Only the fourth answer is scope-shaped"; "The honest phrasing is…"; "durability is a different subject with its own curriculum"; "the previous lesson's over-sharing failure" | The coroutine-scope disambiguation callout is genuinely useful; its ownership clause and the other curriculum references are not. | Keep the disambiguation as a fact ("`CoroutineScope` is a different concept: …"); name **Choosing an Owner From the Lifetime the Requirement Needs** once; "is about scope"; "The accurate phrasing is". | Open |
| LC-199 | `LC-VOCAB` | Low | `unit_object_graphs_lifetimes_and_scopes` | `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraphs 7–10; SENIOR / paragraphs 1–3 | "**The shape that is correct.**"; "reaches the same shape without any library"; "What matters is the split, not the shape"; "the same shape the previous unit's per-use summary builder had"; "Recognising the shape is more useful than remembering the example"; "the half-built object from the lesson on constructor injection… That lesson priced all three"; "belongs to the navigation and lifecycle material"; "each of the three frameworks in this curriculum" | "Shape" five times for "arrangement"/"pattern", plus positional pointers (the costs are restated locally). | "The correct arrangement"; "the same arrangement"; "not whether it is a class or a function"; "Recognising the pattern…"; name **A Dependency Should Be Visible** if a pointer is wanted. | Open |
| LC-200 | `LC-META` | Medium | `unit_object_graphs_lifetimes_and_scopes` | `lesson_when_a_broken_graph_tells_you` | PRACTICAL / paragraphs 5, 7, comparison 2; SENIOR / paragraphs 3–4, bullet list 1 | "That table is the reason this lesson refuses to generalise… Inventing a shared behaviour for a category of tools is how a curriculum acquires claims that are wrong about every member it never checked."; "worth reading as a shape rather than as a feature"; "the hazard the previous unit named"; "the previous unit showed a fully injected program with every arrow wrong"; "So the honest summary of any such check…"; "the architecture curriculum already named"; "the axis the closing unit of this curriculum compares… the unit on Dagger is where…" | The Lesson states its own editorial standard (how a curriculum avoids wrong claims) to the learner. The engineering point — duplicate-key behaviour is tool-specific, so read the tool's reference — is good and needs no justification about curricula. | "What happens on a duplicate key is defined by each mechanism and documented in its reference." Drop the curriculum sentence; "as a model"; name **When Should a Graph Error Surface?** and **Dagger: Compile-Time Object Graphs** once. | Open |
| LC-201 | `LC-META` | Medium | `unit_dagger_compile_time_object_graphs` | — (Unit-level) | Unit summary | "The previous two units made every decision a dependency graph requires… This unit asks one question of each of them… Constructible bindings and…; the declaration forms for…; components as…; scopes read as…; qualifiers and multibindings as… It closes where the tool's reputation is made… Dagger is a curriculum subject here — the examples are authored to be read, not configured." | At ~220 words the longest Unit summary in the corpus: a recap, a Lesson inventory and an authoring note to the reader about how the examples were written. | "Dagger encodes the graph decisions — construction, bindings, graph surface, reuse, keys — as declarations checked at build time. Know what it verifies and what it does not." Delete the authoring note. | Open |
| LC-202 | `LC-CLARITY` | Medium | `unit_dagger_compile_time_object_graphs` | `lesson_dagger_constructs_what_it_can_see` | Lesson summary; CORE / paragraphs 1–6; PRACTICAL / paragraphs 1, 3, 7; SENIOR / paragraph 3 | Summary "Unit 1 wrote the assembly by hand and Unit 2 traced the graph underneath it"; "the reader already decided in Units 1 and 2"; "The first question was the unit-1 one"; "That was the whole argument of the first unit"; "Those are the second and third questions of Unit 2"; "In Unit 1 the composition root read something like this"; "each is a Unit 2 decision the reader already knows how to make" | "Unit 1" and "Unit 2" are numbered within the DI Topic and mean different Units from the Compose and Coroutines "Unit 1"; a summary built on them is unreadable to anyone arriving from the Dagger Unit list. The Lesson also refers to the learner in the third person ("the reader was doing two jobs by hand"). | Replace numbers with the decisions themselves ("construct, fetch or receive"; "which nodes are shared"); address the reader as "you" or not at all; summary: "Dagger builds what your constructors already describe…". | Open |
| LC-203 | `LC-VOICE` | Low | `unit_dagger_compile_time_object_graphs` | — (Unit-level) | L27.1 PRACTICAL / paragraph 3, SENIOR / callout; L27.4 CORE / paragraph 2; L27.5 SENIOR / paragraph 1; L27.6 SENIOR / paragraph 3; L27.7 PRACTICAL / callout 2 | "That sentence is the honest description of the tool"; "Finish with the honesty that carries the answer"; "the honest way to compare them is to ask what the new graph can see"; "the honest answer is that there is nothing to reason from"; "Two honest limits"; "belongs in any honest comparison" | "Honest" as a recurring intensifier. | State the claims directly ("That sentence describes the tool accurately"; "Compare them by what the new graph can see"). | Open |
| LC-204 | `LC-META` | Low | `unit_dagger_compile_time_object_graphs` | — (Unit-level) | L27.1 PRACTICAL / callout 1, SENIOR / paragraph 2; L27.2 PRACTICAL / callout 2, SENIOR / paragraph 3; L27.4 PRACTICAL / callout 2; L27.7 PRACTICAL / paragraph 5, callout 2, SENIOR / paragraph 7, call-out | "That a build step exists is the whole of what this unit says about the build… build-engineering questions with their own curriculum"; "The objective of this unit is the graph decision"; "Build modularisation is a separate subject and this unit does not enter it" (×2); "nothing in this curriculum's selection rule depends on it"; "outside what this unit needs"; "knowing where that boundary runs is most of what this unit was for"; "which is the distinction the whole unit was built to give you" | Scope statements and Unit-purpose statements close many sections, including the final interview call-out. | Keep the engineering caveats ("a build step is required; its configuration is out of scope here" at most once); delete purpose statements. | Open |
| LC-205 | `LC-VOCAB` | Low | `unit_dagger_compile_time_object_graphs` | — (Unit-level) | L27.1 PRACTICAL / paragraph 2, callout 2, SENIOR / paragraph 2; L27.2 PRACTICAL / paragraphs 2, 9, callout 1; L27.3 SENIOR / paragraph 3; L27.6 PRACTICAL / paragraph 5, SENIOR / paragraph 2 | "The shape of what the generated code does"; "not even the right shape of claim"; "the shape of generated output"; "it is the familiar shape"; "where the binding's shape can be expressed by `@Binds`"; "the documented shape is an interface…"; "the shape the first unit argued against"; "The shape to avoid is the obvious one"; "plugin-shaped" | Nine vague uses for "what it does", "kind of claim", "layout", "pattern", "design". | "What the generated code does"; "the right kind of claim"; "the documented layout"; "the service-locator pattern"; "The design to avoid"; "plugin-based". | Open |
| LC-206 | `LC-XREF` | Low | `unit_dagger_compile_time_object_graphs` | — (Unit-level) | Lessons L27.2–L27.7 | "the reading application from the last unit"; "the thing Unit 2 actually drew"; "the distinction the first unit drew"; "The first unit defined the composition root"; summary of L27.6 "Two lessons ago in the previous unit"; "The previous unit left a collision unresolved on purpose"; "the previous unit rejected all of those"; "the previous unit priced both directions" | Positional references to the DI foundation Units throughout; most are followed by a local restatement. | Delete positional phrasing; where a pointer helps, use titles (**Two Dependencies of the Same Type**, **A Scope Is a Rule; an Owner Is a Lifetime**, **One Place That Knows How to Build**). | Open |
| LC-207 | `LC-VOICE` | Medium | `unit_hilt_android_lifecycle_integration` | — (Unit-level) | Unit summary and all six Lesson summaries | Unit: "See how Hilt generates Dagger components around Android owners, then choose component lifetimes, integration boundaries, ViewModel construction, module placement, and custom Dagger structure from requirements."; Lessons: "Separate Dagger's object-graph model from…"; "Choose a generated Hilt component from…"; "Explain field injection and Hilt entry points from…"; "Keep ViewModel construction, ViewModel ownership, dependency scope, and runtime input as four separate decisions."; "Read @InstallIn as graph placement…"; "Choose between Hilt conventions and custom Dagger components by…" | Every summary is a one-line learning objective addressed to the reader as a task list; the Unit reads like a lesson plan in the Lesson list, and the summaries say what to do rather than what is true. | Write each summary as the idea: "Hilt is Dagger plus a predefined Android component hierarchy and generated integration…"; "A Hilt component is chosen by matching the lifetime a requirement needs to the Android owner it follows…". | Open |
| LC-208 | `LC-CLARITY` | Medium | `unit_hilt_android_lifecycle_integration` | — (Unit-level) | L28.1 CORE / paragraph 1, PRACTICAL / paragraph 1; L28.2 CORE / paragraph 1, PRACTICAL / paragraph 1; L28.3 CORE / paragraph 1, SENIOR / paragraph 2; L28.4 PRACTICAL / paragraph 5; L28.5 CORE / paragraph 1 | "Unit 3 put the engineer in charge of a Dagger graph"; "Continue the reading graph from Unit 3"; "Carry Unit 2's reasoning forward unchanged"; "Example B now has a precise requirement"; "Unit 1's normal form was simple"; "This is Unit 1's service-locator nuance made concrete"; "encodes the same split as Unit 2"; "Unit 3 already established what a Dagger module does" | DI-Topic Unit numbers again carry the argument ("Unit 2's reasoning" is the lifetime → owner → scope chain), and "Example B" is a label never defined anywhere in the curriculum. | Replace each with the fact it stands for (the chain is already drawn in the next code block); "the reading application's `ReaderSession`" for "Example B"; titles where a pointer is wanted (**A Scope Is a Rule; an Owner Is a Lifetime**, **Asking For It, or Being Given It**). | Open |
| LC-209 | `LC-META` | Low | `unit_hilt_android_lifecycle_integration` | `lesson_hilt_viewmodels_and_runtime_input` | CORE / paragraph 3; PRACTICAL / paragraph 4 | "this Lesson needs only the first"; "This Lesson does not choose navigation argument APIs or teach saved-state serialization. The point is narrower:" | Scope narration. | "`SavedStateHandle` is one route for navigation-derived values:" and state the narrower point directly. | Open |
| LC-210 | `LC-VOCAB` | Low | `unit_hilt_android_lifecycle_integration` | — (Unit-level) | L28.4 CORE / paragraph 2; L28.6 SENIOR / paragraphs 1–2 | "so Hilt rejects that shape"; "common Android owner shapes"; "becomes the dominant shape" | Vague "shape" for a request pattern, typical ownership structures and a prevailing pattern. | "rejects that request"; "the lifetimes typical Android apps have"; "becomes the rule rather than the exception". | Open |
| LC-211 | `LC-VOICE` | Medium | `unit_koin_and_dependency_injection_in_kmp` | — (Unit-level) | Unit summary and all five Lesson summaries | Unit: "Express the construction, reuse and ownership decisions from the earlier Units with Koin's multiplatform definitions, then complete one shared graph…"; Lessons: "Read Koin startup as composition-root work…, and separate this codebase's classic DSL from modern Koin's wider capabilities."; "Choose `single` or `factory` from…"; "Select a bounded Koin scope when…"; "Separate a Koin ViewModel construction recipe from…"; "Place platform-independent construction in shared code…" | Objective-style imperative summaries (same generation as Hilt), one of which names "this codebase". | Describe the idea in each summary; remove "this codebase". | Resolved — see [Remediation Log](#remediation-log) |
| LC-212 | `LC-META` | Medium | `unit_koin_and_dependency_injection_in_kmp` | `lesson_the_koin_container_and_its_modules` | Lesson summary; CORE / paragraph 2 code; PRACTICAL / paragraphs 1–3 | Summary "separate this codebase's classic DSL…"; `startDesktopLocalDataGraph()` listing `curriculumDataModule`, `learningContentModule`, `assessmentDataModule`, `savedQuestionDataModule`, `lessonStudyDataModule`, `topicStudyPresentationModule`…; "Each lists the same seven shared modules and then adds two platform modules."; "they are not configured in this example and their setup belongs to build engineering rather than this Unit" | The worked example is this learning application's actual startup (Rule 11), named as "this codebase" in the summary; the module names are meaningless to a learner who is not reading this repository. | Keep the pattern with neutral names (`dataModule`, `settingsModule`, `platformDataModule`); frame it as "consider an app with four hosts"; drop "this codebase" and the Unit-scope clause. | Resolved — see [Remediation Log](#remediation-log) |
| LC-213 | `LC-CLARITY` | Medium | `unit_koin_and_dependency_injection_in_kmp` | — (Unit-level) | L29.1 CORE / paragraph 1; L29.2 CORE / paragraph 1, PRACTICAL / paragraph 4; L29.3 CORE / paragraph 2; L29.4 PRACTICAL / paragraph 3 | "Units 1 and 2 made construction explicit"; "Begin with Unit 2's question"; "That is the service-locator shape from Unit 1"; "This encodes Unit 2's generic rule"; "useful evidence of Unit 2's same-type ambiguity" | DI-Topic Unit numbers again name arguments; in L29.4 they sit beside "the destination supplies Unit first, Lesson second", where Unit and Lesson are the example app's domain objects — the two meanings collide in one paragraph. | Name the argument ("the shared-or-fresh question", "a service locator"); use titles if a pointer is needed (**One Instance, or a New One Each Time?**, **Two Dependencies of the Same Type**). | Resolved — see [Remediation Log](#remediation-log) |
| LC-214 | `LC-VOCAB` | Low | `unit_koin_and_dependency_injection_in_kmp` | `lesson_koin_definitions_and_reuse` | PRACTICAL / paragraph 4, code 3 comment; also L29.4 PRACTICAL / callout 1 | "The second shape is materially different."; "That is the service-locator shape from Unit 1."; code comment "// Hidden dependency: service-locator-shaped business code"; "still has the service-locator shape" | "Shape" for "version" and "pattern", including in code. | "The second version"; "a service locator"; code comment "// Hidden dependency: business code acting as a service locator". | Resolved — see [Remediation Log](#remediation-log) |
| LC-215 | `LC-META` | Low | `unit_koin_and_dependency_injection_in_kmp` | `lesson_koin_definitions_and_reuse` | PRACTICAL / paragraph 5 | "An `AppearanceStateHolder` is retained because the theme is applied above navigation… A `StudyProgressStateHolder` is retained because several simultaneously alive learning surfaces must show one learner-owned state projection." | This application's own state holders as evidence, with "learner-owned" product vocabulary. | Generic example ("a theme holder applied above navigation"; "a progress projection several screens show at once"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-216 | `LC-META` | Low | `unit_koin_and_dependency_injection_in_kmp` | `lesson_koin_scopes_and_their_owners` | SENIOR / paragraph 2 | "The worked case-study graph currently declares no custom Koin scopes and no `scoped` definitions… The interview-flow example demonstrates a Koin capability the graph does not presently need; it is not presented as missing architecture." | A dated statement about this repository ("currently", "presently") and an author's disclaimer. | Delete the paragraph; S para 1 already states when a custom scope is worthwhile. | Resolved — see [Remediation Log](#remediation-log) |
| LC-217 | `LC-META` | High | `unit_koin_and_dependency_injection_in_kmp` | `lesson_resolving_viewmodels_at_the_boundary` | SENIOR / paragraphs 1–2; CORE / paragraph 2 | "…it does not fully specify which `ViewModelStoreOwner` this application's navigation arrangement selects. Ownership claims therefore come from the platform ViewModel contract and the shipped lifetime curriculum… When navigation integration changes, the owner selection must be re-verified independently of the Koin definition."; "The case-study graph contains 15 ViewModel definitions: seven without runtime parameters and eight parameterized definitions. Its UI integration contains 20 `koinViewModel()` resolutions across 15 files…"; "the same separation the lifecycle curriculum already established" | The Senior section is an audit of this application (counts of definitions and files), a maintenance instruction for authors, and a reference to "the shipped lifetime curriculum" — product metadata the learner has no way to interpret (Rule 11, Rule 12). | Replace the Senior section with the learner-useful point: "The Koin API resolves the ViewModel; which `ViewModelStoreOwner` it attaches to depends on your navigation integration, so verify ownership from the platform contract, not from the DI call." Delete the counts. | Resolved — see [Remediation Log](#remediation-log) |
| LC-218 | `LC-META` | Medium | `unit_koin_and_dependency_injection_in_kmp` | `lesson_one_graph_across_platforms` | PRACTICAL / paragraphs 1–4; SENIOR / comparison 1, paragraphs 2–3 | The appearance and database graphs of this application named module by module (`androidAppearanceModule`…`createWebCurriculumDatabase()`); table row "This case study starts Koin from webMain — Repository evidence that its selected libraries compile for the configured web targets"; "the next Unit owns that selection"; "The case study proves only a bounded claim: seven shared classic-DSL modules plus two modules per host form a working local-first graph across its configured targets…" | The Practical example is well chosen but is this repository's graph by name; the Senior section reasons about what "the case study" and "repository evidence" prove. | Keep the shared-abstraction/host-binding example with neutral names; drop the "repository evidence" row and the case-study-limits paragraph; replace "the next Unit owns that selection" with a pointer to **Three Projects, Three Answers**. | Resolved — see [Remediation Log](#remediation-log) |
| LC-219 | `LC-VOICE` | Low | `unit_choosing_a_dependency_injection_strategy` | — (Unit-level) | Unit summary and all four Lesson summaries | "Choose the smallest dependency-construction strategy…"; "Compare manual DI, Dagger, Hilt and Koin by…"; "Trace one missing dependency through…"; "Apply one decision method to…"; "Apply architecture proportionality to dependency construction, complete a credible no-container design, and preserve…" | Objective-style imperative summaries (Hilt/Koin generation). | Describe the idea ("A container automates graph work; it adds no capability…"). | Resolved — see [Remediation Log](#remediation-log) |
| LC-220 | `LC-LEAK` | High | `unit_choosing_a_dependency_injection_strategy` | `lesson_when_should_a_graph_error_surface` | SENIOR / paragraph 3 | "The deprecated assessment question `di_framework_tradeoff_compile_vs_runtime` preserves a useful responsibility… A future assessment should keep the axis and replace the brand binary with a mechanism-qualified scenario; this Unit does not reactivate or edit that Question." | An internal Question ID and its status, plus an authoring backlog note about future assessment work, in learner prose. | Delete the paragraph. If the idea is wanted, one sentence: "The old 'Dagger is compile-time, Koin is runtime' framing keeps one useful axis — earlier discovery costs build-time work — but its brand split is obsolete." | Resolved — see [Remediation Log](#remediation-log) |
| LC-221 | `LC-CLARITY` | Low | `unit_choosing_a_dependency_injection_strategy` | `lesson_when_should_a_graph_error_surface` | SENIOR / paragraph 2 | "Unit 3's compiler Lesson and Unit 2's scope-owner Lesson carry the full argument." | DI-Topic numbering plus informal Lesson nicknames; neither can be followed by a learner without the course map. | "See **What the Dagger Compiler Actually Checked** and **A Scope Is a Rule; an Owner Is a Lifetime**." | Resolved — see [Remediation Log](#remediation-log) |
| LC-222 | `LC-VERBOSE` | Low | `unit_choosing_a_dependency_injection_strategy` | `lesson_what_a_container_actually_buys` | PRACTICAL / comparisons 1–2 | Two five-column tables (13 rows) of long cells; rows "Lifetime complexity" and "Graph and lifetime complexity" overlap in subject. | Dense and partly duplicated; the reader has to compare 52 long cells to extract the four-way contrast the prose already states. | Merge the two lifetime rows; shorten cells to the observable difference; consider splitting by question rather than by framework. | Resolved — see [Remediation Log](#remediation-log) |
| LC-223 | `LC-META` | Low | `unit_choosing_a_dependency_injection_strategy` | `lesson_three_projects_three_answers` | PRACTICAL / paragraph 6; SENIOR / paragraph 3 | "Other KMP-compatible approaches may exist outside this Unit's four taught strategies; completeness of the ecosystem is not required to apply the method."; "A learning application with shared Koin definitions across Android, iOS, desktop and web…" | An authoring disclaimer about the Unit's coverage; a lightly disguised reference to this application. | Delete the disclaimer; "An application with a shared Koin graph across four platforms…". | Resolved — see [Remediation Log](#remediation-log) |

## Internal Identifier Inventory

Every token in learner prose that looks like an identifier was classified. **28 confirmed
leaks** — 25 Lesson IDs (18 distinct), 1 Question ID, 1 backlog epic key and 1 backlog epic
reference — in **16 Lessons** across **8 Units** (7, 8, 9, 10, 11, 19, 22, 30). No Unit,
Topic or Subtopic ID and no status value appears in learner prose; no ID appears in a code
block.

| # | Leaked identifier | Kind | Appears in Lesson | Location | Finding | Human-facing replacement, if a pointer is kept |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | `lesson_state_hoisting` | Lesson ID | `lesson_classes_of_screen_state` | CORE / paragraph 2 | LC-040 | **State Hoisting and the Lowest Sensible Owner** |
| 2 | `lesson_stability_and_skipping` | Lesson ID | `lesson_screen_state_and_ui_events` | SENIOR / paragraph 2 | LC-044 | **Stability and Skipping** |
| 3 | `lesson_remember_saveable` | Lesson ID | `lesson_screen_state_owner_boundary` | CORE / paragraph 3 | LC-047 | **`rememberSaveable` and State That Must Survive** |
| 4 | `lesson_snapshot_observation` | Lesson ID | `lesson_external_state_in_compose` | CORE / paragraph 3 | LC-050 | **How Compose Observes State** |
| 5 | `lesson_state_flow` | Lesson ID | `lesson_external_state_in_compose` | PRACTICAL / paragraph 2 | LC-050 | **`StateFlow`: One Current Value** |
| 6 | `lesson_snapshot_flow` | Lesson ID | `lesson_external_state_in_compose` | PRACTICAL / paragraph 2 | LC-050 | **`snapshotFlow` and Crossing Into Flow** |
| 7 | `lesson_state_flow` | Lesson ID | `lesson_collect_as_state` | CORE / paragraph 2 | LC-052 | **`StateFlow`: One Current Value** |
| 8 | `lesson_composition_and_recomposition` | Lesson ID | `lesson_collect_as_state` | CORE / paragraph 4 | LC-052 | **Composition and Recomposition** |
| 9 | E24 | Backlog epic key | `lesson_collect_as_state` | PRACTICAL / paragraph 2 | LC-052 | — (delete) |
| 10 | `lesson_flow_collection_lifetime` | Lesson ID | `lesson_collection_lifetime_and_cost` | CORE / paragraph 3 | LC-055 | **Collection Lifetime and Flow Cancellation** |
| 11 | `lesson_cooperative_cancellation` | Lesson ID | `lesson_collection_lifetime_and_cost` | CORE / paragraph 3 | LC-055 | **Cancellation Is Cooperative** |
| 12 | `lesson_sharing_cold_flows` | Lesson ID | `lesson_collection_lifetime_and_cost` | SENIOR / paragraph 2 | LC-055 | **Sharing Cold Flows with `stateIn` and `shareIn`** |
| 13 | `lesson_sharing_cold_flows` | Lesson ID | `lesson_lifecycle_aware_collection` | SENIOR / paragraph 2 | LC-057 | **Sharing Cold Flows with `stateIn` and `shareIn`** |
| 14 | `lesson_composable_execution` | Lesson ID | `lesson_why_effects_are_controlled` | CORE / paragraph 3 | LC-059 | **What a Composable Is and How It Executes** |
| 15 | `lesson_composition_and_recomposition` | Lesson ID | `lesson_why_effects_are_controlled` | CORE / paragraph 3 | LC-059 | **Composition and Recomposition** |
| 16 | `lesson_coroutine_scope_ownership` | Lesson ID | `lesson_launched_effect` | PRACTICAL / paragraph 2 | LC-061 | **`CoroutineScope` and Who Owns a Coroutine's Lifetime** |
| 17 | `lesson_cooperative_cancellation` | Lesson ID | `lesson_launched_effect` | PRACTICAL / paragraph 2 | LC-061 | **Cancellation Is Cooperative** |
| 18 | `lesson_composable_identity` | Lesson ID | `lesson_launched_effect` | PRACTICAL / paragraph 3 | LC-061 | **Composable Identity** |
| 19 | `lesson_remember_key_memoization` | Lesson ID | `lesson_effect_keys_as_dependencies` | PRACTICAL / paragraph 1 | LC-063 | **`remember(key)` as Memoization** |
| 20 | `lesson_stability_and_skipping` | Lesson ID | `lesson_effect_keys_as_dependencies` | PRACTICAL / paragraph 3 | LC-063 | **Stability and Skipping** |
| 21 | `lesson_coroutine_builders` | Lesson ID | `lesson_remember_coroutine_scope` | CORE / paragraph 2 | LC-069 | **Starting Coroutines: `launch`, `async` and `runBlocking`** |
| 22 | `lesson_coroutine_scope_ownership` | Lesson ID | `lesson_remember_coroutine_scope` | PRACTICAL / paragraph 3 | LC-069 | **`CoroutineScope` and Who Owns a Coroutine's Lifetime** |
| 23 | `lesson_effect_keys_as_dependencies` | Lesson ID | `lesson_disposable_effect` | PRACTICAL / paragraph 5 | LC-073 | **What an Effect's Keys Declare** |
| 24 | `lesson_cancellation_cleanup_and_timeouts` | Lesson ID | `lesson_disposable_effect` | SENIOR / paragraph 1 | LC-073 | **Cleanup, `NonCancellable` and Timeouts** |
| 25 | `lesson_flow_builders_and_callback_adapters` | Lesson ID | `lesson_flow_adapter_or_compose_producer` | SENIOR / paragraph 2 | LC-077 | **Flow Builders and Adapting Callback APIs** |
| 26 | `lesson_state_hoisting` | Lesson ID | `lesson_responsibility_and_change` | PRACTICAL / paragraph 8 | LC-134 | **State Hoisting and the Lowest Sensible Owner** |
| 27 | "the epic's own rule" | Backlog epic reference | `lesson_policy_and_framework_detail` | SENIOR / paragraph 1 | LC-165 | — (delete) |
| 28 | `di_framework_tradeoff_compile_vs_runtime` | Question ID (deprecated) | `lesson_when_should_a_graph_error_surface` | SENIOR / paragraph 3 | LC-220 | — (delete) |

Related product-metadata phrasing that is not an identifier, filed as `LC-META`: "the shipped
lifetime curriculum" (LC-217), "this codebase" in a Lesson summary (LC-212), "the application
you are reading this in" (LC-154).

Tokens reviewed and **dismissed** as real technical identifiers:

| Token | Where | Why it stays |
| --- | --- | --- |
| `lesson_study` (×4) | **Who Defines the Abstraction?**, PRACTICAL / paragraph 6 | Package paths in a worked example. The paragraph has a separate Rule 11 problem (LC-167), but the tokens are not curriculum IDs. |
| `is_overdue` | **What Architecture Actually Decides**, SENIOR / comparison 1 | A database column in the example; the contract's own example of a legitimate snake_case identifier |
| `CoffeeMaker_Factory` | **What Dagger Can Construct on Its Own**, PRACTICAL / paragraph 4 | Quoted from Dagger's documentation as an example generated class name |
| `DEFAULT_CONCURRENCY` | **Flattening: Should New Input Cancel Old Work?**, PRACTICAL / paragraph 3 | A kotlinx.coroutines constant |
| `MAX_VALUE` (×2) | **`catch`, `retry` and `onCompletion`** | `Long.MAX_VALUE` |

## Cross-Reference Audit

588 prose references to other Lessons, Units, "curricula" or course positions were classified.

| Decision | Count |
| --- | ---: |
| Keep | 10 |
| Rewrite using human title | 185 |
| Remove | 349 |
| Bring required context local | 44 |
| **Total** | **588** |

What the references look like, in order of frequency:

- **Positional narration** — "the previous unit", "the next lesson", "two lessons from here",
  "the first lesson of this unit", "earlier in this unit". Almost always followed by the fact
  it points at, which is why most are Remove.
- **Informal names for other Units** — "the foundations unit", "the state-holder unit", "the
  data-ownership unit", "the failure unit", "the hot streams unit", "the Compose curriculum",
  "the streams curriculum". Useful pointers here are Rewrite using human title.
- **Topic-local Unit numbers** — "Unit 2's reasoning", "Unit 9's effect model", "the
  unit-1 one". Where the number stands in for a concept, the decision is Bring context local.
- **Curricula and Units that do not exist** — "the performance unit", "the lazy-layout unit",
  "the build and modularization curriculum", "the testing curriculum", "the background-work
  curriculum", "the persistence curriculum", "the lifecycle-and-navigation curriculum", "the
  language curriculum", "the Kotlin Multiplatform curriculum". Remove, or state the one fact.
- **Internal IDs** — covered in the inventory; each is Rewrite using human title or Remove.

"Rewrite using human title" records that a pointer is *worth keeping*; the rewrite may still
decide the Lesson reads better without it. Where a direction in the ledger names a title,
that title exists in the curriculum at the audited commit.

References already acceptable (Keep) and references that currently substitute for
explanation (Bring required context local):

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_composition_and_recomposition` | SENIOR / paragraph 4 | belongs to the performance unit (no such Unit) | Bring required context local |
| `lesson_recomposition_scopes` | SENIOR / paragraph 3 | it belongs to the performance unit (no such Unit) | Bring required context local |
| `lesson_composable_identity` | SENIOR / paragraph 2 | the over-specified-key case from Unit 2 | Bring required context local |
| `lesson_keys_and_identity_in_lists` | PRACTICAL / callout 1 | Those are the second and third questions of this unit | Bring required context local |
| `lesson_stability_and_skipping` | Lesson summary | The third question of the unit | Bring required context local |
| `lesson_stability_annotations` | SENIOR / paragraph 6 | what makes the previous lesson's failures impossible | Bring required context local |
| `lesson_stability_annotations` | SENIOR / paragraph 7 | the observation the previous lesson recorded and told you not to design against | Bring required context local |
| `lesson_snapshot_flow` | CORE / paragraph 3 | taught by the Flow Fundamentals unit of this curriculum… both linked below | Keep |
| `lesson_snapshot_flow` | CORE / bullet list 2 | an observing context in exactly the sense of the previous lesson | Bring required context local |
| `lesson_stateless_screen_content` | CORE / paragraph 3 | the stateful/stateless overload pair from the earlier hoisting lesson | Bring required context local |
| `lesson_external_state_in_compose` | CORE / paragraph 3 | the snapshot observation model from `lesson_snapshot_observation` | Bring required context local |
| `lesson_disposable_effect` | PRACTICAL / callout 1 | a publication, which is the next lesson's subject, or a coroutine, which is Unit 9's | Bring required context local |
| `lesson_disposable_effect` | SENIOR / paragraph 1 | `lesson_cancellation_cleanup_and_timeouts` already covers how a cancelled coroutine unwinds | Bring required context local |
| `lesson_produce_state` | CORE / paragraph 3 | all four come from Unit 9's effect model | Bring required context local |
| `lesson_produce_state` | SENIOR / paragraph 4 | the purpose-built conversion is Unit 8's | Bring required context local |
| `lesson_flow_adapter_or_compose_producer` | SENIOR / paragraph 4 | Unit 8's collection is what does that | Bring required context local |
| `lesson_transient_effect_delivery` | PRACTICAL / paragraph 2 | The stream lesson measured exactly that… this lesson cites it rather than re-deriving it | Bring required context local |
| `lesson_transient_effect_delivery` | PRACTICAL / comparison 1 | the shape the stream curriculum calls current-value semantics | Bring required context local |
| `lesson_coroutine_builders` | PRACTICAL / paragraph 3 | a second half to this that the failure unit owns | Bring required context local |
| `lesson_structured_concurrency` | SENIOR / paragraph 3 | telling those two apart is the next unit's job | Bring required context local |
| `lesson_with_context_and_main_safety` | SENIOR / paragraph 4 | a connection from the Compose lesson Keeping Work Out of Composition | Keep |
| `lesson_supervision_and_failure_isolation` | PRACTICAL / paragraph 2 | in the vocabulary of the previous Lesson, it has no propagation path | Bring required context local |
| `lesson_flow_buffering_and_conflation` | SENIOR / paragraph 4 | the state-holder types the hot streams unit introduces | Bring required context local |
| `lesson_what_a_repository_owns` | CORE / paragraph 2 | the question the foundations unit taught | Bring required context local |
| `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 6 | the reminder-time preference from the first lesson of this unit | Bring required context local |
| `lesson_when_a_domain_layer_earns_its_place` | PRACTICAL / paragraph 1 | The reminder-time preference from the data-ownership unit | Bring required context local |
| `lesson_dependency_inversion_in_practice` | CORE / paragraph 2 | the one the earlier lesson already established | Bring required context local |
| `lesson_one_screen_five_questions` | CORE / bullet list 1 | ownership in the sense the foundations unit fixed | Bring required context local |
| `lesson_one_screen_five_questions` | PRACTICAL / paragraph 1 | One hypothetical screen runs through this lesson and the three that follow | Keep |
| `lesson_mvp_view_contract` | CORE / paragraph 1 | the practice-configuration screen from the previous lesson | Bring required context local |
| `lesson_mvp_view_contract` | PRACTICAL / bullet list 1 | a boundary in the foundations unit's exact sense | Bring required context local |
| `lesson_delivery_guarantees` | PRACTICAL / paragraph 3 | Two conclusions from the streams curriculum apply immediately and are not re-derived here | Bring required context local |
| `lesson_smallest_sufficient_architecture` | PRACTICAL / paragraph 1 | the reminder-time setting the domain unit already used | Bring required context local |
| `lesson_a_dependency_should_be_visible` | PRACTICAL / paragraph 4 | The architecture curriculum's lesson on when an interface is a boundary gives the test | Keep |
| `lesson_injected_inverted_or_both` | PRACTICAL / callout 1 | The architecture curriculum already states the consequence exactly: "…" | Bring required context local |
| `lesson_injected_inverted_or_both` | PRACTICAL / comparison 2 | Version A of the first lesson | Bring required context local |
| `lesson_injected_inverted_or_both` | PRACTICAL / comparison 2 | Version B of the first lesson… every cost the fifth lesson prices | Bring required context local |
| `lesson_when_wiring_it_yourself_is_enough` | SENIOR / paragraph 3 | The architecture curriculum closes on a test | Keep |
| `UNIT:unit_object_graphs_lifetimes_and_scopes` | Unit summary | Dagger, Hilt and Koin are later notations for exactly these decisions | Keep |
| `lesson_two_dependencies_of_the_same_type` | PRACTICAL / paragraph 5 | The test is the familiar one from the architecture material | Keep |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 3 | The first question was the unit-1 one… the last two units traced | Bring required context local |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 6 | the second and third questions of Unit 2 | Bring required context local |
| `lesson_declaring_the_rest_of_the_graph` | PRACTICAL / paragraph 3 | the condition the architecture material sets for an interface earning its place | Keep |
| `lesson_which_graph_owns_this_binding` | SENIOR / paragraph 3 | the shape the first unit argued against | Bring required context local |
| `lesson_when_the_type_is_not_the_key` | Lesson summary | Two lessons ago in the previous unit, a consumer asked for an `HttpClient` | Bring required context local |
| `lesson_when_the_type_is_not_the_key` | SENIOR / paragraph 2 | the inversion the architecture material described | Keep |
| `lesson_hilt_is_dagger_with_decisions_made` | CORE / paragraph 1 | Unit 3 put the engineer in charge of a Dagger graph | Bring required context local |
| `lesson_which_android_component_owns_this` | CORE / paragraph 1 | Carry Unit 2's reasoning forward unchanged | Bring required context local |
| `lesson_which_android_component_owns_this` | PRACTICAL / paragraph 1 | Example B now has a precise requirement | Bring required context local |
| `lesson_hilt_viewmodels_and_runtime_input` | PRACTICAL / paragraph 5 | encodes the same split as Unit 2 | Bring required context local |
| `lesson_the_koin_container_and_its_modules` | CORE / paragraph 1 | Units 1 and 2 made construction explicit | Bring required context local |
| `lesson_koin_definitions_and_reuse` | CORE / paragraph 1 | Begin with Unit 2's question | Bring required context local |
| `lesson_koin_scopes_and_their_owners` | CORE / paragraph 2 | This encodes Unit 2's generic rule | Bring required context local |
| `lesson_the_smallest_sufficient_strategy` | CORE / paragraph 1 | The architecture curriculum ended with proportionality | Keep |

Per Unit:

| # | Unit ID | Keep | Rewrite using human title | Remove | Bring required context local | Total |
| ---: | --- | ---: | ---: | ---: | ---: | ---: |
| 1 | `unit_thinking_in_compose` | 0 | 4 | 2 | 0 | 6 |
| 2 | `unit_state_and_state_ownership` | 0 | 13 | 8 | 0 | 21 |
| 3 | `unit_recomposition` | 0 | 3 | 7 | 2 | 12 |
| 4 | `unit_identity_keys_and_stability` | 0 | 5 | 11 | 5 | 21 |
| 5 | `unit_derived_state_and_expensive_work` | 0 | 3 | 11 | 0 | 14 |
| 6 | `unit_snapshot_fundamentals` | 1 | 3 | 9 | 1 | 14 |
| 7 | `unit_production_screen_state_and_udf` | 0 | 3 | 8 | 1 | 12 |
| 8 | `unit_observable_state_collection` | 0 | 9 | 8 | 1 | 18 |
| 9 | `unit_effect_lifecycle_and_launched_effect` | 0 | 4 | 6 | 0 | 10 |
| 10 | `unit_latest_values_and_event_driven_work` | 0 | 2 | 4 | 0 | 6 |
| 11 | `unit_cleanup_synchronization_and_producers` | 0 | 3 | 9 | 5 | 17 |
| 12 | `unit_production_ui_effects_and_selection` | 0 | 3 | 15 | 2 | 20 |
| 13 | `unit_coroutines_and_structured_concurrency` | 0 | 11 | 15 | 2 | 28 |
| 14 | `unit_context_dispatchers_and_concurrency` | 1 | 5 | 12 | 0 | 18 |
| 15 | `unit_cancellation_failure_and_coordination` | 0 | 5 | 6 | 1 | 12 |
| 16 | `unit_flow_fundamentals` | 0 | 9 | 19 | 0 | 28 |
| 17 | `unit_flow_composition_timing_and_failure` | 0 | 3 | 8 | 1 | 12 |
| 18 | `unit_stateflow_sharedflow_and_hot_streams` | 0 | 9 | 16 | 0 | 25 |
| 19 | `unit_architecture_responsibilities_and_boundaries` | 0 | 6 | 13 | 0 | 19 |
| 20 | `unit_screen_state_holders_and_ui_state` | 0 | 8 | 15 | 0 | 23 |
| 21 | `unit_repositories_and_data_ownership` | 0 | 6 | 16 | 2 | 24 |
| 22 | `unit_domain_logic_and_dependency_direction` | 0 | 10 | 20 | 2 | 32 |
| 23 | `unit_responsibility_models_mvp_mvvm_mvi` | 1 | 11 | 22 | 3 | 37 |
| 24 | `unit_state_events_lifetime_and_selection` | 0 | 11 | 17 | 2 | 30 |
| 25 | `unit_dependency_injection_as_object_construction` | 2 | 11 | 13 | 3 | 29 |
| 26 | `unit_object_graphs_lifetimes_and_scopes` | 2 | 9 | 22 | 0 | 33 |
| 27 | `unit_dagger_compile_time_object_graphs` | 2 | 9 | 29 | 4 | 44 |
| 28 | `unit_hilt_android_lifecycle_integration` | 0 | 2 | 3 | 4 | 9 |
| 29 | `unit_koin_and_dependency_injection_in_kmp` | 0 | 4 | 3 | 3 | 10 |
| 30 | `unit_choosing_a_dependency_injection_strategy` | 1 | 1 | 2 | 0 | 4 |
| | **Total** | 10 | 185 | 349 | 44 | 588 |

Full inventory, by Unit:

<details>
<summary>1. Thinking in Compose — 6 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_composable_execution` | CORE / paragraph 2 | everything later in the Compose path | Remove |
| `lesson_composable_execution` | CORE / paragraph 3 | a mechanism with its own unit later in this path (recomposition) | Rewrite using human title |
| `lesson_composable_execution` | PRACTICAL / paragraph 3 | Those APIs have their own unit later in this path (effects) | Rewrite using human title |
| `lesson_state_down_events_up` | CORE / paragraph 3 | it has its own lesson later in this path (owner choice) | Rewrite using human title |
| `lesson_state_down_events_up` | SENIOR / paragraph 2 | Those belong to the architecture topic | Remove |
| `lesson_state_down_events_up` | SENIOR / paragraph 3 | its own lesson later in this path (hoisting) | Rewrite using human title |

</details>

<details>
<summary>2. State and State Ownership — 21 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_observable_state` | CORE / paragraph 1 | The previous unit left a deliberate gap | Remove |
| `lesson_observable_state` | CORE / paragraph 7 | It has its own unit later in this path (snapshots) | Rewrite using human title |
| `lesson_observable_state` | CORE / paragraph 7 | Everything in this unit and the next works from the contract | Remove |
| `lesson_observable_state` | PRACTICAL / callout 1 | the subject of the Recomposition unit | Rewrite using human title |
| `lesson_observable_state` | PRACTICAL / paragraph 5 | the distinction the next lesson exists for | Rewrite using human title |
| `lesson_observable_state` | PRACTICAL / paragraph 7 | which the recomposition unit covers | Rewrite using human title |
| `lesson_observable_state` | PRACTICAL / paragraph 7 | the previous unit's lesson on data flow direction warned about | Remove |
| `lesson_observable_state` | SENIOR / paragraph 2 | the reasoning the next unit is built on | Remove |
| `lesson_observable_state` | SENIOR / paragraph 3 | That failure has its own lesson at the end of this unit | Rewrite using human title |
| `lesson_remember_composition_memory` | CORE / paragraph 1 | The previous lesson ended on a counter… | Remove |
| `lesson_remember_composition_memory` | PRACTICAL / callout 1 | Which mechanism does is the next lesson | Rewrite using human title |
| `lesson_remember_composition_memory` | SENIOR / paragraph 2 | which is hoisting, two lessons from here | Rewrite using human title |
| `lesson_remember_composition_memory` | SENIOR / paragraph 3 | composition identity… its own unit later in this path | Rewrite using human title |
| `lesson_remember_saveable` | SENIOR / paragraph 2 | which is the next Lesson | Rewrite using human title |
| `lesson_remember_saveable` | SENIOR / paragraph 4 | The Production Screen State and Unidirectional Data Flow unit later in this path covers… | Rewrite using human title |
| `lesson_remember_saveable` | SENIOR / paragraph 4 | detailed state-holder architecture remains later curriculum | Remove |
| `lesson_state_hoisting` | CORE / paragraph 3 | the same direction the previous unit established | Remove |
| `lesson_state_hoisting` | PRACTICAL / paragraph 3 | the reason the next unit treats read location as a design decision | Rewrite using human title |
| `lesson_state_hoisting` | PRACTICAL / paragraph 5 | The Production Screen State and Unidirectional Data Flow unit covers that Compose-side boundary | Rewrite using human title |
| `lesson_state_hoisting` | PRACTICAL / paragraph 5 | detailed state-holder design remains later curriculum | Remove |
| `lesson_observable_collections` | SENIOR / paragraph 3 | the subject of the identity and stability unit | Rewrite using human title |

</details>

<details>
<summary>3. Recomposition — 12 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_composition_and_recomposition` | CORE / paragraph 1 | Two units have now supplied the halves… The first said… The second said… | Remove |
| `lesson_composition_and_recomposition` | PRACTICAL / paragraph 6 | the previous unit's rule about side effects | Remove |
| `lesson_composition_and_recomposition` | PRACTICAL / paragraph 7 | effect APIs… have their own unit later in this path | Rewrite using human title |
| `lesson_composition_and_recomposition` | SENIOR / paragraph 4 | a fact worth carrying into the next lesson | Remove |
| `lesson_composition_and_recomposition` | SENIOR / paragraph 4 | belongs to the performance unit (no such Unit) | Bring required context local |
| `lesson_recomposition_scopes` | CORE / paragraph 1 | The previous lesson said the runtime invalidates… | Remove |
| `lesson_recomposition_scopes` | PRACTICAL / paragraph 2 | which is the subject of the next unit | Rewrite using human title |
| `lesson_recomposition_scopes` | SENIOR / paragraph 3 | it belongs to the performance unit (no such Unit) | Bring required context local |
| `lesson_recomposition_cost` | Lesson summary | so that the units on stability and derived state are read as tools | Remove |
| `lesson_recomposition_cost` | CORE / paragraph 1 | it turns the next two units into a checklist | Remove |
| `lesson_recomposition_cost` | CORE / paragraph 4 | what the unit on keeping work out of composition is about | Rewrite using human title |
| `lesson_recomposition_cost` | PRACTICAL / paragraph 4 | The tools for that are the subject of later units | Remove |

</details>

<details>
<summary>4. Identity, Keys, Stability and Immutability — 21 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_composable_identity` | CORE / paragraph 1 | Three units have built up a working model | Remove |
| `lesson_composable_identity` | CORE / paragraph 3 | The second was Unit 2's subject… the third is two lessons away | Rewrite using human title |
| `lesson_composable_identity` | CORE / paragraph 5 | a question the `remember` lesson deferred | Remove |
| `lesson_composable_identity` | PRACTICAL / paragraph 5 | the earlier lesson on where to hoist state is the decision procedure | Rewrite using human title |
| `lesson_composable_identity` | SENIOR / paragraph 2 | the over-specified-key case from Unit 2 | Bring required context local |
| `lesson_composable_identity` | SENIOR / paragraph 3 | which is the next lesson | Rewrite using human title |
| `lesson_keys_and_identity_in_lists` | CORE / paragraph 1 | The previous lesson said identity comes from the call site | Remove |
| `lesson_keys_and_identity_in_lists` | PRACTICAL / callout 1 | Those are the second and third questions of this unit | Bring required context local |
| `lesson_keys_and_identity_in_lists` | SENIOR / paragraph 4 | belong to the lazy-layout unit later in this path (no such Unit) | Remove |
| `lesson_immutability_vs_stability` | CORE / paragraph 1 | The lesson on observable collections established the first half | Remove |
| `lesson_immutability_vs_stability` | PRACTICAL / paragraph 3 | the failure the earlier lesson covered | Remove |
| `lesson_immutability_vs_stability` | PRACTICAL / bullet list 1 | That is skipping, and it is the next lesson | Rewrite using human title |
| `lesson_stability_and_skipping` | Lesson summary | The third question of the unit | Bring required context local |
| `lesson_stability_and_skipping` | CORE / paragraph 4 | precisely the gap the previous lesson took apart | Remove |
| `lesson_stability_and_skipping` | PRACTICAL / paragraph 6 | which is the next lesson's subject | Rewrite using human title |
| `lesson_stability_and_skipping` | SENIOR / paragraph 5 | The previous unit's argument stands unchanged | Remove |
| `lesson_stability_and_skipping` | SENIOR / paragraph 5 | belong to the performance unit later in this path (no such Unit) | Remove |
| `lesson_stability_annotations` | CORE / paragraph 1 | The previous lesson left one thread open | Remove |
| `lesson_stability_annotations` | PRACTICAL / paragraph 3 | using the entity type from the previous lesson | Remove |
| `lesson_stability_annotations` | SENIOR / paragraph 6 | what makes the previous lesson's failures impossible | Bring required context local |
| `lesson_stability_annotations` | SENIOR / paragraph 7 | the observation the previous lesson recorded and told you not to design against | Bring required context local |

</details>

<details>
<summary>5. Derived State and Expensive Work — 14 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous two units answered when a composable runs | Remove |
| `lesson_remember_key_memoization` | CORE / paragraph 1 | Unit 2 introduced `remember(key)` | Remove |
| `lesson_remember_key_memoization` | CORE / callout 1 | the comparison rule the previous unit taught | Remove |
| `lesson_remember_key_memoization` | CORE / paragraph 4 | Unit 3 left this exact function open as a problem | Remove |
| `lesson_remember_key_memoization` | PRACTICAL / paragraph 1 | The two directions were named in Unit 2 | Remove |
| `lesson_remember_key_memoization` | PRACTICAL / callout 1 | the third lesson of this unit is about the thing that does | Rewrite using human title |
| `lesson_remember_key_memoization` | SENIOR / paragraph 2 | Unit 3's point about counting | Remove |
| `lesson_remember_key_memoization` | SENIOR / paragraph 3 | Unit 2's positional-identity point applies unchanged | Remove |
| `lesson_derived_state` | SENIOR / paragraph 4 | the under-specified key of the first lesson in this unit | Remove |
| `lesson_derived_state` | SENIOR / paragraph 7 | the next unit's subject | Rewrite using human title |
| `lesson_work_outside_composition` | CORE / paragraph 1 | none of the previous lessons asked | Remove |
| `lesson_work_outside_composition` | CORE / paragraph 2 | Unit 1's execution contract, unchanged | Remove |
| `lesson_work_outside_composition` | PRACTICAL / paragraph 6 | they belong to the effects and coroutine material later in this path | Rewrite using human title |
| `lesson_work_outside_composition` | SENIOR / paragraph 3 | Unit 3's standard for turning an observation into a finding is unchanged | Remove |

</details>

<details>
<summary>6. Snapshot Fundamentals — 14 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | Four units have each answered a different question | Remove |
| Unit summary | Unit summary | The second lesson takes the same model somewhere the UI is not | Remove |
| `lesson_snapshot_observation` | Lesson summary | The last four units each ended with a rule | Remove |
| `lesson_snapshot_observation` | CORE / paragraph 1 | Each of the last four units left you holding a rule | Remove |
| `lesson_snapshot_observation` | CORE / bullet list 1 | as Unit 3 explained | Remove |
| `lesson_snapshot_observation` | PRACTICAL / paragraph 3 | Unit 2's lesson on collections and observable mutation… its three fixes live there | Rewrite using human title |
| `lesson_snapshot_observation` | PRACTICAL / paragraph 5 | the identity, stability and derived-state units' subjects | Rewrite using human title |
| `lesson_snapshot_observation` | SENIOR / paragraph 3 | The next lesson is built directly on that property | Remove |
| `lesson_snapshot_observation` | SENIOR / paragraph 4 | the conclusion Unit 2 reached about collections and nested data | Remove |
| `lesson_snapshot_flow` | CORE / paragraph 3 | taught by the Flow Fundamentals unit of this curriculum… both linked below | Keep |
| `lesson_snapshot_flow` | CORE / paragraph 3 | Operators, hot streams and sharing, and back pressure follow in the units after it | Remove |
| `lesson_snapshot_flow` | CORE / bullet list 2 | an observing context in exactly the sense of the previous lesson | Bring required context local |
| `lesson_snapshot_flow` | CORE / callout 1 | the previous lesson's model with a different kind of dependent attached | Remove |
| `lesson_snapshot_flow` | PRACTICAL / paragraph 1 | the effect APIs as a family… are a later unit's subject | Rewrite using human title |

</details>

<details>
<summary>7. Production Screen State and Unidirectional Data Flow — 12 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | applies the earlier state-hoisting and state-down/events-up foundations | Remove |
| `lesson_classes_of_screen_state` | CORE / paragraph 1 | The earlier state-hoisting lesson asks where one value should live | Remove |
| `lesson_classes_of_screen_state` | CORE / paragraph 2 | `lesson_state_hoisting` contains the full ownership argument | Rewrite using human title |
| `lesson_stateless_screen_content` | CORE / paragraph 1 | at the level chosen in the previous lesson | Remove |
| `lesson_stateless_screen_content` | CORE / paragraph 3 | the stateful/stateless overload pair from the earlier hoisting lesson | Bring required context local |
| `lesson_stateless_screen_content` | CODE / code 1 comment | Observable-state conversion is the next unit's subject | Remove |
| `lesson_screen_state_and_ui_events` | CORE / paragraph 1 | Detailed state modelling belongs to later architecture curriculum | Remove |
| `lesson_screen_state_and_ui_events` | SENIOR / paragraph 2 | This connects to `lesson_stability_and_skipping` | Rewrite using human title |
| `lesson_screen_state_owner_boundary` | CORE / paragraph 3 | introduced in their owning curricula | Remove |
| `lesson_screen_state_owner_boundary` | CORE / paragraph 3 | The earlier `lesson_remember_saveable` provides the lifetime ladder | Rewrite using human title |
| `lesson_screen_state_owner_boundary` | PRACTICAL / paragraph 4 | the next unit's subject | Remove |
| `lesson_screen_state_owner_boundary` | SENIOR / paragraph 3 | This is where the Compose curriculum deliberately stops… application-architecture and dependency-injection decisions | Remove |

</details>

<details>
<summary>8. Observable State Collection and Lifecycle — 18 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_external_state_in_compose` | CORE / paragraph 1 | Unit 7 ended with a screen-level owner | Remove |
| `lesson_external_state_in_compose` | CORE / paragraph 3 | the snapshot observation model from `lesson_snapshot_observation` | Bring required context local |
| `lesson_external_state_in_compose` | PRACTICAL / paragraph 1 | the same question-library boundary as Unit 7, one step later | Remove |
| `lesson_external_state_in_compose` | PRACTICAL / paragraph 2 | That contract belongs to `lesson_state_flow` | Rewrite using human title |
| `lesson_external_state_in_compose` | PRACTICAL / paragraph 2 | remains the subject of `lesson_snapshot_flow` | Rewrite using human title |
| `lesson_external_state_in_compose` | PRACTICAL / paragraph 3 | the previewable, reusable contract Unit 7 established | Remove |
| `lesson_external_state_in_compose` | PRACTICAL / paragraph 3 | The next lesson names the purpose-built conversion API | Remove |
| `lesson_collect_as_state` | CORE / paragraph 1 | the dependency the previous lesson was missing | Remove |
| `lesson_collect_as_state` | CORE / paragraph 2 | already taught in `lesson_state_flow` | Rewrite using human title |
| `lesson_collect_as_state` | CORE / paragraph 4 | `lesson_composition_and_recomposition` owns the deeper recomposition model | Remove |
| `lesson_collect_as_state` | PRACTICAL / paragraph 2 | Operator semantics remain E24's subject | Rewrite using human title |
| `lesson_collect_as_state` | PRACTICAL / paragraph 3 | the Flow curriculum owns context preservation and dispatcher placement | Rewrite using human title |
| `lesson_collect_as_state` | PRACTICAL / callout 1 | later units make that distinction precise | Remove |
| `lesson_collect_as_state` | SENIOR / paragraph 1 | Unit 11 teaches `produceState` as its own mechanism | Rewrite using human title |
| `lesson_collection_lifetime_and_cost` | CORE / paragraph 3 | `lesson_flow_collection_lifetime` and `lesson_cooperative_cancellation` own those mechanics | Rewrite using human title |
| `lesson_collection_lifetime_and_cost` | SENIOR / paragraph 2 | `lesson_sharing_cold_flows` teaches `stateIn`, `shareIn`… | Rewrite using human title |
| `lesson_lifecycle_aware_collection` | SENIOR / paragraph 1 | Unit 11 remains responsible for teaching `produceState`… lifecycle curriculum remains responsible for `repeatOnLifecycle` | Remove |
| `lesson_lifecycle_aware_collection` | SENIOR / paragraph 2 | sharing policy taught in `lesson_sharing_cold_flows` | Rewrite using human title |

</details>

<details>
<summary>9. Effect Lifecycle and LaunchedEffect — 10 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_why_effects_are_controlled` | CORE / paragraph 3 | belong to `lesson_composable_execution` and `lesson_composition_and_recomposition`. This unit applies them. | Remove |
| `lesson_why_effects_are_controlled` | SENIOR / paragraph 1 | later units provide the remaining mechanisms | Remove |
| `lesson_why_effects_are_controlled` | SENIOR / paragraph 2 | architecture material outside this unit | Remove |
| `lesson_launched_effect` | PRACTICAL / paragraph 2 | `lesson_coroutine_scope_ownership` and `lesson_cooperative_cancellation` | Rewrite using human title |
| `lesson_launched_effect` | PRACTICAL / paragraph 3 | `lesson_composable_identity` contains the full identity model | Rewrite using human title |
| `lesson_launched_effect` | SENIOR / paragraph 1 | dispatcher selection remains coroutine curriculum | Remove |
| `lesson_effect_keys_as_dependencies` | CORE / paragraph 2 | A later unit solves that latest-value problem | Rewrite using human title |
| `lesson_effect_keys_as_dependencies` | PRACTICAL / paragraph 1 | `lesson_remember_key_memoization` owns the memoization half | Remove |
| `lesson_effect_keys_as_dependencies` | PRACTICAL / paragraph 3 | `lesson_stability_and_skipping` owns stability and strong skipping | Remove |
| `lesson_effect_key_failures` | SENIOR / paragraph 3 | Unit 10 starts from that tension and supplies the appropriate mechanism | Rewrite using human title |

</details>

<details>
<summary>10. Latest-Value Effects and Event-Driven Coroutine Work — 6 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_remember_updated_state` | CORE / paragraph 1 | Unit 9 gave effect keys a lifetime meaning | Remove |
| `lesson_remember_updated_state` | SENIOR / paragraph 2 | Unit 9 explains how equality and current compiler memoization affect… | Rewrite using human title |
| `lesson_remember_coroutine_scope` | CORE / paragraph 2 | covered by `lesson_coroutine_builders` | Rewrite using human title |
| `lesson_remember_coroutine_scope` | PRACTICAL / paragraph 2 | the uncontrolled-body-work defect from Unit 9 | Remove |
| `lesson_remember_coroutine_scope` | PRACTICAL / paragraph 3 | `lesson_coroutine_scope_ownership` and the structured-concurrency material own the deeper Job rules | Remove |
| `lesson_who_owns_the_trigger` | SENIOR / paragraph 1 | belongs to later architecture curriculum | Remove |

</details>

<details>
<summary>11. Cleanup, External Synchronization and State Producers — 17 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_disposable_effect` | CORE / paragraph 1 | Unit 9 and Unit 10 asked who owns a coroutine | Remove |
| `lesson_disposable_effect` | PRACTICAL / paragraph 5 | `lesson_effect_keys_as_dependencies` owns the key-comparison rules | Remove |
| `lesson_disposable_effect` | PRACTICAL / callout 1 | a publication, which is the next lesson's subject, or a coroutine, which is Unit 9's | Bring required context local |
| `lesson_disposable_effect` | SENIOR / paragraph 1 | `lesson_cancellation_cleanup_and_timeouts` already covers how a cancelled coroutine unwinds | Bring required context local |
| `lesson_disposable_effect` | SENIOR / paragraph 3 | Unit 10's `rememberUpdatedState` | Remove |
| `lesson_side_effect_publication` | PRACTICAL / paragraph 1 | Unit 9 established that Compose may… discard | Remove |
| `lesson_side_effect_publication` | SENIOR / paragraph 3 | which Unit 10 already made the deciding one | Remove |
| `lesson_produce_state` | CORE / paragraph 1 | Unit 8 answered one half of the observation problem | Remove |
| `lesson_produce_state` | CORE / paragraph 3 | all four come from Unit 9's effect model | Bring required context local |
| `lesson_produce_state` | SENIOR / paragraph 4 | the purpose-built conversion is Unit 8's | Bring required context local |
| `lesson_produce_state` | SENIOR / paragraph 4 | which is the next Lesson's subject | Rewrite using human title |
| `lesson_flow_adapter_or_compose_producer` | CORE / paragraph 1 | The previous Lesson's producer suspended | Remove |
| `lesson_flow_adapter_or_compose_producer` | PRACTICAL / paragraph 1 | the first Lesson's `DisposableEffect` | Remove |
| `lesson_flow_adapter_or_compose_producer` | PRACTICAL / paragraph 3 | Unit 10's trigger distinction still applies | Remove |
| `lesson_flow_adapter_or_compose_producer` | SENIOR / paragraph 2 | `lesson_flow_builders_and_callback_adapters` already covers how to build that adapter | Rewrite using human title |
| `lesson_flow_adapter_or_compose_producer` | SENIOR / paragraph 4 | Unit 8's collection is what does that | Bring required context local |
| `lesson_flow_adapter_or_compose_producer` | SENIOR / paragraph 4 | the subject of a later unit in this path | Rewrite using human title |

</details>

<details>
<summary>12. Production UI Effects and Mechanism Selection — 20 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | Every mechanism in this path has now been met on its own | Remove |
| `lesson_choosing_a_compose_mechanism` | CORE / paragraph 1 | Five units have introduced these mechanisms | Remove |
| `lesson_choosing_a_compose_mechanism` | PRACTICAL / paragraph 7 | a mechanism an earlier unit owns | Remove |
| `lesson_choosing_a_compose_mechanism` | SENIOR / paragraph 3 | belong to the architecture curriculum | Remove |
| `lesson_transient_ui_effects` | CORE / paragraph 2 | the previous lesson's decision starts with rendering | Remove |
| `lesson_transient_ui_effects` | PRACTICAL / paragraph 3 | as the effect unit described | Remove |
| `lesson_transient_ui_effects` | PRACTICAL / paragraph 4 | the one the trigger lesson asked about a similar shape | Rewrite using human title |
| `lesson_transient_ui_effects` | PRACTICAL / paragraph 6 | the navigation curriculum's subject | Remove |
| `lesson_transient_ui_effects` | SENIOR / paragraph 2 | That question is the next lesson | Rewrite using human title |
| `lesson_transient_ui_effects` | SENIOR / paragraph 3 | the architecture curriculum owns it | Remove |
| `lesson_transient_effect_delivery` | CORE / paragraph 1 | The previous lesson ended with an occurrence… | Remove |
| `lesson_transient_effect_delivery` | PRACTICAL / paragraph 1 | The coroutines and Flow path already settled the emitter's half | Remove |
| `lesson_transient_effect_delivery` | PRACTICAL / paragraph 2 | The stream lesson measured exactly that… this lesson cites it rather than re-deriving it | Bring required context local |
| `lesson_transient_effect_delivery` | PRACTICAL / comparison 1 | the shape the stream curriculum calls current-value semantics | Bring required context local |
| `lesson_transient_effect_delivery` | PRACTICAL / paragraph 5 | the previous lesson's mechanisms execute | Remove |
| `lesson_transient_effect_delivery` | PRACTICAL / paragraph 6 | the stream curriculum already establishes what current-value semantics provide | Rewrite using human title |
| `lesson_transient_effect_delivery` | PRACTICAL / paragraph 7 | the architecture curriculum's work and not this unit's | Remove |
| `lesson_transient_effect_delivery` | SENIOR / paragraph 2 | The dimensions that actually decide were established by the stream curriculum | Remove |
| `lesson_transient_effect_delivery` | SENIOR / paragraph 3 | this lesson exists beside the stream material rather than repeating it | Remove |
| `lesson_transient_effect_delivery` | SENIOR / paragraph 4 | the architecture curriculum answers them | Remove |

</details>

<details>
<summary>13. Coroutine Fundamentals and Structured Concurrency — 28 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | deliberately later units — all three assume this material | Remove |
| `lesson_suspension_and_blocking` | PRACTICAL / paragraph 2 | The third question has a unit of its own and is deliberately left there | Remove |
| `lesson_suspension_and_blocking` | PRACTICAL / paragraph 2 | Choosing dispatchers… is the next unit's subject | Rewrite using human title |
| `lesson_suspension_and_blocking` | SENIOR / paragraph 2 | removes the ground the next unit stands on | Remove |
| `lesson_suspension_and_blocking` | SENIOR / paragraph 2 | which is exactly why the dispatchers unit separates the pools | Remove |
| `lesson_suspension_and_blocking` | SENIOR / paragraph 4 | which the cancellation unit later builds on | Remove |
| `lesson_suspension_and_blocking` | SENIOR / paragraph 4 | the third unit's subject rather than this one's | Rewrite using human title |
| `lesson_coroutine_builders` | CORE / paragraph 3 | a `Job` handle, which the next lesson is about | Rewrite using human title |
| `lesson_coroutine_builders` | CODE / code 1 comment | the subject two lessons from here | Remove |
| `lesson_coroutine_builders` | PRACTICAL / paragraph 3 | a second half to this that the failure unit owns | Bring required context local |
| `lesson_coroutine_builders` | SENIOR / paragraph 2 | telling it apart from the scope builders is the next unit's job | Rewrite using human title |
| `lesson_job_and_parent_child` | CORE / paragraph 3 | the mechanism underneath everything the last lesson of this unit describes | Remove |
| `lesson_job_and_parent_child` | CORE / paragraph 3 | the context unit's to take apart | Rewrite using human title |
| `lesson_job_and_parent_child` | CORE / paragraph 5 | the next unit's first lesson | Rewrite using human title |
| `lesson_job_and_parent_child` | PRACTICAL / paragraph 4 | where such a failure travels next is the failure unit's subject | Rewrite using human title |
| `lesson_job_and_parent_child` | PRACTICAL / paragraph 5 | the third unit's entire subject | Rewrite using human title |
| `lesson_job_and_parent_child` | SENIOR / paragraph 3 | appears in the cancellation unit for a different purpose | Remove |
| `lesson_coroutine_scope_ownership` | CORE / paragraph 2 | the `Job` from the previous lesson | Remove |
| `lesson_coroutine_scope_ownership` | PRACTICAL / paragraph 2 | the failure unit teaches it properly | Rewrite using human title |
| `lesson_coroutine_scope_ownership` | PRACTICAL / paragraph 6 | the architecture curriculum's subject, not this one's | Remove |
| `lesson_coroutine_scope_ownership` | SENIOR / paragraph 4 | the failure unit's subject | Remove |
| `lesson_structured_concurrency` | CORE / paragraph 1 | Each earlier lesson contributed a piece | Remove |
| `lesson_structured_concurrency` | CORE / bullet list 1 | the completing state from the previous lesson | Remove |
| `lesson_structured_concurrency` | CORE / paragraph 3 | the reason the previous lesson spent so long on ownership | Remove |
| `lesson_structured_concurrency` | CORE / paragraph 3 | that is the context unit's subject | Rewrite using human title |
| `lesson_structured_concurrency` | PRACTICAL / paragraph 5 | a separate decision the next unit makes | Rewrite using human title |
| `lesson_structured_concurrency` | SENIOR / paragraph 3 | It is the failure unit's subject | Remove |
| `lesson_structured_concurrency` | SENIOR / paragraph 3 | telling those two apart is the next unit's job | Bring required context local |

</details>

<details>
<summary>14. Coroutine Context, Dispatchers and Concurrent Work — 18 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | Unit 1 supplies the lifetime model | Remove |
| `lesson_coroutine_context` | CORE / paragraph 1 | Unit 1 left two facts side by side | Remove |
| `lesson_coroutine_context` | CORE / paragraph 2 | what it handles is a Unit 3 question | Rewrite using human title |
| `lesson_coroutine_context` | PRACTICAL / paragraph 4 | The lifecycle consequence follows Unit 1 | Remove |
| `lesson_coroutine_context` | PRACTICAL / paragraph 4 | The next two lessons make that execution decision | Remove |
| `lesson_coroutine_context` | SENIOR / paragraph 3 | the deeper explanation of Unit 1's caveat | Remove |
| `lesson_coroutine_context` | SENIOR / paragraph 3 | Unit 3 explains what supervision actually changes | Rewrite using human title |
| `lesson_coroutine_context` | SENIOR / bullet list 1 | Use Unit 3 for cancellation, failure handling and supervision rules | Rewrite using human title |
| `lesson_dispatchers` | PRACTICAL / paragraph 1 | the next lesson explains placement in detail | Rewrite using human title |
| `lesson_dispatchers` | CORE / paragraph 4 | The performance and Android-platform curricula own the detailed diagnosis | Remove |
| `lesson_dispatchers` | SENIOR / paragraph 4 | the testing curriculum owns how to do that | Remove |
| `lesson_with_context_and_main_safety` | CORE / paragraph 4 | Unit 1 introduced coroutineScope | Remove |
| `lesson_with_context_and_main_safety` | SENIOR / paragraph 1 | as the previous lesson established | Remove |
| `lesson_with_context_and_main_safety` | SENIOR / paragraph 4 | a connection from the Compose lesson Keeping Work Out of Composition | Keep |
| `lesson_with_context_and_main_safety` | SENIOR / paragraph 4 | Those are separate lessons | Remove |
| `lesson_sequential_and_concurrent_work` | CORE / paragraph 5 | Unit 1 introduced async as the builder for a future result | Remove |
| `lesson_sequential_and_concurrent_work` | PRACTICAL / paragraph 7 | the execution decisions from the previous lessons | Remove |
| `lesson_sequential_and_concurrent_work` | SENIOR / paragraph 3 | Unit 3 owns the exact propagation and supervision rules | Rewrite using human title |

</details>

<details>
<summary>15. Cancellation, Failure and Coordination — 12 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | Units 1 and 2 built structured work and decided where it runs | Remove |
| `lesson_cooperative_cancellation` | CORE / paragraph 1 | Unit 1 introduced cancellation as something the hierarchy carries and stopped there | Remove |
| `lesson_cooperative_cancellation` | SENIOR / paragraph 4 | the subject of the third Lesson in this Unit | Rewrite using human title |
| `lesson_cancellation_cleanup_and_timeouts` | SENIOR / paragraph 3 | Unit 1's ownership argument arriving from a different direction | Remove |
| `lesson_exception_propagation` | CORE / paragraph 2 | the ordinary rules follow from Unit 1's hierarchy | Remove |
| `lesson_exception_propagation` | SENIOR / paragraph 2 | which is the subject of the next Lesson | Rewrite using human title |
| `lesson_exception_propagation` | SENIOR / paragraph 4 | the same asymmetry as the first Lesson in this Unit | Remove |
| `lesson_exception_propagation` | SENIOR / paragraph 5 | the architecture curriculum owns that argument | Rewrite using human title |
| `lesson_supervision_and_failure_isolation` | CORE / paragraph 1 | The previous Lesson established the ordinary rule | Remove |
| `lesson_supervision_and_failure_isolation` | PRACTICAL / paragraph 2 | in the vocabulary of the previous Lesson, it has no propagation path | Bring required context local |
| `lesson_supervision_and_failure_isolation` | SENIOR / paragraph 1 | Unit 2 taught the context half of the launch(SupervisorJob()) trap | Rewrite using human title |
| `lesson_shared_state_and_coordination` | SENIOR / paragraph 3 | belongs to the hot and cold streams material later in this curriculum | Rewrite using human title |

</details>

<details>
<summary>16. Flow Fundamentals — 28 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The first three units built a coroutine… | Remove |
| Unit summary | Unit summary | cancelled by the model the cancellation unit already taught | Remove |
| `lesson_why_flow` | CORE / paragraph 1 | Everything the first three units built | Remove |
| `lesson_why_flow` | CORE / paragraph 3 | the next Lesson's subject | Rewrite using human title |
| `lesson_why_flow` | SENIOR / paragraph 1 | the subject of the hot streams unit later in this curriculum | Rewrite using human title |
| `lesson_why_flow` | SENIOR / paragraph 2 | the ownership argument the scope Lesson made about ad-hoc scopes | Remove |
| `lesson_why_flow` | SENIOR / paragraph 3 | the architecture curriculum owns it… somebody else's Lesson | Remove |
| `lesson_cold_flows` | CORE / paragraph 1 | The previous Lesson called a Flow a description | Remove |
| `lesson_cold_flows` | CORE / paragraph 5 | the rest of the curriculum uses them precisely | Remove |
| `lesson_cold_flows` | PRACTICAL / paragraph 5 | is the flow composition unit later in this curriculum | Rewrite using human title |
| `lesson_cold_flows` | SENIOR / paragraph 3 | the hot streams unit later in this curriculum exists to correct | Rewrite using human title |
| `lesson_cold_flows` | SENIOR / paragraph 3 | the Compose bridge into flows linked below | Remove |
| `lesson_flow_collection_lifetime` | Lesson summary | the ordinary cooperative cancellation Unit 3 already taught | Remove |
| `lesson_flow_collection_lifetime` | CORE / paragraph 1 | this Lesson comes fourth in the curriculum… the first three units | Remove |
| `lesson_flow_collection_lifetime` | PRACTICAL / paragraph 2 | the same handle the Job Lesson introduced | Remove |
| `lesson_flow_collection_lifetime` | PRACTICAL / paragraph 4 | the effects and lifecycle integration material later in this curriculum | Rewrite using human title |
| `lesson_flow_collection_lifetime` | SENIOR / paragraph 1 | Unit 3's model is unchanged here | Remove |
| `lesson_flow_collection_lifetime` | SENIOR / paragraph 3 | the one Unit 3 already gave | Remove |
| `lesson_flow_context_and_flow_on` | CORE / paragraph 1 | The previous Lesson settled… Unit 2's context model applies unchanged | Remove |
| `lesson_flow_context_and_flow_on` | CORE / paragraph 3 | the same misconception Unit 1 removed | Remove |
| `lesson_flow_context_and_flow_on` | SENIOR / paragraph 2 | the buffering material in the flow composition unit later in this curriculum | Rewrite using human title |
| `lesson_flow_context_and_flow_on` | SENIOR / paragraph 3 | Unit 2's precision about context and threads | Remove |
| `lesson_flow_context_and_flow_on` | SENIOR / paragraph 4 | the failure material in the flow composition unit later in this curriculum | Rewrite using human title |
| `lesson_flow_builders_and_callback_adapters` | CORE / paragraph 2 | the rule the previous Lesson established | Remove |
| `lesson_flow_builders_and_callback_adapters` | PRACTICAL / paragraph 5 | the shape the cleanup Lesson taught | Remove |
| `lesson_flow_builders_and_callback_adapters` | SENIOR / paragraph 1 | the subject of the hot streams unit later in this curriculum | Rewrite using human title |
| `lesson_flow_builders_and_callback_adapters` | SENIOR / paragraph 3 | a separate subject this unit deliberately leaves alone | Remove |
| `lesson_flow_builders_and_callback_adapters` | SENIOR / paragraph 4 | the buffering material in the flow composition unit later in this curriculum | Rewrite using human title |

</details>

<details>
<summary>17. Flow Composition, Timing and Failure — 12 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous unit settled what a Flow is | Remove |
| `lesson_transforming_and_filtering_flows` | CORE / paragraph 1 | The previous unit established what an operator is | Remove |
| `lesson_combining_flows` | PRACTICAL / paragraph 6 | unlike the flattening operators the next Lesson covers | Rewrite using human title |
| `lesson_combining_flows` | SENIOR / paragraph 4 | the subject of the hot streams and state unit later in this curriculum | Rewrite using human title |
| `lesson_flattening_flows` | PRACTICAL / paragraph 4 | the coroutine kind the second unit established | Remove |
| `lesson_flattening_flows` | SENIOR / paragraph 4 | the cooperative-cancellation model the cancellation Lesson established | Remove |
| `lesson_flow_buffering_and_conflation` | CORE / paragraph 5 | the previous unit already supplied it | Remove |
| `lesson_flow_buffering_and_conflation` | PRACTICAL / paragraph 6 | exactly as the cancellation unit described | Remove |
| `lesson_flow_buffering_and_conflation` | SENIOR / paragraph 4 | the state-holder types the hot streams unit introduces | Bring required context local |
| `lesson_flow_failure_and_completion` | PRACTICAL / paragraph 4 | the architecture curriculum owns it | Remove |
| `lesson_flow_failure_and_completion` | SENIOR / paragraph 5 | the coroutine model the cancellation Lesson established | Remove |
| `lesson_flow_failure_and_completion` | SENIOR / paragraph 6 | the argument the previous unit left half-finished | Rewrite using human title |

</details>

<details>
<summary>18. StateFlow, SharedFlow and Hot Streams — 25 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous two units settled cold Flow | Remove |
| `lesson_hot_and_cold_streams` | CORE / paragraph 1 | Everything the previous two units taught was cold | Remove |
| `lesson_hot_and_cold_streams` | CORE / paragraph 3 | the fourth lesson of this unit is where they do | Rewrite using human title |
| `lesson_hot_and_cold_streams` | CORE / paragraph 6 | which are the next two lessons… the sharing lesson after those | Remove |
| `lesson_hot_and_cold_streams` | CORE / comparison 1 | This lesson / The StateFlow and SharedFlow lessons / The sharing lesson | Remove |
| `lesson_hot_and_cold_streams` | PRACTICAL / paragraph 5 | the sharing lesson shows a shared stream whose upstream stops | Rewrite using human title |
| `lesson_hot_and_cold_streams` | SENIOR / paragraph 1 | the Compose curriculum already made it. The snapshot lesson observes | Rewrite using human title |
| `lesson_hot_and_cold_streams` | SENIOR / paragraph 3 | The last lesson in this unit turns that into a decision procedure | Rewrite using human title |
| `lesson_state_flow` | CORE / paragraph 3 | an architecture question this unit deliberately leaves alone | Remove |
| `lesson_state_flow` | PRACTICAL / paragraph 5 | the buffering lesson already established the model | Remove |
| `lesson_state_flow` | SENIOR / paragraph 1 | the shared-state problem from the cancellation and coordination unit | Rewrite using human title |
| `lesson_state_flow` | SENIOR / paragraph 4 | the connection the next lesson picks up | Remove |
| `lesson_state_flow` | SENIOR / paragraph 5 | questions later material answers | Remove |
| `lesson_shared_flow` | SENIOR / paragraph 3 | the final lesson of this unit says what does | Rewrite using human title |
| `lesson_shared_flow` | SENIOR / paragraph 4 | the trade the buffering lesson framed for cold flows | Remove |
| `lesson_shared_flow` | SENIOR / paragraph 5 | the sharing policies in the next lesson | Rewrite using human title |
| `lesson_sharing_cold_flows` | CORE / paragraph 3 | the current-value contract from the state lesson | Remove |
| `lesson_sharing_cold_flows` | CORE / paragraph 3 | the broadcast contract from the previous lesson | Remove |
| `lesson_sharing_cold_flows` | PRACTICAL / paragraph 1 | the scope-ownership lesson answers it | Remove |
| `lesson_sharing_cold_flows` | SENIOR / paragraph 5 | the same operator-fusion rule from the state lesson | Remove |
| `lesson_choosing_a_stream_abstraction` | CORE / paragraph 3 | the sharing lesson says how to get there | Rewrite using human title |
| `lesson_choosing_a_stream_abstraction` | CORE / paragraph 5 | the ones the previous lesson listed | Remove |
| `lesson_choosing_a_stream_abstraction` | SENIOR / callout 1 | as the previous lesson measured | Remove |
| `lesson_choosing_a_stream_abstraction` | SENIOR / paragraph 3 | belongs to the effects and architecture material later in this path | Rewrite using human title |
| `lesson_choosing_a_stream_abstraction` | SENIOR / paragraph 4 | What this unit's five lessons contribute… from the earlier units | Remove |

</details>

<details>
<summary>19. Architecture as Responsibilities and Boundaries — 19 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | the test every later architecture unit applies | Remove |
| `lesson_what_architecture_decides` | PRACTICAL / paragraph 2 | the modularization curriculum's subject… the last lesson in this unit | Remove |
| `lesson_what_architecture_decides` | SENIOR / paragraph 4 | which is where the next lesson starts | Remove |
| `lesson_responsibility_and_change` | CORE / paragraph 1 | The previous lesson ended on a question it could not answer | Remove |
| `lesson_responsibility_and_change` | PRACTICAL / paragraph 8 | the Compose curriculum already asked it… `lesson_state_hoisting` decided… | Rewrite using human title |
| `lesson_responsibility_and_change` | SENIOR / paragraph 4 | the subject of the next two lessons… the later unit on domain logic and dependency direction | Rewrite using human title |
| `lesson_dependency_direction_and_boundaries` | CORE / paragraph 5 | the later unit on domain logic and dependency direction takes it seriously | Rewrite using human title |
| `lesson_dependency_direction_and_boundaries` | PRACTICAL / paragraph 3 | whether a given feature should pay for it is this unit's last lesson | Remove |
| `lesson_dependency_direction_and_boundaries` | PRACTICAL / paragraph 9 | this curriculum answers it in the unit on domain logic and dependency direction | Remove |
| `lesson_dependency_direction_and_boundaries` | SENIOR / paragraph 3 | the judgement the next two lessons sharpen | Remove |
| `lesson_dependency_direction_and_boundaries` | SENIOR / paragraph 4 | the build and modularization curriculum's subject | Remove |
| `lesson_when_an_interface_is_a_boundary` | PRACTICAL / paragraph 6 | belong to the testing curriculum | Remove |
| `lesson_when_an_interface_is_a_boundary` | SENIOR / paragraph 4 | the subject of the unit on domain logic and dependency direction | Rewrite using human title |
| `lesson_layers_and_their_cost` | CORE / paragraph 1 | the four previous lessons were the question | Remove |
| `lesson_layers_and_their_cost` | SENIOR / paragraph 2 | The previous lesson's example is a direct case | Remove |
| `lesson_layers_and_their_cost` | SENIOR / paragraph 2 | the build and modularization curriculum's subject, and this unit stops here | Remove |
| `lesson_layers_and_their_cost` | SENIOR / paragraph 3 | this curriculum takes up properly in the unit on domain logic and dependency direction | Rewrite using human title |
| `lesson_layers_and_their_cost` | SENIOR / paragraph 4 | the unit on data ownership owns it | Rewrite using human title |
| `lesson_layers_and_their_cost` | SENIOR / paragraph 5 | That starts next: the screen-level owner the Compose curriculum stopped at… | Remove |

</details>

<details>
<summary>20. Screen State Holders, ViewModel and UI State — 23 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The Compose curriculum takes a screen as far as an owner… This unit designs it | Remove |
| `lesson_state_holder_responsibility` | CORE / paragraph 1 | The borrowed-items feature from the previous unit… The Compose curriculum has already taken this as far as it goes | Remove |
| `lesson_state_holder_responsibility` | CORE / paragraph 2 | it starts where the previous unit ended… Ask the question that unit built | Remove |
| `lesson_state_holder_responsibility` | PRACTICAL / paragraph 1 | the reader, writer and lifetime test the Compose curriculum already taught | Remove |
| `lesson_state_holder_responsibility` | PRACTICAL / paragraph 5 | the dependency-injection curriculum owns it | Rewrite using human title |
| `lesson_state_holder_responsibility` | SENIOR / paragraph 3 | is the closing unit of this curriculum, and the next four lessons do the groundwork | Rewrite using human title |
| `lesson_state_holder_responsibility` | SENIOR / paragraph 4 | they are the subjects of the two units after this one | Rewrite using human title |
| `lesson_viewmodel_lifetime_and_persistence` | CORE / paragraph 4 | which is where the Compose curriculum left the argument | Remove |
| `lesson_viewmodel_lifetime_and_persistence` | PRACTICAL / paragraph 1 | The Compose curriculum already asked, from the Composition's edge | Remove |
| `lesson_viewmodel_lifetime_and_persistence` | SENIOR / paragraph 4 | belong to the lifecycle-and-navigation curriculum and to the persistence curriculum | Remove |
| `lesson_modelling_ui_state` | CORE / paragraph 1 | the Compose curriculum established the value-based contract | Remove |
| `lesson_modelling_ui_state` | PRACTICAL / paragraph 4 | the one the next section is built on | Remove |
| `lesson_modelling_ui_state` | SENIOR / paragraph 6 | the repositories unit owns it | Rewrite using human title |
| `lesson_modelling_ui_state` | SENIOR / paragraph 6 | is already taught in the Compose curriculum | Rewrite using human title |
| `lesson_state_out_intentions_in` | CORE / paragraph 3 | The Compose curriculum already taught this shape inside the composable tree | Remove |
| `lesson_state_out_intentions_in` | PRACTICAL / paragraph 4 | the coroutines curriculum owns what those stream types are | Remove |
| `lesson_state_out_intentions_in` | SENIOR / paragraph 3 | exactly as the first lesson in this unit said about the help panel | Remove |
| `lesson_state_out_intentions_in` | SENIOR / paragraph 4 | the unit of this curriculum that compares responsibility models | Rewrite using human title |
| `lesson_owner_scoped_work` | CORE / paragraph 2 | That much the coroutines curriculum already settled | Remove |
| `lesson_owner_scoped_work` | PRACTICAL / paragraph 4 | the closing unit's decision… which has its own curriculum | Rewrite using human title |
| `lesson_owner_scoped_work` | SENIOR / paragraph 3 | The shared state holders from the first lesson of this unit | Remove |
| `lesson_owner_scoped_work` | SENIOR / paragraph 4 | the closing unit's subject | Rewrite using human title |
| `lesson_owner_scoped_work` | SENIOR / paragraph 5 | taught in the coroutines curriculum… the lifecycle-and-navigation curriculum's… the background-work curriculum | Remove |

</details>

<details>
<summary>21. Repositories, Data Ownership and Single Source of Truth — 24 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous unit asked who owns the state a screen renders | Remove |
| `lesson_what_a_repository_owns` | CORE / paragraph 1 | The borrowed-items feature the previous two units worked through | Remove |
| `lesson_what_a_repository_owns` | CORE / paragraph 1 | the previous unit's test applies here unchanged | Remove |
| `lesson_what_a_repository_owns` | CORE / paragraph 2 | the question the foundations unit taught | Bring required context local |
| `lesson_what_a_repository_owns` | CORE / paragraph 4 | the persistence and networking curricula own their APIs | Remove |
| `lesson_what_a_repository_owns` | PRACTICAL / paragraph 4 | Ask the foundations unit's question of it | Remove |
| `lesson_what_a_repository_owns` | PRACTICAL / paragraph 8 | the foundations unit already settled what to do with it | Rewrite using human title |
| `lesson_what_a_repository_owns` | PRACTICAL / paragraph 9 | the unit after this one is where it belongs | Rewrite using human title |
| `lesson_what_a_repository_owns` | SENIOR / paragraph 4 | The coordination the next lesson works through… that lesson says so | Remove |
| `lesson_coordinating_sources` | PRACTICAL / paragraph 7 | the background-work curriculum owns it | Remove |
| `lesson_coordinating_sources` | PRACTICAL / paragraph 10 | the subject of the last lesson in this unit | Rewrite using human title |
| `lesson_coordinating_sources` | SENIOR / paragraph 3 | this unit's boundary with the persistence curriculum | Remove |
| `lesson_coordinating_sources` | SENIOR / paragraph 4 | which the previous lesson already said is a legitimate place to be | Remove |
| `lesson_single_source_of_truth` | PRACTICAL / paragraph 2 | the lifetime question the previous unit separated out | Remove |
| `lesson_single_source_of_truth` | PRACTICAL / paragraph 7 | the same argument the previous unit made about a screen's state | Remove |
| `lesson_observable_or_one_shot_api` | PRACTICAL / paragraph 7 | the coroutines and Flow curriculum's, and the lessons linked from this one | Rewrite using human title |
| `lesson_observable_or_one_shot_api` | PRACTICAL / paragraph 9 | the failure the previous unit named at the screen boundary | Remove |
| `lesson_observable_or_one_shot_api` | SENIOR / paragraph 3 | the previous lesson's definition of authority | Remove |
| `lesson_model_and_error_boundaries` | CORE / paragraph 2 | the one the foundations unit already taught | Remove |
| `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 1 | the hypothetical library service from earlier in this unit | Remove |
| `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 6 | the reminder-time preference from the first lesson of this unit | Bring required context local |
| `lesson_model_and_error_boundaries` | PRACTICAL / bullet list 1 | the refresh state from the coordination lesson | Rewrite using human title |
| `lesson_model_and_error_boundaries` | SENIOR / paragraph 3 | There is a boundary with the previous unit here… That unit asked | Rewrite using human title |
| `lesson_model_and_error_boundaries` | SENIOR / paragraph 4 | the language curriculum owns how they work… the persistence and networking curricula | Remove |

</details>

<details>
<summary>22. Domain Logic, Use Cases and Dependency Direction — 32 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous unit gave this feature a repository and a data boundary | Remove |
| `lesson_when_a_domain_layer_earns_its_place` | CORE / paragraph 1 | The previous unit left the borrowed-items feature with a repository | Remove |
| `lesson_when_a_domain_layer_earns_its_place` | CORE / paragraph 3 | the foundations unit's test applies here unchanged | Remove |
| `lesson_when_a_domain_layer_earns_its_place` | CORE / paragraph 4 | which the last lesson of this unit takes up properly | Rewrite using human title |
| `lesson_when_a_domain_layer_earns_its_place` | PRACTICAL / paragraph 1 | The reminder-time preference from the data-ownership unit | Bring required context local |
| `lesson_when_a_domain_layer_earns_its_place` | PRACTICAL / paragraph 2 | exactly as the mapping layer in the foundations unit did | Rewrite using human title |
| `lesson_when_a_domain_layer_earns_its_place` | PRACTICAL / paragraph 9 | the foundations unit's cost model | Remove |
| `lesson_when_a_domain_layer_earns_its_place` | SENIOR / paragraph 2 | belongs to the build and modularization curriculum | Remove |
| `lesson_when_a_domain_layer_earns_its_place` | SENIOR / paragraph 3 | The next lesson reads those types closely | Remove |
| `lesson_when_a_domain_layer_earns_its_place` | SENIOR / paragraph 4 | the next asks whether an individual operation earns its own use-case type | Rewrite using human title |
| `lesson_use_cases_and_pass_through_cost` | CORE / paragraph 1 | The previous lesson decided whether a feature earns another layer | Remove |
| `lesson_use_cases_and_pass_through_cost` | CORE / paragraph 5 | the one this unit has used throughout | Remove |
| `lesson_use_cases_and_pass_through_cost` | PRACTICAL / paragraph 3 | the operation the previous lesson used to earn the layer | Remove |
| `lesson_use_cases_and_pass_through_cost` | SENIOR / paragraph 4 | the screen-state unit already drew it | Rewrite using human title |
| `lesson_use_cases_and_pass_through_cost` | SENIOR / paragraph 4 | the failure the next lesson opens with | Remove |
| `lesson_policy_and_framework_detail` | CORE / paragraph 1 | The previous two lessons decided where an operation belongs | Remove |
| `lesson_policy_and_framework_detail` | CORE / paragraph 3 | the same instrument the foundations unit used | Remove |
| `lesson_policy_and_framework_detail` | PRACTICAL / paragraph 4 | The data-ownership unit already priced this move | Rewrite using human title |
| `lesson_policy_and_framework_detail` | SENIOR / paragraph 1 | the testing curriculum's subject | Remove |
| `lesson_policy_and_framework_detail` | SENIOR / paragraph 2 | belong entirely to the Kotlin Multiplatform curriculum | Remove |
| `lesson_policy_and_framework_detail` | SENIOR / paragraph 4 | That is the next lesson, and it is the one the foundations unit promised | Rewrite using human title |
| `lesson_dependency_inversion_in_practice` | CORE / paragraph 1 | Two earlier lessons stopped deliberately… the foundations unit… the lesson on interfaces… the data-ownership unit deferred | Remove |
| `lesson_dependency_inversion_in_practice` | CORE / paragraph 2 | the one the earlier lesson already established | Bring required context local |
| `lesson_dependency_inversion_in_practice` | SENIOR / paragraph 3 | belong entirely to the dependency-injection curriculum | Rewrite using human title |
| `lesson_dependency_inversion_in_practice` | SENIOR / paragraph 5 | the one the foundations unit answered. That lesson asked… | Rewrite using human title |
| `lesson_clean_architecture_intent` | CORE / paragraph 1 | This unit has now made four decisions | Remove |
| `lesson_clean_architecture_intent` | CORE / bullet list 1 | the same policy-and-detail distinction the previous lesson drew | Remove |
| `lesson_clean_architecture_intent` | CORE / paragraph 4 | the previous lessons in this unit are where that decision is actually made | Rewrite using human title |
| `lesson_clean_architecture_intent` | SENIOR / paragraph 2 | the foundations unit's cost model | Remove |
| `lesson_clean_architecture_intent` | SENIOR / paragraph 3 | the same precision… that the policy lesson insisted on | Remove |
| `lesson_clean_architecture_intent` | SENIOR / paragraph 4 | the build and modularization curriculum | Remove |
| `lesson_clean_architecture_intent` | SENIOR / paragraph 5 | reading a real codebase against all three is the next unit's subject | Rewrite using human title |

</details>

<details>
<summary>23. MVP, MVVM and MVI Responsibility Models — 37 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous four units taught every decision these pattern names are about | Remove |
| `lesson_one_screen_five_questions` | Lesson summary | Four units taught how to describe… | Remove |
| `lesson_one_screen_five_questions` | CORE / paragraph 1 | Four units have been building one instrument… the next four lessons… the previous units already gave you | Remove |
| `lesson_one_screen_five_questions` | CORE / bullet list 1 | ownership in the sense the foundations unit fixed | Bring required context local |
| `lesson_one_screen_five_questions` | PRACTICAL / paragraph 1 | One hypothetical screen runs through this lesson and the three that follow | Keep |
| `lesson_one_screen_five_questions` | PRACTICAL / paragraph 3 | Those are the previous units' decisions | Remove |
| `lesson_one_screen_five_questions` | SENIOR / paragraph 3 | The state-holder unit worked all of that out | Rewrite using human title |
| `lesson_one_screen_five_questions` | SENIOR / paragraph 4 | The next three lessons take the same screen through three sets of answers | Remove |
| `lesson_mvp_view_contract` | Lesson summary | not as a strawman for the lesson that follows | Remove |
| `lesson_mvp_view_contract` | CORE / paragraph 1 | the practice-configuration screen from the previous lesson | Bring required context local |
| `lesson_mvp_view_contract` | CORE / paragraph 2 | the arrangement the last lesson described | Remove |
| `lesson_mvp_view_contract` | PRACTICAL / paragraph 4 | the problem the state-holder unit treats at length | Rewrite using human title |
| `lesson_mvp_view_contract` | PRACTICAL / paragraph 5 | belong to the lifecycle curriculum | Remove |
| `lesson_mvp_view_contract` | PRACTICAL / bullet list 1 | a boundary in the foundations unit's exact sense | Bring required context local |
| `lesson_mvp_view_contract` | SENIOR / paragraph 3 | it matters for how the next lesson is read | Remove |
| `lesson_mvvm_observed_state` | Lesson summary | The state-holder unit built this arrangement without naming it | Remove |
| `lesson_mvvm_observed_state` | CORE / paragraph 2 | the difference from the previous lesson | Remove |
| `lesson_mvvm_observed_state` | CORE / paragraph 3 | The state-holder unit worked all of that out | Rewrite using human title |
| `lesson_mvvm_observed_state` | PRACTICAL / paragraph 1 | The state-holder unit designed an owner… they have already built it | Remove |
| `lesson_mvvm_observed_state` | PRACTICAL / paragraph 2 | The coroutines curriculum owns how `StateFlow` behaves | Remove |
| `lesson_mvvm_observed_state` | PRACTICAL / paragraph 4 | the architecture-versus-structure argument from the foundations unit | Rewrite using human title |
| `lesson_mvvm_observed_state` | PRACTICAL / paragraph 5 | the property the foundations unit established | Remove |
| `lesson_mvvm_observed_state` | PRACTICAL / paragraph 6 | a full comparison in the classification lesson… the state-holder unit weighed them | Rewrite using human title |
| `lesson_mvvm_observed_state` | SENIOR / paragraph 1 | a design question the next two lessons take up | Remove |
| `lesson_mvvm_observed_state` | SENIOR / paragraph 3 | which the state-holder unit worked through one situation at a time | Rewrite using human title |
| `lesson_mvi_intent_and_reduction` | CORE / paragraph 1 | the same answers as the previous lesson | Remove |
| `lesson_mvi_intent_and_reduction` | PRACTICAL / paragraph 2 | the same invariant argument the state-holder unit made | Remove |
| `lesson_mvi_intent_and_reduction` | PRACTICAL / paragraph 6 | are the subject of the next unit | Rewrite using human title |
| `lesson_mvi_intent_and_reduction` | SENIOR / paragraph 1 | The classification lesson puts designs in front of you | Rewrite using human title |
| `lesson_mvi_intent_and_reduction` | SENIOR / paragraph 2 | which the Compose curriculum owns | Rewrite using human title |
| `lesson_mvi_intent_and_reduction` | SENIOR / paragraph 4 | as with every structural decision in the previous four units | Remove |
| `lesson_classifying_a_real_architecture` | Lesson summary | This lesson is the unit's payoff | Remove |
| `lesson_classifying_a_real_architecture` | CORE / paragraph 1 | The three previous lessons each showed… the previous four units spent their length | Remove |
| `lesson_classifying_a_real_architecture` | PRACTICAL / paragraph 6 | the Compose curriculum's subject | Rewrite using human title |
| `lesson_classifying_a_real_architecture` | PRACTICAL / paragraph 7 | the architecture is the one from two lessons ago | Remove |
| `lesson_classifying_a_real_architecture` | SENIOR / paragraph 3 | shown in the previous units to be an independent decision | Remove |
| `lesson_classifying_a_real_architecture` | SENIOR / paragraph 4 | the previous unit is the reason the question arises at all | Rewrite using human title |

</details>

<details>
<summary>24. State, Events, Lifetime and Architecture Selection — 30 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The five previous units each started from something that already existed… the Compose curriculum deliberately left… somebody else's curriculum | Remove |
| `lesson_state_or_occurrence` | CORE / paragraph 1 | The five previous units each began with something that already existed | Remove |
| `lesson_state_or_occurrence` | CORE / paragraph 2 | The Compose curriculum established that contrast at the screen boundary | Rewrite using human title |
| `lesson_state_or_occurrence` | CORE / paragraph 4 | that is the next-but-one lesson's subject | Rewrite using human title |
| `lesson_state_or_occurrence` | PRACTICAL / paragraph 2 | which the streams curriculum already characterises | Rewrite using human title |
| `lesson_state_or_occurrence` | PRACTICAL / paragraph 3 | the data-ownership unit makes, and the mechanism belongs to the persistence curriculum | Rewrite using human title |
| `lesson_state_or_occurrence` | PRACTICAL / paragraph 10 | which is the next lesson | Rewrite using human title |
| `lesson_delivery_guarantees` | Lesson summary | The Compose curriculum reached a negative result and stopped there | Remove |
| `lesson_delivery_guarantees` | CORE / paragraph 1 | The previous lesson sorted a fact… The Compose curriculum asked that… This lesson does that work | Remove |
| `lesson_delivery_guarantees` | PRACTICAL / paragraph 2 | the transient one the Compose curriculum already supplies | Rewrite using human title |
| `lesson_delivery_guarantees` | PRACTICAL / paragraph 2 | the precise sense the closing lesson of this unit defines | Rewrite using human title |
| `lesson_delivery_guarantees` | PRACTICAL / paragraph 3 | Two conclusions from the streams curriculum apply immediately and are not re-derived here | Bring required context local |
| `lesson_delivery_guarantees` | PRACTICAL / paragraph 6 | which is the next lesson's subject | Remove |
| `lesson_delivery_guarantees` | SENIOR / paragraph 3 | belongs to the persistence and lifecycle curricula | Remove |
| `lesson_delivery_guarantees` | SENIOR / paragraph 4 | none of them is this curriculum's | Remove |
| `lesson_delivery_guarantees` | SENIOR / paragraph 5 | the streams curriculum sets out which contract provides which | Rewrite using human title |
| `lesson_choosing_the_owner_by_lifetime` | CORE / paragraph 1 | The previous lesson ended on a question it deliberately did not answer | Remove |
| `lesson_choosing_the_owner_by_lifetime` | CORE / paragraph 4 | the same cost discipline the foundations unit applied | Remove |
| `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraph 3 | exactly as the state-holder unit designed it… that unit described | Rewrite using human title |
| `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraph 4 | the arrangement the data-ownership unit describes | Rewrite using human title |
| `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraph 8 | the background-work curriculum's subject | Remove |
| `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / comparison 1 | a mechanism whose curriculum is not this one | Remove |
| `lesson_choosing_the_owner_by_lifetime` | SENIOR / paragraph 2 | the state-holder unit works the full ladder of events through | Rewrite using human title |
| `lesson_choosing_the_owner_by_lifetime` | SENIOR / paragraph 3 | belong to the lifecycle and navigation curriculum | Remove |
| `lesson_smallest_sufficient_architecture` | CORE / paragraph 1 | Every previous unit produced a candidate | Remove |
| `lesson_smallest_sufficient_architecture` | PRACTICAL / paragraph 1 | the reminder-time setting the domain unit already used | Bring required context local |
| `lesson_smallest_sufficient_architecture` | PRACTICAL / paragraph 2 | the boundary the data-ownership unit designed | Remove |
| `lesson_smallest_sufficient_architecture` | PRACTICAL / paragraph 4 | the condition the domain unit stated… from the guarantee lesson… from the lifetime lesson | Remove |
| `lesson_smallest_sufficient_architecture` | SENIOR / paragraph 1 | what the pattern unit taught | Remove |
| `lesson_smallest_sufficient_architecture` | SENIOR / paragraph 5 | The foundations unit asked four questions… the whole curriculum has been assembling | Remove |

</details>

<details>
<summary>25. Dependency Injection as Object Construction — 29 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | From there the unit follows one small object graph as it grows | Remove |
| `lesson_who_constructs_this_object` | CORE / paragraph 5 | Everything that comes later in this curriculum | Remove |
| `lesson_who_constructs_this_object` | PRACTICAL / paragraph 5 | This shape has a name and its own lesson later in this unit | Rewrite using human title |
| `lesson_who_constructs_this_object` | PRACTICAL / paragraph 8 | later units in this curriculum examine what each of them actually does | Rewrite using human title |
| `lesson_a_dependency_should_be_visible` | CORE / paragraph 1 | The previous lesson settled that receiving a collaborator is dependency injection | Remove |
| `lesson_a_dependency_should_be_visible` | PRACTICAL / paragraph 4 | The architecture curriculum's lesson on when an interface is a boundary gives the test | Keep |
| `lesson_a_dependency_should_be_visible` | PRACTICAL / paragraph 4 | The next lesson shows what changes at a boundary where one does | Remove |
| `lesson_a_dependency_should_be_visible` | PRACTICAL / paragraph 6 | the unit on Hilt meets the one that matters on Android | Rewrite using human title |
| `lesson_a_dependency_should_be_visible` | PRACTICAL / bullet list 1 | the unit on graphs and lifetimes treats that distinction properly | Rewrite using human title |
| `lesson_a_dependency_should_be_visible` | SENIOR / paragraph 1 | belongs to the testing curriculum and is deliberately not taught here | Remove |
| `lesson_injected_inverted_or_both` | CORE / paragraph 3 | The architecture curriculum already taught the second one in full | Rewrite using human title |
| `lesson_injected_inverted_or_both` | PRACTICAL / callout 1 | The architecture curriculum already states the consequence exactly: "…" | Bring required context local |
| `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 4 | the test the architecture curriculum set | Remove |
| `lesson_injected_inverted_or_both` | PRACTICAL / comparison 2 | Version A of the first lesson | Bring required context local |
| `lesson_injected_inverted_or_both` | PRACTICAL / comparison 2 | Version B of the first lesson… every cost the fifth lesson prices | Bring required context local |
| `lesson_injected_inverted_or_both` | SENIOR / paragraph 1 | the one the architecture curriculum gave | Remove |
| `lesson_injected_inverted_or_both` | SENIOR / paragraph 2 | the next lesson's subject matters | Remove |
| `lesson_one_place_that_knows_how_to_build` | CORE / paragraph 1 | Every lesson so far has taken something away from a class | Remove |
| `lesson_one_place_that_knows_how_to_build` | PRACTICAL / paragraph 4 | the closing lesson's question | Rewrite using human title |
| `lesson_one_place_that_knows_how_to_build` | PRACTICAL / paragraph 6 | which is version A of the first lesson, reappearing one level up | Remove |
| `lesson_one_place_that_knows_how_to_build` | SENIOR / paragraph 3 | it is the next lesson's subject | Rewrite using human title |
| `lesson_one_place_that_knows_how_to_build` | SENIOR / paragraph 4 | which is what the unit on Koin examines | Rewrite using human title |
| `lesson_asking_for_it_or_being_given_it` | CORE / paragraph 1 | the same question this unit opened with | Remove |
| `lesson_asking_for_it_or_being_given_it` | PRACTICAL / paragraph 6 | the later units of this curriculum contradict this one: the unit on Hilt… the unit on Koin… | Rewrite using human title |
| `lesson_asking_for_it_or_being_given_it` | PRACTICAL / callout 1 | this curriculum compares their mechanisms | Remove |
| `lesson_asking_for_it_or_being_given_it` | SENIOR / paragraph 2 | the criteria the framework units later apply | Remove |
| `lesson_when_wiring_it_yourself_is_enough` | CORE / paragraph 4 | a requirement in its own right that the next unit teaches properly | Rewrite using human title |
| `lesson_when_wiring_it_yourself_is_enough` | SENIOR / paragraph 2 | the units on Dagger, Hilt and Koin… the closing unit of this curriculum | Rewrite using human title |
| `lesson_when_wiring_it_yourself_is_enough` | SENIOR / paragraph 3 | The architecture curriculum closes on a test | Keep |

</details>

<details>
<summary>26. Object Graphs, Lifetimes and Scopes — 33 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | Dagger, Hilt and Koin are later notations for exactly these decisions | Keep |
| `lesson_from_one_dependency_to_a_graph` | CORE / paragraph 1 | The previous unit ended with a controller | Remove |
| `lesson_from_one_dependency_to_a_graph` | CORE / paragraph 3 | the rest of this unit and the three framework units after it | Remove |
| `lesson_from_one_dependency_to_a_graph` | CORE / paragraph 5 | The previous unit defined a composition root | Remove |
| `lesson_from_one_dependency_to_a_graph` | CORE / paragraph 6 | The unit on Dagger introduces a type | Rewrite using human title |
| `lesson_from_one_dependency_to_a_graph` | PRACTICAL / paragraph 1 | The running example ends the previous unit with | Remove |
| `lesson_from_one_dependency_to_a_graph` | PRACTICAL / paragraph 6 | The per-use summary builder from the previous unit | Remove |
| `lesson_from_one_dependency_to_a_graph` | PRACTICAL / paragraph 8 | that is the next lesson's entire subject | Rewrite using human title |
| `lesson_from_one_dependency_to_a_graph` | PRACTICAL / callout 1 | the previous unit's lesson on injection against inversion | Rewrite using human title |
| `lesson_from_one_dependency_to_a_graph` | PRACTICAL / paragraph 11 | the unit on Dagger names one | Rewrite using human title |
| `lesson_from_one_dependency_to_a_graph` | SENIOR / paragraph 3 | the previous unit judged this graph | Remove |
| `lesson_one_instance_or_a_new_one` | CORE / paragraph 1 | Take the graph from the previous lesson | Remove |
| `lesson_one_instance_or_a_new_one` | PRACTICAL / paragraph 3 | later units grow it rather than inventing it all at once | Remove |
| `lesson_one_instance_or_a_new_one` | PRACTICAL / paragraph 6 | A second consequence follows in the next lesson | Rewrite using human title |
| `lesson_one_instance_or_a_new_one` | SENIOR / paragraph 3 | they are what the next lesson is entirely about | Remove |
| `lesson_scope_is_a_rule_owner_is_a_lifetime` | CORE / callout 2 | The coroutines curriculum owns the first | Remove |
| `lesson_scope_is_a_rule_owner_is_a_lifetime` | PRACTICAL / paragraph 1 | the reading application's requirement from the previous lesson | Remove |
| `lesson_scope_is_a_rule_owner_is_a_lifetime` | PRACTICAL / paragraph 2 | The architecture curriculum already taught how to pick an owner | Rewrite using human title |
| `lesson_scope_is_a_rule_owner_is_a_lifetime` | PRACTICAL / paragraph 7 | durability is a different subject with its own curriculum | Remove |
| `lesson_scope_is_a_rule_owner_is_a_lifetime` | SENIOR / paragraph 2 | the previous lesson's over-sharing failure | Remove |
| `lesson_runtime_input_is_not_a_dependency` | CORE / paragraph 4 | `QuestionCatalogConfig` from the previous unit | Remove |
| `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 7 | the half-built object from the lesson on constructor injection… That lesson priced all three | Rewrite using human title |
| `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 9 | in exactly the sense the first lesson gave that word | Remove |
| `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 10 | the previous unit already used one for per-use construction | Remove |
| `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 11 | belongs to the navigation and lifecycle material | Remove |
| `lesson_runtime_input_is_not_a_dependency` | SENIOR / paragraph 2 | the previous unit's per-use summary builder | Remove |
| `lesson_two_dependencies_of_the_same_type` | PRACTICAL / paragraph 5 | The test is the familiar one from the architecture material | Keep |
| `lesson_two_dependencies_of_the_same_type` | SENIOR / paragraph 2 | which the architecture curriculum already identified as an abstraction earning nothing | Remove |
| `lesson_when_a_broken_graph_tells_you` | CORE / bullet list 1 | the previous lesson's collision | Remove |
| `lesson_when_a_broken_graph_tells_you` | PRACTICAL / comparison 2 | the hazard the previous unit named | Remove |
| `lesson_when_a_broken_graph_tells_you` | SENIOR / bullet list 1 | the previous unit showed a fully injected program with every arrow wrong | Rewrite using human title |
| `lesson_when_a_broken_graph_tells_you` | SENIOR / paragraph 3 | the architecture curriculum already named | Remove |
| `lesson_when_a_broken_graph_tells_you` | SENIOR / paragraph 4 | the closing unit of this curriculum compares… the unit on Dagger is where | Rewrite using human title |

</details>

<details>
<summary>27. Dagger: Compile-Time Object Graphs — 44 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | The previous two units made every decision a dependency graph requires | Remove |
| `lesson_dagger_constructs_what_it_can_see` | Lesson summary | Unit 1 wrote the assembly by hand and Unit 2 traced the graph | Remove |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 1 | By the end of the last unit the reader was doing two jobs by hand | Remove |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 2 | something the reader already decided in Units 1 and 2 | Remove |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 3 | The first question was the unit-1 one… the last two units traced | Bring required context local |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 4 | That was the whole argument of the first unit | Remove |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 5 | exactly as the graph drawing in the previous unit had them | Remove |
| `lesson_dagger_constructs_what_it_can_see` | CORE / paragraph 6 | the second and third questions of Unit 2 | Bring required context local |
| `lesson_dagger_constructs_what_it_can_see` | PRACTICAL / paragraph 1 | In Unit 1 the composition root read something like this | Remove |
| `lesson_dagger_constructs_what_it_can_see` | PRACTICAL / paragraph 3 | Unit 1 already expressed every one of these arrangements by hand | Remove |
| `lesson_dagger_constructs_what_it_can_see` | PRACTICAL / paragraph 6 | Unit 2 treated when a broken graph tells you… The final lesson of this unit says exactly what that check covers | Rewrite using human title |
| `lesson_dagger_constructs_what_it_can_see` | PRACTICAL / paragraph 7 | each is a Unit 2 decision the reader already knows how to make | Remove |
| `lesson_dagger_constructs_what_it_can_see` | SENIOR / paragraph 3 | Every arrangement in this unit was expressible in Unit 1 without a library | Remove |
| `lesson_declaring_the_rest_of_the_graph` | CORE / paragraph 1 | the reading application from the last unit | Remove |
| `lesson_declaring_the_rest_of_the_graph` | PRACTICAL / paragraph 3 | the condition the architecture material sets for an interface earning its place | Keep |
| `lesson_declaring_the_rest_of_the_graph` | SENIOR / comparison 1 | The next two lessons are about exactly this | Rewrite using human title |
| `lesson_declaring_the_rest_of_the_graph` | SENIOR / paragraph 2 | The next lesson is about that place | Remove |
| `lesson_which_graph_owns_this_binding` | CORE / paragraph 1 | the thing Unit 2 actually drew | Remove |
| `lesson_which_graph_owns_this_binding` | CORE / paragraph 3 | exactly the graph-tracing the previous unit taught | Remove |
| `lesson_which_graph_owns_this_binding` | CORE / paragraph 4 | the final lesson of this unit is about when the reader finds out | Rewrite using human title |
| `lesson_which_graph_owns_this_binding` | PRACTICAL / bullet list 1 | the published surface of another component, which the next lesson is about | Rewrite using human title |
| `lesson_which_graph_owns_this_binding` | PRACTICAL / paragraph 6 | the distinction the first unit drew between being handed a collaborator and going to fetch one | Remove |
| `lesson_which_graph_owns_this_binding` | PRACTICAL / paragraph 7 | The first unit defined the composition root as a responsibility | Remove |
| `lesson_which_graph_owns_this_binding` | SENIOR / paragraph 3 | the shape the first unit argued against | Bring required context local |
| `lesson_child_graph_or_separate_graph` | CORE / bullet list 2 | the same entry-point list the previous lesson read as a public surface | Remove |
| `lesson_child_graph_or_separate_graph` | SENIOR / paragraph 3 | which is the next lesson | Rewrite using human title |
| `lesson_dagger_scopes_and_component_instances` | Lesson summary | The previous unit separated three things the word "scope" is collapsed into | Remove |
| `lesson_dagger_scopes_and_component_instances` | CORE / paragraph 1 | The previous unit's rule was that such a requirement is satisfied… | Remove |
| `lesson_dagger_scopes_and_component_instances` | CORE / paragraph 5 | the chain from the previous unit survives intact | Remove |
| `lesson_dagger_scopes_and_component_instances` | SENIOR / paragraph 1 | The previous unit priced both directions of getting the owner wrong | Rewrite using human title |
| `lesson_when_the_type_is_not_the_key` | Lesson summary | Two lessons ago in the previous unit, a consumer asked for an `HttpClient` | Bring required context local |
| `lesson_when_the_type_is_not_the_key` | CORE / paragraph 1 | The previous unit left a collision unresolved on purpose | Remove |
| `lesson_when_the_type_is_not_the_key` | CORE / paragraph 2 | The previous unit's first answer still stands | Rewrite using human title |
| `lesson_when_the_type_is_not_the_key` | PRACTICAL / paragraph 3 | the same gain the previous unit got from two distinct types | Remove |
| `lesson_when_the_type_is_not_the_key` | SENIOR / paragraph 2 | the inversion the architecture material described | Keep |
| `lesson_when_the_type_is_not_the_key` | SENIOR / paragraph 4 | the thing the previous unit ruled out | Remove |
| `lesson_what_the_dagger_compiler_checked` | CORE / paragraph 1 | The previous unit asked when a broken graph tells you | Rewrite using human title |
| `lesson_what_the_dagger_compiler_checked` | PRACTICAL / paragraph 1 | The component from earlier in this unit had a hole in it | Remove |
| `lesson_what_the_dagger_compiler_checked` | PRACTICAL / paragraph 3 | the previous unit's collision | Remove |
| `lesson_what_the_dagger_compiler_checked` | PRACTICAL / paragraph 4 | the one the previous lesson gave | Remove |
| `lesson_what_the_dagger_compiler_checked` | PRACTICAL / callout 1 | The previous unit rejected all of those as design answers | Remove |
| `lesson_what_the_dagger_compiler_checked` | SENIOR / paragraph 3 | the per-destination object from the previous unit | Remove |
| `lesson_what_the_dagger_compiler_checked` | SENIOR / paragraph 4 | the rule the previous lesson gave | Remove |
| `lesson_what_the_dagger_compiler_checked` | SENIOR / paragraph 5 | the over-sharing failure the previous unit priced… the vocabulary from that unit | Rewrite using human title |

</details>

<details>
<summary>28. Hilt: Android Lifecycle-Aware Dagger — 9 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_hilt_is_dagger_with_decisions_made` | CORE / paragraph 1 | Unit 3 put the engineer in charge of a Dagger graph | Bring required context local |
| `lesson_hilt_is_dagger_with_decisions_made` | PRACTICAL / paragraph 1 | Continue the reading graph from Unit 3 | Remove |
| `lesson_which_android_component_owns_this` | CORE / paragraph 1 | Carry Unit 2's reasoning forward unchanged | Bring required context local |
| `lesson_which_android_component_owns_this` | PRACTICAL / paragraph 1 | Example B now has a precise requirement | Bring required context local |
| `lesson_which_android_component_owns_this` | PRACTICAL / paragraph 3 | covered by the existing process and saved-state material | Rewrite using human title |
| `lesson_when_android_owns_construction` | CORE / paragraph 1 | Unit 1's normal form was simple | Remove |
| `lesson_when_android_owns_construction` | SENIOR / paragraph 2 | This is Unit 1's service-locator nuance made concrete | Rewrite using human title |
| `lesson_hilt_viewmodels_and_runtime_input` | PRACTICAL / paragraph 5 | encodes the same split as Unit 2 | Bring required context local |
| `lesson_which_graph_does_this_binding_join` | CORE / paragraph 1 | Unit 3 already established what a Dagger module does | Remove |

</details>

<details>
<summary>29. Koin and Dependency Injection in KMP — 10 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| Unit summary | Unit summary | the construction, reuse and ownership decisions from the earlier Units | Remove |
| `lesson_the_koin_container_and_its_modules` | CORE / paragraph 1 | Units 1 and 2 made construction explicit | Bring required context local |
| `lesson_the_koin_container_and_its_modules` | PRACTICAL / paragraph 3 | their setup belongs to build engineering rather than this Unit | Remove |
| `lesson_koin_definitions_and_reuse` | CORE / paragraph 1 | Begin with Unit 2's question | Bring required context local |
| `lesson_koin_definitions_and_reuse` | PRACTICAL / paragraph 4 | the service-locator shape from Unit 1 | Rewrite using human title |
| `lesson_koin_scopes_and_their_owners` | CORE / paragraph 2 | This encodes Unit 2's generic rule | Bring required context local |
| `lesson_resolving_viewmodels_at_the_boundary` | CORE / paragraph 2 | the same separation the lifecycle curriculum already established | Rewrite using human title |
| `lesson_resolving_viewmodels_at_the_boundary` | PRACTICAL / paragraph 3 | useful evidence of Unit 2's same-type ambiguity | Rewrite using human title |
| `lesson_resolving_viewmodels_at_the_boundary` | SENIOR / paragraph 1 | the shipped lifetime curriculum | Remove |
| `lesson_one_graph_across_platforms` | SENIOR / paragraph 2 | the next Unit owns that selection | Rewrite using human title |

</details>

<details>
<summary>30. Choosing a Dependency Injection Strategy — 4 references</summary>

| Lesson | Location | Reference | Decision |
| --- | --- | --- | --- |
| `lesson_when_should_a_graph_error_surface` | SENIOR / paragraph 2 | Unit 3's compiler Lesson and Unit 2's scope-owner Lesson carry the full argument | Rewrite using human title |
| `lesson_when_should_a_graph_error_surface` | SENIOR / paragraph 3 | this Unit does not reactivate or edit that Question | Remove |
| `lesson_three_projects_three_answers` | PRACTICAL / paragraph 6 | outside this Unit's four taught strategies | Remove |
| `lesson_the_smallest_sufficient_strategy` | CORE / paragraph 1 | The architecture curriculum ended with proportionality | Keep |

</details>


## Shape Audit

All 194 occurrences were read and classified: 188 in prose, 6 in code comments.

| Decision | Prose | Code comments | Total |
| --- | ---: | ---: | ---: |
| Keep | 25 | 0 | 25 |
| Replace | 140 | 6 | 146 |
| Remove | 23 | 0 | 23 |
| **Total** | **188** | **6** | **194** |

**Editorial conclusion.** "Shape" is used legitimately in three recognisable senses and
habitually everywhere else. The legitimate uses are *state shape* (the structure of a UI
state or of shared state: data class against sealed hierarchy; one value against related
fields), *API shape* when the text is contrasting a stream with a suspending read as kinds of
API, and *graph shape* where a Lesson defines it on the spot (depth, sharing, reach of a
change). Everything else has a more precise word the Lesson itself usually already uses:
**signature** (the contract's own example appears verbatim: "two functions with the same
shape"), **contract**, **representation** or **format** (Architecture), **arrangement** or
**pattern** (DI and MVP/MVVM/MVI), **kind of work**, **delivery model**. Twenty-three
occurrences are pure framing — "the shape worth noticing is what is absent", "Read the shape
of that warning", "The shape is what matters" — and should simply be deleted. The habit is
not spread evenly: Flow Fundamentals (15), Screen State Holders (15), Repositories (28, of
which 13 in one Lesson), MVP/MVVM/MVI (13), and the two DI foundation Units (13 each) carry
most of it. There is no single replacement word; "structure" everywhere would repeat the
habit.

Per Unit:

| # | Unit ID | Keep | Replace | Remove | Total |
| ---: | --- | ---: | ---: | ---: | ---: |
| 1 | `unit_thinking_in_compose` | 0 | 2 | 0 | 2 |
| 2 | `unit_state_and_state_ownership` | 0 | 3 | 1 | 4 |
| 3 | `unit_recomposition` | 0 | 0 | 1 | 1 |
| 4 | `unit_identity_keys_and_stability` | 1 | 1 | 1 | 3 |
| 5 | `unit_derived_state_and_expensive_work` | 0 | 1 | 0 | 1 |
| 6 | `unit_snapshot_fundamentals` | 0 | 0 | 1 | 1 |
| 10 | `unit_latest_values_and_event_driven_work` | 0 | 1 | 0 | 1 |
| 11 | `unit_cleanup_synchronization_and_producers` | 0 | 3 | 1 | 4 |
| 12 | `unit_production_ui_effects_and_selection` | 0 | 2 | 1 | 3 |
| 13 | `unit_coroutines_and_structured_concurrency` | 0 | 5 | 3 | 8 |
| 14 | `unit_context_dispatchers_and_concurrency` | 0 | 4 | 0 | 4 |
| 15 | `unit_cancellation_failure_and_coordination` | 4 | 3 | 0 | 7 |
| 16 | `unit_flow_fundamentals` | 0 | 12 | 3 | 15 |
| 17 | `unit_flow_composition_timing_and_failure` | 0 | 4 | 0 | 4 |
| 18 | `unit_stateflow_sharedflow_and_hot_streams` | 0 | 4 | 0 | 4 |
| 19 | `unit_architecture_responsibilities_and_boundaries` | 2 | 6 | 1 | 9 |
| 20 | `unit_screen_state_holders_and_ui_state` | 7 | 7 | 1 | 15 |
| 21 | `unit_repositories_and_data_ownership` | 5 | 21 | 2 | 28 |
| 22 | `unit_domain_logic_and_dependency_direction` | 0 | 6 | 2 | 8 |
| 23 | `unit_responsibility_models_mvp_mvvm_mvi` | 0 | 10 | 3 | 13 |
| 24 | `unit_state_events_lifetime_and_selection` | 3 | 6 | 1 | 10 |
| 25 | `unit_dependency_injection_as_object_construction` | 0 | 13 | 0 | 13 |
| 26 | `unit_object_graphs_lifetimes_and_scopes` | 3 | 10 | 0 | 13 |
| 27 | `unit_dagger_compile_time_object_graphs` | 0 | 8 | 1 | 9 |
| 28 | `unit_hilt_android_lifecycle_integration` | 0 | 3 | 0 | 3 |
| 29 | `unit_koin_and_dependency_injection_in_kmp` | 0 | 3 | 0 | 3 |
| 30 | `unit_choosing_a_dependency_injection_strategy` | 0 | 2 | 0 | 2 |
| | **Total (prose)** | 25 | 140 | 23 | 188 |

Code comments:

| Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- |
| `lesson_remember_saveable` | PRACTICAL / code 2 | // Wrong shape: … // Right shape: … | Replace | "Saves the whole article" / "Saves only its id" |
| `lesson_model_and_error_boundaries` | PRACTICAL / code 1 | // the service's shape / the device's shape / the application's shape | Replace | "wire format", "stored representation", "application model" |
| `lesson_koin_definitions_and_reuse` | PRACTICAL / code 3 | // Hidden dependency: service-locator-shaped business code | Replace | "business code acting as a service locator" |

Full inventory, by Unit (sweep numbers are stable identifiers for this pass):

<details>
<summary>1. Thinking in Compose — 2 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S001 | `lesson_state_down_events_up` | PRACTICAL / paragraph 1 | The shape people quote most often is a current value plus an onValueChange callback | Replace | "parameter pair" / "signature" |
| S002 | `lesson_state_down_events_up` | PRACTICAL / paragraph 1 | it is the right shape for a component that edits a single value | Replace | "the right API for" |

</details>

<details>
<summary>2. State and State Ownership — 4 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S003 | `lesson_observable_state` | PRACTICAL / comparison 1 | mirrors the value-plus-callback shape hoisting uses | Replace | "the value-plus-callback pair" |
| S004 | `lesson_remember_saveable` | CORE / comparison 1 | Whenever the UI's shape changes | Replace | "whenever composables enter or leave the Composition" |
| S005 | `lesson_state_hoisting` | CORE / paragraph 3 | That value-plus-callback shape | Replace | "That value-plus-callback pair" |
| S006 | `lesson_observable_collections` | CORE / paragraph 4 | it removes the shape of the bug from the code | Remove | "the bug can no longer be written" |

</details>

<details>
<summary>3. Recomposition — 1 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S007 | `lesson_recomposition_scopes` | PRACTICAL / paragraph 6 | The same idea appears in other shapes — passing `() -> Int`… | Remove | "Passing `() -> Int`… defers the read the same way" |

</details>

<details>
<summary>4. Identity, Keys, Stability and Immutability — 3 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S008 | `lesson_composable_identity` | PRACTICAL / paragraph 5 | when the panel changes shape | Keep | literal visual/layout change |
| S009 | `lesson_keys_and_identity_in_lists` | CORE / paragraph 3 | the shape most interview questions take | Remove | state the scenario |
| S010 | `lesson_stability_annotations` | SENIOR / paragraph 1 | when the composition shape happens to reach a skip | Replace | "when a recomposition happens to reach a skipped call" |

</details>

<details>
<summary>5. Derived State and Expensive Work — 1 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S011 | `lesson_derived_state` | SENIOR / paragraph 2 | `remember { derivedStateOf { ... } }` is the shape for a reason, not as idiom | Replace | "is the required form" / "is written that way for a reason" |

</details>

<details>
<summary>6. Snapshot Fundamentals — 1 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S012 | `lesson_snapshot_flow` | CORE / paragraph 1 | Every consumer so far has had the same shape: a composable that reads state | Remove | "So far every consumer has been a composable that…" |

</details>

<details>
<summary>10. Latest-Value Effects and Event-Driven Coroutine Work — 1 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S013 | `lesson_remember_updated_state` | PRACTICAL / comparison 1 | Correct shape | Replace | "Implementation" |

</details>

<details>
<summary>11. Cleanup, External Synchronization and State Producers — 4 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S014 | `lesson_disposable_effect` | CORE / paragraph 2 | it has a different shape from "run this suspend work…" | Replace | "is a different problem from" |
| S015 | `lesson_disposable_effect` | CORE / paragraph 3 | the mechanism whose shape matches that problem | Remove | "is built for that problem" |
| S016 | `lesson_side_effect_publication` | SENIOR / paragraph 4 | Keep it to assignment-shaped publication | Replace | "simple assignments" |
| S017 | `lesson_flow_adapter_or_compose_producer` | PRACTICAL / paragraph 2 | the producer shape is what that code actually wanted | Replace | "a state producer" |

</details>

<details>
<summary>12. Production UI Effects and Mechanism Selection — 3 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S018 | `lesson_choosing_a_compose_mechanism` | PRACTICAL / paragraph 6 | the shape worth noticing is what is absent | Remove | "There is no remembered flag…" |
| S019 | `lesson_transient_ui_effects` | PRACTICAL / paragraph 4 | the one the trigger lesson asked about a similar shape | Replace | "a similar flag-driven snackbar" |
| S020 | `lesson_transient_effect_delivery` | PRACTICAL / comparison 1 | the shape the stream curriculum calls current-value semantics | Replace | "current-value semantics (as `StateFlow` provides)" |

</details>

<details>
<summary>13. Coroutine Fundamentals and Structured Concurrency — 8 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S021 | `lesson_suspension_and_blocking` | PRACTICAL / paragraph 3 | Two functions with the same shape | Replace | "the same signature" |
| S022 | `lesson_coroutine_builders` | CORE / paragraph 6 | is the same shape with one ordinary argument in front | Replace | "the same call" |
| S023 | `lesson_coroutine_builders` | CORE / callout 1 | not from the shape of the surrounding code | Remove | "not from habit" |
| S024 | `lesson_coroutine_scope_ownership` | PRACTICAL / paragraph 1 | Here is the shape with no owner | Replace | "a scope with no owner" |
| S025 | `lesson_coroutine_scope_ownership` | PRACTICAL / paragraph 6 | the one worth recognising as the shape to imitate | Replace | "the model to imitate" |
| S026 | `lesson_coroutine_scope_ownership` | SENIOR / paragraph 2 | Read the shape of that warning | Remove | delete the framing |
| S027 | `lesson_structured_concurrency` | PRACTICAL / bullet list 1 | this shape returned in 5 ms | Replace | "this version" |
| S028 | `lesson_structured_concurrency` | SENIOR / callout 1 | the detached-`launch` shape | Remove | "a detached `launch`" |

</details>

<details>
<summary>14. Coroutine Context, Dispatchers and Concurrent Work — 4 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S029 | `lesson_dispatchers` | Unit summary (unit_context_dispatchers_and_concurrency) | chooses dispatchers from the shape of the work | Replace | "the kind of work" |
| S030 | `lesson_dispatchers` | CORE / comparison 1 | Work shape | Replace | "Kind of work" |
| S031 | `lesson_with_context_and_main_safety` | PRACTICAL / paragraph 1 | a common Kotlin shape for a repository wrapping a legacy blocking dependency | Replace | "a common way to wrap" |
| S032 | `lesson_sequential_and_concurrent_work` | PRACTICAL / paragraph 6 | before using this shape | Replace | "before starting one child per item" |

</details>

<details>
<summary>15. Cancellation, Failure and Coordination — 7 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S033 | `lesson_cooperative_cancellation` | SENIOR / bullet list 1 | they suit different shapes of loop | Replace | "different kinds of loop" |
| S034 | `lesson_cancellation_cleanup_and_timeouts` | PRACTICAL / paragraph 2 | Keep the block small and cleanup-shaped | Replace | "limited to cleanup" |
| S035 | `lesson_cancellation_cleanup_and_timeouts` | SENIOR / bullet list 1 | kept small and cleanup-shaped | Replace | "limited to cleanup" |
| S036 | `lesson_shared_state_and_coordination` | CORE / callout 1 | the right answer depends on the shape of the state | Keep | "state shape" in the contract's legitimate sense (one value / related fields / owned body) |
| S037 | `lesson_shared_state_and_coordination` | SENIOR / paragraph 1 | the shape of the state usually decides it | Keep | same meaning; the table defines it |
| S038 | `lesson_shared_state_and_coordination` | SENIOR / comparison 1 | Shape of the state | Keep | table header defining the three state structures |
| S039 | `lesson_shared_state_and_coordination` | SENIOR / callout 1 | for a given shape of state | Keep | same meaning |

</details>

<details>
<summary>16. Flow Fundamentals — 15 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S040 | `lesson_why_flow` | Unit summary (unit_flow_fundamentals) | why the shape exists | Replace | "why a stream type exists" |
| S041 | `lesson_why_flow` | CORE / paragraph 1 | had the same shape at its edges | Replace | "the same contract with its caller" |
| S042 | `lesson_why_flow` | CORE / paragraph 1 | That shape is right for a great many operations | Replace | "That one-answer contract" |
| S043 | `lesson_why_flow` | CORE / paragraph 3 | Kotlin's type for the second shape | Replace | "the second contract" |
| S044 | `lesson_why_flow` | CORE / callout 1 | The question that chooses the shape | Replace | "chooses the return type" |
| S045 | `lesson_why_flow` | PRACTICAL / paragraph 3 | the more powerful shape | Remove | state the trade directly |
| S046 | `lesson_why_flow` | SENIOR / paragraph 1 | a repository-shaped API | Replace | "a repository API" |
| S047 | `lesson_why_flow` | SENIOR / paragraph 3 | This shape is what makes a single source of truth workable | Replace | "An observable API" |
| S048 | `lesson_cold_flows` | CORE / paragraph 2 | after constructing exactly this shape | Replace | "this flow" |
| S049 | `lesson_flow_collection_lifetime` | CORE / paragraph 1 | applied to a new shape | Remove | delete with the sentence |
| S050 | `lesson_flow_builders_and_callback_adapters` | Lesson summary | The producer's shape picks the builder | Replace | "How the producer delivers values" |
| S051 | `lesson_flow_builders_and_callback_adapters` | CORE / paragraph 1 | producers of one particular shape | Replace | "one particular kind" |
| S052 | `lesson_flow_builders_and_callback_adapters` | CORE / callout 1 | The builder follows from the producer's shape | Replace | "from how the producer works" |
| S053 | `lesson_flow_builders_and_callback_adapters` | PRACTICAL / paragraph 5 | exactly the shape the cleanup Lesson taught | Remove | "the same guarantee a `finally` block gives" |
| S054 | `lesson_flow_builders_and_callback_adapters` | SENIOR / comparison 1 | Producer shape | Replace | "How values are produced" |

</details>

<details>
<summary>17. Flow Composition, Timing and Failure — 4 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S055 | `lesson_combining_flows` | PRACTICAL / paragraph 5 | There is a third shape | Replace | "a third operator" |
| S056 | `lesson_combining_flows` | SENIOR / paragraph 3 | the problem needed a different shape | Replace | "a different stream contract" |
| S057 | `lesson_flattening_flows` | SENIOR / paragraph 3 | a latest-shaped operator | Replace | "a latest-value operator" |
| S058 | `lesson_flow_failure_and_completion` | SENIOR / paragraph 2 | the same shape of pipeline | Replace | "the same pipeline" |

</details>

<details>
<summary>18. StateFlow, SharedFlow and Hot Streams — 4 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S059 | `lesson_state_flow` | CORE / paragraph 3 | what shape it should take | Replace | "how it should be modelled" (sentence likely deleted) |
| S060 | `lesson_choosing_a_stream_abstraction` | CORE / paragraph 6 | no Flow shape answers | Replace | "no Flow type answers" |
| S061 | `lesson_choosing_a_stream_abstraction` | PRACTICAL / paragraph 1 | a delivery shape none of the Flow types offer | Replace | "a delivery model" |
| S062 | `lesson_choosing_a_stream_abstraction` | PRACTICAL / paragraph 2 | the difference is the **shape** of the delivery | Replace | "who receives each element" |

</details>

<details>
<summary>19. Architecture as Responsibilities and Boundaries — 9 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S063 | `lesson_what_architecture_decides` | SENIOR / paragraph 3 | not because one shape is more elegant than another | Keep | broad structural form of a codebase |
| S064 | `lesson_responsibility_and_change` | SENIOR / paragraph 1 | rather than about code shape | Keep | broad structural form |
| S065 | `lesson_dependency_direction_and_boundaries` | PRACTICAL / paragraph 1 | The shape the service sends | Replace | "The payload format" |
| S066 | `lesson_dependency_direction_and_boundaries` | SENIOR / paragraph 3 | the answer has the same shape | Remove | "the answer is always the same:" |
| S067 | `lesson_when_an_interface_is_a_boundary` | CORE / paragraph 4 | would not dictate the implementation's shape | Replace | "the implementation's API" |
| S068 | `lesson_when_an_interface_is_a_boundary` | PRACTICAL / paragraph 3 | whenever the file store's shape changes | Replace | "the file store's methods" |
| S069 | `lesson_when_an_interface_is_a_boundary` | SENIOR / paragraph 3 | the shape of the contract always was | Replace | "what the contract says, and who owns it" |
| S070 | `lesson_layers_and_their_cost` | PRACTICAL / comparison 1 | When the two shapes are the same | Replace | "representations" |
| S071 | `lesson_layers_and_their_cost` | PRACTICAL / comparison 1 | When the two shapes diverge | Replace | "representations" |

</details>

<details>
<summary>20. Screen State Holders, ViewModel and UI State — 15 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S072 | `lesson_modelling_ui_state` | Unit summary (unit_screen_state_holders_and_ui_state) | what shape the current UI state should take | Keep | "state shape" in the legitimate sense |
| S073 | `lesson_modelling_ui_state` | Lesson summary | what shape that value takes | Keep | "state shape" |
| S074 | `lesson_modelling_ui_state` | CORE / paragraph 2 | Several shapes can express all of that | Replace | "Several models" |
| S075 | `lesson_modelling_ui_state` | CORE / paragraph 3 | the shape follows | Keep | "state shape" |
| S076 | `lesson_modelling_ui_state` | CORE / callout 1 | its shape is a per-screen decision | Keep | "state shape" |
| S077 | `lesson_modelling_ui_state` | PRACTICAL / paragraph 1 | It is the shape most screens start with | Replace | "the model" |
| S078 | `lesson_modelling_ui_state` | PRACTICAL / paragraph 4 | A second point about the same shape | Remove | delete with the unclear sentence |
| S079 | `lesson_modelling_ui_state` | SENIOR / paragraph 1 | The case the two shapes handle worst | Replace | "the two models" |
| S080 | `lesson_modelling_ui_state` | SENIOR / paragraph 5 | The shape is an answer to a question about the product | Keep | "state shape" |
| S081 | `lesson_state_out_intentions_in` | CORE / paragraph 2 | The shape that makes the claim true | Replace | "The API" |
| S082 | `lesson_state_out_intentions_in` | CORE / paragraph 3 | already taught this shape inside the composable tree | Replace | "this direction" |
| S083 | `lesson_state_out_intentions_in` | CORE / paragraph 4 | written today's state shape into the contract | Keep | "state shape" |
| S084 | `lesson_state_out_intentions_in` | PRACTICAL / paragraph 3 | The correct shape costs one line | Replace | "The correct version" |
| S085 | `lesson_state_out_intentions_in` | PRACTICAL / paragraph 7 | setter-shaped functions | Replace | "setters" |
| S086 | `lesson_state_out_intentions_in` | PRACTICAL / paragraph 7 | published the current state shape as part of the contract | Keep | "state shape" |

</details>

<details>
<summary>21. Repositories, Data Ownership and Single Source of Truth — 28 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S087 | `lesson_what_a_repository_owns` | Unit summary (unit_repositories_and_data_ownership) | shapes the data API from what its consumer must observe | Replace | "designs" |
| S088 | `lesson_what_a_repository_owns` | CORE / bullet list 1 | one shaped by whatever produced the data | Keep | ordinary verb ("determined by") |
| S089 | `lesson_what_a_repository_owns` | CORE / paragraph 4 | all examples of that shape | Replace | "examples of a data source" |
| S090 | `lesson_what_a_repository_owns` | PRACTICAL / paragraph 1 | The shape is three types | Replace | "The design is three types" |
| S091 | `lesson_coordinating_sources` | PRACTICAL / paragraph 3 | the same shape becomes wrong | Replace | "the same read path" |
| S092 | `lesson_coordinating_sources` | PRACTICAL / paragraph 10 | What shape that failure reason should take | Replace | "How that failure reason is represented" |
| S093 | `lesson_observable_or_one_shot_api` | Lesson summary | compares both shapes | Replace | "compares a stream and a suspending read" |
| S094 | `lesson_observable_or_one_shot_api` | CORE / paragraph 1 | The shape of a data API | Keep | "API shape" in the legitimate sense |
| S095 | `lesson_observable_or_one_shot_api` | CORE / bullet list 1 | "A stream is the modern shape." | Keep | quoted slogan |
| S096 | `lesson_observable_or_one_shot_api` | CORE / bullet list 1 | A shape is not an argument | Remove | "Fashion is not an argument" or delete |
| S097 | `lesson_observable_or_one_shot_api` | CORE / bullet list 1 | names two shapes | Replace | "names two kinds of API" |
| S098 | `lesson_observable_or_one_shot_api` | CORE / callout 1 | Choose the API shape | Keep | "API shape" |
| S099 | `lesson_observable_or_one_shot_api` | CORE / callout 1 | a stream is the shape that expresses that | Replace | "a stream expresses that" |
| S100 | `lesson_observable_or_one_shot_api` | PRACTICAL / paragraph 4 | two different shapes | Replace | "two different APIs" |
| S101 | `lesson_observable_or_one_shot_api` | PRACTICAL / paragraph 7 | choosing the observable shape | Replace | "choosing an observable API" |
| S102 | `lesson_observable_or_one_shot_api` | PRACTICAL / paragraph 8 | One shape is wrong regardless | Replace | "One API is wrong" |
| S103 | `lesson_observable_or_one_shot_api` | PRACTICAL / callout 1 | not a legacy shape | Replace | "not a legacy API" |
| S104 | `lesson_observable_or_one_shot_api` | SENIOR / paragraph 1 | offer both shapes | Replace | "offer both kinds of read" |
| S105 | `lesson_observable_or_one_shot_api` | SENIOR / paragraph 3 | the two shapes are simply two ways | Replace | "the two reads" |
| S106 | `lesson_model_and_error_boundaries` | CORE / paragraph 3 | the received shape does match | Replace | "the received format" |
| S107 | `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 2 | Each is shaped by a different set of constraints | Keep | ordinary verb |
| S108 | `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 5 | when the shapes are otherwise aligned | Replace | "representations" |
| S109 | `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 9 | identical in shape to the shared-model coupling | Remove | "the same coupling as" |
| S110 | `lesson_model_and_error_boundaries` | PRACTICAL / paragraph 10 | Three shapes are all defensible | Replace | "Three options" |
| S111 | `lesson_model_and_error_boundaries` | PRACTICAL / bullet list 1 | is this shape | Replace | "is this option" |
| S112 | `lesson_model_and_error_boundaries` | PRACTICAL / callout 1 | the stored shape | Replace | "stored representation" |
| S113 | `lesson_model_and_error_boundaries` | PRACTICAL / callout 1 | the application's shape | Replace | "the application's representation" |
| S114 | `lesson_model_and_error_boundaries` | SENIOR / paragraph 4 | the three shapes above | Replace | "the three options" |

</details>

<details>
<summary>22. Domain Logic, Use Cases and Dependency Direction — 8 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S115 | `lesson_when_a_domain_layer_earns_its_place` | SENIOR / paragraph 1 | they have the same shape | Remove | "and they are built the same way:" |
| S116 | `lesson_use_cases_and_pass_through_cost` | Lesson summary | side by side in the same shape | Replace | "with the same structure" |
| S117 | `lesson_use_cases_and_pass_through_cost` | CORE / paragraph 5 | two types deliberately given the same shape | Replace | "the same structure" |
| S118 | `lesson_use_cases_and_pass_through_cost` | PRACTICAL / paragraph 3 | Now the same shape with something inside it | Replace | "the same class" |
| S119 | `lesson_use_cases_and_pass_through_cost` | SENIOR / bullet list 1 | have the same shape | Remove | delete with the app-specific list |
| S120 | `lesson_use_cases_and_pass_through_cost` | SENIOR / bullet list 1 | turn stored facts into the shape a consumer needs | Replace | "the form a consumer needs" |
| S121 | `lesson_dependency_inversion_in_practice` | PRACTICAL / paragraph 3 | When storage changes shape | Replace | "changes its representation" |
| S122 | `lesson_clean_architecture_intent` | PRACTICAL / comparison 1 | where the stored shape and the application's diverge | Replace | "stored representation" |

</details>

<details>
<summary>23. MVP, MVVM and MVI Responsibility Models — 13 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S123 | `lesson_one_screen_five_questions` | PRACTICAL / comparison 1 | The shape of question 4's answer | Replace | "How input is modelled" |
| S124 | `lesson_mvp_view_contract` | CORE / paragraph 2 | the shape is immediately different | Replace | "the answers are immediately different" |
| S125 | `lesson_mvp_view_contract` | PRACTICAL / paragraph 3 | none of it would change the shape | Replace | "the arrangement" |
| S126 | `lesson_mvp_view_contract` | PRACTICAL / paragraph 3 | The shape is what matters | Remove | delete the framing |
| S127 | `lesson_mvvm_observed_state` | PRACTICAL / paragraph 5 | whether a codebase is MVVM-shaped | Replace | "has an MVVM arrangement" |
| S128 | `lesson_mvvm_observed_state` | SENIOR / paragraph 1 | the problem was never the shape of the output | Replace | "whether output is pushed or published" |
| S129 | `lesson_mvi_intent_and_reduction` | PRACTICAL / paragraph 2 | expressed in a different shape | Remove | delete with the positional clause |
| S130 | `lesson_classifying_a_real_architecture` | CORE / paragraph 1 | its input shape from a third | Replace | "its input model" |
| S131 | `lesson_classifying_a_real_architecture` | PRACTICAL / paragraph 1 | rather than by the shape of the summary | Remove | "rather than by the label" |
| S132 | `lesson_classifying_a_real_architecture` | PRACTICAL / paragraph 5 | people who have adopted a shape from people who chose a model | Replace | "adopted a syntax" |
| S133 | `lesson_classifying_a_real_architecture` | PRACTICAL / callout 1 | input shape | Replace | "input model" |
| S134 | `lesson_classifying_a_real_architecture` | SENIOR / callout 1 | input shape | Replace | "input model" |
| S135 | `lesson_classifying_a_real_architecture` | SENIOR / callout 1 | output shape | Replace | "output model" |

</details>

<details>
<summary>24. State, Events, Lifetime and Architecture Selection — 10 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S136 | `lesson_state_or_occurrence` | Unit summary (unit_state_events_lifetime_and_selection) | a repository to shape | Keep | ordinary verb |
| S137 | `lesson_state_or_occurrence` | CORE / paragraph 4 | The first decides the shape of the model | Replace | "the kind of model" |
| S138 | `lesson_delivery_guarantees` | PRACTICAL / paragraph 7 | Its worked shape is exactly the pending-or-handled pair | Replace | "Its worked example" |
| S139 | `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraph 4 | Now the requirement changes shape | Replace | "Now the requirement changes" |
| S140 | `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraph 5 | a data-layer shape that leaves nothing else to observe | Replace | "a data layer with no observable reads" |
| S141 | `lesson_choosing_the_owner_by_lifetime` | PRACTICAL / paragraph 5 | Change that shape | Replace | "Change that" |
| S142 | `lesson_choosing_the_owner_by_lifetime` | SENIOR / paragraph 3 | describe the shape of the ladder | Remove | delete the framing |
| S143 | `lesson_smallest_sufficient_architecture` | CORE / paragraph 3 | requirements are that shape | Replace | "requirements like these" |
| S144 | `lesson_smallest_sufficient_architecture` | SENIOR / paragraph 2 | a reviewer has one shape to check | Keep | broad structural form of a feature |
| S145 | `lesson_smallest_sufficient_architecture` | SENIOR / paragraph 2 | tooling… can assume the shape | Keep | broad structural form |

</details>

<details>
<summary>25. Dependency Injection as Object Construction — 13 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S146 | `lesson_who_constructs_this_object` | PRACTICAL / paragraph 5 | This shape has a name | Replace | "This pattern has a name (service locator)" |
| S147 | `lesson_a_dependency_should_be_visible` | PRACTICAL / paragraph 6 | Some situations force this shape | Replace | "force field injection" |
| S148 | `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 1 | Shape one: injected, and not inverted | Replace | "Arrangement one" |
| S149 | `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 3 | when the storage changes shape | Replace | "changes its representation" |
| S150 | `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 4 | Shape two: inverted… | Replace | "Arrangement two" |
| S151 | `lesson_injected_inverted_or_both` | PRACTICAL / comparison 2 | Shape one above | Replace | "Arrangement one" |
| S152 | `lesson_injected_inverted_or_both` | PRACTICAL / comparison 2 | Shape two above | Replace | "Arrangement two" |
| S153 | `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 8 | label shape two | Replace | "arrangement two" |
| S154 | `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 8 | shape two sits in the injected and inverted row | Replace | "arrangement two" |
| S155 | `lesson_injected_inverted_or_both` | PRACTICAL / paragraph 8 | What shape two demonstrates | Replace | "arrangement two" |
| S156 | `lesson_one_place_that_knows_how_to_build` | SENIOR / paragraph 2 | the shape of the program is knowable | Replace | "the object graph" |
| S157 | `lesson_asking_for_it_or_being_given_it` | PRACTICAL / bullet list 1 | the service-locator shape | Replace | "the service-locator pattern" |
| S158 | `lesson_when_wiring_it_yourself_is_enough` | PRACTICAL / paragraph 5 | a shape that has changed three times | Replace | "a graph that has changed" |

</details>

<details>
<summary>26. Object Graphs, Lifetimes and Scopes — 13 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S159 | `lesson_from_one_dependency_to_a_graph` | SENIOR / paragraph 3 | but its **shape**: how deep it goes, how many nodes are shared | Keep | defined in place as depth, sharing and change reach — broad graph structure |
| S160 | `lesson_from_one_dependency_to_a_graph` | SENIOR / paragraph 3 | it is almost always the shape that became unreadable | Keep | same defined sense |
| S161 | `lesson_from_one_dependency_to_a_graph` | SENIOR / paragraph 3 | shape is information about the graph | Keep | same defined sense |
| S162 | `lesson_one_instance_or_a_new_one` | SENIOR / callout 1 | refuse the default-shaped answer | Replace | "the default answer" |
| S163 | `lesson_scope_is_a_rule_owner_is_a_lifetime` | PRACTICAL / paragraph 2 | Only the fourth answer is scope-shaped | Replace | "is about scope" |
| S164 | `lesson_runtime_input_is_not_a_dependency` | Lesson summary | lands on the generic shape that routes each kind correctly | Replace | "the generic arrangement" |
| S165 | `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 8 | **The shape that is correct.** | Replace | "The correct arrangement." |
| S166 | `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 9 | reaches the same shape without any library | Replace | "the same arrangement" |
| S167 | `lesson_runtime_input_is_not_a_dependency` | PRACTICAL / paragraph 10 | the split, not the shape | Replace | "not whether it is a class or a function" |
| S168 | `lesson_runtime_input_is_not_a_dependency` | SENIOR / paragraph 2 | the same shape the previous unit's per-use summary builder had | Replace | "the same arrangement" |
| S169 | `lesson_runtime_input_is_not_a_dependency` | SENIOR / paragraph 3 | Recognising the shape is more useful | Replace | "Recognising the pattern" |
| S170 | `lesson_two_dependencies_of_the_same_type` | PRACTICAL / paragraph 7 | states the generic shape plainly | Replace | "the generic model" |
| S171 | `lesson_when_a_broken_graph_tells_you` | PRACTICAL / paragraph 5 | worth reading as a shape rather than as a feature | Replace | "as a model" |

</details>

<details>
<summary>27. Dagger: Compile-Time Object Graphs — 9 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S172 | `lesson_dagger_constructs_what_it_can_see` | PRACTICAL / paragraph 2 | The shape of what the generated code does | Remove | "What the generated code does is…" |
| S173 | `lesson_dagger_constructs_what_it_can_see` | PRACTICAL / callout 2 | not even the right shape of claim | Replace | "the right kind of claim" |
| S174 | `lesson_dagger_constructs_what_it_can_see` | SENIOR / paragraph 2 | the shape of generated output | Replace | "the content of generated output" |
| S175 | `lesson_declaring_the_rest_of_the_graph` | PRACTICAL / paragraph 2 | it is the familiar shape | Replace | "it is a familiar graph node" |
| S176 | `lesson_declaring_the_rest_of_the_graph` | PRACTICAL / callout 1 | where the binding's shape can be expressed by `@Binds` | Replace | "where the binding can be expressed" |
| S177 | `lesson_declaring_the_rest_of_the_graph` | PRACTICAL / paragraph 9 | the documented shape is an interface… | Replace | "the documented layout" |
| S178 | `lesson_which_graph_owns_this_binding` | SENIOR / paragraph 3 | the shape the first unit argued against | Replace | "the service-locator pattern" |
| S179 | `lesson_when_the_type_is_not_the_key` | PRACTICAL / paragraph 5 | The shape to avoid is the obvious one | Replace | "The design to avoid" |
| S180 | `lesson_when_the_type_is_not_the_key` | SENIOR / paragraph 2 | wherever a system is plugin-shaped | Replace | "plugin-based" |

</details>

<details>
<summary>28. Hilt: Android Lifecycle-Aware Dagger — 3 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S181 | `lesson_hilt_viewmodels_and_runtime_input` | CORE / paragraph 2 | so Hilt rejects that shape | Replace | "rejects that request" |
| S182 | `lesson_hilt_or_hand_written_dagger` | SENIOR / paragraph 1 | common Android owner shapes | Replace | "typical Android ownership lifetimes" |
| S183 | `lesson_hilt_or_hand_written_dagger` | SENIOR / paragraph 2 | becomes the dominant shape | Replace | "becomes the rule rather than the exception" |

</details>

<details>
<summary>29. Koin and Dependency Injection in KMP — 3 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S184 | `lesson_koin_definitions_and_reuse` | PRACTICAL / paragraph 4 | The second shape is materially different | Replace | "The second version" |
| S185 | `lesson_koin_definitions_and_reuse` | PRACTICAL / paragraph 4 | the service-locator shape from Unit 1 | Replace | "a service locator" |
| S186 | `lesson_resolving_viewmodels_at_the_boundary` | PRACTICAL / callout 1 | still has the service-locator shape | Replace | "is still a service locator" |

</details>

<details>
<summary>30. Choosing a Dependency Injection Strategy — 2 occurrences</summary>

| Sweep # | Lesson | Location | Occurrence | Decision | Direction |
| --- | --- | --- | --- | --- | --- |
| S187 | `lesson_what_a_container_actually_buys` | PRACTICAL / comparison 2 | Naming, owner structure and entry-point shape remain project decisions | Replace | "entry-point design" |
| S188 | `lesson_the_smallest_sufficient_strategy` | SENIOR / paragraph 2 | can all supply the same consumer shape | Replace | "the same consumer constructor" |

</details>


## Voice and Tone Findings

### Recurring voice patterns

| Pattern | Evidence | Where it concentrates | Direction |
| --- | --- | --- | --- |
| **"worth …" framing** | 247 uses: "is worth being precise about", "the one worth carrying away", "worth stating plainly", "worth having in your hands" | Generations A, C, D and E (6–19 per Unit); almost absent from Units 7–10 and 28–30 | Lead with the fact; keep "worth" only where it is an actual cost–benefit judgement |
| **"honest" as intensifier** | 79 uses: "The honest answer is…", "the honest framing", "counted honestly", "the honest mechanism", "What is not honest…" | Units 4 and 19–27; five in one Lesson (LC-028, LC-164) | Use the technical word — accurate, correct, valid, safe — or delete |
| **Summaries written as learning objectives** | 33 Lesson summaries open with an imperative ("Choose…", "Separate…", "Read…", "Say exactly what runs…") | Units 7–8, 10, 14–16, 28–30 (every summary in Units 28–30) | State the idea: what is true, not what the reader should do |
| **Summaries written as authoring outlines** | 30 Lesson summaries end "This lesson derives…, separates…, and shows…" | Units 19, 21–27 | Delete the outline sentence; the first sentence is usually the real summary |
| **Recap-and-trailer openings and endings** | "The five previous units each began with…"; "That closes the unit… That starts next:"; "This is the last question in the subject" | Units 6, 12, 19–24, 27 | Open on the subject; end on the last technical point |
| **Scope defence** | "deliberately" (101), "is not this lesson's subject", "this unit stops here", "not re-derived here", "named here only so the problem has a visible destination" | All generations; densest in Units 7–9, 12, 13, 19–24 | Delete; state the one fact the learner needs instead |
| **Corrective contrast template** | "is not X. It is Y." (40) and "not X, but Y" | Units 1, 17, 25–27 (four each) | Keep one per Lesson at most; otherwise state the positive claim |
| **"Do not…" callouts** | 11 callouts open with a prohibition addressed to the learner | Units 8–10 (and one each in 11, 30) | Open with the fact the prohibition protects |
| **Author-process narration** | "were run rather than reasoned about"; "That last claim is worth measuring rather than asserting, so it was"; "checked against the 2.11.0-beta01 lifecycle sources… A later dependency update should re-read those contracts" | Units 4–5, 8, 15 | Report the measured result and its toolchain; drop the process and any maintenance instruction |
| **The reader in the third person** | "What the learner gets" (table header), "the reader was doing two jobs by hand", "the mental model learners should lead with" | Units 5–8, 27 | Address the reader as "you" or not at all; use "user" for an app's user |
| **Build vocabulary as voice** | "the resolved Compose runtime", "the configured runtime", "in the configured version" | Units 8–11, 17 | "In Compose runtime 1.11.2, …" — name the version, not the build |
| **Telegraphic definitions** | "Screen composable means wiring boundary. Content composable means rendering plus reporting intent." | Units 7, 14 | One natural sentence |

The common thread is that the author is visible: defending scope, recording verification,
explaining the Unit's design, grading the reader's answer. The technical sentences between
those passages are usually direct and good, which is why most Lessons are Light copy edits.

### Tone

Strong wording is **justified** — and was left alone — where it states a contract: composable
bodies must be idempotent and side-effect free for correctness; `remember` does not survive a
configuration change; cancellation is cooperative; `StateFlow` never completes and conflates
by `equals`; `emit` in `flow {}` must not switch context; a Dagger key must have exactly one
binding; Hilt-injected fields cannot be private; nothing in memory survives process death.
Hedging these would make the Lessons less accurate.

Architecture and DI Lessons are **already conditional** about preferences: the domain layer
is "optional", recommendations are quoted with their conditions ("when possible",
"recommendations and not strict requirements"), and several Lessons argue explicitly against
their own rule being over-applied. This is the most consistently well-judged aspect of the
corpus.

Unjustified prescription is rare and specific:

- a placement guideline stated as an invariant — "What it must not hold is business logic or
  application data" about a plain state holder (LC-012);
- judging the reader rather than advising — "is the answer that sounds rehearsed" (LC-019);
- prohibition-first callouts across two Units (LC-058, LC-066).

## Clarity and Verbosity Findings

### Why prose becomes cryptic

1. **A course coordinate replaces a fact.** "The contract has four parts, and all four come
   from Unit 9's effect model" (LC-076); "the second and third questions of Unit 2"
   (LC-202). The sentence is precise only to someone holding the course map.
2. **Topic-local Unit numbers collide across Topics** (LC-095, LC-202, LC-208, LC-213). The
   same "Unit 1" means three different Units depending on where it is read.
3. **References to material that is not there or not where it says.** "The performance
   unit" and "the lazy-layout unit" do not exist (LC-015, LC-016, LC-023, LC-026);
   "later in this curriculum" points backwards to Compose Lessons (LC-114, LC-127, LC-130).
4. **Senior sections that depend on the previous Lesson's specifics.** "What makes the
   previous lesson's failures impossible", "the observation the previous lesson recorded and
   told you not to design against" (LC-029); "in the vocabulary of the previous Lesson, it has
   no propagation path" (LC-103).
5. **Undefined labels.** "Example B" (defined nowhere, LC-208), "the foundations unit"
   (a nickname, LC-161), "Shape one / Shape two" (LC-190), "bounded bridge" in a title
   (LC-046).
6. **Evidence the learner cannot inspect.** This application's own classes, packages, module
   names and counts cited as proof (LC-153, LC-154, LC-163, LC-167, LC-182, LC-212, LC-217,
   LC-218).
7. **Positional pointers into large tables** — "the sixth and seventh are one countdown…
   the twelfth row is the eleventh" (LC-082); "Version A of the first lesson" in table cells
   (LC-189).
8. **Sentences carried over from another context** — the unexplained practice-builder
   invariant (LC-147) and the collision of two example domains in one sentence (LC-180).

### Why prose becomes long

1. Course narration and scope defence (see LC-META findings).
2. Unit summaries that recap the previous Units and preview every Lesson (the longest is 205
   words, LC-201); summaries in Units 17, 18 and 23–27 exceed 140 words.
3. Recap paragraphs duplicating the interview callout at the end of Lessons (Units 15, 18,
   19, 22, 24).
4. Evidence discussions about this repository ("this is evidence for the boundary and poor
   evidence for the coordination…").
5. Provenance disclaimers before code ("This is common Kotlin code with kotlinx.coroutines
   imports…", LC-096).
6. Dense five-column comparison tables whose cells repeat the prose (LC-222).

### Lessons over ~2 000 words

None of the 17 holds more than one mental model, so none should be split. Scaffolding is an
estimate of the share of prose that is course narration, scope defence, evidence discussion
about this repository, or recap.

| Lesson | Words | One mental model? | Length driven by | Avoidable scaffolding | Materially shorter? |
| --- | ---: | --- | --- | --- | --- |
| **Is This State, or Is It Something That Happened?** | 2 423 | Yes | Requirement table, duplicate-celebration failure, lossy-compression argument | ~10% | Somewhat |
| **When Does Another Layer Earn Its Existence?** | 2 416 | Yes | Three worked features | ~20% (Senior boundaries, codebase evidence) | Yes |
| **How Much Architecture Does This Feature Need?** | 2 401 | Yes | Two features designed and mis-designed | ~10% | Somewhat |
| **Choosing an Owner From the Lifetime the Requirement Needs** | 2 296 | Yes | Four-rung ladder with a priced example per rung | ~15% (incl. this-app evidence) | Somewhat |
| **What Guarantee Does This Occurrence Need?** | 2 292 | Yes | Five guarantee questions and "exactly once" decomposition | ~20% (curriculum hand-off) | Yes |
| **One Screen, Five Questions** | 2 290 | Yes | The five-question instrument and three-team table | ~30% (Unit-design rationale) | Yes, materially |
| **`snapshotFlow` and Crossing Into Flow** | 2 236 | Yes | Measured evaluation/emission behaviour | ~5% | No |
| **What a Repository Is Responsible For** | 2 211 | Yes | Decision table and pass-through counter-case | ~15% | Yes |
| **Choosing the Smallest Sufficient Mechanism** | 2 184 | Yes | Four facts and a 13-row worked table | ~7% | No |
| **Use Cases That Earn Their Place, and Pass-Through Cost** | 2 150 | Yes | Twin types and the convention argument | ~20% (Senior this-app audit) | Yes |
| **Coordinating Local and Remote Sources** | 2 106 | Yes | Read/write paths and the unknown-outcome write | ~8% | Slightly |
| **How Compose Observes State** | 2 093 | Yes | Three measured failures and consistency limits | ~8% | Slightly |
| **Classifying What a Real Codebase Actually Does** | 2 080 | Yes | Three designs and four falsified claims | ~15% | Yes |
| **Which Source Is Authoritative?** | 2 052 | Yes | Per-fact authority table and a concrete conflict | ~10% | Slightly |
| **From One Dependency to a Graph** | 2 035 | Yes | Graph trace and edge-responsibility table | ~8% | Slightly |
| **What a Screen State Holder Is Responsible For** | 2 023 | Yes | Responsibility list and four-value placement table | ~15% | Yes |
| **MVI: Intent, Reduction and One Current State** | 2 006 | Yes | Transition walkthrough and ceremony accounting | ~5% | No |

## Lesson Audit Matrix

Finding IDs include Lesson-specific findings and the Unit-level findings whose location
covers the Lesson. Words are learner-facing prose words.

| # | Unit ID | Unit title | Lesson ID | Lesson title | Words | Finding IDs | Audit result | Rewrite scope |
| --- | --- | --- | --- | --- | ---: | --- | --- | --- |
| 01.1 | `unit_thinking_in_compose` | Thinking in Compose | `lesson_declarative_ui` | Declarative UI and Why Compose Exists | 944 | — | Strong; the one "worth being honest about its limits" framing is acceptable. No edit needed. | None |
| 01.2 | `unit_thinking_in_compose` | Thinking in Compose | `lesson_composable_execution` | What a Composable Is and How It Executes | 1585 | LC-002, LC-003 | Excellent mechanism teaching; remove course narration and thin the "worth"/"not X. It is Y" cadence. Note: the Lesson has two CORE sections (structural, not editorial). | Light copy edit |
| 01.3 | `unit_thinking_in_compose` | Thinking in Compose | `lesson_state_down_events_up` | State Down, Events Up | 1174 | LC-004, LC-005 | Clear; positional references and one ownership sentence to remove; "shape" twice in one sentence. | Light copy edit |
| 02.1 | `unit_state_and_state_ownership` | State and State Ownership | `lesson_observable_state` | Observable State: `mutableStateOf` and `State<T>` | 1720 | LC-007, LC-008 | Technically excellent (read-creates-dependency, counter diagnosis); dense positional narration is the main problem. | Light copy edit |
| 02.2 | `unit_state_and_state_ownership` | State and State Ownership | `lesson_remember_composition_memory` | `remember`: Composition Memory | 1193 | LC-009 | Strong four-combination table; only positional references to replace. | Light copy edit |
| 02.3 | `unit_state_and_state_ownership` | State and State Ownership | `lesson_remember_saveable` | `rememberSaveable` and State That Must Survive | 1539 | LC-010 | Clear survival ladder; one authoring-vocabulary sentence and a code-comment "shape". | Light copy edit |
| 02.4 | `unit_state_and_state_ownership` | State and State Ownership | `lesson_state_hoisting` | State Hoisting and the Lowest Sensible Owner | 1499 | LC-011, LC-012 | Strong decision model; remove scope narration and qualify one placement rule. | Light copy edit |
| 02.5 | `unit_state_and_state_ownership` | State and State Ownership | `lesson_observable_collections` | Collections and Observable Mutation | 1057 | LC-013 | Strong; one course-navigation paragraph ending. | Light copy edit |
| 03.1 | `unit_recomposition` | Recomposition | `lesson_composition_and_recomposition` | Composition and Recomposition | 1558 | LC-015 | Excellent write/invalidate/execute/frame walkthrough; course-coordinate opening and a dangling pointer. | Light copy edit |
| 03.2 | `unit_recomposition` | Recomposition | `lesson_recomposition_scopes` | Recomposition Scopes and Selective Execution | 1422 | LC-016, LC-017 | Excellent scope explanation (inline Column trap); positional references and a dangling pointer. | Light copy edit |
| 03.3 | `unit_recomposition` | Recomposition | `lesson_recomposition_cost` | Recomposition Is Not the Problem | 1019 | LC-018, LC-019 | Sound argument; the summary and connective paragraphs describe the Lesson's role in the course. | Light copy edit |
| 04.1 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | `lesson_composable_identity` | Composable Identity | 1440 | LC-021, LC-022 | Strong call-site model; Unit-number coordinates and author-verification narration. | Light copy edit |
| 04.2 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | `lesson_keys_and_identity_in_lists` | key and Keys in Lazy Lists | 1514 | LC-023, LC-024 | Strong three-"key" comparison; a dangling lazy-layout pointer and "questions of this unit". | Light copy edit |
| 04.3 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | `lesson_immutability_vs_stability` | Immutability in Kotlin vs. What Compose Needs | 1451 | LC-025 | Strong reference-vs-object distinction; self-description only. | Light copy edit |
| 04.4 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | `lesson_stability_and_skipping` | Stability and Skipping | 1728 | LC-026, LC-027 | Technically careful; opaque summary, repository-build framing, ledger voice and a dangling pointer. | Light copy edit |
| 04.5 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | `lesson_stability_annotations` | @Stable and @Immutable as Contracts | 1687 | LC-028, LC-029 | Correct contract framing; "honest" tic and Senior sentences that depend on the previous Lesson. | Light copy edit |
| 05.1 | `unit_derived_state_and_expensive_work` | Derived State and Expensive Work | `lesson_remember_key_memoization` | `remember(key)` as Memoization | 1524 | LC-031, LC-032 | Excellent dependency-list framing and failure table; strip Unit numbers and the verification narration. | Light copy edit |
| 05.2 | `unit_derived_state_and_expensive_work` | Derived State and Expensive Work | `lesson_derived_state` | `derivedStateOf` | 1856 | LC-033 | One of the strongest Lessons in the corpus (filters work vs notifications, pull-based recalculation); two positional sentences only. | Light copy edit |
| 05.3 | `unit_derived_state_and_expensive_work` | Derived State and Expensive Work | `lesson_work_outside_composition` | Keeping Work Out of Composition | 1506 | LC-034 | Sound placement argument; the densest self-positioning in the Unit. | Light copy edit |
| 06.1 | `unit_snapshot_fundamentals` | Snapshot Fundamentals | `lesson_snapshot_observation` | How Compose Observes State | 2093 | LC-036, LC-037 | Unifying mechanism explained with measured, three-way diagnosis; recap-by-Unit framing to remove. | Light copy edit |
| 06.2 | `unit_snapshot_fundamentals` | Snapshot Fundamentals | `lesson_snapshot_flow` | `snapshotFlow` and Crossing Into Flow | 2236 | LC-038 | Excellent (read inside the block, `toList()`, conflation); one authoring-vocabulary bridge paragraph. | Light copy edit |
| 07.1 | `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow | `lesson_classes_of_screen_state` | Three Classes of State on One Screen | 942 | LC-040, LC-041 | Concrete three-owner table is good; remove the ID leak, spec-voice summary and rubric callout. | Light copy edit |
| 07.2 | `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow | `lesson_stateless_screen_content` | The Stateless Content Boundary | 581 | LC-042, LC-043 | Sound screen/content split; scope disclaimers (including in code) and clipped definitions. | Light copy edit |
| 07.3 | `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow | `lesson_screen_state_and_ui_events` | One Screen State Value, Events Back Up | 760 | LC-044, LC-045 | Short Lesson in which scope disclaimers and an ID leak take a large share of the prose; needs re-voicing, not new content. | Substantive editorial rewrite |
| 07.4 | `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow | `lesson_screen_state_owner_boundary` | The Screen-Level Owner as a Bounded Bridge | 927 | LC-046, LC-047, LC-048 | Authoring vocabulary in the title, an ID leak, app-specific navigation description and a curriculum-stop ending. | Substantive editorial rewrite |
| 08.1 | `unit_observable_state_collection` | Observable State Collection and Lifecycle | `lesson_external_state_in_compose` | What a Composable Can and Cannot Observe | 573 | LC-050, LC-051, LC-058 | Sharp two-observation-systems explanation; remove three ID leaks and Unit-7 coordinates. | Light copy edit |
| 08.2 | `unit_observable_state_collection` | Observable State Collection and Lifecycle | `lesson_collect_as_state` | `collectAsState`: Converting a Stream into Compose State | 737 | LC-052, LC-053, LC-054, LC-058 | Correct and useful, but leaks (two IDs, a backlog key), objective-style summary, ownership sentences and author-facing Senior notes need re-voicing across all sections. | Substantive editorial rewrite |
| 08.3 | `unit_observable_state_collection` | Observable State Collection and Lifecycle | `lesson_collection_lifetime_and_cost` | Collection Has a Lifetime and a Cost | 779 | LC-055, LC-058 | Good cost and cold/shared distinction; three ID leaks to replace with the local facts already present. | Light copy edit |
| 08.4 | `unit_observable_state_collection` | Observable State Collection and Lifecycle | `lesson_lifecycle_aware_collection` | Lifecycle-Aware Collection and the Lifecycle a Screen Actually Has | 855 | LC-056, LC-057, LC-058 | Valuable host-mapping table; summary is project-coupled and the Senior section is written as an author's verification log. | Substantive editorial rewrite |
| 09.1 | `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and LaunchedEffect | `lesson_why_effects_are_controlled` | Why Compose Needs an Effect API | 803 | LC-059, LC-060, LC-066 | Clear three-failure framing; an ID-leak paragraph opening and scope narration. | Light copy edit |
| 09.2 | `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and LaunchedEffect | `lesson_launched_effect` | `LaunchedEffect`: Work a Composition Owns | 656 | LC-061, LC-062, LC-066 | Precise five-moment lifecycle; three ID leaks and a scope sentence. | Light copy edit |
| 09.3 | `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and LaunchedEffect | `lesson_effect_keys_as_dependencies` | What an Effect's Keys Declare | 715 | LC-063, LC-064, LC-066 | Good keys-as-lifetime model and equality contrast; two ID leaks. | Light copy edit |
| 09.4 | `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and LaunchedEffect | `lesson_effect_key_failures` | Two Ways to Get Effect Keys Wrong | 702 | LC-065, LC-066 | Strong two-direction diagnosis; scope narration at the end. | Light copy edit |
| 10.1 | `unit_latest_values_and_event_driven_work` | Latest-Value Effects and Event-Driven Coroutine Work | `lesson_remember_updated_state` | Reading the Current Value Without Restarting | 670 | LC-067, LC-068 | Precise read-time explanation; Unit references and "resolved" vocabulary. | Light copy edit |
| 10.2 | `unit_latest_values_and_event_driven_work` | Latest-Value Effects and Event-Driven Coroutine Work | `lesson_remember_coroutine_scope` | `rememberCoroutineScope`: Launching From an Event | 545 | LC-069, LC-070 | Clear trigger/lifetime split; two ID leaks. | Light copy edit |
| 10.3 | `unit_latest_values_and_event_driven_work` | Latest-Value Effects and Event-Driven Coroutine Work | `lesson_who_owns_the_trigger` | Composition-Driven or Event-Driven? | 588 | LC-071 | Good synthesis; scope disclaimers. | Light copy edit |
| 11.1 | `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers | `lesson_disposable_effect` | Registration and Release as One Decision | 1348 | LC-072, LC-073, LC-074 | Excellent concrete leak example and lifecycle table; Unit-number naming, two ID leaks, two "shape". | Light copy edit |
| 11.2 | `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers | `lesson_side_effect_publication` | `SideEffect`: Publishing to Non-Compose Code | 931 | LC-075, LC-079 | Precise timing contract; Unit numbers and one dangling sentence. | Light copy edit |
| 11.3 | `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers | `lesson_produce_state` | `produceState`: a Composition-Scoped Producer | 1002 | LC-076 | Valuable non-obvious finding (holder is unkeyed); Unit numbers stand in for mechanism names. | Light copy edit |
| 11.4 | `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers | `lesson_flow_adapter_or_compose_producer` | A Flow Below the UI, or a Producer at the Boundary? | 975 | LC-077, LC-078 | Strong placement table; one ID leak and positional references. | Light copy edit |
| 12.1 | `unit_production_ui_effects_and_selection` | Production UI Effects and Mechanism Selection | `lesson_choosing_a_compose_mechanism` | Choosing the Smallest Sufficient Mechanism | 2184 | LC-081, LC-082 | Over 2,000 words but one coherent decision model; length is the worked table and three worked screens. About 150 words of course narration can go. | Light copy edit |
| 12.2 | `unit_production_ui_effects_and_selection` | Production UI Effects and Mechanism Selection | `lesson_transient_ui_effects` | Rendering State and Running a Transient Effect | 1325 | LC-083 | Clear condition-vs-occurrence argument; positional and ownership narration to remove. | Light copy edit |
| 12.3 | `unit_production_ui_effects_and_selection` | Production UI Effects and Mechanism Selection | `lesson_transient_effect_delivery` | What Delivery Guarantee Does This Occurrence Need? | 1546 | LC-084, LC-085 | Correct reasoning, but a large share of the Senior section justifies the Lesson's existence and assigns work to other curricula; key facts are cited to other Lessons rather than stated. | Substantive editorial rewrite |
| 13.1 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | `lesson_suspension_and_blocking` | Suspension Is Not Blocking | 1543 | LC-087, LC-088 | Model Lesson for voice and precision; replace positional pointers and one "shape". | Light copy edit |
| 13.2 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | `lesson_coroutine_builders` | Starting Coroutines: `launch`, `async` and `runBlocking` | 1428 | LC-089, LC-090 | Clear intent-based builder choice; scope narration including a code comment. | Light copy edit |
| 13.3 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | `lesson_job_and_parent_child` | `Job`: the Handle That Carries Lifetime | 1336 | LC-091 | Strong completing-state demonstration; three over-long "not taught here" passages to compress. | Light copy edit |
| 13.4 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | `lesson_coroutine_scope_ownership` | `CoroutineScope` and Who Owns a Coroutine's Lifetime | 1609 | LC-092, LC-093 | Excellent ownership framing and failure list; "shape" habit. | Light copy edit |
| 13.5 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | `lesson_structured_concurrency` | Structured Concurrency and What It Guarantees | 1603 | LC-094 | Strong guarantee/non-guarantee table; positional pointers. | Light copy edit |
| 14.1 | `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work | `lesson_coroutine_context` | `CoroutineContext` and What Children Inherit | 1119 | LC-095, LC-096, LC-097 | Precise slot/key model and Job-parent distinction; seven cross-topic-ambiguous Unit numbers. | Light copy edit |
| 14.2 | `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work | `lesson_dispatchers` | Dispatchers and Where Code Actually Runs | 1287 | LC-095, LC-096, LC-097, LC-098, LC-099 | Accurate multi-target dispatcher table; references to non-existent curricula and provenance disclaimers. | Light copy edit |
| 14.3 | `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work | `lesson_with_context_and_main_safety` | `withContext` and Main-Safety | 1269 | LC-095, LC-096, LC-097, LC-100 | Sound ownership-of-the-switch argument; scope defences. | Light copy edit |
| 14.4 | `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work | `lesson_sequential_and_concurrent_work` | Sequential by Default, Concurrent on Purpose | 1219 | LC-095, LC-096, LC-097, LC-101 | Good immediate-await trap and cost reasoning; disclaimers and a Unit-3 pointer. | Light copy edit |
| 15.1 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | `lesson_cooperative_cancellation` | Cancellation Is Cooperative | 1136 | LC-103, LC-104, LC-108 | Strong request-vs-stop model and broad-catch trap; "honest answer" framing and ordinals. | Light copy edit |
| 15.2 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | `lesson_cancellation_cleanup_and_timeouts` | Cleanup, `NonCancellable` and Timeouts | 1267 | LC-103, LC-104, LC-105, LC-107, LC-108 | Excellent measured timeout table and prompt-cancellation detail; "cleanup-shaped" and curriculum framing. | Light copy edit |
| 15.3 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | `lesson_exception_propagation` | How a Coroutine Failure Travels | 1220 | LC-103, LC-104, LC-106, LC-108 | Clear propagation vs observation; ordinals and one curriculum-ownership sentence. | Light copy edit |
| 15.4 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | `lesson_supervision_and_failure_isolation` | `SupervisorJob`, `supervisorScope` and the Limits of Isolation | 1130 | LC-103, LC-108 | Precise one-boundary model; ordinals, one of which carries a definition. | Light copy edit |
| 15.5 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | `lesson_shared_state_and_coordination` | Shared Mutable State and Choosing a Coordination Mechanism | 1352 | LC-103, LC-104, LC-108 | Good atomic/confinement/Mutex decision; "shape of the state" is legitimate here. Only Unit-wide consistency edits apply (one "honest", one "worth", one "later in this curriculum" pointer). | None |
| 16.1 | `unit_flow_fundamentals` | Flow Fundamentals | `lesson_why_flow` | One Value or Many: Why `Flow` Exists | 1347 | LC-109, LC-111, LC-112 | Clear one-vs-many argument with the cost made explicit; "shape" seven times and ownership narration. | Light copy edit |
| 16.2 | `unit_flow_fundamentals` | Flow Fundamentals | `lesson_cold_flows` | Cold Flows: Producer, Collector and Operators | 1229 | LC-109, LC-113 | Excellent recipe model, measured, with a sharp terminal-operator rule; curriculum pointers. | Light copy edit |
| 16.3 | `unit_flow_fundamentals` | Flow Fundamentals | `lesson_flow_collection_lifetime` | Collection Lifetime and Flow Cancellation | 1257 | LC-109, LC-114 | Strong ownership framing and measured non-cooperative producer; an inaccurate ordering sentence and misdirected "later" pointer. | Light copy edit |
| 16.4 | `unit_flow_fundamentals` | Flow Fundamentals | `lesson_flow_context_and_flow_on` | Context Preservation and `flowOn` | 1171 | LC-109, LC-115 | Model explanation of context preservation with the real runtime error; Unit numbers. | Light copy edit |
| 16.5 | `unit_flow_fundamentals` | Flow Fundamentals | `lesson_flow_builders_and_callback_adapters` | Flow Builders and Adapting Callback APIs | 1396 | LC-109, LC-116, LC-117 | Strong adapter walkthrough; "producer shape" habit and pointers. | Light copy edit |
| 17.1 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | `lesson_transforming_and_filtering_flows` | Transforming and Filtering: by Value and by Time | 1245 | LC-119 | Excellent value-vs-time framing with a shared debounce/sample timeline; one positional opening. | Light copy edit |
| 17.2 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | `lesson_combining_flows` | `combine` and `zip`: Current Values or Paired Emissions | 1263 | LC-120 | Clear combine/zip/merge contracts and a measured batching caveat; build-coupled phrasing and pointers. | Light copy edit |
| 17.3 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | `lesson_flattening_flows` | Flattening: Should New Input Cancel Old Work? | 1292 | LC-121 | Strong three-axis table and abandon-safety test; minor voice edits. | Light copy edit |
| 17.4 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | `lesson_flow_buffering_and_conflation` | When the Collector Cannot Keep Up | 1279 | LC-122 | Model Lesson: one measured example across four strategies; positional pointers only. | Light copy edit |
| 17.5 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | `lesson_flow_failure_and_completion` | `catch`, `retry` and `onCompletion` | 1228 | LC-123 | Precise position-based failure model and onCompletion defect; positional pointers. | Light copy edit |
| 18.1 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | `lesson_hot_and_cold_streams` | Hot and Cold: When Production Happens | 1298 | LC-125, LC-126 | Excellent three-axis separation with measurements; a navigation column in the table and scope narration. | Light copy edit |
| 18.2 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | `lesson_state_flow` | `StateFlow`: One Current Value | 1294 | LC-127 | Precise equality-conflation and `update` treatment; scope narration and a misdirected "later material". | Light copy edit |
| 18.3 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | `lesson_shared_flow` | `SharedFlow`: Replay, Buffering and Subscribers | 1668 | LC-128 | Best explanation of replay vs extra buffer in the corpus; minor pointers. | Light copy edit |
| 18.4 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | `lesson_sharing_cold_flows` | Sharing Cold Flows with `stateIn` and `shareIn` | 1599 | LC-129 | Strong scope-vs-policy split and measured WhileSubscribed clocks; positional pointers only. | Light copy edit |
| 18.5 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | `lesson_choosing_a_stream_abstraction` | Choosing a Stream Abstraction by Delivery Guarantees | 1453 | LC-130, LC-131 | Sound decision procedure and channel-loss measurement; finale framing, Exclude narration and a recap paragraph. | Light copy edit |
| 19.1 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | `lesson_what_architecture_decides` | What Architecture Actually Decides | 1587 | LC-133 | Excellent requirement-change framing; references to non-existent curricula. | Light copy edit |
| 19.2 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | `lesson_responsibility_and_change` | Responsibility, Cohesion and What Changes Together | 1574 | LC-134, LC-135 | Strong people-based SRP and the test for splitting; one ID leak and an authoring-rationale paragraph. | Light copy edit |
| 19.3 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | `lesson_dependency_direction_and_boundaries` | Which Way May This Dependency Point? | 1665 | LC-136, LC-137 | Model separation of source dependency, control and data; dense scope pointers. | Light copy edit |
| 19.4 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | `lesson_when_an_interface_is_a_boundary` | When an Interface Is a Boundary, and When It Is Only Indirection | 1590 | LC-138, LC-139 | Sound consumer-owned-contract test and cost list; scope explanations. | Light copy edit |
| 19.5 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | `lesson_layers_and_their_cost` | Layers as One Answer, and What They Cost | 1765 | LC-140, LC-141 | Convincing same-layer-worthless-then-valuable example; a Unit-recap-and-trailer ending to remove. | Light copy edit |
| 20.1 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | `lesson_state_holder_responsibility` | What a Screen State Holder Is Responsible For | 2023 | LC-143, LC-144 | Strong responsibility-before-class argument, but the opening two paragraphs, a table cell and the Senior section are framed as curriculum hand-offs; the example list reports this application's own holders. | Substantive editorial rewrite |
| 20.2 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | `lesson_viewmodel_lifetime_and_persistence` | The ViewModel Owner: Lifetime Is Not Persistence | 1592 | LC-145 | Excellent owner-chain walk and multiplatform host table; curriculum clauses. | Light copy edit |
| 20.3 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | `lesson_modelling_ui_state` | Modelling the Current UI State | 1805 | LC-146, LC-147 | Excellent data-class-vs-sealed trade-off table and region nesting; one unexplained invariant sentence and app-specific example types. | Light copy edit |
| 20.4 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | `lesson_state_out_intentions_in` | State Out, Intentions In | 1442 | LC-148, LC-149 | Strong intention-vs-setter contrast and the form case; "shape" habit and scope narration. | Light copy edit |
| 20.5 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | `lesson_owner_scoped_work` | Work Whose Lifetime Is the Owner's | 1480 | LC-150 | Correct owner test and `viewModelScope` facts; defensive scope narration and a closing curriculum-partition paragraph. | Light copy edit |
| 21.1 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | `lesson_what_a_repository_owns` | What a Repository Is Responsible For | 2211 | LC-152, LC-153 | Over 2,000 words, one coherent model (repository = owned decisions). Length is partly reasoning and partly avoidable: two Senior paragraphs of evidence discussion about this application, repeated "foundations unit" framing and ownership sentences — roughly 15% could go. | Substantive editorial rewrite |
| 21.2 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | `lesson_coordinating_sources` | Coordinating Local and Remote Sources | 2106 | LC-152, LC-154 | Over 2,000 words; one coherent model (four decisions) with genuine reasoning (the unknown-outcome write). Remove the reader's-application sentences and curriculum partition; modest shortening. | Light copy edit |
| 21.3 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | `lesson_single_source_of_truth` | Which Source Is Authoritative? | 2052 | LC-152, LC-155 | Over 2,000 words; strong per-fact definition and three-fact table. Cut the evidence-honesty Senior paragraph and positional references. | Light copy edit |
| 21.4 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | `lesson_observable_or_one_shot_api` | An Observable API, or a One-Shot Read? | 1542 | LC-152, LC-156, LC-157 | Sound consumer-driven argument; "shape" thirteen times and an app-specific counterexample. | Light copy edit |
| 21.5 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | `lesson_model_and_error_boundaries` | Model and Error Boundaries: What May Cross | 1914 | LC-152, LC-158, LC-159 | Good mapping-cost analysis and error-vocabulary rule; "shape" competing with "representation", including in code. | Light copy edit |
| 22.1 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | `lesson_when_a_domain_layer_earns_its_place` | When Does Another Layer Earn Its Existence? | 2416 | LC-161, LC-162 | Over 2,000 words (the second-longest Lesson). One coherent model; the three worked features are justified length, but about a fifth is scaffolding (curriculum references, codebase evidence, scope disclaimers) and can go without losing understanding. | Light copy edit |
| 22.2 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | `lesson_use_cases_and_pass_through_cost` | Use Cases That Earn Their Place, and Pass-Through Cost | 2150 | LC-161, LC-163, LC-164, LC-169 | Over 2,000 words. Core and Practical are strong; the Senior section is an audit of this application's classes and needs replacing, and the "honest" framing recurs throughout. | Substantive editorial rewrite |
| 22.3 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | `lesson_policy_and_framework_detail` | Policy, Framework and Detail | 1972 | LC-161, LC-165, LC-166 | Strong signature-vocabulary diagnosis; a backlog "epic" reference, a KDoc-quoted this-app example and curriculum narration. | Light copy edit |
| 22.4 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | `lesson_dependency_inversion_in_practice` | Who Defines the Abstraction? | 1858 | LC-161, LC-167 | Model A/B import trace and inversion-vs-injection table; opening map of earlier Lessons and a this-app package audit. | Light copy edit |
| 22.5 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | `lesson_clean_architecture_intent` | Clean Architecture: the Dependency Rule, Not the Diagram | 1804 | LC-161, LC-168 | Correct rule-not-diagram argument with two compliant designs; ordering justification and recap/trailer ending. | Light copy edit |
| 23.1 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | `lesson_one_screen_five_questions` | One Screen, Five Questions | 2290 | LC-171, LC-175, LC-177 | Over 2,000 words for one coherent instrument; roughly a third is Unit-design rationale and course narration that a direct reader does not need. Materially shorter without losing understanding. | Substantive editorial rewrite |
| 23.2 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | `lesson_mvp_view_contract` | MVP: an Explicit View Contract | 1954 | LC-172, LC-175, LC-177 | Excellent: attach/detach derived from the dependency, enumerability vs consistency. Remove fairness and scope narration. | Light copy edit |
| 23.3 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | `lesson_mvvm_observed_state` | MVVM: a UI That Observes State | 1844 | LC-173, LC-177 | Strong corrections (class name, folder, observable count); positional framing ("you have already built it"). | Light copy edit |
| 23.4 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | `lesson_mvi_intent_and_reduction` | MVI: Intent, Reduction and One Current State | 2006 | LC-174, LC-175, LC-177 | Over 2,000 words; one coherent model (input as value, explicit transition) with honest ceremony accounting. Positional narration and "honest" framing only. | Light copy edit |
| 23.5 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | `lesson_classifying_a_real_architecture` | Classifying What a Real Codebase Actually Does | 2080 | LC-176, LC-177 | Over 2,000 words. Strong three-design classification and falsification of four claims; Senior section audits this application's classes and frames the Lesson as the Unit's payoff. | Substantive editorial rewrite |
| 24.1 | `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection | `lesson_state_or_occurrence` | Is This State, or Is It Something That Happened? | 2423 | LC-179, LC-180 | Longest Lesson (≈2,400 words). One coherent model; length is mostly justified by the worked requirement table, the duplicate-celebration failure and the lossy-compression argument. Course narration and pointers are the avoidable part. | Light copy edit |
| 24.2 | `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection | `lesson_delivery_guarantees` | What Guarantee Does This Occurrence Need? | 2292 | LC-181 | Over 2,000 words. Strong guarantee questions and "exactly once" decomposition, but summary, opening and Senior close are framed as a curriculum hand-off and should be re-voiced; overlaps the Compose Lesson with a near-identical title. | Substantive editorial rewrite |
| 24.3 | `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection | `lesson_choosing_the_owner_by_lifetime` | Choosing an Owner From the Lifetime the Requirement Needs | 2296 | LC-182, LC-183 | Over 2,000 words. The lifetime ladder and the "wrong in both directions" insight are excellent; the load-bearing example for the application rung is this application's own verified design and must be re-made as a hypothetical. | Substantive editorial rewrite |
| 24.4 | `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection | `lesson_smallest_sufficient_architecture` | How Much Architecture Does This Feature Need? | 2401 | LC-184 | Over 2,000 words; the two-feature comparison and mispricing exercise justify most of it. Capstone references to other Units by nickname should become the decisions themselves. | Light copy edit |
| 25.1 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | `lesson_who_constructs_this_object` | Who Constructs This Object? | 1411 | LC-186, LC-187 | Model opening: construct/fetch/receive with a precise comparison table; course narration only. | Light copy edit |
| 25.2 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | `lesson_a_dependency_should_be_visible` | A Dependency Should Be Visible | 1715 | LC-186, LC-188 | Four constructor guarantees and the nine-parameter reading are excellent; pointers and "honest". | Light copy edit |
| 25.3 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | `lesson_injected_inverted_or_both` | Injected, Inverted, or Both? | 1396 | LC-186, LC-189, LC-190 | Clear two-axis proof; self-quotation of the curriculum, ordinal table cells and "Shape one/two" labels. | Light copy edit |
| 25.4 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | `lesson_one_place_that_knows_how_to_build` | One Place That Knows How to Build | 1558 | LC-186, LC-191 | Strong composition-root-as-responsibility explanation; positional pointers. | Light copy edit |
| 25.5 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | `lesson_asking_for_it_or_being_given_it` | Asking For It, or Being Given It | 1509 | LC-186, LC-192 | Excellent locator-vs-integration distinction, currently justified by curriculum consistency; re-justify on engineering grounds. | Light copy edit |
| 25.6 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | `lesson_when_wiring_it_yourself_is_enough` | When Wiring It Yourself Is the Right Answer | 1752 | LC-186, LC-193 | Strong clerical-cost analysis and conditioned reading of the Android recommendation; pointers and "honest". | Light copy edit |
| 26.1 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | `lesson_from_one_dependency_to_a_graph` | From One Dependency to a Graph | 2035 | LC-195, LC-196 | Over 2,000 words; one coherent model with a precise edge-responsibility table. Length is mainly the graph trace; course narration is the avoidable part. | Light copy edit |
| 26.2 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | `lesson_one_instance_or_a_new_one` | One Instance, or a New One Each Time? | 1910 | LC-195, LC-197 | Strong requirement-first reuse decision with both failure directions; minor voice edits. | Light copy edit |
| 26.3 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | `lesson_scope_is_a_rule_owner_is_a_lifetime` | A Scope Is a Rule; an Owner Is a Lifetime | 1935 | LC-195, LC-198 | Among the clearest Lessons in the corpus (requirement → owner → rule; one rule, two owners); curriculum references only. | Light copy edit |
| 26.4 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | `lesson_runtime_input_is_not_a_dependency` | A Value the Graph Cannot Know | 1774 | LC-195, LC-199 | Good origin-not-type test and factory seam; "shape" habit. | Light copy edit |
| 26.5 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | `lesson_two_dependencies_of_the_same_type` | Two Dependencies of the Same Type | 1648 | LC-195 | Sharp design-first treatment of same-type collisions. Only Unit-wide consistency edits apply (summary pattern, one architecture-curriculum clause). | None |
| 26.6 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | `lesson_when_a_broken_graph_tells_you` | When Does a Broken Graph Tell You? | 1674 | LC-195, LC-200 | Corrects a common myth well; one paragraph states the curriculum's own editorial standard. | Light copy edit |
| 27.1 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_dagger_constructs_what_it_can_see` | What Dagger Can Construct on Its Own | 1599 | LC-202, LC-203, LC-204, LC-205, LC-206 | Accurate, restrained treatment of `@Inject` and "no reflection ≠ no cost"; dense Unit-number coordinates and third-person "the reader". | Light copy edit |
| 27.2 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_declaring_the_rest_of_the_graph` | Declaring the Rest of the Graph | 1734 | LC-204, LC-205, LC-206 | Expressiveness-based `@Provides`/`@Binds` rule and "a module owns nothing" are excellent; minor "shape" edits. | Light copy edit |
| 27.3 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_which_graph_owns_this_binding` | Which Graph Owns This Binding? | 1421 | LC-205, LC-206 | Clear component-as-graph reading and reachability list; positional references. | Light copy edit |
| 27.4 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_child_graph_or_separate_graph` | A Child Graph, or a Separate Graph? | 1248 | LC-203, LC-204, LC-206 | Visibility framing of subcomponent vs dependency is exemplary. Only Unit-wide consistency edits apply (summary pattern, one "honest", one scope sentence, one pointer). | None |
| 27.5 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_dagger_scopes_and_component_instances` | A Scope Is a Promise the Component Keeps | 1418 | LC-203, LC-206 | The instance-counting exercise is one of the corpus's best; positional references only. | Light copy edit |
| 27.6 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_when_the_type_is_not_the_key` | When the Type Is Not the Key | 1252 | LC-203, LC-205, LC-206 | Qualifier-as-key and multibinding-as-inversion are well argued; "shape" and pointers. | Light copy edit |
| 27.7 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | `lesson_what_the_dagger_compiler_checked` | What the Dagger Compiler Actually Checked | 1555 | LC-203, LC-204, LC-206 | Precise guarantee/limit framing and a compiles-but-wrong example; Unit-purpose closing and positional references. | Light copy edit |
| 28.1 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | `lesson_hilt_is_dagger_with_decisions_made` | Hilt Is Dagger With the Decisions Already Made | 645 | LC-207, LC-208 | Clear Dagger-vs-Hilt layering; objective-style summary and Unit numbers. | Light copy edit |
| 28.2 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | `lesson_which_android_component_owns_this` | Which Android Component Owns This? | 685 | LC-207, LC-208 | Precise three-owner comparison for `ReaderSession`; undefined "Example B" and Unit numbers. | Light copy edit |
| 28.3 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | `lesson_when_android_owns_construction` | When Android Owns Construction | 634 | LC-207, LC-208 | Construction-ownership explanation of field injection and entry points is exact; Unit numbers. | Light copy edit |
| 28.4 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | `lesson_hilt_viewmodels_and_runtime_input` | ViewModels, Their Component, and the Values That Arrive Late | 683 | LC-207, LC-208, LC-209, LC-210 | Four-decision separation is excellent; scope narration. | Light copy edit |
| 28.5 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | `lesson_which_graph_does_this_binding_join` | Which Graph Does This Binding Join? | 457 | LC-207, LC-208 | Clean visibility-vs-identity controlled comparison. Only Unit-wide consistency edits apply (objective-style summary, one Unit number). | None |
| 28.6 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | `lesson_hilt_or_hand_written_dagger` | Hilt, or Components You Write Yourself? | 716 | LC-207, LC-210 | Balanced two-scenario decision. Only Unit-wide consistency edits apply (objective-style summary, two "shape"). | None |
| 29.1 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | `lesson_the_koin_container_and_its_modules` | The Container, and the Modules That Fill It | 709 | LC-211, LC-212, LC-213 | Correct composition-root/container/module separation; example is this application's startup by name. | Light copy edit |
| 29.2 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | `lesson_koin_definitions_and_reuse` | Definitions, and the Reuse Requirement Behind Them | 697 | LC-211, LC-213, LC-214, LC-215 | Clear reuse-vs-binding axes and constructor-first rule; this-app state holders and "shape". | Light copy edit |
| 29.3 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | `lesson_koin_scopes_and_their_owners` | Scopes, and the Owner That Has to Stay Alive | 587 | LC-211, LC-213, LC-216 | Good owner-first scope reasoning; one dated case-study paragraph. | Light copy edit |
| 29.4 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | `lesson_resolving_viewmodels_at_the_boundary` | Resolving a ViewModel at the Boundary | 611 | LC-211, LC-213, LC-217 | Construction-vs-store-ownership and route-parameter split are well taught; the Senior section is a repository audit with counts, a maintenance note and "the shipped lifetime curriculum". | Substantive editorial rewrite |
| 29.5 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | `lesson_one_graph_across_platforms` | One Graph, Several Platforms | 727 | LC-211, LC-218 | Sound shared-vs-host split; the whole Lesson is framed as "the case study" of this repository, including "repository evidence" of target support. | Substantive editorial rewrite |
| 30.1 | `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy | `lesson_what_a_container_actually_buys` | What a Container Actually Buys | 1504 | LC-219, LC-222 | Accurate capability-vs-automation framing; very dense comparison tables. | Light copy edit |
| 30.2 | `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy | `lesson_when_should_a_graph_error_surface` | When Should a Graph Error Surface? | 1003 | LC-219, LC-220, LC-221 | Precise mechanism-qualified detection table; a Question-ID leak with a backlog note to delete. | Light copy edit |
| 30.3 | `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy | `lesson_three_projects_three_answers` | Three Projects, Three Answers | 1293 | LC-219, LC-223 | Clear three-project decision traces with revisit triggers; one coverage disclaimer. | Light copy edit |
| 30.4 | `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy | `lesson_the_smallest_sufficient_strategy` | The Smallest Sufficient Strategy | 1096 | LC-219 | Complete no-container design and observable triggers. Only Unit-wide consistency edits apply (objective-style summary, one "shape"). | None |

## Unit Audit Summary

At audit time, every Unit's status was **Reviewed** (rewrite not started). The table
records that baseline. Current remediation status: Units 7 through 11, 23, 24, 29 and 30 are **Re-verified** (see
[Remediation Log](#remediation-log)); every other Unit remains **Reviewed**.

| # | Unit ID | Human title | Lessons | Reviewed | None / Light / Substantive | Dominant categories | Overall rewrite scope | Explanation |
| ---: | --- | --- | ---: | ---: | --- | --- | --- | --- |
| 1 | `unit_thinking_in_compose` | Thinking in Compose | 3 | 3 | 1 / 2 / 0 | LC-META, LC-XREF | Light copy edit | Early-generation Compose writing: natural engineer voice, strong examples; issues are "later in this path" narration and a few framing tics. |
| 2 | `unit_state_and_state_ownership` | State and State Ownership | 5 | 5 | 0 / 5 / 0 | LC-XREF, LC-META | Light copy edit | Same early Compose generation as Unit 1: natural voice and concrete bugs, but each Lesson carries "previous/next unit" narration and two Lessons leak "remains later curriculum". |
| 3 | `unit_recomposition` | Recomposition | 3 | 3 | 0 / 3 / 0 | LC-XREF, LC-META | Light copy edit | Technically the strongest Compose Unit so far; course narration and two references to a non-existent "performance unit". |
| 4 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability | 5 | 5 | 0 / 5 / 0 | LC-XREF, LC-CLARITY, LC-VOICE | Light copy edit | Most internally cross-referenced Compose Unit ("the three questions of this unit", Unit 2, previous lesson); introduces the experiment-report voice ("was run", "measured here") and the "honest" tic that recur later. |
| 5 | `unit_derived_state_and_expensive_work` | Derived State and Expensive Work | 3 | 3 | 0 / 3 / 0 | LC-XREF, LC-META | Light copy edit | Highest density of explicit Unit numbers in the corpus ("Unit 2", "Unit 3", "Unit 1"), almost always followed by a local restatement, so removal is mechanical. Measurement-backed prose ("Measured on a JVM target") is accurate and attributed per Rule 11. |
| 6 | `unit_snapshot_fundamentals` | Snapshot Fundamentals | 2 | 2 | 0 / 2 / 0 | LC-META, LC-XREF | Light copy edit | A synthesis Unit whose summaries are written as a recap of the course; content itself is high quality and measurement-backed. |
| 7 | `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow | 4 | 4 | 0 / 2 / 2 | LC-META, LC-LEAK, LC-VOICE | Substantive editorial rewrite | A distinct, later authoring generation: terse specification voice, imperative summaries, three Lesson-ID leaks, and the "bounded bridge" authoring vocabulary. The worked example (question library, "the learner navigates", "area's back stack") leans on this application's own design. |
| 8 | `unit_observable_state_collection` | Observable State Collection and Lifecycle | 4 | 4 | 0 / 2 / 2 | LC-LEAK, LC-META, LC-TONE | Substantive editorial rewrite | Same generation as Unit 7: the most ID-dense Unit in the corpus (9 Lesson IDs plus a backlog key), ownership phrasing ("owns those mechanics", "remains responsible for teaching"), verification-log voice ("resolved", "checked against") and "Do not…" callouts. |
| 9 | `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and LaunchedEffect | 4 | 4 | 0 / 4 / 0 | LC-LEAK, LC-META, LC-TONE | Light copy edit | Same generation as Units 7–8 (ownership-of-concepts phrasing, "resolved runtime", "Do not…" callouts, seven Lesson-ID leaks), but each Lesson's core explanation is already local, so the edit is mostly deletion. |
| 10 | `unit_latest_values_and_event_driven_work` | Latest-Value Effects and Event-Driven Coroutine Work | 3 | 3 | 0 / 3 / 0 | LC-LEAK, LC-META | Light copy edit | Same generation as Units 7–9, lighter on problems: two ID leaks and scope disclaimers; explanations are compact and mostly natural. |
| 11 | `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers | 4 | 4 | 0 / 4 / 0 | LC-XREF, LC-LEAK, LC-VOCAB | Light copy edit | More natural voice than Units 7–8 and strong worked examples; Unit numbers are used as names for mechanisms ("Unit 9's effect model", "Unit 8's collection"), which is the specific clarity cost to fix. |
| 12 | `unit_production_ui_effects_and_selection` | Production UI Effects and Mechanism Selection | 3 | 3 | 0 / 2 / 1 | LC-META | Light copy edit | Capstone of the Compose-effects generation: strong synthesis, but it narrates the course ("five units", "this path has built towards") and the final Lesson is dominated by curriculum-ownership language. |
| 13 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency | 5 | 5 | 0 / 5 / 0 | LC-XREF, LC-META, LC-VOCAB | Light copy edit | Coroutines/Flow generation: the most natural engineer voice in the corpus and generous local bridging. Its signature problem is forward pointers by position ("the failure unit", "the third unit's subject", "the next unit's job") and a heavier "shape" habit. |
| 14 | `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work | 4 | 4 | 0 / 4 / 0 | LC-CLARITY, LC-VERBOSE, LC-VOICE | Light copy edit | Same Topic as Unit 13 but a visibly different voice: short declarative sentences, imperative summaries, closing checklists, and KMP provenance disclaimers before code. Numbered as "Unit 1/Unit 3" within its Topic, which collides with Compose numbering. |
| 15 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination | 5 | 5 | 1 / 4 / 0 | LC-XREF, LC-VOICE | Light copy edit | Natural, measured Coroutines voice (closer to Unit 13 than Unit 14) with topic-local Unit numbers, Lesson ordinals, and "honest/worth" framing; ends each Lesson with a duplicated recap. |
| 16 | `unit_flow_fundamentals` | Flow Fundamentals | 5 | 5 | 0 / 5 / 0 | LC-VOCAB, LC-XREF | Light copy edit | Same natural Coroutines/Flow voice as Units 13 and 15; highest "shape" density in the corpus (15) and routine "later in this curriculum" pointers, one of which points the wrong way. |
| 17 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure | 5 | 5 | 0 / 5 / 0 | LC-XREF | Light copy edit | Among the best-written Units: one running example per Lesson, measured, direct. Remaining work is positional pointers, a few "shape" uses and an over-long summary. |
| 18 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams | 5 | 5 | 0 / 5 / 0 | LC-META, LC-XREF | Light copy edit | Same high-quality Flow voice; its specific habit is describing the Unit's own architecture (which Lesson owns which axis), including inside a table. |
| 19 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries | 5 | 5 | 0 / 5 / 0 | LC-META, LC-VOCAB | Light copy edit | Architecture generation: argumentative, confident, example-driven prose. Characteristic problems are "the X curriculum's subject" pointers (several to curricula that do not exist), an authoring rationale for not teaching SOLID, a trailer-style Unit ending, and use of this application's own repositories as evidence. |
| 20 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State | 5 | 5 | 0 / 4 / 1 | LC-META, LC-VOCAB, LC-CLARITY | Light copy edit | Architecture voice at its most self-referential: "the Compose curriculum", "the coroutines curriculum", "the closing unit" and several non-existent curricula appear in every Lesson. The worked examples increasingly use this application's own types (study-progress holders, `LearningLessonUiState`, `PracticeBuilderUiState`, `AppCoroutineScope`). |
| 21 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth | 5 | 5 | 0 / 4 / 1 | LC-META, LC-VOCAB | Light copy edit | Highest "shape" count of any Unit (29) and the corpus's clearest Rule 11 problem: Lessons audit this learning application's own repositories and state holders as evidence, and one addresses "the application you are reading this in". Every Lesson summary describes what "this lesson" does. |
| 22 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction | 5 | 5 | 0 / 4 / 1 | LC-META, LC-VOICE | Light copy edit | The most scaffolded Unit: "the foundations unit" as a proper noun, "This lesson…" summaries, recap/trailer endings, and Senior sections that audit this application's own classes, packages and KDoc. Technical reasoning is excellent and the rewrite is mostly deletion plus genericising examples. |
| 23 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models | 5 | 5 | 0 / 3 / 2 | LC-META, LC-VOCAB | Substantive editorial rewrite | The most self-describing Unit in the corpus: the summary, the first Lesson and several transitions explain the Unit's pedagogy (running example, names-last, classification-not-prescription). Technical content is very strong; the rewrite is a re-voicing of framing, not new content. |
| 24 | `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection | 4 | 4 | 0 / 2 / 2 | LC-META | Substantive editorial rewrite | Architecture capstone: all four Lessons exceed 2,000 words, and the Unit narrates its relationship to "the Compose curriculum", "the streams curriculum" and earlier Units throughout. It overlaps Compose Unit 12 on delivery guarantees. Content is the most synthetic in the corpus; the rewrite must preserve it while removing hand-off framing. |
| 25 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction | 6 | 6 | 0 / 6 / 0 | LC-META, LC-VOCAB | Light copy edit | DI generation: shares the Architecture voice (argumentative, precise, "the X curriculum" references, "This lesson…" summaries) but with far less codebase-as-evidence material and a clean running example. A light pass removes most problems. |
| 26 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes | 6 | 6 | 1 / 5 / 0 | LC-META, LC-VOCAB | Light copy edit | DI generation at its strongest: running example, precise vocabulary, careful refusal to generalise across tools. Problems are the shared DI/Architecture habits ("This lesson…" summaries, "the X curriculum", "shape" for arrangement). |
| 27 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs | 7 | 7 | 1 / 6 / 0 | LC-CLARITY, LC-META, LC-VOCAB | Light copy edit | Strong, restrained tool teaching (no over-claiming). Distinctive problems: the longest Unit summary, DI-Topic "Unit 1/Unit 2" numbering in the first Lesson, third-person "the reader", and "honest"/"shape" habits. |
| 28 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger | 6 | 6 | 2 / 4 / 0 | LC-VOICE, LC-CLARITY | Light copy edit | A later, terser generation: compact Lessons (the shortest in the DI Topic), almost no "worth/honest" framing, but objective-style summaries and DI-Topic Unit numbers used as names for arguments. |
| 29 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP | 5 | 5 | 0 / 3 / 2 | LC-META, LC-VOICE, LC-CLARITY | Substantive editorial rewrite | The corpus's clearest Rule 11 Unit: its worked graph is this learning application's own Koin configuration, cited by module and type name, audited with counts, and called "this codebase", "the case study" and "repository evidence". Summaries follow the Hilt Unit's objective style. |
| 30 | `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy | 4 | 4 | 1 / 3 / 0 | LC-LEAK, LC-VOICE | Light copy edit | Terse late generation (as Hilt/Koin): little rhetoric, strong decision method. One high-severity leak (a Question ID with an assessment-authoring note) and objective-style summaries. |

## Authoring-Generation Observations

The style changes visibly across the corpus, and the changes line up with authoring phases
rather than with Topics. Six generations are distinguishable:

| Gen. | Units | Recognisable by | Typical problems | Rewrite intensity |
| --- | --- | --- | --- | --- |
| **A** — early Compose | 1–6 | Natural engineer voice, concrete bugs, measured claims; 1 200–2 200 words | "The previous unit left a deliberate gap", Unit numbers ("Unit 2", "Unit 3") in Units 4–6, "worth", a few "shape" | Light: mostly deleting coordinates |
| **B** — Compose production and effects | 7–12 | Units 7–10 are short (600–800 words) and terse, almost specification-like; Units 11–12 regain length | All 25 Lesson-ID leaks except one; "owns those mechanics", "remains later curriculum", "bounded bridge"; "resolved/configured runtime"; "Do not…" callouts; objective summaries in 7–8; Units 11–12 use Unit numbers as names for mechanisms | Substantive for 7–8; Light for 9–12 |
| **C** — Coroutines and Flow | 13, 15–18 | The most natural voice in the corpus; measured, example-driven, generous local bridging | Forward pointers by position ("the failure unit", "later in this curriculum", "the third unit's subject"); "shape" peaks in Unit 16; imperative summaries in 15–16; "honest" | Light |
| **C′** — Context and dispatchers | 14 | Same Topic, but short declarative sentences, closing checklists and KMP provenance disclaimers before code | Topic-local "Unit 1/Unit 3"; disclaimers; checklists | Light |
| **D** — Application Architecture | 19–24 | Long (≈1 600–2 350 words), argumentative, requirement-driven, carefully conditional | "the Compose curriculum", "the foundations unit", non-existent curricula; "This lesson…" summaries; recap/trailer endings; this application used as evidence (Rule 11); "shape" peaks in Units 20–23; "honest" | Light for most Lessons; Substantive for 23–24 and for Senior sections built on this-app evidence |
| **E** — DI foundations and Dagger | 25–27 | Architecture's voice with a clean running example; restrained tool claims | "This lesson…" summaries; very long Unit summaries (153–205 words); positional references at their densest (33 in Unit 27); DI-Topic Unit numbers; "Shape one/two" | Light |
| **F** — Hilt, Koin, strategy | 28–30 | Short, dense, almost no rhetoric; 25–27-word Unit summaries | Every summary an imperative objective; DI-Topic Unit numbers; Unit 29 is a case study of this repository (Rule 11); the Question-ID leak | Light for 28 and 30; Substantive for 29 |

Per-Unit style signals (raw counts in learner prose; "Unit N" includes "unit-1"):

| # | Unit | Gen. | Lessons | Avg words | Unit-summary words | "worth" | "honest" | "shape" | "Unit N" | previous/next Unit or Lesson | "curriculum" | Lesson IDs leaked | Imperative Lesson summaries | "This lesson…" summaries |
| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | Thinking in Compose | A | 3 | 1234 | 84 | 8 | 1 | 2 | 0 | 0 | 0 | 0 | 0 | 0 |
| 2 | State and State Ownership | A | 5 | 1402 | 95 | 10 | 1 | 4 | 0 | 9 | 2 | 0 | 0 | 0 |
| 3 | Recomposition | A | 3 | 1333 | 92 | 10 | 1 | 1 | 0 | 5 | 0 | 0 | 0 | 0 |
| 4 | Identity, Keys, Stability and Immutability | A | 5 | 1564 | 124 | 13 | 6 | 3 | 2 | 12 | 0 | 0 | 0 | 0 |
| 5 | Derived State and Expensive Work | A | 3 | 1629 | 95 | 6 | 2 | 1 | 7 | 4 | 0 | 0 | 0 | 0 |
| 6 | Snapshot Fundamentals | A | 2 | 2164 | 117 | 6 | 0 | 1 | 3 | 5 | 1 | 0 | 0 | 0 |
| 7 | Production Screen State and Unidirectional Data Flow | B | 4 | 802 | 76 | 0 | 0 | 0 | 0 | 2 | 3 | 3 | 2 | 0 |
| 8 | Observable State Collection and Lifecycle | B | 4 | 736 | 70 | 0 | 1 | 0 | 5 | 3 | 2 | 9 | 1 | 0 |
| 9 | Effect Lifecycle and LaunchedEffect | B | 4 | 719 | 62 | 0 | 0 | 0 | 1 | 2 | 1 | 7 | 0 | 0 |
| 10 | Latest-Value Effects and Event-Driven Coroutine Work | B | 3 | 601 | 73 | 0 | 0 | 1 | 3 | 1 | 1 | 2 | 1 | 0 |
| 11 | Cleanup, External Synchronization and State Producers | B | 4 | 1064 | 80 | 3 | 1 | 4 | 11 | 6 | 0 | 3 | 0 | 0 |
| 12 | Production UI Effects and Mechanism Selection | B | 3 | 1685 | 90 | 5 | 0 | 3 | 0 | 5 | 8 | 0 | 0 | 0 |
| 13 | Coroutine Fundamentals and Structured Concurrency | C | 5 | 1504 | 118 | 19 | 3 | 8 | 0 | 15 | 1 | 0 | 0 | 0 |
| 14 | Coroutine Context, Dispatchers and Concurrent Work | C′ | 4 | 1224 | 66 | 1 | 0 | 4 | 10 | 3 | 2 | 0 | 4 | 0 |
| 15 | Cancellation, Failure and Coordination | C | 5 | 1221 | 86 | 10 | 3 | 7 | 5 | 4 | 4 | 0 | 5 | 0 |
| 16 | Flow Fundamentals | C | 5 | 1280 | 128 | 16 | 2 | 15 | 6 | 5 | 11 | 0 | 5 | 0 |
| 17 | Flow Composition, Timing and Failure | C | 5 | 1261 | 146 | 13 | 1 | 4 | 0 | 5 | 2 | 0 | 0 | 0 |
| 18 | StateFlow, SharedFlow and Hot Streams | C | 5 | 1462 | 149 | 9 | 4 | 4 | 0 | 8 | 3 | 0 | 0 | 0 |
| 19 | Architecture as Responsibilities and Boundaries | D | 5 | 1636 | 93 | 19 | 5 | 9 | 0 | 8 | 9 | 1 | 0 | 1 |
| 20 | Screen State Holders, ViewModel and UI State | D | 5 | 1668 | 91 | 11 | 4 | 15 | 0 | 7 | 20 | 0 | 0 | 0 |
| 21 | Repositories, Data Ownership and Single Source of Truth | D | 5 | 1965 | 118 | 12 | 6 | 28 | 0 | 11 | 9 | 0 | 0 | 5 |
| 22 | Domain Logic, Use Cases and Dependency Direction | D | 5 | 2040 | 118 | 14 | 8 | 8 | 0 | 13 | 9 | -3 | 0 | 4 |
| 23 | MVP, MVVM and MVI Responsibility Models | D | 5 | 2035 | 156 | 11 | 5 | 13 | 0 | 12 | 10 | 0 | 0 | 1 |
| 24 | State, Events, Lifetime and Architecture Selection | D | 4 | 2353 | 162 | 12 | 5 | 10 | 0 | 8 | 19 | 0 | 0 | 2 |
| 25 | Dependency Injection as Object Construction | E | 6 | 1557 | 153 | 9 | 3 | 13 | 0 | 13 | 14 | 0 | 0 | 5 |
| 26 | Object Graphs, Lifetimes and Scopes | E | 6 | 1829 | 169 | 12 | 10 | 13 | 0 | 22 | 12 | 0 | 0 | 6 |
| 27 | Dagger: Compile-Time Object Graphs | E | 7 | 1461 | 205 | 17 | 6 | 9 | 11 | 33 | 4 | 0 | 0 | 6 |
| 28 | Hilt: Android Lifecycle-Aware Dagger | F | 6 | 637 | 25 | 0 | 0 | 3 | 7 | 0 | 0 | 0 | 6 | 0 |
| 29 | Koin and Dependency Injection in KMP | F | 5 | 666 | 27 | 0 | 0 | 3 | 5 | 2 | 3 | 0 | 5 | 0 |
| 30 | Choosing a Dependency Injection Strategy | F | 4 | 1224 | 26 | 1 | 1 | 2 | 2 | 0 | 1 | 0 | 4 | 0 |

Implications for the rewrite: generation C needs the least work and should be edited
lightly to avoid flattening its voice; generation B needs its leaks and scope sentences
removed but no expansion; generations D and E need deletion of curriculum narration and
replacement of this-app evidence; generation F needs summaries re-voiced and Unit numbers
replaced, not more rhetoric.

## Rewrite Batching Recommendation

Batches follow the [rewrite workflow](#rewrite-workflow). Order is by learner impact,
severity, amount of leakage, shared terminology, and editing effort.

| Order | Batch | Why here | Size |
| ---: | --- | --- | --- |
| **1** | **Units 7–8 — Production Screen State and Unidirectional Data Flow; Observable State Collection and Lifecycle** | 12 of the 25 Lesson-ID leaks and the backlog key; two Substantive Units; the "bounded bridge" Lesson title; one shared worked example and one shared vocabulary (screen-level owner, content boundary, question library) that must be edited together | 8 Lessons, ≈ 6 150 words |
| 2 | Units 9–11 — Effect Lifecycle and LaunchedEffect; Latest-Value Effects; Cleanup, External Synchronization and State Producers | The remaining 12 Compose Lesson-ID leaks; same generation and vocabulary ("resolved runtime", effect ownership); Unit numbers used as names for mechanisms | 11 Lessons, ≈ 8 900 words |
| 3 | Units 29–30 — Koin and Dependency Injection in KMP; Choosing a Dependency Injection Strategy | Clearest Rule 11 problem, the Question-ID leak and three High findings; same terse generation and Koin vocabulary; do it after the Rule 11 contract revision so the replacement examples follow the new rule | 9 Lessons, ≈ 8 200 words |
| 4 | Units 23–24 — MVP/MVVM/MVI; State, Events, Lifetime and Architecture Selection | Two Substantive Units, one High Rule 11 finding, the longest Lessons; the near-duplicate Lesson title with Unit 12 is resolved here | 9 Lessons, ≈ 19 600 words |
| 5 | Units 19–22 — Architecture foundations, screen state holders, repositories, domain logic | Same vocabulary as batch 4 ("the foundations unit", this-app evidence); two Substantive Lessons; heavy "shape" in Units 20–21; the remaining two leaks (Units 19, 22) | 20 Lessons, ≈ 36 500 words — may be split into 19–20 and 21–22 |
| 6 | Unit 12 — Production UI Effects and Mechanism Selection | One Substantive Lesson; title differentiation with Unit 24 | 3 Lessons, ≈ 5 050 words |
| 7 | Units 25–28 — DI foundations, object graphs, Dagger, Hilt | Light edits, but a shared running example and shared terminology (scope/owner, "Shape one/two", DI-Topic Unit numbers) | 25 Lessons, ≈ 34 400 words — may be split into 25–26 and 27–28 |
| 8 | Units 13–18 — Coroutines and Flow | Least damaged; positional pointers and "shape" | 29 Lessons, ≈ 38 500 words — split by Unit pairs |
| 9 | Units 1–6 — early Compose | Least damaged; coordinates and framing | 21 Lessons, ≈ 31 700 words |

**Recommended first batch: Units 7 and 8.** They carry the highest learner-visible damage per
word in the corpus — half of all Lesson-ID leaks, a backlog key, authoring vocabulary in a
Lesson title, curriculum-ownership sentences in almost every section, and a worked example
that describes this application's own navigation ("the learner navigates", "the area's back
stack"). They are small (8 Lessons), tightly coupled (Unit 8 opens by continuing Unit 7's
owner and example, and both use the same question-library screen), and share a single
generation's voice, so one editor can re-voice them consistently in one pass. They do not
depend on the Rule 11 contract revision as heavily as the Architecture and Koin batches.

## Authoring-Contract Follow-Ups

The contract revised before this audit describes most of what was found: IDs in prose,
curriculum narration, positional references, "shape", framing words, calibrated absolutes,
concise-not-cryptic. Its examples are the right ones — several appear in the corpus
verbatim. It is **not sufficient** in six places, and a targeted revision is recommended
**before** the Rule 11-heavy batches (3–5). Recorded here only; the contract was not edited.

1. **Rule 11 needs a sharper line between an example and a report.** The contract currently
   allows naming this application's types "as parts of an example", and itself uses
   `LessonStudyRepository` as the model of acceptable framing. The corpus shows where that
   leads: this app's real repositories, state holders, Koin modules and KDoc presented as
   evidence, with counts, dated statements ("currently declares no custom scopes") and
   evidential disclaimers. Missing rule: *examples use invented, neutral names; a Lesson does
   not cite this repository's classes, module names, file counts or documentation as
   evidence, and never says what the codebase "currently" does.*
2. **Lesson and Unit summaries have no rule.** Two templates dominate — the imperative
   learning objective and the "This lesson …" authoring outline — plus Unit summaries that
   recap earlier Units and preview every Lesson (up to 205 words). Missing rule: *a summary
   states the idea in the learner's terms; it does not describe the Lesson, instruct the
   reader, or recap the course.*
3. **References to material that does not exist.** Rule 12 bans positions and IDs but not
   pointers to planned or imaginary Units and curricula ("the performance unit", "the testing
   curriculum"). Missing rule: *never refer to a Unit, Lesson or curriculum that is not in the
   shipped curriculum; the word "curriculum" should not appear in learner prose at all.*
4. **Author-process and maintenance narration.** Rule 11 covers attributing measurements to a
   toolchain, but not narrating how the author verified something or instructing future
   maintainers ("A later dependency update should re-read those contracts"). Missing rule:
   *report results, not the verification process; no maintenance instructions in prose.*
5. **Code comments and table cells.** Curriculum narration was found in code comments ("is
   the subject two lessons from here") and in table columns ("Where the answer lives: This
   lesson"). Missing clarification: *Rule 12 applies to comments, table headers and cells.*
6. **Titles become references.** Once cross-references use human titles, two titles that
   differ by one word (**What Delivery Guarantee Does This Occurrence Need?** and **What
   Guarantee Does This Occurrence Need?**) become ambiguous. Missing checklist item: *Lesson
   titles are unique enough to be cited unambiguously.*

A smaller audit-framework follow-up: add a category for Rule 11 problems (for example
`LC-PROJECT`), which this pass had to file under `LC-META`, and note in the voice section
that the reader should not be referred to in the third person ("the learner", "the reader").

**Status (2026-09-27): incorporated.** All six follow-ups and the third-person voice note
are now in [`docs/content/learning-content-authoring.md`](../content/learning-content-authoring.md),
integrated into the existing rules rather than appended:

| Follow-up | Where it now lives |
| --- | --- |
| 1. Example vs report | Rule 11 — invented example identities, no repository facts as evidence, no "currently" statements about this repository's implementation outside versioned API contracts |
| 2. Summaries | Rule 12 — *Summaries state the idea* |
| 3. Nonexistent material | Rule 12 — cross-references resolve to shipped Units and Lessons; "curriculum" kept out of Lesson prose |
| 4. Author-process narration | Rule 12 — results stated, verification and maintenance instructions omitted |
| 5. Comments and table cells | Purpose and Scope — learner-facing text defined to include titles, summaries, callouts, table headers and cells, and code comments |
| 6. Titles as references | Rule 12 and the Lesson checklist — titles distinct enough to cite unambiguously |
| Third-person voice | Rule 7 — *Voice* |

`LC-PROJECT` was added to [Categories](#categories) for future findings; existing findings
keep their category and number. No production curriculum content changed, and the
rewrite batching order above is unchanged. This satisfies the prerequisite for Batch 3.

## Rewrite Workflow

After the audit, production content is rewritten in small, reviewable batches: one Unit
at a time, or a tightly related cluster of Units when terminology must stay consistent
across them.

For each batch:

1. Read the batch's findings in this ledger, the matching cross-reference and "shape" rows,
   and the Lesson matrix entries.
2. Preserve technical meaning. An editorial rewrite that changes a claim is a content
   change and needs its sources rechecked under Rule 9.
3. Preserve stable Unit and Lesson IDs.
4. Preserve Subtopic mappings and `relatedLessonIds` unless a separate semantic issue
   requires a change, recorded as such.
5. Change only learner-facing prose (and code comments where a finding names them).
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
fingerprint line only. The one intended exception is a retitled Lesson: the report
renders Lesson titles, so a finding that requires a new title (LC-046, for example) also
changes that Lesson's heading. Anything more means the batch changed mappings or
structure and needs review as such.

## Audit Status

**COMPLETE** — corpus audit of commit `3ec5225`, 2026-09-27. Every active Lesson (135 of 135)
and every Unit title and summary (30 of 30) was read in full. At audit completion no
rewrite had started and every Unit's status was **Reviewed**. Rewrite progress since then
is recorded in the [Remediation Log](#remediation-log); the validation table below
describes the audit itself, not later rewrites.

Completion criteria:

- [x] Every active Lesson has been read in full.
- [x] Every automated sweep candidate has been reviewed in context and recorded or dismissed.
- [x] Every Unit has an audit status other than **Not reviewed**.
- [x] The internal-identifier inventory is complete: every ID-like token in learner prose is
      classified as a leak or as a real technical identifier.
- [x] The cross-reference review is complete, with every prose reference classified.
- [x] Corpus-level voice findings are documented.
- [x] Every Unit and every Lesson has an assigned rewrite scope.
- [x] No regular-expression hit has been treated as a defect without contextual review.

Validation of this document (scripted, against the audited commit):

| Check | Result | Detail |
| --- | --- | --- |
| Every active Lesson appears exactly once in the Lesson audit matrix | Pass | 135 rows, 135 distinct, 0 missing |
| Every active Unit appears in the Unit summary | Pass | 30 rows |
| All finding IDs are unique and sequential | Pass | 223 findings, LC-001…LC-223 |
| Every finding ID referenced elsewhere in this document exists | Pass | 201 distinct IDs referenced outside the ledger; missing: none |
| Every finding referenced by a Lesson row exists | Pass | 200 distinct IDs in matrix |
| Every confirmed metadata leak is in the identifier inventory | Pass | 26 ID tokens found by sweep + E24 + epic reference; 28 inventory rows |
| "Shape" classifications reconcile with the corpus | Pass | 188 prose rows (Keep 25, Replace 140, Remove 23) against 188 sweep matches; 3 code-comment rows covering 6 matches |
| Cross-reference totals reconcile with the inventory | Pass | 588 inventory rows: Keep 10, Title 185, Remove 349, Local 44 |
| No production curriculum file was modified | Pass | git status --short before writing: clean |

The only file changed by this audit is `docs/quality/learning-content-audit.md`.

## Remediation Log

Rewrite batches applied after the audit. Everything above this section is the audit
record and is left as audited: evidence quotes, locations, Lesson titles and counts still
describe commit `3ec5225`. A finding's current status is in the ledger's Status column;
how it was resolved is recorded here.

### Batch 1 — Units 7 and 8 (2026-09-27)

Scope: `unit_production_screen_state_and_udf` and `unit_observable_state_collection`,
findings LC-039 through LC-058. Every finding was verified against the current text
before editing; the curriculum was unchanged since the audited commit, so none had
already been resolved. Status: **Rewrite complete**, then **Re-verified** after all eight
Lessons were re-read in full.

Preserved: Unit and Lesson IDs, statuses, authored order, section depths and block
sequence, Subtopic mappings, `relatedLessonIds`, sources, and every code block except one
editorial comment (LC-042). No other Unit changed. The learning-question coverage report
changed only in its fingerprint and in the heading of the retitled Lesson.

| Finding | Resolution |
| --- | --- |
| LC-039 | Unit summary describes the three kinds of state, the content contract and owner lifetime; "applies the earlier… foundations" removed. |
| LC-040 | ID sentence replaced by the owner rule stated locally, with an optional pointer to **State Hoisting and the Lowest Sensible Owner**. |
| LC-041 | Summary describes the subject and both placement failures; "The earlier state-hoisting lesson" removed; the interview callout states the reasoning instead of what "a strong answer should" contain. |
| LC-042 | Positional and scope disclaimers removed; the overload pair is explained locally (a stateless function plus an interchangeable stateful convenience overload); code comment now "Convert the owner's observable state into a current value here." |
| LC-043 | Telegraphic callout rewritten as one sentence. |
| LC-044 | ID and "without re-teaching" removed; equality argument kept, with an optional pointer to **Stability and Skipping**. |
| LC-045 | Scope disclaimers removed. The `loadFailed` simplification is now described as a property of the example, with an optional pointer to **Modelling the Current UI State**; the closing UDF paragraph keeps its technical content without "this unit stops". |
| LC-046 | Retitled **Moving State Outside the Composition: Owner vs Lifetime**; summary no longer describes where the curriculum stops; the closing Senior paragraph applies the ownership/lifetime/persistence distinction to concrete design choices. |
| LC-047 | ID and "owning curricula" removed; process death is explained locally with an optional pointer to **`rememberSaveable` and State That Must Survive**. |
| LC-048 | "The learner" becomes "The user"; "the area's back stack" becomes "the back stack"; the Navigation 3 setup is framed as a hypothetical application; both disclaimers removed. |
| LC-049 | Unit summary compressed to converting, cost and pausing. |
| LC-050 | IDs removed. The snapshot-observed read is now explained locally (Rule 12 "bring context local" row); `StateFlow` and `snapshotFlow` pointers use **`StateFlow`: One Current Value** and **`snapshotFlow` and Crossing Into Flow**. |
| LC-051 | Unit-number coordinates and "The next lesson" removed; the example starts from the owner exposing `StateFlow`. |
| LC-052 | Both IDs and "E24" removed; the `StateFlow` current-value fact is stated locally. No operator pointer added because the paragraph does not need one. |
| LC-053 | Summary rewritten as a description; ownership and "later units" sentences removed; the Senior note names Compose runtime 1.11.2 and points to **`produceState`: a Composition-Scoped Producer**. The `flowOn` sentence states what `flowOn` does instead of which curriculum owns it. |
| LC-054 | "Resolved runtime/implementation" replaced by "Compose runtime 1.11.2" or the plain API contract. |
| LC-055 | IDs, ownership phrasing and "sharing bridge" removed; cooperative cancellation is stated locally with pointers to **Cancellation Is Cooperative** and **Sharing Cold Flows with `stateIn` and `shareIn`**. |
| LC-056 | Summary describes host mappings without project coupling; responsibility narration removed; the verification log became a version caveat that keeps the beta status and version numbers. |
| LC-057 | ID replaced by "its owner scope and `SharingStarted` policy" with an optional title pointer. |
| LC-058 | The five "Do not…" callouts now open with the fact. The same pattern in Unit 7 was checked and not found. |

Beyond the quoted evidence, the full-Unit re-read also turned the remaining imperative
Lesson summaries (`lesson_stateless_screen_content`, `lesson_screen_state_and_ui_events`,
`lesson_collection_lifetime_and_cost`, `lesson_external_state_in_compose`) and one Unit 8
interview callout ("A strong explanation…") into descriptions. It also removed verification
wording that the ledger quotes did not list ("the configured … host", a
dispatcher-setup disclaimer).

Inventory reconciliation for this batch:

- **Identifier inventory** rows 1–13 (12 Lesson IDs and the E24 backlog key): all removed
  from learner prose. Rows 14–28 belong to other Units and are unchanged.
- **Cross-reference inventory**, Unit 7 (12 rows) and Unit 8 (18 rows): every **Remove**
  row was deleted. Every **Bring required context local** row (Unit 7 overload pair,
  Unit 8 snapshot observation) now carries the missing fact in the Lesson. **Rewrite using
  human title** rows keep a title pointer where it offers useful further reading
  (11 pointers across the two Units, including the **Modelling the Current UI State**
  pointer that LC-045's rewrite direction suggested). Where the local fact was enough,
  the pointer was dropped (`lesson_state_flow` in `lesson_collect_as_state`, the
  Flow-context row, and the E24 row). Every retained title was checked against the
  bundled curriculum, and every pointer can be removed without breaking its paragraph.
- **Shape inventory**: no Unit 7–8 rows. The remaining uses of "boundary" and "bridge" in
  these Units are engineering uses (the screen/content boundary, stream-to-state
  conversion), kept deliberately.

Technical claims: the edits keep existing claims and their version qualifications. Two
sentences now state mechanism explicitly where a curriculum pointer used to stand in for
it: cancellation takes effect at cancellable suspension points or cancellation checks,
and `flowOn` changes the context of upstream work only. Both are covered by the Lessons'
existing Kotlin Flow sources ("Flow cancellation checks", "Flow context").

### Batch 2 — Units 9, 10 and 11 (2026-09-27)

Scope: `unit_effect_lifecycle_and_launched_effect`, `unit_latest_values_and_event_driven_work`
and `unit_cleanup_synchronization_and_producers`, findings LC-059 through LC-079. Every
finding was checked against the text at the implementation baseline (`f258bb3`) before
editing. None of these Units had changed since the audited commit, so no finding was
already resolved. Status: **Rewrite complete**, then **Re-verified** after all eleven
Lessons and the three Unit summaries were re-read in full, in authored order.

Preserved: Unit and Lesson IDs and titles, topics, statuses, authored order, section depths
and block sequence, comparison row counts, Subtopic mappings, `relatedLessonIds`, sources,
and every code block, comments included. No other Unit changed. The learning-question
coverage report changed only in its learning-curriculum fingerprint.

| Finding | Resolution |
| --- | --- |
| LC-059 | Both ID sentences deleted; the paragraph now opens with the execution rules it already stated. No pointer was kept. |
| LC-060 | "On purpose" became "The condition matters". Senior paragraph 1 ends on the four ownership problems. Senior paragraph 2 ends with the engineering point: the work needs an owner that outlives the UI position, such as a screen-level state holder. |
| LC-061 | IDs removed. The paragraph states that the stop is ordinary cooperative cancellation, then points to **Cancellation Is Cooperative**. The identity sentence now says the branch containing the call site decides the owning UI lifetime, with a pointer to **Composable Identity**. |
| LC-062 | "Resolved … contract" became "In Compose runtime 1.11.2". The scope sentence and the coroutine-curriculum clause became "Which dispatcher that is depends on the host". |
| LC-063 | Both ownership clauses removed. The stability sentence now says the skipping comparison plays no part in deciding whether an effect restarts. |
| LC-064 | Scope narration replaced by "`rememberUpdatedState` handles that latest-value case", with a pointer to **Reading the Current Value Without Restarting**. |
| LC-065 | The summary describes the diagnosis instead of instructing the reader. "Unit 10 … stops before teaching the solution" now names `rememberUpdatedState` and points to **Reading the Current Value Without Restarting**. |
| LC-066 | The four quoted callouts now open with the fact. Examples: "A `Boolean` that records whether the body has already run does not fix this"; "Recomposition does not rerun LaunchedEffect on its own"; "An effect's lifetime begins when it enters Composition"; "A fresh object alone does not explain a restart". |
| LC-067 | The opening states the key rule without a Unit number. The equality detail points to **What an Effect's Keys Declare**. The Senior paragraph uses "In Compose runtime 1.11.2" and "the public contract is what to rely on". |
| LC-068 | Column header "Correct shape" became "Implementation" (S013). |
| LC-069 | The builder-mechanics sentence was deleted: Practical paragraph 3 already says `launch` returns a `Job`. The concurrency-policy sentence now states the fact: the handler chooses whether repeated clicks run in parallel, are ignored or cancel earlier work, and `rememberCoroutineScope` does not choose it. |
| LC-070 | "From Unit 9" removed, and the coined "uncontrolled-body-work defect" was replaced by what happens: a `launch` in the body runs whenever the body executes, and no event controls it. "Resolved runtime/implementation" became "Compose runtime 1.11.2" or "the 1.11.2 implementation". |
| LC-071 | Both disclaimers deleted. Senior paragraph 1 ends at "Move it to an owner whose lifetime actually matches the requirement." The callout opens with the misconception it corrects rather than "Do not reduce…". |
| LC-072 | Unit numbers removed. Core paragraph 1 opens with "Not everything a screen owns is a coroutine". The callout names `SideEffect` and `LaunchedEffect`. The Senior paragraph names `rememberUpdatedState` directly. |
| LC-073 | The key-comparison ID sentence was deleted. The cancellation ID became a local fact: a `finally` block runs as the cancelled coroutine unwinds. It is followed by an optional pointer to **Cleanup, `NonCancellable` and Timeouts** for cleanup that suspends. |
| LC-074 | "A different shape from" became "a different problem from" (S014). "The mechanism whose shape matches" became "is built for that problem" (S015). |
| LC-075 | Unit numbers and "bridge back" removed; Senior paragraph 3 now opens "Choosing the right mechanism instead starts from the trigger". The dangling sentence now states the distinction: the skipped child published once because its call did not execute, not because `SideEffect` watches for state changes. |
| LC-076 | Unit numbers removed. The four-part contract now says its first three parts are `LaunchedEffect`'s lifetime rules (launch on entry, cancel on exit, restart on key change), which the paragraph goes on to state. "Unit 8's" became `collectAsState` or its lifecycle-aware variant. "The question this Lesson exists for" became "The revealing case is a key change". The next-Lesson reference names **A Flow Below the UI, or a Producer at the Boundary?**. |
| LC-077 | ID and "does not repeat it" replaced by "(`callbackFlow` builds that adapter; see **Flow Builders and Adapting Callback APIs**)". |
| LC-078 | Positional references removed. The first reference now reads "A producer that loads data suspends", and the second "a `DisposableEffect`". The trigger distinction is named as composition-driven versus event-driven. "Unit 8's collection" became collecting into Compose `State` with `collectAsState` or its lifecycle-aware variant. "The producer shape" became "a state producer" (S017) and "honest" became "right". The later-unit pointer names **Choosing the Smallest Sufficient Mechanism**. |
| LC-079 | "Assignment-shaped publication" became "simple assignments" (S016). |

The full re-read also made edits the ledger quotes did not list:

- All three Unit summaries and the `lesson_who_owns_the_trigger` summary now describe
  the subject instead of what "this unit" or "the final lesson" does, or instructing
  the reader.
- The same corrective-callout pattern was re-voiced in Unit 10 ("Do not reduce…") and in
  `lesson_side_effect_publication` ("Do not compress…").
- Build narration that the ledger quotes only partly covered ("on the configured
  runtime/toolchain", "on this runtime") now carries the version already used throughout
  these Lessons: "a probe with Compose runtime 1.11.2".
- In the worked example, "A learner opens a session" became "A user opens a session",
  matching Batch 1's LC-048 change.
- One "every later question in this lesson" and one "the one that separates a senior
  answer" framing were tightened.

Inventory reconciliation for this batch:

- **Identifier inventory** rows 14–25 (12 Lesson IDs): all removed from learner prose.
  A contextual search of every prose block, table cell, summary and code comment in the
  three Units found no remaining curriculum identifiers, Unit numbers, course positions or
  product metadata. Rows 26–28 belong to other Units and are unchanged.
- **Cross-reference inventory**, 33 rows: Unit 9 (10), Unit 10 (6), Unit 11 (17).
  - **Remove** (19 rows): every one was deleted.
  - **Bring required context local** (5 rows, all in Unit 11): each now carries the fact in
    the Lesson. The `DisposableEffect` callout names both alternatives. The `finally`
    sentence explains coroutine cleanup. `produceState` states its launch, cancel and
    restart lifecycle as `LaunchedEffect`'s. Both "Unit 8" rows name `collectAsState`.
  - **Rewrite using human title** (9 rows): 8 keep a title pointer. The
    `lesson_coroutine_builders` row, and the `lesson_coroutine_scope_ownership` half of
    the `lesson_launched_effect` row, were dropped because the local fact was enough.
  - One pointer was added under a Bring-local row (**Cleanup, `NonCancellable` and
    Timeouts**), for 9 title pointers in total.
  - Every retained title was checked against the bundled curriculum, and every pointer
    can be removed without breaking its paragraph.
- **Shape inventory** S013–S017: all applied as listed. The remaining uses of "boundary"
  in Unit 11 are the engineering sense (the UI boundary, where an adapter lives) and were
  kept.

Technical claims: the edits keep existing claims and their version qualifications. Three
sentences now state a mechanism where a curriculum pointer stood in for it:

- A cancelled `LaunchedEffect` releases in its `finally` block as the coroutine unwinds.
  This matches the Lesson's own comparison table.
- `produceState`'s lifetime follows `LaunchedEffect`. The Lesson's ProduceState source
  shows it is implemented with one.
- Repeated-click policy belongs to the handler. The Lesson already said another click is
  free to start another child.

No source changed.

Concerns for later batches:

- `lesson_effect_key_failures` Senior paragraph 1 still says "with a current Kotlin/Compose
  compiler". The measured outcomes match the probe that `lesson_effect_keys_as_dependencies`
  attributes to compiler 2.4.10 and runtime 1.11.2. The wording was left alone because
  no finding names it and the probe record is not in the repository.
- The "configured runtime" observations are now attributed to Compose runtime 1.11.2,
  the version these Lessons already cite. The probes themselves are not in the repository.

### Batch 3 — Units 29 and 30 (2026-09-28)

Scope: `unit_koin_and_dependency_injection_in_kmp` and
`unit_choosing_a_dependency_injection_strategy`, findings LC-211 through LC-223. Every
finding was checked against the text at the implementation baseline (`d0e9b88`) before
editing. Both Units were unchanged since the audited commit, so no finding was already
resolved. The batch followed the revised Rule 11 (project-agnostic worked examples). Status:
**Rewrite complete**, then **Re-verified** after all nine Lessons and both Unit summaries
were re-read in full, in authored order.

Preserved: Unit and Lesson IDs and titles, topics, statuses, authored order, section depths,
Subtopic mappings, `relatedLessonIds` and sources. No other Unit changed. The
learning-question coverage report changed only in its learning-curriculum fingerprint;
the question fingerprint and every association and count are unchanged. Three intended
structural changes, each required by a finding:

- `lesson_koin_scopes_and_their_owners` SENIOR lost its second paragraph (LC-216).
- `lesson_one_graph_across_platforms` SENIOR lost its case-study paragraph and the
  "repository evidence" row of its comparison, which now has 2 rows (LC-218).
- `lesson_what_a_container_actually_buys` PRACTICAL comparison 1 went from 6 rows to 5 after
  the two lifetime rows were merged (LC-222).

Unlike Batches 1 and 2, code blocks changed. Every Unit 29 code example that reproduced
this application's own startup, modules, classes or graph was rewritten as one invented,
consistent example: a multiplatform order-tracking application. Each rewritten example
keeps the mechanism it teaches: the same Koin calls, the same `single`/`factory`/`viewModel`
structure, the same runtime-parameter split and positional-parameter hazard, and the same
shared-abstraction/host-binding arrangement.

| Finding | Resolution |
| --- | --- |
| LC-211 | The Unit summary and all five Lesson summaries now state the idea: Koin records construction decisions as definitions a shared graph can hold; startup is composition-root work and a module only groups; `single`/`factory` encode reuse separately from interface binding; a scope's lifetime comes from its owner; a ViewModel definition is a recipe while the store owner decides retention; shared versus host construction. "this codebase" removed. |
| LC-212 | The startup example is now `startDesktopApp()` listing `networkModule`, `dataModule`, `settingsModule`, `ordersModule` and `desktopPlatformModule`. The Practical section frames an invented four-host order-tracking application, with four shared modules and one platform module per host. "The grouping is deliberately read as evidence" became "How definitions are grouped is an organizational choice, not a rule". The build-engineering scope clause became "They are framework capabilities that need build configuration, and the example above does not enable them." "Current Koin" became "Koin 4.2". |
| LC-213 | Every DI Unit number removed. "Units 1 and 2 made construction explicit" now states that dependency injection makes construction explicit. "Unit 2's question" is stated as the reuse requirement, with a pointer to **One Instance, or a New One Each Time?**. "Unit 2's generic rule" became "The declaration encodes a reuse rule", with a pointer to **A Scope Is a Rule; an Owner Is a Lifetime**. "Unit 2's same-type ambiguity" is explained locally ("type alone cannot distinguish them… swapping the two arguments still compiles"), with a pointer to **Two Dependencies of the Same Type**. The Unit/Lesson collision is gone: the positional-parameter example now uses `orderId` and `itemId`. |
| LC-214 | "The second shape" became "The second version" (S184). "the service-locator shape from Unit 1" became "the repository is using the container as a service locator" (S185), followed by an optional pointer to **Asking For It, or Being Given It**. "still has the service-locator shape" became "is still acting as a service locator" (S186). The code comment now reads `// Hidden dependency: business code acting as a service locator`. |
| LC-215 | `AppearanceStateHolder` and `StudyProgressStateHolder` replaced by an invented `ThemeController` (the theme is applied above navigation and outlives the settings screen) and `SyncStatusHolder` (several screens alive at once must show the same sync progress). The `single`-by-requirement argument is unchanged. |
| LC-216 | The paragraph about the case-study graph's scopes was deleted, with no replacement. The preceding paragraph already states when a custom scope is worth creating. |
| LC-217 | The Senior section was rewritten as teaching. Paragraph 1: Koin's Compose integration resolves and forwards parameters but does not choose the owner; in Koin 4.2, `koinViewModel()` defaults its `viewModelStoreOwner` parameter to `LocalViewModelStoreOwner.current`, so the host or navigation integration in scope decides. Paragraph 2: ownership comes from the platform ViewModel contract; two requests share an instance only in the same store with the same key; destination-owned versus Activity-owned clearing; a definition or `koinViewModel()` call shows construction, not lifetime. All counts, "this application's navigation arrangement", "the shipped lifetime curriculum" and the re-verification instruction were removed. Core paragraph 2's "lifecycle curriculum" became a local statement plus a pointer to **The ViewModel Owner: Lifetime Is Not Persistence**. The code examples use invented `OrderListViewModel`, `OrderDetailViewModel` and `OrderItemViewModel`. The settings-screen `koinInject()` and preview-tolerant theme-wrapper cases are now described generically. |
| LC-218 | The worked example is now an invented `SettingsStorage` abstraction consumed by shared `SettingsRepository` and `ThemeController`, bound per host by `androidPlatformModule`, `iosPlatformModule`, `desktopPlatformModule` and `webPlatformModule`. The database paragraph describes an invented `OrderDatabase` bound per host, where only Android needs a `Context`. All `create…CurriculumDatabase()` identifiers and real module names were removed. The "repository evidence" row and the case-study-limits paragraph were deleted; the limits the example cannot justify now sit in the Practical caveat as a property of the example (Rule 11). "the next Unit owns that selection" became "…still decide the choice. **Three Projects, Three Answers** works through that decision for three concrete projects." |
| LC-219 | The Unit summary and all four Lesson summaries now describe the idea. For example, "A DI container automates or standardizes object-graph work; it adds no capability that manual construction lacks." |
| LC-220 | The Question ID, its deprecated status and the future-assessment note were removed entirely. The paragraph now keeps only the technical idea: the compile-time-versus-runtime framing keeps one useful axis (earlier discovery is paid for with build-time work), but as a split between brands it is too coarse. Identifier inventory row 28 is resolved. |
| LC-221 | "Unit 3's compiler Lesson and Unit 2's scope-owner Lesson carry the full argument" became pointers to **What the Dagger Compiler Actually Checked** and **A Scope Is a Rule; an Owner Is a Lifetime**, each saying what it adds. The paragraph's own `ReaderSession` example still carries the argument. |
| LC-222 | Both tables were rewritten so each cell states the observable difference in a phrase. "Lifetime complexity" and "Graph and lifetime complexity" were merged into "Lifetimes and graph boundaries". "Graph size and rate of change" became "A deep constructor change", and "Amount of assembly code" became "Assembly code the team writes". Table 2 keeps all seven dimensions. The two-table split by question (assembly and ownership; when and where the cost is paid) was kept. "entry-point shape" became "entry-point design" (S187). |
| LC-223 | The ecosystem-coverage disclaimer was deleted. "A learning application with shared Koin definitions…" became "An application with a shared Koin graph across Android, iOS, desktop and web…", and "keeps migration cost honest" became "applies to migration cost". |

The full re-read also made edits the ledger quotes did not list:

- Unit 29 Rule 11 coupling beyond the quoted evidence was also removed. This covers the
  `CurriculumImporter`/`LocalCurriculumRepository`/`CurriculumDatabase` binding example,
  `CurriculumDataInitializer`, `AppShellViewModel`, `TopicDetailViewModel`,
  `LearningLessonViewModel`, "A real shared module", "Assessment services" in the
  shared-construction table, and the route IDs `topicId`, `attemptId`, `unitId` and
  `lessonId`. All are this application's own identifiers, and all now come from the
  invented example.
- "Current Koin" / "current Koin guidance" / "its current compile-safety documentation"
  became "Koin 4.2", "Koin's KMP guidance" and "its compile-safety documentation".
- `lesson_when_should_a_graph_error_surface`: "Do not repeat the documentation's
  promotional phrase…" now opens with the fact.
- `lesson_three_projects_three_answers`: "Now test whether you can resist the obvious
  answer" became "The obvious answer is sometimes the wrong one", and "The correct lesson
  is not…" became "The aim is not…".
- `lesson_the_smallest_sufficient_strategy` (scope **None**): the summary, and S188 ("the
  same consumer shape" → "the same consumer constructor"). Core paragraph 1's "The
  architecture curriculum ended with proportionality" was audited as **Keep**, but it now
  states the rule and points to **How Much Architecture Does This Feature Need?**. The
  audit predates the contract clause that keeps the word *curriculum* out of Lesson prose.
- One "The useful question is" framing in `lesson_what_a_container_actually_buys` became
  "The difference is".

Inventory reconciliation for this batch:

- **Identifier inventory** row 28 (`di_framework_tradeoff_compile_vs_runtime`) is removed
  from learner prose. Rows 26–27 belong to other Units and are unchanged. A contextual
  search of every prose block, table cell, summary and code comment in both Units found
  no remaining curriculum identifiers, Unit numbers, course positions, product metadata or
  identifiers from this application's source.
- **Cross-reference inventory**, 14 rows: Unit 29 (10) and Unit 30 (4).
  - **Remove** (5 rows): all deleted — the Unit 29 summary's "earlier Units", the
    build-engineering clause, "the shipped lifetime curriculum", Unit 30's "does not
    reactivate or edit that Question" and "outside this Unit's four taught strategies".
  - **Bring required context local** (3 rows, all Unit 29): each now states the fact in
    the Lesson. Two add an optional title pointer (**One Instance, or a New One Each
    Time?**, **A Scope Is a Rule; an Owner Is a Lifetime**).
  - **Rewrite using human title** (5 rows): all keep a title pointer — **Asking For It, or
    Being Given It**; **The ViewModel Owner: Lifetime Is Not Persistence**; **Two
    Dependencies of the Same Type**; **Three Projects, Three Answers**; and **What the
    Dagger Compiler Actually Checked** with **A Scope Is a Rule; an Owner Is a Lifetime**.
  - **Keep** (1 row): rewritten with a title, as noted above.
  - That makes 9 title pointers across the two Units. Every title was checked against the
    bundled curriculum, and every pointer can be removed without breaking its paragraph.
- **Shape inventory** S184–S188 and the `lesson_koin_definitions_and_reuse` code-comment
  row are applied as listed. No other "shape" remains in either Unit. The remaining uses of
  "boundary" are engineering uses (resolution, integration, host and platform boundaries).

Technical claims: the edits keep existing DI semantics. They cover `single` versus
`factory`, scope ownership and `close()` effects, construction versus `ViewModelStoreOwner`,
runtime parameters, shared versus host bindings, the manual/Dagger/Hilt/Koin distinctions,
and build-time versus runtime detection. Three sentences state a mechanism more explicitly
than before:

- In Koin 4.2, `koinViewModel()` takes the owner as a parameter defaulting to
  `LocalViewModelStoreOwner.current`. This is the public signature in
  koin-compose-viewmodel 4.2.2, the version the project pins, and it sits under the
  Lesson's existing "Koin 4.2: ViewModel in Compose" source.
- Instances are shared per store and key; a destination owner clears when its entry
  leaves the back stack, and an Activity owner when the Activity finishes. Both are covered
  by the Lesson's existing "Android Developers: ViewModel overview" source.
- With the classic DSL, a class's new dependency changes only that class's definition. This
  restates the old cell ("definitions centralize construction recipes").

No source changed.

Test change: `BundledLearningCurriculumTest.koinUnitProtectsModernCapabilitiesAndRepositoryEvidence`
asserted the exact text this batch removed: "service-locator shape", "no custom Koin
scopes", "20 `koinViewModel()` resolutions", and this application's startup and storage
identifiers. It is renamed `koinUnitProtectsModernCapabilitiesAndProjectAgnosticExamples`.
It now asserts the same teaching points in their new wording (the service-locator code
comment, the default `LocalViewModelStoreOwner.current` owner, the per-host platform
modules, `SettingsStorage` and `expect val platformModule`). It also asserts that the
removed application identifiers, the ViewModel resolution count and "case study" do not
return. The first `:shared:jvmTest` run failed on this test alone (1 of 1681); after the
update, the full task passed.

Concerns for later batches:

- `lesson_one_graph_across_platforms` SENIOR comparison: "Koin Compose lists Android, iOS
  and desktop as full, web as experimental" is a platform-support claim without a version,
  and support tables date quickly. It was kept as authored; it should be re-checked against
  the "Koin 4.2: Koin for Compose" source on the next material edit.

### Batch 4 — Units 23 and 24 (2026-09-28)

Scope: `unit_responsibility_models_mvp_mvvm_mvi` and `unit_state_events_lifetime_and_selection`,
findings LC-170 through LC-184, plus the LC-085 title observation. Every finding was
checked against the text at the implementation baseline (`1963922`) before editing. Both
Units were unchanged since the audited commit, so no finding was already resolved. The
batch followed the revised Rule 11 (project-agnostic worked examples) and Rule 12. Status:
**Rewrite complete**, then **Re-verified** after both Unit summaries and all nine Lessons
were re-read in full, in authored order.

Preserved: Unit and Lesson IDs, topics, statuses, authored order, section depths, Subtopic
mappings, `relatedLessonIds`, sources, and every comparison's headers and row count. No
other Unit changed. The learning-question coverage report changed only in its
learning-curriculum fingerprint and in the heading of the retitled Lesson; the question
fingerprint and every association and count are unchanged. Two intended structural
changes:

- `lesson_delivery_guarantees` was retitled (see below).
- `lesson_classifying_a_real_architecture` SENIOR went from 6 blocks to 5. Paragraphs 3 and
  4 (the two scope limits) were merged into one paragraph, as LC-176 directed.

Two code blocks changed, both in a prose-like string only: "what the reader did" and "what
the reader intended" became "what the user did/intended" (`lesson_one_screen_five_questions`
PRACTICAL code 1; `lesson_mvvm_observed_state` CORE code 2).

**Lesson title.** `lesson_delivery_guarantees` was **What Guarantee Does This Occurrence
Need?** and is now **Lost, Repeated or Acknowledged: Stating an Occurrence's Guarantee**. The
new title names the three outcomes the Lesson decides between and its role, stating the
guarantee before any owner or mechanism. It shares no leading words with the Compose Lesson
**What Delivery Guarantee Does This Occurrence Need?**, which keeps its title. No learner
prose cited the old title, so no references needed updating. Two Lessons in this batch now
cite the new title (`lesson_mvi_intent_and_reduction`, `lesson_state_or_occurrence`), and
`lesson_delivery_guarantees` cites the Compose Lesson by its title; both titles are now
unambiguous in a sentence. The only other occurrence of the old title outside this audit
was the title assertion in `BundledLearningCurriculumTest`, which was updated. The planning
documents (`architecture-learning-blueprint.md`, `architecture-units-1-6-plan.md`) still
record the title the Lesson was planned under; they are authoring history, not learner
prose, and were left alone. The contract's own example of the ambiguous pair (Rule 12) was
also left alone, because it illustrates the rule.

| Finding | Resolution |
| --- | --- |
| LC-085 | Resolved on the Unit 24 side by the retitle above. The Compose Lesson's title is unchanged. |
| LC-170 | The Unit summary now states the idea: MVP, MVVM and MVI arrange responsibilities around the same five questions (state location, mutation, owner's knowledge of the UI, input path, output path), no normative specification governs the names, and a label summarises the answers only when both sides agree on it. The course recap, the running-example rationale and the learning objective were removed. |
| LC-171 | `lesson_one_screen_five_questions` now stands on the instrument itself. What remains is the terminology warning (with the Fowler quotation), the five questions, the sixth consequence, the practice screen and its six answers, the three-teams table, MVC in one paragraph, and the Senior history and lifetime points. Removed: what earlier Units taught, why names come now, why one screen recurs, "the skill this unit is for", the preview of later Lessons, and why MVC gets no implementation. The summary states the idea. The audit's **Keep** row ("One hypothetical screen runs through this lesson and the three that follow") was also removed, because it narrates the course's design; the example is now introduced as "a practice-configuration screen in a quiz application". |
| LC-172 | The summary ends on "real strengths as well as real costs". The "strawman" clause, "the unit's standing rule", "what this lesson teaches", "deliberately as little code", the lifecycle-curriculum scope clause, "the only honest way to teach" and the "next lesson" sentence were removed. The screen's requirements and the five questions are now stated in the Lesson. "A boundary in the foundations unit's exact sense" became "**The contract is owned by its consumer.**" MVP's strengths and costs are unchanged. |
| LC-173 | The Lesson now states the arrangement directly. The UI observes current state; the owner holds no view contract; input reaches the owner through calls; output is observable state; and the dependency direction defines the pattern, not the class name. "They have already built it", "this curriculum's stack", "the coroutines curriculum owns", "the foundations unit" (twice), "this curriculum defines" and "this curriculum has been careful" were removed. `StateFlow` is described locally in one clause. There is one pointer to **What a Screen State Holder Is Responsible For**, and the Lesson reads without it. |
| LC-174 | Positional and ownership narration was removed. The opening states the screen's requirements and the classification questions locally. The "effect" clarification is now a fact about Compose: `LaunchedEffect`, `DisposableEffect` and `SideEffect` already use the word, so make it explicit when "effect" means a one-off output. Pointers: **Lost, Repeated or Acknowledged: Stating an Occurrence's Guarantee**, **Classifying What a Real Codebase Actually Does** and **Stability and Skipping**. |
| LC-175 | All five audited "honest" framings were removed or restated: "The honest first answer", "the only honest way", "counted honestly" (now "The ceremony is real, and it can be counted…"), "The honest statement runs the other way" (now "The effect can even run the other way"), and "The honest answer is that it scales…" (now "It scales…"). No "honest(ly)" remains in either Unit. |
| LC-176 | Senior paragraphs 1–2 no longer use this application's classes. `LessonScrollStateReducer` became an invented `ToolbarScrollReducer` in a feed screen; it is still not pure, because it accumulates scroll distance. `PracticeBuilderViewModel`/`PracticeBuilderUiState` became an invented checkout screen, `CheckoutViewModel`/`CheckoutUiState`, which illustrates design A. The bold evidence-validity disclaimer became one plain sentence: one reducer shows that techniques travel independently of labels, and it does not make the application MVI. The two scope facts (presentation-only models; not alternatives to Clean Architecture) are now one paragraph without course narration. The summary and Core paragraphs 1–2 no longer call the Lesson "the unit's payoff" or "the skill this unit is actually for", and no longer mention "the previous four units". |
| LC-177 | All 13 Unit 23 "shape" occurrences were applied per the inventory (see reconciliation). No "shape" remains in Unit 23. |
| LC-178 | The Unit summary now states the engineering problem. Starting from a requirement with nothing built: represent something true or something that happened; what delivery guarantee it needs; whose lifetime matches; how much structure the feature earns; and a transient mechanism cannot promise handling. The five-Unit recap, the Compose-curriculum hand-off, "somebody else's curriculum" and the learning objective were removed. The Keep occurrence S136 ("a repository to shape") is retained in a sentence about architecture work in general. |
| LC-179 | The opening recap is now one sentence posing the question. Six curriculum pointers were replaced. Three are pointers by title: **Rendering State and Running a Transient Effect**, **Choosing an Owner From the Lifetime the Requirement Needs** and **Lost, Repeated or Acknowledged: Stating an Occurrence's Guarantee**. One is a pointer by title plus a local fact: **Which Source Is Authoritative?**. One is only a local fact: a late observer reads the latest value immediately, as a `StateFlow` collector does. The requirement table, the duplicate-celebration failure and the lossy-compression argument are intact. The Lesson is 2.5% shorter rather than the estimated 10–15%, because the local facts and title pointers replaced most of the narration rather than just deleting it. |
| LC-180 | "…when they open the borrowed-items list…" now reads "…when they next open the app…". The same collision appeared elsewhere in the Lesson and was fixed there too: Practical paragraph 6's borrowing-request and borrowing-account examples, and Senior paragraph 5's "list of loans" and "studied lessons". They are now practice-session and account examples, so the Lesson keeps one domain. |
| LC-181 | The summary and opening now start from the engineering question: a transient in-memory mechanism can deliver to a present consumer but cannot promise handling. The Compose-curriculum hand-off is gone. The replay and emission facts are stated locally (a `SharedFlow` replay cache or a `StateFlow`'s current value only changes what a late subscriber can recover). "The transient one the Compose curriculum already supplies" is now a local statement (scroll if the list is on screen, otherwise drop it) plus a pointer to **What Delivery Guarantee Does This Occurrence Need?**. The persistence/lifecycle-curricula clause became "such as a local database". Senior paragraph 4 now makes one point: event buses, durable queues, delivery protocols and outboxes are separate design problems, and the decision is complete once the missing guarantee, the required fact and its owner can be named. "Is honest" became "is accurate". The example opens "Consider a library application…" rather than "In the library application…". Retitled as above. |
| LC-182 | Practical paragraph 5 no longer reports facts about this application. `AppCoroutineScope` as this application's type, "Five state holders receive it", "every repository in that application… across every interface and implementation" and "local-first with no network layer" are gone. It is now a hypothetical: if the data layer exposes only one-shot suspending reads, nothing is observable for destination-scoped owners, so a projection several destinations need must have an owner whose lifetime spans them, while the database stays the source of truth. Rung three's example was also this application's own model ("the studied-lesson and progress projections"); it is now the Lesson's library domain ("the loan count and overdue status several screens show"). The ladder, both over- and under-scoping failures, and "application scope is not the generic answer" are unchanged. |
| LC-183 | Curriculum and positional narration was removed from Core paragraphs 1 and 4, Practical paragraphs 3 and 8, the callout, the comparison and Senior paragraphs 2–4. The background-work rung now names "a scheduler such as WorkManager". The table cell is now "A scheduler such as WorkManager, with its constraints, deferral and retry policy". The destination-group rung now says its expression depends on the navigation library. The two Android guidance quotations are kept. "The honest answer" became "the right answer" and "The honest conclusion is therefore" became "So". Senior paragraph 2 points to **The ViewModel Owner: Lifetime Is Not Persistence**. |
| LC-184 | Informal Unit and Lesson nicknames were replaced by the decisions themselves: "the four conditions" are listed (reuse, orchestration, a rule that belongs to neither screen nor store, enough decision-making to want one answer); "from the guarantee lesson" became "must be acknowledged once and survive the process"; "from the lifetime lesson" became "an owner whose lifetime exceeds the screen's — … outside the process entirely, such as a scheduler"; "what the pattern unit taught" became "a pattern name summarises decisions rather than making them"; "the owner-scoped-work test" is stated. The five-question closing list is kept, introduced by the questions themselves rather than by "the foundations unit… the whole curriculum". "Applying it honestly" and "one honest argument" were re-voiced. |

The full re-read also made edits the ledger quotes did not list:

- The Unit 23 practice screen and the `lesson_state_or_occurrence` practice session called
  the application's user "the reader", which reads as the person reading the Lesson. Those
  uses became "the user". The library Lessons keep "reader" as the library's own role (a
  patron), which is domain vocabulary.
- Lesson summaries that were outlines or recaps now state the idea:
  `lesson_one_screen_five_questions`, `lesson_mvvm_observed_state`,
  `lesson_classifying_a_real_architecture`, `lesson_state_or_occurrence`,
  `lesson_delivery_guarantees` and `lesson_smallest_sufficient_architecture`.
- Lessons that opened from another Lesson's example now state the example locally.
  `lesson_mvp_view_contract`, `lesson_mvvm_observed_state` and
  `lesson_mvi_intent_and_reduction` each restate the practice screen's requirements or the
  MVP starting point. Three library-domain openings changed from "the library application"
  to "Consider a library application".
- Rhetorical framings were trimmed where they clustered: several "worth…" constructions,
  "The sharpest thing in this lesson", "Finally, the reasoning trap this lesson exists to
  remove", and "One last framing". Stacked bold spans were separated.
- `lesson_state_or_occurrence` Senior paragraph 4 now names event sourcing ("an architecture
  of its own, event sourcing") where it said the architecture was one "this curriculum does
  not teach".

Inventory reconciliation for this batch:

- **Identifier inventory**: no Unit 23–24 rows. A contextual search of every prose block,
  table cell, summary and code comment in both Units found no curriculum identifiers, Unit
  numbers, course positions, "curriculum", "this unit/lesson", or identifiers from this
  application's source.
- **Cross-reference inventory**, 67 rows: Unit 23 (37) and Unit 24 (30).
  - **Remove** (39 rows: 22 and 17): all deleted. Two of the deleted Unit 23 rows left a
    local restatement in their place: the practice screen's requirements, in the MVVM and
    MVI openings.
  - **Bring required context local** (5 rows): each now states the fact.
    - Unit 23: ownership as "who may write a value, not who reads it"; the MVP opening's
      screen requirements; the consumer-owned contract.
    - Unit 24: the replay and emission facts; the reminder setting described in place.
  - **Rewrite using human title** (22 rows: 11 and 11). 10 keep a title pointer.
    - Unit 23 keeps 3: **Lost, Repeated or Acknowledged: Stating an Occurrence's
      Guarantee**; **Classifying What a Real Codebase Actually Does**; **Stability and
      Skipping**.
    - Unit 24 keeps 7: **Rendering State and Running a Transient Effect**; **Choosing an Owner
      From the Lifetime the Requirement Needs**; **Which Source Is Authoritative?**; **Lost,
      Repeated or Acknowledged: Stating an Occurrence's Guarantee**; **What Delivery Guarantee
      Does This Occurrence Need?**; **Choosing a Stream Abstraction by Delivery Guarantees**;
      **The ViewModel Owner: Lifetime Is Not Persistence**.
    - The other 12 were dropped because the local statement was enough. Examples: "the
      state-holder unit worked all of that out", and "the data-ownership unit describes"
      (now "when the data layer exposes observable reads").
  - **Keep** (1 row, Unit 23): removed, as recorded under LC-171.
  - Two pointers were added under Remove rows. Both are optional further reading, and the
    audit's direction and the batch instructions asked for them. They are **What a Screen
    State Holder Is Responsible For** (MVVM) and **Choosing an Owner From the Lifetime the
    Requirement Needs** (`lesson_delivery_guarantees` Practical paragraph 6).
  - That makes 12 title pointers across the two Units. Every title was checked against the
    bundled curriculum. Every pointer can be deleted without breaking its paragraph.
- **Shape inventory**, Unit 23 (S123–S135): all applied as listed.
  - S123 became "How input is modelled (question 4)". S124 became "the answers are
    immediately different". S125 became "the arrangement". S126 was deleted.
  - S127 became "has an MVVM arrangement". S128 became "whether output is pushed or
    published". S129 was deleted with its clause.
  - S130, S133 and S134 became "input model", and S135 became "output model". S131 became
    "by its label". S132 became "adopted a syntax".
- **Shape inventory**, Unit 24 (S136–S145):
  - S136, S144 and S145 were kept.
  - S137 became "the kind of model". S138 became "Its worked example". S139 became "the
    requirement changes". S140 became "a data layer with no observable reads". S141 became
    "Change that". S142 was deleted with the navigation-curriculum sentence.
  - S143 became "whose requirements are that small", not the listed "requirements like
    these". The sentence comes before the examples, so "these" would have had no antecedent.
  - Only the three Keep uses of "shape" remain in the two Units. The remaining uses of
    "boundary" are engineering uses (the repository boundary, consumer-owned contract,
    the UI layer).

Technical claims: the edits keep the existing architecture models. They cover
responsibility-first classification, MVP's view-contract consequences, MVVM's observed-state
arrangement and MVI's input-as-value, reduction and current-state model. They also cover the
state/occurrence distinction, the five delivery-guarantee questions and the "exactly once"
decomposition, the lifetime ladder, and smallest-sufficient architecture with its
uniformity counter-argument. Several sentences now state a mechanism where a curriculum
pointer stood in for it:

- A `StateFlow` always holds one current value, and a late collector reads it immediately.
  A `SharedFlow` replay cache only changes what a late subscriber can recover. Both are
  covered by the Lessons' existing SharedFlow and UI-layer sources.
- Work that must survive the process belongs to a scheduler such as WorkManager, which
  records the work durably and runs it under constraints and a retry policy. This is covered
  by the existing "Background work overview" source.
- The application-scope example keeps the original design (a dedicated type wrapping a
  scope with a `SupervisorJob`), now presented as an example rather than as this
  application's code.
- The domain-layer conditions (reuse, orchestration, a rule owned by neither screen nor
  store, enough decision-making) restate the conditions in **When Does Another Layer Earn Its
  Existence?**. They are covered by the existing "Domain layer" source.

No source changed. No claim was found that needed `TECHNICAL REVIEW NEEDED`.

Test change: `BundledLearningCurriculumTest` asserts the Unit 24 Lesson titles in order, so
the retitled entry was updated. A comment in
`theSynthesisUnitLinksBackwardsAndClosesTheLoopOverEveryEarlierArchitectureUnit` said the
application-scope rung "turns on this repository having no observable source". It now reads
"a data layer", matching the hypothetical. The `:shared:jvmTest` run passed (1 681 tests,
0 failures).

Lesson prose length (learner-facing words, code excluded), before → after:

| Lesson | Before | After | Change |
| --- | ---: | ---: | ---: |
| **One Screen, Five Questions** | 2 290 | 1 990 | −13% |
| **MVP: an Explicit View Contract** | 1 954 | 1 892 | −3% |
| **MVVM: a UI That Observes State** | 1 844 | 1 678 | −9% |
| **MVI: Intent, Reduction and One Current State** | 2 006 | 1 960 | −2% |
| **Classifying What a Real Codebase Actually Does** | 2 080 | 1 982 | −5% |
| **Is This State, or Is It Something That Happened?** | 2 423 | 2 363 | −2% |
| **Lost, Repeated or Acknowledged: Stating an Occurrence's Guarantee** | 2 292 | 2 254 | −2% |
| **Choosing an Owner From the Lifetime the Requirement Needs** | 2 296 | 2 185 | −5% |
| **How Much Architecture Does This Feature Need?** | 2 401 | 2 387 | −1% |

Unit summaries: 156 → 98 words (Unit 23) and 162 → 136 words (Unit 24). The reductions are
smaller than the audit's scaffolding estimates, because most removed narration was replaced by
the local fact it stood in for. The largest single cut is the course-design narration in
**One Screen, Five Questions**.

Concerns for later batches:

- Other Units point at this batch's material in curriculum terms rather than by title.
  A search of learner prose found these:
  - Unit 12 (`lesson_transient_ui_effects`, `lesson_transient_effect_delivery`,
    `lesson_choosing_a_compose_mechanism`): "the architecture curriculum owns it" for the
    delivery-owner decision.
  - Unit 20 (`lesson_state_holder_responsibility`, `lesson_owner_scoped_work`): "the closing
    unit" for the lifetime ladder.
  - Units 25–26: "the architecture curriculum" for proportionality and owner selection.
  These belong to Batches 5, 6 and 7. When they are rewritten, their natural title targets
  are **Lost, Repeated or Acknowledged: Stating an Occurrence's Guarantee**, **Choosing an
  Owner From the Lifetime the Requirement Needs** and **How Much Architecture Does This
  Feature Need?**. No learner prose outside this batch cites the old title.
