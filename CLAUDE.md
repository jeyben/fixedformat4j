# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Java Version

Tests require Java 11. Set `JAVA_HOME` before running Maven:

```bash
# Temurin 11 (installed via brew install --cask temurin@11 or sdkman)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home

# Or use java_home helper on macOS
export JAVA_HOME=$(/usr/libexec/java_home -v 11)

mvn test
```

## Build and Test Commands

This is a Maven multi-module project. Run from the repo root:

```bash
# Build and install all modules
mvn install

# Run all tests
mvn test

# Run tests for the main library only
cd fixedformat4j && mvn test

# Run a single test class
mvn test -pl fixedformat4j -Dtest=TestBigDecimalFormatter

# Run a single test method
mvn test -pl fixedformat4j -Dtest=TestBigDecimalFormatter#testSomeMethod
```

## Project Structure

Four Maven modules:
- `fixedformat4j/` — the core library (main artifact)
- `fixedformat4j-processor/` — optional compile-time annotation processor (since 1.9.0); mirrors the statically decidable subset of the runtime validation via the mirror API. Any change to `FieldValidator`/`PatternValidator` checks must be mirrored in the processor's `FieldChecker`/`RecordValidator`, and vice versa.
- `fixedformat4j-micrometer/` — optional Micrometer instrumentation module (since 1.9.0); decorator-based, no changes to the core artifact; never add Micrometer hooks to core.
- `samples/` — usage examples

**Version synchronisation:** none of the modules declare a `<parent>`. When bumping the project version, update the `<version>` element in **every** module pom.xml (`fixedformat4j/pom.xml`, `fixedformat4j-processor/pom.xml`, `fixedformat4j-micrometer/pom.xml`, `samples/pom.xml`) in addition to the root `pom.xml`.

Core production source is under `fixedformat4j/src/main/java/com/ancientprogramming/fixedformat4j/`.

## Architecture

The library maps Java POJOs annotated with `@Record` and `@Field` to/from fixed-width flat-file strings.

**Entry point:** `FixedFormatManager` interface, implemented by `FixedFormatManagerImpl`. Two operations:
- `load(Class<T>, String data)` — parses a fixed-width string into a new instance of the annotated class
- `export(T instance)` / `export(String template, T instance)` — serializes an annotated instance to a fixed-width string

`FixedFormatManagerImpl` additionally implements `FixedFormatIntrospector` (since 1.9.0): `introspect(Class<?>)` exposes the field layout as public immutable `FieldInfo` descriptors, ordered by offset. The interface is deliberately separate from `FixedFormatManager` (ISP; keeps third-party manager implementations compatible).

**Annotation layer** (`annotation/` package):
- `@Record` — marks a class as a fixed-format record; declares total length and padding char
- `@Field` — placed on getter methods (or record components, since 1.9.0); declares `offset` (1-based), `length`, `align`, `paddingChar`, and optional `formatter`
- `@Fields` — groups multiple `@Field` annotations on one getter (for multi-format fields)
- `@FixedFormatNumber`, `@FixedFormatDecimal`, `@FixedFormatBoolean`, `@FixedFormatPattern` — supplementary annotations on getters to control number signs, decimal handling, boolean values, and date/time patterns

**Formatter layer** (`format/` and `format/impl/` packages):
- `FixedFormatter<T>` — interface with `parse(String, FormatInstructions)` and `format(T, FormatInstructions)`
- `AbstractFixedFormatter<T>` — base class that handles padding/alignment; subclasses implement `asObject()` and `asString()`
- `ByTypeFormatter` — default formatter; dispatches to the appropriate typed formatter based on the field's Java type (String, Integer, Long, Short, Double, Float, BigDecimal, Boolean, Character, Date)
- Individual formatters in `format/impl/`: `StringFormatter`, `IntegerFormatter`, `LongFormatter`, `ShortFormatter`, `DoubleFormatter`, `FloatFormatter`, `BigDecimalFormatter`, `BooleanFormatter`, `CharacterFormatter`, `DateFormatter`
- `AbstractNumberFormatter` and `AbstractDecimalFormatter` — shared logic for numeric types

**Data objects** (`format/data/` package): Immutable value objects (`FixedFormatBooleanData`, `FixedFormatDecimalData`, `FixedFormatNumberData`, `FixedFormatPatternData`) that carry parsed annotation configuration into formatter calls.

**Context objects:**
- `FormatContext<T>` — carries the field offset, data type, and formatter class
- `FormatInstructions` — carries length, alignment, padding char, and the four data objects above
- `FixedFormatUtil` — static helpers for slicing the data string (`fetchData`) and instantiating formatters via reflection

**Recursive record support:** If a field's type is itself annotated with `@Record`, `FixedFormatManagerImpl` recursively loads/exports that nested type.

**Java record support (since 1.9.0):** `@Record` classes may be Java `record` types (JDK 16+). Annotations on record components propagate to the accessors; `load()` binds all parsed values through the canonical constructor in one call (`JavaRecordSupport` + `ConstructorBinding`, cached in `ClassMetadataCache`). The artifact stays compiled at release 11 — record APIs are accessed reflectively, once per class.

## GitHub Actions

All workflows must use Node.js 24 compatible actions (Node.js 20 is removed from runners September 16, 2026):
- Prefer actions with native Node.js 24 support: `actions/checkout@v6`, `actions/setup-java@v5`, `actions/configure-pages@v6`, `actions/upload-pages-artifact@v4`, `actions/deploy-pages@v5`.
- For actions without a Node.js 24-native version yet (e.g. `jekyll-build-pages`, `upload-pages-artifact` — which internally calls `upload-artifact@v4`), add `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` to the workflow-level `env:` block.

## Key Conventions

- Field offsets are **1-based**.
- `@Field` annotations go on **getter** methods (`get*` or `is*`) for conventional POJOs, or on **record components** for Java `record` types (since 1.9.0); the manager derives the setter name by reflection for POJOs, and uses the canonical constructor for records.
- The default formatter (`ByTypeFormatter`) is chosen automatically from the getter's return type; specify `formatter=` on `@Field` only when overriding.
- Tests live in `fixedformat4j/src/test/java/` and use JUnit 5 (Jupiter). Issue-specific regression tests are under `issues/` sub-package. Tests that need JDK 16+ syntax (e.g. Java records) live in `fixedformat4j/src/test/java17/`, compiled only on JDK 17+ via the auto-activated `jdk17-tests` profile.

## Framework-breaking changes — consider twice

This is a public library. Any change that alters observable behavior for consumers must be treated as a breaking-change candidate and handled with extra care. This section applies **both when implementing and when reviewing PRs** — the `/code-review` workflow should use the list below as an explicit checklist and flag every item it finds.

**What counts as framework-breaking:**
- Annotation surface — adding / removing / renaming attributes on `@Field`, `@Record`, `@Fields`, `@FixedFormatNumber`, `@FixedFormatDecimal`, `@FixedFormatBoolean`, `@FixedFormatPattern`, or changing their default values.
- Public interfaces — `FixedFormatManager`, `FixedFormatter<T>` method signatures or contracts.
- Extension points — protected methods on `AbstractFixedFormatter`, `AbstractNumberFormatter`, `AbstractDecimalFormatter`, `AbstractPatternFormatter` that user formatters subclass.
- Activation gates — conditions under which optional behavior fires (e.g. `NullCharSupport.isNullCharActive`), even when the code change is one line.
- Parse / export semantics — anything that changes the output for a given input: null handling, padding, sign handling, rounding, round-trip fidelity.
- Exception types or validation messages at documented boundaries.

**When implementing such a change:**

1. **Pause and flag it.** Stop before editing. Surface the change explicitly and request confirmation, even if TDD is green. A passing suite proves the new behavior is correct for the new rule — not that no consumer depended on the old rule.

2. **Write the breaking-change checklist** into the plan and PR description:
   - **What breaks** — a concrete input/output pair that changes (e.g. `"     "` on `@Field(nullChar=' ', paddingChar=' ')` loaded `0` before, loads `null` after).
   - **Who's affected** — annotation consumers, formatter subclassers, on-disk records serialized by prior versions.
   - **Round-trip impact** — does `load(export(x)) == x` still hold for existing records?
   - **Migration / deprecation path** — is the old behavior reachable via an opt-out? Is a major-version bump warranted? What does the changelog entry say?

**When reviewing a PR:**

- Walk the "What counts as framework-breaking" list and post an inline comment on any diff hunk that touches one of those surfaces.
- If the checklist is missing from the PR description and the diff qualifies, call that out as a blocker.
- Treat one-line changes with the same scrutiny as large diffs — small diff, large blast radius.

## Design — SOLID principles

When designing a new class, interface, or refactoring an existing one, explicitly apply SOLID and call out where a proposal bends or breaks each principle.

- **SRP (Single Responsibility)** — one reason to change per class. Watch for formatters that mix parsing, validation, and error reporting; lift responsibilities apart when a second reason to change appears.
- **OCP (Open / Closed)** — prefer extension over modification for public types. A new `@Field` attribute or a new `FixedFormatter<T>` subclass should not force edits to unrelated formatters.
- **LSP (Liskov Substitution)** — subclasses of `AbstractFixedFormatter`, `AbstractNumberFormatter`, `AbstractDecimalFormatter`, `AbstractPatternFormatter` must preserve the superclass contract. Do not narrow return types, throw unexpected exceptions, or change null semantics in an override.
- **ISP (Interface Segregation)** — `FixedFormatManager` and `FixedFormatter<T>` exist as narrow interfaces for a reason. Do not widen them speculatively; add a second interface if a new capability applies only to some implementations.
- **DIP (Dependency Inversion)** — depend on the `FixedFormatter<T>` / `FixedFormatManager` abstractions, not on concrete classes. `ByTypeFormatter` is the canonical indirection site.

During plan-mode design, name which principles the proposal upholds and which it consciously trades off. During PR review, flag diffs that silently violate any of the five.
