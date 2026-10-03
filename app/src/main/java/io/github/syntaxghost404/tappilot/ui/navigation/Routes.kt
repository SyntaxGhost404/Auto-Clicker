package io.github.syntaxghost404.tappilot.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey

/** Destinations shown in the navigation bar. */
@Serializable
sealed interface TopLevelRoute : Route

@Serializable
data object Home : TopLevelRoute

@Serializable
data object Sequences : TopLevelRoute

@Serializable
data object Settings : TopLevelRoute

@Serializable
data object Welcome : Route

/** Prominent disclosure followed by the steps to enable the accessibility service. */
@Serializable
data object ServiceSetup : Route

@Serializable
data object SinglePoint : Route

@Serializable
data class SequenceEditor(val scriptId: String) : Route

@Serializable
data object HowTo : Route

@Serializable
data object Troubleshooting : Route

@Serializable
data object About : Route

/** Switches tabs while keeping Home at the root, so back from any tab returns home first. */
fun MutableList<NavKey>.selectTab(tab: TopLevelRoute) {
    if (lastOrNull() == tab) return
    clear()
    add(Home)
    if (tab != Home) add(tab)
}

/** The tab that owns the current screen. */
fun List<NavKey>.currentTab(): TopLevelRoute =
    asReversed().firstOrNull { it is TopLevelRoute } as? TopLevelRoute ?: Home
