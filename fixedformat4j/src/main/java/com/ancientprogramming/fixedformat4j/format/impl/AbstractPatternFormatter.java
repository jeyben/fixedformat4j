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
package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

/**
 * Base class for pattern-based date/time formatters.
 *
 * <p>Overrides {@link #getRemovePadding} to restore padding characters that belong
 * to the date/time value rather than to the field padding. This is necessary when
 * {@code paddingChar} equals a character that can also appear within a formatted date
 * (e.g. {@code '0'} with a pattern like {@code yyyyMMddHHmmss} where seconds may be
 * {@code 00}).
 *
 * <p>After stripping padding, if the result is shorter than the formatted output length
 * for the pattern, it is re-padded to that length so that the underlying date parser
 * receives a well-formed string.
 *
 * @param <T> the date/time type handled by this formatter
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.6.1
 */
public abstract class AbstractPatternFormatter<T> extends AbstractFixedFormatter<T> {

  @Override
  protected String getRemovePadding(String value, FormatInstructions instructions) {
    String stripped = instructions.getAlignment().remove(value, instructions.getPaddingChar());
    if (stripped.isEmpty()) {
      return stripped;
    }
    String pattern = instructions.getFixedFormatPatternData().getPattern();
    int formattedLength = formattedLengthForPattern(pattern);
    if (stripped.length() < formattedLength) {
      return instructions.getAlignment().apply(stripped, formattedLength, instructions.getPaddingChar());
    }
    return stripped;
  }

  /**
   * Returns the number of characters that this formatter's pattern produces when applied
   * to any date/time value. Used to restore padding characters that are part of the
   * date/time value rather than field padding.
   *
   * @param pattern the date/time pattern string
   * @return the fixed character length of the formatted output
   */
  protected abstract int formattedLengthForPattern(String pattern);
}
