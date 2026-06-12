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

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Base class for formatting decimal data
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public abstract class AbstractDecimalFormatter<T extends Number> extends AbstractNumberFormatter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractDecimalFormatter.class);

  /** {@inheritDoc} */
  public String asString(T obj, FormatInstructions instructions) {
    FixedFormatDecimalData decimalData = instructions.getFixedFormatDecimalData();
    int decimals = decimalData.getDecimals();
    BigDecimal value = obj == null ? BigDecimal.ZERO
        : obj instanceof BigDecimal ? (BigDecimal) obj : BigDecimal.valueOf(obj.doubleValue());
    RoundingMode roundingMode = decimalData.getRoundingMode();
    BigDecimal roundedValue = value.setScale(decimals, roundingMode);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Value before rounding = '{}', value after rounding = '{}', decimals = {}, rounding mode = {}", value, roundedValue, decimals, roundingMode);
    }

    // toPlainString is locale-independent: '.' separator, no grouping, no exponent.
    // After setScale the fraction part is exactly 'decimals' digits (absent when decimals == 0).
    String rawString = roundedValue.toPlainString();
    int separatorIdx = rawString.indexOf('.');
    String beforeDelimiter = separatorIdx < 0 ? rawString : rawString.substring(0, separatorIdx);
    String afterDelimiter = separatorIdx < 0 ? "" : rawString.substring(separatorIdx + 1);

    String result = decimalData.isUseDecimalDelimiter()
        ? beforeDelimiter + decimalData.getDecimalDelimiter() + afterDelimiter
        : beforeDelimiter + afterDelimiter;
    if (LOG.isDebugEnabled()) {
      LOG.debug("result[{}]", result);
    }
    return result;
  }

  protected final String resolveDecimalString(String string, FormatInstructions instructions) {
    String toConvert = getStringToConvert(string, instructions);
    return "".equals(toConvert) ? "0" : toConvert;
  }

  protected String getStringToConvert(String string, FormatInstructions instructions) {
    String toConvert = string;
    boolean useDecimalDelimiter = instructions.getFixedFormatDecimalData().isUseDecimalDelimiter();
    if (useDecimalDelimiter) {
      char delimiter = instructions.getFixedFormatDecimalData().getDecimalDelimiter();
      toConvert = removeChar(toConvert, delimiter);
    }
    boolean applyNegativeSign = false;
    final Character negativeSignChar = instructions.getFixedFormatNumberData().getNegativeSign();
    if (negativeSignChar != null && toConvert.startsWith(negativeSignChar.toString())) {
      toConvert = toConvert.substring(1);
      applyNegativeSign = true;
    }

    int decimals = instructions.getFixedFormatDecimalData().getDecimals();
    final boolean theZeroString = isAllZeros(toConvert);
    if (decimals > 0 && !theZeroString) {
      if (toConvert.length() < decimals) {
        toConvert = "0".repeat(decimals - toConvert.length()) + toConvert;
      }
      int pivot = toConvert.length() - decimals;
      toConvert = toConvert.substring(0, pivot) + '.' + toConvert.substring(pivot);
    }
    if (applyNegativeSign) {
      toConvert = "-".concat(toConvert);
    }
    return toConvert;
  }

  private static boolean isAllZeros(String s) {
    if (s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) != '0') {
        return false;
      }
    }
    return true;
  }

  private static String removeChar(String s, char ch) {
    if (s.indexOf(ch) < 0) return s;
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) != ch) sb.append(s.charAt(i));
    }
    return sb.toString();
  }
}
