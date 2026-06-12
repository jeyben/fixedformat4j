---
title: Get fixedformat4j
---

# Get the latest version of fixedformat4j

fixedformat4j 1.8.0 is published to **Maven Central**.

## Requirements

- **Java 11 or later** — Java 8 is no longer supported as of version 1.4.0.

## Add the dependency

Maven Central is the default repository for all three build tools below — no extra repository configuration or authentication is needed.

The artifact is also browsable at [central.sonatype.com](https://central.sonatype.com/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j).

### Maven

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.8.0</version>
</dependency>
```

### Gradle

Groovy DSL (`build.gradle`):

```groovy
implementation 'com.ancientprogramming.fixedformat4j:fixedformat4j:1.8.0'
```

Kotlin DSL (`build.gradle.kts`):

```kotlin
implementation("com.ancientprogramming.fixedformat4j:fixedformat4j:1.8.0")
```

### Ivy

Add to your `ivy.xml` dependencies block:

```xml
<dependency org="com.ancientprogramming.fixedformat4j"
            name="fixedformat4j"
            rev="1.8.0"/>
```

## Optional: compile-time validation

Since 1.9.0 a second artifact, `fixedformat4j-processor`, validates `@Field` / `@Record`
configuration at compile time. It is an annotation processor — wire it into the compiler
rather than the runtime classpath:

```xml
<annotationProcessorPaths>
  <path>
    <groupId>com.ancientprogramming.fixedformat4j</groupId>
    <artifactId>fixedformat4j-processor</artifactId>
    <version>1.9.0</version>
  </path>
</annotationProcessorPaths>
```

```groovy
annotationProcessor 'com.ancientprogramming.fixedformat4j:fixedformat4j-processor:1.9.0'
```

See [Compile-time validation](usage/compile-time-validation) for the full setup options and
the list of checks.

## Logging

fixedformat4j uses [SLF4J](https://www.slf4j.org/) for logging. SLF4J is a logging facade — you must provide a binding on the classpath to route log output to your preferred logging framework (e.g. Logback, Log4j 2, or `slf4j-simple`).

Example with Logback:

```xml
<dependency>
  <groupId>ch.qos.logback</groupId>
  <artifactId>logback-classic</artifactId>
  <version>1.5.18</version>
</dependency>
```

If no binding is found, SLF4J will print a one-time warning and silently discard all log output — the library will still function correctly.

