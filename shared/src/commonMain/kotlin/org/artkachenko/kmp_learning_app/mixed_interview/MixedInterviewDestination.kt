package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.runtime.Composable
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingDestination
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MixedInterviewDestination(
    launch: AssessmentTakingLaunch,
    onBack: () -> Unit,
    onAttemptPersisted: (String) -> Unit,
    onCompleted: (String) -> Unit,
) {
    AssessmentTakingDestination(
        title = stringResource(Res.string.mixed_interview_title),
        launch = launch,
        onBack = onBack,
        onAttemptPersisted = onAttemptPersisted,
        onCompleted = onCompleted,
    )
}
