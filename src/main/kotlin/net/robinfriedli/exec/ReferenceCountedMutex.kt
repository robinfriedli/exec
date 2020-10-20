package net.robinfriedli.exec

import java.util.concurrent.atomic.AtomicInteger

/**
 * Synchronisation primitive used by [MutexSync] mapped to a value to enable synchronising by that value. This Mutex removes
 * itself from the containing map automatically when no longer used.
 */
class ReferenceCountedMutex<T>(private val key: T, private val containingMap: MutableMap<T, ReferenceCountedMutex<T>>) {

    private val rc = AtomicInteger(Int.MIN_VALUE)

    fun incrementRc(): Int {
        return rc.updateAndGet { i ->
            when {
                i == Int.MIN_VALUE -> {
                    // Int.MIN_VALUE marks a freshly initialized Mutex, set to 1 on first increment
                    1
                }
                i < 1 -> {
                    // do not allow increment once rc has reached 0
                    i
                }
                else -> {
                    i + 1
                }
            }
        }
    }

    /**
     * @return true if the mutex removed itself from the map
     */
    fun decrementRc(): Boolean {
        val currentRc = rc.decrementAndGet()
        if (currentRc == 0) {
            return containingMap.remove(key) != null
        }
        return false
    }

}