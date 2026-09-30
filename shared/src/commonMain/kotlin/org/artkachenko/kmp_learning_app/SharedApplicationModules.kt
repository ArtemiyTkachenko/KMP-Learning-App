package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.settings.appearanceModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.module.Module

/**
 * The shared half of every runtime host's Koin graph.
 *
 * Each `start*LocalDataGraph` function installs these modules and then its own two platform
 * modules, which supply the only definitions the shared graph expects from outside: one
 * `CurriculumDatabase` and one `AppPreferenceStorage`.
 *
 * The list is declared once because only the desktop startup function runs in the test suite. The
 * Android, iOS and web functions run in no automated check, so a module listed by hand in each
 * host could be left out of one of them and would fail only at its first resolution on that
 * platform. `SharedHostStartupTest` resolves the whole product graph from this same list.
 */
internal fun sharedApplicationModules(): List<Module> = listOf(
    curriculumDataModule,
    learningContentModule,
    assessmentDataModule,
    savedQuestionDataModule,
    lessonStudyDataModule,
    topicStudyPresentationModule,
    appearanceModule,
    curriculumVisibilityModule,
)
