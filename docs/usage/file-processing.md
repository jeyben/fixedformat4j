---
title: File Processing
---

# File Processing

## Overview

`FixedFormatManager` maps a single line to a Java object. When you need to process an entire file, `FixedFormatReader` does the heavy lifting: it reads line by line, routes each line to the correct `@Record` class via a configurable pattern, and produces typed record objects.

Use `FixedFormatReader` when you need to:
- Read a file with a single record type without writing your own loop.
- Read a **mixed-type file** where different lines belong to different `@Record` classes.
- Stream a large file without loading it all into memory.
- Apply consistent error handling across every line.

For one-off parsing of a string you already have in memory, continue to use `FixedFormatManager.load(...)` directly.

---

## Defining record classes

Record classes work exactly as described in the [annotations reference](annotations). Annotate each class with `@Record` and place `@Field` on each getter (or on the field when using Lombok):

```java
@Record(length = 34)
public class EmployeeRecord {

  private String  name;
  private Integer employeeId;

  @Field(offset = 1, length = 20)
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  @Field(offset = 21, length = 6, align = Align.RIGHT, paddingChar = '0')
  public Integer getEmployeeId() { return employeeId; }
  public void setEmployeeId(Integer id) { this.employeeId = id; }
}
```

---

## Defining patterns

A `FixedFormatMatchPattern` decides whether a line belongs to a particular record class. The built-in `RegexFixedFormatMatchPattern` compiles a regular expression at construction time and applies it with `find()` semantics (partial-line match).

```java
// Matches any line that starts with "EMP"
FixedFormatMatchPattern employeePattern = new RegexFixedFormatMatchPattern("^EMP");

// Matches every line (use for single-type files)
FixedFormatMatchPattern anyLine = new RegexFixedFormatMatchPattern(".*");
```

Implement `FixedFormatMatchPattern` directly for custom discrimination logic — for example, testing a fixed-width type code in a specific column:

```java
FixedFormatMatchPattern headerPattern = line ->
    line != null && line.length() >= 3 && "HDR".equals(line.substring(0, 3));
```

---

## Building a reader

Use the fluent builder to configure the reader. At least one mapping must be added before calling `build()`.

**Single-type file:**

```java
FixedFormatReader<EmployeeRecord> reader = FixedFormatReader.<EmployeeRecord>builder()
    .addMapping(EmployeeRecord.class, new RegexFixedFormatMatchPattern(".*"))
    .build();
```

**Heterogeneous file** — use `T=Object` when the record classes share no common supertype:

```java
FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
    .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
    .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
    .unmatchedLineStrategy(UnmatchedLineStrategy.SKIP)
    .build();
```

Mappings are evaluated in registration order. The `multiMatchStrategy` controls what happens if more than one pattern matches (see [Strategies](#strategies) below).

---

## Reading into a List

`readAsList` eagerly reads all records and returns them in encounter order. Overloads are available for `File`, `Path`, `InputStream`, and `Reader`. All default to UTF-8; pass an explicit `Charset` when needed.

```java
// From a File (UTF-8)
List<EmployeeRecord> records = reader.readAsList(new File("employees.txt"));

// From a Path with an explicit charset
List<EmployeeRecord> records = reader.readAsList(
    Path.of("employees.txt"), StandardCharsets.ISO_8859_1);

// From any Reader
List<EmployeeRecord> records = reader.readAsList(new StringReader(data));
```

---

## Streaming large files

`readAsStream` returns a **lazy** `Stream<T>` backed by the underlying reader. Lines are read on demand — only the current line is held in memory. The stream closes the reader automatically when the stream itself is closed, so always use try-with-resources:

```java
try (Stream<EmployeeRecord> stream = reader.readAsStream(Path.of("employees.txt"))) {
    stream
        .filter(e -> e.getEmployeeId() > 1000)
        .forEach(this::process);
}
```

The stream is sequential and ordered. The same `File`, `Path`, `InputStream`, and `Reader` overloads are available as for `readAsList`.

---

## Reading into a Map

For heterogeneous files, `readAsMap` groups records by their matched class:

```java
FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
    .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
    .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
    .unmatchedLineStrategy(UnmatchedLineStrategy.SKIP)
    .build();

Map<Class<?>, List<Object>> byType = reader.readAsMap(Path.of("data.txt"));

List<Object> headers = byType.get(HeaderRecord.class); // may be absent if no HDR lines
List<Object> details = byType.get(DetailRecord.class);
```

The map is a `LinkedHashMap` whose key order follows the `addMapping` registration order. Classes with no matching lines are excluded from the map entirely.

---

## Per-record callbacks

`readWithCallback` drives the read loop and invokes a callback for each parsed record. Two signatures are available:

**`Consumer<T>`** — receive only the record:

```java
reader.readWithCallback(new File("employees.txt"),
    record -> System.out.println(record.getName()));
```

**`BiConsumer<Class<? extends T>, T>`** — receive the matched class and the record (useful when `T=Object`):

```java
reader.readWithCallback(new File("data.txt"), (clazz, record) -> {
    if (clazz == HeaderRecord.class) {
        processHeader((HeaderRecord) record);
    } else if (clazz == DetailRecord.class) {
        processDetail((DetailRecord) record);
    }
});
```

---

## Strategies

All three strategy types are interfaces. The built-in behaviours are available as static factory methods. Pass a lambda for custom logic.

### Multi-match — when more than one pattern matches

| Factory method | Behaviour |
|---|---|
| `MultiMatchStrategy.firstMatch()` *(default)* | Use the first matching mapping in registration order; ignore the rest. |
| `MultiMatchStrategy.throwOnAmbiguity()` | Throw `FixedFormatException` listing the line number and all matching class names. |
| `MultiMatchStrategy.allMatches()` | Emit one record per matching mapping, in registration order. |

```java
FixedFormatReader.<Object>builder()
    .addMapping(TypeARecord.class, patternA)
    .addMapping(TypeBRecord.class, patternB)
    .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
    .build();
```

Implement `MultiMatchStrategy` directly for custom resolution logic:

```java
.multiMatchStrategy((matched, lineNumber) ->
    matched.stream()
        .filter(m -> m.getRecordClass() == PreferredRecord.class)
        .collect(Collectors.toList()))
```

### Unmatched lines — when no pattern matches

| Factory method | Behaviour |
|---|---|
| `UnmatchedLineStrategy.skip()` *(default)* | Silently ignore the line. Useful for header, footer, or comment lines. |
| `UnmatchedLineStrategy.throwException()` | Throw `FixedFormatException` with the line number and raw content. |
| Lambda | Invoke any custom logic; throw to abort, return to continue. |

```java
FixedFormatReader.<EmployeeRecord>builder()
    .addMapping(EmployeeRecord.class, new RegexFixedFormatMatchPattern("^EMP"))
    .unmatchedLineStrategy((lineNumber, line) ->
        System.err.println("Unmatched line " + lineNumber + ": " + line))
    .build();
```

### Parse errors — when a matched line fails to parse

| Factory method | Behaviour |
|---|---|
| `ParseErrorStrategy.throwException()` *(default)* | Rethrow immediately. The wrapped exception includes the line number. |
| `ParseErrorStrategy.skipAndLog()` | Skip the line and log details at WARN level via SLF4J. |
| Lambda | Invoke any custom logic; throw to abort, return to skip the record. |

```java
FixedFormatReader.<EmployeeRecord>builder()
    .addMapping(EmployeeRecord.class, new RegexFixedFormatMatchPattern(".*"))
    .parseErrorStrategy((wrapped, line, lineNumber) ->
        System.err.println("Parse error on line " + lineNumber + ": " + wrapped.getMessage()))
    .build();
```

**Note:** `ParseErrorStrategy.skipAndLog()` only logs if an SLF4J binding is present at runtime. For guaranteed error visibility, use a custom lambda that writes to your preferred output.

---

## Pre-match filtering

Use `includeLines` to select which lines reach pattern matching. Lines for which the predicate returns `false` are silently skipped and do **not** trigger the unmatched-line strategy:

```java
FixedFormatReader.<EmployeeRecord>builder()
    .addMapping(EmployeeRecord.class, new RegexFixedFormatMatchPattern(".*"))
    .includeLines(line -> !line.isBlank() && !line.startsWith("#"))
    .build();
```
