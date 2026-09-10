# Coroutines and Flow Learning Blueprint

## Purpose

This is the complete learning map for Kotlin coroutines and Flow, produced under
`docs/content/learning-content-authoring.md` before any production Lesson for the subject
is authored. It is the second Topic mapped under that contract, after
`docs/content/compose-learning-blueprint.md`.

It is a **plan, not content**. No Lesson text is authored here, and nothing in this file is
a runtime artifact. Authoring proceeds Unit by Unit against this map, through E24-02
to E24-07.

Home Topic: `async_reactive` (Coroutines, Flow & Reactive Programming).

Scope: 6 Learning Units, 29 planned Lessons, plus explicit Reference and Exclude
decisions and a record of concepts the current assessment taxonomy cannot express.

The confirmed authoring plan — stable identities, prerequisites, boundaries, the semantic
Question review and the version-sensitive claims — is in
[`coroutines-flow-units-1-6-plan.md`](coroutines-flow-units-1-6-plan.md). This blueprint
holds objectives, depth layers and editorial decisions; that document holds what the
review had to settle.

## How to Read This Blueprint

Every planned Lesson records:

- **Objective** — what the learner should be able to do afterwards.
- **Core / Practical / Senior** — the concepts belonging to each depth layer, per Rule 6 of
  the authoring contract. A missing Senior line means deeper material would be artificial.
- **Primary** — Subtopic IDs the Lesson is responsible for teaching thoroughly.
- **Supporting** — Subtopic IDs the Lesson explains only far enough to stay understandable,
  including cross-Topic bridges. Cross-Topic IDs are annotated with their owning Topic.
- **Notes** — Teach/Bridge/Reference/Exclude decisions, prerequisites, and pointers to
  deeper future content.

Every Topic and Subtopic ID below is a real ID from the bundled curriculum
(`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json`) and was
checked `ACTIVE` on 2026-09-10. Concepts with no exact Subtopic are recorded in
[Taxonomy gaps](#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic) rather than
given invented IDs. **This blueprint does not change the question taxonomy.**

The `L1.1` style labels are positional reading aids for this document and its companion
plan. They are **not** identities; the stable ids are in the plan's identity tables and are
never numbered by position.

## The Central Questions

This subject is where most production Android defects and most senior interview signal
live, and both come from the same place: code that is syntactically fine and whose
lifetime, execution context, or failure path nobody can name. Every Lesson below is
designed so that a reader can answer some of these about code in front of them.

- What owns this coroutine's lifetime, and what actually cancels it?
- Does this operation suspend or block?
- What execution context is this actually running on?
- Is this work sequential, or concurrent on purpose?
- If this child fails, who else fails?
- What happens to unfinished work?
- Is this Flow cold or shared? Who starts collection, and what ends it?
- Do I need every value, or only the latest? Should new input cancel work already running?
- Am I pairing emissions, or combining current values?
- Is this abstraction representing state, a stream, or message delivery — and what
  delivery guarantee does it actually make?

The corollary is an authoring rule: examples and eventual assessment are built around
**predicting behaviour, finding defects, and choosing abstractions**, never around
recalling which operator has which name.

## Unit Order and Why It Matters

1. Coroutine Fundamentals and Structured Concurrency
2. Coroutine Context, Dispatchers and Concurrent Work
3. Cancellation, Failure and Coordination
4. Flow Fundamentals
5. Flow Composition, Timing and Failure
6. StateFlow, SharedFlow and Hot Streams

The order encodes conceptual dependencies, not convenience:

- **Suspension (1) before everything.** Every later Unit assumes the learner can say what
  suspension is and is not. Taught after dispatchers, "suspend" silently becomes "runs in
  the background", which is the single most damaging misconception in the subject.
- **Lifetime and structure (1) before context (2).** A dispatcher decides *where* code
  runs; a scope decides *whether it runs at all and for how long*. A learner who meets
  `Dispatchers.IO` before `CoroutineScope` concludes that coroutines are a threading API.
- **Context and concurrency (2) before cancellation and failure (3).** Failure propagation
  is a property of the `Job` in the context, and `async`'s failure semantics cannot be
  discussed before `async` has been introduced as intentional concurrency.
- **Cancellation and failure (3) before Flow (4).** Flow cancellation *is* coroutine
  cancellation, Flow's exception transparency is a constraint on the same exception model,
  and collection lifetime is scope lifetime. Teaching Flow first means teaching all three
  twice, badly.
- **Cold Flow (4) fully before composition (5).** Operators are only comprehensible once
  "nothing runs until someone collects" is secure. Buffering in particular is meaningless
  without the default backpressure model established in Unit 4.
- **Composition (5) before hot streams (6).** `stateIn` and `shareIn` take an upstream cold
  Flow and a sharing policy; both halves must already exist. And `StateFlow`'s conflation
  is only interesting against Unit 5's `conflate`, `collectLatest` and `buffer`.
- **Hot streams (6) last.** It is the Unit where the subject's real decision — plain value,
  cold Flow, `StateFlow`, `SharedFlow`, or `Channel` — can finally be posed with every
  input available. Placed earlier it degrades into the `StateFlow`-is-state slogan.

The sequence deliberately does **not** open with `viewModelScope` and a screen-state
example, even though that is where most readers first meet coroutines. Starting at the
Android integration point teaches the integration and hides the model.

---

## Unit 1 — Coroutine Fundamentals and Structured Concurrency

**Purpose:** establish what suspension is, where a coroutine's lifetime comes from, and
what the parent-child structure guarantees, before any execution or failure detail.
**Prerequisites:** none inside this subject. Kotlin lambdas and higher-order functions are
bridged where builder blocks need them.

#### L1.1 — Suspension Is Not Blocking

- **Objective:** decide, for a given call, whether it suspends or blocks, and say what
  `suspend` does and does not promise.
- **Core:** a suspending function can pause and resume without holding its thread; the
  thread is released to other work while it waits; `suspend` is a compile-time contract
  about *how* a function may pause, not a statement about *where* it runs; a suspending
  function can only be called from another suspending function or a coroutine builder.
- **Practical:** `delay` against `Thread.sleep` in the same coroutine; a `suspend` function
  that does blocking work inside it and is therefore not main-safe; why "I made it
  `suspend`, so it is off the main thread" is false; recognising a suspension point.
- **Senior:** what suspension buys — many more coroutines than threads, because a suspended
  coroutine costs a continuation rather than a stack; and the consequence, that a coroutine
  which never suspends monopolises its dispatcher thread for as long as it runs and never
  reaches a point where cancellation can be observed. **Keep "blocking" precise while making
  that point.** A CPU-bound loop is doing work and belongs on `Dispatchers.Default`; a
  blocking call holds a thread that is doing nothing and belongs on `Dispatchers.IO`. The two
  share a consequence — the thread is unavailable and cancellation goes unnoticed — and they
  are not the same thing. Collapsing them here would contradict this Lesson's own objective
  and would remove the ground L2.2's dispatcher argument stands on.
- **Primary:** `coroutine_fundamentals`
- **Supporting:** `coroutine_dispatchers`, `jvm_fundamentals` (kotlin_language),
  `android_main_thread` (android_platform)
- **Notes:** Threads appear only as the contrast case — **Bridge**, not Teach. Continuation
  passing style, state-machine generation and the compiler transform are **Exclude**: no
  product decision depends on the generated shape, and naming it invites trivia. The one
  sentence of mechanism worth keeping is "a suspended coroutine is a stored continuation",
  which is what makes the cost argument concrete. Dispatchers are named and deferred to
  Unit 2.

#### L1.2 — Starting Coroutines: `launch`, `async` and `runBlocking`

- **Objective:** choose the builder that matches the intent, and say what each one returns.
- **Core:** `launch` starts work whose result is not needed and returns a `Job`; `async`
  starts work that produces a value and returns a `Deferred`; `runBlocking` is a bridge
  from non-coroutine code that blocks the calling thread until its body finishes; builders
  are extensions on `CoroutineScope`, which is why they cannot be called from nowhere.
- **Practical:** `async { }` whose `Deferred` is never consumed, and why `launch` states
  the intent better; `runBlocking` on the main thread as a self-inflicted ANR; `runBlocking`
  as legitimate in `main` functions and tests.
- **Primary:** `coroutine_builders`
- **Supporting:** `coroutine_fundamentals`, `coroutine_scope`, `coroutine_parallelism`,
  `kotlin_lambdas` (kotlin_language)
- **Notes:** `async` is introduced here only as "the builder that returns a value".
  Intentional concurrency is L2.4 and its failure semantics are L3.3 — both are named and
  deferred. `withContext` is deliberately absent: it is not a builder and putting it in this
  list is what produces the belief that it starts a coroutine. `CoroutineStart` modes and
  `LAZY` are **Reference**, mentioned in one line at most.

#### L1.3 — `Job`: the Handle That Carries Lifetime

- **Objective:** describe what a `Job` represents and use it to reason about a coroutine's
  progress and its relationship to other coroutines.
- **Core:** every coroutine has a `Job`; the `Job` is what you hold to wait for, inspect or
  cancel the coroutine; a `Job` launched inside another coroutine becomes its child; a
  parent does not complete until its children complete; `join` waits, `isActive` reports.
- **Practical:** `launch { }` returning a `Job` that is discarded, and when that is fine;
  waiting for several children with `joinAll`; reading a completed-versus-cancelled outcome;
  where `Deferred` fits as a `Job` that also carries a value.
- **Senior:** the lifecycle states worth knowing — active, completing, completed,
  cancelling, cancelled — and specifically that "completing" exists because a parent whose
  own body has finished must still wait for its children.
- **Primary:** `coroutine_jobs`
- **Supporting:** `coroutine_scope`, `coroutine_context`, `structured_concurrency`
- **Notes:** Cancellation is named as something a `Job` can carry and is **deferred whole**
  to Unit 3; this Lesson must not start teaching cooperative cancellation. The full state
  diagram with its transition table is **Reference** — teach the five states and why
  "completing" exists, not the table.

#### L1.4 — `CoroutineScope` and Who Owns a Coroutine's Lifetime

- **Objective:** name the owner of any coroutine in a piece of code, and identify code where
  nobody owns it.
- **Core:** a scope is a context with a `Job` in it, and launching from a scope makes that
  `Job` the parent; the scope's owner is whoever cancels it; a coroutine outlives nothing
  and nobody by accident — it lives exactly as long as its scope allows.
- **Practical:** a class that creates `CoroutineScope(SupervisorJob() + Dispatchers.Default)`
  and never cancels it; `GlobalScope` and why "it needs to outlive the screen" is a reason
  to inject a scope with a real owner, not to discard ownership; lifecycle-owned scopes such
  as `viewModelScope` as the shape to imitate; work that genuinely must survive the screen,
  and why that is a scheduling problem rather than a scope problem.
- **Senior:** the leak this prevents is not only memory — an un-owned scope keeps producing
  results for a screen that is gone, retains everything the coroutine captured, and turns
  every failure into an unattributable crash.
- **Primary:** `coroutine_scope`
- **Supporting:** `coroutine_jobs`, `lifecycle_coroutines`, `coroutine_leaks` (performance),
  `viewmodel_lifecycle` (lifecycle_navigation)
- **Notes:** **Bridge** to `lifecycle_navigation` and `architecture`: `viewModelScope` is
  shown as an example of an owned scope, not taught as ViewModel architecture. WorkManager
  is named in one sentence as where survive-the-process work belongs and is otherwise
  **Exclude** — it is `background_work`'s subject. `SupervisorJob` appears in the scope
  construction idiom and is explicitly deferred to L3.4 rather than half-explained.

#### L1.5 — Structured Concurrency and What It Guarantees

- **Objective:** state the three guarantees structured concurrency makes, and the ones it
  does not make.
- **Core:** coroutines form a parent-child tree; a parent waits for its children; cancelling
  a parent cancels its children recursively; an uncaught child failure cancels its parent.
  `coroutineScope { }` is the builder that gives a suspending function its own sub-tree and
  does not return until everything inside it is finished.
- **Practical:** a suspending function that starts work and returns before the work is done,
  and what that costs the caller; the same function rewritten with `coroutineScope`;
  concurrent decomposition inside a suspending function without leaking concurrency into
  its signature.
- **Senior:** the guarantee is about *lifetime*, not scheduling — children still run
  concurrently, still inherit context, and can still override their dispatcher; and the
  reason the guarantee is valuable is that it makes "is this work still running?" a question
  with an answer.
- **Primary:** `structured_concurrency`
- **Supporting:** `coroutine_jobs`, `coroutine_scope`, `coroutine_builders`,
  `coroutine_exceptions`
- **Notes:** Failure propagation is stated as a fact of the model here and **owned** by
  L3.3; supervision is named as the exception to it and owned by L3.4. `supervisorScope` is
  not introduced in this Unit. The `coroutineScope`-versus-`withContext` distinction belongs
  to L2.3 and is deferred there.

---

## Unit 2 — Coroutine Context, Dispatchers and Concurrent Work

**Purpose:** answer "where does this actually run" and "should this work overlap", and
replace the folklore that `suspend` means IO and `async` means faster.
**Prerequisites:** Unit 1.

#### L2.1 — `CoroutineContext` and What Children Inherit

- **Objective:** read a coroutine's effective context off the code that launched it.
- **Core:** the context is an indexed set of elements, keyed by type; the elements that
  matter are the `Job`, the dispatcher, and — later — the exception handler and the name;
  elements combine with `+`, and a later element of the same key replaces an earlier one;
  a child inherits its parent's context and gets a **new** `Job` whose parent is the
  parent's `Job`.
- **Practical:** `launch(Dispatchers.IO)` inside a `Dispatchers.Main` scope, and what each
  element of the resulting context came from; reading `coroutineContext[Job]`; naming a
  coroutine for a readable stack trace.
- **Senior:** the `Job` is the one element that is never simply inherited, and that is the
  whole of structured concurrency expressed as a context rule. Which is also why passing a
  `Job` explicitly into a builder is a re-parenting operation and not a configuration
  option — the coroutine leaves the scope's tree.
- **Primary:** `coroutine_context`
- **Supporting:** `coroutine_jobs`, `coroutine_dispatchers`, `structured_concurrency`
- **Notes:** `CoroutineExceptionHandler` is named as a context element and **deferred** to
  L3.3; `SupervisorJob` likewise to L3.4. The `launch(SupervisorJob())` trap is set up here —
  the re-parenting rule is a context fact — and its supervision half is completed in L3.4;
  neither Lesson teaches the whole of it alone, which is recorded deliberately.
  `ThreadLocal` context elements and `CopyableThreadContextElement` are **Exclude**:
  genuine, rare, and no reasoning objective needs them.

#### L2.2 — Dispatchers and Where Code Actually Runs

- **Objective:** choose a dispatcher from the shape of the work rather than from habit.
- **Core:** a dispatcher decides which thread or pool a coroutine resumes on;
  `Dispatchers.Default` is sized for CPU-bound work; `Dispatchers.IO` is sized to tolerate
  blocking calls; `Dispatchers.Main` is the single UI thread on platforms that have one.
- **Practical:** blocking file or database work on `Default` starving unrelated computation;
  the documented behaviour that `IO` and `Default` **share threads**, so
  `withContext(Dispatchers.IO)` from `Default` frequently is not a thread switch at all;
  `limitedParallelism` as the way to bound a specific resource; injecting dispatchers
  instead of hard-coding them.
- **Senior:** `Dispatchers.IO`'s parallelism default (64 or the core count, whichever is
  larger) and its elasticity — views obtained through `limitedParallelism` are not bounded
  by that limit — and what that means for a codebase that treats `IO` as free.
- **Primary:** `coroutine_dispatchers`
- **Supporting:** `coroutine_context`, `android_main_thread` (android_platform),
  `main_thread_performance` (performance), `anr` (performance)
- **Notes:** `Dispatchers.Unconfined` is **Reference** — one line saying it exists, is an
  advanced tool, and is not a performance optimisation. Custom executors and
  `asCoroutineDispatcher` are **Reference**. The exact thread-pool implementation is
  **Exclude**. KMP caveat is mandatory: `Dispatchers.Main` requires a platform main
  dispatcher and is not available on every target, so shared code should take a dispatcher
  rather than name `Main` — see the plan's source-freshness section.

#### L2.3 — `withContext` and Main-Safety

- **Objective:** decide where a context switch belongs and recognise one that does nothing.
- **Core:** `withContext` runs a block in a modified context and suspends until it returns a
  value; it starts no concurrent work; its effect ends with the block. Main-safety is the
  contract that a suspending function is safe to call from the main thread, and it is the
  responsibility of the function doing the work, not of its caller.
- **Practical:** a repository function that wraps its own blocking call in
  `withContext(ioDispatcher)`; the caller that then wraps it again, achieving nothing but a
  dispatch; an already-asynchronous client — Retrofit, Ktor, Room — that needs no wrapper at
  all; `withContext` compared with `coroutineScope`, which changes no context but creates a
  child sub-tree.
- **Senior:** a redundant `withContext` is not free — it costs a dispatch and a resumption
  each way — but the reason to remove it is that it documents a false belief about where the
  work happens.
- **Primary:** `coroutine_context_switching`
- **Supporting:** `coroutine_dispatchers`, `coroutine_fundamentals`, `structured_concurrency`,
  `repository_pattern` (architecture)
- **Notes:** **Bridge** to `architecture` for where main-safety sits in a layered design;
  the layering itself is not taught. Testing dispatchers is named as the second reason to
  inject them and **deferred whole** to the testing curriculum (E31).

#### L2.4 — Sequential by Default, Concurrent on Purpose

- **Objective:** decide whether two operations should overlap, and write the version that
  actually does.
- **Core:** suspending calls in a coroutine body run one after another; overlap is something
  you ask for; `async` starts work immediately and `await` waits for its result; starting
  both `Deferred`s and awaiting afterwards is what overlaps them.
- **Practical:** `async { a() }.await()` followed by `async { b() }.await()`, which looks
  parallel and is not; the corrected version and the drop from the sum of both durations to
  roughly the slower one; `awaitAll`; dependent calls that cannot overlap however they are
  written.
- **Senior:** concurrency is a structure — several things in progress — and parallelism is
  an execution property that needs more than one thread. Two coroutines on a single-threaded
  dispatcher are concurrent and not parallel, and that is often exactly enough, because most
  mobile "slowness" is waiting rather than computing. Concurrency also costs: more in-flight
  requests, more partial-failure states, and a harder-to-read function.
- **Primary:** `coroutine_parallelism`
- **Supporting:** `coroutine_builders`, `structured_concurrency`, `coroutine_dispatchers`
- **Notes:** `async` failure semantics are named — a failing `async` child still cancels its
  parent whether or not anyone awaits it — and **owned** by L3.3. `select` and racing
  builders are **Exclude**: real, rare, and a distraction from the decision this Lesson
  teaches.

---

## Unit 3 — Cancellation, Failure and Coordination

**Purpose:** answer what stops a coroutine, what happens to the rest of the tree when one
fails, and what happens to state two coroutines touch at once.
**Prerequisites:** Units 1 and 2.

#### L3.1 — Cancellation Is Cooperative

- **Objective:** predict whether a given coroutine will actually stop when it is cancelled.
- **Core:** cancelling sets the `Job` to cancelling and relies on the coroutine to notice;
  suspending functions in `kotlinx.coroutines` check for cancellation and resume by throwing
  `CancellationException`; code that never suspends never notices; `cancel()` returns
  immediately, which is why `cancelAndJoin` exists.
- **Practical:** a CPU-bound loop that ignores cancellation, and the three ways to make it
  cooperative — `isActive`, `ensureActive()`, `yield()`; cancellation propagating down a tree
  of children; a broad `catch (e: Exception)` swallowing `CancellationException` and letting
  cancelled work continue.
- **Senior:** why cancellation is not preemptive at all — a preempted coroutine could be
  stopped mid-mutation, so cooperative cancellation is what makes `finally` and invariants
  meaningful. The cost is that "cancelled" means "asked to stop", and the gap between the
  request and the stop is real time in which the coroutine is still running.
- **Primary:** `coroutine_cancellation`
- **Supporting:** `coroutine_jobs`, `structured_concurrency`, `kotlin_exceptions`
  (kotlin_language)
- **Notes:** Cleanup, `NonCancellable` and timeouts are L3.2. The distinction between
  cancellation and ordinary failure is stated here in one sentence and **owned** by L3.3.

#### L3.2 — Cleanup, `NonCancellable` and Timeouts

- **Objective:** release resources correctly on cancellation, and express a deadline in
  terms of the cancellation model.
- **Core:** a cancelled coroutine unwinds through `finally`, so cleanup belongs there; a
  cancelled coroutine can no longer suspend, so a suspending call in `finally` needs
  `withContext(NonCancellable)`; `withTimeout` cancels its block and throws
  `TimeoutCancellationException`, `withTimeoutOrNull` returns `null` instead.
- **Practical:** a file handle or socket closed in `finally`; a suspending `close()` that
  silently does nothing because the scope is already cancelled; a timeout applied to work
  that never checks for cancellation, and therefore neither stops the work nor returns to the
  caller on time; `runInterruptible` for a blocking JVM call that only responds to thread
  interruption.
- **Senior:** a timeout is not a separate mechanism — it is cancellation with a clock — which
  is why everything true of cancellation is true of timeouts. The consequence worth being
  able to state is that **`withTimeout` bounds nothing on its own.** It requests cancellation
  at the deadline, and because its body is a child scope it cannot return until that body
  finishes, so a non-cooperative block delays the cancellation *and* the caller equally. Two
  measured cases make it concrete — see
  [the timeout measurement](coroutines-flow-units-1-6-plan.md#what-withtimeout-does-to-non-cooperative-work).
  The caller moves on early only when the awaited work was never a child in the first place,
  which is L1.4's ownership argument arriving from a different direction.
- **Primary:** `coroutine_cancellation`
- **Supporting:** `kotlin_exceptions` (kotlin_language), `coroutine_context_switching`,
  `coroutine_jobs`
- **Notes:** `runInterruptible` and thread interruption are **JVM-only**; the Lesson must say
  so rather than presenting them as universal, because this is a Kotlin Multiplatform
  repository. `NonCancellable` must be presented with its danger attached — it is an escape
  hatch for short cleanup, not a way to finish work that was cancelled on purpose.

#### L3.3 — How a Coroutine Failure Travels

- **Objective:** given a failing coroutine, name everything else that fails, and where the
  exception can be observed.
- **Core:** builders differ — `launch` propagates an uncaught exception up the tree, `async`
  stores it in its `Deferred` and rethrows it at `await`; a failing child cancels its parent,
  which cancels its remaining children; `CancellationException` is the exception the model
  uses for cancellation and is ignored by handlers; a `CoroutineExceptionHandler` is a
  last-resort handler for an uncaught exception that has reached a **root** coroutine.
- **Practical:** `try`/`catch` around `await` as the ordinary way to handle an `async`
  failure; an `async` whose `Deferred` is dropped, where the failure still tears the scope
  down; a `CoroutineExceptionHandler` installed on a child, where it does nothing; catching
  a specific exception inside `launch` as the normal case.
- **Senior:** the reason the handler cannot be installed anywhere is that children delegate
  handling to their parent all the way to the root, so a handler below the root is asking to
  handle something that will never reach it. And the reason `async` is excluded entirely is
  that its exception is a value in the `Deferred`, which the handler has no claim on.
- **Primary:** `coroutine_exceptions`
- **Supporting:** `coroutine_builders`, `coroutine_jobs`, `coroutine_cancellation`,
  `error_modeling` (architecture)
- **Notes:** **Bridge** to `architecture` for modelling expected failure as a result type
  rather than an exception — one pointer, not a section. Supervision is the exception to
  every propagation rule stated here and is **owned** by L3.4. Uncaught-exception behaviour
  on each platform host is **Exclude**.

#### L3.4 — `SupervisorJob`, `supervisorScope` and the Limits of Isolation

- **Objective:** isolate independent failures without pretending the failures did not happen.
- **Core:** supervision reverses one arrow — a child's failure does not cancel the parent or
  its siblings, while cancellation still travels downward; `supervisorScope { }` supervises
  the coroutines launched directly in it; a scope built with `SupervisorJob()` supervises the
  coroutines launched from that scope.
- **Practical:** three independent refreshes where one failing must not cancel the other two;
  each supervised child needing its own error handling, because nothing above will do it;
  the two traps — `launch(SupervisorJob())`, which re-parents one coroutine out of the scope
  rather than supervising anything, and a nested `launch` inside a `supervisorScope`, whose
  failure cancels its own parent because supervision only reaches direct children.
- **Senior:** supervision is a statement that these children are independent; if they are
  not, isolating them turns a clean failure into a screen showing half-correct data. The
  question to ask before reaching for it is not "how do I stop this cancelling everything"
  but "is this work genuinely independent".
- **Primary:** `coroutine_supervision`
- **Supporting:** `coroutine_exceptions`, `coroutine_context`, `structured_concurrency`,
  `coroutine_jobs`
- **Notes:** Completes the `launch(SupervisorJob())` trap begun in L2.1. Must not present
  supervision as a way to make errors disappear — the Lesson's failure mode is a reader who
  adds `SupervisorJob` to silence a crash.

#### L3.5 — Shared Mutable State and Choosing a Coordination Mechanism

- **Objective:** identify state that two coroutines can touch at once, and choose a
  mechanism for it deliberately.
- **Core:** coroutines on a multi-threaded dispatcher have every ordinary parallelism
  problem; a lost update is the simplest demonstration; `@Volatile` fixes visibility and not
  atomicity; the three real answers are an atomic or thread-safe structure, confining the
  state to a single context, and mutual exclusion with `Mutex`.
- **Practical:** a counter incremented from many coroutines producing the wrong total; the
  same code with an atomic; `Mutex.withLock` and why `Mutex.lock()` suspends rather than
  blocking, which is what makes it usable inside a coroutine; fine-grained confinement being
  slow and coarse-grained confinement being fast; not sharing at all — sending messages
  through a `Channel` — as the fourth option.
- **Senior:** the choice is a design question before it is a performance question. An atomic
  fits one independent variable; confinement fits a body of related state with one owner;
  a `Mutex` fits a critical section that must stay consistent across several suspending
  steps. The failure mode is reaching for a lock because the code looked racy, and thereby
  serialising a hot path nobody measured.
- **Primary:** `coroutine_parallelism`
- **Supporting:** `hot_vs_cold_streams`, `jvm_fundamentals` (kotlin_language),
  `android_memory_model` (performance), `coroutine_dispatchers`
- **Notes:** This is the Unit's **bounded** treatment of coordination and it must stay a
  decision Lesson, not a primitive catalogue. `Semaphore`, `actor`, and the full `Channel`
  API are **Reference** at most; the `Channel` comparison here is one paragraph and the real
  contrast lives in L6.5. Java memory-model detail beyond "visibility and atomicity are
  different problems" is **Exclude**. Standard-library `AtomicInt`/`AtomicReference`
  availability differs by target, so examples must not assume `java.util.concurrent.atomic`
  in common code.

---

## Unit 4 — Flow Fundamentals

**Purpose:** establish the cold-stream model completely — who produces, who collects, when
anything runs, and where it runs — before a single operator or hot abstraction appears.
**Prerequisites:** Units 1–3.

#### L4.1 — One Value or Many: Why `Flow` Exists

- **Objective:** decide whether an API should return a value once or produce values over
  time.
- **Core:** a suspending function returns one result and is finished; a `Flow` represents
  values that arrive over time and is not finished until it says so; the difference is in the
  problem, not in the technology.
- **Practical:** the same repository expressed both ways — `suspend fun getUser(): User`
  against `fun observeUser(): Flow<User>` — and what each forces the caller to do; a screen
  that must reflect later local writes, which a one-shot call cannot do; a Flow used where a
  single value was wanted, which buys nothing and costs a collector.
- **Senior:** the reason this is the first Lesson of the Unit is that the wrong answer here
  is invisible for months — a one-shot API that should have been observable produces a UI
  that is correct on arrival and stale afterwards, and every later fix is a refresh
  mechanism reimplementing what a stream would have given free.
- **Primary:** `flow_fundamentals`
- **Supporting:** `coroutine_fundamentals`, `hot_vs_cold_streams`,
  `repository_pattern` (architecture), `single_source_of_truth` (architecture)
- **Notes:** **Bridge** to `architecture` for repository API shape. `LiveData`, RxJava and
  callbacks are named in one sentence as the alternatives Flow replaced and are otherwise
  **Exclude** — see [Excluded material](#excluded-material). Hot streams are named as
  something that exists and deferred whole to Unit 6.

#### L4.2 — Cold Flows: Producer, Collector and Operators

- **Objective:** say exactly what runs, and when, for a given flow chain.
- **Core:** a cold flow's builder block does not run when the flow is created; it runs when a
  collector collects, and each new collector starts an independent execution; `emit` hands a
  value downstream; `collect` is the terminal operator that starts everything; intermediate
  operators return a new cold flow and start nothing; production and collection are
  sequential by default.
- **Practical:** a `flow { }` with logging in it, showing that nothing prints until
  `collect`; collecting the same flow twice and getting two independent runs; a chain of
  `map` and `filter` with no terminal operator, which is inert; `first()`, `toList()` and
  `single()` as terminal operators that are not `collect`.
- **Senior:** the analogy that carries most weight is `Sequence` — same laziness, same
  per-consumer re-execution, with suspension added — and the place it breaks is that a
  `Flow` collector is a coroutine with a lifetime, which a `Sequence` consumer is not.
- **Primary:** `flow_fundamentals`
- **Supporting:** `flow_collection`, `flow_operators`, `kotlin_sequences` (kotlin_language)
- **Notes:** Operators appear only as "intermediate versus terminal"; the operator families
  are Unit 5. `flowOn` is deferred to L4.4. This Lesson is the canonical treatment of
  coldness, which the shipped Compose Lesson `lesson_snapshot_flow` currently bridges in
  four bullet points — see the plan's `snapshotFlow` section.

#### L4.3 — Collection Lifetime and Flow Cancellation

- **Objective:** name the coroutine that a given collection runs in, and what will end it.
- **Core:** a cold flow has no coroutine of its own; `collect` runs the producer inside the
  collecting coroutine; collection ends when the flow completes or the collecting coroutine
  is cancelled; cancelling the collector cancels the producer, cooperatively, at its next
  suspension point.
- **Practical:** `scope.launch { flow.collect { } }` and its shorthand
  `flow.onEach { }.launchIn(scope)`, which returns a `Job` immediately; a flow that never
  completes, and why the collecting coroutine therefore never finishes on its own; a paged
  database read cancelled part-way, and what happens to the in-flight page.
- **Senior:** "who cancels this collection" is the same question as "who owns this scope",
  which is why Unit 1 came first. The observable consequence is that a collection whose scope
  nobody owns is a leak that also keeps its upstream resource open.
- **Primary:** `flow_collection`
- **Supporting:** `coroutine_cancellation`, `coroutine_scope`, `lifecycle_coroutines`,
  `coroutine_jobs`
- **Notes:** Lifecycle-aware collection on Android (`repeatOnLifecycle`, `flowWithLifecycle`)
  and Compose collection (`collectAsStateWithLifecycle`) are **Bridge** at one sentence each
  and are owned by E25 and the Compose blueprint's Unit 8. `cancellable()` is **Reference**.

#### L4.4 — Context Preservation and `flowOn`

- **Objective:** say which part of a chain runs in which context, and recognise the
  violation that the runtime rejects.
- **Core:** a flow runs in the collector's context by default; the emitter may not change
  that context itself, and doing so fails with an explicit invariant violation; `flowOn`
  changes the context of everything **upstream** of it and leaves everything downstream in
  the collector's context.
- **Practical:** a `flow { }` that wraps its body in `withContext(Dispatchers.IO)` and throws;
  the same intent expressed correctly with `.flowOn(Dispatchers.IO)`; where in a chain
  `flowOn` must be placed for it to affect what you meant; the fact that two `flowOn` calls
  in one chain each apply to their own upstream segment.
- **Senior:** context preservation is the rule that lets a collector reason about its own
  context without reading the whole chain — a `collect` block on `Main` stays on `Main` no
  matter what any upstream operator did. Exception transparency is the same principle applied
  to failures, and is why an operator may not swallow a downstream exception; the operators
  that implement it are L5.5.
- **Primary:** `flow_context`
- **Supporting:** `coroutine_context`, `coroutine_dispatchers`, `coroutine_context_switching`,
  `flow_errors`
- **Notes:** `channelFlow` and `callbackFlow` are named here as the sanctioned way to emit
  from a different coroutine and are **owned** by L4.5. `buffer`'s interaction with `flowOn`
  is named and deferred to L5.4.

#### L4.5 — Flow Builders and Adapting Callback APIs

- **Objective:** turn an existing producer — a value, a collection, a listener — into a Flow
  without leaking it.
- **Core:** `flowOf` and `asFlow` for values already in hand; `flow { }` for a sequential
  producer; `channelFlow { }` when values come from several coroutines; `callbackFlow { }`
  for a listener-based API, which must end in `awaitClose { }`.
- **Practical:** adapting a location or connectivity listener; `awaitClose` as the one place
  the listener is unregistered, and the `IllegalStateException` the builder throws if the
  block returns without it; why the alternative to that exception is a silent leak with the
  flow ended and the callback still registered; the builder's `capacity` as where buffering
  is configured.
- **Senior:** `callbackFlow` is where a hot external source is made to look cold, and the
  seam is worth naming: the underlying listener is shared and event-based, while each
  collector gets its own registration. Anything the source emitted before this collector
  subscribed is gone, which is a property of the source and not something the builder can fix.
- **Primary:** `flow_fundamentals`
- **Supporting:** `flow_context`, `coroutine_cancellation`, `flow_buffering`,
  `memory_leaks` (performance)
- **Notes:** The full `Channel` API behind `channelFlow` is **Reference**; this Lesson needs
  only "there is a channel, and it is why emission from several coroutines is legal here".
  `produceIn` and channel-based operators are **Exclude**.

---

## Unit 5 — Flow Composition, Timing and Failure

**Purpose:** teach operator selection as a set of semantic decisions. Not an operator
encyclopedia — each Lesson is a question a reader will actually be asked in a review.
**Prerequisites:** Unit 4.

#### L5.1 — Transforming and Filtering: by Value and by Time

- **Objective:** choose between filtering on what a value is and filtering on when it
  arrived.
- **Core:** `map`, `filter` and `transform` build a new cold chain and run nothing;
  `transform` is the general form that may emit zero or many values per input;
  `distinctUntilChanged` drops a value equal to the one immediately before it; `debounce`
  waits for a quiet period; `sample` takes the latest value per fixed window.
- **Practical:** a search field where `debounce` collapses a burst of keystrokes and
  `distinctUntilChanged` removes a repeat of the same query — different jobs, frequently
  wanted together; `distinctUntilChanged` comparing only with the previous value, so a value
  that reappears later passes; `map` with a suspending transform, which is allowed and is
  part of why Flow exists.
- **Primary:** `flow_operators`
- **Supporting:** `flow_fundamentals`, `kotlin_lambdas` (kotlin_language),
  `kotlin_equality` (kotlin_language)
- **Notes:** The operator list stops here deliberately. `take`, `drop`, `onStart`, `onEach`
  and the scan/fold family are **Reference** — named in a short table, not taught. Any
  operator whose behaviour a reader can predict from its name does not earn a section.

#### L5.2 — `combine` and `zip`: Current Values or Paired Emissions

- **Objective:** decide whether a downstream value should be built from the latest of each
  source, or from matched pairs.
- **Core:** `combine` waits until every source has emitted once, then re-emits whenever any
  source emits, using each source's most recent value; `zip` is strictly pairwise and holds a
  value until the other source has an unused one; `merge` interleaves without combining.
- **Practical:** screen state built from a user flow and a settings flow, where a settings
  change alone must produce a fresh state — `combine`; a rarely emitting source throttling the
  whole pair under `zip`, which is almost always the wrong shape for UI; the startup rule
  producing "nothing appears" when one source never emits.
- **Senior:** `combine` is a statement about *state* — the latest of everything — and `zip` is
  a statement about *correspondence* — this value goes with that one. Naming which of the two
  the problem is decides the operator before any API is consulted.
- **Primary:** `flow_operators`
- **Supporting:** `flow_fundamentals`, `stateflow`, `flow_collection`
- **Notes:** `combineTransform` and the arity-4+ overloads are **Reference**.
  `combine`'s behaviour when a source completes is worth one sentence and not a section.

#### L5.3 — Flattening: Should New Input Cancel Old Work?

- **Objective:** choose a flattening strategy from what should happen to work already in
  flight.
- **Core:** each upstream value maps to an inner flow, and the strategies differ only in how
  those inner flows relate — `flatMapConcat` waits for the previous one to finish,
  `flatMapMerge` runs several concurrently and interleaves, `flatMapLatest` cancels the
  previous one when a new value arrives.
- **Practical:** search-as-you-type, where the request for the abandoned query must stop —
  `flatMapLatest`; a queue of uploads that must preserve order — `flatMapConcat`; independent
  fan-out where interleaving is fine — `flatMapMerge` and its `concurrency` bound; what
  cancelling an inner flow means for a side effect that already started.
- **Senior:** the three answer three different questions — ordering, overlap, and whether
  stale work is worth finishing — and the reason `flatMapLatest` is the common Android answer
  is that most UI-driven work is stale the moment newer input arrives. All three carry
  `@ExperimentalCoroutinesApi` in the configured version, which is an accuracy point the
  Lesson must state rather than a reason to avoid them.
- **Primary:** `flow_operators`
- **Supporting:** `coroutine_cancellation`, `coroutine_parallelism`, `flow_collection`
- **Notes:** `flattenConcat` / `flattenMerge` are **Reference** — the `flatMap` forms are the
  ones written in practice. `transformLatest` and `mapLatest` are named as the same
  cancellation rule applied to a simpler shape.

#### L5.4 — When the Collector Cannot Keep Up

- **Objective:** decide which values may be lost, and which work may be abandoned, when the
  producer is faster than the collector.
- **Core:** by default the collector applies backpressure — the producer suspends until the
  collector is ready; `buffer` decouples them up to a capacity; `conflate` keeps only the
  latest unconsumed value; `collectLatest` starts the block for every value and cancels it
  when a newer one arrives.
- **Practical:** the same fast producer and slow collector under all four behaviours, with
  what each one loses; `conflate` letting the current block finish while `collectLatest`
  abandons it, and why that distinction decides whether a partially processed value is safe;
  `buffer` trading memory and latency for throughput; `buffer`'s `onBufferOverflow` and its
  relationship to `conflate`.
- **Senior:** the question underneath all four is whether a value has independent meaning.
  If every value must be processed, buffering is the only honest option and an unbounded
  buffer is a memory leak waiting for a slow day. If only the latest matters, conflation is
  not a compromise — it is the correct model, and the intermediate values were never
  information.
- **Primary:** `flow_buffering`
- **Supporting:** `flow_collection`, `flow_context`, `coroutine_cancellation`,
  `flow_operators`
- **Notes:** `buffer` and `flowOn` interact — `flowOn` introduces a buffer of its own — and
  that is worth one accurate sentence, not a section. `BufferOverflow` modes are
  **Reference** beyond the two that `conflate` names.

#### L5.5 — `catch`, `retry` and `onCompletion`

- **Objective:** place failure handling where it can actually see the failure.
- **Core:** exception transparency means an operator may only handle exceptions from
  **upstream** of itself; `catch` therefore sees failures declared above it and never the
  collector's own; `catch` does not catch cancellation; `retry` and `retryWhen` resubscribe
  to the upstream; `onCompletion` runs on success, failure and cancellation alike.
- **Practical:** `.catch { }.collect { }` where the `collect` block throws and `catch` does
  not fire, and the two fixes — a `try`/`catch` inside `collect`, or moving the work into an
  operator above `catch`; `catch` emitting a fallback value; `retryWhen` bounded by attempt
  count and restricted to a recoverable cause; `onCompletion` receiving the cause, including
  a `CancellationException`, and why that makes it a poor place for a "success" side effect.
- **Senior:** the restriction is deliberate rather than an oversight — an upstream operator
  that could swallow a downstream failure would make a flow chain unreadable, because a
  failure's handler would depend on code declared after it. Which also explains why a
  `try`/`catch` wrapped around the whole chain is a different tool: it catches everything,
  including what `catch` deliberately refuses.
- **Primary:** `flow_errors`
- **Supporting:** `kotlin_exceptions` (kotlin_language), `coroutine_cancellation`,
  `flow_operators`, `error_modeling` (architecture)
- **Notes:** **Bridge** to `architecture` for representing an expected failure as a value in
  the stream rather than an exception — one pointer. `retry` with exponential backoff is a
  worked example, not a section on backoff policy.

---

## Unit 6 — StateFlow, SharedFlow and Hot Streams

**Purpose:** complete the model with streams that exist independently of their collectors,
and end at a defensible decision among all the abstractions the subject offers.
**Prerequisites:** Units 4 and 5. Unit 1's ownership model is assumed throughout.

#### L6.1 — Hot and Cold: When Production Happens

- **Objective:** classify a stream by when it produces and by what a late subscriber gets.
- **Core:** a cold stream produces per collector, on collection, from the start; a hot stream
  produces independently of any collector, so a subscriber joins a stream already in progress
  and receives what arrives while it is subscribed, plus whatever the stream was configured to
  retain for it. "Hot" is about **where production lives**, not about speed, eagerness,
  retention, or whether a producer is currently running.
- **Practical:** the same source expressed both ways and what a second, later collector
  receives from each; a value emitted while nobody is subscribed, which a cold flow cannot
  produce at all and which a hot flow delivers to nobody — though whether a *later*
  subscriber still sees it is decided by retention, not by hotness.
- **Senior:** the shipped Compose Lesson on `snapshotFlow` already carries the sharpest form
  of this argument: observable state is a lossy compression of the events that produced it,
  so an observer of state may arrive late and still be correct, while an observer of events
  may not. That framing is what makes the rest of this Unit a decision rather than a table.
- **Primary:** `hot_vs_cold_streams`
- **Supporting:** `flow_fundamentals`, `flow_collection`, `stateflow`, `sharedflow`
- **Notes:** `StateFlow` and `SharedFlow` are named as the two implementations and taught in
  L6.2 and L6.3. Links backwards to `lesson_cold_flows` and to the shipped
  `lesson_snapshot_flow`. **This Lesson's main authoring risk is folding three axes into
  one.** Hotness is where production lives; *retention* is a separate axis, and so is
  *start/stop policy*. `StateFlow` always retains its latest value and `SharedFlow` retains
  its configured `replay`, so "a hot flow discards what it emitted with no subscribers" holds
  only for `replay = 0`; and a stream produced by `shareIn(scope, WhileSubscribed())` stops
  its upstream when the last subscriber leaves, so "a hot producer keeps its upstream open
  regardless" is false as a general claim. Name the three axes, attribute retention to L6.2
  and L6.3 and start/stop policy to L6.4, and let this Lesson own only the production-lifetime
  distinction. The over-general version would be walked back by the next three Lessons in a
  row, which is exactly the inconsistent-depth failure Rule 1 of the authoring contract
  exists to prevent.

#### L6.2 — `StateFlow`: One Current Value

- **Objective:** predict exactly which values a `StateFlow` collector observes.
- **Core:** a `StateFlow` always has a value, readable without collecting; it replays that
  one value to every new subscriber and buffers nothing else; it never completes; updates are
  conflated, so a slow collector skips intermediate values but always ends on the latest.
- **Practical:** `MutableStateFlow` held privately and exposed as `StateFlow`; assigning a
  value equal to the current one, which emits nothing at all because conflation compares with
  `Any.equals`; a `data class` state whose `equals` is not what the author assumed, and the
  update that therefore disappears; `update { }` for a read-modify-write that two coroutines
  might race.
- **Senior:** equality-based conflation is the property that makes `StateFlow` correct for UI
  state and wrong for anything countable — three identical "refresh finished" updates are one
  update. It is also the direct reason a state type must have meaningful equality, which is
  the same constraint Compose skipping imposes for a different reason.
- **Primary:** `stateflow`
- **Supporting:** `hot_vs_cold_streams`, `flow_collection`, `kotlin_equality`
  (kotlin_language), `state_ownership` (architecture)
- **Notes:** **Bridge** to `architecture` for state ownership; **Bridge** to `android_ui` and
  E25 for how a UI collects it. Screen-state modelling, `UiState` hierarchies and ViewModel
  design are **Exclude** here — the Compose blueprint's Unit 8 and the architecture
  curriculum own them. `LiveData` comparison is **Reference**, one row in a table.

#### L6.3 — `SharedFlow`: Replay, Buffering and Subscribers

- **Objective:** state what a `SharedFlow` guarantees a subscriber will receive, and
  configure it deliberately.
- **Core:** a `SharedFlow` broadcasts each emission to all current subscribers; a new
  subscriber first receives the replay cache and then live values; `replay`,
  `extraBufferCapacity` and `onBufferOverflow` are the three knobs; it never completes.
- **Practical:** `replay = 0` and a subscriber that arrives one second late, receiving
  nothing; `emit` on an unbuffered shared flow suspending until every subscriber has taken
  the value, and returning immediately when there are none; `tryEmit` and what its `false`
  return means; the emission made while the screen was in the background and therefore lost.
- **Senior:** "SharedFlow is for events" is only useful with the delivery semantics attached.
  A shared flow with no subscribers drops values silently; with `onBufferOverflow` set to
  drop, it drops them under load; with suspension, a slow subscriber backpressures the
  emitter. Each is a defensible choice and none of them is "each event is delivered exactly
  once to whoever needs it", which is what people usually mean.
- **Primary:** `sharedflow`
- **Supporting:** `hot_vs_cold_streams`, `flow_buffering`, `stateflow`, `flow_collection`
- **Notes:** `subscriptionCount` is **Reference**. `MutableSharedFlow`'s
  `resetReplayCache` is **Reference**. The comparison against a `Channel` for one-time
  delivery is deliberately held back to L6.5, where all the options are on the table.

#### L6.4 — `stateIn`, `shareIn` and `SharingStarted`

- **Objective:** convert a cold upstream into a shared one and say what starts and stops it.
- **Core:** `stateIn` and `shareIn` run the upstream once in a given scope and share its
  values; `stateIn` requires an initial value and produces a `StateFlow`, `shareIn` produces a
  `SharedFlow` with the replay you choose; the `SharingStarted` argument, not the collectors,
  decides when the upstream runs.
- **Practical:** three policies as three tradeoffs — `Eagerly` starts immediately and never
  stops; `Lazily` starts at the first subscriber and never stops; `WhileSubscribed` starts at
  the first subscriber and stops when the last one leaves, after `stopTimeoutMillis`;
  the timeout sized so a configuration change does not restart a database query while a real
  departure does stop it; `replayExpirationMillis`, which defaults to never resetting the
  cache, and what a stale replayed value looks like on a screen re-entered much later; the
  suspending `stateIn(scope)` overload with no initial value.
- **Senior:** sharing moves the upstream's lifetime from the collector to the scope, and that
  is the whole of both the benefit and the risk. The benefit is one database query instead of
  four. The risk is that the collection site no longer controls whether the work is running,
  so a policy chosen by habit — usually `Eagerly` — becomes work the app performs for a screen
  nobody opened.
- **Primary:** `flow_sharing`
- **Supporting:** `stateflow`, `sharedflow`, `coroutine_scope`, `lifecycle_coroutines`
- **Notes:** The 5-second `WhileSubscribed` figure must be explained as a configuration-change
  heuristic on Android, not stated as a rule. This repository's own state holders use
  `SharingStarted.Eagerly`, which is a legitimate choice for an app-lifetime holder and a
  useful contrast case. `SharingStarted` as a custom implementable interface is **Reference**.

#### L6.5 — Choosing Between a Value, a Flow, a State Holder and a Channel

- **Objective:** pick the abstraction from the delivery guarantee the problem needs.
- **Core:** a plain value when nothing observes changes; a cold `Flow` when each consumer
  should drive its own production; a `StateFlow` when there is a current value every observer
  needs, including late ones; a `SharedFlow` when occurrences are broadcast to whoever is
  listening; a `Channel` when each element must go to exactly one receiver.
- **Practical:** a worked set of scenarios decided out loud — screen state, a database query
  observed by two screens, a "sync finished" notice, a queue of outgoing commands, a
  navigation instruction that must happen once; the fan-out property that separates a
  `Channel` from a `SharedFlow`, and the consumption property that separates it from a
  `StateFlow`.
- **Senior:** the honest version of the state-versus-events distinction. State can be
  observed late and conflated because only the latest is information; events cannot, which
  makes "deliver this exactly once" a requirement no hot Flow satisfies by itself. The
  practical consequences are the ones worth being able to state: a `Channel` gives
  single-consumer delivery and no replay; a `SharedFlow` gives broadcast and configurable
  replay but no delivery guarantee to an absent subscriber; and modelling the occurrence as
  acknowledged state is the third option, which trades simplicity for a guarantee.
- **Primary:** `hot_vs_cold_streams`
- **Supporting:** `stateflow`, `sharedflow`, `flow_sharing`, `state_ownership` (architecture)
- **Notes:** This is the epic's **bounded** `Channel` treatment, together with one paragraph
  in L3.5. It is justified because the decision above is incomplete without it and because
  the bank already assesses exactly this contrast. A full Channel Unit — pipelines, fan-in,
  fan-out, ticker channels, `produce` — remains **Exclude**; see
  [Excluded material](#excluded-material). One-off UI events in a Compose screen are E25's
  and the architecture curriculum's decision; this Lesson supplies the stream semantics and
  stops.

---

## Reference Material

Concise treatment is worthwhile, but none of this belongs on the main learning path and
none of it is a prerequisite for anything above.

| Area | Why Reference rather than Teach | Where it sits |
| --- | --- | --- |
| `CoroutineStart` modes, including `LAZY` | Real, occasionally useful, almost never the subject of a decision that matters | One line in L1.2 |
| The full `Job` state-transition table | The five states and why "completing" exists carry the reasoning; the table is lookup | Bounded in L1.3 |
| `Dispatchers.Unconfined` | Advanced tool, frequently mistaken for an optimisation | Named in L2.2 |
| Custom dispatchers, `asCoroutineDispatcher` | Rare in product code; the injection argument already covers the need | Named in L2.2 |
| `Semaphore`, `actor` | Genuine coordination tools, but the three in L3.5 cover the decision | Named in L3.5 |
| `Channel` API surface — pipelines, fan-in/fan-out, ticker channels, `produce` | A subject of its own; E24 needs the delivery contrast, not the toolkit | Bounded in L3.5 and L6.5 |
| `cancellable()` | One operator answering one narrow question | Named in L4.3 |
| `take`, `drop`, `onStart`, `scan`, `fold` | Predictable from the name; teaching them is an encyclopedia | Short table in L5.1 |
| `flattenConcat` / `flattenMerge` | The `flatMap` forms are what is written | Named in L5.3 |
| `BufferOverflow` modes beyond conflation | Configuration detail behind a decision already taught | Named in L5.4 |
| `subscriptionCount`, `resetReplayCache` | Narrow APIs; useful when needed, no reasoning objective | Named in L6.3 |
| Custom `SharingStarted` implementations | Framework-author territory | Named in L6.4 |
| `LiveData` comparison | Historical; matters only when reading an older codebase | One table row in L6.2 |

## Excluded Material

Accurate, but it does not improve interview readiness and would add noise. Each exclusion is
revisitable if the target job profile changes — by editing this blueprint, not by quietly
adding a Lesson.

| Area | Why excluded |
| --- | --- |
| Continuation-passing style and the compiler's state machine | Implementation of the `suspend` transform; no product decision depends on the generated shape, and naming it invites trivia |
| `suspendCoroutine` / `suspendCancellableCoroutine` internals | Library-author territory; L4.5 covers the one adaptation case product code writes |
| Coroutine scheduler and thread-pool implementation | The observable contracts in L2.2 are the useful part |
| RxJava, and Flow-to-Rx migration | Explicitly out of scope for E24; `rxjava_fundamentals` and `flow_vs_rxjava` hold no active questions and remain unmapped |
| `LiveData` as a subject | Superseded for new code; the only interview-relevant part is the `StateFlow` contrast, kept as Reference |
| Exhaustive Flow operator catalogue | An API list, not a path through the subject; Unit 5 is organised by decision instead |
| Full Channel curriculum | Would unbalance Units 3 and 6 for an abstraction most product code meets only as a comparison |
| `runTest`, `TestDispatcher`, virtual time, Turbine | Owned by the coroutine-testing curriculum (E31); teaching it here would date twice |
| Compose effect APIs and lifecycle-aware collection | Owned by E25 and the Compose blueprint's Unit 8 |
| MVVM/MVI, ViewModel and repository architecture | Owned by the architecture curriculum; E24 uses them as bounded examples only |
| Uncaught-exception host behaviour per platform | Platform trivia; the propagation model is what transfers |
| Java memory-model detail beyond visibility versus atomicity | The distinction is what decides a mechanism; the model itself is `jvm_fundamentals`' subject |

## Taxonomy Gaps: Concepts With No Exact Assessment Subtopic

Recorded rather than given invented IDs. **E24-01 does not change the question taxonomy**;
whether any of these should become a Subtopic is a separate decision for a future
question-bank change.

| Concept | Nearest existing Subtopic | Decision |
| --- | --- | --- |
| Shared mutable state, races, `Mutex`, atomics, confinement | `coroutine_parallelism` ("Concurrency and async/await") | The broader Subtopic is semantically correct — this is concurrency — so L3.5 maps to it at a different depth from L2.4. Recorded because the Subtopic's name reads narrower than its use here. |
| `Channel` as a delivery abstraction | `hot_vs_cold_streams` | Semantically correct for the bounded comparison E24 teaches, and already the bank's own choice: `flow_vs_channel_delivery_model` is mapped there today. No Channel Subtopic exists in any Topic. |
| Timeouts (`withTimeout`, `withTimeoutOrNull`) | `coroutine_cancellation` | Correct rather than a stretch: the authoritative page is itself titled "Cancellation and timeouts", and L3.2 teaches timeouts as cancellation with a clock. |
| Flow builders (`callbackFlow`, `channelFlow`) | `flow_fundamentals` | Correct and already the bank's choice for `callback_flow_await_close_registration`. |
| Main-safety as a contract | `coroutine_context_switching` | The Subtopic is named for `withContext`, which is the mechanism; the contract has no Subtopic of its own. Supporting prose in L2.3, no invented ID. |
| Coroutine internals — continuations, the scheduler | none | Excluded from the path, so no gap needs filling. |

Separately, three Subtopics inside the home Topic are **not mapped by any Lesson** in this
blueprint, which is a deliberate scope decision rather than an oversight:

| Subtopic | Active questions | Why unmapped |
| --- | --- | --- |
| `rxjava_fundamentals` | 0 | RxJava is out of scope for E24 |
| `flow_vs_rxjava` | 0 | Migration content is out of scope for E24 |
| `livedata` | 1 | Kept as Reference in L6.2; its one active question is a `StateFlow` comparison and stays supporting-only |

And one Subtopic is mapped **supporting-only** across the whole epic:

| Subtopic | Active questions | Consequence |
| --- | --- | --- |
| `lifecycle_coroutines` | 1 | Lifecycle-aware scope integration is E25's and the architecture curriculum's subject. Its active question therefore creates no Unit practice for any E24 Unit, which is intended. |

## Authoritative Source Families

Identified so that a Lesson author does not start from a blank search. Individual Lessons
still cite the specific page supporting each claim, per Rule 9. The current state of these
sources — including a restructuring that moved several subjects — is recorded in the plan's
[source-freshness section](coroutines-flow-units-1-6-plan.md#source-freshness-and-technical-assumptions).

- **Kotlin coroutines documentation** (`kotlinlang.org/docs/coroutines-*`) for suspension,
  builders, structured concurrency, cancellation, context and dispatchers, exception
  handling, shared mutable state, channels, and the cold/hot Flow model.
- **`kotlinx.coroutines` API reference** (`kotlinlang.org/api/kotlinx.coroutines/`) for exact
  contracts, defaults, experimental annotations, and every operator the guide pages no longer
  cover. This is the authority wherever the guide and the KDoc differ in detail.
- **Android Developers — Kotlin coroutines and Flow** for Android-specific application
  guidance: dispatcher injection, main-safety, lifecycle-aware collection, and
  `StateFlow`/`SharedFlow` in an app architecture.
- **kotlinx.coroutines release notes and the project repository** for behaviour that changed
  between versions, which is where secondary sources date fastest.
- **Kotlin language documentation** for the bridged Kotlin concepts — lambdas, exceptions,
  equality, collections and sequences.

Coroutine and Flow **semantics** are comparatively stable; what dates is Android application
guidance, experimental API status, and dispatcher implementation detail. Re-check those on
any material edit.

## Status

This blueprint is complete as a map. No Unit is authored yet: E24-02 through E24-07 author
Units 1 to 6 in order, against the confirmed identities and boundaries in
[`coroutines-flow-units-1-6-plan.md`](coroutines-flow-units-1-6-plan.md). When authoring
reveals a wrong Lesson boundary, update this file in the same change.

The subject already has one published bridge into it: `lesson_snapshot_flow` in
`unit_snapshot_fundamentals`, which teaches four facts about cold Flow so that its own
material stands alone, and says in prose that the coroutines and Flow curriculum does not
exist yet. Reconciling that Lesson with Unit 4 — including correcting that sentence — belongs
to E24-05 and is specified in the plan.
