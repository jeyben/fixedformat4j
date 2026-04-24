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
package com.ancientprogramming.fixedformat4j.io.strategy;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.Optional;

/**
 * Strategy invoked when the last chunk on a physical line is shorter than the declared
 * {@code recordWidth}. Returns the string to push through the match/parse pipeline (possibly
 * padded), or {@link Optional#empty()} to discard it silently.
 *
 * <p>Four built-in strategies are provided as static factory methods:</p>
 * <ul>
 *   <li>{@link #skip()} — discard the partial chunk</li>
 *   <li>{@link #pad()} — right-pad with spaces to {@code recordWidth}</li>
 *   <li>{@link #pad(char)} — right-pad with a custom character to {@code recordWidth}</li>
 *   <li>{@link #throwException()} — fail fast with a {@link FixedFormatException}</li>
 * </ul>
 *
 * <p>Because this is a {@link FunctionalInterface}, a lambda can also be supplied directly.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 * @see com.ancientprogramming.fixedformat4j.io.PackedRecordReader
 */
@FunctionalInterface
public interface PartialChunkStrategy {

  /**
   * Resolves a partial chunk.
   *
   * @param lineNumber  1-based physical line number of the source line containing this chunk
   * @param chunkIndex  1-based position of this chunk within its physical line (always the last)
   * @param chunk       the partial content; {@code chunk.length() < recordWidth}
   * @param recordWidth the expected full width in characters
   * @return the string to pass through pattern-match and parse, or {@link Optional#empty()} to discard
   */
  Optional<String> resolve(long lineNumber, int chunkIndex, String chunk, int recordWidth);

  /**
   * Returns a strategy that silently discards partial chunks.
   *
   * @return a no-op skip strategy; never {@code null}
   */
  static PartialChunkStrategy skip() {
    return (lineNumber, chunkIndex, chunk, recordWidth) -> Optional.empty();
  }

  /**
   * Returns a strategy that right-pads partial chunks with spaces to {@code recordWidth}.
   *
   * @return a space-padding strategy; never {@code null}
   */
  static PartialChunkStrategy pad() {
    return pad(' ');
  }

  /**
   * Returns a strategy that right-pads partial chunks with {@code padChar} to {@code recordWidth}.
   *
   * @param padChar the character used to fill the trailing positions
   * @return a padding strategy; never {@code null}
   */
  static PartialChunkStrategy pad(char padChar) {
    return (lineNumber, chunkIndex, chunk, recordWidth) -> {
      StringBuilder sb = new StringBuilder(recordWidth);
      sb.append(chunk);
      while (sb.length() < recordWidth) {
        sb.append(padChar);
      }
      return Optional.of(sb.toString());
    };
  }

  /**
   * Returns a strategy that throws {@link FixedFormatException} when a partial chunk is encountered.
   *
   * @return a fail-fast strategy; never {@code null}
   */
  static PartialChunkStrategy throwException() {
    return (lineNumber, chunkIndex, chunk, recordWidth) -> {
      throw new FixedFormatException(
          "Partial chunk at line " + lineNumber + ", chunk " + chunkIndex
              + ": expected " + recordWidth + " chars but got " + chunk.length());
    };
  }
}
