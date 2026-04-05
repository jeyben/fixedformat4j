---
title: Get fixedformat4j
---

# Get the latest version of fixedformat4j

fixedformat4j 1.4.0 is published to **GitHub Packages**.

## Requirements

- **Java 11 or later** — Java 8 is no longer supported as of version 1.4.0.

## Step 1 — Authenticate with GitHub Packages

GitHub Packages requires a GitHub personal access token (classic) with at least the `read:packages` scope, even for public packages.

Create a token at **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)** and add the following server to your Maven `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

## Step 2 — Add the repository

Add the GitHub Packages registry to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/jeyben/fixedformat4j</url>
  </repository>
</repositories>
```

## Step 3 — Add the dependency

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.4.0</version>
</dependency>
```

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
