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
the same shape and must not be forced into one:

- Do **not** require one Learning Unit per Subtopic.
- Do **not** require one Lesson per Subtopic.
- Do **not** require one Question family per Lesson.

A Learning Unit routinely covers several existing Subtopics, and a single Subtopic is
routinely touched by more than one Lesson at different depths. The Question Subtopic
remains the stable bridge into assessment coverage — it is the vocabulary both sides
share, not a structural constraint on either.

The reason the shapes differ is that they were optimized for different jobs. The question
taxonomy is intentionally granular because fine-grained Subtopics make coverage
measurable, practice selection precise, and progress meaningful. Granularity that helps
assessment hurts teaching: a learner does not want fourteen disconnected micro-lessons
that each match one Subtopic, they want a coherent path through a subject.

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
complete treatment of a concept. This is what makes bounded supporting explanation
honest — the learner is told where the full story is instead of being left with a
half-explanation.

A well-formed pointer reads like this:

> `StateFlow` is introduced here as the ViewModel-to-UI state carrier. See the Flow
> curriculum for hot and cold streams, sharing strategies, operators, and cancellation.

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
versus `stateIn`, replay caches, or subscription timeouts. That is what the pointer to
related content is for.

Primary and supporting relationships are therefore *not* required to stay inside the home
Topic, and a blueprint that keeps every mapping inside its own Topic is usually a sign
that bridging was avoided rather than that it was unnecessary.

## Rule 4 — Teach, Bridge, Reference, Exclude

Every area a blueprint maps gets exactly one of four editorial decisions.

**Teach** — interview-relevant concepts this Topic must teach properly. These become
primary concepts of some Lesson, with Core depth at minimum.

**Bridge** — concepts primarily owned by another Topic that are required to understand the
current material. Explain enough locally for the current Lesson to stand on its own, then
point to the deeper treatment. Bridged concepts appear as supporting concepts.

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
  authoritative statement of behavior.

The source-quality philosophy of `docs/content/content-authoring.md` applies unchanged: a
source must support the specific claim being made rather than merely be a page about the
same subject; secondary sources are exceptional; SEO interview-question collections and
anonymous forum answers are not authoritative.

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
proving nothing about the ones it admits.

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

## Authoring Checklist

Before a blueprint is considered complete:

- [ ] The complete interview-relevant subject is mapped, including what will not be
      taught.
- [ ] Conceptual dependencies and Unit ordering are recorded, with reasons.
- [ ] Every Unit divides into focused Lessons with stated learning objectives.
- [ ] Every mapped area carries a Teach / Bridge / Reference / Exclude decision.
- [ ] Every Lesson lists primary and supporting concepts separately.
- [ ] Every Topic and Subtopic ID used is a real ID from the bundled curriculum.
- [ ] Concepts with no exact assessment Subtopic are documented as gaps, not invented.
- [ ] Exclusions carry a one-line reason.

Before a Lesson is considered ready to ship:

- [ ] It teaches one primary concept, or splits.
- [ ] Core explains the concept in plain English with a correct mental model.
- [ ] Practical material reflects real engineering, including at least one failure mode.
- [ ] Senior depth is present only where it is genuinely deeper reasoning.
- [ ] It is roughly 5–10 minutes of focused reading.
- [ ] Supporting concepts are explained to the depth this Lesson needs, and no further.
- [ ] Related and deeper content is linked where a concept was deliberately bounded.
- [ ] Sources are authoritative and support the specific claims made.
- [ ] The questions for its primary Subtopics are answerable by a reader who understood
      it — verified by reading them, not by mapping them.
- [ ] The Lesson teaches the concept rather than the phrasing of any question.
