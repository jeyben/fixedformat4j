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
package com.ancientprogramming.fixedformat4j.format;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;

/**
 * Immutable description of one {@code @Field} of a record class, as returned by
 * {@link FixedFormatIntrospector#introspect(Class)}.
 *
 * <p>All values are fully resolved: {@link #getEffectiveAlignment()} is never
 * {@link Align#INHERIT} — record-level alignment defaults have already been applied.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public final class FieldInfo {

  private final String propertyName;
  private final int offset;
  private final int length;
  private final Class<?> dataType;
  private final Align effectiveAlignment;
  private final char paddingChar;
  private final char nullChar;
  private final String nullValue;
  private final Class<? extends FixedFormatter<?>> formatterClass;
  private final int repeatCount;
  private final boolean nestedRecord;

  public FieldInfo(
      String propertyName,
      int offset,
      int length,
      Class<?> dataType,
      Align effectiveAlignment,
      char paddingChar,
      char nullChar,
      String nullValue,
      Class<? extends FixedFormatter<?>> formatterClass,
      int repeatCount,
      boolean nestedRecord) {
    this.propertyName = propertyName;
    this.offset = offset;
    this.length = length;
    this.dataType = dataType;
    this.effectiveAlignment = effectiveAlignment;
    this.paddingChar = paddingChar;
    this.nullChar = nullChar;
    this.nullValue = nullValue;
    this.formatterClass = formatterClass;
    this.repeatCount = repeatCount;
    this.nestedRecord = nestedRecord;
  }

  /**
   * @return the bean-style property name, e.g. {@code "customerId"} for {@code getCustomerId()};
   *         for Java record classes the component name as-is
   */
  public String getPropertyName() {
    return propertyName;
  }

  /** @return the 1-based offset of the field within the record */
  public int getOffset() {
    return offset;
  }

  /** @return the field length in characters, or {@link Field#REST_OF_LINE} ({@code -1}) */
  public int getLength() {
    return length;
  }

  /** @return the declared Java type of the property (the collection/array type for repeating fields) */
  public Class<?> getDataType() {
    return dataType;
  }

  /** @return the resolved alignment — {@link Align#LEFT} or {@link Align#RIGHT}, never {@link Align#INHERIT} */
  public Align getEffectiveAlignment() {
    return effectiveAlignment;
  }

  /** @return the padding character */
  public char getPaddingChar() {
    return paddingChar;
  }

  /** @return the null sentinel character, or {@link Field#UNSET_NULL_CHAR} when not configured */
  public char getNullChar() {
    return nullChar;
  }

  /** @return the literal null sentinel string, or the empty string when not configured */
  public String getNullValue() {
    return nullValue;
  }

  /** @return the configured formatter class ({@code ByTypeFormatter} when not overridden) */
  public Class<? extends FixedFormatter<?>> getFormatterClass() {
    return formatterClass;
  }

  /** @return the repetition count; {@code 1} for non-repeating fields */
  public int getRepeatCount() {
    return repeatCount;
  }

  /** @return {@code true} when the field's type is itself a {@code @Record} class loaded recursively */
  public boolean isNestedRecord() {
    return nestedRecord;
  }

  @Override
  public String toString() {
    return "FieldInfo{" + propertyName + " offset=" + offset + " length=" + length
        + " type=" + dataType.getName() + "}";
  }
}
