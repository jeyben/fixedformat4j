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
import org.apache.commons.lang3.StringUtils;

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
    if (StringUtils.isEmpty(value)) {
      return null;
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
      Enum<?>[] constants = enumClass.getEnumConstants();
      if (ordinal < 0 || ordinal >= constants.length) {
        throw new FixedFormatException(
            String.format("Ordinal [%d] is out of range for enum [%s] with %d constants", ordinal, enumClass.getName(), constants.length));
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
      return null;
    }
    return enumFormat(instructions) == EnumFormat.NUMERIC
        ? String.valueOf(value.ordinal())
        : value.name();
  }

  private EnumFormat enumFormat(FormatInstructions instructions) {
    FixedFormatEnumData data = instructions.getFixedFormatEnumData();
    return (data != null) ? data.getEnumFormat() : EnumFormat.LITERAL;
  }
}
