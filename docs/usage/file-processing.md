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

A `LinePattern` decides whether a line belongs to a particular record class. Three
factory methods cover the common cases:

```java
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;

// Lines whose first three characters are "EMP"
LinePattern employeePattern = LinePattern.prefix("EMP");

// Every line
LinePattern anyLine = LinePattern.matchAll();

// Lines whose characters at positions 0..3 are "K400" AND at positions 7..8 are "01"
LinePattern transactionPattern =
    LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001");
```

`prefix(literal)` is shorthand for `positional(new int[]{0, 1, ..., literal.length() - 1}, literal)`.

Patterns are restricted to position-and-literal matching on purpose: it lets the
reader bucket all registered mappings into hash tables at build time, so per-line
routing is near O(1) regardless of how many record types you register.

---

## Building a reader

Use the fluent builder to configure the reader. At least one mapping must be added before calling `build()`.

**Single-type file:**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, LinePattern.matchAll())
    .build();
```

**Heterogeneous file (strict — throw on any unrecognised line):**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
    .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
    .build();
```

**Heterogeneous file (lenient — skip and log unrecognised lines):**

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
    .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
    .unmatchStrategy(UnmatchStrategy.skip())
    .build();
```

When more than one pattern matches a line, the reader orders matches by **most
detailed first** (depth = number of positions in the pattern), then by registration
order as the tiebreaker. The `multiMatchStrategy` then chooses how to use that
ordered list (see [Strategies](#strategies) below).

---

## Worked example: heterogeneous file with a catch-all

Real fixed-format files often combine three routing needs in one file: short literal
type codes (`X1`, `X2`), record types whose unique identity spans multiple
non-contiguous columns (a major code at offsets 0–3 plus a sub-type at offsets 7–8
for `K`-records), and a long tail of "everything else" that should still be parsed
but doesn't warrant per-type classes. This example shows how to express all three
in one builder.

**The file**

Suppose `data.txt` looks like this (each line is 33 characters; `x` marks payload
columns whose values do not affect routing):

```
X1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
K400xxx01xxxxxxxxxxxxxxxxxxxxxxxx
K400xxx02xxxxxxxxxxxxxxxxxxxxxxxx
K410xxx01xxxxxxxxxxxxxxxxxxxxxxxx
X2xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
K410xxx03xxxxxxxxxxxxxxxxxxxxxxxx
SC410xxx03xxxxxxxxxxxxxxxxxxxxxxx
FC412xxx02xxxxxxxxxxxxxxxxxxxxxxx
SC411xxx03xxxxxxxxxxxxxxxxxxxxxxx
SC412xxx03xxxxxxxxxxxxxxxxxxxxxxx
FC412xxx03xxxxxxxxxxxxxxxxxxxxxxx
```

The routing intent:

- `X1` and `X2` are unique header-style records — one class each.
- Each `K`-record is uniquely identified by its major code (cols 0–3) and sub-type
  (cols 7–8), with three wildcard payload columns in between. `K40001`, `K40002`,
  `K41001`, and `K41003` each get their own class.
- Anything else — `SC*`, `FC*`, and any future type code — belongs in a single
  `OtherRecord` catch-all class.

**The record classes**

Each class is a normal `@Record`-annotated POJO. `@Field` getters are elided for
brevity:

```java
@Record(length = 33) public class X1Record     { /* @Field getters */ }
@Record(length = 33) public class X2Record     { /* @Field getters */ }
@Record(length = 33) public class K40001Record { /* @Field getters */ }
@Record(length = 33) public class K40002Record { /* @Field getters */ }
@Record(length = 33) public class K41001Record { /* @Field getters */ }
@Record(length = 33) public class K41003Record { /* @Field getters */ }
@Record(length = 33) public class OtherRecord  { /* @Field getters */ }
```

**The builder**

```java
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;

FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(X1Record.class,     LinePattern.prefix("X1"))
    .addMapping(X2Record.class,     LinePattern.prefix("X2"))
    .addMapping(K40001Record.class, LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001"))
    .addMapping(K40002Record.class, LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40002"))
    .addMapping(K41001Record.class, LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K41001"))
    .addMapping(K41003Record.class, LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K41003"))
    .addMapping(OtherRecord.class,  LinePattern.matchAll())
    .build();
```

**Why this works**

For every line the reader returns *all* matching mappings ordered by depth
descending (most detailed first), with registration order as the tiebreaker. The
default `MultiMatchStrategy.firstMatch()` then picks the head of that list:

| Line                | Matches (depth desc)                          | `firstMatch` picks |
|---------------------|----------------------------------------------|--------------------|
| `X1xxxxx…`          | X1Record (depth 2), OtherRecord (depth 0)    | **X1Record**       |
| `K400xxx01…`        | K40001Record (depth 6), OtherRecord (depth 0) | **K40001Record**  |
| `K410xxx03…`        | K41003Record (depth 6), OtherRecord (depth 0) | **K41003Record**  |
| `SC410xxx03…`       | OtherRecord (depth 0)                         | **OtherRecord**   |
| `FC412xxx02…`       | OtherRecord (depth 0)                         | **OtherRecord**   |
| any unknown line    | OtherRecord (depth 0)                         | **OtherRecord**   |

So the catch-all wins only when no specific pattern matches — exactly the desired
routing.

**Why no `unmatchStrategy` is configured**

Each line goes through this pipeline: `excludeLines` → `findMatches` → **if the
matched list is empty**, `unmatchStrategy.handle(...)` fires; otherwise
`multiMatchStrategy.resolve(...)` runs and the line is parsed.
`LinePattern.matchAll()` always matches, so for *every* line the matched list
contains at least the `OtherRecord` mapping. The unmatched branch is unreachable;
configuring `unmatchStrategy` here would have no observable effect.

This is a deliberate choice between two alternatives:

- **Catch-all class** (this example) — register `LinePattern.matchAll()` against a
  fallback record class. Unknown lines are parsed and grouped into that class.
  `unmatchStrategy` is not configured because it cannot fire.
- **No catch-all class** — omit the `LinePattern.matchAll()` mapping. Unknown
  lines now produce an empty matched list, triggering `unmatchStrategy`: default
  `throwException()` aborts processing; `UnmatchStrategy.skip()` drops the line
  with a WARN log; a custom lambda does whatever you like.

These are alternatives, not layers — pick one.

**Why no `multiMatchStrategy` is configured**

The default `firstMatch()` picks the most detailed match per line, which is what
this idiom needs. The other built-ins do not fit: `throwOnAmbiguity()` would throw
on every recognised line because every line *also* matches `matchAll()`;
`allMatches()` would emit a redundant `OtherRecord` alongside every specific
record.

**Performance note**

All four K-positional patterns share the same `positions` array
`{0, 1, 2, 3, 7, 8}`, so they live in one hash bucket inside the reader's
internal index. Per-line K-routing is a single hash lookup against the 6-character
extracted key (`"K40001"`, `"K41003"`, …) regardless of how many K-variants you
register. Adding `K42007`, `K43012`, … later is free.

**Consuming the result in encounter order**

`reader.read(path).getAll()` returns every parsed record in the order it
appeared in the file:

| index | class           | source line                           |
|-------|-----------------|---------------------------------------|
| 0     | `X1Record`      | `X1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`   |
| 1     | `K40001Record`  | `K400xxx01xxxxxxxxxxxxxxxxxxxxxxxx`   |
| 2     | `K40002Record`  | `K400xxx02xxxxxxxxxxxxxxxxxxxxxxxx`   |
| 3     | `K41001Record`  | `K410xxx01xxxxxxxxxxxxxxxxxxxxxxxx`   |
| 4     | `X2Record`      | `X2xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`   |
| 5     | `K41003Record`  | `K410xxx03xxxxxxxxxxxxxxxxxxxxxxxx`   |
| 6–10  | `OtherRecord`   | `SC410…`, `FC412…`, `SC411…`, …       |

Iterate the list and dispatch on type:

```java
List<Object> all = reader.read(Path.of("data.txt")).getAll();

for (Object record : all) {
    if (record instanceof X1Record) {
        handleX1((X1Record) record);
    } else if (record instanceof X2Record) {
        handleX2((X2Record) record);
    } else if (record instanceof K40001Record) {
        handleK40001((K40001Record) record);
    } else if (record instanceof K40002Record) {
        handleK40002((K40002Record) record);
    } else if (record instanceof K41001Record) {
        handleK41001((K41001Record) record);
    } else if (record instanceof K41003Record) {
        handleK41003((K41003Record) record);
    } else if (record instanceof OtherRecord) {
        handleOther((OtherRecord) record);
    }
}
```

If the goal is per-record, per-type processing in encounter order, prefer
`process(... HandlerRegistry)` — same ordering, no casts:

```java
reader.process(Path.of("data.txt"), new HandlerRegistry()
    .on(X1Record.class,     this::handleX1)
    .on(K40001Record.class, this::handleK40001)
    // …one .on(...) per class…
    .on(OtherRecord.class,  this::handleOther));
```

Use `getAll()` when you need a `List` for batch operations (count, filter,
transform a collection at once); use `process(...)` when you want per-record,
per-type processing in encounter order.

---

## Reading as ReadResult

`read` is the recommended collect-then-process method for heterogeneous files. It reads all records eagerly and returns a `ReadResult` — a type-safe, class-keyed container that eliminates casts at the call site.

```java
FixedFormatReader reader = FixedFormatReader.builder()
    .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
    .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
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
    .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
    .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
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
| `MultiMatchStrategy.firstMatch()` *(default)* | Use the most detailed match (largest depth); break ties by registration order. |
| `MultiMatchStrategy.throwOnAmbiguity()` | Throw `FixedFormatException` listing the line number and all matching class names. |
| `MultiMatchStrategy.allMatches()` | Emit one record per matching mapping, ordered most detailed first, ties by registration order. |

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

> **Note:** `UnmatchStrategy` only fires when *no* registered `LinePattern`
> matches a line. If you register a `LinePattern.matchAll()` catch-all (see the
> [worked example](#worked-example-heterogeneous-file-with-a-catch-all)) every
> line matches and the unmatched branch is unreachable — pick the catch-all *or*
> the unmatched strategy, not both.

```java
FixedFormatReader.builder()
    .addMapping(EmployeeRecord.class, LinePattern.prefix("EMP"))
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
    .addMapping(EmployeeRecord.class, LinePattern.matchAll())
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
    .addMapping(EmployeeRecord.class, LinePattern.matchAll())
    .excludeLines(line -> line.isBlank() || line.startsWith("#"))
    .build();
```
