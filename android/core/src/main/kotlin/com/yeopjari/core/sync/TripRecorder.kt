package com.yeopjari.core.sync

import com.yeopjari.core.model.TripEvent

/**
 * 주행 이벤트를 모아서 서버로 보낸다. [FLUSH_SIZE] 개가 쌓이거나 flush() 를 부르면 보낸다.
 * 보내기 실패하면 버리지 않고 다음 flush 때 다시 보낸다 (터널·지하주차장 대비).
 */
class TripRecorder(private val api: YeopjariApi, val tripId: String) {

    companion object {
        const val FLUSH_SIZE = 20
    }

    private val pending = mutableListOf<TripEvent>()
    private val all = mutableListOf<TripEvent>()

    val recorded: List<TripEvent> get() = synchronized(this) { all.toList() }

    fun record(events: List<TripEvent>) {
        if (events.isEmpty()) return
        val shouldFlush = synchronized(this) {
            pending += events
            all += events
            pending.size >= FLUSH_SIZE
        }
        if (shouldFlush) flush()
    }

    /** @return 보내기 성공 여부 */
    fun flush(): Boolean {
        val batch = synchronized(this) { pending.toList().also { pending.clear() } }
        if (batch.isEmpty()) return true
        return try {
            api.sendEvents(tripId, batch)
            true
        } catch (e: Exception) {
            synchronized(this) { pending.addAll(0, batch) }
            false
        }
    }

    /** 남은 이벤트를 보내고 리포트를 받는다. */
    fun finish(): ReportDto {
        check(flush()) { "이벤트를 서버에 보내지 못했어요" }
        return api.finishTrip(tripId)
    }
}
