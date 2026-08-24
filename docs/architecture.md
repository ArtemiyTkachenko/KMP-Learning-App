# Architecture Notes

## Application Composition

Shared application dependencies are currently assembled manually at the shared
application composition root, `App()`, and passed into the application shell as
an explicit parameter object.

The dependency graph is intentionally small, so explicit constructor and factory
injection is preferred over a dependency-injection framework. This keeps
dependencies visible at call sites and avoids adding framework lifecycle,
configuration, and testing overhead before there are repositories, persistence,
domain services, or other dependencies to coordinate.

Koin is the intended dependency-injection framework if future work makes manual
wiring materially cumbersome. Examples that would justify introducing it include
multiple repositories, platform-specific persistence implementations, domain
services shared by several ViewModels, or test replacement wiring that becomes
repetitive.

ViewModel factories are supplied from the composition root, but actual ViewModel
creation remains inside the Navigation 3 entry's `viewModel { ... }` call. That
keeps construction policy centralized while preserving Navigation 3 entry-scoped
ViewModel ownership: each back-stack entry receives its own ViewModelStore, and
the ViewModel is cleared when that entry is removed.

## Curriculum Content Model

The curriculum content contract lives in shared `commonMain` code as immutable
Kotlin models with flat Topic, Subtopic, and Question collections linked by
stable string IDs. The flat shape is intentional: it keeps content identity
independent from display text and avoids coupling the model to a future database
or import format.

Substantive content validation is deferred to E06-05 so a validator can report
multiple authoring errors for a complete curriculum instead of failing object
construction on the first malformed item. Serialization and import mechanics are
deferred to E07, so the model deliberately avoids serialization annotations and
persistence-specific metadata for now.
