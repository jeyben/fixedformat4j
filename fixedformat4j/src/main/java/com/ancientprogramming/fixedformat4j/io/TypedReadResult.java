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
package com.ancientprogramming.fixedformat4j.io;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The result of a {@link FixedFormatReader#readAsTypedResult} call: an immutable, class-keyed
 * container of parsed records that provides type-safe retrieval without casts at the call site.
 *
 * <p>The type safety relies on the invariant that every record stored under key {@code K}
 * is an instance of {@code K}. This invariant is maintained automatically when the result
 * is produced by {@link FixedFormatReader#readAsTypedResult}. Callers who construct a
 * {@code TypedReadResult} directly are responsible for upholding it; violations will not
 * be detected at construction time and will manifest as {@link ClassCastException} at the
 * call site of {@link #get}.</p>
 *
 * <pre>{@code
 * TypedReadResult result = reader.readAsTypedResult(path);
 * List<HeaderRecord> headers = result.get(HeaderRecord.class); // no cast
 * List<DetailRecord> details = result.get(DetailRecord.class); // no cast
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public final class TypedReadResult {

  private final Map<Class<?>, List<Object>> data;
  private final List<Object> all;

  public TypedReadResult(Map<Class<?>, List<Object>> data, List<Object> all) {
    this.data = Collections.unmodifiableMap(data);
    this.all = Collections.unmodifiableList(all);
  }

  /**
   * Returns all records stored under {@code clazz} as a correctly typed list.
   *
   * <p>Safe by invariant: every entry stored under key {@code K} was produced by
   * {@code manager.load(K, line)} and is therefore an instance of {@code K}.</p>
   *
   * @param <R>   the record type
   * @param clazz the class to retrieve records for
   * @return an unmodifiable list of {@code R} instances; empty if no records matched {@code clazz}
   */
  @SuppressWarnings("unchecked")
  public <R> List<R> get(Class<R> clazz) {
    List<Object> records = data.get(clazz);
    if (records == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList((List<R>) records);
  }

  /**
   * Returns all records from all classes in encounter order.
   *
   * @return an unmodifiable flat list of all parsed records; never {@code null}
   */
  public List<Object> getAll() {
    return all;
  }

  /**
   * Returns {@code true} if at least one record was matched for {@code clazz}.
   *
   * @param clazz the class to check
   * @return {@code true} if records exist for {@code clazz}
   */
  public boolean contains(Class<?> clazz) {
    return data.containsKey(clazz);
  }

  /**
   * Returns the set of classes that produced at least one record.
   *
   * @return an unmodifiable set; never {@code null}
   */
  public Set<Class<?>> classes() {
    return data.keySet();
  }
}
