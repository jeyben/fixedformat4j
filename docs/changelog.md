---
title: Changelog
---

# Changelog

## [Unreleased] 1.9.0

### New features

- **`FixedFormatReader.openStream()` — lazy stream processing** ([#115](https://github.com/jeyben/fixedformat4j/issues/115)) —
  Adds `openStream()` methods to `FixedFormatReader` that return a lazy `Stream` backed by a
  `Spliterator`, so callers can process arbitrarily large files with bounded memory. Records are
  read and parsed one at a time on demand rather than loading the entire file into a `List` upfront.

  The `open` prefix signals that the caller owns the stream lifecycle and must close it via
  `try`-with-resources — closing the stream automatically closes the underlying reader or file.

  Two overload families are provided:

  - **Untyped** — `openStream(Reader)`, `openStream(InputStream)`, `openStream(InputStream, Charset)`,
    `openStream(Path)`, `openStream(Path, Charset)` — return `Stream<Object>`
  - **Typed** — `openStream(Reader, Class<T>)`, `openStream(InputStream, Class<T>)`,
    `openStream(Path, Class<T>)` — filter to records of the requested type and return `Stream<T>`
    without requiring a cast at the call site

  All configured strategies (`MultiMatchStrategy`, `UnmatchStrategy`, `ParseErrorStrategy`) and the
  `excludeLines` filter apply identically to the streaming path.

  ```java
  // Process a large file record by record — only one line in memory at a time
  try (Stream<DetailRecord> s = reader.openStream(Path.of("data.txt"), DetailRecord.class)) {
      s.filter(r -> r.getAmount() > 0)
       .forEach(this::process);
  }
  ```

### Refactoring

- **`FieldValidator` extracted** — six validation methods moved out of `FixedFormatManagerImpl` into a dedicated package-private class; the manager now has a single responsibility (load/export orchestration).
- **`DecimalFormatCache` extracted** — `DecimalFormat` thread-local caching infrastructure separated from `AbstractDecimalFormatter`, keeping formatting logic and caching concerns in separate classes.
- **Reduced boilerplate in number formatters** — duplicate `asString()` bodies in `IntegerFormatter`, `ShortFormatter`, and `LongFormatter` collapsed via a shared `valueOrNull()` helper on `AbstractNumberFormatter`.
- **Reduced boilerplate in decimal formatters** — duplicate `asObject()` pattern in `DoubleFormatter`, `FloatFormatter`, and `BigDecimalFormatter` collapsed via a shared `resolveDecimalString()` helper on `AbstractDecimalFormatter`.

No behaviour change for existing annotated record classes, custom formatters, or serialized fixed-width data.

---

## 1.8.1 (2026-05-05)

### Performance improvements

`load()` and `export()` are measurably faster, especially for workloads that process many records or hit the same record class repeatedly:

- **Less reflection per call** — formatter instances are resolved once at cache-warm time instead of being created fresh on every field operation.
- **Date and time fields are cheaper** — `DateTimeFormatter` and `SimpleDateFormat` instances are cached and reused, eliminating the most expensive allocation in date parsing and formatting.
- **Tighter memory use** — internal maps are pre-sized and constant string values are pre-computed at startup rather than rebuilt on every operation.

**No API or behaviour change.** Existing annotated record classes, custom formatters, and serialized fixed-width data are unaffected. Upgrade by bumping the version number.

---

## 1.8.0 (2026-05-01)

### Breaking changes

- **Removed deprecated `protected FixedFormatManagerImpl#readDataAccordingFieldAnnotation`** ([#109](https://github.com/jeyben/fixedformat4j/issues/109)) —
  The method was deprecated since #77 (1.7.0) and was never on the live `load()` path, which has used
  `ClassMetadataCache` exclusively since that release. Only consumers that `extend FixedFormatManagerImpl`
  and override or call this method are affected; consumers using the `FixedFormatManager` interface are
  unaffected.

  **Migration:** subclassers should drive parsing through `FixedFormatManager#load(Class, String)` instead.

- **`@Record(align)` now uses `RecordAlign` instead of `Align`** ([#81](https://github.com/jeyben/fixedformat4j/issues/81)) —
  A new two-value enum `RecordAlign { LEFT, RIGHT }` replaces `Align` as the type of `@Record#align()`.
  Because `Align` includes the `INHERIT` sentinel, which has no meaning at the record level, the old
  type admitted a combination that was only detectable at runtime (and was rejected with a
  `FixedFormatException` since 1.7.1). `RecordAlign` makes that mistake impossible at compile time
  and removes the runtime check.

  **Migration:** replace `Align.LEFT` / `Align.RIGHT` with `RecordAlign.LEFT` / `RecordAlign.RIGHT`
  on every `@Record` annotation that specifies the `align` attribute:

  ```java
  // Before (1.7.x)
  @Record(length = 20, align = Align.RIGHT)
  public class MyRecord { … }

  // After (1.8.0+)
  @Record(length = 20, align = RecordAlign.RIGHT)
  public class MyRecord { … }
  ```

  Records that do not specify `align` are **unaffected** — the default (`RecordAlign.LEFT`)
  preserves the existing behaviour. The `Align` enum itself is unchanged and continues to be
  used for `@Field(align = …)`.

### New features

- **`FixedFormatReader` — file and stream processing** ([#82](https://github.com/jeyben/fixedformat4j/issues/82),
  [#95](https://github.com/jeyben/fixedformat4j/issues/95)) —
  Reads fixed-format records from files, streams, or `Reader`s line-by-line, routing each line
  to one or more `@Record`-annotated classes via `LinePattern` discriminators. Three factories cover
  the common cases: `LinePattern.prefix("HDR")`, `LinePattern.positional(int[], String)` for
  multi-position checks (e.g. type code at offset 0..2 plus a sub-type at offset 7..8), and
  `LinePattern.matchAll()` for catch-all routing. Patterns are bucketed into hash tables at build
  time, so per-line routing is near O(1) regardless of how many record types are registered.
  `FixedFormatReader` is unparameterized.

  Two output shapes:
  - `read()` — returns `ReadResult`, a type-safe class-keyed container; `get(Class<R>)` returns `List<R>` with no cast required. Also provides `getAll()`, `contains(Class<?>)`, and `classes()`.
  - `process(source, HandlerRegistry)` — push-style; dispatches each parsed record to the typed `Consumer<R>` registered in a per-call `HandlerRegistry`. Classes absent from the registry are silently ignored. Because the registry is supplied at call time, the same reader is safe to use from multiple threads.

  Every shape accepts `Reader`, `InputStream`, or `Path`; stream overloads default to UTF-8.

  Three configurable strategies: `MultiMatchStrategy` (`firstMatch` / `throwOnAmbiguity` /
  `allMatches`), `UnmatchStrategy` (`skip` / `throwException`), and `ParseErrorStrategy`
  (`throwException` / `skipAndLog`). An `excludeLines(Predicate<String>)` pre-filter runs
  before pattern matching and bypasses `UnmatchStrategy`.

  `RecordMapping<T>` is the public value type carrying the class and pattern for each registered
  mapping; it is surfaced as the parameter and return type of `MultiMatchStrategy.resolve()`.
  Consumers implementing a custom `MultiMatchStrategy` must reference it directly.

  `FixedFormatIOException` (extends `FixedFormatException`) is thrown on underlying `IOException`.

  ```java
  import com.ancientprogramming.fixedformat4j.io.read.LinePattern;

  FixedFormatReader reader = FixedFormatReader.builder()
      .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
      .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
      .build();

  ReadResult result = reader.read(Path.of("data.txt"));
  List<HeaderRecord> headers = result.get(HeaderRecord.class); // no cast
  List<DetailRecord> details = result.get(DetailRecord.class); // no cast
  ```

  See [File processing](usage/file-processing) for a complete guide.

- **`@Field(length = -1)` — rest-of-line field** ([#97](https://github.com/jeyben/fixedformat4j/issues/97)) —
  A `String` field may now declare `length = -1` to capture everything from its `offset` to the
  end of the line, regardless of how long that line is. This is useful for free-text trailers,
  comments, or variable-length payloads appended after a fixed-width prefix.

  Constraints validated at startup: only `String` return type, `count = 1`, no `align`, no
  `paddingChar` override, no `nullChar`. The `@Field(length = -1)` field must be the last field
  (highest `offset`) in the record. Combining `@Record(length = …)` with a rest-of-line field is
  rejected because record-level padding would corrupt the verbatim round-trip.

  ```java
  @Record
  public class LogRecord {
      @Field(offset = 1, length = 3)
      public String getLevel() { … }          // e.g. "INF", "ERR"

      @Field(offset = 4, length = -1)
      public String getMessage() { … }        // captures remainder of each line
  }
  ```

### Bug fixes

- **Classloader leak prevention via `ClassValue`** ([#89](https://github.com/jeyben/fixedformat4j/issues/89)) —
  The three JVM-level caches (`ClassMetadataCache`, `FixedFormatManagerImpl.VALIDATED_CLASSES`, and
  `AbstractPatternFormatter.PATTERN_LENGTH_CACHE`) were backed by static
  `ConcurrentHashMap<Class<?>, …>` instances. A `ConcurrentHashMap` holds **strong references** to
  its keys, so a `Class` used as a key can never be garbage-collected — even after all application
  references to it are gone. In multi-classloader environments (OSGi, servlet containers, Spring
  Boot DevTools, Jakarta EE) this causes the child `ClassLoader` that defined the record class to
  be retained indefinitely, leaking all classes it loaded.

  All three caches are now backed by `ClassValue<T>`. Computed values are stored inside the
  `Class` object itself; when the record class's defining `ClassLoader` becomes unreachable the
  cached metadata is collected with it — no external map, no leak.

  **No API or behaviour change.** Existing annotated record classes, custom formatters, and
  serialized fixed-width data are unaffected.

---

## 1.7.2 (2026-04-20)

### Behaviour changes

- **`@Field(nullChar = …)` now activates when `nullChar == paddingChar`** ([#84](https://github.com/jeyben/fixedformat4j/issues/84)) —
  The activation gate for null-aware handling is relaxed so that setting `nullChar` equal to
  `paddingChar` is a supported, idiomatic configuration — the "blank-is-null" convention.
  Previously this combination was documented as a no-op; it now enables the same load and
  export semantics as the distinct-sentinel configuration.

  Typical uses:

  ```java
  // All spaces means null (e.g. optional date)
  @Field(offset = 1, length = 8, paddingChar = ' ', nullChar = ' ')
  public Date getInvoiceDate() { … }

  // All zeros means null (e.g. optional numeric with zero-padding)
  @Field(offset = 9, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = '0')
  public Integer getQuantity() { … }
  ```

  **Migration:** the prior `nullChar != paddingChar` activation rule is removed from the
  javadoc. Records that intentionally set `nullChar == paddingChar` to get no-op behaviour
  (none known) should omit `nullChar` instead.

## 1.7.1 (2026-04-18)

### Breaking changes

- **`@Field.align()` default changed from `Align.LEFT` to `Align.INHERIT`** —
  The raw annotation value returned by `fieldAnnotation.align()` is now `Align.INHERIT` for
  any field that does not set `align` explicitly. The *effective runtime behaviour* is
  unchanged — the framework resolves `INHERIT` to `LEFT` via the enclosing `@Record`'s
  `align` default — but code that reads the annotation directly (annotation processors,
  reflection tools, custom bootstrap code) and passes the result to `Align.apply()` or
  `Align.remove()` will now receive an `UnsupportedOperationException`.

  **Migration:** read `FormatInstructions.getAlignment()` instead of the raw annotation
  (it is already resolved), or guard against `Align.INHERIT` before calling `apply`/`remove`.

- **`Align.INHERIT` new enum constant** — The `Align` enum gains a third value. `switch`
  statements over `Align` without an explicit `default` arm now have an unhandled case.
  Add a `default:` branch (or an explicit `case INHERIT:`) that throws or delegates
  appropriately.

### New features

- **Opt-in `nullChar` attribute on `@Field` to represent null values** ([#29](https://github.com/jeyben/fixedformat4j/issues/29)) —
  Adds a `nullChar` attribute to the `@Field` annotation that lets callers distinguish a
  genuinely-absent field from a zero or empty value.

  **Activation rule:** null-aware handling is enabled only when `nullChar` differs from
  `paddingChar`. The default value (`'\0'`) is a sentinel that can never appear in a regular
  fixed-width payload, so all existing records retain their pre-1.7.1 behaviour unchanged.

  - **On load** — if every character in the field slice equals `nullChar`, the setter is not
    invoked and the field stays `null`. Configuring `nullChar` on a primitive-typed field
    throws `FixedFormatException` at validation time.
  - **On export** — if the getter returns `null`, the field is emitted as `length` copies of
    `nullChar`, bypassing the formatter entirely.

  For repeating fields (`count > 1`) the check is applied **per element**: each slot is evaluated independently. Primitive array element types (e.g. `int[]`) cannot hold `null` and are unaffected.

  ```java
  // Null and zero are now distinguishable:
  // "     " (spaces) → null   "00042" → 42
  @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = ' ')
  public Integer getAmount() { … }
  ```

- **Record-level default alignment via `@Record(align = …)`** ([#30](https://github.com/jeyben/fixedformat4j/issues/30)) —
  Adds an `align` attribute to the `@Record` annotation that sets a default alignment for all
  fields in the record. Individual fields may still override it with an explicit `@Field(align = …)`.
  The effective runtime behaviour for existing records is unchanged: `@Record.align()` defaults to
  `Align.LEFT`, and fields that inherit that default continue to behave as they did before.
  See the breaking-change note above regarding the raw `@Field.align()` annotation value.

  ```java
  // Before — alignment repeated on every field
  @Record(length = 20)
  public class MyRecord {
    @Field(offset = 1, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getField1() { … }

    @Field(offset = 11, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getField2() { … }
  }

  // After — alignment declared once at record level
  @Record(length = 20, align = Align.RIGHT)
  public class MyRecord {
    @Field(offset = 1, length = 10, paddingChar = '0')
    public Integer getField1() { … }

    @Field(offset = 11, length = 10, paddingChar = '0')
    public Integer getField2() { … }
  }
  ```

### Bug fixes

- **Null nested `@Record` field now exports as padding instead of throwing** ([#45](https://github.com/jeyben/fixedformat4j/issues/45)) —
  Exporting a parent record whose nested `@Record` field is `null` previously threw a
  `FixedFormatException`. It now outputs the field's `paddingChar` repeated for the declared
  `@Field` length, consistent with how all other formatters handle `null` values.

---

## 1.7.0 (2026-04-18)

### Breaking changes

- **`AbstractFixedFormatter.getRemovePadding` removed** — deprecated in 1.6.1 and now deleted.
  Rename any override to `stripPadding`; the signature is identical. The call chain is now
  `parse()` → `stripPadding()` directly.

  ```java
  // Before (1.6.x)
  @Override
  protected String getRemovePadding(String value, FormatInstructions instructions) { … }

  // After (1.7.0+)
  @Override
  protected String stripPadding(String value, FormatInstructions instructions) { … }
  ```

---

### New features

- **Enum support via `@FixedFormatEnum`** ([#67](https://github.com/jeyben/fixedformat4j/issues/67)) —
  Annotate any getter that returns an `enum` type with `@FixedFormatEnum` to control how the value
  is serialised in the fixed-width record. Two modes are available through the `EnumFormat` enum:

  - `LITERAL` (default) — stores and reads the enum constant name (`Enum.name()` / `valueOf()`).
  - `NUMERIC` — stores and reads the ordinal as a zero-padded integer (`Enum.ordinal()` / index lookup).

  ```java
  public enum Status { ACTIVE, INACTIVE }

  // LITERAL (default): stores "ACTIVE" / "INACTIVE"
  @Field(offset = 1, length = 8)
  @FixedFormatEnum
  public Status getStatus() { … }

  // NUMERIC: stores "0" / "1"
  @Field(offset = 1, length = 1)
  @FixedFormatEnum(EnumFormat.NUMERIC)
  public Status getStatus() { … }
  ```

---

### Performance improvements

- **Field metadata caching** ([#77](https://github.com/jeyben/fixedformat4j/issues/77)) —
  `ClassMetadataCache` precomputes and caches all field descriptors per annotated class on first
  use, eliminating repeated annotation scanning on every `load()` / `export()` call. The cache is
  process-wide and thread-safe.

- **MethodHandle dispatch** ([#75](https://github.com/jeyben/fixedformat4j/issues/75)) —
  Getter and setter invocation now uses `MethodHandle` instead of `Method.invoke()`, reducing
  per-call overhead after JIT warmup.

- **Reduced string allocations** ([#76](https://github.com/jeyben/fixedformat4j/issues/76)) —
  Padding and sign handling rewritten to minimise intermediate `String` object creation per field.

---

## 1.6.1 (2026-04-10)

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

