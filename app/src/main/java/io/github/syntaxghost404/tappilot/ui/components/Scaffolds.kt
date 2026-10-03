package io.github.syntaxghost404.tappilot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.ui.navigation.Home
import io.github.syntaxghost404.tappilot.ui.navigation.Sequences
import io.github.syntaxghost404.tappilot.ui.navigation.Settings
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute

private data class Tab(val route: TopLevelRoute, val label: Int, val selected: ImageVector, val unselected: ImageVector)

private val Tabs = listOf(
    Tab(Home, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    Tab(Sequences, R.string.nav_sequences, Icons.Rounded.Route, Icons.Outlined.Route),
    Tab(Settings, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
)

@Composable
fun AppNavigationBar(current: TopLevelRoute, onSelect: (TopLevelRoute) -> Unit) {
    ShortNavigationBar {
        Tabs.forEach { tab ->
            val selected = tab.route == current
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(if (selected) tab.selected else tab.unselected, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/**
 * Scaffold for the three navigation-bar destinations: a flexible large top app bar that collapses
 * on scroll, the navigation bar, and optional FAB and snackbars.
 */
@Composable
fun TopLevelScaffold(
    current: TopLevelRoute,
    onSelectTab: (TopLevelRoute) -> Unit,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                actions = actions,
                colors = appBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { AppNavigationBar(current, onSelectTab) },
        floatingActionButton = floatingActionButton,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        content = content,
    )
}

/** Scaffold for screens pushed on top of a tab, with a back button. */
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                colors = appBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.navigate_up))
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        content = content,
    )
}

/**
 * Screens sit on surfaceContainer so that segmented list items and cards (which use surface) read
 * as distinct groups, the Material 3 Expressive list pattern.
 */
@Composable
private fun appBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
)

/** Accent-coloured label above a group of settings or a form card. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** A roomy, softly tinted container for a form. */
@Composable
fun FormCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = color,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/**
 * A free-form row that matches [androidx.compose.material3.SegmentedListItem] styling, for settings
 * whose controls do not fit a list item (button groups, sliders).
 */
@Composable
fun SegmentedPanel(index: Int, count: Int, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape,
        color = ListItemDefaults.segmentedColors().containerColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), content = content)
    }
}

/**
 * Colours for controls placed on a coloured card. They are derived from the card's own content
 * colour, so contrast holds under any palette, including dynamic ones with bright dark-mode
 * containers.
 */
@Immutable
data class OnCardColors(val strong: Color, val onStrong: Color, val soft: Color, val onSoft: Color)

fun onCardColors(container: Color, content: Color) = OnCardColors(
    strong = content,
    onStrong = container,
    soft = content.copy(alpha = 0.12f),
    onSoft = content,
)
