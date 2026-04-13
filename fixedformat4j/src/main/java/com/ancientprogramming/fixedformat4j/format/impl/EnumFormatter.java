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
 * Formatter for enum fields. Supports two serialization modes:
 * <ul>
 *   <li>{@link EnumFormat#LITERAL} — uses {@link Enum#name()} / {@link Enum#valueOf}</li>
 *   <li>{@link EnumFormat#NUMERIC} — uses {@link Enum#ordinal()} as an integer string</li>
 * </ul>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
public class EnumFormatter extends AbstractFixedFormatter<Enum> {

  private final FormatContext<?> context;

  /**
   * Creates an {@code EnumFormatter} bound to the given format context.
   *
   * @param context the format context whose data type is the enum class to parse/format
   */
  public EnumFormatter(FormatContext<?> context) {
    this.context = context;
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Enum asObject(String value, FormatInstructions instructions) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    Class<? extends Enum> enumClass = (Class<? extends Enum>) context.getDataType();
    EnumFormat format = enumFormat(instructions);
    if (format == EnumFormat.NUMERIC) {
      int ordinal;
      try {
        ordinal = Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        throw new FixedFormatException(
            String.format("Could not parse ordinal value '%s' for enum [%s]", value, enumClass.getName()), e);
      }
      Enum[] constants = enumClass.getEnumConstants();
      if (ordinal < 0 || ordinal >= constants.length) {
        throw new FixedFormatException(
            String.format("Ordinal %d is out of range for enum [%s] (has %d constants)", ordinal, enumClass.getName(), constants.length));
      }
      return constants[ordinal];
    } else {
      try {
        return Enum.valueOf(enumClass, value.trim());
      } catch (IllegalArgumentException e) {
        throw new FixedFormatException(
            String.format("Could not convert string '%s' to enum [%s]", value, enumClass.getName()), e);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public String asString(Enum value, FormatInstructions instructions) {
    if (value == null) {
      return "";
    }
    EnumFormat format = enumFormat(instructions);
    return (format == EnumFormat.NUMERIC)
        ? String.valueOf(value.ordinal())
        : value.name();
  }

  private EnumFormat enumFormat(FormatInstructions instructions) {
    FixedFormatEnumData data = instructions.getFixedFormatEnumData();
    return (data != null) ? data.getEnumFormat() : EnumFormat.LITERAL;
  }
}
