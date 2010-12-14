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

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Sign;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber.DEFAULT_NEGATIVE_SIGN;
import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber.DEFAULT_POSITIVE_SIGN;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestBigDecimalFormatter extends TestCase {
  BigDecimalFormatter formatter = new BigDecimalFormatter();

  public void testParse() {
    assertEquals(new BigDecimal("100.50"), formatter.parse("+000010050", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals(new BigDecimal("1234.56"), formatter.parse("+000123456", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));

    //use delimiter
    assertEquals(new BigDecimal("1234.56"), formatter.parse("+001234.56", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.UNNECESSARY))));
    assertEquals(new BigDecimal("123.456"), formatter.parse("+00123_456", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '_', RoundingMode.UNNECESSARY))));

    //no decimals
    assertEquals(new BigDecimal("123456"), formatter.parse("+000123456", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(0, true, '_', RoundingMode.UNNECESSARY))));

    assertEquals(new BigDecimal("0"), formatter.parse("+000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(0, true, '_', RoundingMode.UNNECESSARY))));
    assertEquals(new BigDecimal("0"), formatter.parse("-000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(0, true, '_', RoundingMode.UNNECESSARY))));

    assertEquals(new BigDecimal("-100.50"), formatter.parse("-000010050", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));

    //test signing prepended
    assertEquals(new BigDecimal("0"), formatter.parse("000000000+", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.APPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.UNNECESSARY))));

    assertEquals(new BigDecimal("0"), formatter.parse("000000000+", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.APPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals(new BigDecimal("0"), formatter.parse("000000000-", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.APPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));

    assertEquals(new BigDecimal("-100.50"), formatter.parse("000010050-", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.APPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
  }

  public void testFormat() {
    assertEquals("+000010050", formatter.format(new BigDecimal(100.5), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000001005", formatter.format(new BigDecimal(100.51), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(1, false, '.', RoundingMode.HALF_UP))));
    assertEquals("+000123456", formatter.format(new BigDecimal(1234.562), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.HALF_UP))));

    assertEquals("+001234.56", formatter.format(new BigDecimal(1234.562), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP))));
    assertEquals("+00123.456", formatter.format(new BigDecimal(123.4562), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '.', RoundingMode.HALF_UP))));

    assertEquals("+00000.000", formatter.format(new BigDecimal(0.0), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+00000.000", formatter.format(null, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '.', RoundingMode.UNNECESSARY))));

    //the number is bigger than the total length
    assertEquals("+456789.01", formatter.format(new BigDecimal(123456789.01), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP))));

    assertEquals("-000010050", formatter.format(new BigDecimal(-100.5), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000000.00", formatter.format(new BigDecimal(0), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.UNNECESSARY))));
  }

  public void testFormatWithRounding() {
    // test rounding
    assertEquals("+000001.17", formatter.format(new BigDecimal(1.165), new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP))));
  }
  
  public void testFormatBigDecimalWith4DecimalsKeepAllDecimalsNoDecimalDelimiter() {
    assertEquals("+00012345612", formatter.format(new BigDecimal(1234.5612), new FormatInstructions(12, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(4, false, '.', RoundingMode.HALF_UP))));	    
  }

  public void testFormatBigDecimalWith5DecimalsTo4DecimalsNoDecimalDelimiter() {
    assertEquals("+00012345612", formatter.format(new BigDecimal(1234.56121), new FormatInstructions(12, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(4, false, '.', RoundingMode.HALF_UP))));
  }
  
  public void testFormatBigDecimalWith4DecimalsKeepAllDecimalsUseDecimalDelimiter () {
    assertEquals("+001234.5612", formatter.format(new BigDecimal(1234.5612), new FormatInstructions(12, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(4, true, '.', RoundingMode.HALF_UP))));  
  }
  
  public void testFormatBigDecimalWith5DecimalsTo4DecimalsUseDecimalDelimiter () {
    assertEquals("+00123.4561", formatter.format(new BigDecimal(123.45612), new FormatInstructions(11, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(4, true, '.', RoundingMode.HALF_UP))));  
  }
}
