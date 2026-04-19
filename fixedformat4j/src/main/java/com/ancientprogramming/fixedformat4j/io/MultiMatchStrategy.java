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
 * Controls what happens when more than one {@link ClassPatternMapping} matches the same line.
 *
 * <p>The default strategy used by {@link FixedFormatReader} is {@link #FIRST_MATCH}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public enum MultiMatchStrategy {

  /**
   * Use the first matching mapping in registration order and ignore the rest.
   * This is the default strategy.
   */
  FIRST_MATCH,

  /**
   * Throw {@link com.ancientprogramming.fixedformat4j.exception.FixedFormatException} if more
   * than one pattern matches, including the line number and all matching class names in the
   * exception message.
   */
  THROW_ON_AMBIGUITY,

  /**
   * Load the line once per matching mapping and emit one record object per match,
   * in registration order.
   */
  ALL_MATCHES
}
