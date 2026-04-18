---
layout: default
title: Benchmarks
---

# Benchmarks

These benchmarks are produced by [JMH](https://github.com/openjdk/jmh) and compare successive releases of fixedformat4j running on the same hardware in the same CI run.

**Important:** Absolute numbers depend on the runner hardware and should not be compared across separate workflow runs. Only the relative differences between versions within a single run are meaningful. See the `.meta.json` sidecar files in [`docs/assets/benchmarks/`](https://github.com/jeyben/fixedformat4j/tree/master/docs/assets/benchmarks/) for the git SHA and timestamp of each result set.

Three fixture record sizes are benchmarked:

- **Small** — 5 fields (String, Integer, BigDecimal, Date, Boolean), 54 chars total
- **Wide** — 20 fields (10 Strings, 5 Integers, 3 BigDecimals, 2 Booleans), 188 chars total
- **Nested** — outer record containing an inner `@Record`-annotated field, 21 chars total

## Throughput (ops/µs — higher is better)

<canvas id="chart-load-thrpt"></canvas>
<canvas id="chart-export-thrpt"></canvas>

## Average time (µs/op — lower is better)

<canvas id="chart-load-avgt"></canvas>
<canvas id="chart-export-avgt"></canvas>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<script src="{{ '/assets/js/benchmarks.js' | relative_url }}"></script>
