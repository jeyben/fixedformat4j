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
package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;

import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern.DATE_PATTERN;
import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern.LOCALDATE_PATTERN;
import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern.DATETIME_PATTERN;

/**
 * Data object containing the exact same data as {@link FixedFormatPattern}
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FixedFormatPatternData {

  private String pattern;
  public static final FixedFormatPatternData DEFAULT = new FixedFormatPatternData(DATE_PATTERN);
  public static final FixedFormatPatternData LOCALDATE_DEFAULT = new FixedFormatPatternData(LOCALDATE_PATTERN);
  public static final FixedFormatPatternData DATETIME_DEFAULT = new FixedFormatPatternData(DATETIME_PATTERN);

  /**
   * Creates a pattern data object with the given date/time pattern.
   *
   * @param pattern the date/time pattern string (e.g. {@code "yyyyMMdd"})
   */
  public FixedFormatPatternData(String pattern) {
    this.pattern = pattern;
  }

  /**
   * Returns the date/time pattern string.
   *
   * @return the pattern (e.g. {@code "yyyyMMdd"})
   */
  public String getPattern() {
    return pattern;
  }


  public String toString() {
    return String.format("FixedFormatPatternData{pattern='%s'}", pattern);
  }
}
