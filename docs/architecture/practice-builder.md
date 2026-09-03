# The Practice Builder

The screen that turns a scope into a runnable `AssessmentConfig.Focused`, and how that configuration is persisted. See [practice selection](practice-selection.md) for the selection policies it previews.

## The Practice Builder

Choosing a Topic or Subtopic no longer starts an assessment; it opens a builder
scoped to that stable ID, and the builder produces the `AssessmentConfig.Focused`
that assessment taking then runs. The screen is deliberately four decisions —
length, levels, source, and the scope it was opened from — rather than a general
assessment-settings surface, and every one of them is already answered on arrival:
the defaults are the previous one-tap run, so the flow costs a returning learner
one extra tap and no decisions.

`PracticeBuilderViewModel` owns every rule about what a runnable configuration is,
and the Composable owns none of them. The last-level protection is the clearest
case: an empty level set is representable in the domain and explicitly
non-runnable, so a screen that could reach it would strand the learner on a Start
button that can never work. Enforcing it in the state holder means one
implementation rather than one per control that touches levels.

The builder can be opened on an initial source as well as a scope.
`AppRoute.PracticeBuilderTopic` and `PracticeBuilderSubtopic` carry a typed
`PracticeQuestionSource` defaulting to `ALL`, which keeps the Topic Detail entry
semantically unchanged, and `PracticePreset.toPracticeBuilderRoute` is the single
mapping from a semantic practice intent to that route. One preset-capable route
per scope rather than a route per practice kind: a later shortcut surface reuses
this mapping instead of adding a parallel preset system. An arriving source seeds
the initial state and nothing else — count and levels stay the builder's defaults,
an unsupported source falls back to `ALL` exactly as `selectSource` refuses one,
availability is re-checked normally, the learner can still change it, and nothing
starts on arrival.

Source options carry availability as a property of the *policy*, not of the
learner's content. All four current sources are selectable. The builder reads that through
`AssessmentQuestionSelector.isSourceSupported`, which answers without loading any
content; probing by attempting a selection would read content, and completed
history, once per option just to render a screen. A selector test asserts the two
agree for every source, which is what keeps the duplicated `when` honest.

Choosing `UNSEEN`, `WEAK_AREAS`, or `UNRESOLVED_MISTAKES` changes nothing
structurally: it re-runs the same preflight against completed history, so Start is
enabled only when matching content exists. A supported source with nothing left to
ask is not an unavailable source, and the two states stay separate in the UI for
that reason.

Whether the current configuration has any content is a separate question, and it
is answered *before* Start through `AssessmentQuestionSelector.select` — never
through `AssessmentEngine.start`, which persists an attempt. Checking whether
practice is possible must not create practice as a side effect, so the builder is
given the read-only selection boundary and no repository or engine at all.

The configured run reaches assessment taking as typed route fields — scope ID,
count, levels, source — and never as Questions, answers, or curriculum text. Every
dimension travels because the destination rebuilds the config from the route, and
an absent dimension would silently become its default: practising every level when
the learner asked for one. The level list is normalised to authored order so an
identical setup is an identical back-stack entry. The scope's display name is
resolved from its stable ID on arrival rather than carried, so a renamed Topic
cannot appear under a label frozen into the back stack.

## Persisted practice configuration

Practice levels and source are part of the attempt record, as the nullable
`practice_levels` and `practice_source` columns added in schema v6. They were
deliberately left unpersisted while nothing could vary them — every stored FOCUSED
attempt genuinely was an all-levels `ALL` run — but that stopped being true the
moment the Practice Builder could start, say, an ADVANCED-only run: history would
have described an attempt the learner never made, and retake, which re-runs the
reconstructed config, would have widened the repeat back across the whole scope.

`MIGRATION_5_6` is a pure add of two nullable columns. Nothing is backfilled,
because a literal level list on a historical row would claim the learner chose
something they were never offered; instead a null on a FOCUSED row reconstructs as
the all-levels `ALL` semantics it always had, in the mapper, in one place. MIXED
rows keep writing null, since Mixed has no level or source dimension at all.
Levels are stored as comma-separated `QuestionLevel` names in enum order rather
than in a join table: it is a closed three-value set read only alongside its own
attempt, and normalising the order on write is what makes two identical selections
compare equal. `QuestionAttempt` and scoring schema are untouched.
