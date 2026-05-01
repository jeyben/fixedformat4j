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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Formatter for {@link java.time.LocalDate} data.
 * The formatting and parsing is performed using {@link DateTimeFormatter}.
 * The pattern is configured via {@link com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern}.
 * The default pattern is {@code yyyyMMdd}.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.4.0
 */
public class LocalDateFormatter extends AbstractPatternFormatter<LocalDate> {

  @Override
  protected int computeFormattedLengthForPattern(String pattern) {
    return formatterForPattern(pattern).format(LocalDate.of(1970, 1, 1)).length();
  }

  /** {@inheritDoc} */
  public LocalDate asObject(String string, FormatInstructions instructions) throws FixedFormatException {
    if (StringUtils.isEmpty(string)) {
      return null;
    }
    String pattern = instructions.getFixedFormatPatternData().getPattern();
    try {
      return LocalDate.parse(string, formatterForPattern(pattern));
    } catch (DateTimeParseException e) {
      throw new FixedFormatException(String.format("Could not parse value[%s] by pattern[%s] to %s", string, pattern, LocalDate.class.getName()));
    }
  }

  /** {@inheritDoc} */
  public String asString(LocalDate date, FormatInstructions instructions) {
    if (date == null) {
      return null;
    }
    return formatterForPattern(instructions.getFixedFormatPatternData().getPattern()).format(date);
  }
}
