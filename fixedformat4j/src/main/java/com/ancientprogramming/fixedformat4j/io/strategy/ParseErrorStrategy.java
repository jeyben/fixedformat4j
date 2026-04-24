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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy invoked when a matched line fails to parse into a record object.
 *
 * <p>Implement this interface to define custom error handling — for example, collecting
 * failed lines for a post-processing error report. Two built-in strategies are provided as
 * static factory methods: {@link #throwException()} and {@link #skipAndLog()}.</p>
 *
 * <p>Because this is a {@link FunctionalInterface}, a lambda can be passed wherever a
 * {@code ParseErrorStrategy} is expected:</p>
 * <pre>{@code
 * .parseErrorStrategy((wrapped, line, lineNumber) ->
 *     errorList.add("Line " + lineNumber + ": " + wrapped.getMessage()))
 * }</pre>
 *
 * <p>To abort processing on error, throw from the lambda. To skip the line, return normally.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
@FunctionalInterface
public interface ParseErrorStrategy {

  Logger LOG = LoggerFactory.getLogger(ParseErrorStrategy.class);

  /**
   * Handles a line that matched a pattern but could not be parsed into a record object.
   *
   * <p>Throw a {@link FixedFormatException} to abort processing, or return normally to
   * skip the record and continue with the next line.</p>
   *
   * @param wrapped    a {@link FixedFormatException} wrapping the original parse failure;
   *                   the exception message includes the line number
   * @param line       the raw content of the line, without any trailing line-ending characters
   * @param lineNumber the 1-based line number within the source being read
   */
  void handle(FixedFormatException wrapped, String line, long lineNumber);

  /**
   * Returns a strategy that rethrows the parse exception immediately (fail-fast).
   *
   * @return a fail-fast strategy; never {@code null}
   */
  static ParseErrorStrategy throwException() {
    return (wrapped, line, lineNumber) -> { throw wrapped; };
  }

  /**
   * Returns a strategy that skips the line and logs details at WARN level via SLF4J.
   * Processing continues with the next line.
   *
   * <p><strong>Note:</strong> logging only occurs if an SLF4J binding is present on the
   * classpath at runtime. If no binding is configured, the warning is silently discarded.
   * When guaranteed error visibility is required, use a custom lambda strategy instead.</p>
   *
   * @return a skip-and-log strategy; never {@code null}
   */
  static ParseErrorStrategy skipAndLog() {
    return (wrapped, line, lineNumber) ->
        LOG.warn("Skipping line {}: {} — {}", lineNumber, line,
            wrapped.getCause() != null ? wrapped.getCause().getMessage() : wrapped.getMessage());
  }
}
