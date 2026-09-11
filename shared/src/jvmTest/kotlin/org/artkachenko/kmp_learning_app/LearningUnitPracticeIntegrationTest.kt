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
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingUiState
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
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
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig
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
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeRoute
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
            assertEquals(listOf(5, 3, 5, 3, 2), units.drop(1).map { it.lessons.size })

            // Keep the real parent ViewModels alive throughout every child mutation.
            val browser = browser()
            val topic = topic("android_ui")
            val parents = units.associate { it.id to unit(it.id) }
            suspend fun awaitTopic(count: Int) {
                topic.uiState.await { state ->
                    state is TopicDetailUiState.Content &&
                        (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                        StudyProgressSummary.Progress(count, 21)
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
            browser.uiState.await { state ->
                state is TopicBrowserUiState.Content && state.continueLearning == ContinueLearningUiModel.Complete
            }
            // The `android_ui` Topic's own progress is unaffected by Units in another Topic.
            awaitTopic(21)
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
            awaitTopic(20)
            parents.getValue(earlierUnit.id).uiState.await { state ->
                state is LearningUnitUiState.Content &&
                    (state.studyProgress as? StudyProgressUiState.Available)?.value?.summary ==
                    StudyProgressSummary.Progress(4, 5)
            }
            val rebuilt = LocalLessonStudyRepository(database)
            assertFalse(rebuilt.isStudied(earlierLesson.id))
            // 21 `android_ui` Lessons plus 29 in the coroutines and Flow Units, less the
            // one that was just un-studied.
            assertEquals(49, rebuilt.getStudiedLessons().size)
            assertEquals(originalRecords, rebuilt.getStudiedLessons().filter { it.lessonId in publishedIds })
            assertEquals(0, attemptCount())
            assertEquals(null, assertIs<TopicBrowserUiState.Content>(browser.uiState.value).continueStudying)
        }

    @Test
    fun expandedUnitsConfigureOnlyTheirPrimaryConceptsAndDeduplicateProductionQuestions() =
        runUnitPracticeTest {
            val expected = linkedMapOf(
                "unit_state_and_state_ownership" to (setOf("compose_state", "compose_state_hoisting") to 4),
                "unit_recomposition" to (setOf("compose_recomposition") to 3),
                "unit_identity_keys_and_stability" to (setOf("compose_identity_keys", "compose_stability") to 6),
                "unit_derived_state_and_expensive_work" to (setOf("compose_derived_state") to 3),
                "unit_snapshot_fundamentals" to (setOf("compose_snapshot_system") to 4),
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
            )
            val content = BundledLearningContentRepository()
            expected.forEach { (unitId, expectation) ->
                val (concepts, count) = expectation
                val unit = assertNotNull(content.getUnitById(unitId))
                val builder = builder(PracticeBuilderTarget.LearningUnit(unitId))
                val state = builder.settled()
                assertEquals(unit.title, state.scope.name)
                // The builder opens on the default count and the count control deliberately does
                // not re-run the eligibility read, so this figure is what a run started now would
                // ask. That equals the pool only while the pool is smaller than the default, which
                // stopped being true for three Units when E24-08 authored against their gaps.
                assertEquals(
                    minOf(count, DefaultPracticeQuestionCount),
                    assertIs<PracticeAvailability.Available>(state.availability).eligibleQuestionCount,
                )
                // Ask for more than any Unit's pool, so what follows sees the whole pool.
                builder.selectQuestionCount(20)
                builder.settled()
                val config = builder.start()
                assertEquals(AssessmentScope.Subtopics(concepts), config.scope, unitId)
                val questions = selectedQuestions(config)
                assertEquals(count, questions.size, unitId)
                assertEquals(count, questions.map { it.id }.toSet().size, unitId)
                assertEquals(concepts, questions.map { it.subtopicId }.toSet(), unitId)
                val supportingOnly = unit.lessons.flatMap { it.supportingSubtopicIds }.toSet() - concepts
                assertTrue(questions.none { it.subtopicId in supportingOnly }, unitId)
                assertEquals(config, assertIs<AppRoute.FocusedSubtopicsPractice>(config.toPracticeRoute()).toAssessmentConfig())
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
            builder.selectQuestionCount(20)
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
            "state_flow_equal_value_is_not_a_new_state",
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

            // The configured run survives the back stack unchanged, exactly as Topic and Subtopic
            // runs do, and arrives at assessment taking as a plain multi-Subtopic scope.
            val route = assertIs<AppRoute.FocusedSubtopicsPractice>(config.toPracticeRoute())
            assertEquals(config, route.toAssessmentConfig())

            val attemptId = runPractice(route.toAssessmentConfig())

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
        val viewModel: AssessmentTakingViewModel =
            koin.get { parametersOf(AssessmentTakingLaunch.New(config)) }
        var questionNumber = 1
        while (true) {
            val state = viewModel.awaitQuestion(questionNumber)
            if (state is AssessmentTakingUiState.ReadyToComplete) break
            val content = assertIs<AssessmentTakingUiState.Content>(state)
            // The authored key, read from the curriculum: this suite runs on real Questions, so
            // no naming convention can stand in for the correct answer.
            val question = assertNotNull(curriculumRepository.getQuestionById(content.question.id))
            question.correctAnswerIds.forEach(viewModel::selectAnswer)
            viewModel.submitAnswer()
            questionNumber += 1
        }
        viewModel.completeAssessment()
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
            !state.isSubmitting && state.questionNumber == questionNumber
        is AssessmentTakingUiState.ReadyToComplete -> !state.isCompleting
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
