package tkd.apptkd.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

interface VoicePlayer {
    fun speak(text: String)
    fun shutdown()
}

class AndroidVoicePlayer(context: Context) : VoicePlayer, TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady: Boolean = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("es-MX"))
            if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                isReady = true
            }
        }
    }

    override fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tkd_feedback")
        }
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

class NoOpVoicePlayer : VoicePlayer {
    override fun speak(text: String) = Unit
    override fun shutdown() = Unit
}

