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
import com.ancientprogramming.fixedformat4j.format.ParseException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator that times {@link #load} and {@link #export} per record class, counts
 * {@link ParseException}s with record-class and field tags, and gauges the number of distinct
 * record classes processed through this manager. Behavior is delegated unchanged — including
 * exceptions, which are rethrown after counting.
 *
 * <p>The gauge counts classes observed by <em>this</em> instrumented manager: the global
 * {@code ClassValue}-based metadata cache is deliberately not enumerable (classloader-leak
 * safety), so per-manager observation is the measurable equivalent.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class MeteredFixedFormatManager implements FixedFormatManager {

  private static final String RECORD_CLASS_TAG = "record.class";

  private final FixedFormatManager delegate;
  private final MeterRegistry registry;
  private final Set<Class<?>> seenClasses = ConcurrentHashMap.newKeySet();

  MeteredFixedFormatManager(FixedFormatManager delegate, MeterRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
    Gauge.builder("fixedformat.metadata.cache.classes", seenClasses, Set::size)
        .description("Distinct @Record classes processed through this instrumented manager")
        .register(registry);
  }

  @Override
  public <T> T load(Class<T> clazz, String data) {
    seenClasses.add(clazz);
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.load(clazz, data);
    } catch (ParseException e) {
      countParseError(e);
      throw e;
    } finally {
      sample.stop(registry.timer("fixedformat.load", RECORD_CLASS_TAG, clazz.getName()));
    }
  }

  @Override
  public <T> String export(T instance) {
    seenClasses.add(instance.getClass());
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.export(instance);
    } finally {
      sample.stop(registry.timer("fixedformat.export", RECORD_CLASS_TAG, instance.getClass().getName()));
    }
  }

  @Override
  public <T> String export(String template, T instance) {
    seenClasses.add(instance.getClass());
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.export(template, instance);
    } finally {
      sample.stop(registry.timer("fixedformat.export", RECORD_CLASS_TAG, instance.getClass().getName()));
    }
  }

  private void countParseError(ParseException e) {
    String recordClass = e.getAnnotatedClass() != null ? e.getAnnotatedClass().getName() : "unknown";
    String field = e.getAnnotatedMethod() != null ? e.getAnnotatedMethod().getName() : "unknown";
    registry.counter("fixedformat.parse.errors", RECORD_CLASS_TAG, recordClass, "field", field)
        .increment();
  }
}
