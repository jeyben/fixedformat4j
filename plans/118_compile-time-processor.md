# Plan: fixedformat4j-processor — compile-time @Field/@Record validation (issue #118)

**Branch**: `features/118_compile-time-processor`
**Status**: Active
**Issue**: https://github.com/jeyben/fixedformat4j/issues/118

## Context

All annotation validation today fires at runtime, on first use of a `@Record` class
(`FixedFormatManagerImpl.validatedClasses` `ClassValue` → `FieldValidator` / `PatternValidator`,
both package-private in `format/impl`). A misconfigured annotation compiles cleanly and only
fails when `load()`/`export()` runs. Issue #118 proposes an optional annotation processor that
surfaces the statically decidable subset of those errors at `mvn compile`.

## Compatibility verdict

**Implementable in 1.9.0 with zero breaking changes and an easy 1.8.x upgrade path:**

- New, separate Maven artifact `fixedformat4j-processor`; the core `fixedformat4j` artifact is
  **not modified at all** (no annotation attributes, no public interfaces, no parse/export
  semantics — nothing on the CLAUDE.md framework-breaking checklist is touched).
- Runtime validation stays exactly as-is — it remains the safety net for classes compiled
  without the processor.
- Upgrade from 1.8.x = bump the version; enabling the compile-time check = add one
  `annotationProcessorPaths` entry (or a `provided` dependency). No consumer code changes.
- Version classification: **minor (1.9.0)** — purely additive.
- **Decision (maintainer)**: all checks emit compile **ERROR** (matching the issue's example
  output), including the two checks with no runtime counterpart (record-length overflow,
  duplicate offsets). Docs must state that enabling the processor can flag latent config bugs
  that the 1.8.x runtime tolerated — the processor is opt-in, so this is not a breaking change.

## Design decisions

1. **No shared validation module.** `FieldValidator`/`PatternValidator` are reflection-based and
   package-private; restructuring core mid-1.9.0 would risk the non-breaking guarantee. The
   processor re-implements the checks against the mirror API (`Element`/`TypeMirror`).
   `PatternValidator`'s logic (~15 lines of "try constructing
   `SimpleDateFormat`/`DateTimeFormatter`") is deliberately duplicated. Conscious DRY trade-off;
   note the pairing in Javadoc of the processor check class so the two stay in sync.
2. **Release 11, record-safe.** The processor compiles at `<release>11</release>` like core
   (consumers build on JDK 11). Record detection uses name-based checks
   (`element.getKind().name().equals("RECORD")` / superclass `java.lang.Record` FQN) — the
   mirror-API analog of the reflective trick core already uses in `JavaRecordSupport`.
3. **No formatter access.** Runtime `doValidateFieldPattern` validates the pattern regardless of
   a custom `formatter=`; parity means the processor never needs to read the Class-valued
   `formatter()` member (which would throw `MirroredTypeException`). Read `@Field`/`@Fields`
   directly via `element.getAnnotation(...)` — all needed members are primitives/Strings.
4. **No new dependencies.** Processor jar depends only on `fixedformat4j` (for the annotation
   classes). Registration via `META-INF/services/javax.annotation.processing.Processor` — no
   auto-service. Tests use the JDK's `javax.tools.ToolProvider.getSystemJavaCompiler()` +
   `DiagnosticCollector`, no compile-testing library.
5. **Processor contract**: extends `AbstractProcessor`, supports annotation
   `com.ancientprogramming.fixedformat4j.annotation.Record`, overrides
   `getSupportedSourceVersion()` → `SourceVersion.latestSupported()` (avoids warnings on newer
   JDKs), `process()` returns `false` (claims nothing), emits via `Messager` with the offending
   `Element` so the IDE/javac points at the right line.

## Checks (all compile ERRORs)

Runtime-parity — mirror `FieldValidator`
(`fixedformat4j/src/main/java/com/ancientprogramming/fixedformat4j/format/impl/FieldValidator.java`):

| # | Check | Runtime source |
|---|-------|----------------|
| 1 | Invalid date/time pattern (`Date`→`SimpleDateFormat`, `LocalDate`/`LocalDateTime`→`DateTimeFormatter`; defaults from `FixedFormatPatternData` when `@FixedFormatPattern` absent) | `doValidateFieldPattern` + `PatternValidator` |
| 2 | Enum max value length > `@Field(length)` (LITERAL = longest constant name; NUMERIC = digits of `constants-1`; honors `@FixedFormatEnum`) — enum constants via enclosed `ENUM_CONSTANT` elements | `doValidateEnumFieldLength` |
| 3 | `nullChar` on primitive-typed field (for `count > 1`, resolve element type of array/`List<T>` like `RepeatingFieldSupport.resolveElementType`) | `doValidateFieldNullChar` |
| 4 | `nullValue` rules: mutually exclusive with `nullChar`; `nullValue.length() == length`; not on primitives | `doValidateNullValue` |
| 5 | `REST_OF_LINE` (`length = -1`) rules: String-only; `count == 1`; no `align`/`paddingChar`/`nullChar`/`nullValue`; at most one per record; must be last by offset; incompatible with `@Record(length != -1)` | `doValidateRestOfLineField` / `...IsLastField` / `...RecordLength` |

New static checks (no runtime counterpart — ERROR by maintainer decision; documented as
stricter-than-runtime):

| # | Check |
|---|-------|
| 6 | Field end (`offset + count*length - 1`) exceeds `@Record(length)` when record length != -1 |
| 7 | Duplicate offsets / overlapping field ranges within a record |

Element scanning: on each `@Record` type, visit methods that are conventional getters
(`get*`/`is*`) or record accessors, carrying `@Field`/`@Fields` (annotations on record
components propagate to accessors since `@Field` targets `METHOD, FIELD`). Datatype = return
type `TypeMirror`.

## Module layout

```
fixedformat4j-processor/
  pom.xml                          (parent: root pom; dep: fixedformat4j; junit-jupiter test)
  src/main/java/com/ancientprogramming/fixedformat4j/processor/
    FixedFormatProcessor.java      (AbstractProcessor — orchestration + Messager)
    RecordValidator.java           (per-@Record-type checks: rest-of-line ordering, overflow, duplicates)
    FieldChecker.java              (per-field checks 1–5)
  src/main/resources/META-INF/services/javax.annotation.processing.Processor
  src/test/java/.../processor/
    CompilationTestSupport.java    (in-memory javac harness: source string → diagnostics)
    *Test.java                     (one test class per check area)
```

Parent `pom.xml`: add `<module>fixedformat4j-processor</module>`. Replicate the `release`
profile (javadoc/sources/gpg) from `fixedformat4j/pom.xml` in the processor pom so the
artifact is publishable.

## Steps (each = one commit; RED → GREEN; mutation testing runs in the CI pipeline, not locally)

### Step 1: Module skeleton + processor registration
**RED**: Test compiling a valid minimal `@Record` source with the processor on the processor
path produces no diagnostics (proves the processor loads, runs, and stays silent on valid input).
**GREEN**: New module + empty-pass `FixedFormatProcessor` + service registration + test harness
(`CompilationTestSupport` using `ToolProvider.getSystemJavaCompiler()`, current classpath passed
through so `fixedformat4j` annotations resolve).
**Done when**: `mvn -pl fixedformat4j-processor test` green on JDK 11.

### Step 2: Invalid pattern check (check 1)
**RED**: Source with `@FixedFormatPattern("not-a-date-pattern")` on a `Date` getter → ERROR
diagnostic containing the pattern and the element; valid pattern → clean. Cases: `Date`,
`LocalDate`, `LocalDateTime`, non-date type ignored, default patterns when annotation absent.
**GREEN**: Pattern validation in `FieldChecker` (duplicated try-construct logic).

### Step 3: Enum length check (check 2)
**RED**: Enum with a constant name longer than `@Field(length)` → ERROR; `@FixedFormatEnum(NUMERIC)`
variant uses digit width; fits → clean; `REST_OF_LINE` skipped.
**GREEN**: Enum constant scan via enclosed elements.

### Step 4: nullChar / nullValue checks (checks 3–4)
**RED**: `nullChar` on `int` getter → ERROR; on `Integer` → clean; `count > 1` array/`List`
element-type resolution; `nullValue` + `nullChar` together → ERROR; `nullValue` length mismatch → ERROR.
**GREEN**: Primitive/type-kind checks in `FieldChecker`.

### Step 5: REST_OF_LINE rules (check 5)
**RED**: Each rule violated → ERROR (non-String, count>1, align/padding/nullChar/nullValue set,
two rest-of-line fields, not last by offset, fixed `@Record(length)`); valid trailing rest-of-line → clean.
**GREEN**: `RecordValidator` cross-field pass.

### Step 6: Record-length overflow + duplicate offsets (checks 6–7)
**RED**: Field end past `@Record(length)` → ERROR (the issue's headline example); two fields at
the same offset / overlapping ranges → ERROR; `count`-expanded ranges included.
**GREEN**: Range arithmetic in `RecordValidator`.

### Step 7: Java record sources
**RED**: `@Field` on record components of a Java `record` — same checks fire; valid record → clean.
Tests gated `@EnabledForJreRange(min = JRE.JAVA_17)` (record sources are strings, so no separate
source dir needed; JDK 11 CI skips them, the JDK 17 nightly job runs them).
**GREEN**: Record-accessor scanning (name-based kind check, release-11-safe).

### Step 8: CI + release wiring
- `.github/workflows/maven-publish.yml`: deploy `-pl fixedformat4j` → `-pl fixedformat4j,fixedformat4j-processor`.
- `.github/workflows/pit-report.yml`: extend `-pl` to include the processor module + add
  pitest plugin config to the processor pom (CI-only; if PIT misbehaves against
  compiler-invoking tests, document and defer — do not run PIT locally).
- Verify `nightly-build.yml` root build picks up the module (it builds the reactor — should be automatic).

### Step 9: Documentation
- **New** `docs/usage/compile-time-validation.md` — how to enable:
  - Maven (recommended): `maven-compiler-plugin` → `annotationProcessorPaths` with
    `fixedformat4j-processor`; alternative: `provided`-scope dependency.
  - Gradle: `annotationProcessor 'com.ancientprogramming.fixedformat4j:fixedformat4j-processor:1.9.0'`
    (Groovy + Kotlin DSL).
  - Table of checks with example diagnostics.
  - Explicit note: checks 6–7 are stricter than the 1.8.x runtime — enabling the processor may
    turn latent config bugs into compile errors; runtime behavior is unchanged either way.
  - Perf note: compile-time only, zero runtime cost, nothing added to the runtime classpath.
- `docs/usage/index.md`: link the new page. `docs/get-it.md`: add the processor artifact section.
- `docs/changelog.md` `[Unreleased]`: feature entry incl. the compile-time-only perf note.
- `README.md`: one-paragraph mention. `CLAUDE.md`: add module to Project Structure.

## Pre-PR quality gate

1. `JAVA_HOME=$(/usr/libexec/java_home -v 11) mvn install` from repo root (full reactor, JDK 11).
2. Repeat `mvn test` on JDK 17 to exercise the record-gated tests.
3. Mutation testing: CI `pit-report` pipeline (never locally).
4. Refactoring assessment after each green.

## Verification (end-to-end)

1. Add a deliberately broken `@Record` class to `samples/` **temporarily**, wire
   `annotationProcessorPaths` in `samples/pom.xml`, run `mvn compile -pl samples` and observe
   the ERROR diagnostics match the issue's example format; revert the broken class (decide with
   reviewer whether the samples wiring itself stays as living documentation).
2. Confirm a consumer build **without** the processor compiles the same broken class cleanly
   (proves opt-in / non-breaking).
3. Full reactor build on JDK 11 and 17.

## PR strategy

Plan PR first (this document), then implementation in two small PRs:
- **PR 1**: Steps 1–5 (module + runtime-parity checks) + changelog entry.
- **PR 2**: Steps 6–9 (new static checks, CI/release wiring, docs).

PR descriptions state the version classification (**minor, non-breaking — new optional
artifact, core untouched**) and why the framework-breaking checklist is N/A.

## SOLID notes

- **SRP**: processor scans and reports; it never participates in runtime load/export.
  `FieldChecker` (per-field) vs `RecordValidator` (cross-field) split keeps reasons-to-change apart.
- **OCP**: core is closed for modification here; the processor is a pure extension artifact.
- **LSP/ISP/DIP**: no existing abstractions widened; no core types touched.
- **Conscious trade-off**: validation logic duplicated between `FieldValidator` (reflection) and
  the processor (mirror API) — accepted to avoid restructuring the core artifact; cross-referenced
  in Javadoc so future check changes update both.

---
*Delete this file when the plan is complete.*
