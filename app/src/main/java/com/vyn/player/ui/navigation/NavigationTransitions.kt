package com.vyn.player.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Centralized navigation transition configuration.
 * All screens in the app inherit these transitions automatically
 * through the NavHost, ensuring consistent animation behavior.
 */
object NavigationTransitions {

    private const val DURATION = 300

    /** Forward navigation: new screen slides in from right */
    val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    /** Forward navigation: current screen slides out to left */
    val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(DURATION))
    }

    /** Back navigation: previous screen slides in from left */
    val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    /** Back navigation: current screen slides out to right */
    val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(DURATION))
    }
}
