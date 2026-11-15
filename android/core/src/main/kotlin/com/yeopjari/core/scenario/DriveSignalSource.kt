package com.yeopjari.core.scenario

import com.yeopjari.core.model.DriveFrame

/**
 * 차량 데이터를 주는 쪽. 앱은 이 인터페이스만 본다.
 *
 * - PleosDriveSource (app 모듈, SDK 승인 후): Vehicle SDK + NaviHelper 콜백을 DriveFrame 으로 합친다.
 * - ScenarioReplaySource: 시나리오를 시간 순서대로 재생한다.
 */
interface DriveSignalSource {
    fun start(listener: (DriveFrame) -> Unit)
    fun stop()
}

/** 일정 시간 뒤 실행. 앱은 Handler, 테스트는 즉시 실행. */
fun interface Ticker {
    fun schedule(delayMs: Long, action: () -> Unit)
}

/**
 * @param speed 재생 배속. 시연은 4~8배가 보기 좋다 (실제 30분 → 1분 남짓).
 */
class ScenarioReplaySource(
    private val scenario: Scenario,
    private val ticker: Ticker,
    private val speed: Double = 6.0,
) : DriveSignalSource {

    @Volatile
    private var running = false
    private var listener: ((DriveFrame) -> Unit)? = null

    override fun start(listener: (DriveFrame) -> Unit) {
        this.listener = listener
        running = true
        play(0)
    }

    override fun stop() {
        running = false
        listener = null
    }

    private fun play(i: Int) {
        if (!running || i >= scenario.frames.size) return
        val frame = scenario.frames[i]
        listener?.invoke(frame)
        val next = scenario.frames.getOrNull(i + 1) ?: return
        val delay = ((next.vehicle.atMs - frame.vehicle.atMs) / speed).toLong().coerceAtLeast(1)
        ticker.schedule(delay) { play(i + 1) }
    }
}
