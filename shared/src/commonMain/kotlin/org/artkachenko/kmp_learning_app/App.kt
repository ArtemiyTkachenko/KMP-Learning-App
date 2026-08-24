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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.jetbrains.compose.resources.stringResource
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.placeholder_detail_argument
import kmp_learning_app.shared.generated.resources.placeholder_detail_title
import kmp_learning_app.shared.generated.resources.placeholder_go_back
import kmp_learning_app.shared.generated.resources.placeholder_open_detail
import kmp_learning_app.shared.generated.resources.placeholder_start_body
import kmp_learning_app.shared.generated.resources.placeholder_start_title

@Composable
@Preview
fun App() {
    MaterialTheme {
        AppShell()
    }
}

@Composable
internal fun AppShell(modifier: Modifier = Modifier) {
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
                    PlaceholderDetailDestination(
                        route = route,
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
    route: AppRoute.PlaceholderDetail,
    onBack: () -> Unit,
) {
    PlaceholderDestinationLayout {
        Text(
            text = stringResource(Res.string.placeholder_detail_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(Res.string.placeholder_detail_argument, route.itemId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
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
