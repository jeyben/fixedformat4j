---
title: Changelog
---

# Changelog

## 1.4.0 (unreleased)

### New features

- **`LocalDate` support** — `java.time.LocalDate` is now a first-class field type handled automatically by `ByTypeFormatter`. No custom formatter needed. Configure the date pattern with `@FixedFormatPattern` (default: `yyyyMMdd`).

  ```java
  @Field(offset = 1, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  public LocalDate getEventDate() { return eventDate; }
  ```

  String `"20260405"` parses to `LocalDate.of(2026, 4, 5)`; exporting writes `"20260405"` back.

### Breaking changes

- **Java 11 minimum** — Java 8 is no longer supported. The minimum required runtime is Java 11.
- **Logging: SLF4J replaces Commons Logging** — The library no longer depends on Apache Commons Logging. Logging is now done via [SLF4J](https://www.slf4j.org/). If your project relied on the transitive `commons-logging` dependency, you will need to add an SLF4J binding instead (e.g. `logback-classic` or `slf4j-simple`). See [Get It](get-it) for details.

### Documentation

- Added Quick Start guide, Examples page, and an enriched Annotations reference.

---

[Home](index) | [Usage](usage/) | [Get It](get-it) | [FAQ](faq)
