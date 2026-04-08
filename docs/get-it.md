---
title: Get fixedformat4j
---

# Get the latest version of fixedformat4j

fixedformat4j 1.5.0 is published to **Maven Central**.

## Requirements

- **Java 11 or later** — Java 8 is no longer supported as of version 1.4.0.

## Add the dependency

Maven Central is the default Maven repository — no extra `<repository>` configuration or authentication is needed. Simply add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.5.0</version>
</dependency>
```

The artifact is also browsable at [central.sonatype.com](https://central.sonatype.com/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j).

## No internet access? Download manually

Download `fixedformat4j-1.5.0.jar` directly from the [1.5.0 release page](https://github.com/jeyben/fixedformat4j/releases/tag/1_5_0) and install it into your local Maven repository:

```bash
mvn install:install-file \
  -Dfile=fixedformat4j-1.5.0.jar \
  -DgroupId=com.ancientprogramming.fixedformat4j \
  -DartifactId=fixedformat4j \
  -Dversion=1.5.0 \
  -Dpackaging=jar
```

After that the standard `<dependency>` block above works as-is — no `<repository>` entry needed.

If you want to share the artifact across a team, deploy it to a private Nexus or Artifactory instance using `mvn deploy:deploy-file` with the same coordinates and your repository URL.

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
