package net.robinfriedli.exec.modes

import net.robinfriedli.exec.AbstractNestedModeWrapper
import net.robinfriedli.exec.Invoker
import net.robinfriedli.exec.Mode
import net.robinfriedli.exec.MutexSync
import org.testng.Assert.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ModesTest {

    @DataProvider
    fun boolDataProvider(): Array<Any> {
        return arrayOf(true, false)
    }

    @Test(dataProvider = "boolDataProvider")
    fun testForkMode(immutableMode: Boolean) {
        val mode1 = Mode
            .create()
            .with(AdditionModeWrapper(2))
            .with(AdditionModeWrapper(7))
        val mode2 = if (immutableMode) {
            mode1.immutable()
        } else {
            mode1.fork()
        }.with(AdditionModeWrapper(4))

        val invoker = Invoker.defaultInstance
        val res1 = invoker.invoke<Int>(mode1) { 3 }
        val res2 = invoker.invoke<Int>(mode2) { 3 }

        assertEquals(res1, 12)
        assertEquals(res2, 16)
    }

    class AdditionModeWrapper(private val summand: Int) : AbstractNestedModeWrapper() {
        override fun <T> wrap(callable: Callable<T>): Callable<T> {
            return Callable {
                (callable.call() as Int + summand) as T
            }
        }

    }

    @Test
    fun testExceptionWrappingMode() {
        val invoker = Invoker.newInstance()
        val mode = Mode.create().with(ExceptionWrappingMode { e: Exception -> TestException(e) })

        var caught: TestException? = null
        try {
            invoker.invoke(mode) {
                throw IllegalArgumentException()
            }
        } catch (e: TestException) {
            caught = e
        }

        assertNotNull(caught)
        assertTrue(caught!!.cause is IllegalArgumentException)
    }

    @Test
    fun testSynchronisationMode() {
        val lock = Any()

        val invoker = Invoker.newInstance()
        val mode = Mode.create().with(SynchronisationMode(lock))

        val counter = AtomicInteger(0)
        val runCount = AtomicInteger(0)
        val failed = AtomicBoolean(false)
        val threads = ArrayList<Thread>()
        for (i in 0 until 10) {
            val thread = Thread {
                invoker.invoke(mode) {
                    assertSynchronised(counter, runCount, failed)
                }
            }

            threads.add(thread)
            thread.start()
        }

        for (t in threads) {
            t.join()
        }

        assertEquals(runCount.get(), 10)
        assertEquals(counter.get(), 0)
        assertFalse(failed.get())
    }

    @Test
    fun testMutexSyncMode() {
        val mutexSync = MutexSync<Int>()
        val invoker = Invoker.newInstance()
        val mode1 = Mode.create().with(MutexSyncMode(1, mutexSync))
        val mode2 = Mode.create().with(MutexSyncMode(2, mutexSync))

        val threads = ArrayList<Thread>()
        val failed = AtomicBoolean(false)
        val counter = AtomicInteger(0)
        val runCount = AtomicInteger(0)

        // assert two threads with different mutex keys can run in parallel
        val thread1 = Thread {
            invoker.invoke(mode1) {
                counter.incrementAndGet()
                Thread.sleep(500)
                if (counter.get() != 2) {
                    failed.set(true)
                }

                counter.decrementAndGet()
                runCount.incrementAndGet()
            }
        }
        threads.add(thread1)
        val thread2 = Thread {
            invoker.invoke(mode2) {
                counter.incrementAndGet()
                Thread.sleep(1000)
                counter.decrementAndGet()
                runCount.incrementAndGet()
            }
        }
        threads.add(thread2)
        thread1.start()
        thread2.start()

        Thread.sleep(500)

        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        for (i in 0 until 20) {
            val thread = Thread {
                invoker.invoke(mode1) {
                    assertSynchronised(counter1, runCount, failed)
                }
            }

            threads.add(thread)
            thread.start()
        }

        for (i in 0 until 20) {
            val thread = Thread {
                invoker.invoke(mode2) {
                    assertSynchronised(counter2, runCount, failed)
                }
            }

            threads.add(thread)
            thread.start()
        }

        for (t in threads) {
            t.join()
        }

        assertEquals(runCount.get(), 42)
        assertEquals(counter.get(), 0)
        assertEquals(counter1.get(), 0)
        assertEquals(counter2.get(), 0)
        assertFalse(failed.get())
    }

    private fun assertSynchronised(counter: AtomicInteger, runCount: AtomicInteger, failed: AtomicBoolean) {
        val prevVal = counter.getAndIncrement()
        if (prevVal != 0) {
            failed.set(true)
        }

        Thread.sleep(100)

        val prevVal2 = counter.getAndDecrement()
        if (prevVal2 != 1) {
            failed.set(true)
        }

        Thread.sleep(100)

        if (counter.get() != 0) {
            failed.set(true)
        }

        runCount.incrementAndGet()
    }

    class TestException(cause: Exception) : RuntimeException(cause)

}