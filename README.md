# fixedformat4j

[![Nightly Build](https://github.com/jeyben/fixedformat4j/actions/workflows/nightly-build.yml/badge.svg?branch=master)](https://github.com/jeyben/fixedformat4j/actions/workflows/nightly-build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.ancientprogramming.fixedformat4j/fixedformat4j)](https://central.sonatype.com/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j)
A small, non-intrusive Java library for reading and writing fixed-width flat-file records using annotations.

## Quality

[![Line Coverage](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.lineCoverage&label=Line%20Coverage&color=blue)](https://jeyben.github.io/fixedformat4j/pit-reports/)
[![Mutation Coverage](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.mutationCoverage&label=Mutation%20Coverage&color=brightgreen)](https://jeyben.github.io/fixedformat4j/pit-reports/)
[![Test Strength](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.testStrength&label=Test%20Strength&color=brightgreen)](https://jeyben.github.io/fixedformat4j/pit-reports/)

## Why fixedformat4j?

Fixed-width files are common in banking, payroll, government, and legacy system integration. fixedformat4j lets you describe your record layout once — as annotations on a plain Java class — and then load and export records without writing any parsing or formatting code yourself.

- **Annotation-driven** — declare field offsets, lengths, alignment, and padding in one place
- **Rich type support** — `String`, `Integer`, `Long`, `Short`, `Double`, `Float`, `BigDecimal`, `Boolean`, `Character`, `Date`, `LocalDate`, `LocalDateTime`
- **Signed numbers** — handles `'-1000'` and `'1000-'` both as negative values
- **Implicit decimals** — store `BigDecimal` without a decimal point in the file
- **Nested records** — embed one `@Record` class inside another
- **Extensible** — plug in your own formatter for any custom type

## What's new in 1.6.0

- **`LocalDateTime` support** — `LocalDateTime` is now a built-in type in `ByTypeFormatter`, with a type-specific default pattern (`yyyyMMddHHmmss`). Eager pattern validation raises a clear error at load/export time if the `@FixedFormatPattern` value is invalid.
- **Repeating fields via `@Field(count)`** — a single `@Field` annotation can now map a list of repeated same-format fields by setting `count`; pair with `strictCount` to enforce exact list size on export.
- **PIT mutation testing** — the project now runs PIT (PITest) mutation testing on every release; live results are published to the [Mutation Report](https://jeyben.github.io/fixedformat4j/pit-reports/) page and surfaced as quality badges on this README.
- **Internal refactoring** — `FixedFormatManagerImpl` was decomposed into focused collaborators; all production classes now carry complete Javadoc.

## Journey since 1.4.0

After many years of inactivity, fixedformat4j was revived with the 1.4.0 release. Here is a summary of everything that has improved since then:

- **`LocalDate` and `LocalDateTime` support** — both are now built-in types in `ByTypeFormatter` with type-specific default patterns and eager pattern validation.
- **Repeating fields via `@Field(count)`** — map a list of same-format fields with a single annotation; optional `strictCount` enforces list size on export.
- **Field-level `@Field` / `@Fields` annotations** — place annotations directly on a Java field instead of its getter; works with plain POJOs and Lombok (`@Getter`/`@Setter`).
- **Maven Central distribution** — no GitHub account or personal access token required; standard `<dependency>` block just works.
- **Negative decimal fix** — parsing trailing-sign negatives with implicit decimals (e.g. `000000001-`) no longer throws `NumberFormatException`.
- **Modernised build** — Java 11, SLF4J, commons-lang3, JUnit 5 with comprehensive test coverage.
- **PIT mutation testing** — live quality badges and a published mutation report on every release.
- **New documentation site** — full Markdown docs at [jeyben.github.io/fixedformat4j](https://jeyben.github.io/fixedformat4j/) (Quick Start, Annotations reference, Examples, FAQ, Changelog).

## Quick start

### 1. Add the dependency

fixedformat4j is published to **Maven Central**. No repository configuration or authentication is needed — just add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.6.0</version>
</dependency>
```

Requires **Java 11 or later**. If you want log output, add an [SLF4J binding](https://www.slf4j.org/manual.html#swapping) such as `logback-classic`; without one the library still works, just silently.

See [Get It](https://jeyben.github.io/fixedformat4j/get-it) for full setup instructions.

<details>
<summary>No GitHub account? Download manually</summary>

Download `fixedformat4j-1.6.0.jar` from the [1.6.0 release page](https://github.com/jeyben/fixedformat4j/releases/tag/1_6_0), then install it into your local Maven repository:

```bash
mvn install:install-file \
  -Dfile=fixedformat4j-1.6.0.jar \
  -DgroupId=com.ancientprogramming.fixedformat4j \
  -DartifactId=fixedformat4j \
  -Dversion=1.6.0 \
  -Dpackaging=jar
```

After that the standard `<dependency>` block works as-is. To deploy to a private Nexus or Artifactory instance instead, see [Get It](https://jeyben.github.io/fixedformat4j/get-it).

</details>

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
- `@Field` goes on the **getter**; `offset` is **1-based**.
- Each mapped getter must have a corresponding setter.

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
- [Nested Records](https://jeyben.github.io/fixedformat4j/usage/nested-records) — embedding one record inside another
- [FAQ](https://jeyben.github.io/fixedformat4j/faq)
- [Changelog](https://jeyben.github.io/fixedformat4j/changelog)

**Reports:**
- [Mutation Report](https://jeyben.github.io/fixedformat4j/pit-reports/) — latest PIT mutation testing results (updated on release)

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
