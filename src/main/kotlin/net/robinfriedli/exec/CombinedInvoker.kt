package net.robinfriedli.exec

import java.util.concurrent.Callable

class CombinedInvoker(private val outer: Invoker, private val inner: Invoker) : BaseInvoker() {
    @Throws(Exception::class)
    override fun <T> invoke(mode: Mode, task: Callable<T>): T {
        return outer.invoke(Callable { inner.invoke(mode, task) })
    }
}
