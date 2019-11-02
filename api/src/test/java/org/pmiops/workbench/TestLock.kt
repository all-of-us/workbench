package org.pmiops.workbench

class TestLock {

    private var locked = false

    fun lock(): Int {
        synchronized(this) {
            if (locked) {
                return 0
            }

            locked = true
            return 1
        }
    }

    @Synchronized
    fun release(): Int {
        synchronized(this) {
            if (locked) {
                locked = false
                return 1
            }

            return 0
        }
    }
}
