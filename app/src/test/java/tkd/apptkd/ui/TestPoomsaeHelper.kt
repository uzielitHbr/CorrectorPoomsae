package tkd.apptkd.ui

import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeLoadResult
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeLoader
import java.io.File

object TestPoomsaeHelper {
    fun loadDefinition(): PoomsaeDefinition {
        val candidates = listOf(
            File("src/main/assets/poomsae_taegeuk1.json"),
            File("app/src/main/assets/poomsae_taegeuk1.json"),
            File("../poomsae_taegeuk1.json"),
            File("poomsae_taegeuk1.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("poomsae_taegeuk1.json not found in test candidates: $candidates")
        val result = PoomsaeLoader.loadFromString(file.readText(Charsets.UTF_8))
        return (result as PoomsaeLoadResult.Success).definition
    }
}

