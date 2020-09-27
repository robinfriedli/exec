package net.robinfriedli.exec

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

    }

}