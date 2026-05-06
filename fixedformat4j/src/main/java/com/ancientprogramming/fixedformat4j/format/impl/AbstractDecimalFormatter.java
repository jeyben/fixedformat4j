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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Base class for formatting decimal data
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public abstract class AbstractDecimalFormatter<T extends Number> extends AbstractNumberFormatter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractDecimalFormatter.class);

  private static final Pattern ALL_ZEROS = Pattern.compile("^0+$");

  /** {@inheritDoc} */
  public String asString(T obj, FormatInstructions instructions) {
    BigDecimal roundedValue = null;
    int decimals = instructions.getFixedFormatDecimalData().getDecimals();
    if (obj != null) {
      BigDecimal value = obj instanceof BigDecimal ? (BigDecimal) obj : BigDecimal.valueOf(obj.doubleValue());
      RoundingMode roundingMode = instructions.getFixedFormatDecimalData().getRoundingMode();
      roundedValue = value.setScale(decimals, roundingMode);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Value before rounding = '{}', value after rounding = '{}', decimals = {}, rounding mode = {}", value, roundedValue, decimals, roundingMode);
      }
    }

    DecimalFormatCache.State state = DecimalFormatCache.get(decimals);

    String rawString = roundedValue != null ? state.format.format(roundedValue) : state.zeroString;
    if (LOG.isDebugEnabled()) {
      LOG.debug("rawString: {} - G[{}] D[{}]", rawString, state.groupingSeparator, state.decimalSeparator);
    }
    rawString = removeChar(rawString, state.groupingSeparator);
    boolean useDecimalDelimiter = instructions.getFixedFormatDecimalData().isUseDecimalDelimiter();

    int separatorIdx = rawString.indexOf(state.decimalSeparator);
    String beforeDelimiter = rawString.substring(0, separatorIdx);
    String afterDelimiter = rawString.substring(separatorIdx + 1);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeDelimiter[{}], afterDelimiter[{}]", beforeDelimiter, afterDelimiter);
    }

    afterDelimiter = StringUtils.substring(afterDelimiter, 0, decimals);
    afterDelimiter = StringUtils.rightPad(afterDelimiter, decimals, '0');

    String delimiter = useDecimalDelimiter ? String.valueOf(instructions.getFixedFormatDecimalData().getDecimalDelimiter()) : "";
    String result = beforeDelimiter + delimiter + afterDelimiter;
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
      toConvert = toConvert.replace(String.valueOf(delimiter), "");
    }
    boolean applyNegativeSign = false;
    final Character negativeSignChar = instructions.getFixedFormatNumberData().getNegativeSign();
    if (negativeSignChar != null && toConvert.startsWith(negativeSignChar.toString())) {
      toConvert = toConvert.substring(1);
      applyNegativeSign = true;
    }

    int decimals = instructions.getFixedFormatDecimalData().getDecimals();
    final boolean theZeroString = ALL_ZEROS.matcher(toConvert).matches();
    if (decimals > 0 && !theZeroString) {
      toConvert = StringUtils.leftPad(toConvert, decimals, "0");
      int pivot = toConvert.length() - decimals;
      toConvert = toConvert.substring(0, pivot) + '.' + toConvert.substring(pivot);
    }
    if (applyNegativeSign) {
      toConvert = "-".concat(toConvert);
    }
    return toConvert;
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
