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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for pattern-based date/time formatters.
 *
 * <p>Overrides {@link #stripPadding} to restore padding characters that belong
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

  private static final Map<Class<?>, Map<String, Integer>> PATTERN_LENGTH_CACHE = new ConcurrentHashMap<>();

  /**
   * Strips padding, then re-applies it when the padding character overlaps with
   * characters that are significant in the date/time pattern.
   *
   * <p>For example, with padding {@code '0'} and pattern {@code yyyyMMddHHmmss},
   * a field value of {@code "00"} (seconds) would otherwise be stripped to an empty
   * string, making the date un-parseable. This override detects that case and
   * restores the stripped characters before returning.
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected String stripPadding(String value, FormatInstructions instructions) {
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
   * @deprecated Use {@link #stripPadding} instead. This method will be removed in 1.7.0.
   * @see AbstractFixedFormatter#getRemovePadding
   */
  @Deprecated
  @Override
  protected String getRemovePadding(String value, FormatInstructions instructions) {
    return stripPadding(value, instructions);
  }

  protected final int formattedLengthForPattern(String pattern) {
    return PATTERN_LENGTH_CACHE
        .computeIfAbsent(getClass(), k -> new ConcurrentHashMap<>())
        .computeIfAbsent(pattern, this::computeFormattedLengthForPattern);
  }

  /**
   * Returns the number of characters that this formatter's pattern produces when applied
   * to any date/time value. Used to restore padding characters that are part of the
   * date/time value rather than field padding.
   *
   * <p>This method is called at most once per (formatter type, pattern) pair; results are
   * cached by {@link #formattedLengthForPattern}.
   *
   * @param pattern the date/time pattern string
   * @return the fixed character length of the formatted output
   */
  protected abstract int computeFormattedLengthForPattern(String pattern);
}
