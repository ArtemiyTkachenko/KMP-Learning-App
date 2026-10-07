package org.artkachenko.kmp_learning_app.data.local.assessment

import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.VisibleAssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment.session.CompleteAssessment
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessment
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
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

    single { AppCoroutineScope() }

    single {
        AssessmentHistoryStore(
            assessmentRepository = get(),
            scope = get<AppCoroutineScope>(),
        )
    }

    single {
        AssessmentQuestionSelector(
            curriculumRepository = get(),
            // The visible projection of the app-scoped cache rather than the repository: the
            // Practice Builder re-runs selection after every edit, and a history-derived preflight
            // must not turn each of those into a history query. The store underneath is the one
            // place completion invalidates, so selection sees a just-finished attempt through the
            // same refresh Progress does — and, being projected, never ranks or selects evidence
            // from a hidden Topic.
            completedHistory = get<VisibleAssessmentHistory>(),
        )
    }

    single {
        AssessmentEngine(
            questionSelector = get(),
        )
    }

    single {
        StartAssessment(
            assessmentEngine = get(),
            assessmentRepository = get(),
        )
    }
    single {
        AssessmentRetakeService(
            assessmentRepository = get(),
            startAssessment = get(),
        )
    }
    single {
        // The counterpart of StartAssessment: it owns both the completed write and the history
        // invalidation. The only other writer handed the store to invalidate is
        // ResetLearnerProgress, which owns its delete and invalidation the same way.
        CompleteAssessment(
            assessmentEngine = get(),
            assessmentRepository = get(),
            historyStore = get(),
        )
    }
    single {
        AssessmentSessionLoader(
            assessmentRepository = get(),
            curriculumRepository = get(),
            // Resuming is a presentation of the attempt, so it applies the learner's visibility;
            // the attempt itself is never rewritten to fit it.
            visibility = get<CurriculumVisibilityStateHolder>().visibility,
        )
    }
    single {
        LearningProgressService(
            // The visible projection, so a caller that supplies no attempts cannot bypass it.
            completedHistory = get<VisibleAssessmentHistory>(),
            curriculumRepository = get(),
        )
    }
}
