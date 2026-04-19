# Plan: Address PR #83 Review Findings

**Branch**: features/82_large-file-reading-support
**Status**: Active

## Goal

Resolve the design, resource-management, and API consistency issues identified in the PR #83 review so `FixedFormatReader` ships with a clean, extensible public API.

## Acceptance Criteria

- [ ] `readWithCallback(Reader, BiConsumer)` does not leave the underlying `InputStream` unclosed when called via an `InputStream` overload
- [ ] Strategy dispatch (unmatched / parse-error / multi-match) does not use `switch` over internal enums; each strategy carries its own behaviour
- [ ] `ClassPatternMapping` constructor rejects `null` with `IllegalArgumentException` (not `NullPointerException`)
- [ ] `readWithCallback(InputStream, Charset, Consumer<T>)` overload exists, completing the symmetric overload matrix
- [ ] `FixedFormatReader.Builder` no longer hard-wires `new FixedFormatManagerImpl()` in the `io` package
- [ ] `TestStrategiesAndHandlers` enum-not-null assertions are replaced with behaviour tests
- [ ] `SKIP_AND_LOG` Javadoc documents the SLF4J runtime dependency and recommends `FORWARD_TO_HANDLER` for guaranteed visibility
- [ ] `readAsMap(Reader)` uses a single-pass `computeIfAbsent` approach (no pre-populate + prune)

## Deferred (noted, not addressed in this PR)

- Extendability of `ClassPatternMapping` into a general `Mapping<T>` interface — a breaking API change requiring a major version. Captured as a future issue.
- `FixedFormatReader` extension points (`protected` methods or interface extraction) — similarly breaking. Captured as a future issue.

## Steps

Every step follows RED-GREEN-MUTATE-KILL MUTANTS-REFACTOR. No production code without a failing test.

---

### Step 1: Fix resource leak — `readWithCallback(Reader, BiConsumer)` must close the reader it wraps

**Context**: `readAsMap(InputStream, Charset)` calls `readAsMap(new InputStreamReader(...))` which calls
`readWithCallback(reader, BiConsumer)`. That method wraps the reader in a `BufferedReader` but never closes either.
The `Consumer` variant is safe because it delegates through `readAsStream` which registers `onClose`. The `BiConsumer`
variant bypasses `readAsStream` and has no close at all.

**Acceptance criteria**: After reading via `readAsMap(InputStream)`, `readWithCallback(InputStream, Consumer)`,
or `readWithCallback(InputStream, BiConsumer)`, the underlying `InputStream` is closed. Verified by a test that
wraps a stream in a spy/subclass that tracks `close()` calls.

**RED**: Write a failing test in `TestFixedFormatReaderInputSources` that asserts the stream is closed after
`readWithCallback(inputStream, biConsumer)` (and the analogous `Consumer` path via `readAsMap`).

**GREEN**: Change `readWithCallback(Reader, BiConsumer)` to use try-with-resources so the reader is always closed.
The `Consumer` overload already delegates through `readAsStream` (which closes), so no change needed there.
For `readAsMap(InputStream, Charset)` — since the reader is now closed by the delegate, the pattern is consistent.

**MUTATE**: Run mutation testing on the changed method.

**KILL MUTANTS**: Address surviving mutants.

**REFACTOR**: Assess whether the `BufferedReader`-wrapping logic shared between `readAsStream(Reader)` and
`readWithCallback(Reader, BiConsumer)` can be extracted to a private helper to eliminate the duplication.

**Done when**: All tests green, resource-leak test passes, mutation report reviewed, human approves commit.

---

### Step 2: Fix null-safety in `ClassPatternMapping` and `Builder.addMapping`

**Context**: `ClassPatternMapping(null, pattern)` throws `NullPointerException` (from `null.getAnnotation(...)`)
instead of `IllegalArgumentException`. Similarly `addMapping(clazz, null)` produces an NPE later.

**Acceptance criteria**: `new ClassPatternMapping(null, pattern)` throws `IllegalArgumentException` with a
clear message. `builder.addMapping(clazz, null)` throws `IllegalArgumentException`. Verified by unit tests
in `TestClassPatternMapping` and `TestFixedFormatReaderBuilder`.

**RED**: Add failing tests for the two null cases.

**GREEN**: Add `Objects.requireNonNull`-style guards (or explicit null checks with messages) at the top of
`ClassPatternMapping` constructor and `Builder.addMapping`.

**MUTATE / KILL MUTANTS / REFACTOR**: Standard cycle.

**Done when**: Null-input tests pass, mutation report reviewed, human approves commit.

---

### Step 3: Replace `switch`-on-enum dispatch with behaviour-carrying strategy methods

**Context**: `resolveMatches`, `handleParseError`, and `handleUnmatched` each switch over an enum they own.
Adding a new strategy value requires editing `FixedFormatReader`. This violates OCP.

**Design**: Add an abstract method (or interface method) to each strategy enum that carries the behaviour
directly. For example:

```java
// UnmatchedLineStrategy
public enum UnmatchedLineStrategy {
  SKIP {
    @Override public void handle(long lineNumber, String line, UnmatchedLineHandler handler) {}
  },
  THROW {
    @Override public void handle(long lineNumber, String line, UnmatchedLineHandler handler) {
      throw new FixedFormatException("No pattern matched line " + lineNumber + ": " + line);
    }
  },
  FORWARD_TO_HANDLER {
    @Override public void handle(long lineNumber, String line, UnmatchedLineHandler handler) {
      handler.handle(lineNumber, line);
    }
  };
  public abstract void handle(long lineNumber, String line, UnmatchedLineHandler handler);
}
```

Apply the same pattern to `ParseErrorStrategy` and `MultiMatchStrategy`.

**Acceptance criteria**: `FixedFormatReader` contains no `switch` statements over strategy enums.
All existing strategy tests continue to pass.

**RED**: No new behaviour is introduced; the refactor step is its own test via the existing strategy tests.
Write a compile-time "test" first: remove the `default` case from one switch and verify the compiler
now enforces exhaustiveness — confirming the old switch was the only dispatch point.

**GREEN**: Migrate each enum to the behaviour-carrying constant style. Remove the three switch methods from
`FixedFormatReader` and call the enum method directly.

**MUTATE / KILL MUTANTS / REFACTOR**: Standard cycle.

**Done when**: No switches on strategy enums in `FixedFormatReader`, all tests green, mutation report reviewed, human approves commit.

---

### Step 4: Add missing `readWithCallback(InputStream, Charset, Consumer<T>)` overload

**Context**: `readWithCallback(InputStream, Charset, BiConsumer)` exists but the `Consumer` equivalent does not,
creating an asymmetric overload matrix.

**Acceptance criteria**: `readWithCallback(InputStream, charset, consumer)` exists and works. The full
overload matrix for `readWithCallback` is symmetric across `(Reader, Consumer)`, `(Reader, BiConsumer)`,
`(InputStream, Consumer)`, `(InputStream, Charset, Consumer)`, `(InputStream, BiConsumer)`,
`(InputStream, Charset, BiConsumer)`, `(File, Consumer)`, `(File, Charset, Consumer)`,
`(File, BiConsumer)`, `(File, Charset, BiConsumer)`, `(Path, Consumer)`, `(Path, Charset, Consumer)`,
`(Path, BiConsumer)`, `(Path, Charset, BiConsumer)`.

**RED**: Write a failing test for `readWithCallback(inputStream, charset, consumer)`.

**GREEN**: Add the missing overload, delegating to `readWithCallback(new InputStreamReader(inputStream, charset), consumer)`.

**MUTATE / KILL MUTANTS / REFACTOR**: Standard cycle.

**Done when**: Missing overload added, overload matrix complete, mutation report reviewed, human approves commit.

---

### Step 5: Eliminate the DIP violation — decouple `Builder` default from `FixedFormatManagerImpl`

**Context**: `Builder` field `private FixedFormatManager manager = new FixedFormatManagerImpl()` in the `io`
package directly constructs a class from the `format.impl` package, coupling layers.

**Design**: Add a static factory method `FixedFormatManager.defaultManager()` (or a package-private factory in
the `format` package) that returns a `FixedFormatManagerImpl`. The builder's default becomes
`FixedFormatManager.defaultManager()`. The `io` package then depends only on the `FixedFormatManager` interface.

**Acceptance criteria**: `FixedFormatReader.java` no longer imports `FixedFormatManagerImpl`. The default
behaviour is unchanged. A builder with no explicit `manager(...)` call still parses records correctly.

**RED**: Write (or adapt an existing) test that builds a reader with no explicit `manager()` call and asserts
records parse correctly — this should already exist; confirm it acts as the regression guard.

**GREEN**: Add `FixedFormatManager.defaultManager()`, update the builder default, remove the `FixedFormatManagerImpl`
import from `FixedFormatReader`.

**MUTATE / KILL MUTANTS / REFACTOR**: Standard cycle.

**Done when**: `FixedFormatReader` has no direct dependency on `FixedFormatManagerImpl`, tests green, human approves commit.

---

### Step 6: Replace zero-value enum-not-null tests; document `SKIP_AND_LOG`; simplify `readAsMap(Reader)`

Three small cleanups bundled in one step:

**6a — `TestStrategiesAndHandlers`**: Delete the `assertNotNull(EnumConstant)` assertions. Replace with at
least one per-strategy behaviour test that exercises the strategy through `FixedFormatReader` (e.g., a
`SKIP_AND_LOG` test that asserts the record is absent from the result and a log message was emitted).

**6b — `ParseErrorStrategy.SKIP_AND_LOG` Javadoc**: Add a note that SLF4J must have a binding on the classpath
for the log to appear, and recommend `FORWARD_TO_HANDLER` when guaranteed visibility is required.

**6c — `readAsMap(Reader)` simplification**: The current code pre-populates all keys then prunes empty ones.
Simplify to a single-pass `computeIfAbsent` inside the callback, preserving the `LinkedHashMap` key-order
guarantee (registered-class order) only for classes that actually matched.

**Acceptance criteria**: No `assertNotNull(SomeEnum.CONSTANT)` in the test suite. `SKIP_AND_LOG` Javadoc
mentions the SLF4J dependency. `readAsMap(Reader)` contains no pre-populate loop.

**RED**: Write failing tests for `SKIP_AND_LOG` observable behaviour (record skipped + log captured).
Write a test confirming `readAsMap` key order matches registration order even with gaps.

**GREEN**: Apply all three changes.

**MUTATE / KILL MUTANTS / REFACTOR**: Standard cycle.

**Done when**: All acceptance criteria met, mutation report reviewed, human approves commit.

---

## Pre-PR Quality Gate

Before opening the PR:
1. `mvn test` — all tests green
2. Mutation testing — run `mutation-testing` skill; no unaddressed surviving mutants
3. Refactoring assessment — run `refactoring` skill
4. Verify `FixedFormatReader.java` imports: no `FixedFormatManagerImpl`, no `switch` on strategy enums

---
*Delete this file when the plan is complete. If `plans/` is empty, delete the directory.*
