---
title: Get fixedformat4j
---

# Get the latest version of fixedformat4j

Add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.4.0</version>
</dependency>
```

The library is available on [Maven Central](https://central.sonatype.com/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j).

## Requirements

- **Java 11 or later** — Java 8 is no longer supported as of version 1.4.0.

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

---

[Home](index) | [Usage](usage/) | [FAQ](faq) | [Changelog](changelog)
