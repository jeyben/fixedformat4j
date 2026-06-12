/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.micrometer;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Entry point for Micrometer instrumentation of fixedformat4j.
 *
 * <p>All instrumentation is decorator-based: the core library is not modified and carries no
 * Micrometer dependency. Wrap a {@link FixedFormatManager} via {@link #instrument} and, for
 * file processing, pass the instrumented manager to
 * {@code FixedFormatReader.builder().manager(...)} so reader-driven loads are timed too.
 *
 * <p>Instrumentation cost: meters are resolved through Micrometer's registry lookup per call
 * (nanosecond-scale); uninstrumented managers and readers are completely unaffected.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public final class FixedFormatMetrics {

  private final MeterRegistry registry;

  private FixedFormatMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  /**
   * Creates an instrumentation factory publishing to the given registry.
   *
   * @param registry the Micrometer registry to publish meters to; must not be {@code null}
   * @return a new {@code FixedFormatMetrics}; never {@code null}
   */
  public static FixedFormatMetrics of(MeterRegistry registry) {
    Objects.requireNonNull(registry, "registry must not be null");
    return new FixedFormatMetrics(registry);
  }

  /**
   * Wraps {@code delegate} so every {@code load()} and {@code export()} call is timed.
   *
   * <p>Published meters:
   * <ul>
   *   <li>{@code fixedformat.load} timer, tag {@code record.class}</li>
   *   <li>{@code fixedformat.export} timer, tag {@code record.class}</li>
   *   <li>{@code fixedformat.parse.errors} counter, tags {@code record.class} + {@code field}
   *       (exception still propagates after counting)</li>
   *   <li>{@code fixedformat.metadata.cache.classes} gauge, tag {@code manager.instance}</li>
   * </ul>
   *
   * <p><b>Note:</b> the returned manager implements only {@link FixedFormatManager}. If the
   * delegate also implements
   * {@code com.ancientprogramming.fixedformat4j.format.FixedFormatIntrospector}, keep a
   * reference to the original {@code delegate} when introspection is needed; casting the
   * returned wrapper will throw {@link ClassCastException}.
   *
   * @param delegate the manager to instrument; must not be {@code null}
   * @return a {@link FixedFormatManager} with identical behavior that publishes meters; never {@code null}
   */
  public FixedFormatManager instrument(FixedFormatManager delegate) {
    Objects.requireNonNull(delegate, "delegate must not be null");
    return new MeteredFixedFormatManager(delegate, registry);
  }

  /**
   * Wraps an {@link UnmatchStrategy} so every unmatched line increments
   * {@code fixedformat.reader.lines.unmatched} before the wrapped strategy runs.
   * Pass the result to {@code FixedFormatReaderBuilder.unmatchStrategy(...)}.
   *
   * @param delegate the strategy to wrap; must not be {@code null}
   * @return a counting strategy with identical behavior; never {@code null}
   */
  public UnmatchStrategy countUnmatched(UnmatchStrategy delegate) {
    Objects.requireNonNull(delegate, "delegate must not be null");
    return (lineNumber, line) -> {
      registry.counter("fixedformat.reader.lines.unmatched").increment();
      delegate.handle(lineNumber, line);
    };
  }

  /**
   * Wraps a {@link ParseErrorStrategy} so every failed line increments
   * {@code fixedformat.reader.lines.errors} before the wrapped strategy runs.
   * Pass the result to {@code FixedFormatReaderBuilder.parseErrorStrategy(...)}.
   *
   * <p><b>Double-count note:</b> when the reader's manager is also instrumented via
   * {@link #instrument}, a parse failure will increment <em>both</em> this counter
   * ({@code fixedformat.reader.lines.errors}) <em>and</em>
   * {@code fixedformat.parse.errors} (with {@code record.class} + {@code field} tags).
   * This is intentional: the two meters serve different purposes — coarse line-level
   * counting vs. granular field-level diagnosis.
   *
   * @param delegate the strategy to wrap; must not be {@code null}
   * @return a counting strategy with identical behavior; never {@code null}
   */
  public ParseErrorStrategy countParseErrors(ParseErrorStrategy delegate) {
    Objects.requireNonNull(delegate, "delegate must not be null");
    return (wrapped, line, lineNumber) -> {
      registry.counter("fixedformat.reader.lines.errors").increment();
      delegate.handle(wrapped, line, lineNumber);
    };
  }

  /**
   * Wraps an exclude-lines predicate so every line read increments
   * {@code fixedformat.reader.lines.processed} — the predicate is the first thing the reader
   * evaluates for each line, which makes it the per-line seam. Pass the result to
   * {@code FixedFormatReaderBuilder.excludeLines(...)}; when no lines should be excluded, pass
   * {@code line -> false} as the delegate.
   *
   * @param excludeDelegate the exclusion predicate to wrap; must not be {@code null}
   * @return a counting predicate with identical behavior; never {@code null}
   */
  public Predicate<String> countLines(Predicate<String> excludeDelegate) {
    Objects.requireNonNull(excludeDelegate, "excludeDelegate must not be null");
    return line -> {
      registry.counter("fixedformat.reader.lines.processed").increment();
      return excludeDelegate.test(line);
    };
  }
}
