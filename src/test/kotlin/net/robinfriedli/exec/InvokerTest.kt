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
        val mode = Mode.create()

        var i = 1
        invoker.invoke(mode) {
            i += 1
        }

        assertEquals(i, 2)
    }

    @Test
    fun testApplyCustomModes() {
        var i = 1

        val invoker = BaseInvoker()
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

}