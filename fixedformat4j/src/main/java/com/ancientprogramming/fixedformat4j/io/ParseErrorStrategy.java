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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls what happens when a matched line fails to parse into a record object.
 *
 * <p>The default strategy used by {@link FixedFormatReader} is {@link #THROW}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public enum ParseErrorStrategy {

  /**
   * Rethrow the exception immediately (fail-fast). The line number is included in the
   * wrapped exception message.
   */
  THROW {
    @Override
    public <T> T handle(FixedFormatException wrapped, String line, long lineNumber,
        ParseErrorHandler handler) {
      throw wrapped;
    }
  },

  /**
   * Skip the line and log details (line number, raw content, exception message) at WARN
   * level via SLF4J. Processing continues with the next line.
   *
   * <p><strong>Note:</strong> logging only occurs if an SLF4J binding is present on the
   * classpath at runtime. If no binding is configured, the warning is silently discarded.
   * When guaranteed error visibility is required, prefer {@link #FORWARD_TO_HANDLER}.</p>
   */
  SKIP_AND_LOG {
    @Override
    public <T> T handle(FixedFormatException wrapped, String line, long lineNumber,
        ParseErrorHandler handler) {
      LOG.warn("Skipping line {}: {} — {}", lineNumber, line, wrapped.getCause() != null
          ? wrapped.getCause().getMessage() : wrapped.getMessage());
      return null;
    }
  },

  /**
   * Delegate to a registered {@link ParseErrorHandler}. The handler must be provided
   * via {@link FixedFormatReader.Builder#parseErrorHandler(ParseErrorHandler)} before
   * calling {@link FixedFormatReader.Builder#build()}, otherwise building the reader throws
   * {@link IllegalStateException}.
   */
  FORWARD_TO_HANDLER {
    @Override
    public <T> T handle(FixedFormatException wrapped, String line, long lineNumber,
        ParseErrorHandler handler) {
      handler.handle(lineNumber, line, wrapped);
      return null;
    }
  };

  private static final Logger LOG = LoggerFactory.getLogger(ParseErrorStrategy.class);

  /**
   * Applies this strategy when a matched line fails to parse.
   *
   * @param wrapped    the exception wrapping the original parse failure, with line number
   * @param line       the raw line content
   * @param lineNumber the 1-based line number
   * @param handler    the registered handler; may be {@code null} unless this strategy
   *                   is {@link #FORWARD_TO_HANDLER}
   * @param <T>        the record type (always returns {@code null} for non-THROW strategies)
   * @return {@code null} for skip/handler strategies; never returns for {@link #THROW}
   */
  public abstract <T> T handle(FixedFormatException wrapped, String line, long lineNumber,
      ParseErrorHandler handler);
}
