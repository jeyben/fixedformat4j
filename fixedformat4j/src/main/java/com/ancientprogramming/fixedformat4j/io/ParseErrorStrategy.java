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
  THROW,

  /**
   * Skip the line and log details (line number, raw content, exception message) at WARN
   * level via SLF4J. Processing continues with the next line.
   */
  SKIP_AND_LOG,

  /**
   * Delegate to a registered {@link ParseErrorHandler}. The handler must be provided
   * via {@link FixedFormatReader.Builder#parseErrorHandler(ParseErrorHandler)} before
   * calling {@link FixedFormatReader.Builder#build()}, otherwise building the reader throws
   * {@link IllegalStateException}.
   */
  FORWARD_TO_HANDLER
}
