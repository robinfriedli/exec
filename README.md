# exec

Task execution library derived from [JXP's](https://github.com/robinfriedli/JXP) execution framework.

This library provides task execution and synchronisation applying certain reusable task execution modes.
For example, one could create a mode that manages a transaction when executing a task.

## Installation
### Gradle
```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.robinfriedli:exec:1.2.2"
}
```

### Maven
```
<dependency>
  <groupId>com.github.robinfriedli</groupId>
  <artifactId>exec</artifactId>
  <version>1.2.2</version>
  <type>pom</type>
</dependency>

<repository>
    <name>jitpack.io</name>
    <url>https://jitpack.io</url>
</repository>
```

Also, make sure you are using the Kotlin JVM plugin and Kotlin is set up.
Refer to the guide for [Gradle](https://kotlinlang.org/docs/reference/using-gradle.html) and [Maven](https://kotlinlang.org/docs/reference/using-maven.html).

Java example:
```java
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
```

Equivalent Kotlin example
```kotlin
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
```

Or check out [JXP](https://github.com/robinfriedli/JXP) for examples in production. E.g.:

From [AbstractContext](https://github.com/robinfriedli/JXP/blob/master/src/main/java/net/robinfriedli/jxp/persist/AbstractContext.java#L350):
```java
@Override
public <E> E invoke(boolean commit, boolean instantApply, Callable<E> task) {
    Mode mode = Mode.create()
        .with(new MutexSyncMode<>(getMutexKey(), GLOBAL_CONTEXT_SYNC))
        .with(getTransactionMode(instantApply, false).shouldCommit(commit));
    return invoke(mode, task);
}

@Override
public <E> E invoke(Mode mode, Callable<E> task) {
    Invoker invoker = Invoker.newInstance();
    return invoker.invoke(mode, task, e -> new PersistException("Exception in task", e));
}
```
From [AbstractTransactionalMode](https://github.com/robinfriedli/JXP/blob/master/src/main/java/net/robinfriedli/jxp/exec/AbstractTransactionalMode.java):
```java
@NotNull
@Override
public <E> Callable<E> wrap(@NotNull Callable<E> callable) {
    return () -> {
        activeTransaction = context.getTransaction();
        newTransaction = getTransaction();
        oldTransaction = null;
        if (activeTransaction != null) {
            switchTx();
        } else {
            openTx();
        }

        E returnValue;

        try {
            returnValue = callable.call();
            activeTransaction.internal().apply();
            if (!activeTransaction.isApplyOnly()) {
                if (shouldCommit) {
                    activeTransaction.internal().commit(writeToFile);
                } else {
                    context.getUncommittedTransactions().add(activeTransaction);
                }
            }
        } catch (Exception e) {
            try {
                activeTransaction.internal().assertRollback();
            } catch (Exception e1) {
                Logger logger = context.getBackend().getLogger();
                logger.error("Exception while rolling back changes", e1);
            }
            throw new PersistException(e.getClass().getSimpleName() + " thrown while running task. Closing transaction.", e);
        } finally {
            List<QueuedTask<?>> queuedTasks = activeTransaction.getQueuedTasks();
            boolean failed = activeTransaction.failed();
            closeTx();

            queuedTasks.forEach(t -> {
                if (failed && t.isCancelOnFailure()) {
                    t.cancel(false);
                } else {
                    t.runLoggingErrors();
                }
            });
        }

        return returnValue;
    };
}
```
This is an example of how Modes can be used to manage a Transaction when executing a task.