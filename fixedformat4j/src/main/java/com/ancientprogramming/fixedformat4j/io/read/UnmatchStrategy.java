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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

/**
 * Strategy invoked when no pattern matches a text segment.
 *
 * <p>A "segment" is any string of text that is tried against registered patterns — it may be
 * a full physical line (in {@link FixedFormatReader}) or a fixed-width chunk extracted from a
 * line (in {@link PackedRecordReader}). Two built-in strategies are provided as static factory
 * methods: {@link #skip()} and {@link #throwException()}.</p>
 *
 * <p>Because this is a {@link FunctionalInterface}, a lambda can be passed wherever an
 * {@code UnmatchStrategy} is expected:</p>
 * <pre>{@code
 * .unmatchStrategy((lineNumber, segment) ->
 *     System.err.println("Unmatched segment at line " + lineNumber + ": " + segment))
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
@FunctionalInterface
public interface UnmatchStrategy {

  /**
   * Handles a segment that matched no registered pattern.
   *
   * <p>Implementations may throw a {@link FixedFormatException} to abort processing, or
   * return normally to silently skip the segment.</p>
   *
   * @param lineNumber the 1-based line number within the source being read
   * @param segment    the raw content of the unmatched segment, without trailing line-ending characters
   */
  void handle(long lineNumber, String segment);

  /**
   * Returns a strategy that silently ignores unmatched segments.
   * Useful for files where header, footer, comment lines, or padding chunks are expected.
   *
   * @return a no-op strategy; never {@code null}
   */
  static UnmatchStrategy skip() {
    return (lineNumber, segment) -> {};
  }

  /**
   * Returns a strategy that throws {@link FixedFormatException} when a segment is unmatched,
   * including the line number and raw content in the exception message.
   *
   * @return a fail-fast strategy; never {@code null}
   */
  static UnmatchStrategy throwException() {
    return (lineNumber, segment) -> {
      throw new FixedFormatException("No pattern matched line " + lineNumber + ": " + segment);
    };
  }
}
