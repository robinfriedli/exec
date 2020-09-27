package net.robinfriedli.exec;

import java.util.concurrent.Callable;

import org.testng.annotations.*;

import static org.testng.Assert.*;

public class JavaTest {

    @Test
    public void testSimpleInvocation() {
        Invoker invoker = new BaseInvoker();
        Mode mode = Mode.create();

        MutableInteger i = new MutableInteger(1);
        invoker.invoke(mode, () -> i.add(1));

        assertEquals(i.get(), 2);
    }

    @Test
    public void testApplyCustomModes() {
        MutableInteger i = new MutableInteger(1);

        Invoker invoker = new BaseInvoker();
        Mode mode = Mode.create()
            .with(new AbstractNestedModeWrapper() {
                @Override
                public <T> Callable<T> wrap(Callable<T> callable) {
                    return () -> {
                        i.add(5);
                        return callable.call();
                    };
                }
            })
            .with(new AbstractNestedModeWrapper() {
                @Override
                public <T> Callable<T> wrap(Callable<T> callable) {
                    return () -> {
                        i.mult(2);
                        return callable.call();
                    };
                }
            });

        invoker.invoke(mode, () -> i.add(3));

        assertEquals(i.get(), 15);
    }

    @Test
    public void testInvokeCallable() throws Exception {
        Invoker invoker = Invoker.newInstance();
        Mode mode = Mode.create()
            .with(new AbstractNestedModeWrapper() {
                @SuppressWarnings("unchecked")
                @Override
                public <T> Callable<T> wrap(Callable<T> callable) {
                    return () -> {
                        int i = (Integer) callable.call();
                        return (T) (Object) (i + 5);
                    };
                }
            })
            .with(new AbstractNestedModeWrapper() {
                @SuppressWarnings("unchecked")
                @Override
                public <T> Callable<T> wrap(Callable<T> callable) {
                    return () -> {
                        int i = (Integer) callable.call();
                        return (T) (Object) (i * 2);
                    };
                }
            });

        int i = invoker.invoke(mode, () -> 3);

        assertEquals(i, 11);
    }

    private static class MutableInteger {

        private int i;

        private MutableInteger(int i) {
            this.i = i;
        }

        private void add(int a) {
            i += a;
        }

        private void mult(int a) {
            i *= a;
        }

        private int get() {
            return i;
        }

    }

}
