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
import com.ancientprogramming.fixedformat4j.format.FixedFormatMetadata;
import com.ancientprogramming.fixedformat4j.format.FixedFormatData;
import com.ancientprogramming.fixedformat4j.format.FixedFormatProcessor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.math.BigDecimal;
import java.io.Serializable;

/**
 * Formatter capable of formatting a bunch of known java standard library classes. So far:
 * {@link String}, {@link Integer}, {@link Long}, {@link Date},
 * {@link Character}, {@link Boolean}, {@link Double} and {@link Float}
 *
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class ByTypeFormatter implements FixedFormatter {
  private FixedFormatMetadata metadata;

  private static final Map<Class<? extends Serializable>, Class<? extends FixedFormatter>> KNOWN_FORMATTERS = new HashMap<Class<? extends Serializable>, Class<? extends FixedFormatter>>();

  static {
    KNOWN_FORMATTERS.put(String.class, StringFormatter.class);
    KNOWN_FORMATTERS.put(Integer.class, IntegerFormatter.class);
    KNOWN_FORMATTERS.put(Long.class, LongFormatter.class);
    KNOWN_FORMATTERS.put(Date.class, DateFormatter.class);
    KNOWN_FORMATTERS.put(Character.class, CharacterFormatter.class);
    KNOWN_FORMATTERS.put(Boolean.class, BooleanFormatter.class);
    KNOWN_FORMATTERS.put(Double.class, DoubleFormatter.class);
    KNOWN_FORMATTERS.put(Float.class, FloatFormatter.class);
    KNOWN_FORMATTERS.put(BigDecimal.class,  BigDecimalFormatter.class);
  }

  public ByTypeFormatter(FixedFormatMetadata metadata) {
    this.metadata = metadata;
  }


  public Object parse(String value, FixedFormatData data) {
    FixedFormatter formatter = actualFormatter(metadata.getDataType());
    FixedFormatProcessor.assertIsPatternRequired(data, metadata, formatter);
    FixedFormatProcessor.assertIsBooleanRequired(data, metadata, formatter);
    FixedFormatProcessor.assertIsDecimalRequired(data, metadata, formatter);
    return formatter.parse(value, data);
  }

  public String format(Object value, FixedFormatData data) {
    FixedFormatter formatter = actualFormatter(metadata.getDataType());
    FixedFormatProcessor.assertIsPatternRequired(data, metadata, formatter);
    FixedFormatProcessor.assertIsBooleanRequired(data, metadata, formatter);
    FixedFormatProcessor.assertIsDecimalRequired(data, metadata, formatter);
    return formatter.format(value, data);
  }

  /**
   * In general the by type formatter doesn't require a pattern annotation, but will ask the actual formatter
   * if it requires a pattern or not.
   *
   * @return always <code>false</code>
   */
  public boolean requiresPattern() {
    return false;
  }

  public boolean requiresBoolean() {
    return false;
  }

  public boolean requiresDecimal() {
    return false;
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
