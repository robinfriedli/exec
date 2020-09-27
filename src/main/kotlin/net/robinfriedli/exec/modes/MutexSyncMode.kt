package net.robinfriedli.exec.modes

import net.robinfriedli.exec.AbstractNestedModeWrapper
import net.robinfriedli.exec.MutexSync
import java.util.concurrent.Callable

/**
 * Mode that wraps tasks by executing them using [MutexSync]. This enables synchronising tasks by value by mapping a mutex
 * to the key value provided by the mutexKey parameter. Tasks running with this mode applied will run synchronised when
 * using the same MutexSync instance and the same mutexKey value.
 */
class MutexSyncMode<T>(private var mutexKey: T, private var mutexSync: MutexSync<T>) :
    AbstractNestedModeWrapper() {

    override fun <T> wrap(callable: Callable<T>): Callable<T> {
        return Callable {
            return@Callable mutexSync.evaluateChecked(mutexKey, callable)
        }
    }

}