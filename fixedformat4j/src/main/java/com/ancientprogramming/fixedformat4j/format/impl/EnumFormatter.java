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

import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatEnumData;

/**
 * Formatter for Java enum types.
 *
 * <p>Supports two serialization modes controlled by {@code @FixedFormatEnum}:
 * <ul>
 *   <li>{@link EnumFormat#LITERAL} (default) — uses {@link Enum#name()} / {@link Enum#valueOf}</li>
 *   <li>{@link EnumFormat#NUMERIC} — uses {@link Enum#ordinal()} / index lookup</li>
 * </ul>
 *
 * <p>Padding is handled by the {@link AbstractFixedFormatter} base class using the field's
 * configured {@code paddingChar} and {@code align}. This formatter receives a padding-stripped
 * value in {@link #asObject} and must not apply additional whitespace-specific trimming,
 * so non-space padding characters (e.g. {@code '*'}, {@code '0'}) are handled correctly.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
public class EnumFormatter extends AbstractFixedFormatter<Enum> {

  private final FormatContext<?> context;

  /**
   * Lazily cached constants of the context's enum type, in ordinal order. Cached because
   * {@link Class#getEnumConstants()} clones the array on every call. The array is never
   * exposed or mutated; the benign race on first initialization is harmless.
   */
  private volatile Enum<?>[] constants;

  /**
   * Creates an {@code EnumFormatter} bound to the given format context.
   * The context's data type must be an enum class.
   *
   * @param context the format context describing the field's data type
   */
  public EnumFormatter(FormatContext<?> context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The {@code value} received here has already had its padding stripped by
   * {@link AbstractFixedFormatter#parse} using the field's actual {@code paddingChar},
   * so no additional trimming is performed.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Enum asObject(String value, FormatInstructions instructions) {
    if (value == null) {
      return null;
    }
    if (value.isEmpty()) {
      // In NUMERIC mode with '0' padding, padding-stripping consumes an all-zeros field
      // entirely — but those zeros WERE the value (ordinal 0), not a blank field.
      // Mirrors the empty-means-"0" convention of Sign.remove for numeric fields.
      if (enumFormat(instructions) == EnumFormat.NUMERIC && instructions.getPaddingChar() == '0') {
        value = "0";
      } else {
        return null;
      }
    }
    Class<? extends Enum> enumClass = (Class<? extends Enum>) context.getDataType();
    EnumFormat format = enumFormat(instructions);
    if (format == EnumFormat.NUMERIC) {
      int ordinal;
      try {
        ordinal = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        throw new FixedFormatException(
            String.format("Cannot parse ordinal for enum [%s] from value [%s]", enumClass.getName(), value), e);
      }
      Enum<?>[] constants = constantsOf(enumClass);
      if (ordinal < 0 || ordinal >= constants.length) {
        throw new FixedFormatException(
            String.format("Ordinal [%d] is out of range for enum [%s] (valid range: 0..%d)", ordinal, enumClass.getName(), constants.length - 1));
      }
      return constants[ordinal];
    } else {
      try {
        return Enum.valueOf(enumClass, value);
      } catch (IllegalArgumentException e) {
        throw new FixedFormatException(
            String.format("Cannot find enum constant [%s] in [%s]", value, enumClass.getName()), e);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public String asString(Enum value, FormatInstructions instructions) {
    if (value == null) {
      return "";
    }
    return enumFormat(instructions) == EnumFormat.NUMERIC
        ? String.valueOf(value.ordinal())
        : value.name();
  }

  @SuppressWarnings("rawtypes")
  private Enum<?>[] constantsOf(Class<? extends Enum> enumClass) {
    Enum<?>[] result = constants;
    if (result == null) {
      result = enumClass.getEnumConstants();
      constants = result;
    }
    return result;
  }

  private EnumFormat enumFormat(FormatInstructions instructions) {
    FixedFormatEnumData data = instructions.getFixedFormatEnumData();
    return (data != null) ? data.getEnumFormat() : EnumFormat.LITERAL;
  }
}
