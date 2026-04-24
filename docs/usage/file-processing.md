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

A `Predicate<String>` decides whether a line belongs to a particular record class.
The `LinePredicates.regex(String)` factory (designed for static import) is the most
concise option — it compiles the regular expression once and applies it with `find()`
semantics (partial-line match):

```java
import static com.ancientprogramming.fixedformat4j.io.read.LinePredicates.regex;

Predicate<String> employeePattern = regex("^EMP");  // lines starting with "EMP"
Predicate<String> anyLine         = regex(".*");     // every line
```

Pass any `Predicate<String>` for custom discrimination logic — for example, testing
a fixed-width type code in a specific column:

```java
Predicate<String> headerPattern =
    line -> line.length() >= 3 && "HDR".equals(line.substring(0, 3));
```

---

## Building a reader

Use the fluent builder to configure the reader. At least one mapping must be added before calling `build()`.

**Single-type file:**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, regex(".*"))
    .build();
```

**Heterogeneous file (strict — throw on any unrecognised line):**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, regex("^HDR"))
    .addMapping(DetailRecord.class, regex("^DTL"))
    .build();
```

**Heterogeneous file (lenient — skip and log unrecognised lines):**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, regex("^HDR"))
    .addMapping(DetailRecord.class, regex("^DTL"))
    .unmatchStrategy(UnmatchStrategy.skip())
    .build();
```

Mappings are evaluated in registration order. The `multiMatchStrategy` controls what happens if more than one pattern matches (see [Strategies](#strategies) below).

---

## Reading as ReadResult

`read` is the recommended collect-then-process method for heterogeneous files. It reads all records eagerly and returns a `ReadResult` — a type-safe, class-keyed container that eliminates casts at the call site.

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, regex("^HDR"))
    .addMapping(DetailRecord.class, regex("^DTL"))
    .build();

ReadResult result = reader.read(Path.of("data.txt"));

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

Overloads are available for `Path`, `InputStream`, and `Reader`. All default to UTF-8; pass an explicit `Charset` when needed.

---

## Typed handler dispatch

`process` is the push-style alternative to `read`. Instead of collecting records and querying the result, you supply a `HandlerRegistry` at call time; `process` parses each line and immediately dispatches the record to the matching handler.

Supply handlers via a `HandlerRegistry` at the call site:

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, regex("^HDR"))
    .addMapping(DetailRecord.class, regex("^DTL"))
    .build();

reader.process(Path.of("data.txt"), new HandlerRegistry()
    .on(HeaderRecord.class, header -> System.out.println("Header: " + header.getDate()))
    .on(DetailRecord.class, detail -> System.out.println("Detail: " + detail.getOrderId())));
```

Classes not registered in the `HandlerRegistry` are silently ignored — they are still parsed and routed, but no handler is invoked. Because the registry is supplied per call, the same reader instance is safe to use from multiple threads with independent registries. The same source-type overloads (`Path`, `InputStream`, `Reader`) are available as for `read`.


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
| `UnmatchStrategy.throwException()` *(default)* | Throw `FixedFormatException` with the line number and raw content. |
| `UnmatchStrategy.skip()` | Skip the line and log a WARN via SLF4J. Useful when some record types are intentionally ignored. |
| Lambda | Invoke any custom logic; throw to abort, return to continue. |

```java
FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, regex("^EMP"))
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
    .addMapping(EmployeeRecord.class, regex(".*"))
    .parseErrorStrategy((wrapped, line, lineNumber) ->
        System.err.println("Parse error on line " + lineNumber + ": " + wrapped.getMessage()))
    .build();
```

**Note:** `UnmatchStrategy.skip()` and `ParseErrorStrategy.skipAndLog()` only log if an SLF4J binding is present at runtime. For guaranteed error visibility, use a custom lambda that writes to your preferred output.

---

## Pre-match filtering

Use `excludeLines` to drop lines before pattern matching. Lines for which the predicate returns `true` are silently skipped and do **not** trigger the unmatched-line strategy:

```java
FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, regex(".*"))
    .excludeLines(line -> line.isBlank() || line.startsWith("#"))
    .build();
```
