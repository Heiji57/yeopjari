package com.yeopjari.app.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 음성 안내. 기본은 안드로이드 TTS.
 * TODO(선택): Pleos Gleo AI SDK 의 음성 합성으로 바꾸면 차량 목소리와 통일된다.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pending = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            tts.setSpeechRate(0.95f)
            ready = true
            pending.forEach { speakNow(it) }
            pending.clear()
        }
    }

    fun speak(text: String) {
        if (ready) speakNow(text) else pending += text
    }

    private fun speakNow(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
