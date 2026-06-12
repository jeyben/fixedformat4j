---
title: Compile-time validation
---

# Compile-time validation

Since 1.9.0 an optional annotation processor, published as the separate artifact
`fixedformat4j-processor`, validates `@Field` / `@Record` configuration during `javac`.
Misconfigurations that would otherwise surface as a `FixedFormatException` on the first
runtime use of a record class — potentially in production — become compile errors instead:

```
[ERROR] MyRecord.java:[8,15] @Field length 36 exceeds @Record length 20 on MyRecord.getCreatedAt() (field ends at position 36)
[ERROR] MyRecord.java:[8,15] Invalid date pattern 'not-a-date-pattern' on MyRecord.getCreatedAt(): Illegal pattern character 'n'
```

The processor is purely additive: it claims no annotations, generates no code, and adds
**nothing to the runtime classpath** — its cost is compile-time only. Projects that do not
enable it behave exactly as before, and the runtime validation stays in place as the safety
net for classes compiled without the processor.

## Enabling the processor

### Maven (recommended)

Add the processor to the compiler plugin's processor path. This keeps it off the project's
dependency tree entirely:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>com.ancientprogramming.fixedformat4j</groupId>
        <artifactId>fixedformat4j-processor</artifactId>
        <version>1.9.0</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Alternatively, a `provided`-scope dependency also works — `javac` discovers the processor on
the classpath via the standard `ServiceLoader` mechanism:

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j-processor</artifactId>
  <version>1.9.0</version>
  <scope>provided</scope>
</dependency>
```

> **Note (JDK 23+):** annotation processing from the classpath is no longer implicit. With the
> `provided`-scope variant, also pass `-proc:full` to `javac` (Maven:
> `<compilerArgs><arg>-proc:full</arg></compilerArgs>`). The `annotationProcessorPaths` variant
> needs no extra flag.

### Gradle

Groovy DSL (`build.gradle`):

```groovy
annotationProcessor 'com.ancientprogramming.fixedformat4j:fixedformat4j-processor:1.9.0'
```

Kotlin DSL (`build.gradle.kts`):

```kotlin
annotationProcessor("com.ancientprogramming.fixedformat4j:fixedformat4j-processor:1.9.0")
```

## What is checked

All findings are reported as compile **errors** on the offending getter, field, or record
component.

Checks that mirror the runtime validation (the same configurations throw a
`FixedFormatException` on first use of the class at runtime):

| Check | Example |
|-------|---------|
| Invalid `@FixedFormatPattern` for the field type (`Date` → `SimpleDateFormat`, `LocalDate`/`LocalDateTime` → `DateTimeFormatter`) | `@FixedFormatPattern("not-a-date-pattern")` on a `Date` getter |
| Enum value wider than `@Field(length)` — longest constant name for LITERAL, ordinal digit width for `@FixedFormatEnum(NUMERIC)` | `enum Status { REJECTED_BY_BANK }` in `@Field(length = 5)` |
| `nullChar` on a primitive-typed field (including primitive element types of repeating fields) | `@Field(length = 5, nullChar = ' ') int getAmount()` |
| `nullValue` misuse: combined with `nullChar`, length differing from `@Field(length)`, or on a primitive type | `@Field(length = 5, nullValue = "9998")` |
| `@Field(length = -1)` (rest-of-line) rule violations: non-String type, `count > 1`, explicit `align`/`paddingChar`/`nullChar`/`nullValue`, more than one per record, not the last field by offset, or combined with a fixed `@Record(length)` | `@Field(offset = 1, length = -1) Integer getRest()` |

Checks **stricter than the runtime** — the 1.8.x runtime tolerates these silently (padding or
truncating on export, later fields overwriting earlier ones), so enabling the processor can
turn latent configuration bugs in existing code into compile errors. Runtime behavior is
unchanged either way; fix the annotation or don't enable the processor for that module:

| Check | Example |
|-------|---------|
| Field runs past the declared `@Record(length)` (repetitions of `count > 1` fields included) | `@Field(offset = 1, length = 36)` inside `@Record(length = 20)` |
| Two fields occupy overlapping character ranges (duplicate offsets included) | `@Field(offset = 1, length = 10)` and `@Field(offset = 5, length = 10)` |

Checks that need a live class — custom formatter instantiation, getter/setter pairing — remain
runtime-only.

## Java records

Components of Java `record` types (JDK 16+) are validated exactly like getters; the processor
itself runs on JDK 11 and newer. One caveat applies to any source compiled on JDK 16+: with a
star import of `com.ancientprogramming.fixedformat4j.annotation.*`, the simple name `Record`
is ambiguous with `java.lang.Record` — add the explicit
`import com.ancientprogramming.fixedformat4j.annotation.Record;` to resolve it.
