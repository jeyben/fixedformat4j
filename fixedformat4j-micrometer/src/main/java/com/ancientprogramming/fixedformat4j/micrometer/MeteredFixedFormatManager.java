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
import io.micrometer.core.instrument.Timer;

/**
 * Decorator that times {@link #load} and {@link #export} per record class. Behavior is
 * delegated unchanged — including exceptions.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class MeteredFixedFormatManager implements FixedFormatManager {

  private static final String RECORD_CLASS_TAG = "record.class";

  private final FixedFormatManager delegate;
  private final MeterRegistry registry;

  MeteredFixedFormatManager(FixedFormatManager delegate, MeterRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
  }

  @Override
  public <T> T load(Class<T> clazz, String data) {
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.load(clazz, data);
    } finally {
      sample.stop(registry.timer("fixedformat.load", RECORD_CLASS_TAG, clazz.getName()));
    }
  }

  @Override
  public <T> String export(T instance) {
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.export(instance);
    } finally {
      sample.stop(registry.timer("fixedformat.export", RECORD_CLASS_TAG, instance.getClass().getName()));
    }
  }

  @Override
  public <T> String export(String template, T instance) {
    Timer.Sample sample = Timer.start(registry);
    try {
      return delegate.export(template, instance);
    } finally {
      sample.stop(registry.timer("fixedformat.export", RECORD_CLASS_TAG, instance.getClass().getName()));
    }
  }
}
