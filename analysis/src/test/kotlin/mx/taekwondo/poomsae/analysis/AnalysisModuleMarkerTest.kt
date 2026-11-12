package mx.taekwondo.poomsae.analysis

import org.junit.Assert.assertNotNull
import org.junit.Test

class AnalysisModuleMarkerTest {
    @Test
    fun testModuleInitialization() {
        val marker = object : AnalysisModuleMarker {}
        assertNotNull(marker)
    }
}
