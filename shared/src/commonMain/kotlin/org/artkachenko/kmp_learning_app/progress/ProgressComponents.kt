package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_percentage
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_weak_label
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Shared building blocks for the Progress dashboard and the Topic performance drill-down so the
 * two screens present observation-based statistics identically.
 */
@Composable
internal fun ProgressPerformanceCard(
    title: String,
    subtitle: String?,
    correctCount: Int,
    answeredCount: Int,
    percentage: Double,
    modifier: Modifier = Modifier,
    isWeak: Boolean = false,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(Res.string.progress_score, correctCount, answeredCount))
            Text(stringResource(Res.string.progress_percentage, formatProgressPercentage(percentage)))
            if (isWeak) {
                Text(
                    text = stringResource(Res.string.progress_weak_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun ProgressSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
internal fun ProgressMessage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/**
 * Domain models keep raw percentages; presentation shows a whole number when the value is exact
 * and one decimal place otherwise.
 */
internal fun formatProgressPercentage(percentage: Double): String {
    val rounded = (percentage * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
