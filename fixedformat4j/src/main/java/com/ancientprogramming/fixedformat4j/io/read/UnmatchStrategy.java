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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy invoked when no pattern matches a line.
 *
 * <p>Two built-in strategies are provided as static factory methods: {@link #skip()} and
 * {@link #throwException()}.</p>
 *
 * <p>Because this is a {@link FunctionalInterface}, a lambda can be passed wherever an
 * {@code UnmatchStrategy} is expected:</p>
 * <pre>{@code
 * .unmatchStrategy((lineNumber, line) ->
 *     System.err.println("Unmatched line " + lineNumber + ": " + line))
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
@FunctionalInterface
public interface UnmatchStrategy {

  Logger LOG = LoggerFactory.getLogger(UnmatchStrategy.class);

  /**
   * Handles a line that matched no registered pattern.
   *
   * <p>Implementations may throw a {@link FixedFormatException} to abort processing, or
   * return normally to skip the line.</p>
   *
   * @param lineNumber the 1-based line number within the source being read
   * @param line       the raw content of the unmatched line, without trailing line-ending characters
   */
  void handle(long lineNumber, String line);

  /**
   * Returns a strategy that skips unmatched lines and logs each one at WARN level via SLF4J.
   * Useful for files where header, footer, or comment lines are expected but should still be visible.
   *
   * <p><strong>Note:</strong> logging only occurs if an SLF4J binding is present on the
   * classpath at runtime. If no binding is configured, the warning is silently discarded.
   * When guaranteed error visibility is required, use a custom lambda strategy instead.</p>
   *
   * @return a skip-and-warn strategy; never {@code null}
   */
  static UnmatchStrategy skip() {
    return (lineNumber, line) ->
      LOG.warn("Skipping unmatched line {}: {}", lineNumber, line);
  }

  /**
   * Returns a strategy that throws {@link FixedFormatException} when a line is unmatched,
   * including the line number and raw content in the exception message.
   *
   * @return a fail-fast strategy; never {@code null}
   */
  static UnmatchStrategy throwException() {
    return (lineNumber, line) -> {
      throw new FixedFormatException("No pattern matched line " + lineNumber + ": " + line);
    };
  }
}
