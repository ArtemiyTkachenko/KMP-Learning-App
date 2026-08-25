# Local Persistence Design

## Scope

This document defines the E07 local persistence direction and logical schema for
the Android interview curriculum. It is a design reference only: E07-01 does not
add Room dependencies, database code, repositories, import logic, migrations, or
runtime wiring.

The schema is designed around the current E06 content model:

- `Curriculum`
- `Topic`
- `Subtopic`
- `Question`
- `AnswerOption`
- `SourceReference`
- `ContentStatus`

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

Exact Room annotations, index declarations, converters, drivers, and builders
belong to E07-03.

### Topic

Logical table: `topic`

| Column | Type | Constraint |
| --- | --- | --- |
| `id` | `TEXT` | `PRIMARY KEY` |
| `name` | `TEXT` | `NOT NULL` |
| `status` | `TEXT` | `NOT NULL` |

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

Relationship:

```text
subtopic.topic_id -> topic.id
```

Expected query and index needs:

- `subtopic(topic_id)`
- `subtopic(topic_id, status)`

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

Expected indexes:

- `question(topic_id, status)`
- `question(subtopic_id, status)`

### Answer Option

Logical table: `answer_option`

| Column | Type | Constraint |
| --- | --- | --- |
| `question_id` | `TEXT` | `NOT NULL` |
| `id` | `TEXT` | `NOT NULL` |
| `text` | `TEXT` | `NOT NULL` |

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

### Sources

Logical table: `question_source`

| Column | Type | Constraint |
| --- | --- | --- |
| `question_id` | `TEXT` | `NOT NULL` |
| `url` | `TEXT` | `NOT NULL` |
| `title` | `TEXT` | `NOT NULL` |

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

## Future Assessment-History Compatibility

This issue does not define or implement assessment-history tables. The schema is
intentionally compatible with later records such as:

```text
TestAttempt
  -> QuestionAttempt
       -> questionId
       -> selected answer IDs
```

Stable `Question.id` values and per-question answer IDs make historical attempts
meaningful after content updates. Do not add `TestAttemptEntity`,
`QuestionAttemptEntity`, progress tables, statistics tables, or mistake-review
tables in E07-01.

## Migration and Schema History

E07-03 should establish schema versioning from the first database version and
enable version-controlled Room schema artifacts.

Destructive migration should not be the default production strategy. Migration
tests should be added when schema versions actually change.

## Koin

Do not introduce Koin for E07-01. Manual construction remains sufficient while
the dependency graph is still small.

Koin should be revisited when later E07 work creates a real graph, likely around
E07-05, with a database, importer/data source, repository implementations, and
application-facing repositories.

## Deferred Implementation Work

Deferred to E07-02:

- bundled JSON serialization format;
- serialization annotations or DTOs if needed;
- resource/import representation.

Deferred to E07-03:

- Room and KSP dependencies;
- entities, DAOs, database declaration, converters, indexes, and constraints;
- platform-specific Room builders and SQLite drivers;
- schema export configuration and first schema artifact.

Deferred to E07-04 and later:

- validated import/update behavior;
- mappings between bundled content, persistence entities, and domain models;
- repositories and query APIs;
- runtime UI or assessment behavior.
