package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.jetbrains.compose.resources.stringResource
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.placeholder_detail_argument
import kmp_learning_app.shared.generated.resources.placeholder_detail_interaction_count
import kmp_learning_app.shared.generated.resources.placeholder_detail_title
import kmp_learning_app.shared.generated.resources.placeholder_go_back
import kmp_learning_app.shared.generated.resources.placeholder_record_interaction
import kmp_learning_app.shared.generated.resources.placeholder_open_detail
import kmp_learning_app.shared.generated.resources.placeholder_start_body
import kmp_learning_app.shared.generated.resources.placeholder_start_title

@Composable
@Preview
fun App() {
    val dependencies = remember { AppDependencies() }

    MaterialTheme {
        AppShell(dependencies = dependencies)
    }
}

@Composable
private fun AppShell(
    dependencies: AppDependencies,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(
        appNavigationSavedStateConfiguration,
        AppRoute.PlaceholderStart,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .safeContentPadding(),
            entryDecorators = listOf(
                // Saveable state must be installed before the ViewModel decorator so each
                // navigation entry owns the state registry used by its ViewModel store owner.
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider = entryProvider {
                entry<AppRoute.PlaceholderStart> {
                    PlaceholderStartDestination(
                        onOpenDetail = {
                            backStack.add(AppRoute.PlaceholderDetail(itemId = "placeholder-001"))
                        },
                    )
                }
                entry<AppRoute.PlaceholderDetail> { route ->
                    val viewModel = viewModel {
                        dependencies.createPlaceholderDetailViewModel(route.itemId)
                    }

                    PlaceholderDetailDestination(
                        viewModel = viewModel,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun PlaceholderStartDestination(onOpenDetail: () -> Unit) {
    PlaceholderDestinationLayout {
        Text(
            text = stringResource(Res.string.placeholder_start_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(Res.string.placeholder_start_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
            modifier = Modifier.padding(top = 24.dp),
            onClick = onOpenDetail,
        ) {
            Text(text = stringResource(Res.string.placeholder_open_detail))
        }
    }
}

@Composable
private fun PlaceholderDetailDestination(
    viewModel: PlaceholderDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlaceholderDestinationLayout {
        Text(
            text = stringResource(Res.string.placeholder_detail_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(Res.string.placeholder_detail_argument, uiState.itemId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(
                Res.string.placeholder_detail_interaction_count,
                uiState.interactionCount,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
            modifier = Modifier.padding(top = 24.dp),
            onClick = viewModel::recordInteraction,
        ) {
            Text(text = stringResource(Res.string.placeholder_record_interaction))
        }
        Button(
            modifier = Modifier.padding(top = 24.dp),
            onClick = onBack,
        ) {
            Text(text = stringResource(Res.string.placeholder_go_back))
        }
    }
}

@Composable
private fun PlaceholderDestinationLayout(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}
