package tkd.apptkd.ui

import mx.taekwondo.poomsae.analysis.evaluation.FeedbackCode
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackMessageMapperTest {

    @Test
    fun `all FeedbackCode enum entries have a valid non-zero string resource mapping`() {
        for (code in FeedbackCode.entries) {
            val resId = FeedbackMessageMapper.getStringResId(code)
            assertTrue("Código $code debe tener un recurso válido no cero", resId != 0)
        }
    }
}

