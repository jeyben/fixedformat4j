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

import java.util.Objects;

import static java.lang.String.format;

/**
 * Declarative line discriminator used to route a line to a {@code @Record}-annotated class.
 *
 * <p>A {@code LinePattern} matches a line when, for each declared position {@code p[i]},
 * the line's character at offset {@code p[i]} equals {@code literal.charAt(i)}.
 * The closed shape (positions + literal) lets the reader index mappings into hash buckets at
 * build time, so per-line routing is near O(1) regardless of how many record types are registered.</p>
 *
 * <p>Obtain instances via {@link #positional(int[], String)}, {@link #prefix(String)}, or
 * {@link #matchAll()}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public final class LinePattern {

  private final int[] positions;
  private final String literal;

  private LinePattern(int[] positions, String literal) {
    this.positions = positions;
    this.literal = literal;
  }

  /**
   * Returns a pattern that matches a line when each {@code positions[i]} carries
   * {@code literal.charAt(i)}.
   *
   * @param positions strictly ascending, non-negative line offsets; not null; length must equal
   *                  {@code literal.length()} and must be &gt; 0
   * @param literal   the characters expected at the corresponding offsets; not null
   * @return a positional pattern; never {@code null}
   * @throws NullPointerException     if {@code positions} or {@code literal} is null
   * @throws IllegalArgumentException if positions is empty, contains a negative or duplicate
   *                                  offset, is not strictly ascending, or has a different
   *                                  length than {@code literal}
   */
  public static LinePattern positional(int[] positions, String literal) {
    Objects.requireNonNull(positions, "positions must not be null");
    Objects.requireNonNull(literal, "literal must not be null");
    if (positions.length == 0) {
      throw new IllegalArgumentException("positions must not be empty; use LinePattern.matchAll() to match every line");
    }
    if (positions.length != literal.length()) {
      throw new IllegalArgumentException(format(
          "positions length %d does not equal literal length %d", positions.length, literal.length()));
    }
    if (positions[0] < 0) {
      throw new IllegalArgumentException(format("positions must be non-negative; got %d", positions[0]));
    }
    for (int i = 1; i < positions.length; i++) {
      if (positions[i] <= positions[i - 1]) {
        throw new IllegalArgumentException(format(
            "positions must be strictly ascending; got %d after %d at index %d",
            positions[i], positions[i - 1], i));
      }
    }
    return new LinePattern(positions.clone(), literal);
  }

  /**
   * Returns a pattern that matches a line whose first {@code literal.length()} characters
   * equal {@code literal}. Equivalent to
   * {@code positional(new int[]{0, 1, ..., literal.length() - 1}, literal)}.
   *
   * @param literal the prefix to match; not null and not empty
   * @return a prefix pattern; never {@code null}
   * @throws NullPointerException     if {@code literal} is null
   * @throws IllegalArgumentException if {@code literal} is empty (use {@link #matchAll()} instead)
   */
  public static LinePattern prefix(String literal) {
    Objects.requireNonNull(literal, "literal must not be null");
    if (literal.isEmpty()) {
      throw new IllegalArgumentException("literal must not be empty; use LinePattern.matchAll() to match every line");
    }
    int[] positions = new int[literal.length()];
    for (int i = 0; i < positions.length; i++) {
      positions[i] = i;
    }
    return new LinePattern(positions, literal);
  }

  /**
   * Returns a pattern that matches every line. Use this when registering a single record type
   * for a homogeneous file, or as the catch-all in a heterogeneous file.
   *
   * @return a match-all pattern; never {@code null}
   */
  public static LinePattern matchAll() {
    return new LinePattern(new int[0], null);
  }

  int[] positions() {
    return positions;
  }

  String literal() {
    return literal;
  }

  int depth() {
    return positions.length;
  }
}
