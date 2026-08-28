package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import androidx.compose.runtime.Composable
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.focused_practice_title
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingDestination
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FocusedPracticeDestination(
    launch: AssessmentTakingLaunch,
    onBack: () -> Unit,
    onAttemptPersisted: (String) -> Unit,
    onCompleted: (String) -> Unit,
) {
    AssessmentTakingDestination(
        title = stringResource(Res.string.focused_practice_title),
        launch = launch,
        onBack = onBack,
        onAttemptPersisted = onAttemptPersisted,
        onCompleted = onCompleted,
    )
}
