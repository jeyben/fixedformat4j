---
title: Changelog
---

# Changelog

## 1.6.1 (unreleased)

### Bug fixes

- **`DateFormatter` (and `LocalDateFormatter` / `LocalDateTimeFormatter`) no longer over-strips
  padding characters** ([#33](https://github.com/jeyben/fixedformat4j/issues/33)) — When the
  configured `paddingChar` happened to be a character that also appears in the formatted date
  string (e.g. `paddingChar = '0'` with a time value whose seconds component is `00`), the
  previous `stripPadding` implementation removed those characters from the parsed string, leaving
  it too short and causing a `ParseException`. The fix introduces `AbstractPatternFormatter`,
  which overrides `stripPadding` to remove only leading/trailing padding characters rather than
  all occurrences of the character.

---

### Deprecations

- **`AbstractFixedFormatter.getRemovePadding` deprecated** — The method has been renamed to
  `stripPadding`, which better reflects its behaviour (it transforms a string, not returns a value).
  The old name carried a misleading `get` prefix that implied a zero-argument accessor.

  `getRemovePadding` remains callable and fully functional in 1.6.1; it now delegates to
  `stripPadding`. **It will be removed in 1.7.0.**

  **Migration:** rename any override of `getRemovePadding` to `stripPadding` — the signature is
  identical:

  ```java
  // Before (1.6.0 and earlier)
  @Override
  protected String getRemovePadding(String value, FormatInstructions instructions) { … }

  // After (1.6.1+)
  @Override
  protected String stripPadding(String value, FormatInstructions instructions) { … }
  ```

  **Call chain in 1.6.1:** `parse()` → `getRemovePadding()` → `stripPadding()`

  **Call chain in 1.7.0:** `parse()` → `stripPadding()` (direct; `getRemovePadding` removed)

---

## 1.6.0 (2026-04-09)

### New features

- **Repeating fields** — A single `@Field` annotation can now map consecutive same-format slots
  in a record to a Java array or ordered `Collection` via the new `count` attribute.
  Set `count` to the number of repetitions; the getter/setter must return `T[]`, `List<T>`,
  `LinkedList<T>`, `Set<T>`, `SortedSet<T>`, or `Collection<T>`. Each slot occupies `length`
  characters, starting at `offset + length * index`.

  ```java
  // Three 5-character product codes packed consecutively from position 1
  @Field(offset = 1, length = 5, count = 3)
  public String[] getProductCodes() { return productCodes; }

  // Same field mapped to a List
  @Field(offset = 1, length = 5, count = 3)
  public List<String> getProductCodes() { return productCodes; }
  ```

  The new `strictCount` attribute (default `true`) controls what happens when the
  collection size does not match `count` at export time: `true` throws a
  `FixedFormatException`; `false` logs a warning and exports `min(count, actualSize)` elements.

  ```java
  // Lenient: export however many elements are present, up to count
  @Field(offset = 1, length = 5, count = 3, strictCount = false)
  public List<String> getProductCodes() { return productCodes; }
  ```

  See [Repeating fields](usage/annotations#repeating-fields) in the annotation reference and
  [Example 7](examples#example-7--repeating-fields) for a full walkthrough.

- **`LocalDateTime` support** — `java.time.LocalDateTime` is now a first-class field type handled
  automatically by `ByTypeFormatter`. No custom formatter needed. `@FixedFormatPattern` is optional
  — only specify it when your format differs from the default (`yyyy-MM-dd'T'HH:mm:ss`).

  ```java
  // Default pattern — no @FixedFormatPattern needed
  @Field(offset = 1, length = 19)
  public LocalDateTime getCreatedAt() { return createdAt; }

  // Custom pattern — only required when overriding the default
  @Field(offset = 1, length = 14)
  @FixedFormatPattern("yyyyMMddHHmmss")
  public LocalDateTime getCreatedAt() { return createdAt; }
  ```

  String `"2026-04-09T14:30:00"` parses to `LocalDateTime.of(2026, 4, 9, 14, 30, 0)`; exporting
  writes `"2026-04-09T14:30:00"` back.

---

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

## 1.3.4 (2010-12-14)

### Bug fixes

- **Issue #23** — Removed unnecessary restriction of generic type `T` to `java.lang.Number` in `AbstractNumberFormatter`.
- **Issue #24** — Reverted the `paddingChar` honouring change introduced in 1.3.3 (issue #22).

---

## 1.3.3 (2010-12-14)

### New features

- **Issue #20** — `AbstractDecimalFormatter` now supports explicit rounding.

### Bug fixes

- **Issue #16** — `@FixedFormatDecimal` with more than 3 decimal places truncated the fractional digits.
- **Issue #21** — `AbstractDecimalFormatter` `DecimalFormat` usage was not thread-safe.
- **Issue #22** — `AbstractDecimalFormatter` hard-coded `'0'` as the padding character instead of honouring the `paddingChar` annotation setting.

---

## 1.3.2 (2010-12-03)

### Bug fixes

- **Issue #18** — `Sign.APPEND.apply` failed to detect the minus symbol correctly.

---

## 1.3.1

### Bug fixes

- **Issue #14** — Fixed `NullPointerException` during export.

---

## 1.3.0

### New features

- **Issue #7** — Nested `@Record` support: a class annotated with `@Record` can now contain fields whose type is itself a `@Record`-annotated class. Useful for grouping logically related domain objects (e.g. card details inside a larger record).
- **Issue #10** — `ParseException` now exposes getter methods so callers can retrieve structured failure details and build localised error messages rather than relying on the English exception message.
- **Issue #13** — Support for skipping unparseable fields within records.
- Added built-in `Short`/`short` formatter; registered in `ByTypeFormatter`.

---

## 1.2.2 (2008-10-17)

### Bug fixes

- **Issue #9** — Fixed a loading failure when the input string was slightly shorter than the offset of the last field in the record.

---

## 1.2.1 (2008-10-15)

### New features

- **Issue #8** — Added support for annotated static nested classes and inner classes.
- **Issue #6** — Added support for primitive types (`int`, `boolean`, `float`, etc.) in addition to their boxed counterparts. *(contributed by Marcos Lois Bermúdez)*

### Bug fixes

- Fixed a runtime failure when a getter or setter declared an interface or abstract class as its type and the format manager could not determine the concrete data type. *(contributed by Marcos Lois Bermúdez)*

---

## 1.2.0 (2008-06-12)

### New features

- **Issue #5** — Getters starting with `is` (in addition to `get`) can now carry `@Field` annotations.
- Improved error reporting on parse failures: error messages now include the full format context (class name, method name, and all relevant annotation settings).

---

## 1.1.1 (2008-05-29)

### New features

- Added the ability to leave numbers unsigned; unsigned is now the default.

### Changes

- **Issue #4** — `FixedFormatter` interface generified. Custom formatters must be updated to specify the type parameter.

### Bug fixes

- Fixed a bug when parsing numbers from strings with prepended signs.
- Fixed various smaller bugs in the built-in formatters.

---

## 1.1.0 (2008-05-26)

### New features

- Introduced the ability to parse and format signed numbers (`Sign.PREPEND`, `Sign.APPEND`).

---

## 1.0.0 (2008-05-25)

Initial release of fixedformat4j.

