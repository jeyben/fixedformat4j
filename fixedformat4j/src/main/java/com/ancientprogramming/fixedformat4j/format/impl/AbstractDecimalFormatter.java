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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;

/**
 * Base class for formatting decimal data
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public abstract class AbstractDecimalFormatter<T> extends AbstractNumberFormatter<T> {

  private static final Log LOG = LogFactory.getLog(AbstractDecimalFormatter.class);

  public String asString(T obj, FormatInstructions instructions) {
    DecimalFormat formatter = new DecimalFormat();
    formatter.setDecimalSeparatorAlwaysShown(true);
    formatter.setMaximumFractionDigits(instructions.getFixedFormatDecimalData().getDecimals());
    formatter.setRoundingMode(instructions.getFixedFormatDecimalData().getRoundingMode());

    char decimalSeparator = formatter.getDecimalFormatSymbols().getDecimalSeparator();
    char groupingSeparator = formatter.getDecimalFormatSymbols().getGroupingSeparator();
    String zeroString = "0" + decimalSeparator + "0";

    String rawString = obj != null ? formatter.format(obj) : zeroString;
    if (LOG.isDebugEnabled()) {
      LOG.debug("rawString: " + rawString + " - G[" + groupingSeparator + "] D[" + decimalSeparator + "]");
    }
    rawString = rawString.replaceAll("\\" + groupingSeparator, "");
    boolean useDecimalDelimiter = instructions.getFixedFormatDecimalData().isUseDecimalDelimiter();

    String beforeDelimiter = rawString.substring(0, rawString.indexOf(decimalSeparator));
    String afterDelimiter = rawString.substring(rawString.indexOf(decimalSeparator)+1, rawString.length());
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
