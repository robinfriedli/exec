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

            // It is possible that some thread cleared the mutex just after this thread retrieved it from the map
            // but before this thread increments the rc. In this case the thread that removed the mutex also decremented
            // the rc again to a negative integer so we can check here whether the previous rc was indeed negative when
            // incrementing and retry getting the mutex. The order of these operations (the removing thread decrementing
            // rc twice then this thread incrementing rc) is guaranteed to be correct since incrementRc() and decrementRc()
            // are both synchronised methods.
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