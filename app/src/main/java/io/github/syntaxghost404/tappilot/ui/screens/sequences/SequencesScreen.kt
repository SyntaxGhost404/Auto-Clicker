package io.github.syntaxghost404.tappilot.ui.screens.sequences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.ui.components.SequenceAvatar
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge
import io.github.syntaxghost404.tappilot.ui.components.TopLevelScaffold
import io.github.syntaxghost404.tappilot.ui.format.Summaries
import io.github.syntaxghost404.tappilot.ui.navigation.Sequences
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.withHaptic

interface SequencesActions {
    fun onSelectTab(tab: TopLevelRoute)
    fun onNew()
    fun onOpen(id: String)
    fun onStart(id: String)
    fun onRename(script: Script)
    fun onDuplicate(id: String)
    fun onExport(selection: List<Script>?)
    fun onImport()
    fun onDelete(id: String)
}

@Composable
fun SequencesScreen(
    scripts: List<Script>?,
    actions: SequencesActions,
    snackbarHostState: SnackbarHostState,
) {
    val listState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    TopLevelScaffold(
        current = Sequences,
        onSelectTab = actions::onSelectTab,
        title = stringResource(R.string.sequences_title),
        subtitle = scripts?.takeIf { it.isNotEmpty() }?.let {
            pluralStringResource(R.plurals.sequence_count, it.size, it.size)
        },
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = actions::onImport, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.action_import))
            }
            if (!scripts.isNullOrEmpty()) {
                IconButton(onClick = { actions.onExport(null) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Upload, contentDescription = stringResource(R.string.action_export_all))
                }
            }
        },
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_new_sequence)) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                onClick = withHaptic(onClick = actions::onNew),
                expanded = expandedFab,
            )
        },
    ) { padding ->
        when {
            scripts == null -> Box(Modifier.fillMaxSize())
            scripts.isEmpty() -> EmptyLibrary(padding, actions)
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(scripts, key = { _, script -> script.id }) { index, script ->
                    SequenceRow(
                        script = script,
                        index = index,
                        count = scripts.size,
                        actions = actions,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SequenceRow(script: Script, index: Int, count: Int, actions: SequencesActions, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    var menu by remember { mutableStateOf(false) }
    SegmentedListItem(
        onClick = { actions.onOpen(script.id) },
        onLongClick = { menu = true },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier,
        leadingContent = { SequenceAvatar(script) },
        supportingContent = {
            Text(
                Summaries.composition(resources, script) + " · " + Summaries.stopRule(resources, script.stopRule),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = withHaptic { actions.onStart(script.id) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start))
                }
                Box {
                    IconButton(onClick = { menu = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onRename(script)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_duplicate)) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onDuplicate(script.id)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_export)) },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onExport(listOf(script))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onDelete(script.id)
                            },
                        )
                    }
                }
            }
        },
    ) {
        Text(script.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyLibrary(padding: PaddingValues, actions: SequencesActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
            ShapeBadge(
                MaterialShapes.Cookie12Sided,
                MaterialTheme.colorScheme.tertiaryContainer,
                120.dp,
                modifier = Modifier.offset(x = (-22).dp, y = 10.dp),
            )
            ShapeBadge(
                MaterialShapes.Clover4Leaf,
                MaterialTheme.colorScheme.primaryContainer,
                92.dp,
                modifier = Modifier.offset(x = 30.dp, y = (-24).dp),
            )
            ShapeBadge(MaterialShapes.Pill, MaterialTheme.colorScheme.primary, 64.dp, modifier = Modifier.offset(x = 8.dp, y = 22.dp)) {
                Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.size(24.dp))
        Text(
            stringResource(R.string.sequences_empty_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.sequences_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(24.dp))
        FilledTonalButton(onClick = actions::onImport, shapes = ButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.action_import))
        }
        Spacer(Modifier.size(96.dp).fillMaxWidth())
    }
}
