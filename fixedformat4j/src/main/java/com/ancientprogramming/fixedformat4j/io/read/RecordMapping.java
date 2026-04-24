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

import com.ancientprogramming.fixedformat4j.annotation.Record;

/**
 * Immutable pair of a {@link LinePattern} and the
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated class to
 * instantiate when the pattern matches a line.
 *
 * <p>The constructor validates that {@code recordClass} carries the {@code @Record} annotation
 * so that misconfigured mappings fail fast at reader-build time rather than at parse time.</p>
 *
 * @param <T> the type of record produced by this mapping
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public class RecordMapping<T> {

  private final Class<T> recordClass;
  private final LinePattern pattern;

  /**
   * Creates a new mapping.
   *
   * @param recordClass the class to instantiate when {@code pattern} matches; must be
   *                    annotated with {@link com.ancientprogramming.fixedformat4j.annotation.Record}
   * @param pattern     the pattern that decides which lines are parsed as {@code recordClass}
   * @throws IllegalArgumentException if {@code recordClass} is not annotated with {@code @Record}
   */
  public RecordMapping(Class<T> recordClass, LinePattern pattern) {
    if (recordClass == null) {
      throw new IllegalArgumentException("recordClass must not be null");
    }
    if (pattern == null) {
      throw new IllegalArgumentException("pattern must not be null");
    }
    if (recordClass.getAnnotation(Record.class) == null) {
      throw new IllegalArgumentException(
          recordClass.getSimpleName() + " is not annotated with @Record");
    }
    this.recordClass = recordClass;
    this.pattern = pattern;
  }

  /**
   * Returns the {@code @Record}-annotated class associated with this mapping.
   *
   * @return the record class; never {@code null}
   */
  public Class<T> getRecordClass() {
    return recordClass;
  }

  /**
   * Returns the pattern used to decide whether a line should be parsed as
   * {@link #getRecordClass()}.
   *
   * @return the line pattern; never {@code null}
   */
  public LinePattern getPattern() {
    return pattern;
  }
}
