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
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

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
   * </ul>
   *
   * @param delegate the manager to instrument; must not be {@code null}
   * @return a {@link FixedFormatManager} with identical behavior that publishes meters; never {@code null}
   */
  public FixedFormatManager instrument(FixedFormatManager delegate) {
    Objects.requireNonNull(delegate, "delegate must not be null");
    return new MeteredFixedFormatManager(delegate, registry);
  }
}
