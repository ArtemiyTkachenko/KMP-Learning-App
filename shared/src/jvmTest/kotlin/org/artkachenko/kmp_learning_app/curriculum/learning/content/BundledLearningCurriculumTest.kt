package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.validation.LearningCurriculumValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the shipped resource rather than a fixture, so the packaged bundle itself is
 * proven to decode and to be coherent against the taxonomy it was authored against.
 *
 * These tests assert structure and stable identity — Unit and Lesson ids, home Topic,
 * authored order, and the primary/supporting mappings later work will consume. Editorial
 * wording is reviewed by a person and deliberately not pinned here, because a test that
 * asserts prose only makes rewriting the prose expensive.
 *
 * The subject here is the shipped document itself. What the loader and the repository make
 * of it is `LearningContentEndToEndTest`'s subject.
 */
internal class BundledLearningCurriculumTest {
    private suspend fun units(): List<LearningUnit> = BundledLearningCurriculumSource.load().units

    private suspend fun unit(id: String): LearningUnit = units().single { it.id == id }

    @Test
    fun bundledLearningCurriculumShipsTheActiveUnitsInBlueprintOrder() = runTest {
        // List position is the ordering contract for a publisher-owned document, so this is
        // asserted unsorted: the state model is taught before the recomposition it drives,
        // and the coroutines path follows the Compose path it was authored after.
        assertEquals(
            listOf(
                "unit_thinking_in_compose",
                "unit_state_and_state_ownership",
                "unit_recomposition",
                "unit_identity_keys_and_stability",
                "unit_derived_state_and_expensive_work",
                "unit_snapshot_fundamentals",
                "unit_production_screen_state_and_udf",
                "unit_observable_state_collection",
                "unit_effect_lifecycle_and_launched_effect",
                "unit_latest_values_and_event_driven_work",
                "unit_cleanup_synchronization_and_producers",
                "unit_production_ui_effects_and_selection",
                "unit_coroutines_and_structured_concurrency",
                "unit_context_dispatchers_and_concurrency",
                "unit_cancellation_failure_and_coordination",
                "unit_flow_fundamentals",
                "unit_flow_composition_timing_and_failure",
                "unit_stateflow_sharedflow_and_hot_streams",
                "unit_architecture_responsibilities_and_boundaries",
                "unit_screen_state_holders_and_ui_state",
                "unit_repositories_and_data_ownership",
                "unit_domain_logic_and_dependency_direction",
                "unit_responsibility_models_mvp_mvvm_mvi",
                "unit_state_events_lifetime_and_selection",
                "unit_dependency_injection_as_object_construction",
                "unit_object_graphs_lifetimes_and_scopes",
                "unit_dagger_compile_time_object_graphs",
                "unit_hilt_android_lifecycle_integration",
                "unit_koin_and_dependency_injection_in_kmp",
            ),
            units().map { it.id },
        )

        assertEquals(
            listOf(
                "Thinking in Compose",
                "State and State Ownership",
                "Recomposition",
                "Identity, Keys, Stability and Immutability",
                "Derived State and Expensive Work",
                "Snapshot Fundamentals",
                "Production Screen State and Unidirectional Data Flow",
                "Observable State Collection and Lifecycle",
                "Effect Lifecycle and LaunchedEffect",
                "Latest-Value Effects and Event-Driven Coroutine Work",
                "Cleanup, External Synchronization and State Producers",
                "Production UI Effects and Mechanism Selection",
                "Coroutine Fundamentals and Structured Concurrency",
                "Coroutine Context, Dispatchers and Concurrent Work",
                "Cancellation, Failure and Coordination",
                "Flow Fundamentals",
                "Flow Composition, Timing and Failure",
                "StateFlow, SharedFlow and Hot Streams",
                "Architecture as Responsibilities and Boundaries",
                "Screen State Holders, ViewModel and UI State",
                "Repositories, Data Ownership and Single Source of Truth",
                "Domain Logic, Use Cases and Dependency Direction",
                "MVP, MVVM and MVI Responsibility Models",
                "State, Events, Lifetime and Architecture Selection",
                "Dependency Injection as Object Construction",
                "Object Graphs, Lifetimes and Scopes",
                "Dagger: Compile-Time Object Graphs",
                "Hilt: Android Lifecycle-Aware Dagger",
                "Koin and Dependency Injection in KMP",
            ),
            units().map { it.title },
        )

        // A Unit's home Topic decides where it is browsed, so it is asserted per Unit
        // rather than as one value: the document now spans four home Topics.
        assertEquals(
            listOf(
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "async_reactive",
                "async_reactive",
                "async_reactive",
                "async_reactive",
                "async_reactive",
                "async_reactive",
                "architecture",
                "architecture",
                "architecture",
                "architecture",
                "architecture",
                "architecture",
                "dependency_injection",
                "dependency_injection",
                "dependency_injection",
                "dependency_injection",
                "dependency_injection",
            ),
            units().map { it.topicId },
        )

        units().forEach { unit ->
            assertEquals(ContentStatus.ACTIVE, unit.status, unit.id)
        }
    }

    @Test
    fun eachUnitCarriesItsLessonsInBlueprintOrder() = runTest {
        assertEquals(
            listOf(
                "lesson_declarative_ui",
                "lesson_composable_execution",
                "lesson_state_down_events_up",
            ),
            unit("unit_thinking_in_compose").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_observable_state",
                "lesson_remember_composition_memory",
                "lesson_remember_saveable",
                "lesson_state_hoisting",
                "lesson_observable_collections",
            ),
            unit("unit_state_and_state_ownership").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_composition_and_recomposition",
                "lesson_recomposition_scopes",
                "lesson_recomposition_cost",
            ),
            unit("unit_recomposition").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_composable_identity",
                "lesson_keys_and_identity_in_lists",
                "lesson_immutability_vs_stability",
                "lesson_stability_and_skipping",
                "lesson_stability_annotations",
            ),
            unit("unit_identity_keys_and_stability").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_remember_key_memoization",
                "lesson_derived_state",
                "lesson_work_outside_composition",
            ),
            unit("unit_derived_state_and_expensive_work").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_snapshot_observation",
                "lesson_snapshot_flow",
            ),
            unit("unit_snapshot_fundamentals").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_classes_of_screen_state",
                "lesson_stateless_screen_content",
                "lesson_screen_state_and_ui_events",
                "lesson_screen_state_owner_boundary",
            ),
            unit("unit_production_screen_state_and_udf").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_external_state_in_compose",
                "lesson_collect_as_state",
                "lesson_collection_lifetime_and_cost",
                "lesson_lifecycle_aware_collection",
            ),
            unit("unit_observable_state_collection").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_why_effects_are_controlled",
                "lesson_launched_effect",
                "lesson_effect_keys_as_dependencies",
                "lesson_effect_key_failures",
            ),
            unit("unit_effect_lifecycle_and_launched_effect").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_remember_updated_state",
                "lesson_remember_coroutine_scope",
                "lesson_who_owns_the_trigger",
            ),
            unit("unit_latest_values_and_event_driven_work").lessons.map { it.id },
        )

        // Unit 11 is ordered by problem rather than by API: release what was registered,
        // publish outward, produce inward, then decide where the adapter belongs.
        assertEquals(
            listOf(
                "lesson_disposable_effect",
                "lesson_side_effect_publication",
                "lesson_produce_state",
                "lesson_flow_adapter_or_compose_producer",
            ),
            unit("unit_cleanup_synchronization_and_producers").lessons.map { it.id },
        )

        // Unit 12 is the synthesis: the decision first, then the state-versus-occurrence
        // distinction it exposes, then the delivery question the Compose boundary cannot
        // answer on its own.
        assertEquals(
            listOf(
                "lesson_choosing_a_compose_mechanism",
                "lesson_transient_ui_effects",
                "lesson_transient_effect_delivery",
            ),
            unit("unit_production_ui_effects_and_selection").lessons.map { it.id },
        )

        // The coroutines Unit reads as one argument, so its order is the argument:
        // suspension, then how work starts, then its lifetime, then its owner, then the
        // guarantees ownership and hierarchy make together.
        assertEquals(
            listOf(
                "lesson_suspension_and_blocking",
                "lesson_coroutine_builders",
                "lesson_job_and_parent_child",
                "lesson_coroutine_scope_ownership",
                "lesson_structured_concurrency",
            ),
            unit("unit_coroutines_and_structured_concurrency").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_coroutine_context",
                "lesson_dispatchers",
                "lesson_with_context_and_main_safety",
                "lesson_sequential_and_concurrent_work",
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.id },
        )

        // Unit 3 escalates one argument: whether the work stops, what happens while it
        // stops, what happens if it fails instead, how supervision changes that, and what
        // concurrent work does to state it shares.
        assertEquals(
            listOf(
                "lesson_cooperative_cancellation",
                "lesson_cancellation_cleanup_and_timeouts",
                "lesson_exception_propagation",
                "lesson_supervision_and_failure_isolation",
                "lesson_shared_state_and_coordination",
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Cancellation Is Cooperative",
                "Cleanup, `NonCancellable` and Timeouts",
                "How a Coroutine Failure Travels",
                "`SupervisorJob`, `supervisorScope` and the Limits of Isolation",
                "Shared Mutable State and Choosing a Coordination Mechanism",
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.title },
        )

        assertEquals(
            listOf(
                "`CoroutineContext` and What Children Inherit",
                "Dispatchers and Where Code Actually Runs",
                "`withContext` and Main-Safety",
                "Sequential by Default, Concurrent on Purpose",
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.title },
        )

        // Unit 4 is a single progression rather than five Flow topics: why the shape
        // exists, what cold means, who owns the collection, where it executes, and how an
        // existing producer becomes a flow.
        assertEquals(
            listOf(
                "lesson_why_flow",
                "lesson_cold_flows",
                "lesson_flow_collection_lifetime",
                "lesson_flow_context_and_flow_on",
                "lesson_flow_builders_and_callback_adapters",
            ),
            unit("unit_flow_fundamentals").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "One Value or Many: Why `Flow` Exists",
                "Cold Flows: Producer, Collector and Operators",
                "Collection Lifetime and Flow Cancellation",
                "Context Preservation and `flowOn`",
                "Flow Builders and Adapting Callback APIs",
            ),
            unit("unit_flow_fundamentals").lessons.map { it.title },
        )

        // Unit 5 is a sequence of decisions taken on the pipeline Unit 4 established:
        // transform one stream, join streams, map values into streams of their own,
        // handle a producer faster than its collector, then handle how collection ends.
        assertEquals(
            listOf(
                "lesson_transforming_and_filtering_flows",
                "lesson_combining_flows",
                "lesson_flattening_flows",
                "lesson_flow_buffering_and_conflation",
                "lesson_flow_failure_and_completion",
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Transforming and Filtering: by Value and by Time",
                "`combine` and `zip`: Current Values or Paired Emissions",
                "Flattening: Should New Input Cancel Old Work?",
                "When the Collector Cannot Keep Up",
                "`catch`, `retry` and `onCompletion`",
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.title },
        )

        // Unit 6 keeps three axes apart in its order: where production lives, then the
        // two retentions that answer what a late subscriber gets, then what moves a cold
        // upstream onto a scope, then the decision the whole Flow half was building to.
        assertEquals(
            listOf(
                "lesson_hot_and_cold_streams",
                "lesson_state_flow",
                "lesson_shared_flow",
                "lesson_sharing_cold_flows",
                "lesson_choosing_a_stream_abstraction",
            ),
            unit("unit_stateflow_sharedflow_and_hot_streams").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Hot and Cold: When Production Happens",
                "`StateFlow`: One Current Value",
                "`SharedFlow`: Replay, Buffering and Subscribers",
                "Sharing Cold Flows with `stateIn` and `shareIn`",
                "Choosing a Stream Abstraction by Delivery Guarantees",
            ),
            unit("unit_stateflow_sharedflow_and_hot_streams").lessons.map { it.title },
        )

        // The architecture Unit's order is the argument it makes: what architecture decides,
        // then how a responsibility is identified, then which way a dependency may point,
        // then whether an abstraction is a boundary, and only then layering as one answer.
        // A layer stack introduced earlier would be a conclusion taught before its reasoning.
        assertEquals(
            listOf(
                "lesson_what_architecture_decides",
                "lesson_responsibility_and_change",
                "lesson_dependency_direction_and_boundaries",
                "lesson_when_an_interface_is_a_boundary",
                "lesson_layers_and_their_cost",
            ),
            unit("unit_architecture_responsibilities_and_boundaries").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "What Architecture Actually Decides",
                "Responsibility, Cohesion and What Changes Together",
                "Which Way May This Dependency Point?",
                "When an Interface Is a Boundary, and When It Is Only Indirection",
                "Layers as One Answer, and What They Cost",
            ),
            unit("unit_architecture_responsibilities_and_boundaries").lessons.map { it.title },
        )

        // The state-holder Unit's order is its progression: what the owner is responsible
        // for, then how long one implementation of that responsibility lives, then what
        // shape its current state takes, then how the UI reads from and writes toward it,
        // and only then which work belongs to its lifetime. Every later Lesson needs the
        // responsibility the first one names, so opening on the ViewModel class would put
        // the implementation before the decision it answers.
        assertEquals(
            listOf(
                "lesson_state_holder_responsibility",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_modelling_ui_state",
                "lesson_state_out_intentions_in",
                "lesson_owner_scoped_work",
            ),
            unit("unit_screen_state_holders_and_ui_state").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "What a Screen State Holder Is Responsible For",
                "The ViewModel Owner: Lifetime Is Not Persistence",
                "Modelling the Current UI State",
                "State Out, Intentions In",
                "Work Whose Lifetime Is the Owner's",
            ),
            unit("unit_screen_state_holders_and_ui_state").lessons.map { it.title },
        )

        // The data-ownership Unit's order is one argument in five steps: what responsibility
        // would justify a repository, then who coordinates several sources, then which copy
        // is authoritative when they disagree, then what API shape the consumer actually
        // needs, and only then which representations and failures may cross the boundary.
        // Authority cannot be chosen before there is coordination to have a conflict in, and
        // the API shape cannot be chosen before the contract has something to expose.
        assertEquals(
            listOf(
                "lesson_what_a_repository_owns",
                "lesson_coordinating_sources",
                "lesson_single_source_of_truth",
                "lesson_observable_or_one_shot_api",
                "lesson_model_and_error_boundaries",
            ),
            unit("unit_repositories_and_data_ownership").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "What a Repository Is Responsible For",
                "Coordinating Local and Remote Sources",
                "Which Source Is Authoritative?",
                "An Observable API, or a One-Shot Read?",
                "Model and Error Boundaries: What May Cross",
            ),
            unit("unit_repositories_and_data_ownership").lessons.map { it.title },
        )

        // The domain Unit's order is one argument in five steps, and the order is the
        // pedagogy: whether a feature earns another layer at all, then whether a single
        // operation earns a type, then what counts as policy and what as detail, then who
        // owns the abstraction between them, and only last the named architecture that
        // formalises the direction. Opening on Clean Architecture would hand the reader the
        // conclusion before any of the decisions it is the conclusion of.
        assertEquals(
            listOf(
                "lesson_when_a_domain_layer_earns_its_place",
                "lesson_use_cases_and_pass_through_cost",
                "lesson_policy_and_framework_detail",
                "lesson_dependency_inversion_in_practice",
                "lesson_clean_architecture_intent",
            ),
            unit("unit_domain_logic_and_dependency_direction").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "When Does Another Layer Earn Its Existence?",
                "Use Cases That Earn Their Place, and Pass-Through Cost",
                "Policy, Framework and Detail",
                "Who Defines the Abstraction?",
                "Clean Architecture: the Dependency Rule, Not the Diagram",
            ),
            unit("unit_domain_logic_and_dependency_direction").lessons.map { it.title },
        )

        // The pattern Unit's order is the argument the unit exists to make. The comparison frame
        // comes first, with no pattern name in it; then the three responsibility models applied to
        // one unchanged screen, in the order in which each is a single change to the one before —
        // MVP holds the view, MVVM takes the reference away, MVI changes how input and transitions
        // are modelled; and only last the classification Lesson, which removes the training wheels.
        // Opening on a pattern name would teach the label before the reasoning the label summarises.
        assertEquals(
            listOf(
                "lesson_one_screen_five_questions",
                "lesson_mvp_view_contract",
                "lesson_mvvm_observed_state",
                "lesson_mvi_intent_and_reduction",
                "lesson_classifying_a_real_architecture",
            ),
            unit("unit_responsibility_models_mvp_mvvm_mvi").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "One Screen, Five Questions",
                "MVP: an Explicit View Contract",
                "MVVM: a UI That Observes State",
                "MVI: Intent, Reduction and One Current State",
                "Classifying What a Real Codebase Actually Does",
            ),
            unit("unit_responsibility_models_mvp_mvvm_mvi").lessons.map { it.title },
        )

        // The synthesis Unit's order is the decision sequence it teaches, and it is the only
        // four-Lesson Unit in the Topic because the subject has four decisions in it. What must
        // the application represent; what guarantee does that representation need; whose lifetime
        // matches the guarantee; and how much structure does the whole feature earn. Each Lesson
        // consumes the previous one's answer, so no other order is available to it.
        assertEquals(
            listOf(
                "lesson_state_or_occurrence",
                "lesson_delivery_guarantees",
                "lesson_choosing_the_owner_by_lifetime",
                "lesson_smallest_sufficient_architecture",
            ),
            unit("unit_state_events_lifetime_and_selection").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Is This State, or Is It Something That Happened?",
                "What Guarantee Does This Occurrence Need?",
                "Choosing an Owner From the Lifetime the Requirement Needs",
                "How Much Architecture Does This Feature Need?",
            ),
            unit("unit_state_events_lifetime_and_selection").lessons.map { it.title },
        )

        // E27-02. The order is the argument the Unit makes, and each Lesson consumes the
        // previous one's answer: where a collaborator can come from, what taking it as a
        // constructor parameter makes true, why supplying it is not the same decision as
        // inverting the dependency, who assembles the graph once classes stop doing it, what
        // a lookup hides, and whether this graph needs anything more than a `main` function.
        assertEquals(
            listOf(
                "lesson_who_constructs_this_object",
                "lesson_a_dependency_should_be_visible",
                "lesson_injected_inverted_or_both",
                "lesson_one_place_that_knows_how_to_build",
                "lesson_asking_for_it_or_being_given_it",
                "lesson_when_wiring_it_yourself_is_enough",
            ),
            unit("unit_dependency_injection_as_object_construction").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Who Constructs This Object?",
                "A Dependency Should Be Visible",
                "Injected, Inverted, or Both?",
                "One Place That Knows How to Build",
                "Asking For It, or Being Given It",
                "When Wiring It Yourself Is the Right Answer",
            ),
            unit("unit_dependency_injection_as_object_construction").lessons.map { it.title },
        )

        assertEquals(
            listOf(
                "lesson_from_one_dependency_to_a_graph",
                "lesson_one_instance_or_a_new_one",
                "lesson_scope_is_a_rule_owner_is_a_lifetime",
                "lesson_runtime_input_is_not_a_dependency",
                "lesson_two_dependencies_of_the_same_type",
                "lesson_when_a_broken_graph_tells_you",
            ),
            unit("unit_object_graphs_lifetimes_and_scopes").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "From One Dependency to a Graph",
                "One Instance, or a New One Each Time?",
                "A Scope Is a Rule; an Owner Is a Lifetime",
                "A Value the Graph Cannot Know",
                "Two Dependencies of the Same Type",
                "When Does a Broken Graph Tell You?",
            ),
            unit("unit_object_graphs_lifetimes_and_scopes").lessons.map { it.title },
        )

        assertEquals(
            listOf(
                "lesson_dagger_constructs_what_it_can_see",
                "lesson_declaring_the_rest_of_the_graph",
                "lesson_which_graph_owns_this_binding",
                "lesson_child_graph_or_separate_graph",
                "lesson_dagger_scopes_and_component_instances",
                "lesson_when_the_type_is_not_the_key",
                "lesson_what_the_dagger_compiler_checked",
            ),
            unit("unit_dagger_compile_time_object_graphs").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "What Dagger Can Construct on Its Own",
                "Declaring the Rest of the Graph",
                "Which Graph Owns This Binding?",
                "A Child Graph, or a Separate Graph?",
                "A Scope Is a Promise the Component Keeps",
                "When the Type Is Not the Key",
                "What the Dagger Compiler Actually Checked",
            ),
            unit("unit_dagger_compile_time_object_graphs").lessons.map { it.title },
        )

        assertEquals(
            listOf(
                "lesson_hilt_is_dagger_with_decisions_made",
                "lesson_which_android_component_owns_this",
                "lesson_when_android_owns_construction",
                "lesson_hilt_viewmodels_and_runtime_input",
                "lesson_which_graph_does_this_binding_join",
                "lesson_hilt_or_hand_written_dagger",
            ),
            unit("unit_hilt_android_lifecycle_integration").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Hilt Is Dagger With the Decisions Already Made",
                "Which Android Component Owns This?",
                "When Android Owns Construction",
                "ViewModels, Their Component, and the Values That Arrive Late",
                "Which Graph Does This Binding Join?",
                "Hilt, or Components You Write Yourself?",
            ),
            unit("unit_hilt_android_lifecycle_integration").lessons.map { it.title },
        )

        assertEquals(
            listOf(
                "lesson_the_koin_container_and_its_modules",
                "lesson_koin_definitions_and_reuse",
                "lesson_koin_scopes_and_their_owners",
                "lesson_resolving_viewmodels_at_the_boundary",
                "lesson_one_graph_across_platforms",
            ),
            unit("unit_koin_and_dependency_injection_in_kmp").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "The Container, and the Modules That Fill It",
                "Definitions, and the Reuse Requirement Behind Them",
                "Scopes, and the Owner That Has to Stay Alive",
                "Resolving a ViewModel at the Boundary",
                "One Graph, Several Platforms",
            ),
            unit("unit_koin_and_dependency_injection_in_kmp").lessons.map { it.title },
        )
    }

    @Test
    fun everyLessonDeclaresThePrimaryConceptItTeaches() = runTest {
        // The primary mapping is the stable bridge into assessment coverage, so it is a
        // contract rather than editorial detail.
        assertEquals(
            listOf(
                listOf("compose_fundamentals"),
                listOf("compose_fundamentals"),
                listOf("compose_udf"),
            ),
            unit("unit_thinking_in_compose").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("compose_state"),
                listOf("compose_state"),
                listOf("compose_state"),
                listOf("compose_state_hoisting"),
                listOf("compose_state"),
            ),
            unit("unit_state_and_state_ownership").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("compose_recomposition"),
                listOf("compose_recomposition"),
                listOf("compose_recomposition"),
            ),
            unit("unit_recomposition").lessons.map { it.primarySubtopicIds },
        )

        // Three Lessons share `compose_stability` at different depths, which the authoring
        // contract allows: a Subtopic is touched by more than one Lesson, and the identity
        // pair is a separate concept from the stability pair.
        assertEquals(
            listOf(
                listOf("compose_identity_keys"),
                listOf("compose_identity_keys"),
                listOf("compose_stability"),
                listOf("compose_stability"),
                listOf("compose_stability"),
            ),
            unit("unit_identity_keys_and_stability").lessons.map { it.primarySubtopicIds },
        )

        // All three Unit 5 Lessons own the same concept, so the Unit practises exactly
        // `compose_derived_state`. The Unit is a decision model rather than three separate
        // subjects, and splitting the mapping would claim coverage the taxonomy does not have.
        assertEquals(
            listOf(
                listOf("compose_derived_state"),
                listOf("compose_derived_state"),
                listOf("compose_derived_state"),
            ),
            unit("unit_derived_state_and_expensive_work").lessons.map { it.primarySubtopicIds },
        )

        // Both Unit 6 Lessons own `compose_snapshot_system`: the Unit is one mental model
        // and its second Lesson applies that model to a non-UI consumer rather than
        // teaching a separate concept.
        assertEquals(
            listOf(
                listOf("compose_snapshot_system"),
                listOf("compose_snapshot_system"),
            ),
            unit("unit_snapshot_fundamentals").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("compose_state_hoisting"),
                listOf("compose_udf"),
                listOf("compose_udf"),
                listOf("compose_state_hoisting"),
            ),
            unit("unit_production_screen_state_and_udf").lessons.map { it.primarySubtopicIds },
        )

        // Unit 8 applies one Compose concept at four depths. Flow, lifecycle and KMP
        // concepts are prerequisites and platform context, so Unit practice remains
        // exactly `compose_state` until E25-08 authors its missing Questions.
        assertEquals(
            List(4) { listOf("compose_state") },
            unit("unit_observable_state_collection").lessons.map { it.primarySubtopicIds },
        )

        // Unit 9 is one ownership-and-lifetime argument at four depths. Only the effect
        // concept becomes practice; its Compose, Kotlin and coroutine prerequisites stay
        // supporting until the Units that own them are practised.
        assertEquals(
            List(4) { listOf("compose_side_effects") },
            unit("unit_effect_lifecycle_and_launched_effect").lessons.map { it.primarySubtopicIds },
        )

        // Unit 10 separates latest-value and trigger ownership, but both are still depths
        // of the one effect concept the taxonomy provides. Its coroutine and lifecycle
        // prerequisites remain supporting, so Unit practice stays on the shared pool.
        assertEquals(
            List(3) { listOf("compose_side_effects") },
            unit("unit_latest_values_and_event_driven_work").lessons.map {
                it.primarySubtopicIds
            },
        )

        // Unit 11 crosses the Compose boundary in three directions and then chooses where
        // an adapter belongs, but the taxonomy still offers one effect concept for all four.
        // The lifecycle, performance, Flow and architecture bridges stay supporting.
        assertEquals(
            List(4) { listOf("compose_side_effects") },
            unit("unit_cleanup_synchronization_and_producers").lessons.map {
                it.primarySubtopicIds
            },
        )

        // Unit 12 decides between the mechanisms rather than teaching another one, and the
        // taxonomy still offers exactly one concept for that. Mapping the delivery Lesson
        // primarily to a stream concept would route E24's Questions into an E25 Unit, so
        // the shared pool stays shared and E25-08 answers it as assessment work.
        assertEquals(
            List(3) { listOf("compose_side_effects") },
            unit("unit_production_ui_effects_and_selection").lessons.map {
                it.primarySubtopicIds
            },
        )

        // Unlike the Compose Units, every Lesson here owns a different concept, and each of
        // the five is a distinct `async_reactive` Subtopic. Unit practice is exactly these
        // five: the cross-Topic bridges the Lessons lean on stay supporting.
        assertEquals(
            listOf(
                listOf("coroutine_fundamentals"),
                listOf("coroutine_builders"),
                listOf("coroutine_jobs"),
                listOf("coroutine_scope"),
                listOf("structured_concurrency"),
            ),
            unit("unit_coroutines_and_structured_concurrency").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("coroutine_context"),
                listOf("coroutine_dispatchers"),
                listOf("coroutine_context_switching"),
                listOf("coroutine_parallelism"),
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.primarySubtopicIds },
        )

        // `coroutine_cancellation` is primary in two Lessons at different depths, which the
        // authoring contract allows: the request and the unwind are separate mental models.
        assertEquals(
            listOf(
                listOf("coroutine_cancellation"),
                listOf("coroutine_cancellation"),
                listOf("coroutine_exceptions"),
                listOf("coroutine_supervision"),
                listOf("coroutine_parallelism"),
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.primarySubtopicIds },
        )

        // `flow_fundamentals` is primary in three of the five Lessons — why the shape
        // exists, what cold means, and how a producer becomes one — so Unit practice here
        // is exactly three concepts rather than five.
        assertEquals(
            listOf(
                listOf("flow_fundamentals"),
                listOf("flow_fundamentals"),
                listOf("flow_collection"),
                listOf("flow_context"),
                listOf("flow_fundamentals"),
            ),
            unit("unit_flow_fundamentals").lessons.map { it.primarySubtopicIds },
        )

        // `flow_operators` is primary in the first three Lessons — filtering, combining
        // and flattening are three depths of one concept — so Unit practice here is three
        // concepts rather than five.
        assertEquals(
            listOf(
                listOf("flow_operators"),
                listOf("flow_operators"),
                listOf("flow_operators"),
                listOf("flow_buffering"),
                listOf("flow_errors"),
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.primarySubtopicIds },
        )

        // `hot_vs_cold_streams` is primary in the first and last Lessons — production
        // lifetime is what the opening Lesson establishes and what the closing decision is
        // organised around — so five Lessons practise four concepts.
        assertEquals(
            listOf(
                listOf("hot_vs_cold_streams"),
                listOf("stateflow"),
                listOf("sharedflow"),
                listOf("flow_sharing"),
                listOf("hot_vs_cold_streams"),
            ),
            unit("unit_stateflow_sharedflow_and_hot_streams").lessons.map { it.primarySubtopicIds },
        )

        // `separation_of_concerns` is primary in the first two Lessons — what architecture
        // decides, then how a responsibility is identified — and the closing Lesson declares
        // two primaries because it teaches layering and proportionality together: a layer
        // that is not weighed against its cost is the misconception the Lesson exists for.
        assertEquals(
            listOf(
                listOf("separation_of_concerns"),
                listOf("separation_of_concerns"),
                listOf("dependency_direction"),
                listOf("interface_boundaries"),
                listOf("layered_architecture", "architecture_tradeoffs"),
            ),
            unit("unit_architecture_responsibilities_and_boundaries").lessons.map { it.primarySubtopicIds },
        )

        // Four of the five state-holder Lessons take `state_ownership`, because ownership of
        // the screen's state is genuinely what each of them decides at a different angle —
        // the responsibility, its lifetime, its shape, and the work bounded by it. Only the
        // contract Lesson declares `unidirectional_data_flow`, which is the concept that
        // Lesson alone teaches thoroughly.
        assertEquals(
            listOf(
                listOf("state_ownership"),
                listOf("state_ownership"),
                listOf("state_ownership"),
                listOf("unidirectional_data_flow"),
                listOf("state_ownership"),
            ),
            unit("unit_screen_state_holders_and_ui_state").lessons.map { it.primarySubtopicIds },
        )

        // `repository_pattern` is primary in three of the five data-ownership Lessons —
        // what a repository owns, how it coordinates sources, and what shape its API takes
        // are three depths of one concept — and the closing Lesson declares two primaries
        // because what may cross a layer boundary and how failure is represented as it
        // crosses are the same decision asked about types and about errors.
        assertEquals(
            listOf(
                listOf("repository_pattern"),
                listOf("repository_pattern"),
                listOf("single_source_of_truth"),
                listOf("repository_pattern"),
                listOf("layered_architecture", "error_modeling"),
            ),
            unit("unit_repositories_and_data_ownership").lessons.map { it.primarySubtopicIds },
        )

        // `use_cases` is primary twice because the layer decision and the class decision are
        // genuinely different questions at two scales, and `dependency_direction` twice
        // because policy-against-detail and who-owns-the-abstraction are two halves of one
        // arrow. The inversion Lesson declares two primaries: which side defines a contract
        // is simultaneously a direction decision and a statement about what makes an
        // abstraction a boundary, which is why the foundations Unit deferred it whole.
        assertEquals(
            listOf(
                listOf("use_cases"),
                listOf("use_cases"),
                listOf("dependency_direction"),
                listOf("dependency_direction", "interface_boundaries"),
                listOf("clean_architecture"),
            ),
            unit("unit_domain_logic_and_dependency_direction").lessons.map { it.primarySubtopicIds },
        )

        // Five Lessons, five distinct primary concepts — the only E26 Unit with no shared primary,
        // because each pattern name is its own Subtopic and the comparison has one of its own.
        // `mvc` is primary in the opening Lesson although it holds no ACTIVE Question: the Lesson
        // owns the origin of the vocabulary and the reason it is contested, and manufacturing
        // coverage by re-mapping it would be the dishonest repair. That is GAP-U5-A in
        // `docs/content/architecture-units-1-6-plan.md`.
        assertEquals(
            listOf(
                listOf("mvc"),
                listOf("mvp"),
                listOf("mvvm"),
                listOf("mvi"),
                listOf("mvvm_vs_mvi"),
            ),
            unit("unit_responsibility_models_mvp_mvvm_mvi").lessons.map { it.primarySubtopicIds },
        )

        // Three of the four Lessons take `state_ownership`, which is also primary across four
        // Lessons of the state-holder Unit — the taxonomy has no finer concept for representing an
        // occurrence, for acknowledgement, or for owner selection by lifetime, and inventing one is
        // a question-bank change rather than an authoring decision. The closing Lesson is the
        // second home of `architecture_tradeoffs`, which holds no ACTIVE Question at all, so the
        // Lesson this epic ends on currently contributes nothing to practice. That is GAP-U6-C in
        // `docs/content/architecture-units-1-6-plan.md`, and the mapping is deliberately not
        // changed to manufacture coverage.
        assertEquals(
            listOf(
                listOf("state_ownership"),
                listOf("state_ownership"),
                listOf("state_ownership"),
                listOf("architecture_tradeoffs"),
            ),
            unit("unit_state_events_lifetime_and_selection").lessons.map { it.primarySubtopicIds },
        )

        // E27-02. Six Lessons, five distinct primary concepts: `di_fundamentals` is primary
        // twice because naming the three ways a collaborator can arrive and separating
        // injection from inversion are both statements about what injection *is*, taught at
        // different depths. Every primary is a `dependency_injection` Subtopic, which is what
        // keeps this Unit from altering any other Topic's practice, and the five together are
        // exactly the concepts Unit practice may resolve.
        assertEquals(
            listOf(
                listOf("di_fundamentals"),
                listOf("constructor_injection"),
                listOf("di_fundamentals"),
                listOf("composition_root"),
                listOf("service_locator_vs_di"),
                listOf("manual_di"),
            ),
            unit("unit_dependency_injection_as_object_construction").lessons.map { it.primarySubtopicIds },
        )

        // E27-03. Six Lessons, two distinct primary concepts, which is deliberate rather than thin:
        // `dependency_graphs` carries the structural reasoning — tracing a graph, routing a value
        // that is not part of one, an edge with two candidates, and when a broken edge is reported —
        // while `di_scopes` carries the two Lessons about instance identity and the owner that bounds
        // it. Promoting any supporting concept to widen the pair would claim practice coverage for
        // material later Units own.
        assertEquals(
            listOf(
                listOf("dependency_graphs"),
                listOf("di_scopes"),
                listOf("di_scopes"),
                listOf("dependency_graphs"),
                listOf("dependency_graphs"),
                listOf("dependency_graphs"),
            ),
            unit("unit_object_graphs_lifetimes_and_scopes").lessons.map { it.primarySubtopicIds },
        )

        // Two Lessons take two primary concepts each, which is what makes them one Lesson rather
        // than two: a declaration form is chosen against the module it is declared in, and a key is
        // narrowed or aggregated by the same key model. `dependency_graphs` is deliberately absent —
        // it is supporting in the validation Lesson, and promoting it would route a Unit 2 Question
        // into this Unit's practice rather than teaching anything more.
        assertEquals(
            listOf(
                listOf("dagger_fundamentals"),
                listOf("dagger_modules", "dagger_bindings"),
                listOf("dagger_components"),
                listOf("dagger_components"),
                listOf("dagger_scopes"),
                listOf("dagger_qualifiers", "dagger_multibindings"),
                listOf("dagger_fundamentals"),
            ),
            unit("unit_dagger_compile_time_object_graphs").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("hilt_fundamentals"),
                listOf("hilt_components"),
                listOf("hilt_fundamentals"),
                listOf("hilt_viewmodels"),
                listOf("hilt_modules"),
                listOf("hilt_vs_dagger"),
            ),
            unit("unit_hilt_android_lifecycle_integration").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("koin_fundamentals"),
                listOf("koin_definitions"),
                listOf("koin_scopes"),
                listOf("koin_viewmodels"),
                listOf("koin_multiplatform"),
            ),
            unit("unit_koin_and_dependency_injection_in_kmp").lessons.map { it.primarySubtopicIds },
        )
    }

    @Test
    fun contextUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("coroutine_jobs", "coroutine_dispatchers", "structured_concurrency"),
                listOf("coroutine_context", "android_main_thread", "main_thread_performance", "anr"),
                listOf(
                    "coroutine_dispatchers",
                    "coroutine_fundamentals",
                    "structured_concurrency",
                    "repository_pattern",
                ),
                listOf("coroutine_builders", "structured_concurrency", "coroutine_dispatchers"),
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun productionScreenStateUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("compose_state", "state_ownership", "viewmodel_lifecycle"),
                listOf("compose_state_hoisting", "compose_previews", "compose_fundamentals"),
                listOf(
                    "compose_state",
                    "unidirectional_data_flow",
                    "kotlin_data_classes",
                    "compose_stability",
                ),
                listOf(
                    "state_ownership",
                    "viewmodel_lifecycle",
                    "kmp_lifecycle_viewmodel",
                    "configuration_changes",
                ),
            ),
            unit("unit_production_screen_state_and_udf").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun observableStateCollectionUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("stateflow", "hot_vs_cold_streams", "compose_snapshot_system"),
                listOf("flow_collection", "stateflow", "compose_recomposition"),
                listOf(
                    "flow_collection",
                    "flow_sharing",
                    "coroutine_cancellation",
                    "lifecycle_coroutines",
                ),
                listOf(
                    "lifecycle_aware_apis",
                    "kmp_lifecycle_viewmodel",
                    "compose_multiplatform",
                    "flow_sharing",
                ),
            ),
            unit("unit_observable_state_collection").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun effectLifecycleUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("compose_fundamentals", "compose_recomposition"),
                listOf(
                    "coroutine_scope",
                    "coroutine_cancellation",
                    "structured_concurrency",
                    "compose_identity_keys",
                ),
                listOf("compose_derived_state", "kotlin_equality", "compose_stability"),
                listOf("compose_identity_keys", "coroutine_cancellation", "kotlin_lambdas"),
            ),
            unit("unit_effect_lifecycle_and_launched_effect").lessons.map {
                it.supportingSubtopicIds
            },
        )
    }

    @Test
    fun effectLifecycleUnitLinksBackToItsComposeAndCoroutinePrerequisites() = runTest {
        val lessons = unit("unit_effect_lifecycle_and_launched_effect").lessons.associateBy { it.id }

        assertEquals(
            listOf("lesson_composable_execution", "lesson_composition_and_recomposition"),
            lessons.getValue("lesson_why_effects_are_controlled").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_composable_identity",
                "lesson_coroutine_scope_ownership",
                "lesson_cooperative_cancellation",
            ),
            lessons.getValue("lesson_launched_effect").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_remember_key_memoization", "lesson_stability_and_skipping"),
            lessons.getValue("lesson_effect_keys_as_dependencies").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_remember_key_memoization"),
            lessons.getValue("lesson_effect_key_failures").relatedLessonIds,
        )
    }

    @Test
    fun latestValueAndEventDrivenUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("compose_state", "compose_recomposition", "kotlin_lambdas"),
                listOf("coroutine_scope", "coroutine_builders", "structured_concurrency"),
                listOf(
                    "coroutine_scope",
                    "viewmodel_lifecycle",
                    "lifecycle_coroutines",
                    "coroutine_cancellation",
                ),
            ),
            unit("unit_latest_values_and_event_driven_work").lessons.map {
                it.supportingSubtopicIds
            },
        )
    }

    @Test
    fun latestValueAndEventDrivenUnitLinksBackToItsEffectAndCoroutinePrerequisites() = runTest {
        val lessons = unit("unit_latest_values_and_event_driven_work").lessons.associateBy { it.id }

        assertEquals(
            listOf("lesson_launched_effect", "lesson_effect_keys_as_dependencies"),
            lessons.getValue("lesson_remember_updated_state").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_coroutine_scope_ownership", "lesson_coroutine_builders"),
            lessons.getValue("lesson_remember_coroutine_scope").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_launched_effect",
                "lesson_coroutine_scope_ownership",
                "lesson_structured_concurrency",
            ),
            lessons.getValue("lesson_who_owns_the_trigger").relatedLessonIds,
        )
    }

    @Test
    fun cleanupAndProducerUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("lifecycle_aware_apis", "memory_leaks", "lifecycle_leaks"),
                listOf("compose_recomposition", "compose_fundamentals", "compose_state"),
                listOf("compose_state", "flow_collection", "coroutine_cancellation"),
                listOf("flow_fundamentals", "hot_vs_cold_streams", "separation_of_concerns"),
            ),
            unit("unit_cleanup_synchronization_and_producers").lessons.map {
                it.supportingSubtopicIds
            },
        )
    }

    @Test
    fun cleanupAndProducerUnitLinksBackToItsEffectStateAndFlowPrerequisites() = runTest {
        val lessons = unit("unit_cleanup_synchronization_and_producers").lessons.associateBy { it.id }

        assertEquals(
            listOf(
                "lesson_effect_keys_as_dependencies",
                "lesson_remember_updated_state",
                "lesson_cancellation_cleanup_and_timeouts",
                "lesson_flow_collection_lifetime",
            ),
            lessons.getValue("lesson_disposable_effect").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_composable_execution",
                "lesson_why_effects_are_controlled",
                "lesson_who_owns_the_trigger",
            ),
            lessons.getValue("lesson_side_effect_publication").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_launched_effect", "lesson_collect_as_state", "lesson_snapshot_flow"),
            lessons.getValue("lesson_produce_state").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_flow_builders_and_callback_adapters",
                "lesson_collect_as_state",
                "lesson_snapshot_flow",
            ),
            lessons.getValue("lesson_flow_adapter_or_compose_producer").relatedLessonIds,
        )
    }

    @Test
    fun mechanismSelectionUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("compose_state", "compose_state_hoisting", "compose_udf"),
                listOf("coroutine_scope", "compose_state", "sharedflow"),
                listOf("sharedflow", "hot_vs_cold_streams", "state_ownership", "stateflow"),
            ),
            unit("unit_production_ui_effects_and_selection").lessons.map {
                it.supportingSubtopicIds
            },
        )
    }

    @Test
    fun theSynthesisLessonLinksBackToEveryLessonItSynthesises() = runTest {
        // The one place in the document where a broad related-Lesson list is intentional:
        // L12.1 is the decision across all five earlier Units of this epic, so every one of
        // their Lessons is a place the mechanics live. Omitting one silently would leave a
        // mechanism the decision reaches with nowhere to read it.
        val expected = listOf(
            "unit_production_screen_state_and_udf",
            "unit_observable_state_collection",
            "unit_effect_lifecycle_and_launched_effect",
            "unit_latest_values_and_event_driven_work",
            "unit_cleanup_synchronization_and_producers",
        ).flatMap { unitId -> unit(unitId).lessons.map { it.id } } +
            listOf("lesson_work_outside_composition", "lesson_snapshot_flow")

        assertEquals(
            expected,
            unit("unit_production_ui_effects_and_selection").lessons
                .single { it.id == "lesson_choosing_a_compose_mechanism" }
                .relatedLessonIds,
        )
    }

    @Test
    fun theDeliveryLessonLinksBackToTheStreamArgumentsItApplies() = runTest {
        val lessons = unit("unit_production_ui_effects_and_selection").lessons.associateBy { it.id }

        assertEquals(
            listOf(
                "lesson_screen_state_and_ui_events",
                "lesson_launched_effect",
                "lesson_remember_coroutine_scope",
                "lesson_who_owns_the_trigger",
            ),
            lessons.getValue("lesson_transient_ui_effects").relatedLessonIds,
        )
        // The delivery argument is E24's, and this Lesson applies it at the Compose boundary
        // rather than restating it, so the two stream Lessons are links instead of prose.
        assertEquals(
            listOf(
                "lesson_transient_ui_effects",
                "lesson_lifecycle_aware_collection",
                "lesson_shared_flow",
                "lesson_choosing_a_stream_abstraction",
            ),
            lessons.getValue("lesson_transient_effect_delivery").relatedLessonIds,
        )
    }

    @Test
    fun productionScreenOwnerBoundaryCompletesTheTwoShippedForwardPointers() = runTest {
        val ownerBoundaryId = "lesson_screen_state_owner_boundary"
        val stateOwnershipUnit = unit("unit_state_and_state_ownership")

        assertTrue(
            ownerBoundaryId in stateOwnershipUnit.lessons
                .single { it.id == "lesson_state_hoisting" }
                .relatedLessonIds,
        )
        assertTrue(
            ownerBoundaryId in stateOwnershipUnit.lessons
                .single { it.id == "lesson_remember_saveable" }
                .relatedLessonIds,
        )
    }

    @Test
    fun cancellationUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // The Kotlin, JVM, performance and hot-stream concepts this Unit leans on are
        // assessed in their own Topics. Promoting any of them would hand Unit practice
        // questions the Unit does not teach — GAP-U3-B in
        // `docs/content/coroutines-flow-units-1-6-plan.md` depends on that staying true,
        // because nothing in any Topic yet assesses coroutine races over shared state.
        assertEquals(
            listOf(
                listOf("coroutine_jobs", "structured_concurrency", "kotlin_exceptions"),
                listOf("kotlin_exceptions", "coroutine_context_switching", "coroutine_jobs"),
                listOf(
                    "coroutine_builders",
                    "coroutine_jobs",
                    "coroutine_cancellation",
                    "error_modeling",
                ),
                listOf(
                    "coroutine_exceptions",
                    "coroutine_context",
                    "structured_concurrency",
                    "coroutine_jobs",
                ),
                listOf(
                    "hot_vs_cold_streams",
                    "jvm_fundamentals",
                    "android_memory_model",
                    "coroutine_dispatchers",
                ),
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun flowUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // The architecture, Kotlin, lifecycle and performance concepts Unit 4 bridges are
        // assessed in their own Topics, and none of them is reachable through an E24
        // primary mapping. GAP-U4-A depends on that staying true: the closest existing
        // assessment of the one-shot-versus-stream decision is
        // `repository_observable_api_shape`, which sits in `architecture`, so promoting
        // `repository_pattern` here would claim Unit practice no `async_reactive`
        // Question provides and would hide the gap E24-08 still has to close.
        assertEquals(
            listOf(
                listOf(
                    "coroutine_fundamentals",
                    "hot_vs_cold_streams",
                    "repository_pattern",
                    "single_source_of_truth",
                ),
                listOf("flow_collection", "flow_operators", "kotlin_sequences"),
                listOf(
                    "coroutine_cancellation",
                    "coroutine_scope",
                    "lifecycle_coroutines",
                    "coroutine_jobs",
                ),
                listOf(
                    "coroutine_context",
                    "coroutine_dispatchers",
                    "coroutine_context_switching",
                    "flow_errors",
                ),
                listOf(
                    "flow_context",
                    "coroutine_cancellation",
                    "flow_buffering",
                    "memory_leaks",
                ),
            ),
            unit("unit_flow_fundamentals").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun compositionUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // `stateflow` is supporting in the combining Lesson because current-value
        // reasoning is relevant there, and `kotlin_equality`, `kotlin_lambdas`,
        // `kotlin_exceptions`, `error_modeling`, `coroutine_cancellation` and
        // `coroutine_parallelism` are all bridges into Topics that assess them
        // themselves. Promoting any of them would hand Unit 5 practice Questions this
        // Unit does not teach — and promoting `stateflow` in particular would claim
        // coverage of material Unit 6 has not shipped yet.
        assertEquals(
            listOf(
                listOf("flow_fundamentals", "kotlin_lambdas", "kotlin_equality"),
                listOf("flow_fundamentals", "stateflow", "flow_collection"),
                listOf("coroutine_cancellation", "coroutine_parallelism", "flow_collection"),
                listOf(
                    "flow_collection",
                    "flow_context",
                    "coroutine_cancellation",
                    "flow_operators",
                ),
                listOf(
                    "kotlin_exceptions",
                    "coroutine_cancellation",
                    "flow_operators",
                    "error_modeling",
                ),
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun hotStreamUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Four of the concepts these Lessons lean on are primary nowhere in the Unit:
        // `kotlin_equality`, which carries the equality-based conflation the state Lesson is
        // built on; `state_ownership`, the architecture bridge; `lifecycle_coroutines`,
        // supporting-only across the whole epic by design; and `flow_buffering`, Unit 5's
        // concept reused for a hot stream. Each is assessed, where it is assessed at all, in
        // terms that have nothing to do with this Unit, so promoting any of them would hand
        // Unit 6 practice it does not teach and would hide GAP-U6-A, GAP-U6-B and GAP-U6-C in
        // `docs/content/coroutines-flow-units-1-6-plan.md`. The remaining entries are Unit 6
        // primaries elsewhere, which is what makes the four Lessons that share them read as
        // one argument rather than four subjects.
        assertEquals(
            listOf(
                listOf("flow_fundamentals", "flow_collection", "stateflow", "sharedflow"),
                listOf("hot_vs_cold_streams", "flow_collection", "kotlin_equality", "state_ownership"),
                listOf("hot_vs_cold_streams", "flow_buffering", "stateflow", "flow_collection"),
                listOf("stateflow", "sharedflow", "coroutine_scope", "lifecycle_coroutines"),
                listOf("stateflow", "sharedflow", "flow_sharing", "state_ownership"),
            ),
            unit("unit_stateflow_sharedflow_and_hot_streams").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun architectureFoundationsUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // `solid` is supporting in three Lessons and never primary, which is the E26-01
        // decision recorded in `docs/content/architecture-units-1-6-plan.md`: SOLID is
        // vocabulary here rather than curriculum structure, and promoting it would pull
        // `architecture_solid_dependency_substitution` into a Unit that teaches inversion
        // only as a forward pointer. The two build_delivery bridges and `test_doubles` are
        // named for the same reason — the Unit states a boundary against those curricula and
        // must not claim their practice.
        assertEquals(
            listOf(
                listOf("architecture_tradeoffs", "layered_architecture", "android_modules"),
                listOf("solid", "architecture_tradeoffs", "interface_boundaries"),
                listOf("interface_boundaries", "solid", "layered_architecture", "module_dependency_direction"),
                listOf("dependency_direction", "solid", "architecture_tradeoffs", "test_doubles"),
                listOf("separation_of_concerns", "android_modules", "modularization_tradeoffs"),
            ),
            unit("unit_architecture_responsibilities_and_boundaries").lessons.map {
                it.supportingSubtopicIds
            },
        )
    }

    @Test
    fun architectureFoundationsUnitLinksBackwardsOnlyAndReachesItsOneShippedAnchor() = runTest {
        val lessons = unit("unit_architecture_responsibilities_and_boundaries").lessons.associateBy { it.id }

        // The opening Lesson has no prerequisite inside or outside the subject, so it links
        // to nothing rather than manufacturing graph completeness.
        assertEquals(emptyList(), lessons.getValue("lesson_what_architecture_decides").relatedLessonIds)
        // `lesson_state_hoisting` is the subject's only meaningful shipped anchor: it already
        // teaches the reader/writer/lifetime ownership test at composable scale, and this
        // Lesson generalises it. It is linked once, from the Lesson that uses it, rather than
        // from all five.
        assertEquals(
            listOf("lesson_what_architecture_decides", "lesson_state_hoisting"),
            lessons.getValue("lesson_responsibility_and_change").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_what_architecture_decides", "lesson_responsibility_and_change"),
            lessons.getValue("lesson_dependency_direction_and_boundaries").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_responsibility_and_change", "lesson_dependency_direction_and_boundaries"),
            lessons.getValue("lesson_when_an_interface_is_a_boundary").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_what_architecture_decides",
                "lesson_dependency_direction_and_boundaries",
                "lesson_when_an_interface_is_a_boundary",
            ),
            lessons.getValue("lesson_layers_and_their_cost").relatedLessonIds,
        )

        val shippedAnchors = setOf("lesson_state_hoisting")
        val ownIds = lessons.keys
        // Later E26 Units do not exist yet, so every link either stays inside this Unit or
        // names one of the shipped Lessons above. A forward link would not resolve at all.
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(related in ownIds || related in shippedAnchors, "${lesson.id} -> $related")
            }
        }
    }

    @Test
    fun stateHolderUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Every supporting concept here belongs to another Topic and is bridged only as far
        // as this Unit needs it: the Compose hoisting and state contracts are E23's and
        // E25's, the lifecycle facts are the lifecycle and navigation curriculum's, the
        // coroutine scope model is E24's, and `background_api_selection` is named once as
        // where the answer continues for work the screen owner is too short-lived to hold.
        // None of them may broaden the Unit's practice, which the integration test asserts.
        assertEquals(
            listOf(
                listOf(
                    "compose_state_hoisting",
                    "viewmodel_lifecycle",
                    "kmp_lifecycle_viewmodel",
                    "separation_of_concerns",
                ),
                listOf(
                    "viewmodel_lifecycle",
                    "configuration_changes",
                    "process_death",
                    "saved_state",
                    "kmp_lifecycle_viewmodel",
                ),
                listOf("kotlin_sealed_types", "error_modeling", "compose_stability", "compose_state"),
                listOf("state_ownership", "compose_udf", "stateflow"),
                listOf(
                    "lifecycle_coroutines",
                    "coroutine_scope",
                    "viewmodel_lifecycle",
                    "background_api_selection",
                ),
            ),
            unit("unit_screen_state_holders_and_ui_state").lessons.map { it.supportingSubtopicIds },
        )

        // `state_ownership` is primary in four of the five Lessons and supporting in the
        // fifth, which is the one place in the Unit where the two roles meet. The validator
        // rejects the overlap only within a Lesson, so the split is asserted here: the
        // contract Lesson teaches direction thoroughly and leans on ownership, rather than
        // claiming to teach both.
        val contract = unit("unit_screen_state_holders_and_ui_state")
            .lessons
            .single { it.id == "lesson_state_out_intentions_in" }
        assertEquals(listOf("unidirectional_data_flow"), contract.primarySubtopicIds)
        assertTrue("state_ownership" in contract.supportingSubtopicIds)
    }

    @Test
    fun stateHolderUnitLinksBackwardsOnlyToShippedComposeAndCoroutineAnchors() = runTest {
        val lessons = unit("unit_screen_state_holders_and_ui_state").lessons.associateBy { it.id }

        // Each Lesson names the shipped Lessons whose model it applies, and nothing else.
        // The plan's intended link graph is backward-only, so no Lesson here names one from a
        // Unit authored after this one, however useful the pointer would be.
        assertEquals(
            listOf(
                "lesson_state_hoisting",
                "lesson_classes_of_screen_state",
                "lesson_screen_state_owner_boundary",
            ),
            lessons.getValue("lesson_state_holder_responsibility").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_remember_saveable",
                "lesson_screen_state_owner_boundary",
                "lesson_state_holder_responsibility",
            ),
            lessons.getValue("lesson_viewmodel_lifetime_and_persistence").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_immutability_vs_stability",
                "lesson_screen_state_and_ui_events",
                "lesson_state_holder_responsibility",
            ),
            lessons.getValue("lesson_modelling_ui_state").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_state_down_events_up",
                "lesson_screen_state_and_ui_events",
                "lesson_modelling_ui_state",
            ),
            lessons.getValue("lesson_state_out_intentions_in").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_coroutine_scope_ownership",
                "lesson_remember_coroutine_scope",
                "lesson_who_owns_the_trigger",
                "lesson_state_holder_responsibility",
            ),
            lessons.getValue("lesson_owner_scoped_work").relatedLessonIds,
        )

        val shippedAnchors = setOf(
            "lesson_state_hoisting",
            "lesson_classes_of_screen_state",
            "lesson_screen_state_owner_boundary",
            "lesson_remember_saveable",
            "lesson_immutability_vs_stability",
            "lesson_screen_state_and_ui_events",
            "lesson_state_down_events_up",
            "lesson_coroutine_scope_ownership",
            "lesson_remember_coroutine_scope",
            "lesson_who_owns_the_trigger",
        )
        val ownIds = lessons.keys
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(related in ownIds || related in shippedAnchors, "${lesson.id} -> $related")
            }
        }

        // No Lesson authored *before* this Unit was edited to receive a reciprocal link: E26
        // links backwards only, and the two Compose Lessons that point at this curriculum do
        // so in prose. Units authored afterwards may of course link back into this one, and
        // the data-ownership Unit does.
        val ownedByThisUnit = ownIds.toSet()
        units()
            .takeWhile { it.id != "unit_screen_state_holders_and_ui_state" }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownedByThisUnit },
                    "${lesson.id} links forward into the state-holder Unit",
                )
            }
    }

    @Test
    fun dataOwnershipUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Every supporting concept here belongs to a curriculum this Unit states a boundary
        // against and must not claim practice from: `room_dao` and `retrofit` are named only
        // as examples of a data source, the three local_data concepts motivate a source
        // policy without teaching storage mechanics, the three Flow concepts are E24's
        // contracts applied rather than re-derived, and `kotlin_sealed_types` is one way of
        // writing an error contract down rather than a subject. `state_ownership` is the one
        // most likely to be promoted by mistake, because the authority Lesson borrows the
        // ownership vocabulary the previous Unit established for a different question.
        assertEquals(
            listOf(
                listOf("room_dao", "retrofit", "separation_of_concerns", "architecture_tradeoffs"),
                listOf("offline_first", "cache_invalidation", "caching"),
                listOf("offline_first", "cache_invalidation", "state_ownership"),
                listOf("flow_fundamentals", "flow_collection", "stateflow", "single_source_of_truth"),
                listOf("repository_pattern", "kotlin_sealed_types", "room_dao", "retrofit"),
            ),
            unit("unit_repositories_and_data_ownership").lessons.map { it.supportingSubtopicIds },
        )

        // Two concepts are primary in one Lesson and supporting in another, which is the
        // shape a Unit takes when its Lessons genuinely teach different things about the
        // same vocabulary. The validator rejects the overlap only within a Lesson, so the
        // split is asserted here: the API-shape Lesson leans on authority without claiming
        // to teach it, and the boundary Lesson leans on the repository contract while
        // teaching what crosses it.
        val lessons = unit("unit_repositories_and_data_ownership").lessons.associateBy { it.id }
        val apiShape = lessons.getValue("lesson_observable_or_one_shot_api")
        assertEquals(listOf("repository_pattern"), apiShape.primarySubtopicIds)
        assertTrue("single_source_of_truth" in apiShape.supportingSubtopicIds)
        val boundaries = lessons.getValue("lesson_model_and_error_boundaries")
        assertEquals(listOf("layered_architecture", "error_modeling"), boundaries.primarySubtopicIds)
        assertTrue("repository_pattern" in boundaries.supportingSubtopicIds)
    }

    @Test
    fun dataOwnershipUnitLinksBackwardsOnlyToShippedArchitectureAndFlowAnchors() = runTest {
        val lessons = unit("unit_repositories_and_data_ownership").lessons.associateBy { it.id }

        // Each Lesson names the shipped Lessons whose reasoning it applies, and nothing else.
        // The three E24 links belong to the API-shape Lesson because that is where the stream
        // contracts are applied rather than re-derived, which is the plan's intended graph.
        assertEquals(
            listOf(
                "lesson_when_an_interface_is_a_boundary",
                "lesson_layers_and_their_cost",
                "lesson_state_holder_responsibility",
            ),
            lessons.getValue("lesson_what_a_repository_owns").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_what_a_repository_owns", "lesson_layers_and_their_cost"),
            lessons.getValue("lesson_coordinating_sources").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_coordinating_sources", "lesson_state_holder_responsibility"),
            lessons.getValue("lesson_single_source_of_truth").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_why_flow",
                "lesson_state_flow",
                "lesson_choosing_a_stream_abstraction",
                "lesson_single_source_of_truth",
            ),
            lessons.getValue("lesson_observable_or_one_shot_api").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_layers_and_their_cost",
                "lesson_modelling_ui_state",
                "lesson_what_a_repository_owns",
            ),
            lessons.getValue("lesson_model_and_error_boundaries").relatedLessonIds,
        )

        val shippedAnchors = setOf(
            "lesson_when_an_interface_is_a_boundary",
            "lesson_layers_and_their_cost",
            "lesson_state_holder_responsibility",
            "lesson_modelling_ui_state",
            "lesson_why_flow",
            "lesson_state_flow",
            "lesson_choosing_a_stream_abstraction",
        )
        val ownIds = lessons.keys
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(related in ownIds || related in shippedAnchors, "${lesson.id} -> $related")
            }
        }

        // No Lesson authored before this Unit was edited to receive a reciprocal link. The two
        // earlier architecture Units point forward in prose only, because a forward link would
        // not have resolved when they shipped.
        units()
            .takeWhile { it.id != "unit_repositories_and_data_ownership" }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the data-ownership Unit",
                )
            }
    }

    @Test
    fun domainLogicUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Nine concepts are supporting-only across this Unit and four of them are the ones a
        // careless promotion would damage most. `solid` is supporting-only across the whole
        // epic by design, so `architecture_solid_dependency_substitution` reaches no Unit's
        // practice; `service_locator_vs_di` and `android_modules` belong to the dependency-
        // injection and modularization curricula, which this Unit names and does not teach;
        // and `kmp_architecture` is one bounded sentence about what the policy/detail split
        // makes shareable. Promoting any of them would claim practice coverage for material
        // this Unit deliberately does not carry.
        assertEquals(
            listOf(
                listOf("layered_architecture", "architecture_tradeoffs", "separation_of_concerns"),
                listOf("repository_pattern", "architecture_tradeoffs", "state_ownership"),
                listOf("clean_architecture", "layered_architecture", "kmp_architecture"),
                listOf("solid", "clean_architecture", "repository_pattern", "service_locator_vs_di"),
                listOf("layered_architecture", "use_cases", "dependency_direction", "android_modules"),
            ),
            unit("unit_domain_logic_and_dependency_direction").lessons.map { it.supportingSubtopicIds },
        )

        // Two concepts are primary in one Lesson of this Unit and supporting in another, which
        // the validator permits across Lessons and rejects within one. `clean_architecture` is
        // supporting where the policy/detail split borrows the dependency rule and primary
        // where the rule itself is the subject; `use_cases` is the reverse.
        val lessons = unit("unit_domain_logic_and_dependency_direction").lessons.associateBy { it.id }
        val policy = lessons.getValue("lesson_policy_and_framework_detail")
        assertEquals(listOf("dependency_direction"), policy.primarySubtopicIds)
        assertTrue("clean_architecture" in policy.supportingSubtopicIds)
        val cleanArchitecture = lessons.getValue("lesson_clean_architecture_intent")
        assertEquals(listOf("clean_architecture"), cleanArchitecture.primarySubtopicIds)
        assertTrue("use_cases" in cleanArchitecture.supportingSubtopicIds)
    }

    @Test
    fun domainLogicUnitLinksBackwardsOnlyToShippedArchitectureAnchors() = runTest {
        val lessons = unit("unit_domain_logic_and_dependency_direction").lessons.associateBy { it.id }

        // Every link is backward and chosen by actual semantic dependency rather than by
        // giving each earlier Lesson a mention. The inversion Lesson carries three because the
        // foundations Unit deferred direction and interface ownership to it explicitly and the
        // data-ownership Unit deferred the repository half of the same question.
        assertEquals(
            listOf("lesson_layers_and_their_cost", "lesson_what_a_repository_owns"),
            lessons.getValue("lesson_when_a_domain_layer_earns_its_place").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_when_a_domain_layer_earns_its_place",
                "lesson_what_a_repository_owns",
                "lesson_state_holder_responsibility",
            ),
            lessons.getValue("lesson_use_cases_and_pass_through_cost").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_dependency_direction_and_boundaries",
                "lesson_state_holder_responsibility",
                "lesson_model_and_error_boundaries",
            ),
            lessons.getValue("lesson_policy_and_framework_detail").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_dependency_direction_and_boundaries",
                "lesson_when_an_interface_is_a_boundary",
                "lesson_what_a_repository_owns",
            ),
            lessons.getValue("lesson_dependency_inversion_in_practice").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_layers_and_their_cost",
                "lesson_when_a_domain_layer_earns_its_place",
                "lesson_policy_and_framework_detail",
            ),
            lessons.getValue("lesson_clean_architecture_intent").relatedLessonIds,
        )

        val shippedAnchors = setOf(
            "lesson_layers_and_their_cost",
            "lesson_dependency_direction_and_boundaries",
            "lesson_when_an_interface_is_a_boundary",
            "lesson_state_holder_responsibility",
            "lesson_what_a_repository_owns",
            "lesson_model_and_error_boundaries",
        )
        val ownIds = lessons.keys
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(related in ownIds || related in shippedAnchors, "${lesson.id} -> $related")
            }
        }

        // No Lesson authored before this Unit was edited to receive a reciprocal link. The
        // three earlier architecture Units point forward in prose only, naming the Unit rather
        // than a Lesson id, because a forward link would not have resolved when they shipped.
        units()
            .takeWhile { it.id != "unit_domain_logic_and_dependency_direction" }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the domain-logic Unit",
                )
            }
    }

    @Test
    fun responsibilityModelsUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Seven concepts are supporting-only across this Unit, and three of them are the ones a
        // promotion would damage. `stateflow` is one bounded sentence saying the carrier is not part
        // of the pattern — E24 owns stream mechanics; `kotlin_sealed_types` is the spelling of an
        // intent type and nothing about the language mechanism is taught; `viewmodel_lifecycle` is
        // named only to say that dependency direction and lifetime are separate facts, with the
        // lifetime itself deferred to the state-holder Unit. Promoting any of them would claim
        // practice coverage for material this Unit deliberately does not carry.
        assertEquals(
            listOf(
                listOf("mvp", "mvvm", "mvi", "state_ownership"),
                listOf("mvc", "interface_boundaries", "state_ownership"),
                listOf("state_ownership", "unidirectional_data_flow", "stateflow", "viewmodel_lifecycle"),
                listOf("unidirectional_data_flow", "state_ownership", "kotlin_sealed_types"),
                listOf("mvvm", "mvi", "mvp", "architecture_tradeoffs"),
            ),
            unit("unit_responsibility_models_mvp_mvvm_mvi").lessons.map { it.supportingSubtopicIds },
        )

        // Every pattern concept is primary in one Lesson and supporting in at least one other, which
        // the validator permits across Lessons and rejects within one. That is the Unit's shape
        // rather than an accident: the comparison frame names all three models it has not taught
        // yet, and the classification Lesson names all three it has.
        val lessons = unit("unit_responsibility_models_mvp_mvvm_mvi").lessons.associateBy { it.id }
        listOf("mvp", "mvvm", "mvi").forEach { concept ->
            assertTrue(concept in lessons.getValue("lesson_one_screen_five_questions").supportingSubtopicIds)
            assertTrue(concept in lessons.getValue("lesson_classifying_a_real_architecture").supportingSubtopicIds)
        }
        assertEquals(listOf("mvc"), lessons.getValue("lesson_one_screen_five_questions").primarySubtopicIds)
        assertTrue("mvc" in lessons.getValue("lesson_mvp_view_contract").supportingSubtopicIds)
    }

    @Test
    fun responsibilityModelsUnitLinksBackwardsOnlyToShippedArchitectureAnchors() = runTest {
        val lessons = unit("unit_responsibility_models_mvp_mvvm_mvi").lessons.associateBy { it.id }

        // The MVVM Lesson carries five links because the plan requires it to name what the
        // state-holder Unit already built rather than re-derive it, and to hand the stream carrier
        // back to E24. Every other link is one actual semantic dependency rather than a mention.
        assertEquals(
            listOf("lesson_what_architecture_decides", "lesson_state_holder_responsibility"),
            lessons.getValue("lesson_one_screen_five_questions").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_one_screen_five_questions", "lesson_when_an_interface_is_a_boundary"),
            lessons.getValue("lesson_mvp_view_contract").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_mvp_view_contract",
                "lesson_state_holder_responsibility",
                "lesson_state_out_intentions_in",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_state_flow",
            ),
            lessons.getValue("lesson_mvvm_observed_state").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_mvvm_observed_state",
                "lesson_modelling_ui_state",
                "lesson_state_out_intentions_in",
            ),
            lessons.getValue("lesson_mvi_intent_and_reduction").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_one_screen_five_questions",
                "lesson_mvp_view_contract",
                "lesson_mvvm_observed_state",
                "lesson_mvi_intent_and_reduction",
            ),
            lessons.getValue("lesson_classifying_a_real_architecture").relatedLessonIds,
        )

        val shippedAnchors = setOf(
            "lesson_what_architecture_decides",
            "lesson_when_an_interface_is_a_boundary",
            "lesson_state_holder_responsibility",
            "lesson_state_out_intentions_in",
            "lesson_modelling_ui_state",
            "lesson_viewmodel_lifetime_and_persistence",
            "lesson_state_flow",
        )
        val ownIds = lessons.keys
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(related in ownIds || related in shippedAnchors, "${lesson.id} -> $related")
            }
        }

        // No Lesson authored before this Unit was edited to receive a reciprocal link, and no
        // Lesson here links forward into the synthesis Unit, which has not shipped.
        units()
            .takeWhile { it.id != "unit_responsibility_models_mvp_mvvm_mvi" }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the responsibility-models Unit",
                )
            }
    }

    @Test
    fun theSynthesisUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Ten concepts are supporting-only across this Unit, and every one of them is a concept
        // another shipped curriculum owns and assesses. `sharedflow`, `hot_vs_cold_streams` and
        // `stateflow` carry the stream contracts this Unit applies without re-deriving;
        // `process_death` and `viewmodel_lifecycle` carry the lifetime facts the lifecycle
        // curriculum owns; `background_api_selection` is named once as where the last rung of the
        // lifetime ladder continues. Promoting any of them would claim practice coverage for
        // mechanisms this Unit deliberately refuses to teach.
        assertEquals(
            listOf(
                listOf("unidirectional_data_flow", "stateflow", "sharedflow", "single_source_of_truth"),
                listOf("sharedflow", "hot_vs_cold_streams", "process_death", "single_source_of_truth"),
                listOf("lifecycle_coroutines", "coroutine_scope", "viewmodel_lifecycle", "background_api_selection"),
                listOf("use_cases", "repository_pattern", "layered_architecture", "clean_architecture"),
            ),
            unit("unit_state_events_lifetime_and_selection").lessons.map { it.supportingSubtopicIds },
        )

        // The closing Lesson names four concepts that are primary in Units 1, 3 and 4, and takes
        // none of them as its own: it asks whether a whole feature's structure is proportionate,
        // which is `architecture_tradeoffs`, not whether any one of those structures is correct.
        val lessons = unit("unit_state_events_lifetime_and_selection").lessons.associateBy { it.id }
        val closing = lessons.getValue("lesson_smallest_sufficient_architecture")
        assertEquals(listOf("architecture_tradeoffs"), closing.primarySubtopicIds)
        listOf("use_cases", "repository_pattern", "layered_architecture", "clean_architecture").forEach {
            assertTrue(it in closing.supportingSubtopicIds)
        }
    }

    @Test
    fun theSynthesisUnitLinksBackwardsAndClosesTheLoopOverEveryEarlierArchitectureUnit() = runTest {
        val lessons = unit("unit_state_events_lifetime_and_selection").lessons.associateBy { it.id }

        assertEquals(
            listOf(
                "lesson_transient_ui_effects",
                "lesson_transient_effect_delivery",
                "lesson_state_flow",
                "lesson_state_holder_responsibility",
                "lesson_single_source_of_truth",
            ),
            lessons.getValue("lesson_state_or_occurrence").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_state_or_occurrence",
                "lesson_transient_effect_delivery",
                "lesson_shared_flow",
                "lesson_choosing_a_stream_abstraction",
                "lesson_single_source_of_truth",
            ),
            lessons.getValue("lesson_delivery_guarantees").relatedLessonIds,
        )
        // The observable-API link is load-bearing rather than decorative: the Lesson's
        // application-scope rung turns on this repository having no observable source to
        // subscribe to, which is that Lesson's decision, so the bounded claim carries its pointer.
        assertEquals(
            listOf(
                "lesson_delivery_guarantees",
                "lesson_observable_or_one_shot_api",
                "lesson_owner_scoped_work",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_coroutine_scope_ownership",
            ),
            lessons.getValue("lesson_choosing_the_owner_by_lifetime").relatedLessonIds,
        )

        // The plan's one explicit link requirement for this Unit: the closing Lesson must reach
        // back into every one of the five Units it synthesises. It is asserted by resolving each
        // target to its Unit rather than by listing ids, so the claim is the one the plan makes.
        val closing = lessons.getValue("lesson_smallest_sufficient_architecture")
        val owningUnit = units().flatMap { owner -> owner.lessons.map { it.id to owner.id } }.toMap()
        assertEquals(
            setOf(
                "unit_architecture_responsibilities_and_boundaries",
                "unit_screen_state_holders_and_ui_state",
                "unit_repositories_and_data_ownership",
                "unit_domain_logic_and_dependency_direction",
                "unit_responsibility_models_mvp_mvvm_mvi",
                "unit_state_events_lifetime_and_selection",
            ),
            closing.relatedLessonIds.map { owningUnit.getValue(it) }.toSet(),
        )

        // Every link in the Unit points backwards: to a Lesson shipped before this epic, to an
        // earlier E26 Unit, or to an earlier Lesson of this one.
        val ownIds = lessons.keys
        val order = units().flatMap { it.lessons.map { lesson -> lesson.id } }
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(
                    order.indexOf(related) < order.indexOf(lesson.id),
                    "${lesson.id} -> $related is not a backward link",
                )
            }
        }

        // No Lesson authored before this Unit was edited to receive a reciprocal link. This is the
        // last instructional Unit of the epic, so nothing in the document links forward at all.
        units()
            .takeWhile { it.id != "unit_state_events_lifetime_and_selection" }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the synthesis Unit",
                )
            }
    }

    @Test
    fun dependencyInjectionFoundationsUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Twelve concepts are supporting-only across this Unit. Four of them are the other
        // Lessons' primaries, named where one Lesson needs the next one's vocabulary without
        // claiming to teach it; the rest bridge to `architecture`, which already owns them.
        // `test_doubles` and `test_di` are the two that matter most: substitution is named as a
        // consequence of an explicit dependency and nothing else, because the testing curriculum
        // owns test doubles and test graphs, and promoting either would claim practice coverage
        // for material this Unit deliberately refuses to teach.
        assertEquals(
            listOf(
                listOf("constructor_injection", "manual_di", "service_locator_vs_di", "separation_of_concerns"),
                listOf("di_fundamentals", "interface_boundaries", "dependency_direction", "test_doubles"),
                listOf("dependency_direction", "interface_boundaries", "solid", "constructor_injection"),
                listOf(
                    "manual_di",
                    "dependency_graphs",
                    "service_locator_vs_di",
                    "separation_of_concerns",
                    "layered_architecture",
                ),
                listOf("di_fundamentals", "composition_root", "constructor_injection", "test_di"),
                listOf("composition_root", "dependency_graphs", "di_framework_tradeoffs", "architecture_tradeoffs"),
            ),
            unit("unit_dependency_injection_as_object_construction").lessons.map { it.supportingSubtopicIds },
        )

        // `di_framework_tradeoffs` is supporting in the closing Lesson and primary in three
        // Lessons of the Unit that compares the frameworks, which does not ship yet. Promoting
        // it here would claim the comparison this Unit explicitly defers.
        val lessons = unit("unit_dependency_injection_as_object_construction").lessons.associateBy { it.id }
        val closing = lessons.getValue("lesson_when_wiring_it_yourself_is_enough")
        assertEquals(listOf("manual_di"), closing.primarySubtopicIds)
        assertTrue("di_framework_tradeoffs" in closing.supportingSubtopicIds)
    }

    @Test
    fun dependencyInjectionFoundationsUnitLinksBackwardsOnlyToShippedArchitectureAnchors() = runTest {
        val unitId = "unit_dependency_injection_as_object_construction"
        val lessons = unit(unitId).lessons.associateBy { it.id }

        // `docs/content/dependency-injection-units-1-6-plan.md` names four required backward
        // links into the shipped architecture Units, and each of them is load-bearing rather
        // than decorative: the boundary test this Unit refuses to re-argue, the inversion
        // argument it applies from the injection side, what architecture decides, and the
        // proportionality test the closing Lesson reuses for wiring. The intra-Unit links are
        // the Lessons whose conclusions the current one consumes.
        assertEquals(
            listOf("lesson_state_holder_responsibility"),
            lessons.getValue("lesson_who_constructs_this_object").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_when_an_interface_is_a_boundary", "lesson_who_constructs_this_object"),
            lessons.getValue("lesson_a_dependency_should_be_visible").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_dependency_inversion_in_practice",
                "lesson_dependency_direction_and_boundaries",
                "lesson_a_dependency_should_be_visible",
            ),
            lessons.getValue("lesson_injected_inverted_or_both").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_what_architecture_decides", "lesson_who_constructs_this_object"),
            lessons.getValue("lesson_one_place_that_knows_how_to_build").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_who_constructs_this_object",
                "lesson_a_dependency_should_be_visible",
                "lesson_one_place_that_knows_how_to_build",
            ),
            lessons.getValue("lesson_asking_for_it_or_being_given_it").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_smallest_sufficient_architecture",
                "lesson_layers_and_their_cost",
                "lesson_one_place_that_knows_how_to_build",
            ),
            lessons.getValue("lesson_when_wiring_it_yourself_is_enough").relatedLessonIds,
        )

        // Every link points backwards: to a Lesson shipped before this epic, or to an earlier
        // Lesson of this Unit. The five later dependency-injection Units do not exist yet, so a
        // forward link would not resolve at all.
        val order = units().flatMap { it.lessons.map { lesson -> lesson.id } }
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(
                    order.indexOf(related) < order.indexOf(lesson.id),
                    "${lesson.id} -> $related is not a backward link",
                )
            }
        }

        // No Lesson authored before this Unit was edited to receive a reciprocal link. The three
        // shipped Lessons that point at this curriculum do so in prose, which is what the plan
        // recorded, so nothing in the earlier document names a Lesson of this Unit.
        val ownIds = lessons.keys
        units()
            .takeWhile { it.id != unitId }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the dependency-injection Unit",
                )
            }
    }

    @Test
    fun dependencyInjectionFoundationsUnitTeachesEveryConclusionWithoutAFramework() = runTest {
        // E27-02's acceptance criterion that no Dagger, Hilt or Koin syntax appears beyond a
        // brief forward reference. Only authored code is inspected, because that is where the
        // criterion is about syntax: prose may name a framework in a forward sentence, and
        // policing prose here would flag the very sentences the plan permits.
        val frameworkSyntax = listOf(
            "@Inject",
            "@Provides",
            "@Binds",
            "@Module",
            "@Component",
            "@Subcomponent",
            "@Singleton",
            "@AndroidEntryPoint",
            "@HiltAndroidApp",
            "@HiltViewModel",
            "startKoin",
            "koinViewModel",
            "koinInject",
            "single {",
            "factory {",
            "module {",
            "viewModel {",
            "by inject",
        )
        val authoredCode = unit("unit_dependency_injection_as_object_construction")
            .lessons
            .flatMap { lesson -> lesson.sections.flatMap { it.blocks } }
            .filterIsInstance<LearningBlock.Code>()

        assertTrue(authoredCode.size >= 6, "Expected the Unit to argue from code it actually shows.")
        authoredCode.forEach { block ->
            frameworkSyntax.forEach { token ->
                assertFalse(
                    block.code.contains(token),
                    "Unit 1 code contains framework syntax '$token'.",
                )
            }
        }
    }

    @Test
    fun objectGraphUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // Eleven concepts are supporting-only across this Unit, and the ones that matter are the
        // framework Subtopics. `dagger_qualifiers` is supporting in the same-type Lesson because the
        // generic idea — a key that is more than a type — has no framework-independent
        // Subtopic in the taxonomy, and `dagger_fundamentals` is supporting where a broken graph's
        // detection timing is taught generically. Promoting either would route Dagger Questions into
        // a Unit that teaches no Dagger, which is the opposite of what the plan asks for.
        assertEquals(
            listOf(
                listOf("composition_root", "manual_di", "constructor_injection", "layered_architecture"),
                listOf("dependency_graphs", "state_ownership", "architecture_tradeoffs"),
                listOf(
                    "dependency_graphs",
                    "state_ownership",
                    "android_process_model",
                    "viewmodel_lifecycle",
                ),
                listOf("di_scopes", "constructor_injection", "navigation_fundamentals", "state_ownership"),
                listOf("dagger_qualifiers", "interface_boundaries", "constructor_injection"),
                listOf("di_framework_tradeoffs", "dagger_fundamentals", "kotlin_gradle_plugin"),
            ),
            unit("unit_object_graphs_lifetimes_and_scopes").lessons.map { it.supportingSubtopicIds },
        )

        // No Lesson lists a concept as both primary and supporting, and the two Lessons that own
        // instance identity are the only ones taking `di_scopes` as primary — the Lesson on
        // runtime input names it as supporting because it borrows the vocabulary without teaching it.
        val lessons = unit("unit_object_graphs_lifetimes_and_scopes").lessons
        lessons.forEach { lesson ->
            assertTrue(
                lesson.primarySubtopicIds.none { it in lesson.supportingSubtopicIds },
                "${lesson.id} lists a concept as both primary and supporting",
            )
        }
        assertEquals(
            listOf("lesson_one_instance_or_a_new_one", "lesson_scope_is_a_rule_owner_is_a_lifetime"),
            lessons.filter { "di_scopes" in it.primarySubtopicIds }.map { it.id },
        )
        assertTrue(
            "di_scopes" in lessons.single { it.id == "lesson_runtime_input_is_not_a_dependency" }
                .supportingSubtopicIds,
        )
    }

    @Test
    fun objectGraphUnitLinksBackwardsOnlyToShippedOwnershipAnchorsAndTheFirstUnit() = runTest {
        val unitId = "unit_object_graphs_lifetimes_and_scopes"
        val lessons = unit(unitId).lessons.associateBy { it.id }

        // `docs/content/dependency-injection-units-1-6-plan.md` names the required backward links:
        // the graph Lesson consumes the composition root, the two reuse Lessons apply the shipped
        // owner-selection and ViewModel-lifetime arguments rather than re-deriving them, and the
        // runtime-input Lesson consumes the constructor-injection guarantees it refuses to reteach.
        assertEquals(
            listOf("lesson_one_place_that_knows_how_to_build"),
            lessons.getValue("lesson_from_one_dependency_to_a_graph").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_choosing_the_owner_by_lifetime",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_from_one_dependency_to_a_graph",
            ),
            lessons.getValue("lesson_one_instance_or_a_new_one").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_choosing_the_owner_by_lifetime",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_state_holder_responsibility",
                "lesson_one_instance_or_a_new_one",
            ),
            lessons.getValue("lesson_scope_is_a_rule_owner_is_a_lifetime").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_a_dependency_should_be_visible", "lesson_from_one_dependency_to_a_graph"),
            lessons.getValue("lesson_runtime_input_is_not_a_dependency").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_when_an_interface_is_a_boundary", "lesson_from_one_dependency_to_a_graph"),
            lessons.getValue("lesson_two_dependencies_of_the_same_type").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_when_wiring_it_yourself_is_enough", "lesson_two_dependencies_of_the_same_type"),
            lessons.getValue("lesson_when_a_broken_graph_tells_you").relatedLessonIds,
        )

        // Every link points backwards. The four later dependency-injection Units do not exist yet, so
        // a forward `relatedLessonIds` entry would not resolve at all; forward pointing is prose.
        val order = units().flatMap { it.lessons.map { lesson -> lesson.id } }
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(
                    order.indexOf(related) < order.indexOf(lesson.id),
                    "${lesson.id} -> $related is not a backward link",
                )
            }
        }

        // No earlier Lesson was edited to receive a reciprocal link, including the six Lessons of the
        // first dependency-injection Unit: they point forward in prose only, which is what the plan
        // recorded and what keeps shipped study-progress identities untouched.
        val ownIds = lessons.keys
        units()
            .takeWhile { it.id != unitId }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the object-graph Unit",
                )
            }
    }

    @Test
    fun objectGraphUnitFixesItsVocabularyWithoutAFrameworkNotation() = runTest {
        // E27-03's acceptance criterion that no framework scope annotation or container API defines a
        // concept in this Unit. Only authored code is inspected: this Unit's whole purpose is to name
        // the concepts the three framework Units later encode, so its prose must be free to say that a
        // later Unit encodes them, and policing prose would flag exactly those sentences.
        val frameworkSyntax = listOf(
            "@Inject",
            "@Provides",
            "@Binds",
            "@Module",
            "@Component",
            "@Subcomponent",
            "@Singleton",
            "@Reusable",
            "@Qualifier",
            "@Named",
            "@AssistedInject",
            "@ActivityScoped",
            "@ViewModelScoped",
            "@ActivityRetainedScoped",
            "@InstallIn",
            "@AndroidEntryPoint",
            "@HiltViewModel",
            "startKoin",
            "koinViewModel",
            "koinInject",
            "single {",
            "factory {",
            "module {",
            "viewModel {",
            "by inject",
            "parametersOf",
            "SavedStateHandle",
        )
        val unit = unit("unit_object_graphs_lifetimes_and_scopes")
        val authoredCode = unit
            .lessons
            .flatMap { lesson -> lesson.sections.flatMap { it.blocks } }
            .filterIsInstance<LearningBlock.Code>()

        assertTrue(authoredCode.size >= 10, "Expected the Unit to argue from code and graphs it shows.")
        authoredCode.forEach { block ->
            frameworkSyntax.forEach { token ->
                assertFalse(
                    block.code.contains(token),
                    "Unit 2 code contains framework syntax '$token'.",
                )
            }
        }

        // The reuse decision has to be makeable before any notation exists, so the Lesson that teaches
        // it names no framework at all in any authored text, and the Lesson that fixes the scope
        // vocabulary states the coroutines collision explicitly rather than leaving "scope" ambiguous.
        fun textOf(lessonId: String): String {
            val lesson = unit.lessons.single { it.id == lessonId }
            return lesson.sections
                .flatMap { it.blocks }
                .joinToString(" ") { block ->
                    when (block) {
                        is LearningBlock.Paragraph -> block.text
                        is LearningBlock.BulletList -> block.items.joinToString(" ")
                        is LearningBlock.Callout -> block.text
                        is LearningBlock.Code -> block.code
                        is LearningBlock.Comparison ->
                            (block.headers + block.rows.flatten()).joinToString(" ")
                    }
                }
        }

        val scopeLesson = textOf("lesson_scope_is_a_rule_owner_is_a_lifetime")
        assertTrue(scopeLesson.contains("CoroutineScope"), "The coroutines collision is not disambiguated.")
        assertTrue(scopeLesson.contains("A scope creates nothing"), "The central correction is missing.")
    }

    @Test
    fun daggerUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("constructor_injection", "manual_di", "composition_root", "dependency_graphs"),
                listOf("dagger_fundamentals", "interface_boundaries", "dependency_direction"),
                listOf("dagger_modules", "composition_root", "dependency_graphs", "dagger_bindings"),
                listOf(
                    "dagger_scopes",
                    "dependency_graphs",
                    "layered_architecture",
                    "module_dependency_direction",
                ),
                listOf("di_scopes", "dagger_components", "state_ownership"),
                listOf(
                    "dagger_bindings",
                    "dependency_graphs",
                    "dependency_direction",
                    "feature_modularization",
                ),
                listOf(
                    "dependency_graphs",
                    "di_framework_tradeoffs",
                    "dagger_components",
                    "kotlin_gradle_plugin",
                ),
            ),
            unit("unit_dagger_compile_time_object_graphs").lessons.map { it.supportingSubtopicIds },
        )

        val lessons = unit("unit_dagger_compile_time_object_graphs").lessons
        lessons.forEach { lesson ->
            assertTrue(
                lesson.primarySubtopicIds.none { it in lesson.supportingSubtopicIds },
                "${lesson.id} lists a concept as both primary and supporting",
            )
        }

        // The load-bearing one. `dagger_compile_time_graph_validation` is mapped to
        // `dependency_graphs`, and the Lesson that teaches exactly its reasoning is the last one
        // here. Promoting `dependency_graphs` to primary there would pull that Question into this
        // Unit's practice without anybody deciding to re-map it, and would leave the honest routing
        // problem `docs/content/dependency-injection-units-1-6-plan.md` records invisible. E27-08
        // owns the re-map; until then the concept stays supporting in every Lesson of this Unit.
        assertTrue(
            lessons.none { "dependency_graphs" in it.primarySubtopicIds },
            "`dependency_graphs` became primary in the Dagger Unit.",
        )
        assertTrue(
            lessons.single { it.id == "lesson_what_the_dagger_compiler_checked" }
                .supportingSubtopicIds.contains("dependency_graphs"),
        )

        // `di_scopes` is Unit 2's primary concept and stays supporting here, so the generic reuse
        // Questions are not re-served by a Unit that teaches one framework's notation for them.
        assertTrue(
            lessons.none { "di_scopes" in it.primarySubtopicIds },
            "`di_scopes` became primary in the Dagger Unit.",
        )
    }

    @Test
    fun daggerUnitLinksBackwardsOnlyToTheGenericDecisionsItEncodes() = runTest {
        val unitId = "unit_dagger_compile_time_object_graphs"
        val lessons = unit(unitId).lessons.associateBy { it.id }

        // `docs/content/dependency-injection-units-1-6-plan.md` names four required backward links,
        // one per Lesson that encodes a decision an earlier Unit made. The others are present only
        // where the Lesson genuinely declines to re-argue something already shipped — the interface
        // boundary test, the component the scope Lesson names as an owner, the inversion argument
        // the multibinding reasoning rests on — rather than for symmetry.
        assertEquals(
            listOf(
                "lesson_one_place_that_knows_how_to_build",
                "lesson_from_one_dependency_to_a_graph",
            ),
            lessons.getValue("lesson_dagger_constructs_what_it_can_see").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_when_an_interface_is_a_boundary", "lesson_dagger_constructs_what_it_can_see"),
            lessons.getValue("lesson_declaring_the_rest_of_the_graph").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_one_place_that_knows_how_to_build", "lesson_declaring_the_rest_of_the_graph"),
            lessons.getValue("lesson_which_graph_owns_this_binding").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_which_graph_owns_this_binding"),
            lessons.getValue("lesson_child_graph_or_separate_graph").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_scope_is_a_rule_owner_is_a_lifetime", "lesson_which_graph_owns_this_binding"),
            lessons.getValue("lesson_dagger_scopes_and_component_instances").relatedLessonIds,
        )
        assertEquals(
            listOf("lesson_two_dependencies_of_the_same_type", "lesson_dependency_inversion_in_practice"),
            lessons.getValue("lesson_when_the_type_is_not_the_key").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_scope_is_a_rule_owner_is_a_lifetime",
                "lesson_when_a_broken_graph_tells_you",
                "lesson_dagger_scopes_and_component_instances",
            ),
            lessons.getValue("lesson_what_the_dagger_compiler_checked").relatedLessonIds,
        )

        // Every link points backwards. The Hilt and Koin Units do not exist yet, so a forward entry
        // would not resolve at all; pointing forward is prose.
        val order = units().flatMap { it.lessons.map { lesson -> lesson.id } }
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(
                    order.indexOf(related) < order.indexOf(lesson.id),
                    "${lesson.id} -> $related is not a backward link",
                )
            }
        }

        // No earlier Lesson was edited to receive a reciprocal link, including the twelve Lessons of
        // the two shipped dependency-injection Units.
        val ownIds = lessons.keys
        units()
            .takeWhile { it.id != unitId }
            .flatMap { it.lessons }
            .forEach { lesson ->
                assertTrue(
                    lesson.relatedLessonIds.none { it in ownIds },
                    "${lesson.id} links forward into the Dagger Unit",
                )
            }
    }

    @Test
    fun daggerUnitStaysInsideItsApiAndBuildBoundaries() = runTest {
        val unit = unit("unit_dagger_compile_time_object_graphs")

        fun textOf(lessonId: String): String {
            val lesson = unit.lessons.single { it.id == lessonId }
            return lesson.sections
                .flatMap { it.blocks }
                .joinToString(" ") { block ->
                    when (block) {
                        is LearningBlock.Paragraph -> block.text
                        is LearningBlock.BulletList -> block.items.joinToString(" ")
                        is LearningBlock.Callout -> block.text
                        is LearningBlock.Code -> block.code
                        is LearningBlock.Comparison ->
                            (block.headers + block.rows.flatten()).joinToString(" ")
                    }
                }
        }

        val authoredCode = unit
            .lessons
            .flatMap { lesson -> lesson.sections.flatMap { it.blocks } }
            .filterIsInstance<LearningBlock.Code>()
        assertTrue(authoredCode.size >= 20, "Expected the Unit to argue from code and graphs it shows.")

        // Hilt is E27-05's, and the whole thesis of that Unit is that Hilt is Dagger with the
        // Android decisions already made — which only lands if this Unit contains none of them.
        // Scanned over every authored block, not only code, because a prose mention would leak the
        // same forward material.
        val hiltSyntax = listOf(
            "@AndroidEntryPoint",
            "@HiltViewModel",
            "@InstallIn",
            "SingletonComponent",
            "ActivityRetainedComponent",
            "ViewModelComponent",
            "@HiltAndroidApp",
            "@EntryPoint",
        )
        val koinSyntax = listOf("startKoin", "koinViewModel", "koinInject", "single {", "factory {")
        unit.lessons.forEach { lesson ->
            val text = textOf(lesson.id)
            (hiltSyntax + koinSyntax).forEach { token ->
                assertFalse(text.contains(token), "${lesson.id} leaks forward into ${token}.")
            }
        }

        // E29 owns build engineering, and the plan allows this Unit to say only that a build step
        // exists. No Gradle snippet, no annotation-processor configuration, no build-cost analysis.
        val buildTokens = listOf("kapt", "KSP", "build.gradle", "annotationProcessor", "plugins {")
        unit.lessons.forEach { lesson ->
            val text = textOf(lesson.id)
            buildTokens.forEach { token ->
                assertFalse(text.contains(token), "${lesson.id} enters build configuration via ${token}.")
            }
        }

        // The two positive properties a token blocklist cannot express. The generated-construction
        // Lesson has to make the precise claim rather than the over-claim, and the scope Lesson has
        // to name the owner Unit 2 insisted on naming rather than letting the annotation imply it.
        val generation = textOf("lesson_dagger_constructs_what_it_can_see")
        assertTrue(
            generation.contains("rather than discovering the dependency graph through runtime reflection"),
            "The bounded no-reflection claim is missing.",
        )
        assertTrue(
            generation.contains("predictable, inspectable and paid for"),
            "The matching honesty that no reflection is not no cost is missing.",
        )

        val scopes = textOf("lesson_dagger_scopes_and_component_instances")
        assertTrue(
            scopes.contains("associates scoped instances in the graph with instances of component"),
            "The component-instance ownership anchor is missing.",
        )
        assertTrue(
            scopes.contains("A scope annotation creates no owner"),
            "The carried-forward correction from the object-graph Unit is missing.",
        )
    }

    @Test
    fun hiltUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("hilt_vs_dagger", "dagger_components", "dagger_fundamentals", "activity_lifecycle"),
                listOf(
                    "di_scopes",
                    "dagger_scopes",
                    "activity_lifecycle",
                    "configuration_changes",
                    "android_process_model",
                ),
                listOf("constructor_injection", "service_locator_vs_di", "activity_lifecycle", "hilt_components"),
                listOf(
                    "hilt_components",
                    "viewmodel_lifecycle",
                    "saved_state",
                    "state_ownership",
                    "dependency_graphs",
                ),
                listOf("dagger_modules", "hilt_components", "dagger_scopes", "di_scopes"),
                listOf("hilt_components", "dagger_components", "di_framework_tradeoffs", "architecture_tradeoffs"),
            ),
            unit("unit_hilt_android_lifecycle_integration").lessons.map { it.supportingSubtopicIds },
        )

        val primary = unit("unit_hilt_android_lifecycle_integration")
            .lessons
            .flatMap { it.primarySubtopicIds }
            .toSet()
        assertEquals(
            setOf("hilt_fundamentals", "hilt_components", "hilt_viewmodels", "hilt_modules", "hilt_vs_dagger"),
            primary,
        )
        unit("unit_hilt_android_lifecycle_integration").lessons.forEach { lesson ->
            assertTrue(lesson.primarySubtopicIds.none { it in lesson.supportingSubtopicIds }, lesson.id)
        }
    }

    @Test
    fun hiltUnitLinksOnlyToShippedPrerequisites() = runTest {
        val unitId = "unit_hilt_android_lifecycle_integration"
        val lessons = unit(unitId).lessons.associateBy { it.id }

        assertEquals(
            listOf("lesson_which_graph_owns_this_binding", "lesson_dagger_scopes_and_component_instances"),
            lessons.getValue("lesson_hilt_is_dagger_with_decisions_made").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_scope_is_a_rule_owner_is_a_lifetime",
                "lesson_choosing_the_owner_by_lifetime",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_dagger_scopes_and_component_instances",
            ),
            lessons.getValue("lesson_which_android_component_owns_this").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_a_dependency_should_be_visible",
                "lesson_asking_for_it_or_being_given_it",
                "lesson_lifecycle_aware_collection",
                "lesson_hilt_is_dagger_with_decisions_made",
            ),
            lessons.getValue("lesson_when_android_owns_construction").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_runtime_input_is_not_a_dependency",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_screen_state_owner_boundary",
                "lesson_which_android_component_owns_this",
            ),
            lessons.getValue("lesson_hilt_viewmodels_and_runtime_input").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_declaring_the_rest_of_the_graph",
                "lesson_which_android_component_owns_this",
                "lesson_which_graph_owns_this_binding",
            ),
            lessons.getValue("lesson_which_graph_does_this_binding_join").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_child_graph_or_separate_graph",
                "lesson_smallest_sufficient_architecture",
                "lesson_hilt_is_dagger_with_decisions_made",
                "lesson_which_android_component_owns_this",
            ),
            lessons.getValue("lesson_hilt_or_hand_written_dagger").relatedLessonIds,
        )

        val order = units().flatMap { it.lessons.map { lesson -> lesson.id } }
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(order.indexOf(related) < order.indexOf(lesson.id), "${lesson.id} -> $related")
            }
        }
    }

    @Test
    fun hiltUnitProtectsItsLifetimeConstructionAndBuildBoundaries() = runTest {
        val unit = unit("unit_hilt_android_lifecycle_integration")

        fun textOf(lessonId: String): String = unit.lessons
            .single { it.id == lessonId }
            .sections
            .flatMap { it.blocks }
            .joinToString(" ") { block ->
                when (block) {
                    is LearningBlock.Paragraph -> block.text
                    is LearningBlock.BulletList -> block.items.joinToString(" ")
                    is LearningBlock.Callout -> block.text
                    is LearningBlock.Code -> block.code
                    is LearningBlock.Comparison -> (block.headers + block.rows.flatten()).joinToString(" ")
                }
            }

        val lifetime = textOf("lesson_which_android_component_owns_this")
        listOf(
            "SingletonComponent",
            "ActivityRetainedComponent",
            "ViewModelComponent",
            "ActivityComponent",
            "does not mean durable across process death",
        ).forEach { claim -> assertTrue(lifetime.contains(claim), claim) }

        val construction = textOf("lesson_when_android_owns_construction")
        assertTrue(construction.contains("application does not own construction"))
        assertTrue(construction.contains("should still prefer constructor injection"))
        assertTrue(construction.contains("ContentProvider"))
        assertTrue(construction.contains("architectural cost"))

        val viewModels = textOf("lesson_hilt_viewmodels_and_runtime_input")
        assertTrue(viewModels.contains("does **not** mean Hilt owns the ViewModel lifetime"))
        assertTrue(viewModels.contains("not automatically UI state"))
        assertTrue(viewModels.contains("does not become durable"))

        val installation = textOf("lesson_which_graph_does_this_binding_join")
        assertTrue(installation.contains("does not by itself say"))
        assertTrue(installation.contains("Nothing about visibility changed. Only identity changed."))

        val allText = unit.lessons.joinToString(" ") { textOf(it.id) }
        listOf("kapt", "KSP", "build.gradle", "annotationProcessor", "plugins {", "startKoin", "koinViewModel")
            .forEach { token -> assertFalse(allText.contains(token), token) }
        assertFalse(allText.contains("@HiltAndroidApp"), "The Unit turns root integration into setup instruction.")
    }

    @Test
    fun koinUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        val unit = unit("unit_koin_and_dependency_injection_in_kmp")

        assertEquals(
            listOf(
                listOf(
                    "composition_root",
                    "dependency_graphs",
                    "koin_definitions",
                    "di_framework_tradeoffs",
                    "kmp_architecture",
                ),
                listOf(
                    "koin_fundamentals",
                    "di_scopes",
                    "constructor_injection",
                    "interface_boundaries",
                    "service_locator_vs_di",
                ),
                listOf("di_scopes", "koin_definitions", "koin_fundamentals", "state_ownership"),
                listOf(
                    "koin_definitions",
                    "service_locator_vs_di",
                    "kmp_lifecycle_viewmodel",
                    "viewmodel_lifecycle",
                    "state_ownership",
                ),
                listOf(
                    "koin_fundamentals",
                    "expect_actual",
                    "platform_implementations",
                    "kmp_architecture",
                    "composition_root",
                    "interface_boundaries",
                ),
            ),
            unit.lessons.map { it.supportingSubtopicIds },
        )

        val primary = unit.lessons.flatMap { it.primarySubtopicIds }.toSet()
        assertEquals(
            setOf(
                "koin_fundamentals",
                "koin_definitions",
                "koin_scopes",
                "koin_viewmodels",
                "koin_multiplatform",
            ),
            primary,
        )
        unit.lessons.forEach { lesson ->
            assertTrue(lesson.primarySubtopicIds.none { it in lesson.supportingSubtopicIds }, lesson.id)
        }
    }

    @Test
    fun koinUnitLinksOnlyToShippedPrerequisites() = runTest {
        val lessons = unit("unit_koin_and_dependency_injection_in_kmp").lessons.associateBy { it.id }

        assertEquals(
            listOf("lesson_one_place_that_knows_how_to_build", "lesson_from_one_dependency_to_a_graph"),
            lessons.getValue("lesson_the_koin_container_and_its_modules").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_one_instance_or_a_new_one",
                "lesson_a_dependency_should_be_visible",
                "lesson_asking_for_it_or_being_given_it",
            ),
            lessons.getValue("lesson_koin_definitions_and_reuse").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_scope_is_a_rule_owner_is_a_lifetime",
                "lesson_one_instance_or_a_new_one",
                "lesson_choosing_the_owner_by_lifetime",
            ),
            lessons.getValue("lesson_koin_scopes_and_their_owners").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_runtime_input_is_not_a_dependency",
                "lesson_viewmodel_lifetime_and_persistence",
                "lesson_asking_for_it_or_being_given_it",
                "lesson_two_dependencies_of_the_same_type",
            ),
            lessons.getValue("lesson_resolving_viewmodels_at_the_boundary").relatedLessonIds,
        )
        assertEquals(
            listOf(
                "lesson_one_place_that_knows_how_to_build",
                "lesson_when_an_interface_is_a_boundary",
                "lesson_dependency_inversion_in_practice",
            ),
            lessons.getValue("lesson_one_graph_across_platforms").relatedLessonIds,
        )

        val order = units().flatMap { it.lessons.map { lesson -> lesson.id } }
        lessons.values.forEach { lesson ->
            lesson.relatedLessonIds.forEach { related ->
                assertTrue(order.indexOf(related) < order.indexOf(lesson.id), "${lesson.id} -> $related")
            }
        }
    }

    @Test
    fun koinUnitProtectsModernCapabilitiesAndRepositoryEvidence() = runTest {
        val unit = unit("unit_koin_and_dependency_injection_in_kmp")

        fun textOf(lessonId: String): String = unit.lessons
            .single { it.id == lessonId }
            .sections
            .flatMap { it.blocks }
            .joinToString(" ") { block ->
                when (block) {
                    is LearningBlock.Paragraph -> block.text
                    is LearningBlock.BulletList -> block.items.joinToString(" ")
                    is LearningBlock.Callout -> block.text
                    is LearningBlock.Code -> block.code
                    is LearningBlock.Comparison -> (block.headers + block.rows.flatten()).joinToString(" ")
                }
            }

        unit.lessons.forEach { lesson ->
            assertEquals(
                listOf("CORE", "PRACTICAL", "SENIOR"),
                lesson.sections.map { it.depth.name },
                lesson.id,
            )
            assertTrue(lesson.sources.any { it.title.contains("Koin 4.2") }, lesson.id)
        }

        val container = textOf("lesson_the_koin_container_and_its_modules")
        assertTrue(container.contains("Koin itself is not the composition root"))
        assertTrue(container.contains("classic Kotlin DSL"))
        assertTrue(container.contains("annotations and a Compiler Plugin DSL"))
        assertTrue(container.contains("not configured"))

        val definitions = textOf("lesson_koin_definitions_and_reuse")
        assertTrue(definitions.contains("There is no Kotlin `object`"))
        assertTrue(definitions.contains("does **not** require a class called"))
        assertTrue(definitions.contains("service-locator shape"))

        val scopes = textOf("lesson_koin_scopes_and_their_owners")
        assertTrue(scopes.contains("owner that creates and closes the scope"))
        assertTrue(scopes.contains("no custom Koin scopes"))

        val viewModels = textOf("lesson_resolving_viewmodels_at_the_boundary")
        assertTrue(viewModels.contains("ViewModelStoreOwner"))
        assertTrue(viewModels.contains("parameters.get(0)"))
        assertTrue(viewModels.contains("20 `koinViewModel()` resolutions"))

        val multiplatform = textOf("lesson_one_graph_across_platforms")
        listOf(
            "startAndroidLocalDataGraph",
            "startIosLocalDataGraph",
            "startDesktopLocalDataGraph",
            "startWebLocalDataGraph",
            "AndroidAppPreferenceStorage",
            "IosAppPreferenceStorage",
            "JvmAppPreferenceStorage",
            "WebAppPreferenceStorage",
        ).forEach { claim -> assertTrue(multiplatform.contains(claim), claim) }

        val allText = unit.lessons.joinToString(" ") { textOf(it.id) }
        listOf("build.gradle", "plugins {", "koin-ksp-compiler", "@Singleton", "@KoinViewModel")
            .forEach { token -> assertFalse(allText.contains(token), token) }
    }

    @Test
    fun aPerformanceConceptStaysSupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U3-A in `docs/content/compose-units-2-6-plan.md`: the one active Question on
        // `compose_recomposition_performance` lives in the `performance` Topic and is reached
        // only as supporting context. Promoting it to primary would silently claim Unit 3
        // practice coverage that no Question actually provides, which E23-07 must still see.
        val lesson = unit("unit_recomposition").lessons.single { it.id == "lesson_recomposition_cost" }

        assertTrue(lesson.supportingSubtopicIds.contains("compose_recomposition_performance"))
        assertEquals(listOf("compose_recomposition"), lesson.primarySubtopicIds)
    }

    @Test
    fun kotlinLanguageConceptsStaySupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U4-C in `docs/content/compose-units-2-6-plan.md`: the Kotlin facts this Lesson
        // bridges are assessed in the `kotlin_language` Topic, and the Compose consequence of
        // read-only-but-mutable data is not assessed anywhere. Promoting any of these to
        // primary would claim Unit 4 practice coverage that no Question provides.
        val lesson = unit("unit_identity_keys_and_stability").lessons
            .single { it.id == "lesson_immutability_vs_stability" }

        assertEquals(listOf("compose_stability"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("kotlin_data_classes", "kotlin_equality", "kotlin_collections", "kotlin_variables"),
            ),
        )
    }

    @Test
    fun architectureAndPerformanceConceptsStaySupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U5-B in `docs/content/compose-units-2-6-plan.md`: this Lesson argues where work
        // belongs, so it leans on `main_thread_performance`, `layered_architecture` and
        // `use_cases` — all owned by other Topics and all assessed there, if at all, in terms
        // that have nothing to do with composition. Promoting any of them would hand Unit 5
        // practice questions it does not teach and would hide the gap E23-07 has to see.
        val lesson = unit("unit_derived_state_and_expensive_work").lessons
            .single { it.id == "lesson_work_outside_composition" }

        assertEquals(listOf("compose_derived_state"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("main_thread_performance", "layered_architecture", "use_cases"),
            ),
        )
    }

    @Test
    fun flowConceptsStaySupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U6-A in `docs/content/compose-units-2-6-plan.md`: the single active Question on
        // `compose_snapshot_system` identifies an API name, and nothing assesses the observation
        // model this Unit is built on. The Flow concepts the second Lesson bridges are assessed
        // in the `async_reactive` Topic in terms that have nothing to do with Compose, and
        // `compose_side_effects` belongs to the Effects Unit that E23 does not author. Promoting
        // any of them would hand Unit 6 practice it does not teach and would hide the gap that
        // E23-07 still has to close.
        val lesson = unit("unit_snapshot_fundamentals").lessons
            .single { it.id == "lesson_snapshot_flow" }

        assertEquals(listOf("compose_snapshot_system"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("flow_fundamentals", "hot_vs_cold_streams", "compose_side_effects"),
            ),
        )
    }

    @Test
    fun lifecycleAndPerformanceConceptsStaySupportingInTheCoroutinesUnit() = runTest {
        // `docs/content/coroutines-flow-units-1-6-plan.md`: no E24 Lesson takes a
        // non-`async_reactive` Subtopic as primary, which is what keeps the lifecycle,
        // performance and Android-platform bridges out of this Unit's practice. GAP-U1-B
        // in particular depends on it — `performance_coroutine_scope_leak` assesses the
        // ownership failure from the leak side, and promoting `coroutine_leaks` would
        // claim Unit practice that no `async_reactive` Question provides and would hide
        // the gap E24-08 still has to close.
        val lesson = unit("unit_coroutines_and_structured_concurrency").lessons
            .single { it.id == "lesson_coroutine_scope_ownership" }

        assertEquals(listOf("coroutine_scope"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("coroutine_jobs", "lifecycle_coroutines", "coroutine_leaks", "viewmodel_lifecycle"),
            ),
        )
    }

    @Test
    fun kotlinAndPlatformConceptsStaySupportingInTheSuspensionLesson() = runTest {
        // The same rule at the entry point of the coroutines path: the thread, JVM and
        // main-thread facts this Lesson bridges are assessed in their own Topics, and
        // dispatchers are named here only to be deferred to the Unit that owns them.
        val lesson = unit("unit_coroutines_and_structured_concurrency").lessons
            .single { it.id == "lesson_suspension_and_blocking" }

        assertEquals(listOf("coroutine_fundamentals"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("coroutine_dispatchers", "jvm_fundamentals", "android_main_thread"),
            ),
        )
    }

    @Test
    fun aLessonBridgesToAnotherTopicsSupportingConcept() = runTest {
        // `unidirectional_data_flow` is owned by the architecture Topic while the Unit is
        // browsed under `android_ui`; cross-Topic support is a product rule, not a defect.
        assertTrue(
            unit("unit_thinking_in_compose").lessons
                .single { it.id == "lesson_state_down_events_up" }
                .supportingSubtopicIds
                .contains("unidirectional_data_flow"),
        )

        // The same rule, exercised where the bridge spans three Topics at once: saved state
        // and process death are `lifecycle_navigation` concepts a Compose Lesson has to lean
        // on without reproducing that curriculum.
        assertTrue(
            unit("unit_state_and_state_ownership").lessons
                .single { it.id == "lesson_remember_saveable" }
                .supportingSubtopicIds
                .containsAll(listOf("saved_state", "configuration_changes", "process_death")),
        )
    }

    @Test
    fun everyActiveLessonIsStudyableAndSourced() = runTest {
        units().flatMap { it.lessons }.forEach { lesson ->
            assertEquals(ContentStatus.ACTIVE, lesson.status, lesson.id)
            assertTrue(lesson.sections.isNotEmpty(), lesson.id)
            assertTrue(lesson.sources.isNotEmpty(), lesson.id)
        }
    }

    @Test
    fun relatedLessonReferencesResolveWithinTheShippedDocument() = runTest {
        // Forward links are invalid until their target ships, so this is what stops a Lesson
        // pointing at a Unit that is still only planned.
        val lessons = units().flatMap { it.lessons }
        val lessonIds = lessons.map { it.id }.toSet()

        lessons.forEach { lesson ->
            lesson.relatedLessonIds.forEach { relatedId ->
                assertTrue(relatedId in lessonIds, "${lesson.id} -> $relatedId")
            }
        }
    }

    @Test
    fun bundledLearningCurriculumValidatesAgainstTheBundledBaseCurriculum() = runTest {
        val errors = LearningCurriculumValidator().validate(
            learningCurriculum = BundledLearningCurriculumSource.load(),
            curriculum = BundledCurriculumSource.load(),
        )

        assertEquals(emptyList(), errors)
    }
}
