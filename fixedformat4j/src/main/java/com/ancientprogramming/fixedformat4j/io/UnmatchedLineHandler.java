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
 * Callback invoked when a line matches no registered pattern and
 * {@link UnmatchedLineStrategy#FORWARD_TO_HANDLER} is active.
 *
 * <p>Register an implementation via
 * {@link FixedFormatReader.Builder#unmatchedLineHandler(UnmatchedLineHandler)}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
@FunctionalInterface
public interface UnmatchedLineHandler {

  /**
   * Handles a line that matched no registered {@link ClassPatternMapping}.
   *
   * @param lineNumber the 1-based line number within the source being read
   * @param line       the raw content of the line, without any trailing line-ending characters
   */
  void handle(long lineNumber, String line);
}
