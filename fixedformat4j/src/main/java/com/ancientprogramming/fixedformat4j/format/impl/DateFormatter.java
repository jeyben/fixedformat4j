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
import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Formatter for {@link java.util.Date} data.
 * The formatting and parsing is perfomed by using an instance of the {@link SimpleDateFormat} class.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class DateFormatter extends AbstractFixedFormatter<Date> {

  public Date asObject(String string, FormatInstructions instructions) throws FixedFormatException {
    Date result = null;

    if (!StringUtils.isEmpty(string)) {
      try {
        result = getFormatter(instructions.getFixedFormatPatternData().getPattern()).parse(string);
      } catch (ParseException e) {
        throw new FixedFormatException("Could not parse value[" + string + "] by pattern[" + instructions.getFixedFormatPatternData().getPattern() + "] to " + Date.class.getName());
      }
    }
    return result;
  }

  public String asString(Date date, FormatInstructions instructions) {
    String result = null;
    if (date != null) {
      result = getFormatter(instructions.getFixedFormatPatternData().getPattern()).format(date);
    }
    return result;
  }

  DateFormat getFormatter(String pattern) {
    return new SimpleDateFormat(pattern);
  }
}
