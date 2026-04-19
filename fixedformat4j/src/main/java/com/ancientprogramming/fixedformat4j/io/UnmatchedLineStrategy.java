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
 * Controls what happens when no {@link ClassPatternMapping} pattern matches a line.
 *
 * <p>The default strategy used by {@link FixedFormatReader} is {@link #SKIP}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public enum UnmatchedLineStrategy {

  /**
   * Silently ignore the line. Useful for header, footer, or comment lines that are
   * expected to be present but do not represent data records.
   */
  SKIP,

  /**
   * Throw {@link com.ancientprogramming.fixedformat4j.exception.FixedFormatException} with
   * the line number and raw content included in the exception message.
   */
  THROW,

  /**
   * Delegate to a registered {@link UnmatchedLineHandler}. The handler must be provided
   * via {@link FixedFormatReader.Builder#unmatchedLineHandler(UnmatchedLineHandler)} before
   * calling {@link FixedFormatReader.Builder#build()}, otherwise building the reader throws
   * {@link IllegalStateException}.
   */
  FORWARD_TO_HANDLER
}
