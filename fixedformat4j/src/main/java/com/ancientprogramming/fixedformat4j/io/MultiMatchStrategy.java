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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy that decides which mappings to use when more than one
 * {@link ClassPatternMapping} matches the same line.
 *
 * <p>Three built-in strategies are provided as static factory methods:
 * {@link #firstMatch()}, {@link #throwOnAmbiguity()}, and {@link #allMatches()}.</p>
 *
 * <p>Implement this interface to define custom multi-match logic — for example, choosing
 * the mapping whose {@code @Record} length most closely matches the actual line length:</p>
 * <pre>{@code
 * MultiMatchStrategy bestFit = new MultiMatchStrategy() {
 *     public <T> List<ClassPatternMapping<? extends T>> resolve(
 *             List<ClassPatternMapping<? extends T>> matched, long lineNumber) {
 *         return matched.stream()
 *             .filter(m -> m.getRecordClass().getAnnotation(Record.class).length() == line.length())
 *             .collect(Collectors.toList());
 *     }
 * };
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public interface MultiMatchStrategy {

  /**
   * Resolves which mappings should be used to parse a line that matched more than one pattern.
   *
   * @param matched    all mappings whose pattern matched the line; never empty
   * @param lineNumber the 1-based line number
   * @param <T>        the common record supertype
   * @return the subset of {@code matched} to use for parsing; may be empty to skip the line;
   *         never {@code null}
   */
  <T> List<ClassPatternMapping<? extends T>> resolve(
      List<ClassPatternMapping<? extends T>> matched, long lineNumber);

  /**
   * Returns a strategy that uses the first matching mapping in registration order and ignores
   * the rest. This is the default strategy used by {@link FixedFormatReader}.
   *
   * @return a first-match strategy; never {@code null}
   */
  static MultiMatchStrategy firstMatch() {
    return new MultiMatchStrategy() {
      @Override
      public <T> List<ClassPatternMapping<? extends T>> resolve(
          List<ClassPatternMapping<? extends T>> matched, long lineNumber) {
        return matched.isEmpty() ? matched : matched.subList(0, 1);
      }
    };
  }

  /**
   * Returns a strategy that throws {@link FixedFormatException} if more than one pattern
   * matches, including the line number and all matching class names in the exception message.
   *
   * @return a throw-on-ambiguity strategy; never {@code null}
   */
  static MultiMatchStrategy throwOnAmbiguity() {
    return new MultiMatchStrategy() {
      @Override
      public <T> List<ClassPatternMapping<? extends T>> resolve(
          List<ClassPatternMapping<? extends T>> matched, long lineNumber) {
        if (matched.size() > 1) {
          String classes = matched.stream()
              .map(m -> m.getRecordClass().getSimpleName())
              .collect(Collectors.joining(", "));
          throw new FixedFormatException(
              "Line " + lineNumber + " matched multiple patterns: " + classes);
        }
        return matched;
      }
    };
  }

  /**
   * Returns a strategy that loads the line once per matching mapping and emits one record
   * object per match, in registration order.
   *
   * @return an all-matches strategy; never {@code null}
   */
  static MultiMatchStrategy allMatches() {
    return new MultiMatchStrategy() {
      @Override
      public <T> List<ClassPatternMapping<? extends T>> resolve(
          List<ClassPatternMapping<? extends T>> matched, long lineNumber) {
        return matched;
      }
    };
  }
}
