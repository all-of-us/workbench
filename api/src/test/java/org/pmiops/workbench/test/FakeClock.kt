package org.pmiops.workbench.test

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Mutable clock implementation for testing.  */
class FakeClock @JvmOverloads constructor(private var instant: Instant? = Instant.now(), private val zoneId: ZoneId = ZoneId.systemDefault()) : Clock() {

    override fun millis(): Long {
        return instant!!.toEpochMilli()
    }

    override fun getZone(): ZoneId {
        return zoneId
    }

    override fun instant(): Instant? {
        return instant
    }

    override fun withZone(zone: ZoneId): Clock {
        return FakeClock(instant, zone)
    }

    fun setInstant(instant: Instant) {
        this.instant = instant
    }

    fun increment(millis: Long) {
        setInstant(Instant.ofEpochMilli(this.instant!!.toEpochMilli() + millis))
    }
}
