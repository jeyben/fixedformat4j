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
package com.ancientprogramming.fixedformat4j.io.segment;

import java.util.Objects;

/**
 * A {@link Segment} whose text matched a registered
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record} class and was successfully
 * parsed into a typed record instance.
 *
 * <p>The record is mutable: callers may update fields directly via the record's own setters,
 * or replace the entire record object with {@link #setRecord(Object)}. Both approaches are
 * reflected when the segment list is written back via
 * {@link com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter}.</p>
 *
 * <pre>{@code
 * segments.stream()
 *     .filter(s -> s instanceof ParsedSegment && ((ParsedSegment<?>) s).isOf(DetailRecord.class))
 *     .map(s -> (ParsedSegment<DetailRecord>) s)
 *     .forEach(ps -> ps.getRecord().setAmount(ps.getRecord().getAmount() * 2));
 * }</pre>
 *
 * @param <T> the {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated type
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public final class ParsedSegment<T> implements Segment {

  private final Class<T> type;
  private T record;

  /**
   * Creates a new {@code ParsedSegment} holding the given record.
   *
   * @param type   the exact runtime class of the record; must not be {@code null}
   * @param record the parsed record instance; must not be {@code null}
   */
  public ParsedSegment(Class<T> type, T record) {
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.record = Objects.requireNonNull(record, "record must not be null");
  }

  /**
   * Returns the exact runtime class of the record stored in this segment.
   *
   * @return the record's class; never {@code null}
   */
  public Class<T> getType() {
    return type;
  }

  /**
   * Returns the parsed record instance.
   *
   * @return the record; never {@code null}
   */
  public T getRecord() {
    return record;
  }

  /**
   * Replaces the record with {@code newRecord}.
   *
   * @param newRecord the replacement; must not be {@code null}
   * @throws NullPointerException if {@code newRecord} is {@code null}
   */
  public void setRecord(T newRecord) {
    this.record = Objects.requireNonNull(newRecord, "record must not be null");
  }

  /**
   * Returns {@code true} if this segment holds a record of exactly the given class.
   *
   * @param clazz the class to check against
   * @return {@code true} when {@code getType() == clazz}
   */
  public boolean isOf(Class<?> clazz) {
    return type == clazz;
  }
}
