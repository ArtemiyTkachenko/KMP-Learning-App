# Learning-Content Authoring Contract

## Purpose and Scope

This document defines the editorial standard for **learning content**: the explanatory
study material the app uses to teach an interview-relevant concept before asking the
learner to practise it. It is the learning-side counterpart to
`docs/content/content-authoring.md`, which governs assessment questions.

It applies to every future learning Topic — Compose, dependency injection, coroutines and
Flow, Room and persistence, networking, architecture, Kotlin Multiplatform, testing, and
anything else the curriculum grows into. `docs/content/compose-learning-blueprint.md` is
the first Topic mapped under this contract and doubles as the worked example.

This document defines authoring quality expectations. It does not define the serialized
model, persistence, navigation, or learner progress; those belong to later `E20` issues.

Much of this document is written in authoring vocabulary — what a Lesson *owns*, where the
*full treatment* of a concept *lives*, primary and supporting concepts, Teach and Bridge.
Those are directions to authors for deciding what goes where. They are not templates for
sentences a learner reads; [Rule 12](#rule-12--lessons-are-written-for-learners-not-the-curriculum-graph)
covers how those decisions become ordinary teaching prose.

The editorial rules for **learner-facing text** — chiefly Rules 7, 11 and 12 — apply to
everything the learner reads, not only to paragraphs: Unit titles and summaries, Lesson
titles and summaries, paragraphs, bullet items, callouts, comparison headers and cells, and
the comments inside code examples. A code comment in a teaching example is prose. The
identifiers in that code are not; they are governed by the semantic test in Rule 12.

**Runtime AI-generated learning content is explicitly out of scope.** Every lesson that
ships is authored, reviewed, and grounded in authoritative sources before it reaches the
bundled curriculum. AI assistance during drafting is allowed and expected; unreviewed
generation at runtime is not part of this product.

Also out of scope, for the same reasons as the question bank: behavioral interview
preparation, algorithm and data-structure exercises, and backend curricula.

## Relationship to the Question-Authoring Documents

The question-authoring documents remain separate and unchanged. They govern assessment
content; this document governs study material. The two answer different questions:

| Document | Governs | Central question |
| --- | --- | --- |
| `content-authoring.md` | Assessment questions | Does this question measure the concept fairly? |
| `question-authoring-playbook.md` | Question method | How do I write a question that survives review? |
| `question-validation.md` | Question acceptance | Is this question correct enough to ship? |
| `question-bank-coverage.md` | Bank state | What does the bank already cover? |
| **`learning-content-authoring.md`** | **Learning content** | **Does this lesson teach the concept well enough to be understood and reasoned about?** |

Where the two families overlap — source quality, freshness, stable identity, lifecycle
status — this document reuses the existing rule rather than restating a competing one.

## The Learning Hierarchy

The assessment hierarchy is unchanged:

1. Topic
2. Subtopic
3. Question

The learning hierarchy is conceptually:

1. Topic
2. Learning Unit
3. Lesson

A **Learning Unit** groups concepts that make sense to *learn together*. A **Lesson** is
one focused piece of reading inside a Unit.

The two hierarchies share Topics and they share the Subtopic vocabulary, but they are not
the same structure and must not be forced into one:

- Do **not** require one Learning Unit per Subtopic.
- Do **not** require one Lesson per Subtopic.
- Do **not** require one Question family per Lesson.

A Learning Unit routinely covers several existing Subtopics, and a single Subtopic is
routinely touched by more than one Lesson at different depths. The Question Subtopic
remains the stable bridge into assessment coverage — it is the vocabulary both sides
share, not a structural constraint on either.

The structures differ because they were optimized for different jobs. The question
taxonomy is intentionally granular because fine-grained Subtopics make coverage
measurable, practice selection precise, and progress meaningful. Granularity that helps
assessment hurts teaching: a learner does not want fourteen disconnected micro-lessons
that each match one Subtopic, they want a coherent path through a subject.

Everything that encodes this hierarchy — Topic, Subtopic, Unit, Lesson and Question IDs,
`primarySubtopicIds`, `supportingSubtopicIds`, `relatedLessonIds`, status fields — is
authoring and runtime metadata. It exists for mapping, validation, navigation and progress,
and it stays out of learner-facing prose (Rule 12).

## Rule 1 — Map the Complete Topic Before Authoring Lessons

Before writing any production lesson for a major Topic, produce a **blueprint** for that
Topic that works through these steps in order:

1. Map the complete interview-relevant subject, including material that will not be
   taught, so the decision to omit it is deliberate.
2. Identify the conceptual dependencies — what must be understood before what.
3. Divide the subject into coherent Learning Units.
4. Divide each Unit into focused Lessons.
5. Classify every mapped area as **Teach**, **Bridge**, **Reference**, or **Exclude**.
6. For each Lesson, identify its primary concepts and its supporting cross-topic concepts.
7. Map those concepts onto the existing curriculum taxonomy using real Topic and Subtopic
   IDs, and record honestly where no exact Subtopic exists.
8. Review the existing Questions for the mapped Subtopics.
9. Only then begin authoring lessons.

### Why this rule exists

Writing one interesting lesson at a time is the natural failure mode, and it reliably
produces four problems:

- **Duplication.** The same mental model gets explained three times in three Units,
  slightly differently each time, and none of them is authoritative.
- **Missing prerequisites.** A lesson silently depends on a concept nothing has taught
  yet, so the learner meets `derivedStateOf` before they know what a recomposition scope
  is.
- **Inconsistent depth.** One lesson is a 400-word summary, its neighbour is a 3,000-word
  treatise, and the learner cannot tell which concepts actually matter.
- **Later restructuring.** Fixing any of the above means renumbering, re-splitting, and
  re-linking material that learners have already worked through and that assessment
  scopes may already reference.

The rule is: **design the course before writing the chapters.**

### What this rule does not require

It does **not** require all learning content for a Topic to be implemented at once. The
complete map comes first; authoring then proceeds incrementally, Unit by Unit or Lesson by
Lesson, in whatever order is most useful. A blueprint is a plan, not a batch of work.

A blueprint is a living document. When authoring reveals that a Lesson boundary was wrong,
update the blueprint in the same change rather than letting the map and the material
drift.

## Rule 2 — Primary, Supporting, and Related Concepts

Each Lesson classifies the concepts it touches into three roles.

**Primary concepts** are the concepts the Lesson is responsible for teaching thoroughly.
These are the Lesson's contract with the learner: if the Lesson names a concept as
primary, a reader who finishes it should be able to explain that concept in an interview.
Primary concepts are what "Practice this material" will eventually mean.

**Supporting concepts** are concepts — from the same Topic or another one — that need
enough explanation for the current Lesson to be understandable, but whose complete
treatment lives elsewhere. A supporting concept is explained to the depth this Lesson
needs and no further. Supporting concepts do **not** automatically become primary practice
coverage.

**Related and deeper content** is the other Lessons or Learning Units that hold the
complete treatment of a concept. Bounded supporting explanation is honest because the
Lesson first states what the current argument needs; a reference to deeper material can
then help the learner continue, but it never stands in for that local explanation.

A well-formed supporting explanation reads like this:

> `StateFlow` always has a current value, so Compose can render meaningful state as soon
> as collection begins. Its sharing and conflation behaviour is covered in more depth in
> **StateFlow, SharedFlow and Hot Streams**.

The first sentence teaches what this Lesson needs. The second is optional navigation, uses
the human-facing title, and could be deleted without breaking the argument. Rule 12 covers
when such a reference is worth writing at all.

`relatedLessonIds` and prose references serve different purposes. The serialized
relationship drives navigation in the app; it does not oblige the text to mention every
related Lesson.

A concept must not be both primary and supporting for the same Lesson. If it is, the
Lesson is either teaching too much or claiming too little.

## Rule 3 — Learning Units May Cross Topic Boundaries

This is a core product rule, not an exception.

A Learning Unit has a **home Topic** that determines where it is browsed. The home Topic
does not constrain which concepts the Unit may explain. A Compose lesson on production
screen state may need enough explanation of `ViewModel`, `StateFlow`, `SavedStateHandle`,
lifecycle, and performance to make sense, even though every one of those concepts is
primarily owned by another Topic.

The rule is: **never make the learner leave a lesson merely to understand the lesson.**

The counterweight is equally important: **supporting context must not reproduce another
Topic's complete course.** A Compose lesson explains `StateFlow` as "the observable
state holder the ViewModel exposes and the UI collects"; it does not teach `shareIn`
versus `stateIn`, replay caches, or subscription timeouts. Those stay in the Lesson that
teaches them, reachable through related content.

Home Topic, ownership and bridging are how authors divide the material. The learner
should see the subject, not that division: a Compose Lesson explains the `StateFlow` fact
it needs without announcing that another Topic owns `StateFlow`.

Primary and supporting relationships are therefore *not* required to stay inside the home
Topic, and a blueprint that keeps every mapping inside its own Topic is usually a sign
that bridging was avoided rather than that it was unnecessary.

### Mappings target current taxonomy

Crossing Topics is about *which* Topic owns a concept, not about whether that concept is
still current. **ACTIVE learning content maps only to ACTIVE assessment taxonomy.**

- **Home Topic.** An ACTIVE Unit must have an ACTIVE home Topic. A Unit browsed under a
  retired Topic would stay visible in learning while assessment treats the Topic as gone.
- **Primary and supporting mappings.** An ACTIVE Lesson may map across Topic boundaries,
  but each mapped Subtopic must itself be current assessment taxonomy: the Subtopic is
  ACTIVE *and* its owning Topic is ACTIVE. A Subtopic under a retired Topic is not current
  even if its own status stayed ACTIVE, because assessment hides the descendants of a
  retired parent.
- **Historical content.** DEPRECATED Units and Lessons may keep references to retired
  Topics and Subtopics, so their stable identities stay meaningful. Those references
  must still resolve to an existing ID.

Current taxonomy is the requirement, not current practice: an ACTIVE Subtopic with no
ACTIVE Questions is a valid mapping. Question density is allowed to lag learning coverage
(Rule 10).

When retiring assessment taxonomy, resolve the learning content it would strand in the
same change:

- If retiring a Topic would strand an ACTIVE Unit, re-home the Unit to another suitable
  ACTIVE Topic, or deprecate the Unit.
- If retiring a Subtopic — directly or through its Topic — would strand an ACTIVE Lesson
  mapping, remove the concept or remap it to an appropriate ACTIVE Subtopic, or deprecate
  the Lesson if the Lesson itself is no longer current.

Do not invent a replacement Subtopic merely to satisfy validation; a concept with no
honest assessment Subtopic is a documented gap (Rule 1). `LearningCurriculumValidator`
enforces this rule at content load, and `tools/learning_question_coverage.py` enforces it
for the active content it reports.

## Rule 4 — Teach, Bridge, Reference, Exclude

Every area a blueprint maps gets exactly one of four editorial decisions.

**Teach** — interview-relevant concepts this Topic must teach properly. These become
primary concepts of some Lesson, with Core depth at minimum.

**Bridge** — concepts primarily owned by another Topic that are required to understand the
current material. Explain enough locally for the current Lesson to stand on its own, then,
where it helps the learner continue, name the deeper treatment by its title. Bridged
concepts appear as supporting concepts. "Bridge" is the author's decision; the learner
reads an explanation, not a bridge.

**Reference** — useful knowledge that deserves a concise explanation somewhere but should
not interrupt the main learning path. Reference material may become a short standalone
Lesson at the end of a Unit, or a clearly marked aside; it is never a prerequisite for
anything on the main path.

**Exclude** — accurate technical material that does not materially improve interview
readiness and would create noise. Typical examples: obscure internal runtime class names,
exhaustive API catalogues, implementation trivia, obsolete advice, and tool mechanics with
little interview signal.

The decisive test for Exclude is not truth, it is value:

> **"Technically interesting" is not sufficient justification for inclusion.**

A blueprint must record its Exclude decisions with a one-line reason. An undocumented
omission looks like an oversight and invites someone to "fix" it later; a documented
exclusion is a decision the next author can agree or disagree with deliberately.

Exclusions are revisitable. If the target job profile changes, a previously excluded area
can be promoted — by editing the blueprint, not by quietly adding a lesson.

## Rule 5 — Concept-First and Problem-First Authoring

Do not structure learning around API-name inventories. An API list is a table of contents
for documentation, not a path through a subject.

The failure mode looks like a lesson outline of `@Inject`, `@Provides`, `@Binds`,
`@Component`, `@Scope` — a learner who follows it can recite annotations and still cannot
explain why dependency injection exists. Structure the same material around the ideas
instead: why dependency injection exists, object graphs, bindings, graph assembly,
lifetimes and scopes, graph boundaries, alternative bindings — then introduce the
annotations as the tools that implement those ideas.

The same applies to Compose effects. Do not primarily teach `LaunchedEffect`,
`DisposableEffect`, `rememberCoroutineScope`, and `rememberUpdatedState`. Teach the
problems first:

- run suspend work tied to the lifetime of a composition or a key;
- register an external observer and guarantee its cleanup;
- launch work from an event using a composition-owned scope;
- keep a long-running effect alive while still reading the latest callback.

Then the API is the answer to a question the learner already has.

The authoring order is:

> **problem or mental model → mechanism → API → practical example**

not:

> API → syntax → next API.

APIs still get named precisely — vagueness is not the goal, and an interview answer that
cannot name the mechanism is a weak answer. The rule is about which one leads.

## Rule 6 — Lesson Depth: Core, Practical, Senior

Core, Practical, and Senior are **layers of one Lesson**, not three separate lesson
variants and not three difficulty settings. A learner reads down through them.

**Core** — what the learner must understand first.

- plain English;
- a correct mental model;
- the essential contract of the concept;
- minimal terminology, introduced deliberately;
- no unnecessary edge cases.

**Practical** — how the concept actually appears in Android and KMP engineering.

- realistic code, not toy code;
- ownership and placement choices;
- failure modes and what they look like in a real app;
- debugging;
- common misconceptions;
- comparison with the alternatives a real engineer would weigh.

**Senior** — the deeper reasoning.

- the mechanism underneath the behavior;
- interacting constraints;
- edge cases that change a decision;
- architectural and performance implications;
- trade-offs with no single correct answer.

Senior depth is **not** a synonym for obscure facts. The section must answer:

> What deeper reasoning would distinguish a strong senior answer from someone who merely
> recognizes the API?

A Lesson does not need all three layers. When deeper material would be artificial, omit
the Senior section rather than manufacturing one; an invented Senior section teaches the
learner that senior means trivia.

## Rule 7 — Plain English Before Documentation Language

This product exists partly because official documentation is comprehensive but written as
*reference* material, organized for lookup by someone who already knows what they are
looking for. Interview preparation needs the opposite order.

Authors must:

1. explain the concept plainly, in ordinary language;
2. establish the mental model;
3. introduce the precise terminology, now that there is something to attach it to;
4. then deepen the mechanism.

Do not merely shorten or paraphrase official documentation. A lesson that is the docs with
fewer words is worse than the docs — it loses precision without gaining understanding.

A finished Lesson should answer as many of these as apply:

- Why does this exist?
- What problem does it solve?
- How should I think about it?
- What behavior actually matters?
- Where would I use it?
- What commonly goes wrong?
- What trade-offs matter?
- What should I be able to explain in an interview?

### Voice

Write the way an experienced engineer explains something to another engineer: concise,
technically precise, calm, and direct. Conversational without being chatty; confident
about documented facts; willing to say "it depends" when a trade-off really does depend on
something, and then say on what.

The voice to avoid is any that puts something between the learner and the subject:
documentation paraphrase, academic phrasing for its own sake, corporate or motivational
tone, a content-design document describing itself, or a lecturer correcting the learner.
Natural prose is mostly simpler prose. Do not make it deliberately casual — no jokes,
filler, stock rhetorical questions, or fake conversational asides.

Talk about the subject, not about the person reading. "The learner should understand
that…" and "the reader can now see why…" narrate the reader in the third person and delay
the fact; state it instead — "`StateFlow` always exposes a current value", "this makes the
lifetime difference explicit". The words *learner* and *reader* are not banned — they are
fine where a sentence genuinely concerns the person reading — but they should not be
routine scaffolding.

### Precise vocabulary over habitual vocabulary

Broad nouns are easy to reach for and often hide what the author means. The recurring
example is **shape**. It is legitimate shorthand when the meaning really is broad
structural form — "API shape", "state shape" — but before using it, ask what exactly is
meant, and prefer the precise term when one exists: function signature, parameter list,
state model, data representation, type structure, ownership structure, object graph, API
contract, interface, call pattern, control flow, layout, collection structure.

| Habitual | Precise, where accurate |
| --- | --- |
| two functions with the same shape | two functions with the same signature |
| the producer shape | the producer-based design, or the mechanism itself |
| the shape worth noticing is what is absent | There is no remembered flag. |

There is no single replacement word; "structure" everywhere is the same habit with a
different noun. The right term depends on what the sentence is about, and sometimes the
right edit is to delete the framing entirely.

**Boundary** is different: it is a real engineering term for architecture, APIs, modules,
layers, dependency injection and platform abstractions, and "the repository forms a
boundary between the UI and the data sources" is exactly right. What reads badly is a
*curriculum* boundary described in engineering language — "this is outside the boundary of
this Unit", "this Lesson stops at the boundary" — which Rule 12 covers.

### Rhetorical framing earns its place

Constructions such as "The point is…", "The important part is…", "What matters is…",
"The useful question is…", "The rule is…", "The distinction is…", "the one worth
noticing", "the honest answer", "worth understanding", "deliberately", and "not X, but Y"
are not wrong. Used once, one of them can direct attention. Used as paragraph boilerplate,
they produce a synthetic voice and delay the fact the sentence exists to state. Usually,
state the fact:

| Framed | Direct |
| --- | --- |
| The important part is that cancellation is not a kill switch. | Cancellation is cooperative; it cannot forcibly stop arbitrary blocking work. |
| The honest answer is that this depends on ownership. | It depends on who owns the state. |

This is an editing judgement, not a phrase list. A framing earns its place when removing it
would lose emphasis the reader genuinely needs.

### Confident about invariants, measured about preferences

Match the strength of the language to the strength of the underlying contract.

A **technical invariant** deserves plain, strong wording: an API guarantees something, a
type makes a state unrepresentable, an operation cannot occur, a lifecycle event
definitely cancels a scope, a correctness condition must hold. Hedging these makes the
Lesson less accurate, not more polite.

An **engineering preference** — an architectural choice, a default, a trade-off — does
not. Words such as *always*, *never*, *must*, *should*, *wrong*, *correct*, *only*,
*obviously*, *simply* and *clearly* are not banned; check each one against what backs it.
When the conclusion depends on requirements, say so: "usually", "prefer", "a useful
default", "when X is required", "the trade-off is", "this becomes useful when…". The
learner should come away able to reason about the decision, not holding the author's
preference as a rule to memorize.

## Rule 8 — Thorough but Bounded

A Lesson should normally represent roughly **5–10 minutes of focused reading**. This is a
design target for scope, not a mechanical word limit — do not pad a clean lesson to reach
it or amputate a coherent one to stay under it.

**Split a Lesson when it contains several independent mental models.** Length is a symptom;
the number of distinct ideas is the actual signal. Two mental models in one lesson means
the learner has to hold both before either is secure.

A typical Lesson contains:

- one primary concept;
- a concise explanation;
- one to three useful code examples;
- the practical consequences;
- one to three important mistakes or trade-offs;
- three to six key takeaways or interview-focus points;
- authoritative Sources;
- optional Senior depth.

The boundary this rule protects: **the learning curriculum must not become another
exhaustive documentation site.** Completeness of a *subject* is the goal; completeness of
an *API surface* is not.

### Concise, not cryptic

Brevity succeeds only when the reasoning survives it. A short paragraph that forces the
learner to reconstruct a missing step from another Lesson is not concise, it is
incomplete. Most editorial tightening works the other way round: remove curriculum
narration, remove rhetorical framing, add the one sentence of technical context the
argument actually needs, and the result is usually shorter and clearer. Four sentences
explaining why a concept is treated elsewhere can almost always become one sentence
stating the relevant fact.

Do not write motivational introductions, generic summaries, repeated recaps, or filler
transitions to reach the time target.

## Rule 9 — Sources

Every shipped production Lesson must remain grounded in authoritative sources.

Prefer, depending on the subject:

- official Android and AndroidX/Jetpack documentation for platform, framework, lifecycle,
  UI, and Jetpack APIs;
- Kotlin official documentation for the language, coroutines, Flow, and Kotlin
  Multiplatform;
- official library or framework documentation for Room, Ktor, Koin, Retrofit, OkHttp,
  kotlinx.serialization, and similar;
- official project repositories, release notes, and design documents where those are the
  authoritative statement of behavior;
- specifications and API reference documentation;
- versioned library source, where the implementation itself is the evidence for the claim;
- the original or canonical writing for an architectural concept.

The source-quality philosophy of `docs/content/content-authoring.md` applies: a source must
support the specific claim being made rather than merely be a page about the same subject,
and SEO interview-question collections, anonymous forum answers, and content farms are not
technical authority.

Sources serve two purposes here. They support the claims the Lesson makes, and they remain
available to a learner who needs exact reference detail the Lesson deliberately did not
include. That second purpose is what makes bounded lessons acceptable:

> Learning content is a curated, interview-oriented explanation. It is not a replacement
> for authoritative documentation.

The freshness rules of the question bank apply as well. Compose APIs and recommendations,
platform restrictions, background execution, and navigation APIs date quickly and need
re-checking on material edits; Kotlin language semantics and architecture principles are
comparatively stable.

A blueprint is not required to author production Sources — that is the job of the Lesson.
A blueprint should, where useful, identify the likely authoritative **source families** for
each area so the author does not start from a blank search.

### Authority is claim-specific

Whether a source is authoritative depends on the claim it supports, not on who hosts it.
Learning material spans Android, Kotlin, Compose, coroutines, architecture, Room, Koin,
Dagger and Hilt, Kotlin Multiplatform, and the subjects the curriculum grows into, so the
authoritative source for one claim is often not vendor documentation at all. A secondary
source is acceptable when it is itself the authoritative or canonical source for the concept
being cited — the writing of the person who named or originated an architectural pattern is
the primary statement of that pattern, not an inferior substitute for Android
documentation. Primary sources remain preferred wherever one exists for the claim.

### Source code is cited at an immutable revision

When a Lesson links directly to repository source because the implementation or its KDoc
supports the claim, the citation must identify immutable source: a full 40-character commit
SHA, or an explicitly immutable release or version tag (for example
`kotlinx.coroutines/1.11.0`). Never cite a moving branch such as `main`, `master`, or
`androidx-main` for source the Lesson relies on — the file can change after the Lesson
ships, and the citation would then silently stop supporting the claim. This applies to raw
(`raw.githubusercontent.com`) and rendered (`github.com/.../blob/...`) links alike. When
re-pinning, resolve the branch tip once, use the same SHA for every citation updated
together, and re-read each file at that revision to confirm it still supports the claim.

### What validation can and cannot check

`LearningCurriculumValidator` (`shared/.../curriculum/learning/validation/`) enforces the
machine-checkable half of this rule: every active Lesson carries at least one Source, and
every Source has a non-blank, non-placeholder title and a syntactically valid `http(s)` URL
whose host is not an unreachable one such as `localhost`.

Whether a Source is **authoritative**, current, and actually supports the claim it is
attached to is an editorial judgement and stays with the author and reviewer. It is
deliberately not expressed as a hostname allowlist: authoritative documentation for
Android, Kotlin, Room, Ktor, Koin, and every library the curriculum grows into lives on
hosts no fixed list could enumerate, so an allowlist would reject valid sources while
proving nothing about the ones it admits. Learning Sources are therefore editorially
reviewed rather than hostname-gated, unlike the question bank's source-host allowlist.

One structural rule is machine-checked in the shipped-content suite rather than the
validator: `BundledLearningCurriculumTest` requires every AndroidX source citation to pin a
full commit SHA, because AndroidX citations are where a moving branch actually shipped. The
test checks the committed URL shape only and never reaches the network; confirming that a
pinned file exists and supports the claim is the author's job at pinning time.

## Rule 10 — Relationship to the Question Bank

The existing Question bank is a valuable **acceptance aid** for lesson authoring. A Lesson
that leaves its Subtopic's questions unanswerable has probably under-taught something.

As a rough alignment:

- **Foundation** questions should be supported by the Lesson's Core material.
- **Applied** questions should be supported by its practical reasoning and examples.
- **Advanced** questions should have the necessary mechanism and trade-off coverage in the
  deeper material, where the Lesson carries deeper material at all.

Two limits on this, both important:

**Structural mapping does not prove semantic teaching coverage.** Declaring
`primarySubtopicIds = [compose_stability]` does not mean the Lesson teaches enough about
stability. Authors must actually read the questions for the mapped Subtopics and judge
whether the Lesson teaches enough *reasoning* to answer them — not whether the words
overlap.

**Do not map Lessons to individual Question IDs.** Question IDs change status, get
deprecated, and get added; a Lesson pinned to a question list rots immediately. The
structural relationship is to **stable Subtopic concepts**, and the question review is a
human judgement performed at authoring and review time.

And the rule that outranks both:

> **Learning content must teach the concept, not coach the wording of the quiz answer.**

If a lesson would read differently because of how a specific distractor is phrased, that
is a defect in the lesson. A learner who understands the concept should be able to answer
questions this bank has not written yet.

### Finding the questions to read

`docs/content/learning-question-coverage.md` lists, per Lesson, the active Questions its
primary Subtopics currently reach, split by level, with supporting concepts reported
separately. Use it to find what to read; it is structural evidence, and the judgement of
whether the Lesson actually teaches that reasoning stays with this rule.

The snapshot is generated, so regenerate it in the same change that alters learning
content, learning mappings, or any active Question:

```sh
python3 tools/learning_question_coverage.py --write   # rewrite the snapshot
python3 tools/learning_question_coverage.py --check    # fail if it is stale
```

## Rule 11 — Lessons Are Project-Agnostic

Learner-facing curriculum text teaches Android, Kotlin, Compose, KMP and the surrounding
engineering concepts. It is not documentation for this application, and a reader must
never need to know how this application is built in order to follow a Lesson.

Concrete examples are not the problem — they are most of what makes a Lesson teachable.
The problem is the difference between presenting an arrangement and reporting this
codebase's own. "Consider an application whose repositories expose one-shot suspending
reads" teaches a design; "every repository in this application exposes one-shot
suspending reads, verified across every interface" reports a fact about the repository the
Lesson happens to ship inside, and a reader who has not read that repository cannot check
it, learn from it, or carry it anywhere else.

### Worked examples use invented identities

Worked examples normally use neutral or invented names — a `QuestionRepository`, an
`OrderViewModel`, a `networkModule` — chosen to demonstrate the design, not borrowed from
the code that ships alongside the curriculum. Borrowing real names is how a Lesson drifts
from teaching a design into reporting on an implementation: once the example *is* this
repository, the next sentence cites its file count, its KDoc or its current module list as
proof.

A Lesson therefore does not use this repository's actual classes, repositories, modules,
package structure, file counts, KDoc, dependency setup, object graph, DI modules or
source-tree state as evidence for the technical claim it teaches. The claim rests on the
reasoning, the example and the Sources of Rule 9.

| Report about this codebase (avoid) | Worked example (prefer) |
| --- | --- |
| The data module currently contains nine repositories, and none of them exposes a `Flow`. | Consider a data layer whose repositories expose only one-shot suspending reads. |
| This project currently declares no custom scopes; its Koin modules use `single` and `factory` only. | Take a graph with no custom scopes, where every binding is `single` or `factory`. |
| As the KDoc on the real repository says, "…" | *(state the point in the Lesson's own voice)* |

This is not a ban on named types. An invented `QuestionRepository` with one implementation
is concrete and teachable; the problem is coupling the Lesson to the real one.

### No statements about the present state of this codebase

Temporal statements about this repository's implementation — "this repository uses…",
"the graph currently declares…", "there are currently N…", "at the moment the app…",
"the app presently…" — describe a snapshot of the software the curriculum ships with. They
are wrong in conceptual material: the learner cannot check them, and they go stale on the
next commit. The exception is a versioned external API contract, where the version is
part of the fact: "in lifecycle-runtime-compose 2.11.0-beta01, …".

The words themselves are not the problem. "Currently" and "at the moment" are ordinary
technical language when they describe runtime state ("the currently executing recompose
scope", "at the moment of the read"), a modelling distinction ("is this currently true, or
did it happen?"), or the state inside a worked example ("a title that is currently on
hold").

### Practical consequences

- Do not write "this project", "this repository", "this app", "this application", "in the
  current implementation", or any phrasing that makes the learning app itself the subject.
- Frame worked examples as examples: "consider an application where…", "here is a worked
  instance of…", "take a `QuestionRepository` with one implementation…".
- Attribute a version-specific result to what actually determines it — the target and the
  library version — and state it as a result, not as a record of this build: "on the JVM,
  with kotlinx-coroutines-core 1.11.0, …" is reproducible; "measured on this project's JVM
  target" is not. Rule 12 covers narrating how a result was obtained.
- Do not quote this codebase's own KDoc as though it were a cited source. Reserve quotation
  marks for the authoritative sources of Rule 9.
- Keep the caveats. A worked example that is poor evidence for something should still say
  so — just as a property of the example, not as a confession about this repository.

None of this is a reason to weaken an explanation. Replacing a specific claim with vague
wording loses the teaching point, which is a worse outcome than the coupling it removed.
Examples stay concrete; only their identity changes.

## Rule 12 — Lessons Are Written for Learners, Not the Curriculum Graph

Rule 11 keeps this application out of the Lesson. This rule keeps the curriculum's own
machinery — its structure, its authoring decisions and how it was produced — out of it.
A learner should experience the subject, not the architecture of the course that teaches
it.

### Internal identity stays internal

Unit, Lesson, Topic, Subtopic and Question IDs, and the fields that hold them, never
appear in learner-facing prose as a way of referring to curriculum items. Text such as
"`lesson_state_flow` owns those mechanics" or "see `lesson_composable_identity`" exposes a
database key where the learner needed an explanation.

The test is semantic, not typographic. snake_case is not the problem: a real technical
identifier — a column such as `is_overdue`, a package path, a JSON field, a Gradle
property — belongs in the Lesson whenever it is part of what is being taught, in code,
schema, SQL or API examples, or inline in prose. A token that happens to begin with
`lesson_` can be a legitimate package name in a worked example. The question is whether
the identifier belongs to the subject or to the curriculum.

The same applies to other product metadata: backlog keys, epic and issue numbers, and
status values have no place in a Lesson.

### Each Lesson is locally comprehensible

A competent Android engineer should be able to open a Lesson directly and follow its
central reasoning without knowing which numbered Unit precedes it, what an internal ID
means, the blueprint, the backlog work that produced it, or the curriculum graph.

This does not mean re-teaching every prerequisite. It means supplying the minimum context
the current argument requires. A useful test for any paragraph:

> If the learner arrived here directly from search six months after reading the
> prerequisite, would this paragraph still make sense?

If not, add the missing technical fact — usually a clause or a sentence. Do not add
curriculum narration in its place; "as covered earlier" tells the learner that something
is missing without supplying it.

### Cross-references supplement explanation

A reference to another Lesson or Unit is useful when the current Lesson has already given
enough context for the present argument, the referenced material genuinely goes deeper,
and the learner benefits from knowing where to continue. It must never substitute for
explanation the current argument needs:

| Substitutes for explanation | Explains, then optionally points |
| --- | --- |
| `lesson_state_flow` owns those mechanics. | `StateFlow` always has a current value, so the UI can render as soon as collection starts. For sharing and conflation, see **StateFlow, SharedFlow and Hot Streams**. |
| This was covered in Unit 3. | *(state the fact the argument depends on; drop the reference if it adds nothing)* |
| Unit 4 covers cancellation. | Cancellation is covered in **Cancellation, Failure and Coordination**. — or nothing, if the reference does not help this explanation. |

When another Lesson or Unit is named, use its human-facing title. Avoid Unit and Lesson
numbers and relative positions ("the next Unit", "the earlier Lesson"): authored order can
change and a reader who arrived from search has no position, while a title stays
meaningful. Do not add a reference just because `relatedLessonIds` contains one.

A reference must resolve to a Unit or Lesson that exists in the shipped curriculum, under
the title it ships with. Planned, future or imagined material — "the testing curriculum",
"the performance unit", "a later architecture unit" — is not a destination the learner can
reach. When a concept has no shipped home, state the fact the argument needs locally and
omit the reference. By the same reasoning the word *curriculum* does not belong in Lesson
prose by default; name the technical subject instead ("Flow sharing", not "the Flow
curriculum").

Because titles now serve as references, a Lesson title must be distinctive enough to
identify its target when cited elsewhere. Two titles that differ by a single word —
**What Delivery Guarantee Does This Occurrence Need?** and **What Guarantee Does This
Occurrence Need?** — cannot be told apart in a sentence that cites one of them. Check a new
or changed title against the existing ones before shipping it.

### Authoring decisions become teaching, not narration

The decisions this contract asks authors to make — what a Lesson owns, which concepts are
supporting, where the full treatment lives, what is bridged or excluded — should be
visible in *what* the Lesson explains, not described *to* the learner. Phrases such as
"this Lesson owns…", "the Flow curriculum owns…", "this concept belongs to another Unit",
"the complete treatment lives in…", "this Unit deliberately stops here", "we do not
re-teach…", "the earlier Unit established…" and "the next Unit will explain why" are not
the default way to write a Lesson.

| Authoring decision | Learner-facing result |
| --- | --- |
| `StateFlow` sharing is supporting material here; full treatment belongs to the Flow Unit. | "`StateFlow` keeps a current value, which lets the UI render immediately when collection starts." Optionally: "For sharing policies and conflation, see **StateFlow, SharedFlow and Hot Streams**." |

The learner does not need to know why the material was divided that way. Scope can still
be stated when it helps the learner reason — "this Lesson assumes a single-module app" is
a fact about the example — but the default is to teach within the scope rather than
describe it.

The same holds for how the material was produced and maintained. State the technical
result, the API contract and any version qualification; do not narrate the verification
("we checked this against…", "this was verified against…", "the resolved source shows…")
or address future maintainers ("a future dependency update should re-check…",
"maintainers should revisit…", "this table should be updated when…"). Freshness is the
author's job under Rule 9, not the learner's reading.

| Author-process narration | Result |
| --- | --- |
| This was checked against lifecycle-runtime-compose 2.11.0-beta01, and future updates should re-read the implementation. | In lifecycle-runtime-compose 2.11.0-beta01, the conversion uses `produceState` and `repeatOnLifecycle`. The API may change before the stable release. |

The version caveat stays; the maintenance instruction goes.

### Summaries state the idea

A Unit or Lesson summary states the engineering idea itself, concisely, in learner-facing
language. It does not describe what the Lesson is going to do, tell the learner what they
will learn, instruct them, recap earlier Units, preview later ones, enumerate the sequence,
or explain where the material sits in the course.

| Plan or objective | The idea |
| --- | --- |
| In this Lesson, you will learn how `LaunchedEffect` works and when to use it. | `LaunchedEffect` owns coroutine work whose lifetime is tied to a composable call site and whose restart policy is expressed through keys. |
| This Unit builds on the previous Unit and introduces screen-level ownership. | Screen state ownership depends on the lifetime that must preserve the state. |

Learning objectives belong in the blueprint (Rule 1), where they guide authoring; the
summary a learner reads is the idea those objectives aim at.

## Authoring Checklist

Before a blueprint is considered complete:

- [ ] The complete interview-relevant subject is mapped, including what will not be
      taught.
- [ ] Conceptual dependencies and Unit ordering are recorded, with reasons.
- [ ] Every Unit divides into focused Lessons with stated learning objectives.
- [ ] Every mapped area carries a Teach / Bridge / Reference / Exclude decision.
- [ ] Every Lesson lists primary and supporting concepts separately.
- [ ] Every Topic and Subtopic ID used is a real ID from the bundled curriculum, and
      every one an ACTIVE Unit or Lesson uses is ACTIVE under an ACTIVE Topic.
- [ ] Concepts with no exact assessment Subtopic are documented as gaps, not invented.
- [ ] Exclusions carry a one-line reason.

Before a Lesson is considered ready to ship:

- [ ] It teaches one primary concept, or splits.
- [ ] Core explains the concept in plain English with a correct mental model.
- [ ] Practical material reflects real engineering, including at least one failure mode.
- [ ] Senior depth is present only where it is genuinely deeper reasoning.
- [ ] It is roughly 5–10 minutes of focused reading.
- [ ] Supporting concepts are explained to the depth this Lesson needs, and no further —
      enough locally for the current argument, before any reference elsewhere.
- [ ] Cross-references supplement rather than replace explanation, name the target by its
      human-facing title, and appear only where they help the learner continue.
- [ ] Every cross-reference resolves to a shipped Unit or Lesson — nothing planned or
      imagined — and its title cannot be confused with another; the Lesson's own title is
      distinct enough to be cited the same way.
- [ ] The Unit and Lesson summaries state the idea, not the Lesson plan, an objective, a
      recap or a preview.
- [ ] Sources are authoritative and support the specific claims made.
- [ ] Source-code citations pin a full commit SHA or an immutable release tag, never a
      moving branch.
- [ ] The questions for its primary Subtopics are answerable by a reader who understood
      it — verified by reading them, not by mapping them.
- [ ] The Lesson teaches the concept rather than the phrasing of any question.
- [ ] Worked examples use neutral or invented identities; nothing cites this repository's
      classes, modules, counts, KDoc or current state as evidence, nothing says what this
      repository "currently" does, and version-specific results name their version.
- [ ] No author-process or maintenance narration: results are stated, not how they were
      checked or who should re-check them.
- [ ] No Unit, Lesson, Topic, Subtopic or Question ID, backlog key or status value appears
      in learner-facing text; any snake_case shown to the learner is a real technical
      identifier.
- [ ] The Lesson is understandable without Unit numbers, the curriculum graph, or
      curriculum-ownership narration — checked in titles, summaries, callouts, table
      headers and cells, and code comments, not only paragraphs; "boundary" refers to
      engineering, not to the course, and "curriculum" does not appear by default.
- [ ] "The learner" and "the reader" are not used as routine framing; sentences state the
      technical fact.
- [ ] "Shape" and similarly broad words are used only where they are more accurate than a
      specific term, and repeated rhetorical framing has been edited into direct statements.
- [ ] Strong prescriptive language rests on a real invariant or contract; preferences and
      trade-offs are qualified.
- [ ] It sounds like an engineer explaining the concept, and its concision does not depend
      on omitted reasoning.
