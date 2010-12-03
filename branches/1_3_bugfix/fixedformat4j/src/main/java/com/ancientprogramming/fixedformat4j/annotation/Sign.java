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
package com.ancientprogramming.fixedformat4j.annotation;

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import org.apache.commons.lang.StringUtils;

/**
 * Sign defines where to place a sign defining a positive or negative number.
 * Is to be used in formatters operating numbers.
 * 
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.1.0
 */
public enum Sign {

  /**
   * Doesn't do anything with signs.
   * This just delegate to the {@link Align} defined in {@link FormatInstructions}.
   */
  NOSIGN {
    public String apply(String value, FormatInstructions instructions) {
      return instructions.getAlignment().apply(value, instructions.getLength(), instructions.getPaddingChar());
    }

    public String remove(String value, FormatInstructions instructions) {
      String result =  instructions.getAlignment().remove(value, instructions.getPaddingChar());
      if (StringUtils.isEmpty(result)) {
        result = "0";
      }
      return result;

    }
  },

  /**
   * Prepend the sign to the string
   */
  PREPEND {
    public String apply(String value, FormatInstructions instructions) {
      String sign = StringUtils.substring(value, 0, 1);
      if ("-".equals(sign)) {
        value = StringUtils.substring(value, 1);
      } else {
        sign = "+";
      }
      String result = instructions.getAlignment().apply(value, instructions.getLength(), instructions.getPaddingChar());
      return sign + StringUtils.substring(result, 1);
    }

    public String remove(String value, FormatInstructions instructions) {
      String sign = StringUtils.substring(value, 0, 1);
      String valueWithoutSign = StringUtils.substring(value, 1);
      String result = instructions.getAlignment().remove(valueWithoutSign, instructions.getPaddingChar());
      if (removeSign(instructions, sign, result)) {
        sign = "";
      }

      if (StringUtils.isEmpty(result)) {
        result = "0";
      }
      return sign + result;
    }
  },

  /**
   * Append the sign to the string
   */
  APPEND {
    public String apply(String value, FormatInstructions instructions) {
      String sign = StringUtils.substring(value, 0, 1);
      if ("-".equals(sign)) {
        value = StringUtils.substring(value, 1);
      } else {
        sign = "+";
      }
      String result = instructions.getAlignment().apply(value, instructions.getLength(), instructions.getPaddingChar());
      return StringUtils.substring(result, 1) + sign;

    }
    public String remove(String value, FormatInstructions instructions) {
      String sign = StringUtils.substring(value, value.length()-1);
      String valueWithoutSign = StringUtils.substring(value, 0, value.length()-1);
      String result = instructions.getAlignment().remove(valueWithoutSign, instructions.getPaddingChar());
      if (removeSign(instructions, sign, result)) {
        sign = "";
      }
      if (StringUtils.isEmpty(result)) {
        result = "0";
      }
      return sign + result;
    }
  };

  /**
   *remove sign in three cases:
   * 1. positive sign
   * 2. the unsigned value is empty (can happen if paddingchar is 0 and the value is zero)
   * 3. the unsigned value is 0 (can happen if paddingchar isn't 0 and the value is zero)
   * @param instructions
   * @param sign
   * @param valueWithoutSign
   * @return
   */
  private static boolean removeSign(FormatInstructions instructions, String sign, String valueWithoutSign) {
    return instructions.getFixedFormatNumberData().getPositiveSign().equals(sign.charAt(0)) ||
        StringUtils.isEmpty(valueWithoutSign) ||
        "0".equals(valueWithoutSign);
  }


  public abstract String apply(String value, FormatInstructions instructions);

  public abstract String remove(String value, FormatInstructions instructions);
}
