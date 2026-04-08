---
title: Changelog
---

# Changelog

## 1.5.0 (2026-04-08)

### New features

- **Field-level `@Field` and `@Fields` annotations** — `@Field` and `@Fields` can now be placed
  directly on Java fields in addition to getter methods. The manager discovers them at runtime
  and derives the getter/setter by the `get`/`is` naming convention. This enables clean usage
  with Lombok (`@Getter`/`@Setter`) and reduces boilerplate in plain POJOs.

  ```java
  // Plain POJO — annotate the field instead of the getter
  @Record
  public class EmployeeRecord {
      @Field(offset = 1, length = 10)
      private String name;

      @Field(offset = 11, length = 8)
      @FixedFormatPattern("yyyyMMdd")
      private LocalDate hireDate;

      public String getName() { return name; }
      public void setName(String name) { this.name = name; }
      public LocalDate getHireDate() { return hireDate; }
      public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
  }

  // With Lombok — no getter/setter boilerplate needed
  @Getter @Setter @NoArgsConstructor
  @Record
  public class EmployeeRecord {
      @Field(offset = 1, length = 10)
      private String name;

      @Field(offset = 11, length = 8)
      @FixedFormatPattern("yyyyMMdd")
      private LocalDate hireDate;
  }
  ```

  If both the field and its getter carry `@Field`, an error is logged (configuration mismatch)
  and the field annotation is used.

---

## 1.4.0 (2026-04-05)

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
