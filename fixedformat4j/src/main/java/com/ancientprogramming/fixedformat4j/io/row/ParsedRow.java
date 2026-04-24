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
package com.ancientprogramming.fixedformat4j.io.row;

import java.util.Objects;

/**
 * A {@link Row} whose line matched a registered
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record} class and was successfully
 * parsed into a typed record instance.
 *
 * <p>The record is mutable: callers may update fields directly via the record's own setters,
 * or replace the entire record object with {@link #setRecord(Object)}. Both approaches are
 * reflected when the row list is written back via {@link com.ancientprogramming.fixedformat4j.io.FixedFormatWriter}.</p>
 *
 * <pre>{@code
 * rows.stream()
 *     .filter(r -> r instanceof ParsedRow && ((ParsedRow<?>) r).isOf(DetailRecord.class))
 *     .map(r -> (ParsedRow<DetailRecord>) r)
 *     .forEach(pr -> pr.getRecord().setAmount(pr.getRecord().getAmount() * 2));
 * }</pre>
 *
 * @param <T> the {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated type
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public final class ParsedRow<T> implements Row {

  private final Class<T> type;
  private T record;

  /**
   * Creates a new {@code ParsedRow} holding the given record.
   *
   * @param type   the exact runtime class of the record; must not be {@code null}
   * @param record the parsed record instance; must not be {@code null}
   */
  public ParsedRow(Class<T> type, T record) {
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.record = Objects.requireNonNull(record, "record must not be null");
  }

  /**
   * Returns the exact runtime class of the record stored in this row.
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
   * Returns {@code true} if this row holds a record of exactly the given class.
   *
   * @param clazz the class to check against
   * @return {@code true} when {@code getType() == clazz}
   */
  public boolean isOf(Class<?> clazz) {
    return type == clazz;
  }
}
