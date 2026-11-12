package tkd.apptkd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tkd.apptkd.ui.screens.HistoryScreen
import tkd.apptkd.ui.screens.HomeScreen
import tkd.apptkd.ui.screens.MovementDetailScreen
import tkd.apptkd.ui.screens.PracticeScreen
import tkd.apptkd.ui.screens.SettingsScreen
import tkd.apptkd.ui.screens.SummaryScreen

/**
 * Grafo oficial de navegación de la aplicación (Sección 14.7).
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPractice = { navController.navigate(Screen.Practice.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.Practice.route) {
            PracticeScreen(
                onNavigateBack = { navController.popBackStack() },
                onFinishPractice = {
                    navController.navigate(Screen.Summary.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
            )
        }
        composable(Screen.Summary.route) {
            SummaryScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToDetail = { index ->
                    navController.navigate(Screen.MovementDetail.createRoute(index))
                },
            )
        }
        composable(
            route = Screen.MovementDetail.route,
            arguments = listOf(
                navArgument("movementIndex") { type = NavType.IntType },
            ),
        ) {
            MovementDetailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSummary = { navController.navigate(Screen.Summary.route) },
                onStartPractice = { navController.navigate(Screen.Practice.route) },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
