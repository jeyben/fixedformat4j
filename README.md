# FixedFormat4J

[![Maven Central](https://img.shields.io/maven-central/v/com.ancientprogramming.fixedformat4j/fixedformat4j)](https://central.sonatype.com/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j)
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
[![Nightly Build](https://github.com/jeyben/fixedformat4j/actions/workflows/nightly-build.yml/badge.svg?branch=master)](https://github.com/jeyben/fixedformat4j/actions/workflows/nightly-build.yml)
[![Line Coverage](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.lineCoverage&label=Line%20Coverage&color=blue)](https://jeyben.github.io/fixedformat4j/pit-reports/)
[![Mutation Coverage](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.mutationCoverage&label=Mutation%20Coverage&color=brightgreen)](https://jeyben.github.io/fixedformat4j/pit-reports/)
[![Test Strength](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fjeyben.github.io%2Ffixedformat4j%2Fmutation-score.json&query=%24.testStrength&label=Test%20Strength&color=brightgreen)](https://jeyben.github.io/fixedformat4j/pit-reports/)

**[Documentation](https://jeyben.github.io/fixedformat4j/)** — [Quick Start](https://jeyben.github.io/fixedformat4j/quickstart) · [Annotations](https://jeyben.github.io/fixedformat4j/usage/annotations) · [Examples](https://jeyben.github.io/fixedformat4j/examples) · [Changelog](https://jeyben.github.io/fixedformat4j/changelog) · [Benchmarks](https://jeyben.github.io/fixedformat4j/benchmarks)

A lightweight, non-intrusive Java library for reading and writing fixed-width flat-file records using annotations. **Originally published in 2008 and actively maintained ever since.** **Dramatically faster since 1.7.0** — field metadata caching and `MethodHandle` dispatch deliver 1.6× to 13.8× throughput gains over earlier releases, depending on record size and field types, verified by JMH microbenchmarks.

## Why FixedFormat4J?

Fixed-width flat files are the lingua franca of banking, payroll, EDI, and government data exchange. Every character has a meaning — no delimiters, no headers, just positional fields that have to land exactly right. Consider a typical payroll settlement line:

```
EMP004232SMITH               0000185000CR20260418003
```

That single line encodes: employee ID (`00423`), department code (`2`), name (`SMITH               `), net pay in implied cents (`0000185000` = $1,850.00), direction (`CR`), pay date (`20260418`), region code (`003`).

**Without FixedFormat4J** you write the parsing by hand for every field — and a symmetric block of `String.format` calls for export:

```java
String name       = line.substring(9, 29).trim();
BigDecimal netPay = new BigDecimal(line.substring(29, 39))
                        .movePointLeft(2);
LocalDate payDate = LocalDate.parse(line.substring(40, 48),
                        DateTimeFormatter.ofPattern("yyyyMMdd"));
// Repeat for every field. Off-by-one errors guaranteed.
```

**With FixedFormat4J** you declare the layout once — as annotations on a plain Java class — and never write parsing or formatting code again:

```java
@Getter @Setter @NoArgsConstructor
@Record
public class PayrollRecord {

  @Field(offset = 4, length = 5, align = Align.RIGHT, paddingChar = '0')
  private Integer employeeId;

  @Field(offset = 10, length = 20)
  private String name;

  @Field(offset = 30, length = 10, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
  private BigDecimal netPay;

  @Field(offset = 41, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  private LocalDate payDate;
}
```

**Works with Lombok** — place `@Field` directly on fields and let Lombok generate the getters and setters. `@Getter @Setter @NoArgsConstructor` and you're done. No boilerplate, no hand-written accessors.

**Spring-ready** — `FixedFormatManagerImpl` is a plain Java object with no Spring dependency. Register it once as a `@Bean` and inject it wherever you need it:

```java
@Bean
public FixedFormatManager fixedFormatManager() {
    return new FixedFormatManagerImpl();
}
```

For every annotation attribute, type, and advanced option see the [Annotations reference](https://jeyben.github.io/fixedformat4j/usage/annotations).

## Compared to alternatives

|                        | FixedFormat4J              | BeanIO            | FlatWorm          | Manual `substring` |
|------------------------|----------------------------|-------------------|-------------------|--------------------|
| Configuration          | Java annotations           | XML schema file   | XML schema file   | none               |
| External schema file   | no                         | yes               | yes               | no                 |
| Spring integration     | drop-in `@Bean`            | separate module   | manual            | manual             |
| Enum support           | built-in (`@FixedFormatEnum`) | built-in       | limited           | manual             |
| Custom formatters      | yes (`FixedFormatter<T>`)  | yes               | limited           | n/a                |
| Lombok support         | yes (field annotations)    | no                | no                | no                 |
| Active maintenance     | yes                        | limited           | no                | n/a                |

FixedFormat4J's annotation-only approach means no external schema files to version, no XML to keep in sync with your Java classes, and no framework-specific integration layer to pull in.

## Latest release — 1.7.2 (2026-04-20)

See the [Changelog](https://jeyben.github.io/fixedformat4j/changelog) for what's new.

## Quick start

### 1. Add the dependency

FixedFormat4J is published to **Maven Central**. No repository configuration or authentication is needed.

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

### 5. Read a file line by line

`FixedFormatReader` (since 1.8.0) reads files and streams directly, routing each line to the right record class:

```java
// Homogeneous file — every line is the same record type
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, LinePattern.matchAll())
    .build();

List<EmployeeRecord> records = reader.read(Path.of("employees.txt"))
    .get(EmployeeRecord.class);
```

```java
// Heterogeneous file — route lines by prefix
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
    .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
    .build();

ReadResult result = reader.read(Path.of("payroll.txt"));
List<HeaderRecord> headers = result.get(HeaderRecord.class);
List<DetailRecord> details = result.get(DetailRecord.class);
```

See [File Processing](https://jeyben.github.io/fixedformat4j/usage/file-processing) for the full API including streaming `process()`, charset overloads, and per-line error strategies.

### Writing the same field at multiple offsets

`@Fields` groups multiple `@Field` annotations on a single getter, writing the same value at two or more positions — useful for legacy formats that duplicate a field:

```java
@Fields({
    @Field(offset = 11, length = 8),
    @Field(offset = 19, length = 8)
})
public Date getDateData() { return dateData; }
```

## Supported types

`ByTypeFormatter` maps Java types to the right formatter automatically. All of the following work out of the box — no `formatter =` attribute required:

| Java type | Supplementary annotation | Notes |
|---|---|---|
| `String` | — | padded or trimmed to declared length |
| `Integer` / `int` | `@FixedFormatNumber` | sign handling, zero-padding |
| `Long` / `long` | `@FixedFormatNumber` | |
| `Short` / `short` | `@FixedFormatNumber` | |
| `Double` / `double` | `@FixedFormatDecimal` | |
| `Float` / `float` | `@FixedFormatDecimal` | |
| `BigDecimal` | `@FixedFormatDecimal` | implied decimals, optional delimiter |
| `Boolean` / `boolean` | `@FixedFormatBoolean` | configurable `trueValue` / `falseValue` literals |
| `Character` / `char` | — | single character |
| `LocalDate` | `@FixedFormatPattern` | e.g. `"yyyyMMdd"` |
| `LocalDateTime` | `@FixedFormatPattern` | e.g. `"yyyyMMddHHmmss"` |
| `java.util.Date` | `@FixedFormatPattern` | legacy date support |
| Any `Enum` | `@FixedFormatEnum` | `LITERAL` (name) or `NUMERIC` (ordinal) |
| Nested `@Record` class | — | recursive load / export |

For any type not in this table, implement `FixedFormatter<T>` and reference it via `formatter =` on `@Field`.

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

## Contributing

1. Fork the repository and create a feature branch.
2. **Write a failing test first** — TDD is required; see [CLAUDE.md](CLAUDE.md) for the full workflow.
3. Run `export JAVA_HOME=$(/usr/libexec/java_home -v 11) && mvn test` to verify everything passes.
4. Open a pull request against `master`.

Bug reports and feature requests are welcome as [GitHub issues](https://github.com/jeyben/fixedformat4j/issues).

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
