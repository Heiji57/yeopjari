package com.yeopjari.app.platform

import android.os.Handler
import android.os.Looper
import com.yeopjari.core.scenario.Ticker

/** 시나리오 재생을 메인 스레드에서. 프레임 처리(화면 갱신·음성)가 메인 스레드라 편하다. */
class HandlerTicker(private val handler: Handler = Handler(Looper.getMainLooper())) : Ticker {
    override fun schedule(delayMs: Long, action: () -> Unit) {
        handler.postDelayed({ action() }, delayMs)
    }
}
