package com.riztechacademy.mandibhav.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer

/**
 * The app's navigation state: one back stack per top-level destination, and
 * which of them is showing. Switching tabs keeps every stack as it was.
 *
 * Plain state and plain functions, so it can be unit tested; the composable
 * [rememberTopLevelNavigation] makes it survive rotation and process death.
 */
class TopLevelNavigation(
    val startDestination: NavKey,
    currentTopLevel: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** The top-level destination whose stack is showing. */
    var currentTopLevel: NavKey by currentTopLevel
        private set

    /** The back stack that is showing. */
    val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(currentTopLevel)

    /**
     * Go to [key]. A top-level destination switches to its stack, as it was;
     * tapping the one already showing returns that stack to its first screen.
     * Anything else is pushed onto the current stack.
     */
    fun navigate(key: NavKey) {
        val stack = backStacks[key]
        when {
            stack == null -> currentStack.add(key)
            key == currentTopLevel -> while (stack.size > 1) stack.removeAt(stack.lastIndex)
            else -> currentTopLevel = key
        }
    }

    /**
     * Back: pop the current stack; from the first screen of any other tab,
     * return to the start destination's tab. Returns false when there is
     * nowhere left to go, which is the start destination's first screen.
     */
    fun goBack(): Boolean {
        val stack = currentStack
        return when {
            stack.size > 1 -> {
                stack.removeAt(stack.lastIndex)
                true
            }
            currentTopLevel != startDestination -> {
                currentTopLevel = startDestination
                true
            }
            else -> false
        }
    }

    /**
     * The stacks to show, bottom first: the start destination's, then the
     * current one if it is different. Putting the start tab underneath is what
     * makes Back from another tab land on Prices before leaving the app.
     */
    fun stacksInUse(): List<NavKey> =
        if (currentTopLevel == startDestination) listOf(startDestination)
        else listOf(startDestination, currentTopLevel)
}

/**
 * A [TopLevelNavigation] that is saved: every back stack with
 * [rememberNavBackStack], and the current tab with [rememberSerializable],
 * so rotation and process death bring back the same screens.
 */
@Composable
fun rememberTopLevelNavigation(
    startDestination: NavKey,
    topLevelDestinations: List<NavKey>,
): TopLevelNavigation {
    val currentTopLevel = rememberSerializable(stateSerializer = NavKeySerializer()) {
        mutableStateOf(startDestination)
    }
    val backStacks = topLevelDestinations.associateWith { key -> rememberNavBackStack(key) }
    return remember(startDestination, topLevelDestinations) {
        TopLevelNavigation(startDestination, currentTopLevel, backStacks)
    }
}

/**
 * Turns the stacks in use into the entries NavDisplay shows. Each stack has
 * its own decorators, so each screen keeps its saveable state and its
 * ViewModels while its tab is in the background.
 */
@Composable
fun TopLevelNavigation.entries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val entriesByStack = backStacks.mapValues { (topLevel, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            // The decorators find an entry's saved state and ViewModels by its
            // content key, and by default that is the key itself. The same
            // price open on two tabs would then share one ViewModel, and Back
            // on one tab would clear it under the other. Prefixing the tab
            // gives each stack its own.
            entryProvider = { key ->
                val entry = entryProvider(key)
                NavEntry(key = key, contentKey = "$topLevel/${entry.contentKey}", metadata = entry.metadata) {
                    entry.Content()
                }
            },
        )
    }
    return stacksInUse().flatMap { entriesByStack.getValue(it) }
}
