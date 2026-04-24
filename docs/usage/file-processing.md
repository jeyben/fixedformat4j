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

A `LinePattern` decides whether a line belongs to a particular record class. The built-in `RegexLinePattern` compiles a regular expression at construction time and applies it with `find()` semantics (partial-line match).

```java
// Matches any line that starts with "EMP"
LinePattern employeePattern = new RegexLinePattern("^EMP");

// Matches every line (use for single-type files)
LinePattern anyLine = new RegexLinePattern(".*");
```

Implement `LinePattern` directly for custom discrimination logic — for example, testing a fixed-width type code in a specific column:

```java
LinePattern headerPattern = line ->
    line != null && line.length() >= 3 && "HDR".equals(line.substring(0, 3));
```

---

## Building a reader

Use the fluent builder to configure the reader. At least one mapping must be added before calling `build()`.

**Single-type file:**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, new RegexLinePattern(".*"))
    .build();
```

**Heterogeneous file:**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, new RegexLinePattern("^HDR"))
    .addMapping(DetailRecord.class, new RegexLinePattern("^DTL"))
    .unmatchStrategy(UnmatchStrategy.skip())
    .build();
```

Mappings are evaluated in registration order. The `multiMatchStrategy` controls what happens if more than one pattern matches (see [Strategies](#strategies) below).

---

## Reading into a List

`readAsList` eagerly reads all records and returns them as `List<Object>` in encounter order. For typed access without casts, prefer `readAsResult` (see below). Overloads are available for `File`, `Path`, `InputStream`, and `Reader`. All default to UTF-8; pass an explicit `Charset` when needed.

```java
// From a File (UTF-8)
List<Object> records = reader.readAsList(new File("employees.txt"));

// From a Path with an explicit charset
List<Object> records = reader.readAsList(
    Path.of("employees.txt"), StandardCharsets.ISO_8859_1);

// From any Reader
List<Object> records = reader.readAsList(new StringReader(data));
```

---

## Streaming large files

`readAsStream` returns a **lazy** `Stream<Object>` backed by the underlying reader. Lines are read on demand — only the current line is held in memory. The stream closes the reader automatically when the stream itself is closed, so always use try-with-resources:

```java
try (Stream<Object> stream = reader.readAsStream(Path.of("employees.txt"))) {
    stream
        .filter(r -> r instanceof EmployeeRecord)
        .map(r -> (EmployeeRecord) r)
        .filter(e -> e.getEmployeeId() > 1000)
        .forEach(this::process);
}
```

For typed dispatch without `instanceof` casts, `readAsResult` or `processAll` are preferred.

The stream is sequential and ordered. The same `File`, `Path`, `InputStream`, and `Reader` overloads are available as for `readAsList`.

---

## Reading as ReadResult

`readAsResult` is the recommended collect-then-process method for heterogeneous files. It reads all records eagerly and returns a `ReadResult` — a type-safe, class-keyed container that eliminates casts at the call site.

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, new RegexLinePattern("^HDR"))
    .addMapping(DetailRecord.class, new RegexLinePattern("^DTL"))
    .unmatchStrategy(UnmatchStrategy.skip())
    .build();

ReadResult result = reader.readAsResult(Path.of("data.txt"));

List<HeaderRecord> headers = result.get(HeaderRecord.class); // no cast
List<DetailRecord> details = result.get(DetailRecord.class); // no cast
```

`ReadResult` key methods:

| Method | Returns | Description |
|---|---|---|
| `get(Class<R>)` | `List<R>` | All records of type `R`; empty list if none matched. |
| `getAll()` | `List<Object>` | All records in encounter order, regardless of type. |
| `contains(Class<?>)` | `boolean` | `true` if at least one record of the given class was parsed. |
| `classes()` | `Set<Class<?>>` | Set of all classes that produced at least one record. |

The same `File`, `Path`, `InputStream`, and `Reader` overloads are available as for `readAsList`.

---

## Typed handler dispatch

`processAll` is the push-style alternative to `readAsResult`. Instead of collecting records and querying the result, you register a typed `Consumer<R>` handler per mapping; `processAll` parses each line and immediately dispatches the record to the matching handler.

Register handlers at build time using the three-argument `addMapping` overload:

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, new RegexLinePattern("^HDR"),
        header -> System.out.println("Header: " + header.getDate()))
    .addMapping(DetailRecord.class, new RegexLinePattern("^DTL"),
        detail -> System.out.println("Detail: " + detail.getOrderId()))
    .unmatchStrategy(UnmatchStrategy.skip())
    .build();

reader.processAll(Path.of("data.txt"));
```

Mappings registered without a handler (the two-argument `addMapping` overload) are silently skipped during `processAll` — the line is still parsed and routed, but no handler is invoked. The same source-type overloads (`File`, `Path`, `InputStream`, `Reader`) are available as for other output shapes.

---

## Per-record callbacks

`readWithCallback` drives the read loop and invokes a callback for each parsed record. Two signatures are available:

**`Consumer<Object>`** — receive only the record:

```java
reader.readWithCallback(new File("employees.txt"),
    record -> System.out.println(record));
```

**`BiConsumer<Class<?>, Object>`** — receive the matched class and the record:

```java
reader.readWithCallback(new File("data.txt"), (clazz, record) -> {
    if (clazz == HeaderRecord.class) {
        processHeader((HeaderRecord) record);
    } else if (clazz == DetailRecord.class) {
        processDetail((DetailRecord) record);
    }
});
```

For typed dispatch without casts, `processAll` with per-mapping handlers is preferred.

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
FixedFormatReader.builder()
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
| `UnmatchStrategy.skip()` *(default)* | Silently ignore the line. Useful for header, footer, or comment lines. |
| `UnmatchStrategy.throwException()` | Throw `FixedFormatException` with the line number and raw content. |
| Lambda | Invoke any custom logic; throw to abort, return to continue. |

```java
FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, new RegexLinePattern("^EMP"))
    .unmatchStrategy((lineNumber, line) ->
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
FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, new RegexLinePattern(".*"))
    .parseErrorStrategy((wrapped, line, lineNumber) ->
        System.err.println("Parse error on line " + lineNumber + ": " + wrapped.getMessage()))
    .build();
```

**Note:** `ParseErrorStrategy.skipAndLog()` only logs if an SLF4J binding is present at runtime. For guaranteed error visibility, use a custom lambda that writes to your preferred output.

---

## Pre-match filtering

Use `includeLines` to select which lines reach pattern matching. Lines for which the predicate returns `false` are silently skipped and do **not** trigger the unmatched-line strategy:

```java
FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, new RegexLinePattern(".*"))
    .includeLines(line -> !line.isBlank() && !line.startsWith("#"))
    .build();
```
