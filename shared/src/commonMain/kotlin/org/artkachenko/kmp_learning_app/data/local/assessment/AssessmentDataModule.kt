package org.artkachenko.kmp_learning_app.data.local.assessment

import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
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
}
