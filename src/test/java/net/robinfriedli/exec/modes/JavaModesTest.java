package net.robinfriedli.exec.modes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.*;

import net.robinfriedli.exec.Invoker;
import net.robinfriedli.exec.Mode;
import net.robinfriedli.exec.MutexSync;

import static org.testng.Assert.*;

public class JavaModesTest {

    @Test
    public void testExceptionWrappingMode() throws Exception {
        Invoker invoker = Invoker.newInstance();
        Mode mode = Mode.create().with(new ExceptionWrappingMode(TestException::new));

        TestException caught = null;
        try {
            invoker.invoke(mode, () -> {
                throw new IllegalArgumentException();
            });
        } catch (TestException e) {
            caught = e;
        }

        assertNotNull(caught);
        assertTrue(caught.getCause() instanceof IllegalArgumentException);
    }

    @Test
    public void testSynchronisationMode() throws InterruptedException {
        Object lock = new Object();

        Invoker invoker = Invoker.newInstance();
        Mode mode = Mode.create().with(new SynchronisationMode(lock));

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger runCount = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> invoker.invokeChecked(mode, () -> {
                assertSynchronised(counter, runCount, failed);
                return null;
            }));

            threads.add(thread);
            thread.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(runCount.get(), 10);
        assertEquals(counter.get(), 0);
        assertFalse(failed.get());
    }

    @Test
    public void testMutexSyncMode() throws InterruptedException {
        MutexSync<Integer> mutexSync = new MutexSync<>();
        Invoker invoker = Invoker.newInstance();
        Mode mode1 = Mode.create().with(new MutexSyncMode<>(1, mutexSync));
        Mode mode2 = Mode.create().with(new MutexSyncMode<>(2, mutexSync));

        List<Thread> threads = new ArrayList<>();
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger runCount = new AtomicInteger(0);

        // assert two threads with different mutex keys can run in parallel
        Thread thread1 = new Thread(() -> invoker.invokeChecked(mode1, () -> {
            counter.incrementAndGet();
            Thread.sleep(500);
            if (counter.get() != 2) {
                failed.set(true);
            }

            counter.decrementAndGet();
            runCount.incrementAndGet();
            return null;
        }));
        threads.add(thread1);
        Thread thread2 = new Thread(() -> invoker.invokeChecked(mode2, () -> {
            counter.incrementAndGet();
            Thread.sleep(1000);
            counter.decrementAndGet();
            runCount.incrementAndGet();
            return null;
        }));
        threads.add(thread2);
        thread1.start();
        thread2.start();

        Thread.sleep(500);

        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            Thread thread = new Thread(() -> invoker.invokeChecked(mode1, () -> {
                assertSynchronised(counter1, runCount, failed);
                return null;
            }));

            threads.add(thread);
            thread.start();
        }

        for (int i = 0; i < 20; i++) {
            Thread thread = new Thread(() -> invoker.invokeChecked(mode2, () -> {
                assertSynchronised(counter2, runCount, failed);
                return null;
            }));

            threads.add(thread);
            thread.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(runCount.get(), 42);
        assertEquals(counter.get(), 0);
        assertEquals(counter1.get(), 0);
        assertEquals(counter2.get(), 0);
        assertFalse(failed.get());
    }

    private void assertSynchronised(AtomicInteger counter, AtomicInteger runCount, AtomicBoolean failed) throws InterruptedException {
        int prevVal = counter.getAndIncrement();
        if (prevVal != 0) {
            failed.set(true);
        }

        Thread.sleep(100);

        int prevVal2 = counter.getAndDecrement();
        if (prevVal2 != 1) {
            failed.set(true);
        }

        Thread.sleep(100);

        if (counter.get() != 0) {
            failed.set(true);
        }

        runCount.incrementAndGet();
    }

    private static class TestException extends RuntimeException {
        public TestException(Throwable cause) {
            super(cause);
        }
    }

}
