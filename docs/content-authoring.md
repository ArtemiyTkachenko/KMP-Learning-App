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

## Model Context

The content model uses the hierarchy:

1. Topic
2. Subtopic
3. Question

The Kotlin model stores these as flat collections linked by stable IDs. A
question has `id`, `topicId`, `subtopicId`, `text`, `answers`,
`correctAnswerIds`, `explanation`, `sources`, and `status`.

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
- If multiple answers are correct, the prompt must clearly say so, for example
  "Select all that apply."
- Prefer scenarios that reflect real Android engineering decisions over
  memorization-only prompts.

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
- Topic and subtopic assignment is correct.
- Stable `Question.id` and `AnswerOption.id` handling is appropriate.
- Distractors are plausible and similar in style to the correct answer.
- Multiple-answer wording is explicit when more than one answer is correct.
- `correctAnswerIds` reflect the intended correct answer or answers.
- Explanation teaches why the answer is correct and addresses useful
  misconceptions.
- Sources are authoritative, relevant, and specific to the tested claim.
- Freshness and version sensitivity have been considered.
- `ACTIVE` or `DEPRECATED` status is appropriate.
