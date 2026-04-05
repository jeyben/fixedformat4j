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

Two Maven modules:
- `fixedformat4j/` — the core library (main artifact)
- `samples/` — usage examples

All production source is under `fixedformat4j/src/main/java/com/ancientprogramming/fixedformat4j/`.

## Architecture

The library maps Java POJOs annotated with `@Record` and `@Field` to/from fixed-width flat-file strings.

**Entry point:** `FixedFormatManager` interface, implemented by `FixedFormatManagerImpl`. Two operations:
- `load(Class<T>, String data)` — parses a fixed-width string into a new instance of the annotated class
- `export(T instance)` / `export(String template, T instance)` — serializes an annotated instance to a fixed-width string

**Annotation layer** (`annotation/` package):
- `@Record` — marks a class as a fixed-format record; declares total length and padding char
- `@Field` — placed on getter methods; declares `offset` (1-based), `length`, `align`, `paddingChar`, and optional `formatter`
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

## GitHub Actions

All workflows must use Node.js 24 compatible actions (Node.js 20 is removed from runners September 16, 2026):
- Prefer actions with native Node.js 24 support: `actions/checkout@v6`, `actions/setup-java@v5`, `actions/configure-pages@v6`, `actions/upload-pages-artifact@v4`, `actions/deploy-pages@v5`.
- For actions without a Node.js 24-native version yet (e.g. `jekyll-build-pages`, `upload-pages-artifact` — which internally calls `upload-artifact@v4`), add `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` to the workflow-level `env:` block.

## Key Conventions

- Field offsets are **1-based**.
- `@Field` annotations go on **getter** methods (`get*` or `is*`); the manager derives the setter name by reflection.
- The default formatter (`ByTypeFormatter`) is chosen automatically from the getter's return type; specify `formatter=` on `@Field` only when overriding.
- Tests live in `fixedformat4j/src/test/java/` and use JUnit 5 (Jupiter). Issue-specific regression tests are under `issues/` sub-package.
