# fixedformat4j

[![Nightly Build](https://github.com/jeyben/fixedformat4j/actions/workflows/nightly-build.yml/badge.svg?branch=master)](https://github.com/jeyben/fixedformat4j/actions/workflows/nightly-build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.ancientprogramming.fixedformat4j/fixedformat4j)](https://central.sonatype.com/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j)

**[Documentation](https://jeyben.github.io/fixedformat4j/)** — [Quick Start](https://jeyben.github.io/fixedformat4j/quickstart) · [Annotations](https://jeyben.github.io/fixedformat4j/usage/annotations) · [Examples](https://jeyben.github.io/fixedformat4j/examples) · [Changelog](https://jeyben.github.io/fixedformat4j/changelog) · [Benchmarks](https://jeyben.github.io/fixedformat4j/benchmarks)

A lightweight, non-intrusive Java library for reading and writing fixed-width flat-file records using annotations. **Dramatically faster since 1.7.0** — field metadata caching and `MethodHandle` dispatch deliver 1.6× to 13.8× throughput gains over earlier releases, depending on record size and field types, verified by JMH microbenchmarks.

## Quality

[![Line Coverage](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.lineCoverage&label=Line%20Coverage&color=blue)](https://jeyben.github.io/fixedformat4j/pit-reports/)
[![Mutation Coverage](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.mutationCoverage&label=Mutation%20Coverage&color=brightgreen)](https://jeyben.github.io/fixedformat4j/pit-reports/)
[![Test Strength](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.testStrength&label=Test%20Strength&color=brightgreen)](https://jeyben.github.io/fixedformat4j/pit-reports/)

## Why fixedformat4j?

Fixed-width files are common in banking, payroll, government, and legacy system integration. Consider a typical payroll line from a daily settlement file:

```
EMP004232SMITH               0000185000CR20260418003
```

That single line encodes: employee ID (`00423`), department code (`2`), name (`SMITH               `), net pay in implied cents (`0000185000` = $1,850.00), direction (`CR`), pay date (`20260418`), region code (`003`). fixedformat4j lets you describe that layout once — as annotations on a plain Java class — and then load and export records without writing any parsing or formatting code yourself.

- **Annotation-driven** — declare field offsets, lengths, alignment, and padding in one place
- **Rich type support** — `String`, `Integer`, `Long`, `Short`, `Double`, `Float`, `BigDecimal`, `Boolean`, `Character`, `Date`, `LocalDate`, `LocalDateTime`
- **Signed numbers** — handles `'-1000'` and `'1000-'` both as negative values
- **Implicit decimals** — store `BigDecimal` without a decimal point in the file
- **Nested records** — embed one `@Record` class inside another
- **Extensible** — plug in your own formatter for any custom type
- **Lombok-friendly** — place `@Field` on fields instead of getters; `@Getter @Setter @NoArgsConstructor` and you're done
- **Spring-friendly** — plain Java objects with no Spring dependency; register as `@Bean`s and inject by interface

## What's new in 1.7.2

- **`nullChar` activates when equal to `paddingChar`** — setting `nullChar = ' '` on a space-padded field (or `nullChar = '0'` on a zero-padded field) now enables the "blank-is-null" convention. A fully-padded slice loads as `null`; a `null` value exports as a fully-padded field. Previously this combination was a no-op.

## What's new in 1.7.1

- **`nullChar` on `@Field`** — distinguish a genuinely-absent field from zero or empty. A slice filled with `nullChar` loads as `null`; a `null` value exports as `length × nullChar`. Works per-element for repeating fields. Configuring `nullChar` on a primitive field is rejected at startup.
- **Record-level default alignment** — set `@Record(align = Align.RIGHT)` once instead of repeating `align=` on every field.

## What's new in 1.7.0

- **Enum support via `@FixedFormatEnum`** — map any `enum` with `LITERAL` (name) or `NUMERIC` (ordinal) serialisation.
- **Field metadata caching and `MethodHandle` dispatch** — annotation scanning done once per class; getter/setter calls via `MethodHandle` for lower overhead.
- **`AbstractFixedFormatter.getRemovePadding` removed** — rename any override to `stripPadding` (breaking change).

See the [Changelog](https://jeyben.github.io/fixedformat4j/changelog) for full details.

## Journey since 1.4.0

After many years of inactivity, fixedformat4j was revived with the 1.4.0 release. Here is a summary of everything that has improved since then:

- **`LocalDate` and `LocalDateTime` support** — both are now built-in types in `ByTypeFormatter` with type-specific default patterns and eager pattern validation.
- **Repeating fields via `@Field(count)`** — map a list of same-format fields with a single annotation; optional `strictCount` enforces list size on export.
- **Field-level `@Field` / `@Fields` annotations** — place annotations directly on a Java field instead of its getter; works with plain POJOs and Lombok (`@Getter`/`@Setter`).
- **Date padding bug fixed** — date formatters no longer over-strip padding characters that appear inside the formatted value ([#33](https://github.com/jeyben/fixedformat4j/issues/33)).
- **Maven Central distribution** — no GitHub account or personal access token required; standard `<dependency>` block just works.
- **Negative decimal fix** — parsing trailing-sign negatives with implicit decimals (e.g. `000000001-`) no longer throws `NumberFormatException`.
- **Modernised build** — Java 11, SLF4J, commons-lang3, JUnit 5 with comprehensive test coverage.
- **PIT mutation testing** — live quality badges and a published mutation report on every release.
- **New documentation site** — full Markdown docs at [jeyben.github.io/fixedformat4j](https://jeyben.github.io/fixedformat4j/) (Quick Start, Annotations reference, Examples, FAQ, Changelog).

## Quick start

### 1. Add the dependency

fixedformat4j is published to **Maven Central**. No repository configuration or authentication is needed.

**Maven**

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.7.2</version>
</dependency>
```

**Gradle (Groovy DSL)**

```groovy
implementation 'com.ancientprogramming.fixedformat4j:fixedformat4j:1.7.2'
```

**Gradle (Kotlin DSL)**

```kotlin
implementation("com.ancientprogramming.fixedformat4j:fixedformat4j:1.7.2")
```

**Ivy**

```xml
<dependency org="com.ancientprogramming.fixedformat4j"
            name="fixedformat4j"
            rev="1.7.2"/>
```

Requires **Java 11 or later**. If you want log output, add an [SLF4J binding](https://www.slf4j.org/manual.html#swapping) such as `logback-classic`; without one the library still works, just silently.

See [Get It](https://jeyben.github.io/fixedformat4j/get-it) for full setup instructions.

### 2. Annotate your record class

```java
import com.ancientprogramming.fixedformat4j.annotation.*;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import java.time.LocalDate;

@Record
public class EmployeeRecord {

  private String name;
  private Integer employeeId;
  private LocalDate hireDate;

  @Field(offset = 1, length = 20)
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  @Field(offset = 21, length = 6, align = Align.RIGHT, paddingChar = '0')
  public Integer getEmployeeId() { return employeeId; }
  public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

  @Field(offset = 27, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  public LocalDate getHireDate() { return hireDate; }
  public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
}
```

- `@Record` marks the class as a fixed-format record.
- `@Field` goes on the getter **or directly on the field** (since 1.5.0); `offset` is **1-based**.
- Each mapped field needs a getter and a setter.

#### Using Lombok instead

```java
@Getter @Setter @NoArgsConstructor
@Record
public class EmployeeRecord {

  @Field(offset = 1, length = 20)
  private String name;

  @Field(offset = 21, length = 6, align = Align.RIGHT, paddingChar = '0')
  private Integer employeeId;

  @Field(offset = 27, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  private LocalDate hireDate;
}
```

Place `@Field` on the fields, let Lombok generate the getters and setters, and the result is identical.

### 3. Load from a string

```java
FixedFormatManager manager = new FixedFormatManagerImpl();

String line = "Jane Smith          00042320260405";
EmployeeRecord record = manager.load(EmployeeRecord.class, line);

System.out.println(record.getName());       // "Jane Smith"
System.out.println(record.getEmployeeId()); // 423
System.out.println(record.getHireDate());   // 2026-04-05
```

### 4. Export to a string

```java
record.setEmployeeId(999);
String exported = manager.export(record);
System.out.println(exported);
// "Jane Smith          00099920260405"
```

Every field is re-padded to its declared length using the configured alignment and padding character.

## Documentation

Full documentation is available at **https://jeyben.github.io/fixedformat4j/**:

- [Quick Start](https://jeyben.github.io/fixedformat4j/quickstart) — step-by-step walkthrough
- [Annotations reference](https://jeyben.github.io/fixedformat4j/usage/annotations) — every annotation attribute explained
- [Examples](https://jeyben.github.io/fixedformat4j/examples) — financial records, booleans, file processing, custom formatters
- [File Processing](https://jeyben.github.io/fixedformat4j/usage/file-processing) — reading files and streams with `FixedFormatReader`
- [Nested Records](https://jeyben.github.io/fixedformat4j/usage/nested-records) — embedding one record inside another
- [FAQ](https://jeyben.github.io/fixedformat4j/faq)
- [Changelog](https://jeyben.github.io/fixedformat4j/changelog)

**Reports:**
- [Mutation Report](https://jeyben.github.io/fixedformat4j/pit-reports/) — latest PIT mutation testing results (updated on release)

## Benchmarks

JMH microbenchmarks compare `load()` and `export()` performance across releases. Charts are published at [jeyben.github.io/fixedformat4j/benchmarks](https://jeyben.github.io/fixedformat4j/benchmarks).

To run benchmarks locally (requires Java 11 and `1.6.1` on Maven Central):

```bash
./benchmarks/run.sh                     # 1.6.1 vs master (default)
./benchmarks/run.sh 1.6.1 1.6.0 master # explicit version list
```

Results are written to `docs/assets/benchmarks/` as JMH JSON.

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
