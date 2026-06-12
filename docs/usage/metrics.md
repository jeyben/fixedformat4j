---
title: Metrics
---

# Metrics with Micrometer

Since 1.9.0 the optional artifact `fixedformat4j-micrometer` instruments fixedformat4j with
[Micrometer](https://micrometer.io/), the metrics facade used by Spring Boot Actuator, Quarkus,
Micronaut, and plain Java applications. The core `fixedformat4j` artifact is unchanged and gains
no dependencies — when the module is absent there is zero cost.

## Add the dependency

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j-micrometer</artifactId>
  <version>1.9.0</version>
</dependency>
```

```groovy
implementation 'com.ancientprogramming.fixedformat4j:fixedformat4j-micrometer:1.9.0'
```

## Wiring

All instrumentation is decorator-based via `FixedFormatMetrics`:

```java
MeterRegistry registry = ...;            // any Micrometer registry
FixedFormatMetrics metrics = FixedFormatMetrics.of(registry);

// instrument a manager — load/export timers, parse-error counter, class gauge
FixedFormatManager manager = metrics.instrument(new FixedFormatManagerImpl());

// instrument file processing — inject the manager and wrap the reader strategies
FixedFormatReader reader = FixedFormatReader.builder()
    .manager(manager)
    .addMapping(CustomerRecord.class, LinePattern.prefix("C"))
    .excludeLines(metrics.countLines(line -> false))   // counts every processed line
    .unmatchStrategy(metrics.countUnmatched(UnmatchStrategy.skip()))
    .parseErrorStrategy(metrics.countParseErrors(ParseErrorStrategy.skipAndLog()))
    .build();
```

In Spring Boot, expose the instrumented manager as a bean — Actuator provides the
`MeterRegistry`:

```java
@Bean
public FixedFormatManager fixedFormatManager(MeterRegistry registry) {
  return FixedFormatMetrics.of(registry).instrument(new FixedFormatManagerImpl());
}
```

## Published meters

| Metric | Type | Tags | Meaning |
|---|---|---|---|
| `fixedformat.load` | timer | `record.class` | Duration of each `load()` call |
| `fixedformat.export` | timer | `record.class` | Duration of each `export()` call (both overloads) |
| `fixedformat.parse.errors` | counter | `record.class`, `field` | `ParseException` occurrences (the exception still propagates) |
| `fixedformat.reader.lines.processed` | counter | — | Lines read, including excluded and unmatched ones |
| `fixedformat.reader.lines.unmatched` | counter | — | Lines matching no registered `LinePattern` |
| `fixedformat.reader.lines.errors` | counter | — | Lines that matched but failed to parse |
| `fixedformat.metadata.cache.classes` | gauge | — | Distinct `@Record` classes processed through the instrumented manager |

A spiking `fixedformat.parse.errors` rate is a leading indicator of upstream format drift —
exactly the class of problem fixed-width integrations otherwise suffer silently.

**Gauge semantics:** the library's internal metadata cache is `ClassValue`-based and deliberately
not enumerable (it must never pin classloaders in hot-reload environments), so the gauge counts
the distinct record classes observed by the instrumented manager instance — the measurable
equivalent of "classes currently cached" for the normal one-manager setup.

## Performance

Instrumentation cost is a Micrometer registry lookup plus a timer sample per call — nanosecond
scale, and only on instrumented managers/readers. Uninstrumented code paths and applications
without this module are completely unaffected.
