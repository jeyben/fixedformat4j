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
package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;

import java.math.RoundingMode;

import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal.*;

/**
 * Data object containing the exact same data as {@link FixedFormatDecimal}
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FixedFormatDecimalData {

  private int decimals;
  private boolean useDecimalDelimiter;
  private char decimalDelimiter;
  private RoundingMode roundingMode;
  
  public static final FixedFormatDecimalData DEFAULT = new FixedFormatDecimalData(DECIMALS, USE_DECIMAL_DELIMITER, DECIMAL_DELIMITER, RoundingMode.valueOf(ROUNDING_MODE));

  /**
   * Creates a decimal data object.
   *
   * @param decimals             the number of decimal places
   * @param useDecimalDelimiter  {@code true} to include an explicit decimal delimiter in the field
   * @param decimalDelimiter     the delimiter character used when {@code useDecimalDelimiter} is {@code true}
   * @param roundingMode         the rounding mode applied when scaling the value
   */
  public FixedFormatDecimalData(int decimals, boolean useDecimalDelimiter, char decimalDelimiter, RoundingMode roundingMode) {
    this.decimals = decimals;
    this.useDecimalDelimiter = useDecimalDelimiter;
    this.decimalDelimiter = decimalDelimiter;
    this.roundingMode = roundingMode;
  }

  /**
   * Returns the number of decimal places.
   *
   * @return the decimal count
   */
  public int getDecimals() {
    return decimals;
  }

  /**
   * Returns whether an explicit decimal delimiter character is included in the formatted value.
   *
   * @return {@code true} if a delimiter is used; {@code false} for implicit decimals
   */
  public boolean isUseDecimalDelimiter() {
    return useDecimalDelimiter;
  }

  /**
   * Returns the decimal delimiter character.
   *
   * @return the delimiter character (only meaningful when {@link #isUseDecimalDelimiter()} is {@code true})
   */
  public char getDecimalDelimiter() {
    return decimalDelimiter;
  }

  /**
   * Returns the rounding mode applied when scaling to the configured number of decimal places.
   *
   * @return the {@link RoundingMode}
   */
  public RoundingMode getRoundingMode() {
    return roundingMode;
  }

  public String toString() {
    return String.format("FixedFormatDecimalData{decimals=%d, useDecimalDelimiter=%b, decimalDelimiter='%c', roundingMode='%s'}",
        decimals, useDecimalDelimiter, decimalDelimiter, roundingMode);
  }
}
