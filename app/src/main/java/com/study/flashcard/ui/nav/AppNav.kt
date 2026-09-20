package com.study.flashcard.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.study.flashcard.di.AppContainer
import com.study.flashcard.ui.exam.ExamScreen
import com.study.flashcard.ui.home.HomeScreen
import com.study.flashcard.ui.importscreen.ImportScreen
import com.study.flashcard.ui.progress.ProgressScreen
import com.study.flashcard.ui.settings.SettingsScreen
import com.study.flashcard.ui.study.StudyScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Bộ đề", Icons.Filled.Home),
    Tab(Routes.IMPORT, "Import", Icons.Filled.Add),
    Tab(Routes.PROGRESS, "Tiến độ", Icons.Filled.BarChart),
    Tab(Routes.SETTINGS, "Cài đặt", Icons.Filled.Settings)
)

/** Khung điều hướng chính: bottom bar 4 tab + 2 màn hình chi tiết (học / thi). */
@Composable
fun AppNav(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenImport = { nav.navigate(Routes.IMPORT) },
                    onStudy = { id -> nav.navigate(Routes.study(id)) },
                    onExam = { id -> nav.navigate(Routes.exam(id)) }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(container = container, onDone = { nav.navigate(Routes.HOME) })
            }
            composable(Routes.PROGRESS) { ProgressScreen(container = container) }
            composable(Routes.SETTINGS) { SettingsScreen(container = container) }
            composable(
                Routes.STUDY,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { entry ->
                StudyScreen(
                    container = container,
                    deckId = entry.arguments?.getLong("deckId") ?: 0L,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                Routes.EXAM,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { entry ->
                ExamScreen(
                    container = container,
                    deckId = entry.arguments?.getLong("deckId") ?: 0L,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
