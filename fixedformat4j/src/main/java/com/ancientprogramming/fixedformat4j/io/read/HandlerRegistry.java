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
package com.ancientprogramming.fixedformat4j.io.read;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A per-call registry of typed record handlers for use with
 * {@link FixedFormatReader#process}.
 *
 * <p>Register one handler per record class via the fluent {@link #on} method, then pass
 * the registry to {@link FixedFormatReader#process}. Classes not registered are silently
 * ignored — they are still parsed, but no handler is invoked.</p>
 *
 * <pre>{@code
 * reader.process(Path.of("data.txt"), new HandlerRegistry()
 *     .on(HeaderRecord.class, this::onHeader)
 *     .on(DetailRecord.class, this::onDetail));
 * }</pre>
 *
 * <p>Because {@code HandlerRegistry} instances are created at the call site rather than
 * stored in the reader, the same {@link FixedFormatReader} can safely be used from multiple
 * threads with independent registries.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public final class HandlerRegistry {

  private final Map<Class<?>, Consumer<?>> handlers = new LinkedHashMap<>();

  /**
   * Registers a typed handler for {@code clazz}.
   *
   * @param <R>     the record type
   * @param clazz   the record class; must not be {@code null}
   * @param handler invoked with each parsed record of type {@code R}; must not be {@code null}
   * @return this registry (for chaining)
   */
  public <R> HandlerRegistry on(Class<R> clazz, Consumer<R> handler) {
    handlers.put(Objects.requireNonNull(clazz, "clazz must not be null"),
                 Objects.requireNonNull(handler, "handler must not be null"));
    return this;
  }

  /**
   * Dispatches {@code record} to the handler registered for {@code clazz}, if any.
   * Package-private: only {@link FixedFormatReader} calls this.
   *
   * <p>The unchecked cast is safe by construction: {@link #on} always aligns the class key
   * with the consumer's type parameter.</p>
   */
  @SuppressWarnings("unchecked")
  void dispatch(Class<?> clazz, Object record) {
    Consumer<Object> handler = (Consumer<Object>) handlers.get(clazz);
    if (handler != null) {
      handler.accept(record);
    }
  }
}
