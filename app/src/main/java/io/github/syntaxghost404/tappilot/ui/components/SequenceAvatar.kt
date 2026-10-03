package io.github.syntaxghost404.tappilot.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.core.model.Script

/** A sequence's avatar: its first letter on an expressive shape chosen from its id. */
@Composable
fun SequenceAvatar(script: Script, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    ShapeBadge(
        polygon = rememberShapeFor(script.id),
        color = MaterialTheme.colorScheme.primaryContainer,
        size = size,
        modifier = modifier,
    ) {
        Text(
            script.name.trim().firstOrNull()?.uppercase() ?: "#",
            style = if (size >= 48.dp) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
