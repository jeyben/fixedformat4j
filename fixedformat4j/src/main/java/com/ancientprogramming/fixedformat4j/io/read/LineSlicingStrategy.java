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
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Per-line slicing strategy used by {@link FixedFormatReader} and {@link FixedFormatSegmentReader}
 * to decide whether a physical line should be treated as a single record or sliced into fixed-width
 * chunks.
 *
 * <p>Three built-in strategies are provided as static factory methods:</p>
 * <ul>
 *   <li>{@link #singleRecord()} — each line is one record (default)</li>
 *   <li>{@link #packed(int)} — each line is sliced into chunks of exactly {@code recordWidth}
 *       characters</li>
 *   <li>{@link #mixed(Predicate, int)} — lines matching the predicate are sliced; all others are
 *       treated as single records</li>
 * </ul>
 *
 * <p>Because this is a {@link FunctionalInterface}, a custom strategy can be provided as a lambda
 * directly.</p>
 *
 * <p>Quick start — mixed file (header + packed details + trailer):</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .lineSlicing(LineSlicingStrategy.mixed(line -> line.startsWith("DTL"), 128))
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .addMapping(TrailerRecord.class, new RegexFixedFormatMatchPattern("^TRL"))
 *     .build();
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 * @see FixedFormatReader
 * @see FixedFormatSegmentReader
 */
@FunctionalInterface
public interface LineSlicingStrategy {

  /**
   * Returns the record width to use for slicing this line, or {@link OptionalInt#empty()} to treat
   * the line as a single record.
   *
   * @param line the raw physical line (never {@code null})
   * @return the chunk width, or empty to treat the whole line as one record
   */
  OptionalInt recordWidthFor(String line);

  /**
   * Returns a strategy that treats every line as a single record. This is the default strategy
   * used by {@link FixedFormatReader} and {@link FixedFormatSegmentReader} when no
   * {@code lineSlicing} is specified on the builder.
   *
   * @return a single-record strategy; never {@code null}
   */
  static LineSlicingStrategy singleRecord() {
    return line -> OptionalInt.empty();
  }

  /**
   * Returns a strategy that slices every line into fixed-width chunks of exactly
   * {@code recordWidth} characters.
   *
   * @param recordWidth the number of characters per record; must be &gt; 0
   * @return a packed slicing strategy; never {@code null}
   * @throws IllegalArgumentException if {@code recordWidth} is zero or negative
   */
  static LineSlicingStrategy packed(int recordWidth) {
    if (recordWidth <= 0) {
      throw new IllegalArgumentException("recordWidth must be > 0, got: " + recordWidth);
    }
    return line -> OptionalInt.of(recordWidth);
  }

  /**
   * Returns a strategy that slices lines matching {@code isPackedLine} into fixed-width chunks of
   * {@code recordWidth} characters, and treats all other lines as single records. Use this for
   * files where some lines are packed and others are not.
   *
   * @param isPackedLine predicate that returns {@code true} for lines that should be sliced;
   *                     must not be {@code null}
   * @param recordWidth  the number of characters per record for packed lines; must be &gt; 0
   * @return a mixed slicing strategy; never {@code null}
   * @throws NullPointerException     if {@code isPackedLine} is {@code null}
   * @throws IllegalArgumentException if {@code recordWidth} is zero or negative
   */
  static LineSlicingStrategy mixed(Predicate<String> isPackedLine, int recordWidth) {
    Objects.requireNonNull(isPackedLine, "isPackedLine must not be null");
    if (recordWidth <= 0) {
      throw new IllegalArgumentException("recordWidth must be > 0, got: " + recordWidth);
    }
    return line -> isPackedLine.test(line) ? OptionalInt.of(recordWidth) : OptionalInt.empty();
  }
}
