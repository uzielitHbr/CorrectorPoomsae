package tkd.apptkd.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Practice : Screen("practice")
    data object Summary : Screen("summary")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
