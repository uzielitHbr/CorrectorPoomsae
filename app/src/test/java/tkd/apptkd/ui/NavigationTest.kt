package tkd.apptkd.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tkd.apptkd.ui.navigation.Screen

class NavigationTest {

    @Test
    fun testScreenRoutesAreUniqueAndDefined() {
        val screens = listOf(
            Screen.Home,
            Screen.Practice,
            Screen.Summary,
            Screen.History,
            Screen.Settings
        )

        val routes = screens.map { it.route }
        assertEquals("Debe haber exactamente 5 pantallas base", 5, routes.size)
        assertEquals("Todas las rutas de navegación deben ser únicas", routes.toSet().size, routes.size)

        assertTrue(routes.contains("home"))
        assertTrue(routes.contains("practice"))
        assertTrue(routes.contains("summary"))
        assertTrue(routes.contains("history"))
        assertTrue(routes.contains("settings"))
    }
}
