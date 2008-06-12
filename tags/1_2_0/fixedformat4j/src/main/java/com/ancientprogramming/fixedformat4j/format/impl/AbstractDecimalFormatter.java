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

import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Base class for formatting decimal data
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public abstract class AbstractDecimalFormatter<T> extends AbstractNumberFormatter<T> {

  private static final Log LOG = LogFactory.getLog(AbstractDecimalFormatter.class);

  protected static final char DECIMAL_SEPARATOR;
  protected static final char GROUPING_SEPARATOR;
  protected static final String ZERO_STRING;
  protected static final DecimalFormat FORMATTER;

  static {
    FORMATTER = new DecimalFormat();
    FORMATTER.setDecimalSeparatorAlwaysShown(true);

    DECIMAL_SEPARATOR = FORMATTER.getDecimalFormatSymbols().getDecimalSeparator();
    GROUPING_SEPARATOR = FORMATTER.getDecimalFormatSymbols().getGroupingSeparator();
    ZERO_STRING = "0" + DECIMAL_SEPARATOR + "0";

  }

  public String asString(T obj, FormatInstructions instructions) {

    String rawString = obj != null ? FORMATTER.format(obj) : ZERO_STRING;
    if (LOG.isDebugEnabled()) {
      LOG.debug("rawString: " + rawString + " - G[" + GROUPING_SEPARATOR + "] D[" + DECIMAL_SEPARATOR + "]");
    }
    rawString = rawString.replaceAll("\\" + GROUPING_SEPARATOR, "");
    boolean useDecimalDelimiter = instructions.getFixedFormatDecimalData().isUseDecimalDelimiter();

    String beforeDelimiter = rawString.substring(0, rawString.indexOf(DECIMAL_SEPARATOR));
    String afterDelimiter = rawString.substring(rawString.indexOf(DECIMAL_SEPARATOR)+1, rawString.length());
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeDelimiter[" + beforeDelimiter + "], afterDelimiter[" + afterDelimiter + "]");
    }

    int decimals = instructions.getFixedFormatDecimalData().getDecimals();
    //trim decimals
    afterDelimiter = StringUtils.substring(afterDelimiter, 0, decimals);
    afterDelimiter = StringUtils.rightPad(afterDelimiter, decimals, '0');

    String delimiter = useDecimalDelimiter ? "" + instructions.getFixedFormatDecimalData().getDecimalDelimiter() : "";
    String result = beforeDelimiter + delimiter + afterDelimiter;
    if (LOG.isDebugEnabled()) {
      LOG.debug("result[" + result + "]");
    }
    return result;
  }
 
  protected String getStringToConvert(String string, FormatInstructions instructions) {
    String toConvert;
    boolean useDecimalDelimiter = instructions.getFixedFormatDecimalData().isUseDecimalDelimiter();
    if(useDecimalDelimiter) {
      char delimiter = instructions.getFixedFormatDecimalData().getDecimalDelimiter();
      toConvert = string.replace(delimiter, '.'); //convert to normal delimiter
    } else {
      int decimals = instructions.getFixedFormatDecimalData().getDecimals();
      if (decimals > 0 && string.length() >= decimals) {
        String beforeDelimiter = string.substring(0, string.length()-decimals);
        String afterDelimiter = string.substring(string.length()-decimals, string.length());
        toConvert = beforeDelimiter + '.' + afterDelimiter;
      } else {
        toConvert = string;
      }

    }
    return toConvert;
  }
}
