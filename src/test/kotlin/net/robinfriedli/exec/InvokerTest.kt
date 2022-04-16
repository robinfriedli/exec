package net.robinfriedli.exec

import net.robinfriedli.exec.Invoker.Companion.newInstance
import net.robinfriedli.exec.Mode.Companion.create
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.util.concurrent.Callable

class InvokerTest {

    @Test
    fun testSimpleInvocation() {
        val invoker = BaseInvoker()
        val mode = Mode.empty

        var i = 1
        invoker.invoke(mode) {
            i += 1
        }

        assertEquals(i, 2)
    }

    @Test
    fun testApplyCustomModes() {
        var i = 1

        val invoker = Invoker.defaultInstance
        val mode = Mode.create()
            .with(object : AbstractNestedModeWrapper() {
                override fun <T> wrap(callable: Callable<T>): Callable<T> {
                    return Callable {
                        i += 5
                        return@Callable callable.call()
                    }
                }
            })
            .with(object : AbstractNestedModeWrapper() {
                override fun <T> wrap(callable: Callable<T>): Callable<T> {
                    return Callable {
                        i *= 2
                        return@Callable callable.call()
                    }
                }
            })

        invoker.invoke(mode) {
            i += 3
        }

        assertEquals(i, 15)
    }

    @Test
    fun testInvokeCallable() {
        val invoker = newInstance()
        val mode = create()
            .with(object : AbstractNestedModeWrapper() {
                override fun <T> wrap(callable: Callable<T>): Callable<T> {
                    return Callable {
                        val i = callable.call() as Int
                        (i + 5) as T
                    }
                }
            })
            .with(object : AbstractNestedModeWrapper() {
                override fun <T> wrap(callable: Callable<T>): Callable<T> {
                    return Callable {
                        val i = callable.call() as Int
                        (i * 2) as T
                    }
                }
            })

        val i = invoker.invoke<Int>(mode) { 3 }

        assertEquals(i, 11)
    }

    @Test
    fun testCombinedInvoker() {
        val mode = create()
            .with(object : AbstractNestedModeWrapper() {
                override fun <T> wrap(callable: Callable<T>): Callable<T> {
                    return Callable {
                        (callable.call() as Int + 7) as T
                    }
                }
            })

        val invoker = newInstance()
            .combine(MultiplyingInvoker(2))
            .combine(AddingInvoker(1))
            .combine(MultiplyingInvoker(3))
            .combine(AddingInvoker(2))
            .combine(MultiplyingInvoker(2))

        val i = invoker.invoke<Int>(mode) { 3 }

        // 3
        // + 7 (ModeWrapper is applied to the task directly, thus the innermost operation) = 10
        // then the invokers in reverse order because each invoker calls the super invoker first
        // * 2 = 20
        // + 2 = 22
        // * 3 = 66
        // + 1 = 67
        // * 2 = 134
        assertEquals(i, 134)
    }

    class MultiplyingInvoker(private val factor: Int) : BaseInvoker() {
        override fun <T> invoke(mode: Mode, task: Callable<T>): T {
            return (super.invoke(mode, task) as Int * factor) as T
        }
    }

    class AddingInvoker(private val summand: Int) : BaseInvoker() {
        override fun <T> invoke(mode: Mode, task: Callable<T>): T {
            return (super.invoke(mode, task) as Int + summand) as T
        }
    }

}