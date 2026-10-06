# Kotlin Language & JVM Fundamentals — Initial Learning Blueprint

## Purpose

This document defines the initial learning program for the `kotlin_language` Topic
(**Kotlin Language & JVM Fundamentals**) before any production Lessons are authored.

It follows `docs/content/learning-content-authoring.md`: map the subject first, establish
dependencies and ownership, classify material as Teach / Bridge / Reference / Exclude,
review the existing assessment bank semantically, and only then write learner-facing
content.

This is a **plan, not production lesson content**.

Home Topic: `kotlin_language`.

Initial scope:

- 4 Learning Units
- 9 planned Lessons
- all 23 existing ACTIVE Kotlin/JVM Subtopics reviewed
- all 25 existing ACTIVE Kotlin/JVM Questions reviewed semantically
- explicit Reference and Exclude decisions
- taxonomy gaps recorded rather than papered over
- assessment gaps recorded for the later question-authoring pass

The initial course is intentionally written for an experienced Android engineer preparing
for Senior Android interviews. It is **not** a beginner Kotlin syntax course.

---

## The Learner This Subject Is Written For

The learner can already read and write ordinary Kotlin and Android code. They do not need
a tutorial on braces, `if`, loops, basic variable declaration, or how to declare a class.

The gap this Topic is intended to close is different: production Kotlin often works because
the compiler is doing more than the developer can currently explain. Senior interviews
probe those semantics.

The learner should finish the initial path able to reason about questions such as:

- Why is this smart cast allowed here but rejected there?
- Which callable is selected from the static type, and which behavior is dispatched at
  runtime?
- Is this property storing a value, recomputing one, or delegating access somewhere else?
- Does this `data class` behave like an immutable value, or only look like one?
- Why is `MutableList<Dog>` not safely usable as `MutableList<Animal>`?
- What does `out`, `in`, or `*` actually permit the caller to do?
- Why does `crossinline` exist, and what exactly changes when a lambda is `noinline`?
- What information survives generic type erasure?
- Is a `Sequence` actually an optimization for this pipeline?
- Which Kotlin guarantees weaken or change when Java and JVM representation enter the
  picture?

The subject should make those questions answerable from a mental model rather than from
memorized keyword definitions.

---

## Editorial Position

### What this course is

A compact course in the Kotlin semantics that repeatedly matter in Android code reviews,
library APIs, debugging, and Senior Android interviews.

The emphasis is:

1. compiler-visible type information;
2. callable and dispatch semantics;
3. object, property, and value-model behavior;
4. safe generic API design;
5. inline/reified mechanics;
6. collection evaluation and mutability;
7. the Java/JVM boundary.

### What this course is not

It is not:

- a Kotlin syntax primer;
- a catalogue of every standard-library function;
- a tour of every language keyword;
- a compiler-internals course;
- a JVM bytecode course;
- a replacement for the existing Coroutines/Flow curriculum;
- a place to re-teach Android architecture or Compose.

---

# Initial Curriculum

## Unit 1 — Types and Callable Semantics

**Purpose:** establish how Kotlin's type system and callable model determine what code is
legal and what function is actually invoked.

**Prerequisites:** ordinary Kotlin syntax only.

### L1.1 — Nullability, Smart Casts and Type Refinement

**Stable Lesson ID:** `lesson_kotlin_nullability_and_smart_casts`

**Objective:** reason from a value's static type and surrounding control flow to what the
compiler can safely permit, rather than treating null-safety operators as syntax recipes.

**Core**

- `T` and `T?` are different types.
- Nullability is enforced by the compiler for Kotlin-declared types.
- Explicit null checks, safe calls, Elvis expressions, and safe casts.
- `is` / `!is` checks and smart casts.
- A smart cast is allowed only while the compiler can prove the checked value cannot change
  incompatibly before use.
- `!!` is an explicit assertion that moves a nullability failure back to runtime; it is not
  the normal way to "fix" nullable code.

**Practical**

- Why a local `val` smart-casts easily while a mutable property, open property, or property
  with a custom getter may not.
- Modeling absence in an API rather than scattering `!!`.
- Using `as?` when a failed cast is an expected branch.
- Nullability through generic and collection values.
- Brief bridge to Java platform types: Java can weaken Kotlin's static null guarantee, but
  the full interop treatment belongs to L4.1.

**Senior**

- Smart-cast eligibility is a data-flow/stability question, not a special case of null
  checks.
- API design can make invalid states harder to represent by moving optionality into the
  type.
- Compiler reasoning should be distinguished from runtime validation: a value may be
  declared non-null in Kotlin and still be violated at an external boundary.

**Primary Subtopics**

- `kotlin_nullability`

**Supporting Subtopics**

- `kotlin_variables`
- `kotlin_properties`
- `kotlin_java_interop`

**Taxonomy note**

Smart casts, type checks, and safe casts have no dedicated Subtopic. They are central to
the Nullability lesson but should be recorded as a taxonomy gap rather than assigned a
made-up ID.

**Existing assessment reviewed**

- `kotlin_nullability_001`
- `kotlin_platform_type_null_check` as a supporting interop check

**Assessment gap**

Current coverage verifies the basic `T` vs `T?` distinction but does not test meaningful
smart-cast reasoning. The later question pass should prioritize realistic cases where a
smart cast succeeds or fails because of mutation, a custom getter, capture, or property
openness.

---

### L1.2 — Functions, Lambdas, Receivers and Extensions

**Stable Lesson ID:** `lesson_kotlin_functions_lambdas_and_extensions`

**Objective:** reason about Kotlin functions as values and APIs, including receiver
semantics and static extension resolution.

**Core**

- Functions can be top-level, members, local functions, or values.
- Function types such as `(T) -> R`.
- Higher-order functions and closures.
- Lambda parameter syntax and the difference between an explicit parameter and a receiver.
- Function types with receiver such as `QueryBuilder.() -> Unit`.
- Extension functions provide callable syntax without modifying the receiver's class.
- Extension receivers are resolved statically from the declared type.
- A real member with the same applicable signature wins over an extension.

**Practical**

- Callback APIs and transformation APIs.
- Builder/DSL-style APIs using receiver lambdas.
- Choosing `T.() -> R` when the block conceptually configures or operates inside one
  receiver, versus `(T) -> R` when explicit data flow is clearer.
- Default and named arguments as API-design tools, without turning the Lesson into a syntax
  inventory.
- A concise scope-function reference based on two questions:
  1. Is the receiver exposed as `this` or `it`?
  2. Does the function return the receiver or the block result?
- Why nested scope functions reduce clarity even though the functions themselves add no
  new capability.

**Senior**

- Receiver syntax changes name resolution and readability; it does not create dynamic
  dispatch.
- Extension functions are useful for API composition but cannot override virtual member
  behavior.
- Captured state should be reasoned about as part of the lambda's behavior and lifetime;
  allocation mechanics belong to the inline/JVM material rather than folklore about every
  lambda allocation.

**Primary Subtopics**

- `kotlin_functions`
- `kotlin_lambdas`
- `kotlin_extension_functions`

**Supporting / Reference Subtopics**

- `kotlin_scope_functions` — **Reference**, included concisely rather than promoted to an
  API catalogue.
- `kotlin_nullability`

**Existing assessment reviewed**

- `kotlin_extension_resolution_static`
- `kotlin_lambda_with_receiver_dsl`
- `kotlin_scope_functions_001`

**Assessment gap**

There is no active question that meaningfully tests ordinary higher-order-function/API
design. A later question should require choosing or reasoning about a receiver lambda,
explicit parameter, closure, or callable boundary rather than merely naming syntax.

---

## Unit 2 — Objects and State Modeling

**Purpose:** explain how Kotlin represents behavior and state before teaching the special
types built on top of that model.

**Prerequisites:** Unit 1.

### L2.1 — Classes, Interfaces, Visibility and Objects

**Stable Lesson ID:** `lesson_kotlin_classes_interfaces_and_objects`

**Objective:** predict inheritance, dispatch, visibility, and singleton/class-level
behavior from Kotlin declarations.

**Core**

- Classes are final by default.
- `open`, `abstract`, and `override` express extension explicitly.
- Member functions use virtual dispatch when overridable.
- Interfaces define contracts and may provide implementations.
- `object` declarations create singleton objects.
- Companion objects are objects associated with a containing class; they do not make every
  member automatically equivalent to a Java `static` member.
- `public`, `private`, `protected`, and `internal`.
- Top-level `private` is file-scoped; `internal` is module-scoped in Kotlin.

**Practical**

- Choosing an interface for a replaceable capability versus inheritance for shared subtype
  behavior.
- Why "classes are open unless restricted" is the wrong Java mental model for Kotlin.
- Using an object when one process-local instance is genuinely the model.
- Using a companion for factories or behavior conceptually attached to a type.
- Module boundaries and why `internal` is useful in multi-module Android projects.
- Avoiding mutable global state simply because `object` makes it convenient.

**Senior**

- Static selection and virtual dispatch are different mechanisms; the extension lesson and
  this lesson should make the contrast explicit.
- `internal` is a Kotlin visibility contract, not a strong JVM bytecode access boundary;
  the Java/JVM consequences are completed in L4.1.
- Interface default-method bytecode compatibility modes are Reference material, not Core
  interview knowledge for this first pass.

**Primary Subtopics**

- `kotlin_classes`
- `kotlin_interfaces_inheritance`
- `kotlin_objects`
- `kotlin_visibility`

**Supporting Subtopics**

- `jvm_fundamentals`
- `kotlin_extension_functions`

**Existing assessment reviewed**

- `kotlin_objects_001`
- `object_vs_companion_object_members`
- `kotlin_internal_module_visibility`

**Assessment gap**

The bank currently tests objects and `internal`, but not the basic class model that matters
in senior code review. Future questions should cover at least:

- final-by-default versus `open`;
- overriding and dynamic dispatch;
- interface implementation/conflict reasoning.

---

### L2.2 — Properties, Accessors and Delegation

**Stable Lesson ID:** `lesson_kotlin_properties_and_delegation`

**Objective:** distinguish stored, computed, late-initialized, and delegated properties and
predict the behavior and cost of each.

**Core**

- A Kotlin property is an accessor-based language construct, not automatically a field.
- When a backing field exists and when it does not.
- Custom getters and setters.
- A getter without stored state recomputes on every read.
- Backing properties as an encapsulation pattern.
- `lateinit` represents a non-null mutable property whose initialization is intentionally
  deferred and checked at runtime.
- `by lazy` represents a read-only value computed on first access and retained afterwards.
- Delegated properties route reads/writes through `getValue` / `setValue`.

**Practical**

- Hidden expensive work in a computed getter.
- Exposing a read-only API over private mutable state without confusing read-only with
  immutable.
- `lateinit` versus nullable state versus constructor injection.
- `lazy` for expensive initialization that may never be needed.
- Default `lazy` synchronization and when `PUBLICATION` or `NONE` changes the contract.
- Custom property delegates only when the access behavior is genuinely reusable.

**Senior**

- Property syntax can hide work, synchronization, or failure; API users still need a
  predictable cost model.
- `lazy`'s thread-safety mode is part of correctness, not only performance.
- Delegation is a mechanism for reusable access semantics, not a general reason to hide
  ownership.

**Primary Subtopics**

- `kotlin_properties`
- `kotlin_delegation`

**Supporting Subtopics**

- `kotlin_variables`
- `kotlin_nullability`
- `kotlin_classes`

**Existing assessment reviewed**

- `kotlin_custom_getter_recomputes`
- `kotlin_lazy_thread_safety_mode`

**Assessment gap**

The current bank should be expanded later with questions that require distinguishing:

- `lateinit`, `lazy`, nullable state, and constructor initialization;
- a stored property from a computed property;
- delegated-property behavior from ordinary backing-field behavior.

---

### L2.3 — Data Classes, Equality and Sealed Models

**Stable Lesson ID:** `lesson_kotlin_data_equality_and_sealed_models`

**Objective:** model values and closed alternatives without confusing compiler-generated
convenience with immutability or deep value semantics.

**Core**

- What a data class generates from primary-constructor properties:
  `equals`, `hashCode`, `toString`, `componentN`, and `copy`.
- Properties declared only in the class body are excluded from those generated members.
- `copy()` is shallow.
- `==` is structural equality through null-safe `equals`; `===` is reference identity.
- Sealed classes/interfaces define a controlled direct subtype set.
- Exhaustive `when` over sealed hierarchies.
- A sealed hierarchy models alternatives; a data class models value-bearing instances.
  They solve different problems and are often used together.

**Practical**

- UI states and result/error models.
- Why a `val` property containing a mutable list does not make the object immutable.
- Why two data-class instances can be structurally equal while containing shared mutable
  references.
- How a new sealed subtype intentionally creates compile errors in exhaustive consumers.
- Enum versus sealed as a concise Reference comparison: enum when cases share one uniform
  type/shape; sealed when cases need distinct data or behavior.

**Senior**

- Mutating state that participates in `equals` / `hashCode` can break assumptions in sets,
  maps, caches, and diffing.
- The compiler-generated value-like API does not guarantee deep immutability.
- Closed hierarchies trade extension freedom for explicit evolution of consumers.

**Primary Subtopics**

- `kotlin_data_classes`
- `kotlin_equality`
- `kotlin_sealed_types`

**Supporting Subtopics**

- `kotlin_classes`
- `kotlin_collections`
- `kotlin_objects`

**Existing assessment reviewed**

- `kotlin_data_classes_001`
- `data_class_copy_is_shallow`
- `kotlin_equality_001`
- `kotlin_sealed_types_001`
- `sealed_when_exhaustive_evolution`

**Assessment position**

This is currently the strongest-covered area in the Kotlin bank. The later question pass
should favor Applied scenarios over adding more definition-level Foundation questions.

---

## Unit 3 — Generic APIs and Compiler-Assisted Mechanics

**Purpose:** explain the type-system and compiler mechanisms that make many idiomatic
Kotlin APIs safe and expressive.

**Prerequisites:** Units 1–2.

### L3.1 — Generics, Variance and Type Erasure

**Stable Lesson ID:** `lesson_kotlin_generics_and_variance`

**Objective:** decide which generic operations are type-safe and explain variance from
producer/consumer behavior rather than memorizing `in` and `out`.

**Core**

- Generic types and generic functions.
- Invariance as the safe default.
- Why `MutableList<Dog>` cannot safely become `MutableList<Animal>`.
- Declaration-site covariance with `out`.
- Declaration-site contravariance with `in`.
- Use-site projections where needed.
- Star projections as "the type exists but is unknown here".
- Type arguments are generally erased on the JVM.

**Practical**

- Designing producer and consumer interfaces.
- Reading from `List<*>` / `MutableList<*>` as `Any?` while being unable to add an
  arbitrary value.
- Why read-only collection APIs can expose more variance safely than mutable ones.
- Unchecked casts and why a cast to `List<String>` cannot fully validate every element
  type at runtime.
- Java wildcard interop as a Bridge, not a Java-generics course.

**Senior**

- Variance is a promise about which operations an API exposes.
- Mutability is the reason many useful subtype relationships become unsafe.
- Type erasure constrains runtime checks and serialization/reflection-style APIs.
- Reified parameters solve only the reified type parameter itself; nested generic arguments
  remain subject to erasure.

**Primary Subtopics**

- `kotlin_generics`

**Supporting Subtopics**

- `jvm_fundamentals`
- `kotlin_collections`
- `kotlin_java_interop`

**Existing assessment reviewed**

- `kotlin_star_projection_use_site`
- `generic_in_out_variance_tradeoff`
- `jvm_fundamentals_001` as semantic supporting coverage

**Assessment gap**

Existing questions are good Foundation checks. The later bank expansion should add Applied
API-design scenarios: a producer/consumer interface, a mutable generic abstraction, or a
use-site projection where the correct variance follows from permitted operations.

---

### L3.2 — Inline Functions, Lambda Control Flow and Reified Types

**Stable Lesson ID:** `lesson_kotlin_inline_and_reified_types`

**Objective:** explain what inlining changes about lambda representation/control flow and
why reified type parameters become possible.

**Core**

- Conceptual call-site inlining of an inline function and its inlinable lambdas.
- `noinline`: keep a lambda as an ordinary value so it can be stored or passed where an
  inlinable lambda cannot.
- Non-local returns from inlined lambdas.
- `crossinline`: keep the lambda inlinable but forbid non-local return when invocation may
  occur from another context.
- `reified` type parameters are available only on inline functions.
- Reification permits operations such as `is T`, `as T`, and `T::class` that ordinary
  erased type parameters cannot perform.

**Practical**

- A lambda stored for later -> `noinline`.
- A lambda wrapped inside `Runnable` / another execution context -> `crossinline` when it
  remains inline but cannot return from the caller.
- Reified parser/factory APIs that otherwise need an explicit `Class` / `KClass` token.
- Do not use `inline` as a default modifier on every small function.

**Senior**

- Inlining trades call/lambda overhead against generated code size.
- Public/protected inline functions become part of the caller's compiled code and therefore
  have stronger public-API/binary constraints.
- `@PublishedApi` exists for the specific problem of exposing an internal declaration to
  public inline implementation.
- Reification does not make all nested generic type arguments runtime-checkable.

**Primary Subtopics**

- `kotlin_inline_functions`
- `kotlin_reified_types`

**Supporting Subtopics**

- `kotlin_lambdas`
- `kotlin_generics`
- `jvm_fundamentals`

**Existing assessment reviewed**

- `kotlin_crossinline_non_local_return`
- `noinline_vs_crossinline_lambda`
- `kotlin_reified_types_001`

**Assessment position**

Existing coverage is already technically useful. Expansion should move toward Applied and
Senior API reasoning rather than more keyword-definition questions.

---

### L3.3 — Collections, Mutability and Sequences

**Stable Lesson ID:** `lesson_kotlin_collections_and_sequences`

**Objective:** choose collection representation and evaluation strategy based on ownership,
mutability, pipeline cost, and short-circuit behavior.

**Core**

- Kotlin's read-only collection interfaces do not guarantee immutable underlying objects.
- Mutable aliases can still change an object observed through `List`, `Set`, or `Map`.
- Eager `Iterable` collection operations materialize intermediate results where applicable.
- `Sequence` performs lazy pipeline evaluation when possible.
- Intermediate versus terminal sequence operations.
- Sequences generally support repeated iteration, but specific implementations may be
  constrained to one pass.
- Laziness is a trade-off, not an automatic optimization.

**Practical**

- Returning a stable snapshot versus returning a read-only view of shared mutable data.
- `map` / `filter` chains on ordinary collections versus `asSequence`.
- Why a small one-step transformation can be faster as an ordinary collection pipeline.
- Where lazy processing helps: longer pipelines, large inputs, short-circuiting, avoiding
  intermediate collections.
- The once-only `generateSequence { ... }` overload as an important edge case, not a rule
  about every sequence.

**Senior**

- Aliasing and mutability are API-ownership concerns, not merely collection syntax.
- Sequence overhead and allocation savings must be reasoned about from the actual workload.
- "Read-only" and "immutable" should never be used interchangeably in an interview answer.

**Primary Subtopics**

- `kotlin_collections`
- `kotlin_sequences`

**Supporting Subtopics**

- `kotlin_generics`
- `kotlin_data_classes`
- `jvm_fundamentals`

**Existing assessment reviewed**

- `kotlin_readonly_list_not_immutable`
- `kotlin_sequence_constrained_once`
- `sequence_intermediate_allocation_tradeoff`

**Assessment gap**

Add Applied questions around snapshot/copy ownership and choosing eager versus lazy
processing from explicit workload constraints. Do not add more Sequence trivia unless the
fact changes a realistic engineering decision.

---

## Unit 4 — Kotlin at the Java/JVM Boundary

**Purpose:** integrate the language model with the runtime and interoperability constraints
that matter specifically to Android/JVM engineers.

**Prerequisites:** Units 1–3.

This Unit initially contains one integrated Lesson. Splitting "Java interop" from "JVM
fundamentals" produced duplicated treatment of platform types, erasure, visibility,
companions, and generated API shape. They belong together for the first pass. Future
expansion can add a second Lesson if value classes, annotations, or binary-compatibility
material earns enough interview weight.

### L4.1 — Kotlin at the Java/JVM Boundary

**Stable Lesson ID:** `lesson_kotlin_java_jvm_boundary`

**Objective:** explain which Kotlin source-level guarantees survive, weaken, or change when
code is consumed from Java or represented on the JVM.

**Core**

- Java declarations without useful nullability information become platform types in
  Kotlin; the compiler cannot provide the same static guarantee it provides for
  Kotlin-declared `T` / `T?`.
- Assigning a platform value to a non-null Kotlin type may insert a runtime check.
- Java can pass `null` into Kotlin APIs in ways Kotlin source would reject; Kotlin emits
  runtime checks for public non-null parameters.
- Kotlin has no checked exceptions.
- A Kotlin function does not normally expose a Java checked `throws` declaration;
  `@Throws` changes the Java-facing signature when needed.
- JVM generic type erasure explains why ordinary `T` is not a runtime type token.
- Kotlin source visibility and JVM representation are not identical; notably `internal`
  is enforced by Kotlin's module semantics rather than a dedicated JVM visibility level.
- Companion/object members are not automatically Java statics simply because Kotlin can
  call them through a class/object-oriented syntax.

**Practical**

- Designing a Kotlin API that will also be called from Java.
- Platform-type handling at a Java library boundary.
- `@Throws` when a Java caller needs a declared checked exception.
- Java SAM conversion and Kotlin lambdas.
- Top-level Kotlin functions/properties and their generated Java-facing file-class surface.
- Selective use of JVM interop annotations such as `@JvmStatic`, `@JvmField`,
  `@JvmOverloads`, and `@JvmName` when the Java API genuinely matters.
- Default/named Kotlin arguments do not automatically produce every Java overload.

**Senior**

- Source-language convenience and binary/public API shape are different concerns.
- Primitive versus boxed representation can change across nullable/generic boundaries;
  teach the consequence, not bytecode trivia.
- Interface default-method compatibility and JVM-target settings are version/build
  concerns worth recognizing but not memorizing in detail.
- Public inline APIs add an additional binary-compatibility dimension because callers may
  contain inlined implementation.
- A Senior answer should be able to state where a guarantee is compile-time Kotlin,
  runtime JVM, or Java-interoperability behavior.

**Primary Subtopics**

- `kotlin_java_interop`
- `jvm_fundamentals`
- `kotlin_exceptions`

**Supporting Subtopics**

- `kotlin_nullability`
- `kotlin_visibility`
- `kotlin_objects`
- `kotlin_generics`
- `kotlin_functions`
- `kotlin_inline_functions`

**Existing assessment reviewed**

Direct:

- `kotlin_platform_type_null_check`
- `kotlin_no_checked_exceptions_interop`
- `jvm_fundamentals_001`

Reinforcing questions whose full home is earlier:

- `kotlin_internal_module_visibility`
- `object_vs_companion_object_members`
- `kotlin_reified_types_001`

**Assessment gap**

The present bank barely tests Java-facing API design. The later expansion should prioritize:

- SAM conversion / function-interface boundaries;
- companion/object versus Java static exposure;
- top-level/default-argument Java API shape;
- nullable/generic boxing or another JVM-representation scenario only where it changes a
  real behavior;
- an Applied question that combines platform types with an API-boundary decision.

---

# Unit Order and Why It Matters

1. **Types and Callable Semantics**
2. **Objects and State Modeling**
3. **Generic APIs and Compiler-Assisted Mechanics**
4. **Kotlin at the Java/JVM Boundary**

The dependency chain is deliberate.

- **Nullability before everything else.** Kotlin's strongest everyday distinction from
  Java is that the compiler reasons about what values may exist. Smart casts also establish
  the broader idea that Kotlin uses control-flow information to refine types.
- **Callable semantics before DSLs, inline, or collections.** Lambdas, receiver functions,
  and extensions are prerequisites for understanding Kotlin APIs rather than features to
  memorize later.
- **Ordinary classes/properties before data/sealed specializations.** A data class is easier
  to understand when the learner already knows what an ordinary class/property does and
  can therefore see exactly what the compiler is generating.
- **Properties before value models.** `copy()` and `val` are routinely mistaken for
  immutability; property/ownership semantics make that misconception easier to dismantle.
- **Generics before reified types.** `reified` is an answer to a problem created by ordinary
  erased generics. Teaching the modifier first reduces it to magic syntax.
- **Generics before collections.** The most important collection questions—read-only versus
  mutable APIs, star projections, aliasing—are generic API questions in concrete form.
- **The Java/JVM boundary last.** It synthesizes earlier topics: nullability becomes
  platform types, visibility becomes bytecode/API shape, generics become erasure, objects
  become Java static interop, and inline functions become binary/API considerations.
  Placed first, the Unit becomes disconnected JVM trivia.

---

# Existing Taxonomy: Editorial Decision for Every Subtopic

| Subtopic | Decision | Home / treatment |
| --- | --- | --- |
| `kotlin_variables` | Reference | Basic `val`/`var` assumed; mutability revisited where it affects properties and aliasing. |
| `kotlin_nullability` | Teach | L1.1 |
| `kotlin_functions` | Teach | L1.2 |
| `kotlin_extension_functions` | Teach | L1.2 |
| `kotlin_classes` | Teach | L2.1 |
| `kotlin_data_classes` | Teach | L2.3 |
| `kotlin_objects` | Teach | L2.1; Java-facing consequences reinforced in L4.1 |
| `kotlin_sealed_types` | Teach | L2.3 |
| `kotlin_interfaces_inheritance` | Teach | L2.1 |
| `kotlin_visibility` | Teach | L2.1; JVM consequence reinforced in L4.1 |
| `kotlin_properties` | Teach | L2.2 |
| `kotlin_delegation` | Teach | L2.2 |
| `kotlin_generics` | Teach | L3.1 |
| `kotlin_lambdas` | Teach | L1.2; inline consequences reinforced in L3.2 |
| `kotlin_inline_functions` | Teach | L3.2 |
| `kotlin_reified_types` | Teach | L3.2 |
| `kotlin_collections` | Teach | L3.3 |
| `kotlin_sequences` | Teach | L3.3 |
| `kotlin_equality` | Teach | L2.3 |
| `kotlin_exceptions` | Teach | L4.1, bounded to Kotlin semantics and Java-facing consequences |
| `kotlin_scope_functions` | Reference | Concise decision framework in L1.2; no standalone Lesson |
| `kotlin_java_interop` | Teach | L4.1 |
| `jvm_fundamentals` | Teach | L4.1; erasure bridged earlier in L3.1/L3.2 |

No existing ACTIVE Subtopic is silently omitted.

---

# Complete Subject Map Beyond the Existing Taxonomy

The Topic taxonomy is intentionally broad but does not name every concept the learning path
needs. The following decisions make those omissions explicit.

## Teach inside the initial 9 Lessons

These concepts are important enough to teach now even though some have no exact Subtopic:

- smart casts and their stability requirements;
- `is`, `!is`, `as`, and `as?` as type-refinement/cast tools;
- function types with receiver;
- closure/captured-state reasoning at a conceptual level;
- class finality and virtual dispatch;
- `lateinit` versus `lazy`;
- enum-versus-sealed decision at comparison depth;
- Java SAM conversion;
- generated Java-facing surface for top-level/default-argument/companion APIs;
- selected JVM annotations when they materially change Java usability.

## Reference / Future Expansion Candidates

These are useful but should not displace higher-signal initial material:

1. **Value classes / inline value classes.**
   High-quality future addition. Relevant to domain modeling and JVM boxing/interoperability,
   but not worth squeezing into the first nine Lessons.
2. **Enum classes in depth.**
   The initial course needs only the sealed comparison.
3. **Type aliases.**
   Useful naming tool with little independent interview signal.
4. **Operator and infix conventions.**
   Worth recognizing; rarely central to Senior Android reasoning.
5. **Class delegation (`class X : Interface by delegate`).**
   Distinct from delegated properties; include only a concise mention initially.
6. **Reflection / callable references beyond ordinary `::` usage.**
   Important in frameworks, but lower signal than the type-system and interop model.
7. **Annotations and use-site targets in depth.**
   Android libraries make these relevant, but the full topic fits better as a later
   extension once the Java/JVM boundary is established.
8. **Contracts and `callsInPlace`.**
   Useful for understanding some compiler analysis, but too advanced for the initial path.
9. **Definitely-non-null generic types (`T & Any`).**
   Reference-level until concrete interop needs justify promotion.
10. **JVM default-method compatibility modes.**
    Build/binary-compatibility reference, not memorization material.
11. **JVM records.**
    Reference unless a target role/library makes them directly relevant.
12. **Primitive arrays / boxing matrix.**
    Teach only the practical consequence initially.

## Exclude from the initial learning program

- JVM bytecode instruction-by-instruction tutorials.
- Compiler IR/back-end architecture.
- Exact synthetic class names generated for lambdas or top-level files as trivia.
- Exhaustive standard-library API lists.
- Exhaustive scope-function recipes.
- Exhaustive `@Jvm*` annotation catalogues.
- Obsolete Kotlin/JVM compatibility history unless a current API decision depends on it.
- Reflection implementation internals.
- Full Java-generics instruction; only the interop consequences Kotlin engineers need.
- DSL design as a separate subject beyond receiver lambdas; `@DslMarker` can be future
  Reference material.
- Experimental/new language features solely because they are novel.

---

# Taxonomy Gaps

The blueprint should not invent Subtopic IDs. These are the clearest gaps if the assessment
taxonomy is expanded later.

## High-value gaps

### Smart casts and type refinement

Current home: nearest fit is `kotlin_nullability`, but smart casts also apply to non-null
type checks and inheritance.

Why a separate Subtopic may eventually help: it supports multiple Senior-relevant questions
about data-flow analysis, mutation, custom getters, and safe casts without misclassifying
all of them as nullability.

### Value classes

No exact current Subtopic.

Why it may deserve future promotion: Kotlin/Android libraries use value-like wrappers and
the JVM representation/interoperability trade-offs are interview-relevant.

### JVM-facing API design / annotations

Current home: `kotlin_java_interop` + `jvm_fundamentals`.

This is adequate for the initial path, but a larger future bank may benefit from a more
specific subtopic if questions accumulate around `@JvmStatic`, `@JvmField`,
`@JvmOverloads`, `@JvmName`, use-site targets, and generated Java API shape.

## Gaps that do not require immediate taxonomy changes

- function types with receiver -> sufficiently covered by `kotlin_lambdas`;
- SAM conversion -> sufficiently covered by `kotlin_java_interop` / `kotlin_lambdas`;
- `lateinit` -> sufficiently covered by `kotlin_properties`;
- class delegation -> sufficiently covered by broad `kotlin_delegation` for now;
- enum classes -> comparison can live under sealed/classes until the bank becomes large
  enough to justify its own identity.

---

# Semantic Review of the Existing 25 ACTIVE Questions

This table is a **review snapshot**, not production Lesson-to-question metadata. Production
mapping remains through stable Subtopic IDs.

| Existing Question | Best learning home | Review |
| --- | --- | --- |
| `kotlin_nullability_001` | L1.1 | Good basic Foundation anchor; too shallow to cover smart-cast reasoning. |
| `kotlin_data_classes_001` | L2.3 | Good generated-member Foundation check. |
| `kotlin_objects_001` | L2.1 | Good object-declaration anchor; keep singleton lifetime/process wording bounded. |
| `kotlin_sealed_types_001` | L2.3 | Good closed-set/exhaustiveness anchor. |
| `kotlin_star_projection_use_site` | L3.1 | Strong conceptual question; directly tests what unknown generic type permits. |
| `kotlin_reified_types_001` | L3.2 | Good explanation of why reification works. |
| `kotlin_equality_001` | L2.3 | Necessary Foundation distinction. |
| `kotlin_scope_functions_001` | L1.2 Reference | Valid but lower priority than the callable model itself. |
| `kotlin_sequence_constrained_once` | L3.3 | Accurate edge case; should remain an edge case, not define the Sequence lesson. |
| `jvm_fundamentals_001` | L4.1, bridged in L3.1 | Good erasure anchor; current JVM coverage is otherwise very thin. |
| `kotlin_lazy_thread_safety_mode` | L2.2 | Strong practical property/delegation behavior. |
| `kotlin_crossinline_non_local_return` | L3.2 | Good causal question rather than syntax recall. |
| `kotlin_extension_resolution_static` | L1.2 | Strong static-vs-runtime dispatch check. |
| `kotlin_platform_type_null_check` | L4.1 | Strong interop boundary behavior. |
| `kotlin_readonly_list_not_immutable` | L3.3 | Important practical distinction. |
| `kotlin_custom_getter_recomputes` | L2.2 | Good property-cost/behavior question. |
| `data_class_copy_is_shallow` | L2.3 | Strong practical correction to a common misconception. |
| `object_vs_companion_object_members` | L2.1 | Useful class-level API distinction; reinforce Java consequences later. |
| `generic_in_out_variance_tradeoff` | L3.1 | Good producer/consumer Foundation coverage. |
| `noinline_vs_crossinline_lambda` | L3.2 | Good Applied question; one of the bank's better depth examples. |
| `sequence_intermediate_allocation_tradeoff` | L3.3 | Good Applied performance reasoning; avoid turning its conclusion into a universal rule. |
| `sealed_when_exhaustive_evolution` | L2.3 | Good evolution trade-off rather than definition recall. |
| `kotlin_no_checked_exceptions_interop` | L4.1 | Strong Java-facing exception question. |
| `kotlin_lambda_with_receiver_dsl` | L1.2 | Good receiver-function Foundation check. |
| `kotlin_internal_module_visibility` | L2.1 + L4.1 reinforcement | Useful because it distinguishes Kotlin module visibility from JVM representation. |

---

# Assessment State and Expansion Priorities

## Current depth

The current Kotlin/JVM bank has:

- 25 ACTIVE questions
- 23 `FOUNDATION`
- 2 `APPLIED`
- 0 `ADVANCED`

That does **not** mean the expansion should force an arbitrary level distribution. Question
levels must still be classified by the minimum reasoning actually required.

It does mean that the next authoring pass should deliberately seek **realistic scenarios
that naturally require Applied or Advanced reasoning**, instead of writing more direct
contract-recall questions.

## Highest-priority assessment gaps by Lesson

### L1.1 — Nullability and smart casts

Add scenarios around:

- a smart cast rejected because the property may change;
- a custom getter or mutable capture invalidating the proof;
- choosing `as?` / explicit nullable modeling over a runtime assertion.

### L1.2 — Functions, lambdas, receivers, extensions

Add scenarios around:

- receiver lambda versus explicit parameter API design;
- closure/captured-state behavior;
- extension/member resolution in realistic code.

### L2.1 — Classes, interfaces, visibility, objects

Highest gap in the current bank.

Add scenarios around:

- final-by-default / `open`;
- virtual overriding;
- interface implementation or conflicting default behavior;
- visibility across Kotlin modules.

### L2.2 — Properties and delegation

Add scenarios around:

- `lateinit` versus `lazy` versus nullable state;
- backing/computed properties;
- delegated getter/setter behavior.

### L2.3 — Data/equality/sealed

Coverage is already strong. Prefer Applied scenarios such as:

- mutation of equality/hash participants;
- choosing sealed versus enum versus open hierarchy;
- mutable references shared through data-class copy.

### L3.1 — Generics

Add Applied API-design questions where the candidate must derive variance from permitted
operations rather than recognize `in` / `out`.

### L3.2 — Inline/reified

Coverage is strong. A future Advanced candidate could combine:

- public inline API constraints;
- reified checking;
- erased nested generic arguments;

but only if the stem can make all three necessary rather than merely obscure.

### L3.3 — Collections/sequences

Add scenarios around:

- snapshot copy versus read-only alias;
- eager versus lazy pipeline from concrete workload constraints;
- short-circuiting.

### L4.1 — Java/JVM boundary

Second-highest gap.

Add scenarios around:

- Java SAM interop;
- default arguments / generated overloads;
- companion/object static exposure;
- top-level Java-facing API shape;
- platform nullability at a library boundary;
- one JVM representation question where boxing or generated surface changes actual
  behavior.

---

# Stable Identities for Run 2

## Units

| Order | Stable Unit ID | Title |
| ---: | --- | --- |
| 1 | `unit_kotlin_types_and_callables` | Types and Callable Semantics |
| 2 | `unit_kotlin_objects_and_state` | Objects and State Modeling |
| 3 | `unit_kotlin_generic_api_mechanics` | Generic APIs and Compiler-Assisted Mechanics |
| 4 | `unit_kotlin_java_jvm_boundary` | Kotlin at the Java/JVM Boundary |

## Lessons

| Order | Stable Lesson ID | Title |
| ---: | --- | --- |
| 1 | `lesson_kotlin_nullability_and_smart_casts` | Nullability, Smart Casts and Type Refinement |
| 2 | `lesson_kotlin_functions_lambdas_and_extensions` | Functions, Lambdas, Receivers and Extensions |
| 3 | `lesson_kotlin_classes_interfaces_and_objects` | Classes, Interfaces, Visibility and Objects |
| 4 | `lesson_kotlin_properties_and_delegation` | Properties, Accessors and Delegation |
| 5 | `lesson_kotlin_data_equality_and_sealed_models` | Data Classes, Equality and Sealed Models |
| 6 | `lesson_kotlin_generics_and_variance` | Generics, Variance and Type Erasure |
| 7 | `lesson_kotlin_inline_and_reified_types` | Inline Functions, Lambda Control Flow and Reified Types |
| 8 | `lesson_kotlin_collections_and_sequences` | Collections, Mutability and Sequences |
| 9 | `lesson_kotlin_java_jvm_boundary` | Kotlin at the Java/JVM Boundary |

These IDs should be treated as the initial identity proposal. If repository validation
reveals an actual identity collision, change the colliding proposal before production
content is authored; otherwise keep them stable through the authoring run.

---

# Authoritative Source Families for Run 2

Lesson authors should ground claims primarily in current official Kotlin documentation.

Likely source families:

- Null safety
- Type checks and casts
- Functions
- Lambdas and higher-order functions
- Extensions
- Scope functions
- Classes and inheritance
- Interfaces
- Visibility modifiers
- Object declarations and companion objects
- Properties
- Delegated properties
- Data classes
- Equality
- Sealed classes and interfaces
- Generics
- Inline functions
- Collections
- Sequences
- Exception handling
- Calling Java from Kotlin
- Calling Kotlin from Java
- Kotlin/JVM compiler options or JVM-specific API documentation where the claim is
  specifically about generated JVM behavior

Android documentation should be used only where an Android-specific example needs an
Android contract. Kotlin language semantics should not be sourced from interview blogs or
Android-specific secondary explanations when the Kotlin documentation states the contract
directly.

Version-sensitive JVM behavior should be re-checked when authoring rather than copied from
memory.

---

# Run 2 Authoring Contract

The multi-agent Work run should treat this blueprint as the frozen curriculum unless
authoring reveals a genuine structural defect.

Recommended workflow for each Lesson:

1. **Author**
   - read this Lesson's objective, Core / Practical / Senior boundaries, Subtopics, and
     assessment notes;
   - read the relevant ACTIVE questions semantically;
   - verify claims against authoritative current sources;
   - write project-agnostic learner-facing content;
   - do not coach the wording of existing quiz answers.

2. **Independent technical/editorial reviewer**
   - verify every technical claim;
   - check that the Lesson actually teaches the reasoning promised by the blueprint;
   - check Core / Practical / Senior depth;
   - look for documentation paraphrase, curriculum narration, filler, over-broad claims,
     and unnecessary jargon;
   - check that mapped ACTIVE questions are answerable from understanding, not memorized
     phrases;
   - identify missing examples or misleading simplifications.

3. **Editor / correction pass**
   - accept ordinary reviewer corrections unless there is a substantive technical or
     curricular disagreement;
   - keep the strongest direct explanation;
   - re-verify changed technical claims;
   - preserve the blueprint boundary instead of expanding the Lesson opportunistically.

4. **After all nine Lessons are stable**
   - perform the dedicated assessment-gap pass described above;
   - author new questions under the existing question-authoring contract;
   - classify levels honestly after stems/options are stable;
   - prefer scenarios that create genuine Applied/Advanced reasoning;
   - regenerate learning/question coverage and run normal validation.

## Important Run 2 constraints

- Do not turn Reference topics into extra Lessons without explicitly revising this blueprint.
- Do not add a Lesson merely because an ACTIVE Subtopic exists.
- Do not mirror the question bank one-to-one.
- Do not write a "Kotlin tricks" course.
- Do not promote bytecode trivia to Senior depth.
- Do not force every Lesson to contain an artificial Senior section.
- Do not force a target Foundation/Applied/Advanced percentage.
- Do not let current questions limit the subject; the final course must also prepare the
  learner for questions that do not exist yet.
- Keep each Lesson within roughly one coherent 5–10 minute reading unit.

---

# Decision Summary

The initial Kotlin/JVM learning path should ship **9 Lessons**, not 8 and not 10.

Nine is the smallest count that keeps the major mental models separate:

1. Nullability / compiler type refinement
2. Functions / lambdas / receiver and extension semantics
3. Class / interface / visibility / object semantics
4. Property / delegation semantics
5. Value and closed-state modeling
6. Generic API safety
7. Inline / reified mechanics
8. Collection mutability and evaluation
9. Java/JVM boundary behavior

The course deliberately does **not** spend a full Lesson on `val`/`var`, scope functions,
operator syntax, type aliases, or other lower-signal language features.

The main assessment problem is not lack of Foundation questions. It is lack of realistic
Applied and Advanced Kotlin reasoning, especially around smart casts, class/interface
semantics, property initialization, generic API design, and Java/JVM API boundaries.

That should be the center of the later question-bank expansion.
