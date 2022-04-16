package net.robinfriedli.exec

import java.util.concurrent.Callable
import java.util.function.Function

open class BaseInvoker : Invoker {

    /**
     * Central invoke implementation called by all other methods. This method is responsible for applying [ModeWrapper]s
     * to the task so be sure to call this implementation when implementing a custom [Invoker].
     */
    @Throws(Exception::class)
    override fun <T> invoke(mode: Mode, task: Callable<T>): T {
        val modeWrapper = mode.getWrapper()
        var actualTask = task
        if (modeWrapper != null) {
            for (currentModeWrapper in modeWrapper) {
                actualTask = currentModeWrapper.wrap(actualTask)
            }
        }
        return actualTask.call()
    }

    override fun invoke(mode: Mode, runnable: Runnable) {
        invokeChecked<Any?>(mode) {
            runnable.run()
            null
        }
    }

    override fun <T> invoke(
        mode: Mode,
        task: Callable<T>,
        exceptionMapper: Function<Exception, RuntimeException>
    ): T {
        return try {
            invoke(mode, task)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw exceptionMapper.apply(e)
        }
    }

    override fun <T> invokeChecked(mode: Mode, task: Callable<T>): T {
        return invoke(mode, task, { cause: Exception ->
            RuntimeException(
                cause
            )
        })
    }

    override fun combine(invoker: Invoker): Invoker {
        return CombinedInvoker(this, invoker)
    }
}
