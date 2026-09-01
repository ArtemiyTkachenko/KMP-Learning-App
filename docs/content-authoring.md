# Interview Content Authoring Guide

## Purpose and Scope

This guide defines the editorial standard for Android interview-preparation
content in this project. It is intended for human contributors, AI-assisted
drafting, question-bank maintenance, and PR review.

The curriculum focuses on technical and theory preparation for modern senior
Android interviews. Behavioral interview questions, general DSA/LeetCode
exercises, backend interview curricula, runtime AI-generated questions, and
intentionally adversarial trick-test design are out of scope unless the roadmap
changes.

This document defines authoring quality expectations. It does not define
application storage, import, persistence, serialization, or runtime behavior.

This guide defines the rules. `docs/question-authoring-playbook.md` documents the
method for meeting them: where plausible distractors come from, the anti-cue
audits a question must survive, and the checks to run before opening a PR. Where
the two disagree, this guide wins.

`docs/question-bank-coverage.md` records the current *state* of the bank: what
each subtopic already holds, which gaps are real and which are deliberate, and
the audit baselines a new batch must not degrade. Read it before planning an
expansion so the coverage review does not have to be repeated, and regenerate
its tables in the same PR that changes the bank.

## Model Context

The content model uses the hierarchy:

1. Topic
2. Subtopic
3. Question

The Kotlin model stores these as flat collections linked by stable IDs. A
question has `id`, `topicId`, `subtopicId`, `text`, `answers`,
`selectionMode`, `correctAnswerIds`, `explanation`, `sources`, and `status`.

`SourceReference` contains a human-readable `title` and the actual `url`.
`ContentStatus` supports `ACTIVE` and `DEPRECATED`.

## Question Wording

- Test one identifiable concept where practical.
- Keep wording concise, technically precise, and answerable.
- Avoid deliberate ambiguity and wording traps.
- Avoid obscure trivia unless it is genuinely relevant to senior Android work.
- Avoid unnecessary product-specific, company-specific, or team-specific
  assumptions.
- Make version, API-level, platform, or library assumptions explicit when they
  materially affect the correct answer.
- If `selectionMode` is `MULTIPLE`, the prompt must clearly say so, for example
  "Select all that apply."
- Prefer scenarios that reflect real Android engineering decisions over
  memorization-only prompts.

## Question Interview Level

This section is the authoritative editorial contract for the upcoming
Question-level interview-depth field. The field is not part of the current
Kotlin or JSON model yet; do not add a level key to authored curriculum until
the schema adopts it.

Question interview level describes **the depth and kind of technical reasoning
required by the question**. It does not claim how difficult every candidate
will find it and does not grade the person answering it. A foundational
contract can be unfamiliar to one candidate, while a mechanism classified as
advanced can be routine to someone who works with it every day. Classify the
question, not the learner.

The exact authored values are:

- `FOUNDATION`: understand, recognize, or predict a documented contract;
- `APPLIED`: apply known behavior to a realistic engineering situation;
- `ADVANCED`: reason through deeper mechanisms, interacting constraints,
  failure modes, architecture, or non-obvious trade-offs.

Use Foundation, Applied, and Advanced in prose. Do not create alternate stored
values or treat this dimension as a generic difficulty score.

### Comparison

The dimensions below are classification signals, not a point system. Editorial
judgment is still required.

| Dimension | `FOUNDATION` | `APPLIED` | `ADVANCED` |
| --- | --- | --- | --- |
| Primary task | Understand or predict a contract | Apply it to a realistic case | Reason across mechanisms, constraints, or trade-offs |
| Mechanism depth | Usually one direct mechanism | One or a few known mechanisms used in context | Multiple interacting mechanisms or a non-obvious consequence |
| Scenario role | Optional or contextual | Material to selecting the answer | Carries several meaningful constraints, edges, or system interactions |
| Interacting constraints | Usually one | A direct practical set | Several constraints materially affect correctness |
| Trade-off depth | Little or none | Choose by one or a few practical requirements | Compare viable alternatives across several consequences |
| Failure-mode depth | Direct documented consequence | Diagnose a realistic misuse | Trace a subtle interaction, ordering, or boundary failure |
| Typical form | Identify, distinguish, explain, predict | Choose, apply, diagnose, place responsibility | Trace, evaluate, design, or diagnose under interacting constraints |
| Obscure trivia | Never raises the level | Never raises the level | Never raises the level |

### `FOUNDATION`

A `FOUNDATION` question tests whether the learner understands the essential
documented behavior, contract, vocabulary, or mechanism needed to reason about
the subject correctly. Typical reasoning includes identifying, distinguishing,
recognizing, explaining a core mechanism, predicting direct documented
behavior, or choosing between clearly different concepts.

A question is likely `FOUNDATION` when most of these are true:

- one primary documented concept determines the answer;
- independent constraints do not need to be combined;
- the answer follows directly from an API, language, or platform contract;
- a scenario mainly provides context rather than changing the decision;
- there is little meaningful trade-off analysis;
- any failure is a direct consequence of the documented behavior;
- a technically correct explanation can stay focused on one mechanism.

`FOUNDATION` is not a trivia bucket and is not limited to dictionary definitions
or API-name recall. Asking for an obscure constant or internal class is poor
foundation content unless that fact matters to professional Android work.
Asking why a `ViewModel` can survive configuration change but cannot by itself
restore state after process death tests a useful behavioral model.

### `APPLIED`

An `APPLIED` question requires the learner to use one or more known mechanisms
in a realistic engineering situation. The underlying APIs may already be
familiar; the assessed reasoning is deciding which mechanism fits, why it fits,
where ownership belongs, or what caused a practical bug.

A question is likely `APPLIED` when most of these are true:

- it contains a realistic engineering scenario or constraint;
- definitions alone are insufficient;
- documented behavior must be connected to the scenario;
- several options may be valid technologies, but one best fits the stated
  requirements;
- a practical symptom must be connected to its most likely mechanism or fix;
- the answer must explain why an implementation choice fits;
- the explanation follows mechanism -> scenario -> decision.

Typical examples include choosing state ownership for data with different
lifetimes, selecting `supervisorScope` for independent sibling work, choosing
persistence for observable data across process death, avoiding repeated Compose
work whose inputs rarely change, or retrying only transient network failures
without duplicating side effects. These are deeper because the contract must be
applied, not because their stems are longer.

### `ADVANCED`

An `ADVANCED` question requires deeper reasoning about non-obvious mechanisms,
interacting constraints, subtle failure modes, concurrency, lifecycle edges,
architectural boundaries, system behavior, or material trade-offs. The answer
commonly depends on combining several facts or tracing why the system behaves
as it does beneath the obvious API surface.

A question is likely `ADVANCED` when several of these are true:

- more than one independent technical constraint materially affects the answer;
- the learner must reason through interacting mechanisms rather than recall one
  contract;
- the scenario contains a subtle but realistic failure mode;
- correctness depends on concurrency, lifecycle timing, ownership, ordering,
  cancellation, consistency, performance, or operating-system behavior;
- multiple options are viable choices with real trade-offs;
- explicit requirements, rather than a universal best practice, determine the
  correct decision;
- the learner must trace why alternatives fail under the stated constraints;
- the question resembles an architecture or system-design discussion while
  remaining objectively answerable in the assessment format.

**Obscurity is not depth.** An uncommon method name, rare flag, historical API
detail, niche library artifact, or undocumented fact does not make a question
`ADVANCED`. A direct obscure fact can still require only foundational reasoning.
Conversely, cancellation propagation can be advanced even when every API is
common, because several mechanisms must be traced together.

`ADVANCED` also does not mean long, vague, debatable, or trick-based. A concise
question can require deep reasoning, while a long scenario can still ask for
one direct property. Never raise apparent level with irrelevant details, double
negatives, unfamiliar wording, or parsing difficulty. When alternatives are
genuinely viable, state enough requirements to make one answer, or the intended
set for `MULTIPLE`, defensibly correct. Complexity belongs in the engineering
problem, not in ambiguity.

### The Primary-Reasoning Rule

Classify according to **the deepest reasoning actually required to identify and
justify the correct answer**, not every fact mentioned in the stem or
explanation.

A WorkManager scenario might mention process death, connectivity, and retries,
but if the options can be resolved solely by knowing that WorkManager supports
network constraints, it may still be `FOUNDATION`. Mentioning several concepts
does not prove that they interact in the required reasoning.

Use this minimum-sufficient-knowledge test:

> What is the minimum technical reasoning needed to eliminate the distractors
> and justify the correct answer?

- one direct documented contract -> likely `FOUNDATION`;
- known behavior applied to the scenario -> likely `APPLIED`;
- combined mechanisms or constraints, or traced trade-offs or failure modes ->
  likely `ADVANCED`.

Apply this test to the real answer set. An architectural-sounding stem with
three obviously unrelated distractors does not successfully test advanced
reasoning. Improve the distractors instead of assigning `ADVANCED` based on the
stem's tone.

Explanation depth is not question depth. Classify what is required before the
explanation is shown. A `FOUNDATION` explanation may teach deeper context
without raising the question's level. A shallow explanation for an `ADVANCED`
question is an explanation defect, not evidence for lowering the level.

### Mechanism, Scenario, and Trade-Off Signals

Use three related questions during classification:

1. **Mechanism:** Is one direct contract sufficient, must it be used in context,
   or must several mechanisms and consequences be traced together?
2. **Scenario:** Is the setup merely contextual, does it materially select a
   known mechanism, or does it introduce several meaningful constraints and
   edges?
3. **Trade-off:** Is there no real choice, a direct choice under a few
   requirements, or a comparison of viable alternatives across several
   consequences?

These dimensions can point in different directions. They are signals for the
primary-reasoning judgment, never a mechanical scorecard or point total.

### Boundary Rules

**Foundation versus Applied.** Use `FOUNDATION` when the scenario is decorative
and one documented behavior directly supplies the answer. Use `APPLIED` when
details in the scenario materially determine which mechanism or ownership
choice fits. "Which API restores small UI state?" is foundational. Dividing a
form's small user-entered state from a large reloadable server response requires
applying that contract and is applied.

**Applied versus Advanced.** Use `APPLIED` when a known mechanism is selected
under a realistic but relatively direct set of requirements. Use `ADVANCED`
when several mechanisms or constraints interact, substantial trade-offs among
viable choices must be evaluated, or a subtle failure must be traced. Choosing
a Room transaction for atomic writes is applied; reasoning through an
offline-first write involving a local transaction, optimistic UI, retry, server
conflict, and idempotency under explicit requirements is advanced.

**Deep fact versus advanced reasoning.** Recalling which internal class
implements an API may be obscure, but it is not advanced unless the answer
requires reasoning beyond that fact. Explaining behavior that emerges when two
common mechanisms interact may be advanced. Depth, not rarity, controls the
level.

**Debugging.** Debugging is not automatically advanced. A symptom that maps
directly to one documented misuse can be foundation. Diagnosing a realistic
symptom and choosing the matching correction is usually applied. Tracing a bug
caused by interacting lifecycle, concurrency, state, or system constraints can
be advanced.

**System design.** System-design language is not automatically advanced.
Placing one dependency in the layer that owns its lifetime may be applied. An
advanced design question needs several explicit constraints, such as offline
behavior, consistency, lifecycle, background execution, failure recovery,
concurrency, modularity, or testability, while remaining bounded enough for a
deterministic answer.

**Selection mode.** `SINGLE` and `MULTIPLE` are interaction semantics, not level
signals. A foundation question can ask for all documented StateFlow properties.
An advanced question can have one correct architectural choice. Neither the
number of answers nor the number of correct options raises the level.

**Topic and familiarity.** Topic, interview frequency, and learner seniority do
not determine level. Every major Topic can contain all three values. The whole
bank targets senior Android preparation, and foundational contracts remain
essential at that scope. A common cancellation scenario can be advanced; an
uncommon direct API fact can remain foundation. Frequency informs content
priority, not interview level.

### Same-Concept Progressions

These progressions demonstrate how the required reasoning changes while the
underlying subject stays the same.

#### Coroutines and Structured Concurrency

| Level | Question concept | Why |
| --- | --- | --- |
| `FOUNDATION` | What happens to child coroutines when their ordinary parent `Job` is cancelled? | One direct structured-concurrency contract determines the answer. |
| `APPLIED` | Several independent child requests run together, and one failure must not cancel its siblings. Which structured-concurrency mechanism fits? | Known failure-propagation behavior must be applied to a realistic requirement. |
| `ADVANCED` | Nested scopes combine supervision, `async`, and an awaited child failure. Under the stated parent/child structure, which siblings remain active and where is the exception observed? | The learner must trace several interacting propagation and observation rules. |

#### Lifecycle and State

| Level | Question concept | Why |
| --- | --- | --- |
| `FOUNDATION` | Which mechanism can restore a small ViewModel screen parameter after process recreation? | The `SavedStateHandle` contract directly supplies the answer. |
| `APPLIED` | A multi-step form must survive configuration and process recreation, while a large cached response can be reloaded. Which state belongs in saved state and which belongs in the repository? | Different state must be assigned according to lifetime and size constraints. |
| `ADVANCED` | A screen combines `SavedStateHandle`, repository persistence, navigation state, and process recreation. Given explicit source-of-truth and conflict rules, which source owns each value and how are conflicts resolved? | Several ownership and restoration mechanisms must be reconciled. |

#### Compose State and Recomposition

| Level | Question concept | Why |
| --- | --- | --- |
| `FOUNDATION` | What happens when observable Compose state read by a composable changes? | One documented state-read and recomposition contract determines the answer. |
| `APPLIED` | A composable repeats an expensive transformation on every recomposition although its input rarely changes. Which keyed memoization or derived-state placement addresses the stated cause? | The learner applies known state tools to a concrete performance symptom. |
| `ADVANCED` | A high-frequency source still triggers expensive recomposition despite `derivedStateOf`. Under explicit update-frequency and UI-consistency requirements, which state reads and invalidation boundaries should move? | State observation, invalidation boundaries, and performance constraints interact. |

### Examples Across the Curriculum

These are concise concepts, not complete four-answer questions. They show that
no Topic owns a particular level.

| Topic | Level | Question concept or scenario | Why the level applies |
| --- | --- | --- | --- |
| Android Platform / Application Model | `FOUNDATION` | Why does an in-memory object not survive system-initiated process death? | The process-lifetime contract is sufficient. |
| Lifecycle / State | `APPLIED` | Divide small restorable form state, destination state, and reloadable results among their owners. | Scenario details determine ownership. |
| Compose / UI | `ADVANCED` | Rework state reads and invalidation boundaries for a high-frequency source under explicit consistency constraints. | Several state and performance mechanisms interact. |
| Kotlin | `FOUNDATION` | Does a data class `copy()` deep-copy mutable objects referenced by its properties? | One language contract answers the question. |
| Coroutines / Flow | `ADVANCED` | Trace cancellation and exception observation through nested supervised and ordinary scopes. | Propagation rules interact. |
| Architecture | `APPLIED` | Decide whether UI event consumption belongs in a screen state holder or repository for the stated lifetime. | A known ownership model must be applied. |
| Persistence / Room | `FOUNDATION` | What atomicity guarantee does a Room transaction provide for its enclosed writes? | One transaction contract determines the answer. |
| Networking | `APPLIED` | Retry only transient, idempotent operations while preventing duplicate side effects. | Protocol behavior must be applied to operational constraints. |
| Testing | `APPLIED` | Replace direct wall-clock access with an injected time source so stale-cache behavior is deterministic. | The testability mechanism is selected from a real failure. |
| Mobile System Design | `ADVANCED` | Design an offline write path with optimistic UI, conflict policy, idempotent retry, and recovery requirements. | Several viable choices and failure consequences must be evaluated. |
| Kotlin Multiplatform | `APPLIED` | Place shared validation tests and an Android-only adapter test in the source sets that can exercise them. | Platform availability materially determines placement. |

### Classification Review

The author or reviewer must be able to justify a proposed classification in one
short sentence describing the minimum required reasoning. That sentence is
review evidence, not a production field.

When reviewers disagree:

1. identify the minimum reasoning actually needed to choose correctly;
2. decide whether scenario details materially change the answer;
3. decide whether multiple mechanisms or constraints must be combined;
4. compare the question with the nearest same-concept boundary example;
5. prefer the lower level when higher-level reasoning is interesting but not
   necessary to answer the actual stem and options.

Do not raise a classification because the explanation includes advanced detail,
the distractors sound sophisticated, the stem or answers are long, the question
uses `MULTIPLE`, the Topic is often considered difficult, or the author found
the question unfamiliar. Longer answers, longer explanations, more correct
options, and more elaborate wording are not higher-level evidence.

Complete-bank classification should preserve review-only evidence such as
question ID, proposed level, a one-line justification, and whether the decision
was ambiguous. Do not add justification or ambiguity fields to production
curriculum. Classify the bank according to what it actually tests; meaningful
representation across all three levels is useful, but no exact distribution or
quota is required.

## Answer Selection Mode

Every question must explicitly author `selectionMode`:

- `SINGLE` permits the candidate to hold one selected answer at a time. It must
  have exactly one correct answer.
- `MULTIPLE` permits the candidate to select several answers before submitting.
  It may have one or more correct answers.

`selectionMode` is an interaction rule, not a scoring rule. It must not be
inferred from `correctAnswerIds.size`: doing so exposes hidden answer-key
information through the input controls. A `MULTIPLE` question with one correct
answer is valid and still uses multi-selection controls.

## Answer Options

- Incorrect answers should be plausible distractors.
- Distractors should reflect realistic misconceptions when possible.
- Avoid obviously absurd filler answers.
- Keep answer options similar in style, specificity, and level of detail.
- Avoid making the correct answer consistently longer or more carefully worded
  than the distractors.
- Do not use answer order as part of the question's meaning.
- Avoid "All of the above" and "None of the above" unless there is a concrete
  reason and the wording remains unambiguous.
- Multiple-correct-answer questions are allowed because the model uses
  `correctAnswerIds: List<String>`, but use them only when the technical content
  genuinely has multiple independently correct answers.
- See `docs/question-authoring-playbook.md` for how to satisfy the first two
  rules in practice. Distractor plausibility is the easiest rule to believe you
  have met and the hardest to actually meet, so the playbook gives a taxonomy of
  where good distractors come from and the length, absolute-word, category, and
  grammar audits that catch the cues review tends to miss.

## Explanations

- Explain why the correct answer or answers are correct.
- Go beyond simply restating the selected answer text.
- Clarify important misconceptions when useful.
- Mention why a plausible distractor is wrong when that adds educational value.
- Keep explanations concise enough to be useful in a review screen.
- Avoid introducing unsupported claims that are not backed by the listed
  sources.

## Sources

- Prefer authoritative primary sources whenever available.
- Prefer Android Developers, AndroidX, and Jetpack documentation for Android
  platform, framework, lifecycle, UI, and Jetpack APIs.
- Prefer Kotlin official documentation for Kotlin language, coroutines, Flow,
  and Kotlin Multiplatform topics.
- Prefer official Gradle documentation for Gradle topics.
- Prefer official Firebase documentation for Firebase Cloud Messaging topics.
- Prefer official library documentation for libraries such as Ktor, Koin, Room,
  SQLDelight, OkHttp, Retrofit, and kotlinx.serialization.
- Use secondary sources only exceptionally, and only when a primary source is
  unavailable or insufficient for the specific claim.
- A source must support the specific technical claim being tested, not merely be
  a generic page about the same subject.
- Each source must have a useful human-readable title and the actual URL.
- Do not cite random blogs, SEO interview-question collections, or anonymous
  forum answers as authoritative technical sources.

## Freshness

Some content is likely to become stale and needs closer review:

- Android platform restrictions.
- Permissions.
- Background execution.
- Foreground services.
- Notifications.
- Navigation APIs.
- Compose APIs and recommendations.
- Gradle and Android Gradle Plugin behavior.
- Target SDK and compile SDK behavior.
- Dependency and framework recommendations.

Other concepts are comparatively stable:

- Kotlin language semantics.
- SOLID principles.
- Dependency-injection fundamentals.
- HTTP fundamentals.
- General architecture concepts.

For version-sensitive questions:

- Avoid hard-coding temporary version numbers unless the version is itself the
  point of the question.
- Make API-level, version, or platform assumptions explicit when needed.
- Re-check authoritative sources when reviewing or materially editing the
  question.

## Content Status

`ContentStatus.ACTIVE` means the item is eligible for current practice or
assessment selection.

`ContentStatus.DEPRECATED` means the item is retained for stable identity and
historical attempts, but should not normally be selected for new practice.

Do not confuse `ContentStatus.DEPRECATED` with whether the Android, Kotlin, or
library API discussed by a question is itself deprecated. A question about
Fragments, RxJava, SharedPreferences, legacy Views, or another older technology
may remain `ACTIVE` if it is still relevant to Android interviews.

## Stable Question Identity

`Question.id` identifies the meaning of the question, not its current wording.
Historical `TestAttempt` and `QuestionAttempt` records must remain meaningful
after content maintenance.

Preserve the existing `Question.id` for minor edits:

- Typo fixes.
- Grammar improvements.
- Clearer wording that preserves the same technical meaning.
- Source-link maintenance.
- Explanation improvements that do not change the assessed claim.

Use a new `Question.id` and mark the old question `DEPRECATED` for material
changes:

- The concept being tested changes.
- The technical claim changes materially.
- The correct answer or answers change because the semantics changed.
- The question becomes a substantially different scenario.
- Historical answers would no longer mean the same thing after the rewrite.

Changing a question between `SINGLE` and `MULTIPLE` changes its interaction
semantics and should be considered when deciding whether its stable identity is
still appropriate. Merely making the previously intended mode explicit does not
require ID churn.

## Stable Answer Identity

`AnswerOption.id` identifies the semantic answer, not its position in the list.

Preserve the existing answer ID for:

- Spelling or grammar corrections.
- Clarifying wording that does not change the answer's meaning.

Use a new answer ID when:

- The answer's semantic meaning changes materially.
- The assertion represented by the answer becomes a different answer.

Do not use list position or index as answer identity.

## Topic and Subtopic Assignment

- Every question must use the most appropriate existing `topicId` and
  `subtopicId`.
- Avoid duplicating nearly identical questions under multiple categories.
- If a question spans several concepts, classify it by the primary concept being
  tested.
- Broad system-design questions may exercise multiple areas, but they still need
  one canonical curriculum location in the current model.

## Question-Bank Balance

For the initial question bank and later expansion:

- Do not concentrate most questions in only Kotlin or Compose while leaving
  other agreed topics empty.
- Prioritize common and senior-relevant concepts first.
- Not every subtopic needs an MVP question immediately.
- Deeper density is appropriate for high-frequency areas such as Android
  lifecycle, Compose, Kotlin, coroutines, architecture, persistence, networking,
  and testing.
- Lower-frequency or advanced areas may start with fewer representative
  questions.

## Examples

### Well-Written Single-Answer Question

Question:

When an Android app process is killed in the background and the user later
returns to the previous task, which mechanism is intended to restore small
amounts of UI state needed to recreate the screen?

Answers:

- `saved_state_registry` - Saved-state APIs such as `rememberSaveable` or
  `SavedStateHandle`, depending on the UI layer.
- `viewmodel_only` - A `ViewModel`, because it always survives process death.
- `singleton_cache` - A singleton in memory, because it is shared across the
  app process.
- `activity_field` - A field on the previous `Activity` instance.

Correct answer IDs: `saved_state_registry`

Why this is good: it tests one lifecycle/state concept, avoids trick wording,
and uses realistic misconceptions as distractors.

### Well-Written Multiple-Correct-Answer Question

Question:

Select all that apply. Which statements about Kotlin `StateFlow` are correct?

Answers:

- `stateflow_has_value` - A `StateFlow` always has a current value.
- `stateflow_hot` - A `StateFlow` is a hot flow.
- `stateflow_no_initial` - A `StateFlow` can be created without an initial
  value.
- `stateflow_completes_normally` - A `StateFlow` normally completes after its
  first collector receives the current value.

Correct answer IDs: `stateflow_has_value`, `stateflow_hot`

Why this is good: the prompt explicitly says multiple answers may be correct,
and each correct answer represents an independently true statement.

### Weak or Ambiguous Question

Question:

What is the best way to do navigation in Android?

Why this is poor: "best" is undefined, the Android API/version context is
unclear, the app architecture is unknown, and several answers could be
defensible. A better question would specify Navigation 2, Navigation 3, Compose,
fragments, or back-stack ownership depending on the concept being tested.

### Stable-ID Minor Edit

Before:

Question ID: `compose_recomposition_state_read`

Question text: "What happens when composable reads state and it changes?"

After:

Question ID: `compose_recomposition_state_read`

Question text: "What happens when a composable function reads Compose state and
that state later changes?"

Why the ID is preserved: the same concept is being tested; only clarity and
grammar changed.

### Material Change Requiring a New ID

Before:

Question ID: `workmanager_constraints_network`

Concept: WorkManager network constraints.

After:

Question ID: `foreground_service_type_location`

Concept: Foreground-service type requirements for location work.

Why a new ID is required: the rewritten question tests a different platform
constraint, so historical answers to the old question would no longer represent
the same knowledge.

## Relationship to E06-05 Validation

E06-03 defines editorial and quality expectations. E06-05 will implement
deterministic validation rules such as:

- Unique topic, subtopic, question, and answer IDs.
- Valid topic and subtopic references.
- Hierarchy consistency between `topicId` and `subtopicId`.
- `correctAnswerIds` referencing real answers.
- `SINGLE` questions not containing several correct answers.
- Non-empty required fields.
- Required source presence.

Some standards remain editorial review concerns because they are not reliably
machine-verifiable:

- Whether a distractor is plausible.
- Whether wording is ambiguous.
- Whether an explanation is genuinely educational.
- Whether a source semantically supports the exact claim being tested.

## PR Review Checklist

- Question wording is clear, concise, answerable, and not a trick.
- When interview level is being classified or reviewed, it has a defensible
  one-sentence justification based on the minimum required reasoning.
- Interview level describes the question's reasoning, not learner ability or
  seniority.
- Interview level is not inferred from stem, answer, or explanation length;
  obscurity; Topic; selection mode; or number of correct options.
- An `ADVANCED` classification requires deeper reasoning to identify the answer,
  not merely advanced context in the explanation or sophisticated distractors.
- Mechanism, scenario, and trade-off depth support the proposed classification.
- Topic and subtopic assignment is correct.
- Stable `Question.id` and `AnswerOption.id` handling is appropriate.
- Distractors are plausible and similar in style to the correct answer.
- No correct answer is defensibly wrong under the question as written, and
  every clause of the question stem is itself true.
- Multiple-answer wording is explicit when more than one answer is correct.
- `selectionMode` is explicitly authored and matches the intended interaction.
- `MULTIPLE` wording clearly communicates multi-selection; `MULTIPLE` may still
  have exactly one correct answer.
- `SINGLE` does not contain several correct answers.
- `correctAnswerIds` reflect the intended correct answer or answers.
- Explanation teaches why the answer is correct and addresses useful
  misconceptions.
- Every claim in the explanation is supported by a listed source, including
  claims made about why a distractor is wrong.
- Sources are authoritative, relevant, and specific to the tested claim.
- Freshness and version sensitivity have been considered.
- `ACTIVE` or `DEPRECATED` status is appropriate.
- The mechanical audits in `docs/question-authoring-playbook.md` have been run:
  answer-length parity, absolute-word distribution across correct answers and
  distractors, and a liveness check confirming every source URL still resolves.
