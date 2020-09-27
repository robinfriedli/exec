package net.robinfriedli.exec.modes

import net.robinfriedli.exec.AbstractNestedModeWrapper
import java.util.concurrent.Callable

/**
 * Mode that runs the given task synchronised using the provided synchronisationLock as lock.
 */
class SynchronisationMode(private val synchronisationLock: Any) : AbstractNestedModeWrapper() {

    override fun <T> wrap(callable: Callable<T>): Callable<T> {
        return Callable {
            synchronized(synchronisationLock) {
                return@Callable callable.call()
            }
        }
    }

}