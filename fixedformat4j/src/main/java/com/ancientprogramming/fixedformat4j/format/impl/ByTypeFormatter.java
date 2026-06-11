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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Formatter capable of formatting a bunch of known java standard library classes. So far:
 * {@link String}, {@link Integer}, {@link Short}, {@link Long}, {@link Date}, {@link LocalDate},
 * {@link java.time.LocalDateTime}, {@link Character}, {@link Boolean}, {@link Double}, {@link Float},
 * {@link BigDecimal}, and all {@link Enum} subtypes (handled automatically via
 * {@link EnumFormatter}; use {@link com.ancientprogramming.fixedformat4j.annotation.FixedFormatEnum}
 * to switch between LITERAL and NUMERIC serialization).
 *
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class ByTypeFormatter implements FixedFormatter<Object> {
  private final FormatContext<?> context;

  /**
   * Delegate resolved once from {@code context.getDataType()} and reused by {@link #parse} and
   * {@link #format}, avoiding reflective instantiation on every call. All built-in formatters
   * are stateless (or use thread-local caches), so a single instance is safe to share across
   * threads. The benign race on first resolution at worst creates one extra instance; volatile
   * guarantees safe publication.
   */
  private volatile FixedFormatter<Object> delegate;

  private static final Map<Class<? extends Serializable>, Class<? extends FixedFormatter<?>>> KNOWN_FORMATTERS = Map.ofEntries(
      Map.entry(String.class, StringFormatter.class),
      Map.entry(short.class, ShortFormatter.class),
      Map.entry(Short.class, ShortFormatter.class),
      Map.entry(int.class, IntegerFormatter.class),
      Map.entry(Integer.class, IntegerFormatter.class),
      Map.entry(long.class, LongFormatter.class),
      Map.entry(Long.class, LongFormatter.class),
      Map.entry(Date.class, DateFormatter.class),
      Map.entry(LocalDate.class, LocalDateFormatter.class),
      Map.entry(LocalDateTime.class, LocalDateTimeFormatter.class),
      Map.entry(char.class, CharacterFormatter.class),
      Map.entry(Character.class, CharacterFormatter.class),
      Map.entry(boolean.class, BooleanFormatter.class),
      Map.entry(Boolean.class, BooleanFormatter.class),
      Map.entry(double.class, DoubleFormatter.class),
      Map.entry(Double.class, DoubleFormatter.class),
      Map.entry(float.class, FloatFormatter.class),
      Map.entry(Float.class, FloatFormatter.class),
      Map.entry(BigDecimal.class, BigDecimalFormatter.class));

  /**
   * Creates a {@code ByTypeFormatter} bound to the given format context.
   * The context's data type is used at runtime to select the appropriate typed formatter.
   *
   * @param context the format context describing the field's offset, data type, and formatter class
   */
  public ByTypeFormatter(FormatContext<?> context) {
    this.context = context;
  }

  /** {@inheritDoc} */
  public Object parse(String value, FormatInstructions instructions) {
    return delegate().parse(value, instructions);
  }

  /** {@inheritDoc} */
  public String format(Object value, FormatInstructions instructions) {
    return delegate().format(value, instructions);
  }

  @SuppressWarnings("unchecked")
  private FixedFormatter<Object> delegate() {
    FixedFormatter<Object> result = delegate;
    if (result == null) {
      result = (FixedFormatter<Object>) actualFormatter(context.getDataType());
      delegate = result;
    }
    return result;
  }

  /**
   * Looks up and instantiates the typed formatter for the given {@code dataType}.
   * Enum types are detected dynamically and routed to {@link EnumFormatter}.
   *
   * @param dataType the Java type of the field value
   * @return a formatter capable of handling {@code dataType}
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if no formatter is
   *         registered for {@code dataType} or the formatter cannot be instantiated
   */
  public FixedFormatter<?> actualFormatter(final Class<?> dataType) {
    if (dataType != null && dataType.isEnum()) {
      return new EnumFormatter(context);
    }

    Class<? extends FixedFormatter<?>> formatterClass = KNOWN_FORMATTERS.get(dataType);

    if (formatterClass != null) {
      try {
        return formatterClass.getConstructor().newInstance();
      } catch (NoSuchMethodException e) {
        throw new FixedFormatException(String.format("Could not create instance of[%s] because no default constructor exists", formatterClass.getName()));
      } catch (Exception e) {
        throw new FixedFormatException(String.format("Could not create instance of[%s]", formatterClass.getName()), e);
      }
    } else {
      throw new FixedFormatException(String.format("%s cannot handle datatype[%s]. Provide your own custom FixedFormatter for this datatype.", ByTypeFormatter.class.getName(), dataType.getName()));
    }
  }
}
