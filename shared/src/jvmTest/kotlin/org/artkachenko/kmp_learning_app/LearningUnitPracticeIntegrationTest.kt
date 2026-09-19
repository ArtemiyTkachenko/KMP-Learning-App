package org.artkachenko.kmp_learning_app

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessment
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessmentResult
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingUiState
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.artkachenko.kmp_learning_app.lesson_study.ContinueLearningTarget
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.data.local.lesson_study.repository.LocalLessonStudyRepository
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitViewModel
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailUiState
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.artkachenko.kmp_learning_app.topic_study.topics.ContinueLearningUiModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserUiState
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultEvent
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultUiState
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.DefaultPracticeQuestionCount
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderEvent
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderTarget
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderUiState
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeScopeKind
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * E21-06: the shipped Learning Unit practised through the production graph.
 *
 * Every layer already has its own proof — the derivation in `PracticeTargetResolverTest`, the
 * builder's states in `PracticeBuilderViewModelTest`, the multi-Subtopic selection and persistence
 * in the E21-05 suites. What none of them can show is whether the *authored* Unit, the *authored*
 * Question bank, and the real selection and persistence path still agree: whether pressing
 * "Practice this unit" on the Unit that actually ships reaches Questions that actually exist.
 *
 * So neither side is a fixture here. The learning document is the bundled one and the curriculum is
 * imported from the bundled one through the real importer, which makes this test sensitive to
 * content: retiring a Lesson, re-pointing a primary concept, or deprecating the Questions behind
 * one is meant to fail here rather than pass silently.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LearningUnitPracticeIntegrationTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun existingLearnerTraversesTheExpansionWithLiveParentProgressAndDurableIdentities() =
        runUnitPracticeTest {
            // These published identities predate the expansion. Seed before opening new content.
            val publishedIds = setOf(
                "lesson_declarative_ui", "lesson_composable_execution", "lesson_state_down_events_up",
            )
            publishedIds.forEach { studyRepository.markStudied(it) }
            val originalRecords = studyRepository.getStudiedLessons()
            val units = BundledLearningContentRepository().getActiveUnitsByTopic("android_ui")
            assertEquals(publishedIds, units.first().lessons.map { it.id }.toSet())
            assertEquals(listOf(5, 3, 5, 3, 2, 4, 4, 4, 3, 4, 3), units.drop(1).map { it.lessons.size })

            // Keep the real parent ViewModels alive throughout every child mutation.
            val browser = browser()
            val topic = topic("android_ui")
            val parents = units.associate { it.id to unit(it.id) }
            suspend fun awaitTopic(count: Int) {
                topic.uiState.await { state ->
                    state is TopicDetailUiState.Content &&
                        (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                        StudyProgressSummary.Progress(count, 43)
                }
            }
            suspend fun awaitNext(unitId: String, lessonId: String) {
                browser.uiState.await { state ->
                    state is TopicBrowserUiState.Content &&
                        (state.continueLearning as? ContinueLearningUiModel.Next)?.target ==
                        ContinueLearningTarget(unitId, lessonId)
                }
            }
            awaitTopic(3)
            var studiedCount = 3
            units.drop(1).forEach { unit ->
                unit.lessons.forEachIndexed { index, lesson ->
                    awaitNext(unit.id, lesson.id)
                    val reader = lesson(unit.id, lesson.id)
                    reader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.isStudied == false
                    }
                    assertEquals(studiedCount, studyRepository.getStudiedLessons().size)
                    reader.toggleStudied()
                    reader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.let {
                                it.isStudied && !it.isPending
                            } == true
                    }
                    studiedCount += 1
                    awaitTopic(studiedCount)
                    parents.getValue(unit.id).uiState.await { state ->
                        state is LearningUnitUiState.Content &&
                            (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                            StudyProgressSummary.Progress(index + 1, unit.lessons.size)
                    }
                }
            }
            // Continue Learning walks the whole authored document rather than one Topic, so
            // exhausting `android_ui` hands the learner across to the next authored Unit —
            // browsed under a different home Topic. Study all authored coroutines Units
            // before expecting the exhausted outcome.
            val coroutinesUnits = BundledLearningContentRepository().getActiveUnitsByTopic("async_reactive")
            assertEquals(
                listOf(
                    "unit_coroutines_and_structured_concurrency",
                    "unit_context_dispatchers_and_concurrency",
                    "unit_cancellation_failure_and_coordination",
                    "unit_flow_fundamentals",
                    "unit_flow_composition_timing_and_failure",
                    "unit_stateflow_sharedflow_and_hot_streams",
                ),
                coroutinesUnits.map { it.id },
            )
            assertEquals(listOf(5, 4, 5, 5, 5, 5), coroutinesUnits.map { it.lessons.size })
            val coroutinesTopic = topic("async_reactive")
            val coroutinesParents = coroutinesUnits.associate { it.id to unit(it.id) }
            suspend fun awaitCoroutinesTopic(count: Int) {
                coroutinesTopic.uiState.await { state ->
                    state is TopicDetailUiState.Content &&
                        (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                        StudyProgressSummary.Progress(count, 29)
                }
            }
            awaitCoroutinesTopic(0)
            var coroutinesStudiedCount = 0
            coroutinesUnits.forEach { coroutinesUnit ->
                coroutinesUnit.lessons.forEachIndexed { index, lesson ->
                    awaitNext(coroutinesUnit.id, lesson.id)
                    val crossTopicReader = lesson(coroutinesUnit.id, lesson.id)
                    crossTopicReader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.isStudied == false
                    }
                    crossTopicReader.toggleStudied()
                    crossTopicReader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.let {
                                it.isStudied && !it.isPending
                            } == true
                    }
                    coroutinesStudiedCount += 1
                    awaitCoroutinesTopic(coroutinesStudiedCount)
                    coroutinesParents.getValue(coroutinesUnit.id).uiState.await { state ->
                        state is LearningUnitUiState.Content &&
                            (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                            StudyProgressSummary.Progress(index + 1, coroutinesUnit.lessons.size)
                    }
                }
            }
            // E26-02 added a third home Topic, so the same handover happens again: exhausting
            // the coroutines Units leads into the architecture Unit rather than to Complete.
            val architectureUnits = BundledLearningContentRepository().getActiveUnitsByTopic("architecture")
            assertEquals(
                listOf(
                    "unit_architecture_responsibilities_and_boundaries",
                    "unit_screen_state_holders_and_ui_state",
                    "unit_repositories_and_data_ownership",
                    "unit_domain_logic_and_dependency_direction",
                    "unit_responsibility_models_mvp_mvvm_mvi",
                    "unit_state_events_lifetime_and_selection",
                ),
                architectureUnits.map { it.id },
            )
            // The synthesis Unit carries four Lessons rather than five: it adds no mechanism and
            // has four decisions in it, which `docs/content/architecture-units-1-6-plan.md`
            // records as derived rather than as a quota.
            assertEquals(listOf(5, 5, 5, 5, 5, 4), architectureUnits.map { it.lessons.size })
            val architectureTopic = topic("architecture")
            val architectureParents = architectureUnits.associate { it.id to unit(it.id) }
            val architectureLessonCount = architectureUnits.sumOf { it.lessons.size }
            suspend fun awaitArchitectureTopic(count: Int) {
                architectureTopic.uiState.await { state ->
                    state is TopicDetailUiState.Content &&
                        (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                        StudyProgressSummary.Progress(count, architectureLessonCount)
                }
            }
            awaitArchitectureTopic(0)
            var architectureStudiedCount = 0
            architectureUnits.forEach { architectureUnit ->
                architectureUnit.lessons.forEachIndexed { index, lesson ->
                    awaitNext(architectureUnit.id, lesson.id)
                    val architectureReader = lesson(architectureUnit.id, lesson.id)
                    architectureReader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.isStudied == false
                    }
                    architectureReader.toggleStudied()
                    architectureReader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.let {
                                it.isStudied && !it.isPending
                            } == true
                    }
                    architectureStudiedCount += 1
                    awaitArchitectureTopic(architectureStudiedCount)
                    architectureParents.getValue(architectureUnit.id).uiState.await { state ->
                        state is LearningUnitUiState.Content &&
                            (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                            StudyProgressSummary.Progress(index + 1, architectureUnit.lessons.size)
                    }
                }
            }
            // E27-02 added a fourth home Topic, and the handover happens once more: exhausting
            // the architecture Units leads into the dependency-injection Units, not to Complete.
            // E27-03 added the second of those Units, E27-04 the third, E27-05 the fourth and
            // E27-06 the fifth and E27-07 the sixth. The traversal walks all six without a
            // production special case.
            val diUnits = BundledLearningContentRepository().getActiveUnitsByTopic("dependency_injection")
            assertEquals(
                listOf(
                    "unit_dependency_injection_as_object_construction",
                    "unit_object_graphs_lifetimes_and_scopes",
                    "unit_dagger_compile_time_object_graphs",
                    "unit_hilt_android_lifecycle_integration",
                    "unit_koin_and_dependency_injection_in_kmp",
                    "unit_choosing_a_dependency_injection_strategy",
                ),
                diUnits.map { it.id },
            )
            // The Unit plan derives these counts from separately learnable decisions rather than
            // assigning a quota: six generic foundations, six graph decisions, seven Dagger
            // encodings, six Android/Hilt decisions, five Koin/KMP decisions, then four
            // strategy decisions that synthesize the earlier Units.
            assertEquals(listOf(6, 6, 7, 6, 5, 4), diUnits.map { it.lessons.size })
            val diTopic = topic("dependency_injection")
            val diParents = diUnits.associate { it.id to unit(it.id) }
            val diLessonCount = diUnits.sumOf { it.lessons.size }
            suspend fun awaitDiTopic(count: Int) {
                diTopic.uiState.await { state ->
                    state is TopicDetailUiState.Content &&
                        (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                        StudyProgressSummary.Progress(count, diLessonCount)
                }
            }
            awaitDiTopic(0)
            var diStudiedCount = 0
            diUnits.forEach { diUnit ->
                diUnit.lessons.forEachIndexed { index, lesson ->
                    awaitNext(diUnit.id, lesson.id)
                    val diReader = lesson(diUnit.id, lesson.id)
                    diReader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.isStudied == false
                    }
                    diReader.toggleStudied()
                    diReader.uiState.await { state ->
                        state is LearningLessonUiState.Content &&
                            (state.studyState as? StudyProgressUiState.Available)?.value?.let {
                                it.isStudied && !it.isPending
                            } == true
                    }
                    diStudiedCount += 1
                    awaitDiTopic(diStudiedCount)
                    diParents.getValue(diUnit.id).uiState.await { state ->
                        state is LearningUnitUiState.Content &&
                            (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                            StudyProgressSummary.Progress(index + 1, diUnit.lessons.size)
                    }
                }
            }
            browser.uiState.await { state ->
                state is TopicBrowserUiState.Content && state.continueLearning == ContinueLearningUiModel.Complete
            }
            // The `android_ui` Topic's own progress is unaffected by Units in another Topic.
            awaitTopic(43)
            val firstCoroutinesUnit = coroutinesUnits.first()
            val lastLessonInFirstCoroutinesUnit = firstCoroutinesUnit.lessons.last()
            val coroutinesReader = lesson(firstCoroutinesUnit.id, lastLessonInFirstCoroutinesUnit.id)
            coroutinesReader.toggleStudied()
            coroutinesReader.uiState.await { state ->
                state is LearningLessonUiState.Content &&
                    (state.studyState as? StudyProgressUiState.Available)?.value?.let {
                        !it.isStudied && !it.isPending
                    } == true
            }
            awaitCoroutinesTopic(28)
            coroutinesParents.getValue(firstCoroutinesUnit.id).uiState.await { state ->
                state is LearningUnitUiState.Content &&
                    (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                    StudyProgressSummary.Progress(4, 5)
            }
            awaitNext(firstCoroutinesUnit.id, lastLessonInFirstCoroutinesUnit.id)
            coroutinesReader.toggleStudied()
            awaitCoroutinesTopic(29)
            browser.uiState.await { state ->
                state is TopicBrowserUiState.Content && state.continueLearning == ContinueLearningUiModel.Complete
            }
            val earlierUnit = units[1]
            val earlierLesson = earlierUnit.lessons.last()
            val reader = lesson(earlierUnit.id, earlierLesson.id)
            reader.uiState.await { state ->
                state is LearningLessonUiState.Content &&
                    (state.studyState as? StudyProgressUiState.Available)?.value?.isStudied == true
            }
            reader.toggleStudied()
            awaitNext(earlierUnit.id, earlierLesson.id)
            awaitTopic(42)
            parents.getValue(earlierUnit.id).uiState.await { state ->
                state is LearningUnitUiState.Content &&
                    (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                    StudyProgressSummary.Progress(4, 5)
            }
            val rebuilt = LocalLessonStudyRepository(database)
            assertFalse(rebuilt.isStudied(earlierLesson.id))
            // 43 `android_ui` Lessons, 29 in the coroutines and Flow Units, 29 in the six
            // architecture Units and 34 in the six dependency-injection Units, less the one that
            // was just un-studied.
            assertEquals(134, rebuilt.getStudiedLessons().size)
            assertEquals(originalRecords, rebuilt.getStudiedLessons().filter { it.lessonId in publishedIds })
            assertEquals(0, attemptCount())
            assertEquals(null, assertIs<TopicBrowserUiState.Content>(browser.uiState.value).continueStudying)
        }

    @Test
    fun expandedUnitsConfigureOnlyTheirPrimaryConceptsAndDeduplicateProductionQuestions() =
        runUnitPracticeTest {
            val expected = linkedMapOf(
                // E25-08 added two Compose-state Questions for Unit 8's collection reasoning and
                // two `compose_state_hoisting` Questions for Unit 7's screen-scale ownership
                // reasoning. Both Subtopics are also this shipped E23 Unit's, so its pool grows
                // from 4 to 8. The two hoisting Questions apply the tests this Unit already
                // teaches; the two collection Questions are premature here and are recorded as a
                // documented consequence of the shared Subtopic in the E25 plan.
                "unit_state_and_state_ownership" to (setOf("compose_state", "compose_state_hoisting") to 8),
                "unit_recomposition" to (setOf("compose_recomposition") to 3),
                "unit_identity_keys_and_stability" to (setOf("compose_identity_keys", "compose_stability") to 6),
                "unit_derived_state_and_expensive_work" to (setOf("compose_derived_state") to 3),
                "unit_snapshot_fundamentals" to (setOf("compose_snapshot_system") to 4),
                // E25-03, then E25-08. All four Lessons apply `compose_state`; Flow, lifecycle
                // and KMP concepts remain supporting context. E25-08 closed GAP-U8-A and the
                // combined GAP-U8-B/GAP-U8-C, so the three E23 Compose-state Questions are now
                // joined by two that assess this Unit's own conversion and lifetime reasoning.
                "unit_observable_state_collection" to (setOf("compose_state") to 5),
                // E25-04, then E25-08. The four Lessons share one effect concept, so Unit
                // practice is the whole `compose_side_effects` pool. E25-08 raised that pool
                // from 2 to 10; three of the new Questions assess this Unit's own reasoning and
                // the rest belong to Units 10-12, which is the shared-pool consequence the E25
                // plan records rather than a mapping that may be routed around.
                "unit_effect_lifecycle_and_launched_effect" to (
                    setOf("compose_side_effects") to 10
                ),
                // E25-05, then E25-08. Latest-value reading and event-owned triggering remain
                // one `compose_side_effects` practice scope; supporting coroutine and lifecycle
                // mappings must not expand it. GAP-U10-A now has a Question, and GAP-U10-C is
                // closed together with GAP-U12-A by the required-lifetime Question.
                "unit_latest_values_and_event_driven_work" to (
                    setOf("compose_side_effects") to 10
                ),
                // E25-06, then E25-08. Registration ownership, outward publication, state
                // producers and the adapter-placement decision are four problems, one Subtopic,
                // so Unit practice stays the shared pool. GAP-U11-A and GAP-U11-C now have
                // Questions; GAP-U11-B and GAP-U11-D are deferred in the E25 plan.
                "unit_cleanup_synchronization_and_producers" to (
                    setOf("compose_side_effects") to 10
                ),
                // E25-07, then E25-08. The synthesis Unit takes the same sole primary concept,
                // so all four effect Units still receive one identical pool. Equal counts here
                // are the assertion that the pool really is identical, which is the structural
                // limitation E25-01 recorded rather than something a re-mapping may hide.
                "unit_production_ui_effects_and_selection" to (
                    setOf("compose_side_effects") to 10
                ),
                // E24-02. Five Lessons, five distinct primary concepts, and the cross-Topic
                // bridges into lifecycle, performance, Android platform and Kotlin stay out
                // of the scope entirely — which is what `supportingOnly` below proves.
                // E24-08 re-mapped two Questions out of this Unit, because their reasoning
                // is Unit 3's: `parent_cancellation_propagates_children` to
                // `coroutine_cancellation` and `coroutine_async_exception_surfaces_at_await`
                // to `coroutine_exceptions`. The re-mapping routing test below is what keeps
                // that from drifting back.
                "unit_coroutines_and_structured_concurrency" to (
                    setOf(
                        "coroutine_fundamentals",
                        "coroutine_builders",
                        "coroutine_jobs",
                        "coroutine_scope",
                        "structured_concurrency",
                    ) to 8
                ),
                "unit_context_dispatchers_and_concurrency" to (
                    setOf(
                        "coroutine_context",
                        "coroutine_dispatchers",
                        "coroutine_context_switching",
                        "coroutine_parallelism",
                    ) to 9
                ),
                // E24-04. `coroutine_cancellation` is primary in two of the five Lessons,
                // and `coroutine_parallelism` is shared with Unit 2 — a Subtopic taught at
                // more than one depth still contributes its questions exactly once.
                "unit_cancellation_failure_and_coordination" to (
                    setOf(
                        "coroutine_cancellation",
                        "coroutine_exceptions",
                        "coroutine_supervision",
                        "coroutine_parallelism",
                    ) to 12
                ),
                // E24-05. `flow_fundamentals` is primary in three of the five Lessons, so
                // five Lessons practise three concepts. The architecture, Kotlin, lifecycle
                // and performance bridges the Unit leans on stay out of the scope, which is
                // what `supportingOnly` below proves.
                "unit_flow_fundamentals" to (
                    setOf(
                        "flow_fundamentals",
                        "flow_collection",
                        "flow_context",
                    ) to 7
                ),
                // E24-06. `flow_operators` is primary in three of the five Lessons —
                // filtering, combining and flattening are three depths of one concept —
                // so five Lessons practise three concepts. `stateflow`, the Kotlin
                // bridges and the architecture bridge stay out of the scope, which is
                // what `supportingOnly` below proves. E24-08 closed GAP-U5-A to
                // GAP-U5-D here: ordering under flattening, the cadence/quiet-period
                // choice, what `onCompletion` observes, and what retrying costs.
                "unit_flow_composition_timing_and_failure" to (
                    setOf(
                        "flow_operators",
                        "flow_buffering",
                        "flow_errors",
                    ) to 11
                ),
                // E24-07. `hot_vs_cold_streams` is primary in the first and last Lessons,
                // so five Lessons practise four concepts. `kotlin_equality`,
                // `state_ownership`, `lifecycle_coroutines` and `flow_buffering` stay out
                // of the scope, which is what `supportingOnly` below proves; the `livedata`
                // Question that states StateFlow conflation most clearly is unmapped by
                // E24 and so is still not Unit practice, and neither is
                // `durable_state_vs_one_off_event` in `architecture`. E24-08 closed
                // GAP-U6-A to GAP-U6-F inside `async_reactive` instead.
                "unit_stateflow_sharedflow_and_hot_streams" to (
                    setOf(
                        "hot_vs_cold_streams",
                        "stateflow",
                        "sharedflow",
                        "flow_sharing",
                    ) to 11
                ),
                // E26-03. Unlike Unit 1, this Unit joins the table: both of its primary
                // concepts hold ACTIVE Questions, so the identity the loop asserts between
                // concepts and resolved Subtopics holds. Four Questions arrive through
                // `state_ownership` and one through `unidirectional_data_flow`.
                "unit_screen_state_holders_and_ui_state" to (
                    setOf("state_ownership", "unidirectional_data_flow") to 10
                ),
                // E26-04. `repository_pattern` is primary in three of the five Lessons and
                // the closing Lesson declares two primaries, so five Lessons practise four
                // concepts. All four hold ACTIVE Questions, so the Unit joins the table;
                // which Questions arrive, and the two it shares with the foundations Unit,
                // are pinned by the bespoke test below.
                "unit_repositories_and_data_ownership" to (
                    setOf(
                        "repository_pattern",
                        "single_source_of_truth",
                        "layered_architecture",
                        "error_modeling",
                    ) to 10
                ),
                // E26-05. `use_cases` is primary in two Lessons and `dependency_direction` in
                // two, and the inversion Lesson declares two primaries, so five Lessons
                // practise four concepts. All four hold ACTIVE Questions. The two shared with
                // the foundations Unit are pinned by the bespoke test below.
                "unit_domain_logic_and_dependency_direction" to (
                    setOf(
                        "use_cases",
                        "clean_architecture",
                        "dependency_direction",
                        "interface_boundaries",
                    ) to 9
                ),
            )
            val content = BundledLearningContentRepository()
            expected.forEach { (unitId, expectation) ->
                val (concepts, count) = expectation
                val unit = assertNotNull(content.getUnitById(unitId))
                val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
                val state = builder.settled()
                assertEquals(unit.title, state.scope.name)
                // Availability is the full filtered pool, independent of the chosen session
                // length, so the builder can present only truthful count choices.
                assertEquals(
                    count,
                    assertIs<PracticeAvailability.Available>(state.availability).eligibleQuestionCount,
                )
                // Select the exact dynamic pool option, so what follows sees the whole pool.
                builder.selectQuestionCount(count)
                builder.settled()
                val config = builder.start()
                assertEquals(AssessmentScope.Subtopics(concepts), config.scope, unitId)
                val questions = selectedQuestions(config)
                assertEquals(count, questions.size, unitId)
                assertEquals(count, questions.map { it.id }.toSet().size, unitId)
                assertEquals(concepts, questions.map { it.subtopicId }.toSet(), unitId)
                val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
                assertTrue(questions.none { it.subtopicId in supportingOnly }, unitId)
            }
            assertEquals(0, attemptCount())
        }

    /**
     * E25-09: the four effect Units resolve one pool, and it is the *same* pool.
     *
     * The expectation table above states a count of ten for each of Units 9-12, and equal counts
     * are not the claim the E25 plan actually makes. Four Units whose Lessons all take
     * `compose_side_effects` as their sole primary concept receive one identical set of Questions,
     * and a learner finishing Unit 9 is therefore handed material Units 10-12 have not taught yet.
     * That is an accepted structural limitation of the taxonomy's granularity rather than a defect,
     * and the way to keep an accepted limitation honest is to assert it: if a later change splits
     * the concept, re-maps a Lesson or files a Question elsewhere, the identity below fails and the
     * limitation has to be re-stated rather than quietly drifting.
     *
     * The same pass states the two things a count cannot: that each of E25-08's twelve Questions
     * reaches the Unit whose material it assesses, and that the one DEPRECATED Question sitting on
     * an E25 primary concept reaches none of them.
     */
    @Test
    fun theFourEffectUnitsResolveOneIdenticalPoolAndExcludeRetiredQuestions() = runUnitPracticeTest {
        suspend fun reach(unitId: String): Set<String> {
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            builder.settled()
            builder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(builder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            builder.settled()
            return selectedQuestions(builder.start()).map { it.id }.toSet()
        }

        val effectUnits = listOf(
            "unit_effect_lifecycle_and_launched_effect",
            "unit_latest_values_and_event_driven_work",
            "unit_cleanup_synchronization_and_producers",
            "unit_production_ui_effects_and_selection",
        )
        val pools = effectUnits.associateWith { reach(it) }
        val shared = pools.getValue(effectUnits.first())
        pools.forEach { (unitId, pool) ->
            assertEquals(
                shared,
                pool,
                "$unitId no longer receives the same pool as ${effectUnits.first()}.",
            )
        }
        // Without this the equality above would hold just as well for four empty pools.
        assertTrue(shared.size > 1, "The shared effect pool reached at most one Question.")

        // Each of E25-08's twelve Questions, in the Unit whose reasoning it assesses. The effect
        // Questions are asserted against the shared pool because that is what all four receive.
        listOf(
            "compose_body_work_has_no_lifecycle_owner",
            "compose_effect_key_equality_decides_restart",
            "compose_constant_effect_key_is_a_lifetime_claim",
            "compose_current_callback_without_restarting_the_effect",
            "compose_missing_on_dispose_accumulates_listeners",
            "compose_produce_state_key_change_keeps_the_last_value",
            "compose_required_lifetime_exceeds_the_composition",
            "compose_durable_flag_repeats_a_transient_effect",
        ).forEach { assertContains(shared, it) }

        val screenState = reach("unit_production_screen_state_and_udf")
        listOf(
            "compose_screen_state_lowest_sensible_owner",
            "compose_over_hoisted_ui_element_state_cost",
        ).forEach { assertContains(screenState, it) }

        val collection = reach("unit_observable_state_collection")
        listOf(
            "compose_state_flow_value_read_is_not_observation",
            "compose_lifecycle_collection_stops_the_collector_not_the_producer",
        ).forEach { assertContains(collection, it) }

        // The same two collection Questions also enter the shipped E23 state Unit, because
        // `compose_state` is that Unit's primary concept too and E25 invented no new Subtopic to
        // route around it. E23 does not teach Flow collection, so this is the second accepted
        // structural limitation the plan records — asserted here so it cannot be lost silently.
        val e23State = reach("unit_state_and_state_ownership")
        listOf(
            "compose_state_flow_value_read_is_not_observation",
            "compose_lifecycle_collection_stops_the_collector_not_the_producer",
        ).forEach { assertContains(e23State, it) }

        // A DEPRECATED Question stays out of every pool its Subtopic would otherwise reach.
        listOf(screenState, collection, e23State, shared).forEach { pool ->
            assertFalse(
                "compose_state_001" in pool,
                "A DEPRECATED Question reached Unit practice.",
            )
        }
        assertEquals(0, attemptCount())
    }

    /**
     * E24-08: the three mapping corrections, asserted as routing rather than as metadata.
     *
     * A Subtopic mapping is only visible to a learner through which Unit's practice a Question
     * appears in, so re-mapping one is a behaviour change and belongs here. Each assertion names
     * the reasoning that moved: cancellation propagation and dropped-`Deferred` failure are Unit 3
     * material that Unit 1 used to hand out, `runBlocking` is builder reasoning that sat on the
     * suspension concept, and the new hot-stream Questions must reach Unit 6 while the
     * architecture and lifecycle Questions that discuss the same subject stay outside it.
     */
    @Test
    fun unitPracticeRoutesReMappedQuestionsToTheUnitThatTeachesThem() = runUnitPracticeTest {
        suspend fun reach(unitId: String): Set<String> {
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            builder.settled()
            builder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(builder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            builder.settled()
            return selectedQuestions(builder.start()).map { it.id }.toSet()
        }

        val unitOne = reach("unit_coroutines_and_structured_concurrency")
        val unitThree = reach("unit_cancellation_failure_and_coordination")
        val unitSix = reach("unit_stateflow_sharedflow_and_hot_streams")

        // Unit 1 no longer practises Unit 3's cancellation and failure reasoning.
        listOf(
            "parent_cancellation_propagates_children",
            "coroutine_async_exception_surfaces_at_await",
        ).forEach { questionId ->
            assertFalse(questionId in unitOne, "Unit 1 still practises $questionId")
            assertContains(unitThree, questionId)
        }

        // `runBlocking` moved within Unit 1, so the Unit is unaffected and the concept is not.
        assertContains(unitOne, "coroutine_run_blocking_main_thread")
        assertEquals(
            setOf("coroutine_run_blocking_main_thread", "launch_vs_async_unawaited_result"),
            selectedQuestions(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic("coroutine_builders"),
                    questionCount = 20,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.ALL,
                ),
            ).map { it.id }.toSet(),
        )

        // Unit 6 gains the new stream-semantics Questions; the Questions on the same subject that
        // E24 maps as supporting, or does not map at all, stay out.
        listOf(
            "stateflow_vs_sharedflow_current_value",
            "shared_flow_try_emit_true_is_not_delivery",
            "flow_sharing_policy_and_replay_expiration",
            "hot_sharing_changes_production_not_retention",
            "stream_choice_cannot_supply_a_delivery_guarantee",
        ).forEach { assertContains(unitSix, it) }
        listOf(
            "durable_state_vs_one_off_event",
            "live_data_vs_state_flow_ui_state",
            "viewmodel_scope_cleared_cancellation",
            "performance_coroutine_scope_leak",
        ).forEach { questionId ->
            assertFalse(questionId in unitSix, "A supporting-only Question reached Unit 6: $questionId")
            assertFalse(questionId in unitOne, "A supporting-only Question reached Unit 1: $questionId")
        }
        assertEquals(0, attemptCount())
    }

    /**
     * E26-08: the first `architecture` Unit, now that every primary concept reaches a Question.
     *
     * E26-02 could not put this Unit in the expectation table, because that table asserts
     * `concepts == questions.map { it.subtopicId }.toSet()` and `architecture_tradeoffs` held no
     * ACTIVE Question at all. E26-08 closed GAP-U1-E and GAP-U6-C, so the identity now holds and
     * the test asserts it directly rather than asserting an absence.
     *
     * The exact ids stay pinned, because four of the eleven belong semantically to later Units and
     * reach this one through a shared primary concept: two data-layer Questions through
     * `layered_architecture`, and the whole-feature proportionality Question through
     * `architecture_tradeoffs`, which `docs/content/architecture-units-1-6-plan.md` records as an
     * accepted routing consequence rather than something a re-mapping may quietly repair.
     */
    @Test
    fun theArchitectureFoundationsUnitPractisesItsPrimaryConceptsAndNothingElse() = runUnitPracticeTest {
        val unitId = "unit_architecture_responsibilities_and_boundaries"
        val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
        val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
        val settled = builder.settled()

        assertEquals(unit.title, settled.scope.name)
        val available = assertIs<PracticeAvailability.Available>(settled.availability)
        assertEquals(11, available.eligibleQuestionCount)
        builder.selectQuestionCount(available.eligibleQuestionCount)
        builder.settled()

        val config = builder.start()
        val concepts = setOf(
            "separation_of_concerns",
            "dependency_direction",
            "interface_boundaries",
            "layered_architecture",
            "architecture_tradeoffs",
        )
        assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

        val questions = selectedQuestions(config)
        assertEquals(
            setOf(
                "separation_of_concerns_001",
                "architecture_package_move_changes_nothing",
                "separation_of_concerns_reason_to_change_test",
                "dependency_direction_domain_framework_types",
                "dependency_direction_callback_does_not_reverse_it",
                "architecture_interface_boundary_ownership",
                "interface_with_one_implementation_is_not_a_boundary",
                "architecture_paging_ownership",
                "dto_entity_domain_model_boundary",
                "added_layer_must_isolate_an_independent_change",
                "smallest_structure_that_satisfies_the_requirements",
            ),
            questions.map { it.id }.toSet(),
        )
        // Every primary concept now reaches at least one Question, which is what E26-08 changed.
        assertEquals(concepts, questions.map { it.subtopicId }.toSet())
        assertTrue(questions.any { it.subtopicId == "architecture_tradeoffs" })

        // Supporting concepts never broaden a Unit's practice. `solid` is the one that matters:
        // E26-01 made it supporting-only by design, and its single ACTIVE Question must therefore
        // reach no E26 Unit even though three Lessons name the acronym.
        val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
        assertTrue(questions.none { it.subtopicId in supportingOnly })
        assertFalse(
            "architecture_solid_dependency_substitution" in questions.map { it.id }.toSet(),
            "A supporting-only SOLID Question reached the architecture Unit's practice.",
        )
        assertEquals(0, attemptCount())
    }

    /**
     * E26-03: the state-holder Unit's pool, and the one routing limitation inside it.
     *
     * The expectation table above already asserts the count, the scope and the supporting-only
     * exclusion. What it cannot state is *which* Questions arrive and why four of them are about
     * material this Unit deliberately does not teach. `state_ownership` is a primary concept of
     * this Unit and of the closing synthesis Unit, so every Question written for the later Unit's
     * reasoning reaches this one too: `durable_state_vs_one_off_event`, the re-mapped
     * `architecture_ui_event_consumption`, and the two Questions E26-08 authored for GAP-U6-A and
     * GAP-U6-B. `docs/content/architecture-units-1-6-plan.md` records that as a taxonomy limitation
     * only a Subtopic split would remove, and E26-08 accepted it deliberately rather than leaving
     * the synthesis Unit unassessed. It is asserted here so a later re-map has to re-state the
     * limitation rather than silently repairing it.
     */
    @Test
    fun theStateHolderUnitPractisesItsTwoPrimaryConceptsIncludingOneLaterUnitsQuestion() =
        runUnitPracticeTest {
            val unitId = "unit_screen_state_holders_and_ui_state"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(10, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf("state_ownership", "unidirectional_data_flow")
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "state_ownership_001",
                    "architecture_state_holder_taxonomy",
                    "durable_state_vs_one_off_event",
                    "viewmodel_activity_reference_lifetime",
                    "ui_state_shape_from_the_screens_requirements",
                    "architecture_ui_event_consumption",
                    "occurrence_guarantee_before_mechanism",
                    "owner_chosen_from_the_required_lifetime",
                    "unidirectional_data_flow_001",
                    "exposed_mutable_state_costs_a_second_write_path",
                ),
                questionIds,
            )
            // The recorded overlap, now four Questions wide: each one's reasoning belongs to the
            // synthesis Unit, and each is here because the two Units share a primary concept the
            // taxonomy does not split.
            assertTrue(
                setOf(
                    "durable_state_vs_one_off_event",
                    "architecture_ui_event_consumption",
                    "occurrence_guarantee_before_mechanism",
                    "owner_chosen_from_the_required_lifetime",
                ).all { it in questionIds },
            )

            // The five lifecycle, Compose and coroutine bridges this Unit leans on are the ones
            // most likely to be promoted by mistake, because each has ACTIVE Questions of its
            // own. None of them may reach this Unit's practice.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "viewmodel_lifecycle",
                    "configuration_changes",
                    "process_death",
                    "saved_state",
                    "kmp_lifecycle_viewmodel",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertEquals(0, attemptCount())
        }

    /**
     * E26-04: the data-ownership Unit's pool, and the two Questions it shares with Unit 1.
     *
     * The expectation table above already asserts the count, the scope and the supporting-only
     * exclusion. What it cannot state is *which* Questions arrive. Two of the ten reach this Unit
     * through `layered_architecture`, which is also primary in the foundations Unit's closing
     * Lesson, so `architecture_paging_ownership` and `dto_entity_domain_model_boundary` appear in
     * both Units' practice. `docs/content/architecture-units-1-6-plan.md` records that overlap as
     * calculated rather than discovered, and E26-08 kept `architecture_paging_ownership` where it
     * is because no honest re-map improves it and this epic creates no taxonomy split.
     *
     * The tenth Question is `viewmodel_vs_repository_responsibility`, which E26-08 re-mapped here
     * from `mvvm`: its reasoning is the repository/state-holder responsibility split and the
     * placement of cache and retry policy, which this Unit's opening Lessons own. It is asserted by
     * id so a later re-map has to re-state the consequence rather than silently repairing it.
     */
    @Test
    fun theDataOwnershipUnitPractisesItsPrimaryConceptsIncludingTwoSharedWithUnitOne() =
        runUnitPracticeTest {
            val unitId = "unit_repositories_and_data_ownership"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(10, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf(
                "repository_pattern",
                "single_source_of_truth",
                "layered_architecture",
                "error_modeling",
            )
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            assertEquals(
                setOf(
                    "repository_observable_api_shape",
                    "repository_boundary_needs_a_decision_to_own",
                    "repository_contract_carries_meaning_not_origin",
                    "viewmodel_vs_repository_responsibility",
                    "single_source_of_truth_001",
                    "authoritative_owner_is_chosen_per_fact",
                    "architecture_paging_ownership",
                    "dto_entity_domain_model_boundary",
                    "architecture_error_mapping_boundary",
                    "architecture_error_modeling_result_type",
                ),
                questions.map { it.id }.toSet(),
            )
            // E26-08 re-mapped this Question out of `mvvm`, so it reaches this Unit and no longer
            // reaches the pattern Unit. The pattern-Unit half is asserted in that Unit's test.
            assertTrue("viewmodel_vs_repository_responsibility" in questions.map { it.id }.toSet())

            // The recorded overlap with the foundations Unit, through `layered_architecture`.
            val foundationsBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_architecture_responsibilities_and_boundaries"))
            foundationsBuilder.settled()
            foundationsBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(foundationsBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            foundationsBuilder.settled()
            val foundations = selectedQuestions(foundationsBuilder.start()).map { it.id }.toSet()
            assertEquals(
                setOf("architecture_paging_ownership", "dto_entity_domain_model_boundary"),
                foundations intersect questions.map { it.id }.toSet(),
            )

            // The bridges this Unit leans on all have ACTIVE Questions of their own, and the
            // Flow one matters most: `flow_one_shot_result_vs_observable_stream` assesses very
            // nearly the API-shape decision from the stream side, and it stays in `async_reactive`
            // rather than reaching this Unit. E26-08 owns whether an architecture-side Question
            // would add anything or merely duplicate it.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf("offline_first", "cache_invalidation", "caching", "room_dao", "retrofit")
                    .all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertFalse("flow_one_shot_result_vs_observable_stream" in questions.map { it.id }.toSet())
            assertEquals(0, attemptCount())
        }

    /**
     * E26-05: the domain Unit's pool, the two Questions it shares with Unit 1, and the one
     * architecture Question that still reaches no Unit at all.
     *
     * Three separate claims the expectation table cannot make. First, *which* nine Questions
     * arrive. Second, the Unit 1 intersection, now four Questions wide: `dependency_direction` and
     * `interface_boundaries` are primary in the foundations Unit's third and fourth Lessons and in
     * this Unit's, so the two Questions E26-01 predicted and the two E26-08 authored for GAP-U1-C
     * and GAP-U1-D all appear in both pools. The two older ones complete their reasoning here; the
     * two new ones are Unit 1's own reasoning and are deliberately kept simpler than the inversion
     * Question, so the shared pool is fair in both directions.
     * `docs/content/architecture-units-1-6-plan.md` records the overlap as calculated rather than
     * discovered. Third, `architecture_solid_dependency_substitution` stays outside: `solid` is
     * supporting-only across the whole epic by design, and dependency inversion is taught under the
     * concept where the decision is actually made. Asserting all three means a later re-map has to
     * re-state the consequence rather than quietly repair it.
     */
    @Test
    fun theDomainLogicUnitPractisesItsPrimaryConceptsIncludingTwoSharedWithUnitOne() =
        runUnitPracticeTest {
            val unitId = "unit_domain_logic_and_dependency_direction"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(9, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf(
                "use_cases",
                "clean_architecture",
                "dependency_direction",
                "interface_boundaries",
            )
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "architecture_use_case_reuse",
                    "domain_layer_passthrough_cost",
                    "domain_layer_is_earned_by_the_feature",
                    "clean_architecture_dependency_rule_tradeoff",
                    "dependency_rule_constrains_direction_not_layer_count",
                    "dependency_direction_domain_framework_types",
                    "dependency_direction_callback_does_not_reverse_it",
                    "architecture_interface_boundary_ownership",
                    "interface_with_one_implementation_is_not_a_boundary",
                ),
                questionIds,
            )

            // The recorded overlap with the foundations Unit, through the two concepts both
            // Units take as primary.
            val foundationsBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_architecture_responsibilities_and_boundaries"))
            foundationsBuilder.settled()
            foundationsBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(foundationsBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            foundationsBuilder.settled()
            val foundations = selectedQuestions(foundationsBuilder.start()).map { it.id }.toSet()
            assertEquals(
                setOf(
                    "dependency_direction_domain_framework_types",
                    "dependency_direction_callback_does_not_reverse_it",
                    "architecture_interface_boundary_ownership",
                    "interface_with_one_implementation_is_not_a_boundary",
                ),
                foundations intersect questionIds,
            )

            // Nine supporting-only concepts, holding ACTIVE Questions of their own, broaden
            // nothing. `solid` is the one that matters: it is supporting-only across every
            // E26 Unit, so its Question reaches no Unit's practice at all, which is intended
            // rather than an oversight.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "solid",
                    "service_locator_vs_di",
                    "android_modules",
                    "kmp_architecture",
                    "repository_pattern",
                    "layered_architecture",
                    "architecture_tradeoffs",
                    "state_ownership",
                    "separation_of_concerns",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertFalse("architecture_solid_dependency_substitution" in questionIds)
            assertFalse("architecture_solid_dependency_substitution" in foundations)
            assertEquals(0, attemptCount())
        }

    /**
     * E26-08: the pattern Unit's pool, after the two Questions that belonged elsewhere were moved.
     *
     * E26-06 recorded a pool of four in which only two Questions assessed what the Unit teaches:
     * `viewmodel_vs_repository_responsibility` was filed under `mvvm` while assessing the
     * repository/state-holder split, and `architecture_ui_event_consumption` was filed under `mvi`
     * while assessing replay, consumption and acknowledgement. E26-08 re-mapped both to the
     * Subtopics that own their reasoning and authored the three Questions the Unit was missing, so
     * the pool is still five and every one of them now assesses this Unit's own material.
     *
     * That also closes GAP-U5-A: `mvc` held no ACTIVE Question through six issues, and the mapping
     * was never distorted to manufacture coverage. It now holds the responsibility-classification
     * Question, which is what the opening Lesson actually teaches. The identity between the Unit's
     * concepts and its resolved Subtopics therefore holds for the first time, and is asserted.
     */
    @Test
    fun thePatternUnitPractisesEveryOneOfItsFivePrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_responsibility_models_mvp_mvvm_mvi"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(5, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf("mvc", "mvp", "mvvm", "mvi", "mvvm_vs_mvi")
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "classify_a_screen_by_its_responsibilities",
                    "mvp_vs_mvvm_view_contract",
                    "observed_state_arrangement_is_not_a_class_or_a_folder",
                    "explicit_transition_is_not_the_input_spelling",
                    "architecture_mvi_single_state",
                ),
                questionIds,
            )
            // Five primary concepts, five Questions, one each: the identity E26-06 could not assert.
            assertEquals(concepts, questions.map { it.subtopicId }.toSet())

            // The two re-mapped Questions no longer contaminate this Unit. Their reasoning belongs
            // to the state-holder, data-ownership and synthesis Units, and is asserted there.
            assertFalse("viewmodel_vs_repository_responsibility" in questionIds)
            assertFalse("architecture_ui_event_consumption" in questionIds)

            // The Unit shares no Question with any earlier architecture Unit, because the five
            // pattern concepts are primary nowhere else in the epic. Asserted rather than assumed,
            // since every other architecture Unit so far has had an overlap to record.
            val earlier = listOf(
                "unit_architecture_responsibilities_and_boundaries",
                "unit_screen_state_holders_and_ui_state",
                "unit_repositories_and_data_ownership",
                "unit_domain_logic_and_dependency_direction",
            ).flatMap { earlierUnitId ->
                val earlierBuilder = builder(PracticeBuilderTarget.LearningUnit(earlierUnitId))
                earlierBuilder.settled()
                earlierBuilder.selectQuestionCount(
                    assertIs<PracticeAvailability.Available>(earlierBuilder.uiState.value.availability)
                        .eligibleQuestionCount,
                )
                earlierBuilder.settled()
                selectedQuestions(earlierBuilder.start()).map { it.id }
            }.toSet()
            assertEquals(emptySet(), earlier intersect questionIds)

            // Seven supporting-only concepts, holding fourteen ACTIVE Questions between them,
            // broaden nothing. `state_ownership` is the one that matters most: it is supporting in
            // four of the five Lessons and primary across seven Lessons of two other Units, so a
            // promotion here would pull the whole state-holder pool into a Unit that teaches none
            // of it.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "state_ownership",
                    "unidirectional_data_flow",
                    "stateflow",
                    "viewmodel_lifecycle",
                    "kotlin_sealed_types",
                    "interface_boundaries",
                    "architecture_tradeoffs",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertEquals(0, attemptCount())
        }

    /**
     * E26-08: the synthesis Unit's pool, which is still a strict subset of the state-holder Unit's.
     *
     * E26-07 recorded a pool of four in which `architecture_tradeoffs` — the closing Lesson's
     * primary concept, and the concept the whole epic ends on — reached nothing, and only one of
     * the four Questions semantically assessed this Unit's material. E26-08 closed GAP-U6-A by
     * re-mapping `architecture_ui_event_consumption` out of `mvi`, closed GAP-U6-B and the part of
     * GAP-U6-A the re-map left open with two new Questions, and closed GAP-U6-C and GAP-U1-E under
     * `architecture_tradeoffs`. The identity between the Unit's concepts and its resolved Subtopics
     * therefore holds now, and is asserted.
     *
     * The containment is unchanged and is the accepted cost. Three of the four Lessons take
     * `state_ownership`, which is also primary across four Lessons of the state-holder Unit, so
     * every `state_ownership` Question this Unit reaches is also in that Unit's pool — and the two
     * `architecture_tradeoffs` Questions are likewise shared with the foundations Unit. The plan
     * records both as calculated rather than discovered, and E26-08 accepted them rather than
     * distorting a mapping to separate practice. A later re-map fails an assertion here and has to
     * re-state the consequence rather than quietly repairing it.
     */
    @Test
    fun theSynthesisUnitPractisesBothPrimaryConceptsAndStaysInsideTheStateHolderUnit() =
        runUnitPracticeTest {
            val unitId = "unit_state_events_lifetime_and_selection"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(10, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf("state_ownership", "architecture_tradeoffs")
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "state_ownership_001",
                    "architecture_state_holder_taxonomy",
                    "durable_state_vs_one_off_event",
                    "viewmodel_activity_reference_lifetime",
                    "ui_state_shape_from_the_screens_requirements",
                    "architecture_ui_event_consumption",
                    "occurrence_guarantee_before_mechanism",
                    "owner_chosen_from_the_required_lifetime",
                    "added_layer_must_isolate_an_independent_change",
                    "smallest_structure_that_satisfies_the_requirements",
                ),
                questionIds,
            )
            // The concept the epic's closing Lesson teaches now reaches practice, which is what
            // E26-08 changed, and the whole-feature proportionality Question is the one it ends on.
            assertEquals(concepts, questions.map { it.subtopicId }.toSet())
            assertTrue("smallest_structure_that_satisfies_the_requirements" in questionIds)

            // The re-map E26-07 recorded as owed: the delivery-and-acknowledgement Question this
            // Unit's second Lesson teaches now reaches this Unit rather than the pattern Unit.
            assertTrue("architecture_ui_event_consumption" in questionIds)

            // The recorded containment: this Unit's `state_ownership` half is inside the
            // state-holder Unit's pool, and its `architecture_tradeoffs` half is shared with the
            // foundations Unit instead.
            val stateHolderBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_screen_state_holders_and_ui_state"))
            stateHolderBuilder.settled()
            stateHolderBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(stateHolderBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            stateHolderBuilder.settled()
            val stateHolder = selectedQuestions(stateHolderBuilder.start()).map { it.id }.toSet()
            assertEquals(
                questions.filter { it.subtopicId == "state_ownership" }.map { it.id }.toSet(),
                stateHolder intersect questionIds,
            )

            val foundationsBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_architecture_responsibilities_and_boundaries"))
            foundationsBuilder.settled()
            foundationsBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(foundationsBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            foundationsBuilder.settled()
            val foundations = selectedQuestions(foundationsBuilder.start()).map { it.id }.toSet()
            assertEquals(
                setOf(
                    "added_layer_must_isolate_an_independent_change",
                    "smallest_structure_that_satisfies_the_requirements",
                ),
                foundations intersect questionIds,
            )

            // Ten supporting-only concepts, every one of them owned and assessed by another
            // curriculum, broaden nothing. The stream and lifetime bridges matter most: this Unit
            // applies their conclusions and teaches none of their mechanisms, so promoting one
            // would claim practice coverage for material it deliberately does not carry.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "sharedflow",
                    "hot_vs_cold_streams",
                    "stateflow",
                    "process_death",
                    "viewmodel_lifecycle",
                    "lifecycle_coroutines",
                    "coroutine_scope",
                    "background_api_selection",
                    "use_cases",
                    "repository_pattern",
                    "layered_architecture",
                    "clean_architecture",
                    "unidirectional_data_flow",
                    "single_source_of_truth",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertFalse("stream_choice_cannot_supply_a_delivery_guarantee" in questionIds)
            assertFalse("shared_flow_try_emit_true_is_not_delivery" in questionIds)
            assertFalse("background_api_selection_criteria" in questionIds)
            assertEquals(0, attemptCount())
        }

    /** E27-08: generic construction practice, with the premature Hilt route removed. */
    @Test
    fun theDependencyInjectionFoundationsUnitPractisesItsReviewedPrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_dependency_injection_as_object_construction"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(7, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf(
                "di_fundamentals",
                "constructor_injection",
                "composition_root",
                "service_locator_vs_di",
                "manual_di",
            )
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "di_constructor_injection_testability",
                    "composition_root_001",
                    "service_locator_vs_di_001",
                    "manual_di_graph_growth_cost",
                    "injection_and_inversion_are_separate_decisions",
                    "construct_fetch_receive_responsibility",
                    "integration_boundary_resolution_vs_service_locator",
                ),
                questionIds,
            )
            assertEquals(concepts, questions.map { it.subtopicId }.toSet())
            assertEquals(7, questions.size)
            assertEquals(
                mapOf(QuestionLevel.FOUNDATION to 3, QuestionLevel.APPLIED to 4),
                questions.groupingBy { it.level }.eachCount(),
            )

            // Android-owned construction belongs to the Hilt Unit, not generic foundations.
            assertFalse("hilt_field_injection_framework_classes" in questionIds)

            // Supporting concepts never broaden a Unit's practice. The two that matter here are
            // `test_doubles` and `test_di`: this Unit argues testability from an explicit
            // dependency and teaches no testing, so neither may contribute a Question. The
            // architecture bridges are equally excluded, which is what keeps E26's pools unchanged.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "test_doubles",
                    "test_di",
                    "interface_boundaries",
                    "dependency_direction",
                    "solid",
                    "separation_of_concerns",
                    "layered_architecture",
                    "architecture_tradeoffs",
                    "dependency_graphs",
                    "di_framework_tradeoffs",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertFalse("architecture_solid_dependency_substitution" in questionIds)
            assertFalse("interface_with_one_implementation_is_not_a_boundary" in questionIds)

            // The one deliberate overlap, calculated in the plan rather than discovered: nothing
            // this Unit reaches belongs to a shipped architecture Unit's pool, because no shipped
            // Lesson takes a `dependency_injection` Subtopic as primary.
            val foundationsBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_architecture_responsibilities_and_boundaries"))
            foundationsBuilder.settled()
            foundationsBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(foundationsBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            foundationsBuilder.settled()
            val foundations = selectedQuestions(foundationsBuilder.start()).map { it.id }.toSet()
            assertEquals(emptySet(), foundations intersect questionIds)
            assertEquals(0, attemptCount())
        }

    /** E27-08: generic graph, runtime-input, ambiguity and owner/scope practice only. */
    @Test
    fun theObjectGraphUnitPractisesOnlyItsTwoPrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_object_graphs_lifetimes_and_scopes"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(5, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf("dependency_graphs", "di_scopes")
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "di_scopes_001",
                    "scope_rule_requires_lived_owner",
                    "runtime_input_stays_out_of_graph",
                    "same_type_dependencies_need_distinct_keys",
                    "graph_error_timing_follows_wiring_mechanism",
                ),
                questionIds,
            )
            assertEquals(concepts, questions.map { it.subtopicId }.toSet())
            assertEquals(5, questions.size)
            assertEquals(
                mapOf(QuestionLevel.FOUNDATION to 1, QuestionLevel.APPLIED to 4),
                questions.groupingBy { it.level }.eachCount(),
            )

            // Dagger component validation is taught and practised in Unit 3.
            assertFalse("dagger_compile_time_graph_validation" in questionIds)

            // Supporting concepts never broaden a Unit's practice, and here that is load-bearing in
            // both directions. `dagger_qualifiers` and `dagger_fundamentals` are supporting because the
            // generic ideas they bridge to have no framework-independent Subtopic; were either primary,
            // the Unit would practise Dagger two Units before Dagger is taught. The architecture,
            // lifecycle, platform and build bridges are equally excluded, which is what keeps every
            // shipped Unit's pool unchanged.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "dagger_qualifiers",
                    "dagger_fundamentals",
                    "di_framework_tradeoffs",
                    "kotlin_gradle_plugin",
                    "composition_root",
                    "manual_di",
                    "constructor_injection",
                    "layered_architecture",
                    "state_ownership",
                    "architecture_tradeoffs",
                    "android_process_model",
                    "viewmodel_lifecycle",
                    "navigation_fundamentals",
                    "interface_boundaries",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })

            // The first dependency-injection Unit's pool is untouched by this one: no Subtopic is
            // primary in both, so the two Units share no Question.
            val foundationsBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_dependency_injection_as_object_construction"))
            foundationsBuilder.settled()
            foundationsBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(foundationsBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            foundationsBuilder.settled()
            val foundations = selectedQuestions(foundationsBuilder.start()).map { it.id }.toSet()
            assertEquals(emptySet(), foundations intersect questionIds)
            assertEquals(0, attemptCount())
        }

    /** E27-08: Dagger mechanisms plus module responsibility and validation limits. */
    @Test
    fun theDaggerUnitPractisesOnlyItsSevenPrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_dagger_compile_time_object_graphs"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(11, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf(
                "dagger_fundamentals",
                "dagger_modules",
                "dagger_bindings",
                "dagger_components",
                "dagger_scopes",
                "dagger_qualifiers",
                "dagger_multibindings",
            )
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "dagger_generated_factory_no_reflection",
                    "dagger_inject_provides_binds_selection",
                    "dagger_component_graph_root",
                    "dagger_subcomponent_parent_binding_inheritance",
                    "dagger_component_dependency_vs_subcomponent",
                    "dagger_scope_component_instance_lifetime",
                    "dagger_qualifier_same_type_bindings",
                    "dagger_multibinding_into_set",
                    "dagger_compile_time_graph_validation",
                    "dagger_module_contributes_component_owns",
                    "dagger_compile_success_not_lifetime_proof",
                ),
                questionIds,
            )
            assertEquals(11, questions.size)
            assertEquals(
                mapOf(QuestionLevel.FOUNDATION to 6, QuestionLevel.APPLIED to 5),
                questions.groupingBy { it.level }.eachCount(),
            )

            // The module/component responsibility now has scenario-based coverage.
            assertEquals(
                "dagger_modules",
                questions.single { it.id == "dagger_module_contributes_component_owns" }.subtopicId,
            )

            // Dagger validation now routes with the framework mechanism that teaches it.
            assertEquals(
                "dagger_fundamentals",
                questions.single { it.id == "dagger_compile_time_graph_validation" }.subtopicId,
            )

            // Supporting concepts never broaden a Unit's practice. Here that keeps the generic
            // reuse and graph Questions of the previous two Units out of this one entirely.
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "dependency_graphs",
                    "di_scopes",
                    "composition_root",
                    "manual_di",
                    "constructor_injection",
                    "interface_boundaries",
                    "dependency_direction",
                    "layered_architecture",
                    "module_dependency_direction",
                    "feature_modularization",
                    "state_ownership",
                    "di_framework_tradeoffs",
                    "kotlin_gradle_plugin",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })

            // Neither shipped dependency-injection Unit shares a Question with this one: no
            // Subtopic is primary in two of the three, so the three pools are disjoint.
            listOf(
                "unit_dependency_injection_as_object_construction",
                "unit_object_graphs_lifetimes_and_scopes",
            ).forEach { earlier ->
                val earlierBuilder = builder(PracticeBuilderTarget.LearningUnit(earlier))
                earlierBuilder.settled()
                earlierBuilder.selectQuestionCount(
                    assertIs<PracticeAvailability.Available>(earlierBuilder.uiState.value.availability)
                        .eligibleQuestionCount,
                )
                earlierBuilder.settled()
                val reached = selectedQuestions(earlierBuilder.start()).map { it.id }.toSet()
                assertEquals(emptySet(), reached intersect questionIds, earlier)
            }
            assertEquals(0, attemptCount())
        }

    /** E27-08: Hilt practice includes Android-owned construction and owner-lifetime choices. */
    @Test
    fun theHiltUnitPractisesOnlyItsFivePrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_hilt_android_lifecycle_integration"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(9, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf(
                "hilt_fundamentals",
                "hilt_components",
                "hilt_viewmodels",
                "hilt_modules",
                "hilt_vs_dagger",
            )
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "hilt_entry_point_manual_access",
                    "hilt_activity_retained_component_lifetime",
                    "di_hilt_viewmodel_scope",
                    "dagger_assisted_injection_viewmodel",
                    "hilt_install_in_binding_visibility",
                    "hilt_vs_dagger_convention_tradeoff",
                    "hilt_field_injection_framework_classes",
                    "hilt_viewmodel_vs_activity_retained_owner",
                    "hilt_singleton_component_not_process_durable",
                ),
                questionIds,
            )
            assertEquals(9, questions.size)
            assertEquals(
                mapOf(QuestionLevel.FOUNDATION to 3, QuestionLevel.APPLIED to 6),
                questions.groupingBy { it.level }.eachCount(),
            )

            assertEquals(
                "hilt_fundamentals",
                questions.single { it.id == "hilt_field_injection_framework_classes" }.subtopicId,
            )
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(
                setOf(
                    "constructor_injection",
                    "service_locator_vs_di",
                    "dagger_components",
                    "dagger_fundamentals",
                    "dagger_modules",
                    "dagger_scopes",
                    "di_scopes",
                    "dependency_graphs",
                    "activity_lifecycle",
                    "configuration_changes",
                    "viewmodel_lifecycle",
                    "saved_state",
                    "android_process_model",
                    "state_ownership",
                    "di_framework_tradeoffs",
                    "architecture_tradeoffs",
                ).all { it in supportingOnly },
            )
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertEquals(0, attemptCount())
        }

    /** E27-08: every primary Koin concept has semantic scenario coverage. */
    @Test
    fun theKoinUnitPractisesOnlyItsFivePrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_koin_and_dependency_injection_in_kmp"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(7, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf(
                "koin_fundamentals",
                "koin_definitions",
                "koin_scopes",
                "koin_viewmodels",
                "koin_multiplatform",
            )
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "di_koin_factory_vs_single",
                    "koin_multiplatform_common_module",
                    "koin_container_startup_composition_boundary",
                    "koin_interview_scope_owner",
                    "koin_viewmodel_construction_vs_ownership",
                    "koin_definition_from_reuse_requirement",
                    "koin_shared_and_platform_binding_split",
                ),
                questionIds,
            )
            assertEquals(7, questions.size)
            assertEquals(
                mapOf(QuestionLevel.FOUNDATION to 1, QuestionLevel.APPLIED to 6),
                questions.groupingBy { it.level }.eachCount(),
            )

            assertEquals(concepts, questions.map { it.subtopicId }.toSet())

            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(questions.none { it.subtopicId in supportingOnly })
            assertEquals(0, attemptCount())
        }

    /** E27-08: strategy practice plus the intentional Unit 1 overlap through `manual_di`. */
    @Test
    fun theStrategyUnitPractisesOnlyItsTwoPrimaryConcepts() =
        runUnitPracticeTest {
            val unitId = "unit_choosing_a_dependency_injection_strategy"
            val unit = assertNotNull(BundledLearningContentRepository().getUnitById(unitId))
            val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
            val settled = builder.settled()

            assertEquals(unit.title, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertEquals(3, available.eligibleQuestionCount)
            builder.selectQuestionCount(available.eligibleQuestionCount)
            builder.settled()

            val config = builder.start()
            val concepts = setOf("manual_di", "di_framework_tradeoffs")
            assertEquals(AssessmentScope.Subtopics(concepts), config.scope)

            val questions = selectedQuestions(config)
            val questionIds = questions.map { it.id }.toSet()
            assertEquals(
                setOf(
                    "manual_di_graph_growth_cost",
                    "di_graph_check_timing_by_mechanism",
                    "di_strategy_smallest_sufficient_choice",
                ),
                questionIds,
            )
            assertEquals(3, questions.size)
            assertEquals(
                mapOf(QuestionLevel.APPLIED to 2, QuestionLevel.ADVANCED to 1),
                questions.groupingBy { it.level }.eachCount(),
            )

            assertEquals(concepts, questions.map { it.subtopicId }.toSet())
            val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
            assertTrue(questions.none { it.subtopicId in supportingOnly })

            val foundationsBuilder =
                builder(PracticeBuilderTarget.LearningUnit("unit_dependency_injection_as_object_construction"))
            foundationsBuilder.settled()
            foundationsBuilder.selectQuestionCount(
                assertIs<PracticeAvailability.Available>(foundationsBuilder.uiState.value.availability)
                    .eligibleQuestionCount,
            )
            foundationsBuilder.settled()
            val foundations = selectedQuestions(foundationsBuilder.start()).map { it.id }.toSet()
            assertEquals(setOf("manual_di_graph_growth_cost"), foundations intersect questionIds)
            assertEquals(0, attemptCount())
        }

    /**
     * The whole runtime flow in one pass: the route's Unit ID becomes the current Unit, its ACTIVE
     * Lessons' primary concepts become the scope, and the ordinary focused engine runs it.
     */
    @Test
    fun theShippedComposeUnitPractisesTheConceptsItTeachesThroughTheExistingEngine() =
        runUnitPracticeTest {
            val builder = builder(PracticeBuilderTarget.LearningUnit(ComposeUnitId))
            val settled = builder.settled()

            // The title comes from the resolved Unit, not from the route and not from a concept
            // count: the learner is told what they are about to practise.
            assertEquals(PracticeScopeKind.LEARNING_UNIT, settled.scope.kind)
            assertEquals(ComposeUnitTitle, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertTrue(
                available.eligibleQuestionCount > 0,
                "The shipped Unit reached no authored Questions.",
            )
            // Preflight reads eligibility; it must never create an attempt to find out.
            assertEquals(0, attemptCount())

            val config = builder.start()

            // The authoring rules, asserted against the content that ships: `compose_fundamentals`
            // is primary in two Lessons and appears once; the supporting concepts those Lessons
            // lean on — Views, recomposition, architecture UDF, state hoisting — are absent.
            assertEquals(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopics(
                        setOf("compose_fundamentals", "compose_udf"),
                    ),
                    questionCount = settled.questionCount,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.ALL,
                ),
                config,
            )

            // The structural claim the acceptance criterion makes: the derived scope really does
            // reach the ACTIVE Questions authored against those Subtopics.
            val selected = selectedQuestions(config)
            assertTrue(selected.isNotEmpty(), "The Unit scope selected no Questions.")
            selected.forEach { question ->
                assertContains(
                    setOf("compose_fundamentals", "compose_udf"),
                    question.subtopicId,
                    "Selected ${question.id} outside the Unit's primary concepts.",
                )
            }

            val attemptId = runPractice(config)

            // One ordinary attempt, through the ordinary engine, scored and completed.
            assertEquals(1, attemptCount())
            val stored = assertNotNull(assessmentRepository.getById(attemptId))
            assertEquals(AssessmentStatus.COMPLETED, stored.status)
            assertEquals(config, stored.config)
        }

    /**
     * Assessment history records the assessment, not the study material that suggested it.
     *
     * The stored row keeps the concepts the run actually asked about, so a later re-authoring of
     * the Unit cannot retroactively change what a finished attempt was — and nothing in the row
     * names the Unit, which is what keeps the assessment domain independent of learning content.
     */
    @Test
    fun aFinishedUnitRunPersistsItsConceptsAndNotTheUnitItCameFrom() = runUnitPracticeTest {
        val config = builder(PracticeBuilderTarget.LearningUnit(ComposeUnitId)).also { it.settled() }
            .start()

        val attemptId = runPractice(config)

        val row = assertNotNull(database.assessmentAttemptDao().getTestAttemptById(attemptId))
        assertFalse(
            row.scopeId.orEmpty().contains(ComposeUnitId),
            "The persisted scope named the Learning Unit: ${row.scopeId}.",
        )
        assertFalse(
            row.scopeId.orEmpty().contains(ComposeUnitTitle),
            "The persisted scope carried a presentation label: ${row.scopeId}.",
        )
        // Reconstruction is generic and complete: the attempt reopens as the multi-Subtopic run it
        // was, with no learning content consulted.
        val reconstructed = assertNotNull(assessmentRepository.getById(attemptId)).config
        assertEquals(
            AssessmentScope.Subtopics(setOf("compose_fundamentals", "compose_udf")),
            assertIs<AssessmentConfig.Focused>(reconstructed).scope,
        )
    }

    /**
     * E21-07: the far end of the journey, on the attempt the shipped Unit actually created.
     *
     * `TargetedPracticeLifecycleIntegrationTest` already owns the multi-Subtopic result and retake
     * rules against a fixture catalogue, and this does not restate them. What it adds is the one
     * thing a fixture cannot: that the attempt a *Learning Unit* produced is an ordinary member of
     * assessment history by the time it is reviewed and repeated — reopened by attempt ID alone,
     * with the Unit that suggested it nowhere in the path.
     */
    @Test
    fun aUnitOriginatedAttemptIsReviewedAndRetakenAsAnOrdinaryFocusedRun() {
        // One multi-concept expansion and the final single-concept Unit exercise both shapes.
        listOf(ComposeUnitId, "unit_identity_keys_and_stability", "unit_snapshot_fundamentals").forEach { unitId ->
            runUnitPracticeTest {
                val config = builder(PracticeBuilderTarget.LearningUnit(unitId)).also { it.settled() }
                    .start()
                val concepts = assertIs<AssessmentScope.Subtopics>(config.scope).subtopicIds
                val studiedBefore = studyRepository.getStudiedLessons()
                val attemptId = runPractice(config)

                // Review resolves from the attempt ID and the historical curriculum: no Unit, no Lesson,
                // and no learning content is consulted to reconstruct what was asked.
                val result = assertIs<FocusedResultUiState.Content>(result(attemptId).settledResult())
                assertEquals(attemptId, result.attemptId)
                assertTrue(result.totalQuestions > 0, "The reviewed attempt asked nothing.")
                assertEquals(result.totalQuestions, result.questions.size)
                result.questions.forEach { item ->
                    // Every Question the run asked is still resolvable for review, and each one is inside
                    // the concepts the Unit teaches rather than the wider Topic they happen to share.
                    val available = assertIs<ReviewQuestionItem.Available>(item)
                    assertContains(
                        concepts,
                        available.question.subtopicId,
                        "Reviewed ${available.question.questionId} outside the Unit's primary concepts.",
                    )
                }

                val retakeViewModel = result(attemptId)
                retakeViewModel.settledResult()
                retakeViewModel.repeatPractice()
                val retakeId = assertIs<FocusedResultEvent.RetakeCreated>(
                    withContext(Dispatchers.Default) {
                        withTimeout(AwaitTimeoutMillis) { retakeViewModel.events.first() }
                    },
                ).attemptId

                // A second attempt, not a mutation of the first: a new stable ID, in progress, carrying the
                // stored configuration — the multi-Subtopic scope survives rather than broadening to the
                // Topic the concepts share.
                assertNotEquals(attemptId, retakeId)
                val retake = assertNotNull(assessmentRepository.getById(retakeId))
                assertEquals(AssessmentStatus.IN_PROGRESS, retake.status)
                assertEquals(config, retake.config)
                assertEquals(
                    AssessmentScope.Subtopics(concepts),
                    assertIs<AssessmentConfig.Focused>(retake.config).scope,
                )

                // The source attempt is untouched history, and both attempts now stand on their own.
                val source = assertNotNull(assessmentRepository.getById(attemptId))
                assertEquals(AssessmentStatus.COMPLETED, source.status)
                assertEquals(config, source.config)
                assertEquals(2, attemptCount())
                assertEquals(studiedBefore, studyRepository.getStudiedLessons())
            }
        }
    }

    /** A stale route that names no current Unit fails safely rather than practising something else. */
    @Test
    fun aRouteNamingNoCurrentUnitStartsNothing() = runUnitPracticeTest {
        val builder = builder(PracticeBuilderTarget.LearningUnit("unit_that_was_retired"))

        val settled = builder.settled()
        builder.startPractice()

        assertEquals(PracticeAvailability.TargetUnavailable, settled.availability)
        assertFalse(settled.isStartEnabled)
        assertEquals(0, attemptCount())
    }
}

/**
 * Boots the production graph over an in-memory database holding the *bundled* curriculum.
 *
 * Only determinism is overridden: the database instance, the engine's attempt IDs and clock, and
 * the selector's randomizer. Everything the assertions depend on — which Questions exist, which
 * concepts the Unit teaches — is shipped content.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun runUnitPracticeTest(block: suspend UnitPracticeGraph.() -> Unit) = runTest {
    Dispatchers.setMain(Dispatchers.Unconfined)
    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    assertEquals(
        CurriculumImportResult.Imported,
        CurriculumImporter(database, loadCurriculum = { BundledCurriculumSource.load() })
            .importCurriculum(),
    )

    var attemptSequence = 0
    var clockSequence = 0L
    val app = koinApplication {
        modules(
            curriculumDataModule,
            learningContentModule,
            assessmentDataModule,
            savedQuestionDataModule,
            lessonStudyDataModule,
            topicStudyPresentationModule,
            module {
                single<CurriculumDatabase> { database }
                single {
                    AssessmentQuestionSelector(
                        curriculumRepository = get(),
                        completedHistory = get<AssessmentHistoryStore>(),
                        performanceDerivation = get(),
                        randomize = { it },
                    )
                }
                single {
                    AssessmentEngine(
                        questionSelector = get(),
                        generateAttemptId = { "attempt-${++attemptSequence}" },
                        now = {
                            clockSequence += 1
                            Instant.fromEpochMilliseconds(EpochMillis + clockSequence * 1_000)
                        },
                    )
                }
            },
        )
    }

    try {
        UnitPracticeGraph(database, app.koin).block()
    } finally {
        // The history store's refresh is app-scoped and outlives every screen by design, so it is
        // cancelled before the database closes underneath an in-flight query.
        app.koin.get<AppCoroutineScope>().cancel()
        app.close()
        database.close()
    }
}

private class UnitPracticeGraph(
    val database: CurriculumDatabase,
    private val koin: Koin,
) {
    val assessmentRepository: AssessmentRepository get() = koin.get()
    val studyRepository: LessonStudyRepository get() = koin.get()
    private val questionSelector: AssessmentQuestionSelector get() = koin.get()
    private val curriculumRepository: CurriculumRepository get() = koin.get()

    fun builder(target: PracticeBuilderTarget): PracticeBuilderViewModel =
        koin.get { parametersOf(target) }

    fun browser(): TopicBrowserViewModel = koin.get()
    fun topic(topicId: String): TopicDetailViewModel = koin.get { parametersOf(topicId) }
    fun unit(id: String): LearningUnitViewModel = koin.get { parametersOf(id) }
    fun lesson(unitId: String, lessonId: String): LearningLessonViewModel =
        koin.get { parametersOf(unitId, lessonId) }

    /** The ordinary focused result, addressed the only way the shell addresses it: by attempt ID. */
    fun result(attemptId: String): FocusedResultViewModel = koin.get { parametersOf(attemptId) }

    suspend fun attemptCount(): Int = database.assessmentAttemptDao().countTestAttempts()

    suspend fun selectedQuestions(config: AssessmentConfig) =
        when (val selection = questionSelector.select(config)) {
            is AssessmentSelectionResult.Selected -> selection.questions
            is AssessmentSelectionResult.NoContent -> emptyList()
        }

    /** Answers every question correctly and completes, through the ViewModel that owns persistence. */
    suspend fun runPractice(config: AssessmentConfig): String {
        val attemptId = assertIs<StartAssessmentResult.Created>(
            koin.get<StartAssessment>()(config),
        ).attemptId
        val viewModel: AssessmentTakingViewModel =
            koin.get { parametersOf(attemptId) }
        var questionNumber = 1
        while (true) {
            val state = viewModel.awaitQuestion(questionNumber)
            if (
                state is AssessmentTakingUiState.ReadyToComplete ||
                state is AssessmentTakingUiState.CompletionSucceeded
            ) break
            val content = assertIs<AssessmentTakingUiState.Content>(state)
            // The authored key, read from the curriculum: this suite runs on real Questions, so
            // no naming convention can stand in for the correct answer.
            val question = assertNotNull(curriculumRepository.getQuestionById(content.question.id))
            question.correctAnswerIds.forEach(viewModel::selectAnswer)
            viewModel.submitAnswer()
            viewModel.uiState.await { it is AssessmentTakingUiState.Content && it.feedback != null }
            viewModel.nextQuestion()
            questionNumber += 1
        }
        if (viewModel.uiState.value is AssessmentTakingUiState.ReadyToComplete) {
            viewModel.completeAssessment()
        }
        return assertIs<AssessmentTakingUiState.CompletionSucceeded>(
            viewModel.uiState.await { it is AssessmentTakingUiState.CompletionSucceeded },
        ).attemptId
    }
}

private suspend fun AssessmentTakingViewModel.awaitQuestion(
    questionNumber: Int,
): AssessmentTakingUiState = uiState.await { state ->
    when (state) {
        is AssessmentTakingUiState.Content ->
            !state.isSubmitting && state.feedback == null && state.questionNumber == questionNumber
        is AssessmentTakingUiState.ReadyToComplete -> !state.isCompleting
        is AssessmentTakingUiState.CompletionSucceeded -> true
        AssessmentTakingUiState.NoQuestions,
        AssessmentTakingUiState.Error,
        -> error("Assessment taking reached $state instead of question $questionNumber.")
        else -> false
    }
}

private suspend fun PracticeBuilderViewModel.settled(): PracticeBuilderUiState =
    uiState.await { it.availability !is PracticeAvailability.Checking }

private suspend fun FocusedResultViewModel.settledResult(): FocusedResultUiState =
    uiState.await { it !is FocusedResultUiState.Loading }

private suspend fun PracticeBuilderViewModel.start(): AssessmentConfig.Focused {
    startPractice()
    val event = withContext(Dispatchers.Default) {
        withTimeout(AwaitTimeoutMillis) { events.first() }
    }
    return assertIs<PracticeBuilderEvent.StartPractice>(event).config
}

/** Real Room work runs on Room's own executor, so waiting has to leave virtual time. */
private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T =
    withContext(Dispatchers.Default) {
        withTimeout(AwaitTimeoutMillis) { first(predicate) }
    }

private const val AwaitTimeoutMillis = 10_000L
private const val EpochMillis = 1_700_000_000_000L

/** The shipped Unit, not a fixture: see the class comment. */
private const val ComposeUnitId = "unit_thinking_in_compose"
private const val ComposeUnitTitle = "Thinking in Compose"

/**
 * What the shipped Unit teaches, stated once. Asserted literally rather than re-derived, so a
 * re-authored primary concept has to be acknowledged here instead of being confirmed by the same
 * code that produced it.
 */
private val UnitPracticeConcepts = setOf("compose_fundamentals", "compose_udf")
