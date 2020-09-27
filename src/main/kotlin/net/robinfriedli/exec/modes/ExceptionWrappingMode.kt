package net.robinfriedli.exec.modes

import net.robinfriedli.exec.AbstractNestedModeWrapper
import java.util.concurrent.Callable
import java.util.function.Function

/**
 * Mode that maps exceptions thrown during task execution using the provided mapper function.
 */
class ExceptionWrappingMode(private var mapperFunction: Function<Exception, Exception>) : AbstractNestedModeWrapper() {
    override fun <T> wrap(callable: Callable<T>): Callable<T> {
        return Callable {
            try {
                return@Callable callable.call()
            } catch (e: Exception) {
                throw mapperFunction.apply(e)
            }
        }
    }
}