package net.robinfriedli.exec

import java.util.concurrent.Callable
import java.util.function.Function

interface Invoker {

    companion object {
        @JvmStatic
        val defaultInstance = BaseInvoker()

        @JvmStatic
        fun newInstance(): Invoker {
            return BaseInvoker()
        }
    }

    /**
     * Runs the given callable with the given mode applied.
     *
     * The supplied mode is used to decorate the task by wrapping the task in the mode's [ModeWrapper].
     *
     * @param mode the custom mode that defines the task execution
     * @param task the callable to run
     * @param <T>  the return type of the callable
     * @return the result of the callable
     */
    @Throws(Exception::class)
    fun <T> invoke(mode: Mode, task: Callable<T>): T

    @Throws(Exception::class)
    fun <T> invoke(task: Callable<T>): T {
        return invoke(Mode.empty, task)
    }

    /**
     * Same as [Invoker.invoke] but accepts a Runnable and does not throw a checked exception as Runnables
     * cannot throw checked exceptions
     */
    fun invoke(mode: Mode, runnable: Runnable)

    fun invoke(runnable: Runnable) {
        invoke(Mode.empty, runnable)
    }

    /**
     * Runs the task with the given mode applied and handles checked exceptions by wrapping them into runtime exceptions
     * using the given function.
     *
     * @param exceptionMapper the function that returns a runtime exception based on the caught checked exception
     */
    fun <T> invoke(
        mode: Mode,
        task: Callable<T>,
        exceptionMapper: Function<Exception, RuntimeException>
    ): T

    fun <T> invoke(
        task: Callable<T>,
        exceptionMapper: Function<Exception, RuntimeException>
    ): T {
        return invoke(
            Mode.empty,
            task,
            exceptionMapper
        )
    }

    /**
     * Runs the task wrapping checked exceptions into [RuntimeException]. This is equivalent to calling
     * `invoke(mode, task, e -> new RuntimeException(e))`
     */
    fun <T> invokeChecked(mode: Mode, task: Callable<T>): T

    fun <T> invokeChecked(task: Callable<T>): T {
        return invokeChecked(Mode.empty, task)
    }

    /**
     * Creates a [CombinedInvoker] that combines Invokers by calling the supplied Invoker within this Invoker. Modes are
     * passed to the innermost invoke call
     */
    fun combine(invoker: Invoker): Invoker

}
