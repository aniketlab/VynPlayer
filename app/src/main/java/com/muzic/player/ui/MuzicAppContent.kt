package com.muzic.player.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.muzic.player.ui.navigation.MuzicNavGraph

@Composable
fun MuzicAppContent() {
    val navController = rememberNavController()
    MuzicNavGraph(navController = navController)
}
