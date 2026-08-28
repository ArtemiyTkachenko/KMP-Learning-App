# Local Persistence Design

## Scope

This document defines the E07 local persistence direction and logical schema for
the Android interview curriculum. It started as the E07-01 design reference and
now records the implemented local data path as the later E07 issues connect the
database, importer, repository, runtime initialization, and E08 assessment
attempt persistence.

The schema is designed around the current E06 content model:

- `Curriculum`
- `Topic`
- `Subtopic`
- `Question`
- `AnswerOption`
- `SourceReference`
- `ContentStatus`

Bundled curriculum content is now represented as JSON and decoded into
`Curriculum` before validation or persistence:

```text
initial_curriculum.json
  -> CurriculumJsonCodec
  -> Curriculum
  -> CurriculumValidator
  -> CurriculumImporter
  -> Room
  -> LocalCurriculumRepository
  -> CurriculumRepository
```

## Persistence Decision

Room 3 is the selected persistence technology for this project. The current
stable Room 3 line referenced for later implementation is Room 3.0.1.

Room 3 fits this project because it is primarily an Android/KMP learning and
interview-preparation app. Room is highly relevant to modern Android work and
Android interviews, and Room 3 provides a Kotlin-first multiplatform persistence
model with coroutine-based database access, compile-time query/schema
verification, migrations, and schema export/testing. It also allows the
relational model, DAOs, mappings, and most persistence behavior to live in
shared KMP code while Android remains the primary MVP runtime and validation
platform.

### Room vs SQLDelight

SQLDelight would also be a reasonable persistence choice. It has a SQL-first
design, strong explicit SQL learning value, type-safe generated APIs, strong
multiplatform support, and a mature migration model.

Room 3 is selected here because the project goal is not primarily direct SQL
practice. The project is an Android interview-preparation and KMP portfolio app,
so using the Android ecosystem's standard persistence library provides more
direct learning value while still preserving a relational, multiplatform local
data layer.

## Domain vs Persistence Boundary

Curriculum domain models remain distinct from persistence models. The E06
models should not gain `@Entity`, `@Dao`, `@Database`, converter, or storage
annotations.

Future Room persistence models should conceptually be separate:

- `TopicEntity`
- `SubtopicEntity`
- `QuestionEntity`
- `AnswerOptionEntity`
- `QuestionCorrectAnswerEntity`
- `QuestionSourceEntity`
- `TestAttemptEntity`
- `QuestionAttemptEntity`
- `QuestionAttemptSelectedAnswerEntity`

This keeps the domain contract independent from the database representation.
Bundled content, database tables, and application-facing repository APIs are
separate concerns. Later E07 work should map between persistence entities and
domain models, and repository APIs should return domain concepts rather than
expose Room entities directly.

## Stable Identities

The stable E06 string IDs are the authoritative persistence identities. Do not
introduce generated numeric content IDs for `Topic`, `Subtopic`, or `Question`.

Examples:

- `Topic.id = "kotlin_language"` becomes `topic.id`.
- `Subtopic.id = "kotlin_generics"` becomes `subtopic.id`.
- `Question.id = "kotlin_generics_001"` becomes `question.id`.

Stable string primary keys preserve bundled-content identity, deterministic
updates, historical question references, and future assessment records. A
separate surrogate content ID should be introduced only if a concrete future
requirement justifies it.

## Relational Schema

Logical relationship shape:

```text
Topic
  |
  +--< Subtopic
         |
         +--< Question
                |
                +--< AnswerOption
                |      |
                |      +--< QuestionCorrectAnswer
                |
                +--< QuestionSource
```

Room 3.0.1 is the implemented baseline. Schema version 1 contains curriculum
content tables; schema version 2 adds assessment attempt history.

The authored `Curriculum` JSON uses ordered lists. SQLite row order is not
defined unless it is modeled explicitly, so persisted authored order is stored
with `sort_order` on ordered tables. This keeps display order independent from
stable IDs such as `_a`, `_b`, and `_c`, which remain identities rather than
ordering rules.

### Topic

Logical table: `topic`

| Column | Type | Constraint |
| --- | --- | --- |
| `id` | `TEXT` | `PRIMARY KEY` |
| `name` | `TEXT` | `NOT NULL` |
| `status` | `TEXT` | `NOT NULL` |
| `sort_order` | `INTEGER` | `NOT NULL` |

`id` is the stable `Topic.id`. `status` stores readable `ContentStatus` names
such as `ACTIVE` and `DEPRECATED`.

### Subtopic

Logical table: `subtopic`

| Column | Type | Constraint |
| --- | --- | --- |
| `id` | `TEXT` | `PRIMARY KEY` |
| `topic_id` | `TEXT` | `NOT NULL` |
| `name` | `TEXT` | `NOT NULL` |
| `status` | `TEXT` | `NOT NULL` |
| `sort_order` | `INTEGER` | `NOT NULL` |

Relationship:

```text
subtopic.topic_id -> topic.id
```

Implemented query and index needs:

- `subtopic(topic_id, status)`
- unique `subtopic(topic_id, id)` for the composite question relationship

The later Room implementation may choose the exact index declarations, but these
access patterns should be preserved.

### Question

Logical table: `question`

| Column | Type | Constraint |
| --- | --- | --- |
| `id` | `TEXT` | `PRIMARY KEY` |
| `topic_id` | `TEXT` | `NOT NULL` |
| `subtopic_id` | `TEXT` | `NOT NULL` |
| `text` | `TEXT` | `NOT NULL` |
| `explanation` | `TEXT` | `NOT NULL` |
| `status` | `TEXT` | `NOT NULL` |
| `sort_order` | `INTEGER` | `NOT NULL` |

Persist both `topic_id` and `subtopic_id`. This deliberately mirrors
`Question.topicId` and `Question.subtopicId` so topic and subtopic filtering can
be represented and queried directly.

Relationships:

```text
question.topic_id -> topic.id
question.subtopic_id -> subtopic.id
```

The schema should also preserve the stricter hierarchy invariant:

```text
(question.topic_id, question.subtopic_id)
  -> (subtopic.topic_id, subtopic.id)
```

That prevents a question from referencing a valid topic and a valid subtopic
that belong to different curriculum locations. Implementing this with
Room/SQLite may require an appropriate unique or indexed key on the Subtopic
side. E07-03 owns the exact Room annotation syntax.

Implemented indexes:

- `question(topic_id, status)`
- `question(subtopic_id, status)`
- `question(topic_id, subtopic_id)` for the composite foreign-key child columns

### Answer Option

Logical table: `answer_option`

| Column | Type | Constraint |
| --- | --- | --- |
| `question_id` | `TEXT` | `NOT NULL` |
| `id` | `TEXT` | `NOT NULL` |
| `text` | `TEXT` | `NOT NULL` |
| `sort_order` | `INTEGER` | `NOT NULL` |

Primary key:

```text
(question_id, id)
```

Relationship:

```text
answer_option.question_id -> question.id
```

`AnswerOption.id` is stable within a question. The schema should not require
global answer ID uniqueness just because the current authored naming convention
often makes answer IDs globally unique.

### Correct Answers

Logical table: `question_correct_answer`

| Column | Type | Constraint |
| --- | --- | --- |
| `question_id` | `TEXT` | `NOT NULL` |
| `answer_id` | `TEXT` | `NOT NULL` |

Primary key:

```text
(question_id, answer_id)
```

Composite relationship:

```text
(question_id, answer_id) -> answer_option(question_id, id)
```

Correctness should not be stored as `AnswerOptionEntity.isCorrect`. A relation
table directly represents `Question.correctAnswerIds: List<String>` and supports
one or multiple correct answers without changing the schema.

`question_correct_answer` does not have `sort_order` because correct-answer
identity is set-like; answer display order belongs to `answer_option`.

### Sources

Logical table: `question_source`

| Column | Type | Constraint |
| --- | --- | --- |
| `question_id` | `TEXT` | `NOT NULL` |
| `url` | `TEXT` | `NOT NULL` |
| `title` | `TEXT` | `NOT NULL` |
| `sort_order` | `INTEGER` | `NOT NULL` |

Primary key:

```text
(question_id, url)
```

Relationship:

```text
question_source.question_id -> question.id
```

`SourceReference` does not currently have its own stable domain ID, so the
schema should not invent one. Sources are question-owned metadata, not
independent product entities. A global source table can be introduced later only
if sources become reusable product objects with behavior of their own.

## Relationships and Integrity

The intended integrity model is layered:

```text
bundled authored content
  -> CurriculumValidator
  -> persistence mapping/import
  -> SQLite / Room relational constraints
```

`CurriculumValidator` validates a complete domain/content dataset before
persistence and gives useful authored-content diagnostics. Database constraints
protect persisted relational integrity and prevent invalid foreign-key
relationships. They are complementary; database constraints are not a
replacement for content validation.

## Import and Update Policy

Bundled JSON is decoded into `Curriculum` and validated with
`CurriculumValidator` before any database write begins. Accepted imports are
then mapped into persistence rows and written in a single Room write
transaction.

Stable IDs are upserted. Incoming `Topic`, `Subtopic`, `Question`, and
`AnswerOption` rows insert new identities or update existing identities, but
absence from a later bundle is not a deletion signal. Missing rows are not
automatically deleted or marked deprecated; curriculum retirement must be
explicit through `ContentStatus.DEPRECATED`.

Question-owned relation and metadata rows are synchronized only for incoming
question IDs:

- existing `question_correct_answer` rows for incoming questions are replaced by
  the incoming correct-answer IDs;
- existing `question_source` rows for incoming questions are replaced by the
  incoming source list;
- existing `answer_option` rows for an incoming question are removed when that
  question no longer authors them, unless a historical
  `question_attempt_selected_answer` row still references the option;
- rows for unrelated persisted questions are retained.

This keeps correctness and source metadata from accumulating stale rows while
preserving stable curriculum identity and unrelated local content.

Answer options carry their own `status` alongside topics, subtopics, and
questions. An option the incoming curriculum no longer authors is deleted when
nothing references it, and marked `DEPRECATED` when a historical
`question_attempt_selected_answer` row still does. Retiring rather than deleting
matters because renaming an `AnswerOption` id is a content edit but a data
migration in the database: without a status the retired row would keep appearing
as an extra choice in new assessments. Active curriculum queries therefore read
through `getActiveAnswerOptionsForQuestions`, while
`CurriculumRepository.getQuestionById` reads every option so a past attempt is
still reviewable with the answer text the user actually saw. Re-adding an option
in a later bundle reactivates it, because the import upserts every authored
option as `ACTIVE`.

Stale answer options are deleted last inside the import transaction.
`question_correct_answer` and `question_attempt_selected_answer` both hold
`NO ACTION` foreign keys onto `answer_option(question_id, id)`, so an option can
only be removed after the correct-answer rows for its question have been
replaced, and only when no historical attempt selected it. An option that a past
attempt selected is deliberately retained: keeping historical review resolvable
matters more than hiding one option that a later bundle dropped, and deleting it
would abort the whole import transaction and leave the application unable to
start. Deletion is scoped per question because answer identity is the composite
`(question_id, id)` rather than a globally unique answer ID.

## Expected Query Patterns

The schema is driven by known future access patterns:

- list `ACTIVE` topics;
- list `ACTIVE` subtopics for a topic;
- list `ACTIVE` questions for a topic;
- list `ACTIVE` questions for a subtopic;
- retrieve a question by stable question ID;
- load answer options for a question;
- load correct-answer IDs for a question;
- load source references for a question;
- retain and individually resolve `DEPRECATED` content for historical identity.

Runtime reads are exposed through a shared repository boundary:

```text
Room
  -> LocalCurriculumRepository
  -> curriculum domain models
```

Normal practice queries return only content whose full hierarchy is active:
`Question`, `Subtopic`, and `Topic` must all have `ACTIVE` status. Stable-ID
question lookup is different: it may return `ACTIVE` or `DEPRECATED` questions
so later historical attempts can still resolve the content they referenced.

## Content Lifecycle and Deletion

Persist `ContentStatus.ACTIVE` and `ContentStatus.DEPRECATED` as readable `TEXT`
values. Do not store unexplained integer values such as `0` and `1`.

`ACTIVE -> DEPRECATED` is the normal curriculum retirement path. Physical
deletion should not be the normal content-update strategy because future
assessment history will depend on stable curriculum identity.

Destructive cascades should therefore be used cautiously. Relational constraints
should catch invalid state, while destructive operations should remain explicit
maintenance actions with a concrete reason.

`ContentStatus.DEPRECATED` describes curriculum lifecycle, not Android API
lifecycle. A question about an older or deprecated Android API can still be
`ACTIVE` if it remains useful interview material.

## KMP Platform Boundary

The shared module currently retains Android, iOS, JVM, JS browser, and WasmJS
browser targets.

Expected common/shared responsibilities:

- database declaration and schema where supported;
- entities;
- DAOs;
- domain/persistence mappings;
- shared persistence behavior;
- import-time validation before persistence.

Expected platform-specific responsibilities:

- database file and location;
- Room database builder creation;
- driver and environment-specific setup where required.

Android is the required MVP runtime and primary validation platform. The design
should remain compatible with the retained KMP targets, but E07-01 does not
promise full production-ready database initialization for every target. Web,
JS, and Wasm persistence may require additional browser or worker plumbing and
should not expand the Android-focused MVP scope prematurely.

E07-03 implements Android database creation with `BundledSQLiteDriver` and uses
an in-memory JVM database with the same driver for persistence tests.
`sqlite-bundled` is intentionally scoped to Android runtime and JVM tests
because JS/Wasm require web-specific driver setup, such as a web-worker based
SQLite driver, which is outside the Android-focused MVP database issue.

## Assessment Attempt History

Schema version 2 introduced assessment attempts; the current schema remains
version 3, which additionally preserves retired answer-option identity:

```text
TestAttempt
  -> QuestionAttempt
       -> selected answer IDs
```

`AssessmentSession` itself is not persisted. It is the runtime aggregate that
keeps selected `Question` objects available for scoring while an assessment is
running. Persistent history stores `TestAttempt`, ordered `QuestionAttempt`
rows, and selected stable `AnswerOption` IDs.

`test_attempt` stores:

- `id` as the stable attempt identity;
- `config_type`, `requested_question_count`, `scope_type`, and `scope_id` as
  readable assessment configuration metadata;
- `status` as `IN_PROGRESS` or `COMPLETED`;
- nullable score columns for completed attempts;
- `started_at_epoch_millis` and nullable `completed_at_epoch_millis`.

`question_attempt` stores `(test_attempt_id, question_id)` as its primary key,
references the parent attempt and stable curriculum `question.id`, and uses
`sort_order` for the assessment question sequence. Nullable `is_correct`
represents the current two-state answer model: `NULL` means unanswered, while
`true` or `false` means answered.

`question_attempt_selected_answer` stores one row per submitted answer ID using
the primary key `(test_attempt_id, question_id, answer_id)`. It references both
the parent `question_attempt` and `answer_option(question_id, id)`, so selected
answer IDs remain scoped to the question that owned them. Selected answers are
set-like, so no selected-answer order column is stored.

Historical attempts reference stable curriculum IDs rather than copying question
text, answer text, explanations, or sources. Deprecated curriculum rows are
retained by the content lifecycle, so historical attempts can still resolve the
questions and answers they referenced.

`AssessmentRepository.getCompletedAttempts()` reconstructs all persisted
`COMPLETED` attempts newest first by completion time, then started time and
stable ID. `IN_PROGRESS` attempts remain persisted and addressable by ID but are
excluded from completed learning history. Historical naming uses unrestricted
stable Topic, Subtopic, and Question repository lookups; current browsing and
selection continue to use ACTIVE-only queries.

## Migration and Schema History

E07-03 establishes schema version 1 and enables version-controlled Room schema
artifacts. E08-04 establishes schema version 2 and adds an explicit
`MIGRATION_1_2` that creates only the assessment attempt tables and indexes.
Schema version 3 adds answer-option lifecycle status through `MIGRATION_2_3`.
E11-01 adds history read queries only and does not change the schema.

Destructive migration should not be the default production strategy. Migration
tests validate the migration chain against Room's exported schemas and verify
existing curriculum and assessment rows remain intact.

## Koin

Koin is used for the concrete runtime curriculum data graph introduced by E07:

```text
Android Application
  -> Koin
     -> CurriculumDatabase
     -> CurriculumImporter
     -> CurriculumDataInitializer
     -> CurriculumRepository
```

The shared module defines the curriculum data module. Android supplies the
platform database module using the application context and existing Room
builder. The project uses Koin's classic DSL only; annotation processing,
compiler plugins, Compose injection, and ViewModel DSLs remain deferred.

## Deferred Implementation Work

Deferred to E07-02:

- bundled JSON serialization format;
- serialization annotations or DTOs if needed;
- resource/import representation.

Deferred to E07-04 and later:

- validated import/update behavior;
- mappings between bundled content, persistence entities, and domain models;
- repositories and query APIs;
- runtime UI or assessment behavior.
