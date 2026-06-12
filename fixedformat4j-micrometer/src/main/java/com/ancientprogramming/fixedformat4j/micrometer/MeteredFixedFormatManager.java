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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decorator that times {@link #load} and {@link #export} per record class, counts
 * {@link ParseException}s with record-class and field tags, and gauges the number of distinct
 * record classes processed through this manager. Behavior is delegated unchanged — including
 * exceptions, which are rethrown after counting.
 *
 * <p>The gauge counts classes observed by <em>this</em> instrumented manager: the global
 * {@code ClassValue}-based metadata cache is deliberately not enumerable (classloader-leak
 * safety), so per-manager observation is the measurable equivalent. The tracking set holds
 * the classes <em>weakly</em> for the same reason — a long-lived manager must never pin a
 * record class's defining classloader, so classes that become unreachable (e.g. after a
 * hot-reload) drop out of the gauge. Each manager tags its gauge with a unique
 * {@code manager.instance} value; Micrometer dedupes meters by name+tags, so an untagged
 * gauge would be silently dropped for every manager but the first on a shared registry.
 *
 * <p>Timers are resolved once per record class through {@link ClassValue} caches — never
 * through a {@code Map<Class, Timer>}, which would strongly pin record classes and reintroduce
 * the classloader leak described above. The registry-side meter retains only the class
 * <em>name</em> string, which pins nothing. Since Micrometer dedupes by name+tags, two managers
 * on one registry still share the same underlying timer per record class.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class MeteredFixedFormatManager implements FixedFormatManager {

  private static final String RECORD_CLASS_TAG = "record.class";
  private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

  private final FixedFormatManager delegate;
  private final MeterRegistry registry;
  private final Set<Class<?>> seenClasses =
      Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

  private final ClassValue<Timer> loadTimers = new ClassValue<>() {
    @Override
    protected Timer computeValue(Class<?> clazz) {
      return registry.timer("fixedformat.load", RECORD_CLASS_TAG, clazz.getName());
    }
  };

  private final ClassValue<Timer> exportTimers = new ClassValue<>() {
    @Override
    protected Timer computeValue(Class<?> clazz) {
      return registry.timer("fixedformat.export", RECORD_CLASS_TAG, clazz.getName());
    }
  };

  MeteredFixedFormatManager(FixedFormatManager delegate, MeterRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
    Gauge.builder("fixedformat.metadata.cache.classes", seenClasses, Set::size)
        .description("Distinct @Record classes processed through this instrumented manager")
        .tag("manager.instance", String.valueOf(INSTANCE_COUNTER.incrementAndGet()))
        .register(registry);
  }

  @Override
  public <T> T load(Class<T> clazz, String data) {
    Timer timer = loadTimers.get(clazz);
    seenClasses.add(clazz);
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.load(clazz, data);
    } catch (ParseException e) {
      countParseError(e);
      throw e;
    } finally {
      sample.stop(timer);
    }
  }

  @Override
  public <T> String export(T instance) {
    Timer timer = exportTimers.get(instance.getClass());
    seenClasses.add(instance.getClass());
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.export(instance);
    } finally {
      sample.stop(timer);
    }
  }

  @Override
  public <T> String export(String template, T instance) {
    Timer timer = exportTimers.get(instance.getClass());
    seenClasses.add(instance.getClass());
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.export(template, instance);
    } finally {
      sample.stop(timer);
    }
  }

  private void countParseError(ParseException e) {
    String recordClass = e.getAnnotatedClass() != null ? e.getAnnotatedClass().getName() : "unknown";
    String field = e.getAnnotatedMethod() != null ? e.getAnnotatedMethod().getName() : "unknown";
    registry.counter("fixedformat.parse.errors", RECORD_CLASS_TAG, recordClass, "field", field)
        .increment();
  }
}
