# Dependency Injection Learning Blueprint

## Purpose

This is the complete learning map for dependency injection and object graphs, produced
under `docs/content/learning-content-authoring.md` before any production Lesson for the
subject is authored. It is the fourth Topic mapped under that contract, after
`docs/content/compose-learning-blueprint.md`,
`docs/content/coroutines-flow-learning-blueprint.md` and
`docs/content/architecture-learning-blueprint.md`.

It is a **plan, not content**. No Lesson text is authored here, and nothing in this file is
a runtime artifact. Authoring proceeds Unit by Unit against this map, through E27-02 to
E27-07.

Home Topic: `dependency_injection` (Dependency Injection).

Scope: 6 Learning Units, 34 planned Lessons, plus explicit Reference and Exclude decisions,
a misconception matrix, a terminology register, and a record of concepts the current
assessment taxonomy cannot express.

The confirmed authoring plan — stable identities, the taxonomy inventory, prerequisites,
the semantic Question review, the repository and framework findings, and the practice
routing model — is in
[`dependency-injection-units-1-6-plan.md`](dependency-injection-units-1-6-plan.md). This
blueprint holds objectives, depth layers and editorial decisions; that document holds what
the review had to settle.

## The Learner This Subject Is Written For

The reader has worked through the shipped Compose, Coroutines and Flow, and Application
Architecture Units. They can choose an owner for a piece of state from the lifetime a
requirement names, trace which way a source dependency points, say who should define an
abstraction, and judge whether a boundary earns its cost. The architecture curriculum took
them to the point where a component's collaborators are decided — and then stopped, by
design, on the sentence "how the owner is constructed and supplied is the
dependency-injection curriculum's".

What they cannot yet do is answer the question that starts on the other side of that
sentence. They have met dependency injection as a library somebody added to the project
before they arrived: a `@HiltViewModel` here, a `single { }` there, and an instruction to
copy the pattern. The failure mode is not ignorance of annotations — many can recite
`@Provides`, `@Binds` and `@InstallIn`. It is that not one of those annotations is attached
to a question. Asked what injection actually changed about a class, why the object is a
`single` rather than a `factory`, what a scope annotation guarantees, or whether the project
needed a container at all, the answer is the framework's name.

## How to Read This Blueprint

Every planned Lesson records:

- **Objective** — what the learner should be able to do afterwards.
- **Core / Practical / Senior** — the concepts belonging to each depth layer, per Rule 6 of
  the authoring contract. A missing Senior line means deeper material would be artificial.
- **Primary** — Subtopic IDs the Lesson is responsible for teaching thoroughly.
- **Supporting** — Subtopic IDs the Lesson explains only far enough to stay understandable,
  including cross-Topic bridges. Cross-Topic IDs are annotated with their owning Topic.
- **Notes** — Teach/Bridge/Reference/Exclude decisions, prerequisites, and pointers to
  deeper content.

Every Topic and Subtopic ID below is a real ID from the bundled curriculum
(`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json`) and was
checked `ACTIVE` on 2026-09-18. Concepts with no exact Subtopic are recorded in
[Taxonomy gaps](#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic) rather than
given invented IDs. **This blueprint does not change the question taxonomy.**

The `L1.1` style labels are positional reading aids for this document and its companion
plan. They are **not** identities; the stable ids are in the plan's identity tables and are
never numbered by position.

## The Central Question

The subject is not "which dependency-injection framework should I use". A framework is an
answer, and a learner who collects answers without the question cannot defend any of them —
which is why the same engineer can configure Hilt correctly and still not say what it did.
Every Unit below is built so the reader can work through one question about an object in
front of them:

> **Who is responsible for constructing this object, how long must each thing it is given
> live, and where does that decision belong?**

Its parts are the recurring shape of the whole subject:

- **Requirement.** What does this object genuinely need in order to do its work, and is
  that need part of its design or an accident of how it was written?
- **Construction responsibility.** Who creates the collaborator — the object itself, a
  registry it reaches into, or whoever creates the object?
- **Graph.** Once the collaborators have collaborators, what is the shape of the whole
  thing, and who assembles it?
- **Lifetime and reuse.** How long must a particular instance remain the same instance, and
  what ends it?
- **Location.** Which place in the program is allowed to know how everything is built?

Only after all five does a framework appear, and then only as an encoding: manual wiring,
Dagger, Hilt and Koin are four different notations for the same five decisions, with
different costs and different moments at which they tell you that you got it wrong.

A second question runs underneath the first and closes the subject in Unit 6:

> **What is the smallest dependency strategy this project's requirements actually justify?**

## Unit Order and Why It Matters

1. Dependency Injection as Object Construction
2. Object Graphs, Lifetimes and Scopes
3. Dagger: Compile-Time Object Graphs
4. Hilt: Android Lifecycle-Aware Dagger
5. Koin and Dependency Injection in KMP
6. Choosing a Dependency Injection Strategy

The order encodes conceptual dependencies, not convenience, and it is deliberately **not**
the order in which a working Android engineer first meets the material:

- **Construction (1) before graphs (2).** One injected constructor is a design decision the
  reader can hold whole. Introducing the graph first makes injection look like a
  consequence of scale, which is exactly the belief that produces "we are too small for
  dependency injection" — a sentence that confuses injection with a container.
- **Generic graph reasoning (2) before any framework (3–5).** Every framework word the
  later Units introduce — binding, component, scope, qualifier, module, definition — is a
  notation for something Unit 2 has already made the reader decide by hand. Taught in the
  other order, the framework's vocabulary becomes the concept, and the learner cannot
  transfer anything when the framework changes.
- **Dagger (3) before Hilt (4).** Hilt is Dagger with the Android decisions already made.
  A reader who meets Hilt first has no way to tell which of its rules are dependency
  injection and which are Hilt's opinions, which is precisely the confusion that produces
  "Hilt is a different DI system from Dagger".
- **Hilt (4) before Koin (5).** Unit 4 ends with a graph whose lifetimes are decided by a
  platform's own component hierarchy. Unit 5 then removes the platform: a multiplatform
  graph has to answer the same lifetime questions with no Android component tree to inherit
  them from, which is what makes the comparison in Unit 6 real rather than stylistic.
- **Selection (6) last.** The comparison only becomes honest once all four encodings have
  been seen doing the same job. Placed earlier it degrades into a feature table, and a
  feature table is how framework arguments are won by whoever speaks first.

The sequence deliberately does **not** open with an annotation, a DSL keyword or a diagram
of a container. All three are where most readers first meet the subject, and all three
teach the notation while hiding the decision.

**Manual dependency injection is not a stepping stone.** It is taught in Unit 1 as a
production technique with stated conditions of sufficiency, and Unit 6 returns to it as a
live option in a scenario where it wins. A curriculum that treats hand-wiring as the
beginner's version of Dagger has already answered Unit 6's question before asking it.

---

## Unit 1 — Dependency Injection as Object Construction

**Purpose:** establish what dependency injection actually changes about a class, and reach
every conclusion in the Unit without a container.
**Prerequisites:** the shipped architecture Lessons `lesson_dependency_direction_and_boundaries`,
`lesson_when_an_interface_is_a_boundary` and `lesson_dependency_inversion_in_practice`,
which already fix dependency direction, what makes an abstraction a boundary, and who owns a
contract. Unit 1 applies all three and re-derives none.

The Unit opens on **one class that needs something**, not on a framework: an object has work
to do, that work requires a collaborator, and the only question on the table is where the
collaborator comes from. Every concept below is introduced as an answer to that question.

#### L1.1 — Who Constructs This Object?

- **Objective:** for a class that needs a collaborator, name the three places the
  collaborator can come from, say which one is dependency injection, and say what changes
  about the class in each case.
- **Core:** a dependency is something a class needs in order to work; there are three ways
  it can get one — construct it itself, fetch it from somewhere global, or be given it; the
  third is dependency injection, and it is a property of the class's own signature rather
  than of any tool; "injection" names the direction of the handover, not a mechanism.
- **Practical:** the same small class written three ways, with the consequence of each made
  concrete — the self-constructing version cannot be used against a different
  implementation without editing it and silently decides its collaborator's lifetime; the
  fetching version compiles with an empty signature and fails at run time; the injected
  version cannot be built wrong. The claim "we do not use dependency injection, we have no
  framework" answered directly: a constructor parameter is dependency injection, and
  a project with no library can be doing it everywhere.
- **Senior:** why the interesting consequence is control rather than convenience — the
  caller decides, so a decision that was distributed across every class that constructed
  something is now made in one place, and that relocation is the whole point.
- **Primary:** `di_fundamentals`
- **Supporting:** `constructor_injection`, `manual_di`, `service_locator_vs_di`,
  `separation_of_concerns` (architecture)
- **Notes:** **Teach** the definition and the three-way contrast. **Exclude** every
  framework name beyond a single forward sentence saying that Units 3–5 show four encodings
  of this. **Exclude** testing — substitution is named as a consequence in L1.2 and
  the testing curriculum owns the practice.

#### L1.2 — A Dependency Should Be Visible

- **Objective:** say what a constructor parameter makes true that a field assigned later or
  a lookup does not, and explain why constructor injection is not a framework feature.
- **Core:** a required collaborator taken as a constructor parameter becomes part of the
  type; the object is fully formed the moment it exists, so there is no half-built state and
  no initialise-me call; the property can be immutable; a caller cannot fail to supply it.
- **Practical:** an injected **concrete** type that is still injected, which is the direct
  refutation of "dependency injection requires interfaces"; the same class written with a
  `lateinit` field set afterwards, and the two failure modes that introduces; a constructor
  that has grown to nine parameters read as a design signal about the class rather than as
  an argument for hiding them; optional collaborators and defaults. Substitution appears
  once, as a consequence: because the requirement is in the signature, a caller can supply
  something else — and *which* something else, and how it is built, belongs to the testing
  curriculum.
- **Senior:** why constructor injection predates every library in this curriculum and is
  specified by an independent standard the frameworks adopt rather than define, and why
  that is the reason the technique transfers unchanged between Dagger, Hilt, Koin and no
  framework at all.
- **Primary:** `constructor_injection`
- **Supporting:** `di_fundamentals`, `interface_boundaries` (architecture),
  `dependency_direction` (architecture), `test_doubles` (testing)
- **Notes:** **Teach** the visibility argument and both misconception corrections.
  **Bridge** to `lesson_when_an_interface_is_a_boundary` for whether an abstraction earns
  its existence — this Lesson must not re-argue it. **Exclude** field and setter injection
  as a technique; it is introduced in Unit 4 where a framework owning construction makes it
  necessary, which is the only honest motivation for it.

#### L1.3 — Injected, Inverted, or Both?

- **Objective:** given a graph, say separately whether a dependency was injected and whether
  the source dependency was inverted, and produce an example of each without the other.
- **Core:** injection is about **who supplies** an object; inversion is about **who owns the
  contract**; they are decided independently, at different times, by different people, and
  a codebase can do either without the other.
- **Practical:** the two shapes worked explicitly — a domain class taking a concrete
  storage implementation as a constructor parameter, which is fully injected and whose
  source dependency still points the wrong way; and a consumer-owned interface whose
  implementation is constructed by four lines in a `main` function, which is fully inverted
  with no container anywhere. Then the sentence that follows from both: adding a
  dependency-injection framework to a project changes no import, so it cannot by itself
  invert anything.
- **Senior:** why this is the most consequential confusion in the subject — a team that
  believes the framework inverted their dependencies stops looking at the arrows, and the
  architecture erodes while the wiring stays tidy.
- **Primary:** `di_fundamentals`
- **Supporting:** `dependency_direction` (architecture), `interface_boundaries`
  (architecture), `solid` (architecture), `constructor_injection`
- **Notes:** **Bridge** to `lesson_dependency_inversion_in_practice`, which already teaches
  inversion in full and already states that a container "decides which object is handed
  over; it does not change a single import". This Lesson **applies** that sentence from the
  injection side and must cite rather than repeat it. **Exclude** the full dependency-rule
  argument and Clean Architecture, both owned by the architecture curriculum.

#### L1.4 — One Place That Knows How to Build

- **Objective:** identify where construction knowledge belongs in a program, build a small
  graph by hand at that place, and recognise the same knowledge leaking into a feature
  class.
- **Core:** once classes stop constructing their own collaborators, something still has to;
  the composition root is the responsibility of assembling the object graph, located as
  close to the program's entry point as possible; it is the one place allowed to name every
  concrete implementation, which is what keeps every other place ignorant of them.
- **Practical:** a three-object graph assembled by hand in an entry point, then the same
  graph after a fourth object arrives, so the reader sees construction grow rather than
  meeting a finished diagram; a "helper" in the middle of a feature that constructs its own
  storage, identified as a second composition root and the reason the feature cannot be
  reused; the fact that a composition root is a responsibility and not a class name, which
  is why a program can have one without any type called `AppContainer`.
- **Senior:** why "as close to the entry point as possible" is the actual rule and why
  a platform with several entry points has several — an application object, a background
  entry, a test harness — and what has to be shared between them.
- **Primary:** `composition_root`
- **Supporting:** `manual_di`, `dependency_graphs`, `service_locator_vs_di`,
  `separation_of_concerns` (architecture), `layered_architecture` (architecture)
- **Notes:** **Teach** the responsibility and its location. **Exclude** containers,
  modules and registration DSLs — a composition root is not a container, and the difference
  is stated in the terminology register and carried into Unit 5. **Exclude** application
  startup ordering, process lifecycle and multi-process concerns.

#### L1.5 — Asking For It, or Being Given It

- **Objective:** given a piece of code that obtains a collaborator from a registry, say what
  the code no longer tells a reader, and distinguish that from a framework resolving a
  completed object at an integration boundary.
- **Core:** a service locator is a registry a class reaches into; both patterns end with the
  object in hand, and the difference is entirely in what the class's own signature says; a
  located dependency is invisible to callers, so a missing registration is discovered when
  that line runs rather than when the object is built.
- **Practical:** the same class in both shapes, with three concrete consequences of the
  located version — a reader cannot see what it needs, a caller cannot supply something
  else, and the failure moves from construction to first use; then the important
  qualification this curriculum insists on: a framework-created owner that is handed a
  finished object at the edge of a UI or platform integration is **not** the same design as
  a domain class consulting a global registry, because the located-from question is about
  *which* code hides *its own* requirements. Both halves are needed; taught only as
  "lookup bad", the Lesson makes Unit 4's field injection and Unit 5's Compose resolution
  look like defects.
- **Senior:** why the pattern keeps coming back despite the costs — it is the cheapest way
  to reach a graph from code somebody else constructs — and what has to be true for that
  trade to be acceptable, which is the criterion Units 4 and 5 then apply.
- **Primary:** `service_locator_vs_di`
- **Supporting:** `di_fundamentals`, `composition_root`, `constructor_injection`,
  `test_di` (testing)
- **Notes:** **Teach** the visibility and resolution-responsibility distinction, and the
  integration-boundary qualification, which is the one this curriculum needs most and the
  one most sources omit. **Exclude** test frameworks and global-state teardown mechanics.
  **Exclude** any claim that a specific library "is" a service locator.

#### L1.6 — When Wiring It Yourself Is the Right Answer

- **Objective:** for a described project, decide whether hand-written wiring is sufficient,
  and name the specific condition that would change the answer.
- **Core:** manual dependency injection is a complete technique, not an incomplete one —
  constructor parameters plus one place that assembles them express sharing, lifetimes and
  abstraction bindings without any library; what grows with the graph is clerical cost, not
  capability.
- **Practical:** the concrete costs, stated as things that actually happen — a new parameter
  deep in the graph edits every construction site above it, the assembly file becomes long
  and order-sensitive, and lifetimes become something a human has to remember; then the
  conditions under which none of that has bitten yet, and a worked judgement for a small
  application where adding a container would cost more than it returns. The official Android
  guidance's recommendation is reported accurately together with the problems it names, and
  the reader is shown how to read a recommendation as an argument rather than a rule.
- **Senior:** why the honest framing is a curve rather than a threshold — every project pays
  either wiring cost or framework cost, the two curves cross at a point that depends on graph
  size, rate of change and team familiarity, and Unit 6 is where that crossing is estimated
  rather than asserted.
- **Primary:** `manual_di`
- **Supporting:** `composition_root`, `dependency_graphs`, `di_framework_tradeoffs`,
  `architecture_tradeoffs` (architecture)
- **Notes:** **Teach** manual wiring as a production choice. **Exclude** the full framework
  comparison, which is Unit 6's and depends on Units 3–5 having happened. **Exclude** any
  claim that manual DI is what small teams do — size is one input among several.

---

## Unit 2 — Object Graphs, Lifetimes and Scopes

**Purpose:** turn a single injected constructor into reasoning about a whole graph, and fix
the lifetime vocabulary the three framework Units all depend on.
**Prerequisites:** Unit 1 entire; the shipped architecture Lessons
`lesson_choosing_the_owner_by_lifetime` and `lesson_viewmodel_lifetime_and_persistence`,
which already establish that an owner's lifetime decides what survives and that lifetime is
not persistence.

**This Unit is where the epic's terminology is decided.** Everything in Units 3–5 is a
notation for something named here, so a vocabulary failure here is a vocabulary failure four
times over.

#### L2.1 — From One Dependency to a Graph

- **Objective:** given a requested object, trace the transitive graph beneath it, name the
  construction responsibility at every edge, and identify the root.
- **Core:** a dependency's dependencies are dependencies; the transitive closure of those
  edges is the object graph; a graph has a root, which is the object somebody actually
  asked for; assembling the graph means satisfying every edge, in an order the edges
  themselves imply.
- **Practical:** the Unit 1 example grown until it has depth — a controller needing a
  repository needing a data source needing a configuration value — drawn once and then
  assembled by hand; what "the graph" means when two roots share a subtree; a cycle
  encountered honestly, as a design problem that no tool removes; and the observation that
  the assembly code is now long enough that its shape, rather than its content, is what a
  reader reacts to.
- **Senior:** why a graph is a better mental model than a dependency list — it makes the
  next three questions askable at all, because reuse, lifetime and ambiguity are properties
  of shared nodes rather than of any single class.
- **Primary:** `dependency_graphs`
- **Supporting:** `composition_root`, `manual_di`, `constructor_injection`,
  `layered_architecture` (architecture)
- **Notes:** **Teach** graph, edge, root and assembly as generic ideas. **Exclude** every
  framework's graph vocabulary; `@Component`, a Koin module and a container are all Units
  3–5. **Exclude** cycle-breaking techniques such as lazy or provider indirection, which
  are named once in Unit 3 where the mechanism exists.

#### L2.2 — One Instance, or a New One Each Time?

- **Objective:** for each node in a graph, choose between one shared instance and a fresh
  instance per request, and state the requirement that decided it.
- **Core:** reuse is a requirement, not a default; the three requirements that actually
  produce it are shared mutable state that must be the same state, identity that something
  else depends on, and construction expensive enough to matter; where none holds, a new
  instance per request is the simpler answer and costs nothing.
- **Practical:** the same graph resolved twice under two different requirements, so the
  reader sees the answer change without the code changing; the concrete symptom of getting
  it wrong in each direction — an object that was supposed to be per-screen quietly shared,
  and a cache rebuilt so often it never caches; the observation that a stateless object's
  reuse policy is usually a performance question with a boring answer.
- **Senior:** why "make it a singleton, it is cheaper" is a measurement claim people make
  without measuring, and why the real cost of over-sharing is not memory but the coupling
  between the screens that now share state.
- **Primary:** `di_scopes`
- **Supporting:** `dependency_graphs`, `state_ownership` (architecture),
  `architecture_tradeoffs` (architecture)
- **Notes:** **Teach** the reuse decision from requirements. **Exclude** every scope
  annotation and DSL keyword; this Lesson must be readable by someone who has never seen
  one, which is what makes Units 3–5 able to present theirs as notation.

#### L2.3 — A Scope Is a Rule; an Owner Is a Lifetime

- **Objective:** given a stated lifetime requirement, name the owner that must exist for
  that long, and explain why declaring a scope does not create one.
- **Core:** three different things that the word "scope" is routinely used for, separated
  and named — the **lifetime requirement** (how long product behaviour needs this identity
  to stay valid), the **owner** or container instance (the object that actually retains the
  instances), and the **scope** itself (a rule about reuse within an owner); the order of
  reasoning is requirement first, owner second, scope third; a scope name is a label on a
  rule and creates nothing.
- **Practical:** a requirement stated in product terms and turned into an owner; the same
  scope declared against two different owners and the two different lifetimes that result;
  the plain statement that a container-wide instance lives exactly as long as the container
  and no longer, so it is not immortal, not global in any process sense, and not persistent;
  and the separate, harder fact that nothing held only in memory survives the process being
  killed, which is why "singleton" and "durable" are unrelated words.
- **Senior:** why over-long scoping is a leak rather than an inefficiency — an owner that
  outlives what it retains keeps a destroyed thing alive — and why the reverse error is
  harder to see, because a rebuilt instance produces wrong behaviour rather than a crash.
- **Primary:** `di_scopes`
- **Supporting:** `dependency_graphs`, `state_ownership` (architecture),
  `android_process_model` (android_platform), `viewmodel_lifecycle` (lifecycle_navigation)
- **Notes:** **Acceptance-critical.** **Teach** the three-way separation and both
  misconception corrections. **Bridge** to `lesson_choosing_the_owner_by_lifetime`, which
  already teaches owner selection from a required lifetime — this Lesson reuses its
  conclusion and adds only what a container changes. **Exclude** `@Singleton`,
  `@ActivityRetainedScoped`, `single` and every other notation. **Exclude** process death
  and saved-state mechanisms beyond the single sentence that memory does not survive them.

#### L2.4 — A Value the Graph Cannot Know

- **Objective:** separate a graph dependency from a runtime input, and say why turning the
  second into the first is wrong rather than merely awkward.
- **Core:** a graph dependency is something the assembler can supply because it knows how to
  build it; a runtime input is a value chosen while the program runs — a selected
  identifier, a route argument, a user's answer — and the assembler cannot know it; the test
  is whether the value exists before the object is requested.
- **Practical:** an object needing a repository, a clock and a selected item id, with the
  first two resolved from the graph and the third arriving from the caller; the two bad
  answers shown failing — registering the id as a graph value, which means the *previous*
  screen's id is handed to the next one, and building the object without the id and setting
  it afterwards, which reintroduces the half-built state L1.2 removed; and the general shape
  of the right answer, a factory that takes exactly the runtime parameters and resolves
  everything else.
- **Senior:** why this distinction survives every framework unchanged and is the reason
  Dagger, Hilt and Koin each grew a separate mechanism for it — the graph is a description
  of how to build things, and a runtime value is data.
- **Primary:** `dependency_graphs`
- **Supporting:** `di_scopes`, `constructor_injection`,
  `navigation_fundamentals` (lifecycle_navigation), `state_ownership` (architecture)
- **Notes:** **Teach** the distinction and the general factory shape. **Exclude**
  `@AssistedInject`, `SavedStateHandle` and Koin `parametersOf`, all introduced later as
  encodings of this. **Exclude** navigation argument APIs; the lifecycle and navigation
  curriculum owns them.

#### L2.5 — Two Dependencies of the Same Type

- **Objective:** recognise same-type ambiguity as a design question, state what the
  assembler actually cannot decide, and name the two ways out before any mechanism exists.
- **Core:** an assembler chooses what to supply from a **binding key**, and a key is
  essentially a type; two different things of the same type therefore collide; the collision
  is information — it usually means the type is not saying what the consumer actually needs.
- **Practical:** two clients of the same class configured differently, and what a consumer
  asking for the bare type is really asking; the first answer, which is to give the two
  things different types so the question disappears; the second, which is to attach
  something extra to the key so the two remain distinguishable — named in the abstract here
  and encoded per framework later; and the third, deliberately shown as a non-answer:
  declaration order, module inclusion order and "the most specific one" are not selection
  rules.
- **Senior:** why the distinction between two implementations of one abstraction and two
  configurations of one implementation matters — the first is often a boundary the
  architecture curriculum would recognise, and the second is genuinely a keying problem.
- **Primary:** `dependency_graphs`
- **Supporting:** `dagger_qualifiers`, `interface_boundaries` (architecture),
  `constructor_injection`
- **Notes:** **Teach** the ambiguity as a design question. **Exclude** `@Qualifier`,
  `@Named` and Koin named definitions, which are Units 3 and 5. The `dagger_qualifiers`
  supporting mapping exists because the concept has no framework-independent Subtopic, and
  is recorded in the taxonomy gaps.

#### L2.6 — When Does a Broken Graph Tell You?

- **Objective:** for a graph with a missing or ambiguous edge, say when the failure becomes
  observable, and say what a validated graph does and does not prove.
- **Core:** a graph can be checked when it is described or when it is used; checking early
  means a missing edge is an error before the program runs, checking late means it is an
  error the first time that path executes; either way the check is about **completeness and
  unambiguity of the edges**, which is a much smaller claim than it sounds.
- **Practical:** the same missing edge under both regimes, with the concrete difference —
  a build that stops, versus a screen that opens once in fifty and crashes; the cost of
  early checking stated as work somebody pays for; and the correction the Unit exists to
  make: a graph that assembles is a graph whose edges are satisfiable, and says nothing
  about whether the arrows point the right way, whether the lifetimes are correct, or
  whether the design is good.
- **Senior:** why "it compiles, so the architecture is right" is a category error of exactly
  the kind the architecture curriculum already named, and why the reachability of a
  binding — not its declaration — is what any checker is actually reasoning about.
- **Primary:** `dependency_graphs`
- **Supporting:** `di_framework_tradeoffs`, `dagger_fundamentals`,
  `kotlin_gradle_plugin` (build_delivery)
- **Notes:** **Teach** the timing distinction generically, so that Unit 3's compile-time
  validation and Unit 5's resolution failures are both instances of it. **Exclude** any
  framework's error text, annotation processing, KSP and build configuration — all E29's.

---

## Unit 3 — Dagger: Compile-Time Object Graphs

**Purpose:** show one complete encoding of Units 1–2 in a system that writes the assembly
code for you and checks it before the program runs.
**Prerequisites:** Units 1 and 2 entire. Every Dagger API below is introduced as the answer
to a question one of those Units already made the reader ask.

**Dagger is a curriculum subject only.** No Lesson implies this or any other described
application uses it, and no Dagger dependency, plugin or processor is added to any module.

#### L3.1 — What Dagger Can Construct on Its Own

- **Objective:** say what marking a constructor achieves, describe what Dagger generates and
  when, and explain why no reflection is involved.
- **Core:** an annotated constructor tells the tool "you may build this, and these are its
  edges"; the tool writes, at build time, the factory code a person would otherwise write by
  hand; the generated component implementation is ordinary source compiled with the app, so
  at run time the program is calling generated functions rather than inspecting itself.
- **Practical:** the hand-written assembly from L1.4 placed beside what generation replaces,
  which is the honest statement of what Dagger is for; what the generated factory for a
  three-parameter class looks like in outline and why a reader never edits it; the
  consequence that construction cost is ordinary code rather than reflection, and the
  matching honesty that "no reflection" means predictable rather than free.
- **Senior:** why generating code that mimics what a person would have written is a design
  goal with consequences the reader can observe — a stack trace goes through readable
  generated frames, and a graph mistake is reported by a compiler rather than discovered in
  a debugger.
- **Primary:** `dagger_fundamentals`
- **Supporting:** `constructor_injection`, `manual_di`, `composition_root`,
  `dependency_graphs`
- **Notes:** **Teach** constructor bindings and generated construction. **Exclude** the
  build setup entirely — processors, KSP, kapt and Gradle wiring are E29's, and this Lesson
  states only that a build step exists. **Exclude** generated-file naming conventions
  beyond one illustrative mention.

#### L3.2 — Declaring the Rest of the Graph

- **Objective:** for a type Dagger cannot construct directly, choose between a provider
  method and an abstract binding, and justify the choice from what each can express.
- **Core:** an annotated constructor is unavailable whenever you do not own the class or the
  construction needs logic; a module is a grouping of binding declarations that fills those
  gaps; a provider method is a piece of code that returns the value, and an abstract binding
  simply says "when this abstraction is asked for, use that already-constructible
  implementation".
- **Practical:** three cases worked — a third-party client that has to be built with a
  builder, an interface whose implementation already has an annotated constructor, and a
  configuration value that comes from outside the graph; the rule the cases produce, which
  is that an abstract binding is available exactly when the implementation is already
  injectable and nothing else needs to happen; and the reason it is preferred where it
  applies, which is a real property of the tool rather than a style preference.
- **Senior:** why a module is a collection of declarations rather than a graph — it
  contributes edges and owns nothing — and why that distinction is what makes the same
  module installable in more than one graph.
- **Primary:** `dagger_modules`, `dagger_bindings`
- **Supporting:** `dagger_fundamentals`, `interface_boundaries` (architecture),
  `dependency_direction` (architecture)
- **Notes:** **Teach** the selection rule. **Exclude** presenting the choice as
  interchangeable syntax, which is the misconception the Lesson exists to defeat.
  **Exclude** module instance arguments and legacy module construction patterns beyond a
  Reference sentence.

#### L3.3 — Which Graph Owns This Binding?

- **Objective:** read a component declaration and say what graph it defines, what its API
  is, and which requests it can satisfy.
- **Core:** a component is the graph's root and its public surface at the same time — it
  declares what may be requested, and the tool generates an implementation that satisfies
  exactly those requests from the bindings reachable through its modules; the component
  instance is the thing a program holds, which makes it the concrete form of L1.4's
  composition root.
- **Practical:** a component declared with two provision methods and what changes when a
  third is added; what "reachable" means when a module contributes a binding nothing asks
  for; the observation that the component is where the program crosses from framework-free
  code into the graph, and therefore where L1.5's integration boundary sits in a Dagger
  application; and the correction that a component is not a bag of modules — it is an
  interface with an API, and the modules are how it is satisfied.
- **Senior:** why validation of the relationships between bindings happens at this level
  rather than per module, and what that implies for a module that is correct in isolation
  and unusable in a particular component.
- **Primary:** `dagger_components`
- **Supporting:** `dagger_modules`, `composition_root`, `dependency_graphs`,
  `dagger_bindings`
- **Notes:** **Teach** the component as graph root and declared API. **Exclude**
  `dagger.android`, builder and factory API surface beyond what one example needs, and
  every Android-specific component, which is Unit 4's.

#### L3.4 — A Child Graph, or a Separate Graph?

- **Objective:** given a feature that needs some of another graph's objects, choose between a
  nested graph and an independent graph that depends on it, and justify the choice by what
  each can see.
- **Core:** a nested graph inherits its parent's bindings — all of them, by construction —
  and may add its own and a shorter-lived scope beneath; an independent graph that names
  another as a dependency can use only what that graph publishes on its interface; the
  choice is about the width of the shared surface.
- **Practical:** one feature modelled both ways, with the two consequences made concrete —
  the nested version is convenient and the feature can reach anything, and the dependent
  version requires every shared type to be declared and cannot reach past them; visibility
  shown to flow one way only, so a parent cannot see a child's bindings; and the note that
  siblings cannot see each other, which is usually the property that made the split
  worthwhile.
- **Senior:** why a deliberately narrow surface is an architecture decision rather than a
  wiring one, and how it connects to the module-boundary reasoning the build curriculum
  owns without this Lesson entering it.
- **Primary:** `dagger_components`
- **Supporting:** `dagger_scopes`, `dependency_graphs`, `layered_architecture`
  (architecture), `module_dependency_direction` (build_delivery)
- **Notes:** **Teach** the distinction by visibility. **Exclude** multi-module Gradle
  structure, which the comparison is often confused with; a component boundary is not a
  build boundary and the Lesson says so once.

#### L3.5 — A Scope Is a Promise the Component Keeps

- **Objective:** say what a scope annotation actually causes, name the object whose lifetime
  bounds a scoped instance, and explain why two components produce two instances.
- **Core:** a scope annotation tells a component to reuse one result for that binding within
  itself; scoped instances are associated with **instances of components**, so the lifetime
  a reader is looking for is the component instance's; a component declares the scope it
  represents, and a nested graph may not repeat an ancestor's scope.
- **Practical:** the same annotated binding resolved twice from one component and once from
  a second component, with three instances counted; the direct correction of "the annotation
  means it lives forever", using the fact that releasing the component makes its scoped
  objects collectable; and the reason a nested graph cannot reuse its parent's scope, read
  as a statement about lifetimes rather than as a rule to memorise.
- **Senior:** the reuse-without-identity case — a scope that lets a tool avoid repeated
  allocation without promising one instance — presented as Reference, and why it is unsafe
  for anything whose identity matters.
- **Primary:** `dagger_scopes`
- **Supporting:** `di_scopes`, `dagger_components`, `state_ownership` (architecture)
- **Notes:** **Teach** scope as component-instance reuse, consistent with L2.3.
  **Exclude** defining custom scope annotations as a procedure; one example is enough.
  **Exclude** Android lifetimes, which Unit 4 supplies.

#### L3.6 — When the Type Is Not the Key

- **Objective:** given two bindings of the same type, and given many independent
  contributors to one collection, choose the mechanism that makes each request unambiguous.
- **Core:** the assembler selects by a key, and a key is a type plus an optional
  qualifier; a qualifier makes two same-type bindings into two different keys, and every
  consumer must then say which it means; a multibinding does the opposite — it makes one key
  an aggregate that several independently written modules contribute elements to.
- **Practical:** the same-type case from L2.5 encoded, with both the provider and every
  injection site updated, and the observation that a consumer now states its requirement;
  the contributor case worked as a set of feature initialisers, where the consumer injects
  the whole collection and names no contributor, so adding or removing a feature edits no
  consumer; and the note that the map form exists when contributors need to be looked up by
  a key.
- **Senior:** why multibinding is a dependency-direction tool rather than a collection
  convenience — the consumer depends on the abstraction, the contributors depend on the
  graph, and that inversion is the reason the pattern appears in plugin-shaped systems.
- **Primary:** `dagger_qualifiers`, `dagger_multibindings`
- **Supporting:** `dagger_bindings`, `dependency_graphs`, `dependency_direction`
  (architecture), `feature_modularization` (build_delivery)
- **Notes:** **Teach** both as key mechanics, which is what makes them one Lesson.
  **Exclude** the full multibinding API surface, custom map-key annotations beyond one
  example, and `@Multibinds` declarations — Reference at most.

#### L3.7 — What the Dagger Compiler Actually Checked

- **Objective:** say precisely which properties of a graph are verified before the program
  runs, and state what a successful build does not establish.
- **Core:** the tool resolves every key each component can reach and reports a missing
  binding or an ambiguous key as a build error; validation is of the relationships between
  bindings and happens at the component level; the guarantee is that the described graph can
  be assembled.
- **Practical:** both failure shapes as build output, and the reader asked to fix them by
  adding an edge rather than by changing a setting; the boundary of the guarantee worked
  explicitly on a graph that compiles perfectly and is wrong — a screen-lived object given
  an application lifetime, an abstraction whose implementation the consumer should never
  have known about; and the cost side, stated as a real trade: checking early means a build
  step and generated code.
- **Senior:** why this is the strongest and the most over-claimed property of the tool — it
  eliminates a specific class of runtime failure completely, and eliminates no design error
  whatsoever — and how to say both halves in an interview without sounding like either a
  salesman or a sceptic.
- **Primary:** `dagger_fundamentals`
- **Supporting:** `dependency_graphs`, `di_framework_tradeoffs`, `dagger_components`,
  `kotlin_gradle_plugin` (build_delivery)
- **Notes:** **Teach** what is and is not checked. **Exclude** processor configuration,
  incremental-build behaviour and build-speed measurement, all E29's; the cost is named as
  a fact and not analysed.

---

## Unit 4 — Hilt: Android Lifecycle-Aware Dagger

**Purpose:** answer one question — what does Hilt decide for an Android application that raw
Dagger leaves to the engineer — and answer it without re-teaching either Dagger or the
Android lifecycle.
**Prerequisites:** Unit 3 entire, and the shipped lifecycle and architecture material on
`ViewModel` ownership and lifetime, which this Unit applies rather than restates.

**Hilt is a curriculum subject only.** No Lesson implies this or any other described
application uses it, and no Hilt dependency, plugin or processor is added to any module.

#### L4.1 — Hilt Is Dagger With the Decisions Already Made

- **Objective:** say what Hilt adds to Dagger, and identify which of its rules are
  dependency injection and which are Hilt's conventions.
- **Core:** Hilt is built on Dagger and generates Dagger code; it does not introduce a second
  graph model; what it supplies is a fixed component hierarchy tied to Android lifetimes,
  named scopes for those components, and generated plumbing that attaches graphs to the
  classes Android creates.
- **Practical:** the same graph written twice, as hand-written Dagger components and as Hilt
  with its hierarchy, with the removed code pointed at rather than described; the direct
  correction of "Hilt is a different DI system"; and the statement of what is given up, which
  is the ability to design a component structure of your own — the decision Unit 4's last
  Lesson returns to.
- **Senior:** why an opinionated layer over a general tool is a real engineering trade rather
  than a simplification — conventions remove decisions, and a project whose lifetimes do not
  match the convention pays for them.
- **Primary:** `hilt_fundamentals`
- **Supporting:** `hilt_vs_dagger`, `dagger_components`, `dagger_fundamentals`,
  `activity_lifecycle` (lifecycle_navigation)
- **Notes:** **Teach** Hilt as generated convention over Dagger. **Exclude** setup, plugin
  and processor configuration — E29's. **Exclude** re-teaching Dagger; every Dagger concept
  is cited from Unit 3.

#### L4.2 — Which Android Component Owns This?

- **Objective:** given a lifetime requirement stated in Android terms, name the generated
  component that satisfies it and the scope annotation that goes with it, and say what
  choosing a longer-lived one would cost.
- **Core:** Hilt generates a fixed hierarchy of components whose creation and destruction
  follow Android classes; a binding installed in a component is visible there and in every
  component below it; a scope annotation reuses one instance per instance of that component;
  and the component's own lifetime is the lifetime — the annotation adds no durability, in
  exact continuation of L2.3.
- **Practical:** the hierarchy walked with each component's created-at and destroyed-at
  stated from current documentation; the retained component read carefully, because it is
  created at the first Activity creation and destroyed at the last, which is what makes it
  span configuration recreation; a requirement worked end to end — one instance shared by a
  screen and everything under it across a rotation — and the three candidate components
  compared on what each would actually give; and the plain statement that an
  application-lifetime binding is bounded by the process and is therefore not durable state.
- **Senior:** the documented warning that scoping costs memory until the component dies,
  read as the reason scoping is a decision rather than a default, and the observation that
  the Android component list has changed as the platform's own UI recommendations changed —
  which is why this Lesson's facts are re-verified rather than remembered.
- **Primary:** `hilt_components`
- **Supporting:** `di_scopes`, `dagger_scopes`, `activity_lifecycle` (lifecycle_navigation),
  `configuration_changes` (lifecycle_navigation), `android_process_model` (android_platform)
- **Notes:** **Source-sensitive; see the plan's Hilt findings.** **Teach** only the
  components a requirement in this Unit actually needs. **Exclude** an exhaustive component
  catalogue, default-binding tables beyond what an example uses, and Android lifecycle
  mechanics, which the lifecycle curriculum owns.

#### L4.3 — When Android Owns Construction

- **Objective:** explain why a framework-created class cannot use constructor injection, say
  what Hilt does instead and when, and state the cost of reaching for the escape hatch.
- **Core:** the platform instantiates certain classes itself through a no-argument
  constructor, so there is no call site to pass anything to; Hilt's answer is to generate
  code that populates annotated fields at a defined point in that class's lifecycle;
  field injection exists because construction responsibility belongs to somebody else, and
  for no other reason.
- **Practical:** the contrast drawn explicitly — an application-constructed service taking
  its collaborators as constructor parameters, beside a platform-constructed screen that
  cannot; what the annotated class gets and why the injected field cannot be private; the
  direct correction of "field injection is nicer because the constructor is shorter", which
  reintroduces exactly the half-built object L1.2 removed; and the case where Hilt does not
  support the owner at all, where an explicitly declared entry point reaches the graph from
  unmanaged code — presented as L1.5's integration boundary, with its cost named.
- **Senior:** why the supported-class list is a product decision rather than a technical
  limit, why it has moved as the platform's UI recommendations moved, and what a team should
  conclude from an entry point appearing in ordinary feature code rather than at an edge.
- **Primary:** `hilt_fundamentals`
- **Supporting:** `constructor_injection`, `service_locator_vs_di`,
  `activity_lifecycle` (lifecycle_navigation), `hilt_components`
- **Notes:** **Teach** field injection as a consequence of construction responsibility, and
  entry points as a bounded escape hatch. **Exclude** any suggestion that field injection is
  a style option. **Exclude** content-provider and platform-component mechanics beyond the
  one example the entry point needs.

#### L4.4 — ViewModels, Their Component, and the Values That Arrive Late

- **Objective:** say which component a ViewModel's dependencies come from, distinguish a
  retained-scoped object from a ViewModel-scoped one, and route a navigation-time value
  without putting it in the graph.
- **Core:** an annotated ViewModel is constructed by the graph, and its dependencies come
  from a component whose lifetime follows the ViewModel's; a ViewModel-scoped instance is
  one per ViewModel, and something that must be shared across several ViewModels needs a
  wider owner; the container constructing the ViewModel is not the thing that decides how
  long the ViewModel lives — the store owner is, exactly as the architecture curriculum
  already taught.
- **Practical:** the retained scope and the ViewModel scope placed side by side, with the
  documented consequence stated plainly — a ViewModel-scoped type is shared across
  everything injected into *that* ViewModel, and a different ViewModel asking for it gets a
  different instance; the lifetime of a scoped dependency separated from the ViewModel's own
  lifetime, which is the confusion that makes a per-screen object turn out to be
  process-wide; and the late-arriving value from L2.4 encoded twice, as an assisted factory
  that takes exactly the runtime parameters and as the platform's own saved-state route,
  with the choice between them stated.
- **Senior:** why a retained-scoped object is not a ViewModel even where their lifetimes
  coincide — one is cleared by a documented contract and the other is released with its
  component — and why treating them as interchangeable produces state that is never reset.
- **Primary:** `hilt_viewmodels`
- **Supporting:** `hilt_components`, `viewmodel_lifecycle` (lifecycle_navigation),
  `saved_state` (lifecycle_navigation), `state_ownership` (architecture),
  `dependency_graphs`
- **Notes:** **Acceptance-critical.** **Bridge** to the shipped
  `lesson_viewmodel_lifetime_and_persistence` for what a ViewModel survives; this Lesson
  adds only what the graph changes. **Exclude** the saved-state API surface and navigation
  argument mechanics. **Exclude** presenting assisted injection as the default answer for
  every dynamic value.

#### L4.5 — Which Graph Does This Binding Join?

- **Objective:** read an installation declaration and say where a binding can be injected,
  and separate that from how long its instance lives.
- **Core:** a module declares which generated component receives its bindings, and that
  decides **visibility** — where the binding can be requested from; lifetime is a separate
  decision expressed by a scope annotation, and an unscoped binding installed anywhere still
  produces a new instance per request; the component hierarchy is fixed, and modules join it
  rather than creating components.
- **Practical:** one module moved between two components with the visibility change traced,
  including the request that now fails to compile; the same module with and without a scope
  annotation, with the instance count changed and the visibility unchanged, which is the
  cleanest available demonstration that the two decisions are orthogonal; and the rule that a
  scoped binding must be installed in the component whose scope it names.
- **Senior:** why installation is a declaration about a graph rather than boilerplate, and
  why reading a codebase's installations is the fastest way to see which lifetimes its
  authors believed in.
- **Primary:** `hilt_modules`
- **Supporting:** `dagger_modules`, `hilt_components`, `dagger_scopes`, `di_scopes`
- **Notes:** **Teach** visibility and lifetime as two decisions. **Exclude** module
  organisation conventions and multi-module installation strategy, which E29 owns.

#### L4.6 — Hilt, or Components You Write Yourself?

- **Objective:** for a described Android project, decide between the convention and a
  hand-written component structure, and name what the decision costs in each direction.
- **Core:** the comparison is not capability — underneath it is the same tool, with the same
  bindings, scopes and compile-time checking; it is about who decides the component
  structure, and therefore about whether the project's lifetimes match the ones the
  convention assumes.
- **Practical:** two projects compared against the same axes — how closely their lifetimes
  match Android's, how many engineers have to understand the wiring, how much bespoke
  structure already exists, and what a migration would cost; the case where the convention
  is clearly right and the case where a lifetime the hierarchy does not express makes it
  awkward; and the refusal of a context-free verdict, which is the habit Unit 6 generalises.
- **Senior:** why "it is the recommended library" is a real input and not an argument —
  recommendations encode assumptions about the typical project, and the useful skill is
  saying which assumption your project breaks.
- **Primary:** `hilt_vs_dagger`
- **Supporting:** `hilt_components`, `dagger_components`, `di_framework_tradeoffs`,
  `architecture_tradeoffs` (architecture)
- **Notes:** **Teach** the comparison from requirements. **Exclude** migration procedure and
  build configuration. **Exclude** the four-way framework comparison, which is Unit 6's and
  needs Koin first.

---

## Unit 5 — Koin and Dependency Injection in KMP

**Purpose:** teach a container that is declared in ordinary Kotlin and therefore works from
common multiplatform code, and use a real multiplatform graph as the worked case study.
**Prerequisites:** Units 1–2 entire; Unit 3 for the vocabulary of bindings and graphs; the
shipped multiplatform ViewModel material, which supplies the fact that the owner is
platform-supplied.

**The worked case study is a real codebase.** Per Rule 11 of the authoring contract it is
presented as *an* application rather than as this one: the structure, the module names, the
host startup functions and the ViewModel definitions are real and are named, and the prose
never makes the learning app itself the subject.

#### L5.1 — The Container, and the Modules That Fill It

- **Objective:** describe what starting a Koin application creates, say where that belongs in
  a program, and read a set of modules as a description of a graph.
- **Core:** a Koin application holds a container of definitions; a module is a logical
  grouping of definitions and owns nothing by itself; starting the application with a list of
  modules is the composition root of L1.4, written as a declaration rather than as a sequence
  of constructor calls; the container's own lifetime is the lifetime that bounds everything
  it retains.
- **Practical:** a startup function that assembles several modules, read as the one place
  that knows how every implementation is built; modules grouped by concern, with the honest
  note that this is an organisational choice and not an architectural rule; the composition
  step that lets one module include others; and a bounded, accurate statement of how
  definitions may be declared in current Koin — the classic Kotlin DSL, annotations, and a
  compiler plugin that generates the DSL and verifies the configuration at compile time —
  together with the statement that the worked case study uses the classic DSL.
- **Senior:** why the whole graph being ordinary Kotlin is the property everything else in
  this Unit follows from: it compiles wherever Kotlin compiles, which is what makes a
  multiplatform graph possible at all, and it is also why the classic DSL's mistakes are
  found when a definition is resolved.
- **Primary:** `koin_fundamentals`
- **Supporting:** `composition_root`, `dependency_graphs`, `koin_definitions`,
  `di_framework_tradeoffs`, `kmp_architecture` (kmp)
- **Notes:** **Teach** container, module and startup as the composition root. **Bounded
  Reference** for the annotation and compiler-plugin capability: it exists, it changes when
  errors are found, and this curriculum teaches neither its setup nor its annotations —
  build configuration is E29's. **Exclude** module-loading performance, dynamic module
  loading and container introspection APIs.

#### L5.2 — Definitions, and the Reuse Requirement Behind Them

- **Objective:** choose between a retained definition and a per-request definition from a
  stated requirement, bind an implementation to an abstraction, and say what the retained
  keyword does and does not promise.
- **Core:** a retained definition gives every consumer the same instance for as long as the
  container holds it; a per-request definition constructs a new instance on every
  resolution; the choice is L2.2's reuse decision written down, and nothing else; binding an
  implementation to an interface is a separate decision about which key consumers ask for.
- **Practical:** the L2.2 requirements encoded, so the reader sees the same decision they
  already made expressed as a keyword; the retained keyword's three misreadings corrected in
  turn — it does not mean a Kotlin `object`, it does not mean persistent, and it does not
  outlive the container; the per-request keyword's misreading corrected too, since no factory
  class is required and the word names a strategy; and a definition that binds a repository
  interface to a local implementation, read as the architecture curriculum's inversion with
  the wiring now declared in one file.
- **Senior:** why constructor-first design survives a container entirely — each definition is
  a constructor call, the class itself names no container, and a class that instead resolves
  its own collaborators has become L1.5's service locator no matter which library supplied it.
- **Primary:** `koin_definitions`
- **Supporting:** `koin_fundamentals`, `di_scopes`, `constructor_injection`,
  `interface_boundaries` (architecture), `service_locator_vs_di`
- **Notes:** **Teach** the definitions from requirements. **Exclude** the full DSL surface —
  named definitions, eager creation, property injection and module override rules are
  Reference at most. **Exclude** any claim that the case study's choice of a retained
  definition proves that repositories should generally be retained.

#### L5.3 — Scopes, and the Owner That Has to Stay Alive

- **Objective:** decide whether a lifetime requirement needs a bounded scope rather than a
  container-wide or per-request definition, and name the object whose lifetime bounds it.
- **Core:** a Koin scope is a sub-container with an identity and a lifetime of its own;
  instances defined inside it are reused within it and released when it closes; creating and
  closing the scope is somebody's responsibility, and that somebody is the owner L2.3 already
  made the reader identify.
- **Practical:** one requirement that genuinely needs it — several screens sharing state for
  the duration of a bounded activity such as a signed-in session or a multi-step flow — with
  the two wrong answers shown failing, since a container-wide definition never resets and a
  per-request definition never shares; what closing the scope does; and the failure mode of
  resolving from a closed scope, which is the concrete form of "the owner must actually live
  that long".
- **Senior:** why an explicitly created scope is the case where the lifetime requirement has
  no existing owner to attach to, and why creating one is therefore a design decision with an
  owner to write rather than a configuration setting.
- **Primary:** `koin_scopes`
- **Supporting:** `di_scopes`, `koin_definitions`, `koin_fundamentals`,
  `state_ownership` (architecture)
- **Notes:** **Teach** only as far as the lifetime model needs. **Exclude** the full scope
  API — scope archetypes, platform scope components, linked scopes and scope callbacks are
  Reference at most. The objective is expressing an already-decided lifetime, not touring the
  API.

#### L5.4 — Resolving a ViewModel at the Boundary

- **Objective:** say what the container does and does not decide when a screen resolves a
  ViewModel, pass a runtime value correctly, and judge whether a given resolution call is at
  an acceptable boundary.
- **Core:** a ViewModel definition tells the container how to construct one; the resolution
  call at a screen obtains it through the platform's own store owner, so the container
  constructs and the owner decides the lifetime; a runtime value is passed as a parameter at
  the resolution site, which is L2.4's distinction encoded.
- **Practical:** a destination resolving a ViewModel with no parameters and another passing a
  route-supplied identifier, read from a real multiplatform graph; the parameters read
  positionally when two identifiers share a type, which is exactly L2.5's ambiguity arriving
  in a place the reader now recognises; and the boundary judgement worked, using real
  resolution sites — a destination composable resolving a completed object graph is an
  integration boundary, a theme wrapper tolerating an absent container is infrastructure, and
  a domain class calling into the container would be the service-locator shape L1.5 rejected.
- **Senior:** why "the container created it" and "the container owns it" are different
  claims, and why the second is false here: the store owner supplied by the host is what
  clears the ViewModel, which is the multiplatform fact the shipped curriculum already
  established.
- **Primary:** `koin_viewmodels`
- **Supporting:** `koin_definitions`, `service_locator_vs_di`,
  `kmp_lifecycle_viewmodel` (kmp), `viewmodel_lifecycle` (lifecycle_navigation),
  `state_ownership` (architecture)
- **Notes:** **Bridge** to the shipped multiplatform ViewModel material rather than
  re-teaching ownership. **Exclude** navigation APIs and back-stack mechanics.
  **Exclude** Compose runtime mechanics; the resolution call is shown, the Compose
  curriculum owns everything around it.

#### L5.5 — One Graph, Several Platforms

- **Objective:** split a graph into definitions that belong in shared code and definitions
  each platform must supply, and describe how each host assembles the whole.
- **Core:** shared code can declare every definition whose construction is platform-agnostic;
  what cannot be shared is anything constructed from a platform's own APIs; the shared code
  therefore declares the abstraction and each platform supplies a module binding it; and each
  host has its own composition root that assembles the shared modules together with its own.
- **Practical:** the real case study read end to end — a preference-storage abstraction
  declared in shared code with a store and a state holder built on it, and four platform
  modules binding four different implementations to it, one per host; the four startup
  functions listed side by side, differing only in the platform modules they add and in what
  the platform needs to hand the graph; a database binding split the same way; and the
  explicit statement of what this demonstrates — that one shared graph plus per-platform
  bindings is a workable arrangement — and of what it does not, since it is one small
  local-first application with no networking and no second implementation of anything.
- **Senior:** why a container declared in ordinary Kotlin is a candidate here at all while
  an annotation processor emitting platform-specific code is not, stated as a property of the
  tools rather than as a ranking, and why that single fact does more to decide framework
  choice on a multiplatform project than any other axis in Unit 6.
- **Primary:** `koin_multiplatform`
- **Supporting:** `koin_fundamentals`, `expect_actual` (kmp),
  `platform_implementations` (kmp), `kmp_architecture` (kmp), `composition_root`,
  `interface_boundaries` (architecture)
- **Notes:** **Bridge** only: the interface-plus-platform-module pattern is shown because the
  graph needs it. **Exclude** source-set design, `expect`/`actual` mechanics, native interop
  and iOS integration architecture — E33's entire subject. **Exclude** any proposal to change
  the case study's graph; observations about it are recorded, not acted on.

---

## Unit 6 — Choosing a Dependency Injection Strategy

**Purpose:** decide, from stated requirements, which of the four strategies a project should
use — including the answer that it should add nothing.
**Prerequisites:** Units 1–5 entire. This Unit introduces no new mechanism.

#### L6.1 — What a Container Actually Buys

- **Objective:** name the decision axes that separate the four strategies, and state each one
  as an observable consequence rather than as a preference.
- **Core:** every axis is phrased the same way — a requirement makes a property useful, and a
  strategy supplies that property at a cost; the axes worth reasoning about are graph size and
  rate of change, how much assembly code a human writes, when a graph error is detected,
  lifecycle integration with a platform, target platforms, build cost, how much the tool
  decides by convention, how observable the graph is when debugging, team familiarity, and
  the cost of changing later.
- **Practical:** the four strategies laid against those axes with the consequence named in
  each cell rather than a verdict; three sentences the Unit refuses, taken apart one at a
  time — that compile-time checking is inherently better, that runtime flexibility is
  inherently better, and that the recommended option is therefore the correct one; and the
  distinction between a documented capability and a community claim, with the reader shown
  how to tell which they are reading.
- **Senior:** why every axis is really about *when* you pay — a cost paid at build time, at
  wiring time, at review time or at 3am — and why that reframing makes the comparison
  arguable rather than tribal.
- **Primary:** `di_framework_tradeoffs`
- **Supporting:** `manual_di`, `dagger_fundamentals`, `hilt_fundamentals`,
  `koin_fundamentals`, `architecture_tradeoffs` (architecture)
- **Notes:** **Teach** the axes. **Exclude** a scorecard, a ranking, and any table whose
  bottom row is a winner.

#### L6.2 — When Should a Graph Error Surface?

- **Objective:** state accurately, for each strategy, when a missing or ambiguous binding is
  detected, and say what the earlier detection does and does not prove.
- **Core:** detection time is a property of how a graph is described and checked, not of a
  brand; a generated, compile-checked graph reports a missing edge as a build failure; a
  graph described in ordinary code and resolved at run time reports it when that definition
  is first requested — unless a compile-time verifier is used, which current Koin supplies as
  an option; so the honest axis is "when does this project's configuration get checked",
  and the answer depends on the mechanism chosen rather than on the library's name.
- **Practical:** the same missing edge traced through each strategy, including hand-written
  wiring, where the compiler catches it because a constructor call will not compile —
  an uncomfortable fact worth stating, because it shows early detection is not something only
  a generator can supply; the cost of early detection named as build work and generated code;
  and the direct correction of misconception 14, reusing L3.7's compiling-but-wrong graph.
- **Senior:** why a comparison written before the current tooling is now misleading, and what
  that implies for how an engineer should answer this question in an interview — by naming
  the mechanism the project uses and the version it is on, not by repeating a slogan.
- **Primary:** `di_framework_tradeoffs`
- **Supporting:** `dagger_fundamentals`, `koin_fundamentals`, `dependency_graphs`,
  `manual_di`, `kotlin_gradle_plugin` (build_delivery)
- **Notes:** **Acceptance-critical, and source-sensitive.** **Teach** detection time as a
  mechanism property. **Exclude** compiler-plugin and annotation-processor setup entirely.

#### L6.3 — Three Projects, Three Answers

- **Objective:** choose a strategy for each of three described projects and defend the choice
  from the requirements, not from the strategy.
- **Core:** the method is fixed and applied three times — list the requirements, identify
  which properties they make valuable, identify which strategies supply those properties,
  and name the cost being accepted.
- **Practical:** a small, stable, single-platform application with a graph of a dozen objects,
  where hand-written wiring is the correct answer and adding a container would be paying for
  a problem the project does not have; a growing Android-only application with
  platform-created owners, several screen-level owners, lifecycle-bound lifetimes and a graph
  that changes weekly, where a generated, lifecycle-aware graph starts paying and both the
  convention and a hand-written structure are weighed; and a multiplatform application with
  shared repositories, shared state holders and shared ViewModels plus per-platform bindings,
  where a container declared in ordinary Kotlin is a candidate and an annotation processor
  emitting platform-specific code is not — stated as a constraint the project has, not as a
  ranking of libraries.
- **Senior:** the fourth scenario the reader is asked to construct themselves — the one where
  the obvious answer is wrong — and the interview habit it teaches, which is to ask what the
  project looks like before answering which framework is best.
- **Primary:** `di_framework_tradeoffs`
- **Supporting:** `manual_di`, `hilt_vs_dagger`, `koin_multiplatform`, `dagger_components`,
  `kmp_architecture` (kmp)
- **Notes:** **Teach** the method by application. **Exclude** any scenario engineered so that
  a predetermined answer wins, and any scenario whose graph is inflated to force a framework.

#### L6.4 — The Smallest Sufficient Strategy

- **Objective:** given a project, choose the smallest strategy that satisfies its
  requirements, and name the specific change that would justify the next one.
- **Core:** the same proportionality reasoning the architecture curriculum ends on, applied to
  wiring: a strategy earns its cost when the requirements make its properties worth paying
  for, and the default is the least machinery that satisfies them; migration cost and team
  familiarity are inputs to the decision rather than excuses applied afterwards.
- **Practical:** a project whose right answer is no container at all, worked to completion so
  the reader sees the conclusion defended rather than apologised for; the trigger conditions
  that would change it, each stated as something a person could observe happening; migration
  in both directions costed honestly, including the part that is not code; and the closing
  observation that a team fluent in one strategy has a real advantage that a feature
  comparison cannot see.
- **Senior:** why adopting a framework is a decision a team lives inside for years, why the
  reversibility of the decision matters more than its optimality, and why constructor-first
  design is what keeps it reversible — a codebase whose classes state their requirements can
  change how they are supplied.
- **Primary:** `manual_di`, `di_framework_tradeoffs`
- **Supporting:** `composition_root`, `architecture_tradeoffs` (architecture),
  `di_fundamentals`, `constructor_injection`
- **Notes:** **Bridge** to `lesson_smallest_sufficient_architecture`, which already teaches
  proportionality; this Lesson applies it to one new decision and cites rather than repeats.
  **Exclude** organisational change management and tooling-adoption process.

---

## Misconception and Reasoning Targets

Recorded as reasoning a learner must be able to demonstrate, not as slogans to recite. Each
entry names the Lesson responsible for correcting it. Correcting a misconception means
replacing it with a decision the learner can make, so each row says what the learner does
instead.

| # | Misconception | Corrected in | The reasoning that replaces it |
| --- | --- | --- | --- |
| 1 | "Dependency injection requires a framework." | L1.1 | Point at a constructor parameter and say what was injected, by whom, and what changed about the class |
| 2 | "Dependency injection requires interfaces." | L1.2 | Inject a concrete type and say what is still true of it; then name separately the condition that would make an abstraction worth adding |
| 3 | "Constructor injection is a Dagger feature." | L1.2 | Write the same constructor with no library present and state what it guarantees on its own |
| 4 | "Dependency injection and dependency inversion are the same thing." | L1.3 | Produce an injected dependency whose direction is still wrong, and an inverted one wired by hand |
| 5 | "Using a DI framework automatically produces good architecture." | L1.3, L3.7 | Trace the imports before and after adding a container and observe that none changed |
| 6 | "Every class should get its dependencies from the container." | L1.5, L5.2 | Name what a class's signature stops telling a reader, and say where resolution is acceptable and why |
| 7 | "Service locator and injection are equivalent because both hand you an object." | L1.5 | Compare the two signatures, and say when each design discovers a missing dependency |
| 8 | "More interfaces means more testability." | L1.2 | Argue substitutability from explicit requirements, and leave the abstraction decision to the boundary test |
| 9 | "Declaring a scope defines how long the object lives." | L2.3, L3.5, L4.2 | Name the owner whose lifetime bounds the instance, and say what has to be true for that owner to exist |
| 10 | "Singleton means globally immortal." | L2.3, L4.2 | State the container or component instance that bounds it, and say what releasing that owner does |
| 11 | "Singleton means the value survives the process." | L2.3, L4.2 | Separate in-memory reuse from durable storage, and name the mechanism that would actually be needed |
| 12 | "Koin `single` means a Kotlin `object`." | L5.2 | Describe it as a definition strategy the container applies, and name the container that bounds it |
| 13 | "Koin `factory` requires a factory class." | L5.2 | Describe it as one instance per resolution, and show the definition constructing an ordinary class |
| 14 | "A compile-time validated graph proves the architecture is correct." | L3.7, L6.2 | Show a graph that compiles and is wrong, and state the exact property the checker established |
| 15 | "`@Provides` and `@Binds` are interchangeable style choices." | L3.2 | Name the case each one can express that the other cannot, and choose from that |
| 16 | "A component is just a bag of modules." | L3.3 | Read the component's declared API and say which requests it satisfies and which it cannot |
| 17 | "Subcomponents and component dependencies are the same thing." | L3.4 | Say what each graph can see, and choose from the width of the shared surface |
| 18 | "Qualifiers are names for arbitrary implementation choices." | L2.5, L3.6 | Describe the binding key and say what made two bindings collide |
| 19 | "Multibinding is convenient collection syntax." | L3.6 | Name the dependency direction it establishes between the consumer and the contributors |
| 20 | "Hilt is a separate DI model from Dagger." | L4.1 | Identify which rules come from Dagger and which are Hilt's conventions, in one example |
| 21 | "Field injection is preferable because constructors get long." | L4.3 | Name who owns construction; where the application owns it, use the constructor and read a long one as a design signal |
| 22 | "A singleton-component binding gives process-durable state." | L4.2 | Name the component's created-at and destroyed-at, and the storage that would be needed instead |
| 23 | "Retained scope and ViewModel scope are the same lifetime." | L4.4 | Say which owner each belongs to, and what a second ViewModel asking for the binding receives |
| 24 | "A container owns the ViewModel's lifetime because it constructed it." | L4.4, L5.4 | Separate construction from store ownership, and name the host that clears it |
| 25 | "Koin is inherently runtime-only." | L5.1, L6.2 | Name the mechanism the project uses and say when its configuration is checked |
| 26 | "Hilt is always the right Android choice." | L4.6, L6.3 | State the requirement that makes the convention valuable, and one a project could have that it does not fit |
| 27 | "Koin is always the right multiplatform choice." | L5.5, L6.3 | State the constraint that makes it a candidate, and the costs that constraint does not remove |
| 28 | "Manual dependency injection is only for toy applications." | L1.6, L6.4 | Name the conditions under which hand-wiring is sufficient, and the specific change that would end them |
| 29 | "The graph should hold every value a class needs." | L2.4, L4.4, L5.4 | Separate a graph dependency from a runtime input by asking whether the value exists before the request |
| 30 | "A missing binding is a configuration problem." | L2.5, L2.6 | Read the ambiguity or the gap as information about the design, and fix the edge rather than the settings |
| 31 | "A shared instance is the cheaper default." | L2.2 | Name the requirement that demands reuse; where none exists, construct per request and say why that costs nothing |

Misconceptions 29 to 31 were added during this review rather than taken from the epic's
list: 29 and 30 recur across all four framework encodings, and 31 is the belief the
production evidence in the plan's repository section is most likely to reinforce if it is
presented without its reasons.

## Terminology This Curriculum Fixes

Several of these words are overloaded, and four of them mean different things in each of the
three frameworks this curriculum teaches. Every Lesson uses them as defined here, and a
framework-specific word never redefines a generic one.

| Term | How this curriculum uses it |
| --- | --- |
| **Dependency** | Something a class needs in order to do its work. |
| **Collaborator** | A dependency that is itself an object with behaviour. Used where "dependency" would be ambiguous with a build dependency. |
| **Dependency requirement** | The statement that a class needs a particular capability. It exists before any decision about who supplies it. |
| **Construction** | Creating an instance. Separate from deciding what it is given and from deciding how long it lives. |
| **Injection** | Supplying a dependency to an object from outside it. A direction of handover, never a library. |
| **Constructor injection** | Injection through constructor parameters, so the requirement is part of the type. |
| **Field injection** | Assignment into an already-constructed object's fields by a framework. Used only where the framework owns construction. |
| **Composition root** | The responsibility, located near an entry point, of assembling the object graph. A responsibility and a location, never a class name and never a synonym for container. |
| **Container** | A runtime object that holds definitions, constructs instances from them and retains the ones it is told to. One possible implementation of part of a composition root's work. |
| **Service locator** | A registry a class reaches into for its own collaborators, so its requirements do not appear in its signature. |
| **Resolution** | Obtaining an instance from a container. Resolution at an integration boundary is not the same design as a business object resolving its own collaborators. |
| **Binding** | A declaration of how a request for a particular key is satisfied. |
| **Binding key** | What the assembler matches a request against: a type, plus anything that distinguishes two bindings of that type. |
| **Object graph** | The transitive closure of a root object and everything it needs, directly and indirectly. |
| **Graph root** | The object actually requested, from which the graph is reached. |
| **Graph assembly** | Satisfying every edge of a graph, whether by hand or by a tool. |
| **Instance identity** | Whether two consumers hold the same object. The thing reuse decisions are actually about. |
| **Lifetime requirement** | How long product behaviour needs a particular identity or piece of state to remain valid. Stated before any owner is chosen. |
| **Lifetime** | How long an object exists, and what event ends it. |
| **Owner / scope owner** | The object whose lifetime bounds the instances retained within it. What actually decides a lifetime. |
| **Scope** | A rule about reuse and identity within an owner. A label on a rule; it creates no owner and grants no durability. |
| **Container lifetime** | How long the container or component instance that retains scoped instances exists. Always stated when a scope is discussed. |
| **Runtime parameter / runtime input** | A value chosen while the program runs, which the assembler cannot know and must not hold. |
| **Qualifier** | Something attached to a binding key so that two bindings of one type become two keys. |
| **Multibinding** | A binding whose value is a collection that several independent declarations contribute elements to. |
| **Component** | In Dagger and Hilt, a generated graph with a declared API and an instance whose lifetime bounds its scoped objects. Never used generically for "a part of the app". |
| **Subcomponent** | A graph nested inside another, which can see all of its parent's bindings. |
| **Component dependency** | An independent graph that may use only what another graph publishes on its interface. |
| **Module** | In Dagger, Hilt and Koin, a grouping of binding declarations. **Never** used for a Gradle build unit, which this curriculum calls a build module and defers to the build curriculum. |
| **Definition** | Koin's term for one binding declaration. Used in Unit 5 in place of "binding" where the DSL word is meant. |
| **Dependency injection** | Supplying a class's dependencies from outside it. |
| **Dependency inversion** | The consumer defining the abstraction its implementation must satisfy, so the source dependency points inward. A different decision from injection, decided at a different time. |

**Two collisions are stated explicitly where they occur.** "Module" means a grouping of
binding declarations in all three frameworks and a build unit in Gradle; L3.2 and L5.1 both
say so. "Scope" is a rule here and a coroutine's structured-concurrency boundary in the
shipped Coroutines Units; L2.3 says so and the two are never used in one sentence unqualified.

## Reference Material

Concise treatment is worthwhile, but none of this belongs on the main learning path and none
of it is a prerequisite for anything above.

| Area | Why Reference rather than Teach | Where it sits |
| --- | --- | --- |
| The independent injection annotation standard the frameworks build on | Explains why constructor injection transfers unchanged between them | One sentence in L1.2 |
| Reflection-based containers as a family | Explains what the generated-code approach was reacting to | One clause in L3.1 and L6.1 |
| Dagger's allocation-reducing scope that does not promise identity | A real feature, but unsafe for anything whose identity matters | One paragraph in L3.5 |
| Map-keyed multibindings | The set form carries the reasoning; the map form is a variation | Named in L3.6 |
| Lazy and provider indirection | A mechanism for deferred and repeated construction, including cycle-breaking | Named once in L3.1 or L3.3 |
| Hilt's default bindings per component | Useful to know they exist; the list dates quickly | Named in L4.2 where one example uses one |
| Koin's module composition through inclusion | An organisational convenience that changes no decision | One line in L5.1 |
| Koin annotations and the Koin compiler plugin | Materially changes the accuracy of the framework comparison; its setup does not | Bounded in L5.1, applied in L6.2 |
| Koin's platform scope helpers | Convenience over the scope model L5.3 teaches | Named in L5.3 |
| Named framework alternatives outside the four taught | Recognition value only | One clause in L6.1 |

## Excluded Material

Accurate, but it does not improve interview readiness and would add noise. Each exclusion is
revisitable if the target job profile changes — by editing this blueprint, not by quietly
adding a Lesson.

| Area | Why excluded |
| --- | --- |
| Annotation-processor and compiler-plugin build configuration, KSP and kapt setup, generated-source directories | E29's subject; this curriculum states that a build step exists and analyses none of it |
| Build-performance measurement and optimisation for code generation | E29's subject; L3.7 and L6.1 name the cost as a fact |
| Gradle module structure, convention plugins, feature-module wiring | E29's subject; a framework module is not a build module and this curriculum says so twice |
| Test doubles, mocking libraries, fakes, test component replacement, Hilt test modules, Koin test APIs | E31's subject; substitution is taught as a consequence of explicit requirements and no Lesson shows a test |
| Unit-test strategy and architecture testing | E31's subject |
| Kotlin Multiplatform source-set design, `expect`/`actual` mechanics, native interop, iOS integration architecture | E33's subject; Unit 5 uses only the platform-binding pattern its graph needs |
| Activity and Fragment lifecycle callbacks, navigation APIs, back-stack mechanics, the saved-state API surface | Owned by the lifecycle and navigation curriculum; Unit 4 uses their lifetime conclusions |
| `ViewModel` lifetime and clearing as a subject | Owned by the architecture and lifecycle curricula; Units 4 and 5 apply the conclusion and cite it |
| Dagger Producers, gRPC integrations, `dagger.android`, Dagger SPI | Specialised or legacy surfaces with no interview signal for this profile |
| Exhaustive annotation and DSL catalogues for any of the three frameworks | Rule 5 of the authoring contract exists to prevent exactly this |
| Framework version histories, migration guides and release notes | Product trivia; the curriculum records which documentation version each claim rests on and stops |
| Spring, Guice, Koin alternatives and other ecosystems' containers | Outside the Android and Kotlin Multiplatform target profile |
| Adding Dagger or Hilt to this repository, or migrating it away from Koin | Explicitly out of scope for the epic; the codebase is evidence, not a target |
| Refactoring the case study's Koin graph | E27 is learning-content work; any observation is recorded for a separate issue and not acted on |

## Taxonomy Gaps: Concepts With No Exact Assessment Subtopic

Recorded rather than given invented IDs. **E27-01 does not change the question taxonomy**;
whether any of these should become a Subtopic is a separate decision for a future
question-bank change. Each row names the mapping the curriculum uses instead, so the gap is
visible to E27-08 and to that later decision without blocking authoring.

| Concept | Nearest existing Subtopic | Decision |
| --- | --- | --- |
| Dependency injection distinguished from dependency inversion | `di_fundamentals` | The distinction is about what injection is and is not, which is what the Subtopic is named for. The inversion half is already `dependency_direction` in the `architecture` Topic and is deliberately left there; L1.3 bridges rather than claims it. |
| Lifetime requirement as distinct from scope and from container lifetime | `di_scopes` | The Subtopic is named "Scopes and lifetimes", so both belong to it. Recorded because the separation is acceptance-critical and a single Subtopic cannot express which of the three a Question is about. |
| Runtime input distinguished from a graph dependency | `dependency_graphs` | The decision is about what belongs in the graph, so the graph Subtopic is the honest home. The framework encodings sit in `hilt_viewmodels` (assisted injection) and `koin_viewmodels` (parameters), which is why the generic form has no coverage today. |
| Same-type ambiguity as a design question, before any mechanism | `dependency_graphs` | Only `dagger_qualifiers` names the concept, and it names a Dagger mechanism. Mapping the generic Lesson to `dependency_graphs` keeps the framework Subtopic framework-specific. |
| Graph validation and failure timing as a generic idea | `dependency_graphs` | The one ACTIVE Question here is Dagger-specific, which is itself the finding; the plan records it as a re-map candidate so the Subtopic can hold the generic reasoning. |
| Where framework resolution is acceptable — the integration boundary | `service_locator_vs_di` | Semantically correct: the Subtopic is the comparison, and the boundary qualification is the half of the comparison the bank currently omits. |
| Koin's current declaration mechanisms and when each is checked | `koin_fundamentals` | Correct, and deliberately bounded — assessing which mechanism a release ships would be product trivia rather than reasoning. |
| Choosing a strategy from stated project requirements | `di_framework_tradeoffs` | Exactly what the Subtopic is named for, and it holds no ACTIVE Question, which is the epic's largest single assessment gap. |
| Koin in Kotlin Multiplatform | `koin_multiplatform` (dependency_injection) | **A second Subtopic, `koin_kmp`, exists in the `kmp` Topic with the same meaning and no Questions.** E27 uses the `dependency_injection` one because its Units home there. The duplication is recorded as a cross-topic guard, not resolved: removing or merging a Subtopic is a question-bank change. |

**No `dependency_injection` Subtopic is left unmapped.** All 25 are primary in at least one
Lesson, which is the shape a purpose-built taxonomy should have and is the difference from
the `architecture` Topic, where `solid` was deliberately supporting-only. The complete
inventory, with statuses, Question counts and the Teach decision for each, is in the plan's
[taxonomy inventory](dependency-injection-units-1-6-plan.md#part-1--complete-dependency-injection-taxonomy-inventory).

## Authoritative Source Families

Identified so that a Lesson author does not start from a blank search. Individual Lessons
still cite the specific page supporting each claim, per Rule 9. **This subject is the most
source-sensitive in the curriculum so far**: three of its five framework-facing Units rest on
library documentation that changes between releases, and one rests on a codebase that can
change in any commit.

**Tier 1 — normative for Dagger.** The official Dagger documentation at `dagger.dev`: the
user guide, basic usage, subcomponents, multibindings and assisted injection. These are
authoritative for what the tool does. Behaviour claims about generated code, scoping and
compile-time validation are settled here and nowhere else.

**Tier 1 — normative for Hilt.** Two official sources, and the plan records that they
currently differ in coverage: the Android Developers Hilt documentation, which is
authoritative for what Android recommends and for the components it documents, and the Hilt
documentation at `dagger.dev`, which is authoritative for the library's full component set.
A Lesson that names a component must say which source it rests on. Android-recommended
practice and library capability are not the same claim.

**Tier 1 — normative for Koin.** The official Koin documentation at `insert-koin.io`, read at
the version matching the configured library. Definitions, scopes, modules, the Compose
integrations and the multiplatform guidance are settled there. Marketing pages and comparison
posts are not sources.

**Tier 1 — normative for generic and Android dependency injection.** The official Android
dependency-injection guidance — the overview and the manual dependency-injection page — is
authoritative for the Android framing, for the service-locator comparison and for the costs
of hand-wiring. Where it recommends, the Lesson teaches the recommendation together with the
condition that produces it rather than converting it into a rule.

**Tier 1 — this repository's own source, for Unit 5 only.** Claims about the worked
multiplatform graph are settled against the code — the version catalog, the `commonMain` and
per-platform modules, the four host startup functions, the ViewModel definitions and the
destinations that resolve them — and never inferred from framework documentation. The plan's
repository evidence matrix records what each file demonstrates and what it does not.

**Tier 2 — reused settled sources.** Dependency direction, inversion, boundaries and
proportionality are settled by E26's sources and are not re-researched; ViewModel lifetime and
ownership are settled by E26's and the lifecycle curriculum's. E27 cites them and adds no new
claim about them.

**Not authoritative.** Interview-question collections, framework comparison blogs, benchmark
posts without a method, and anonymous forum answers, exactly as
`docs/content/content-authoring.md` already requires. Community consensus about which
framework is best is not evidence of anything except consensus, and Unit 6 says so.

**Freshness.** This is the curriculum's shortest-lived material. Hilt's documented component
set, Koin's declaration mechanisms and verification behaviour, and the Android recommendation
itself have all moved within the lifetime of the existing question bank, and the plan records
two places where the current documentation no longer supports wording that was accurate when
it was written. Every framework claim is re-verified at authoring time and again at closure;
the generic reasoning of Units 1 and 2 is stable and does not need it.

## Status

This blueprint is complete as a map, and **no Unit has been authored yet**. E27-01 produced
it together with
[`dependency-injection-units-1-6-plan.md`](dependency-injection-units-1-6-plan.md), which
holds the taxonomy inventory, the semantic Question review, the stable identities, the
practice-routing model, the repository and framework findings, and the authoring handoffs.
Authoring runs E27-02 → E27-07 in Unit order; E27-08 addresses assessment gaps and E27-09
verifies and closes the epic.

The six-Unit structure the merged backlog assumes is **unchanged**, for the reasons recorded
in the plan's [scope confirmation](dependency-injection-units-1-6-plan.md#scope-confirmation).

When authoring reveals that a Lesson boundary was wrong, update this file in the same change
rather than letting the map and the material drift.
