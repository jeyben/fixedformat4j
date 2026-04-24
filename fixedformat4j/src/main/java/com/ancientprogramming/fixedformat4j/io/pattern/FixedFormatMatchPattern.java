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
package com.ancientprogramming.fixedformat4j.io.pattern;

/**
 * Strategy interface for deciding whether a line in a fixed-format file should be loaded
 * as a particular record type. Implement this interface to define custom line-discrimination
 * logic beyond the built-in {@link RegexFixedFormatMatchPattern}.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public interface FixedFormatMatchPattern {

  /**
   * Returns {@code true} if the given line should be parsed as the associated record type.
   *
   * <p>Implementations must handle a {@code null} argument gracefully and return {@code false}
   * rather than throwing a {@link NullPointerException}.</p>
   *
   * @param line the raw text of the line, without any trailing line-ending characters;
   *             may be {@code null}
   * @return {@code true} if this pattern matches {@code line}; {@code false} otherwise
   */
  boolean matches(String line);
}
