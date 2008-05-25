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

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Formatter for {@link java.util.Date} data
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class DateFormatter extends AbstractFixedFormatter {

  public Object asObject(String string, FormatInstructions instructions) throws FixedFormatException {
    try {
      return getFormatter(instructions.getFixedFormatPatternData().getPattern()).parse(string);
    } catch (ParseException e) {
      throw new FixedFormatException("Could not parse value[" + string + "] by pattern[" + instructions.getFixedFormatPatternData().getPattern() + "] to " + Date.class.getName());
    }
  }

  public String asString(Object obj, FormatInstructions instructions) {
    Date date = (Date) obj;
    return getFormatter(instructions.getFixedFormatPatternData().getPattern()).format(date);
  }

  DateFormat getFormatter(String pattern) {
    return new SimpleDateFormat(pattern);
  }
}
