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
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Formatter capable of formatting a bunch of known java standard library classes. So far:
 * {@link String}, {@link Integer}, {@link Short}, {@link Long}, {@link Date},
 * {@link Character}, {@link Boolean}, {@link Double}, {@link Float} and {@link BigDecimal}
 *
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class ByTypeFormatter implements FixedFormatter<Object> {
  private FormatContext context;

  private static final Map<Class<? extends Serializable>, Class<? extends FixedFormatter>> KNOWN_FORMATTERS = new HashMap<Class<? extends Serializable>, Class<? extends FixedFormatter>>();

  static {
    KNOWN_FORMATTERS.put(String.class, StringFormatter.class);
    KNOWN_FORMATTERS.put(short.class, ShortFormatter.class);
    KNOWN_FORMATTERS.put(Short.class, ShortFormatter.class);
    KNOWN_FORMATTERS.put(int.class, IntegerFormatter.class);
    KNOWN_FORMATTERS.put(Integer.class, IntegerFormatter.class);
    KNOWN_FORMATTERS.put(long.class, LongFormatter.class);
    KNOWN_FORMATTERS.put(Long.class, LongFormatter.class);
    KNOWN_FORMATTERS.put(Date.class, DateFormatter.class);
    KNOWN_FORMATTERS.put(char.class, CharacterFormatter.class);
    KNOWN_FORMATTERS.put(Character.class, CharacterFormatter.class);
    KNOWN_FORMATTERS.put(boolean.class, BooleanFormatter.class);
    KNOWN_FORMATTERS.put(Boolean.class, BooleanFormatter.class);
    KNOWN_FORMATTERS.put(double.class, DoubleFormatter.class);
    KNOWN_FORMATTERS.put(Double.class, DoubleFormatter.class);
    KNOWN_FORMATTERS.put(float.class, FloatFormatter.class);
    KNOWN_FORMATTERS.put(Float.class, FloatFormatter.class);
    KNOWN_FORMATTERS.put(BigDecimal.class,  BigDecimalFormatter.class);
  }

  public ByTypeFormatter(FormatContext context) {
    this.context = context;
  }


  public Object parse(String value, FormatInstructions instructions) {
    FixedFormatter formatter = actualFormatter(context.getDataType());
    return formatter.parse(value, instructions);
  }

  public String format(Object value, FormatInstructions instructions) {
    FixedFormatter formatter = actualFormatter(context.getDataType());
    return formatter.format(value, instructions);
  }

  public FixedFormatter actualFormatter(final Class<? extends Object> dataType) {
    Class<? extends FixedFormatter> formatterClass = KNOWN_FORMATTERS.get(dataType);

    if (formatterClass != null) {
      try {
        return formatterClass.getConstructor().newInstance();
      } catch (NoSuchMethodException e) {
        throw new FixedFormatException("Could not create instance of[" + formatterClass.getName() + "] because no default constructor exists");
      } catch (Exception e) {
        throw new FixedFormatException("Could not create instance of[" + formatterClass.getName() + "]", e);
      }
    } else {
      throw new FixedFormatException(ByTypeFormatter.class.getName() + " cannot handle datatype[" + dataType.getName() + "]. Provide your own custom FixedFormatter for this datatype.");
    }
  }
}
