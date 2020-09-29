package net.robinfriedli.exec

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

/**
 * Task executor that enables synchronising tasks by value by mapping a mutex to each used value. The mutexes remove
 * themselves from the map when no longer used, see [ReferenceCountedMutex].
 */
class MutexSync<K> {

    private val mutexMap: MutableMap<K, ReferenceCountedMutex<K>> = ConcurrentHashMap()

    @Throws(Exception::class)
    fun <T> evaluateChecked(key: K, toRun: Callable<T>): T {
        var mutex: ReferenceCountedMutex<K>
        while (true) {
            mutex = mutexMap.computeIfAbsent(key, { k: K -> ReferenceCountedMutex(k, mutexMap) })
            val currentRc = mutex.incrementRc()

            // An rc of 0 marks an invalid mutex that was or currently is being removed from the map, meaning the mutex
            // retrieved from the map was in the process of being removed by some other thread or at least before this
            // thread managed to increment the rc, now failing to do so because once the rc has reached 0 it cannot be
            // incremented. In this case the process of acquiring the mutex has to be retried.
            if (currentRc > 0) {
                break
            }
        }

        // -- thread C is here
        synchronized(mutex) {
            return try {
                toRun.call() // - thread B is still here
            } finally {
                mutex.decrementRc()
                // -- thread A is here (without reference counting it would have removed the mutex)
            }
        }
    }

    fun <E> evaluate(key: K, toRun: Supplier<E>): E {
        return try {
            evaluateChecked(key, { toRun.get() })
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            // suppliers do not throw checked exceptions
            throw RuntimeException(e)
        }
    }

    fun run(key: K, toRun: Runnable) {
        evaluate<Any?>(key, {
            toRun.run()
            null
        })
    }

}