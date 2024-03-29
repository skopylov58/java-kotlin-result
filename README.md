# java-kotlin-result

![example workflow](https://github.com/skopylov58/java-kotlin-result/actions/workflows/gradle.yml/badge.svg)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/skopylov58/java-kotlin-result/actions/workflows/gradle.yml)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://skopylov58.github.io/java-kotlin-result/)
[![Habr.com publication](https://badgen.net/badge/habr.com/publication/green)](https://habr.com/ru/post/721326/)

# Port of the Kotlin Result API to the Java

In this article I would like to describe some design considerations behind this port.

## Why Kotlin Result

Why Result? Because Result is eagerly needed and wanted and at the same time missing feature of standard Java library.

Why Kotlin? Why not? Kotlin has decent standard library and I do not want to invent my own APIs. If you are from Kotlin world, then there will not be any problems to you to use this library, if you from Java world then you will have chance to feel the taste of Kotlin.

## Result catches only Exceptions

Result does not catch any ```Throwable```s like do some libraries (Vavr for example). The reason is simple, Throwable includes Errors like OutOfMemoryError, StackOverflowError and many other errors that are considered to be un-recoverable. There is no use to catch and handle these errors, it would better stop application and fix the problem.


## Some add-ons to the Kotlin Result

### Handling and logging exceptions uniformly

In the production it is highly desireable to use uniform approach to exception logging and handling across the whole application or project. But Kotlin API (and many others) do not have means to force the user to handle exceptions uniformly. Let's look at the code below.

```java
    var some = runCatching(() -> {
        ...
        //may throw NullPointerException
        ...
    })
    .filter(...)
    .map(...)
    .recover(...)
    .getOrDefault(...);
```
You may notice that possible exceptions are not handled or logged at all. Well, exception may be handled inside ```onFailure(...)``` method, but there are not any guarantees that programmer will do that.

Another problem - what to do with RuntimeExceptions like NullPointerException, ArrayIndexOutOfBoundException and others? Initially Java creators  consider runtime exceptions as bugs indicators, so run-time exceptions should not be caught, but instead, bugs should be fixed. But in practice, developers often wrap checked exceptions into RuntimeException for variety of reasons - comply with existing APIs, etc.

To force uniform exception handling, I have introduced exception interceptor which is just ```Consumer<Exception>```. And it's up to library user to define own exception logging and handling policies. Let's say you have decided to log all runtime exceptions on WARNING level, all checked exceptions on TRACE level and disable (do not catch) NullPointerException. This policy can be implemented as follows:

```java
        Consumer<Exception> logException = e -> {
            Logger logger = System.getLogger("result");
            Level level = e instanceof RuntimeException ? Level.WARNING : Level.TRACE;
            logger.log(level, e.getMessage(), e);
        };
        
        Consumer<Exception> banNPE = e -> {
            if (e instanceof NullPointerException npe) {
                throw npe;
            }
        };

        Failure.withInterceptor(logException.andThen(banNPE));
```

```Failure.withInterceptor``` method will set interceptor globally for JVM, and after that all Result exceptions in the application will be logged and handled in the same uniform way. Be aware that there is not any default interceptor.

### Are the using of interceptors thread safe?

As per my understanding, yes, it is thread-safe as long as interceptors are pure, i.e.
- do not have/maintain any own internal state
- do not use/produce any side effects

You may say that exception logging is a side effect, and this is correct from theoretical point of view. But practically all logging libraries I have met are thread safe.


### Integration with Java's Optional and Stream

Result seamlessly integrates with standard Java Optional or Stream

```java
    List<URL> foo(List<String> urls) {
        return urls.stream()        //Stream<String>
        .map(Result.lift(URL::new)) //Stream<Result>, may throw MalformedURLException
        .flatMap(Result::stream)    //Stream<URL>, this filters failures
        .toList();
    }
```

### AutoCloseable Result

Result implements AutoCloseable interface and could be used in the try-with-resource block to automatically close resources. Sample below opens socket, writes some bytes to the output stream. Socket will be closed automatically after last curly brace (if successfully opened).

```java
        try (var socket = Result.runCatching(() -> new Socket("localhost", 1234))) {
            socket.mapCatching(Socket::getOutputStream)
            .onSuccessCatching(o -> o.write(new byte[] { 1, 2, 3, 4 }))
            .onFailure(e -> System.out.println(e));
        };
```

