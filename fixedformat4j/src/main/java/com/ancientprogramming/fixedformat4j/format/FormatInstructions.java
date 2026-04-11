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
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatEnumData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;

/**
 * Contains instructions on how to export and load fixed formatted data.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FormatInstructions {

  private int length;
  private Align alignment;
  private char paddingChar;
  private FixedFormatPatternData fixedFormatPatternData;
  private FixedFormatBooleanData fixedFormatBooleanData;
  private FixedFormatNumberData fixedFormatNumberData;
  private FixedFormatDecimalData fixedFormatDecimalData;
  private FixedFormatEnumData fixedFormatEnumData;

  /**
   * Creates a fully-populated set of format instructions.
   * Delegates to the 8-argument constructor with {@link FixedFormatEnumData#DEFAULT}.
   *
   * @param length                  the fixed width of the field in characters
   * @param alignment               the alignment strategy used to pad and strip the field
   * @param paddingChar             the character used for padding
   * @param fixedFormatPatternData  date/time pattern configuration, or {@code null} if unused
   * @param fixedFormatBooleanData  boolean value configuration, or {@code null} if unused
   * @param fixedFormatNumberData   number sign configuration, or {@code null} if unused
   * @param fixedFormatDecimalData  decimal precision configuration, or {@code null} if unused
   */
  public FormatInstructions(int length, Align alignment, char paddingChar, FixedFormatPatternData fixedFormatPatternData, FixedFormatBooleanData fixedFormatBooleanData, FixedFormatNumberData fixedFormatNumberData, FixedFormatDecimalData fixedFormatDecimalData) {
    this(length, alignment, paddingChar, fixedFormatPatternData, fixedFormatBooleanData, fixedFormatNumberData, fixedFormatDecimalData, FixedFormatEnumData.DEFAULT);
  }

  /**
   * Creates a fully-populated set of format instructions including enum configuration.
   *
   * @param length                  the fixed width of the field in characters
   * @param alignment               the alignment strategy used to pad and strip the field
   * @param paddingChar             the character used for padding
   * @param fixedFormatPatternData  date/time pattern configuration, or {@code null} if unused
   * @param fixedFormatBooleanData  boolean value configuration, or {@code null} if unused
   * @param fixedFormatNumberData   number sign configuration, or {@code null} if unused
   * @param fixedFormatDecimalData  decimal precision configuration, or {@code null} if unused
   * @param fixedFormatEnumData     enum serialization configuration, or {@code null} if unused
   */
  public FormatInstructions(int length, Align alignment, char paddingChar, FixedFormatPatternData fixedFormatPatternData, FixedFormatBooleanData fixedFormatBooleanData, FixedFormatNumberData fixedFormatNumberData, FixedFormatDecimalData fixedFormatDecimalData, FixedFormatEnumData fixedFormatEnumData) {
    this.length = length;
    this.alignment = alignment;
    this.paddingChar = paddingChar;
    this.fixedFormatPatternData = fixedFormatPatternData;
    this.fixedFormatBooleanData = fixedFormatBooleanData;
    this.fixedFormatNumberData = fixedFormatNumberData;
    this.fixedFormatDecimalData = fixedFormatDecimalData;
    this.fixedFormatEnumData = fixedFormatEnumData;
  }

  /**
   * Returns the fixed character width of the field.
   *
   * @return the field length in characters
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns the alignment strategy used to pad and strip the field value.
   *
   * @return the {@link Align} constant for this field
   */
  public Align getAlignment() {
    return alignment;
  }

  /**
   * Returns the character used to pad the field to its full length.
   *
   * @return the padding character
   */
  public char getPaddingChar() {
    return paddingChar;
  }

  /**
   * Returns the date/time pattern configuration for this field.
   *
   * @return the {@link FixedFormatPatternData}, or {@code null} if no pattern annotation is present
   */
  public FixedFormatPatternData getFixedFormatPatternData() {
    return fixedFormatPatternData;
  }

  /**
   * Returns the boolean value configuration for this field.
   *
   * @return the {@link FixedFormatBooleanData}, or {@code null} if no boolean annotation is present
   */
  public FixedFormatBooleanData getFixedFormatBooleanData() {
    return fixedFormatBooleanData;
  }

  /**
   * Returns the decimal precision configuration for this field.
   *
   * @return the {@link FixedFormatDecimalData}, or {@code null} if no decimal annotation is present
   */
  public FixedFormatDecimalData getFixedFormatDecimalData() {
    return fixedFormatDecimalData;
  }

  /**
   * Returns the number sign configuration for this field.
   *
   * @return the {@link FixedFormatNumberData}, or {@code null} if no number annotation is present
   */
  public FixedFormatNumberData getFixedFormatNumberData() {
    return fixedFormatNumberData;
  }

  /**
   * Returns the enum serialization configuration for this field.
   *
   * @return the {@link FixedFormatEnumData}, never {@code null} (defaults to {@link FixedFormatEnumData#DEFAULT})
   */
  public FixedFormatEnumData getFixedFormatEnumData() {
    return fixedFormatEnumData;
  }

  public String toString() {
    return String.format("FormatInstructions{length=%d, alignment=%s, paddingChar='%c', fixedFormatPatternData=%s, fixedFormatBooleanData=%s, fixedFormatNumberData=%s, fixedFormatDecimalData=%s, fixedFormatEnumData=%s}",
        length, alignment, paddingChar, fixedFormatPatternData, fixedFormatBooleanData, fixedFormatNumberData, fixedFormatDecimalData, fixedFormatEnumData);
  }
}
