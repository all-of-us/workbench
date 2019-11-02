package org.pmiops.workbench.test

import java.util.Random

/** Stubbed Random implementation for testing.  */
class FakeLongRandom(private val value: Long) : Random() {

    override fun nextLong(): Long {
        return value
    }
}
