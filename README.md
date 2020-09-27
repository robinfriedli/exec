# exec

Task execution library derived from [JXP's](https://github.com/robinfriedli/JXP) execution framework.

This library provides task execution and synchronisation applying certain reusable task execution modes.
For example, one could create a mode that manages a transaction when executing a task.

## Installation
### Gradle
```
repositories {
    jcenter()
}

dependencies {
    implementation "net.robinfriedli:exec:1.0"
}
```

### Maven
```
<dependency>
  <groupId>net.robinfriedli</groupId>
  <artifactId>exec</artifactId>
  <version>1.0</version>
  <type>pom</type>
</dependency>

<repository>
    <id>jcenter</id>
    <name>jcenter-bintray</name>
    <url>https://jcenter.bintray.com</url>
</repository>
```

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
```