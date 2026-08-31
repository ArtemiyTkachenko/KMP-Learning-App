package org.artkachenko.kmp_learning_app.data.local.assessment

import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.koin.dsl.module

internal val assessmentDataModule = module {
    single {
        AssessmentAttemptStore(
            database = get(),
        )
    }

    single<AssessmentRepository> {
        LocalAssessmentRepository(
            store = get(),
        )
    }

    single {
        AssessmentQuestionSelector(
            curriculumRepository = get(),
        )
    }

    single { AppCoroutineScope() }

    single {
        AssessmentHistoryStore(
            assessmentRepository = get(),
            scope = get<AppCoroutineScope>(),
        )
    }

    single {
        AssessmentEngine(
            questionSelector = get(),
        )
    }

    single {
        AssessmentRetakeService(
            assessmentRepository = get(),
            assessmentEngine = get(),
        )
    }
    single {
        AssessmentSessionLoader(
            assessmentRepository = get(),
            curriculumRepository = get(),
        )
    }
    single {
        LearningProgressService(
            assessmentRepository = get(),
            curriculumRepository = get(),
        )
    }
}
