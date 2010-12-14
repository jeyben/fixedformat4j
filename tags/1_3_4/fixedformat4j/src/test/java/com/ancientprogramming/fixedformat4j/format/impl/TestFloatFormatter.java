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

import java.math.RoundingMode;

import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber.DEFAULT_NEGATIVE_SIGN;
import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber.DEFAULT_POSITIVE_SIGN;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestFloatFormatter extends TestCase {
  FloatFormatter formatter = new FloatFormatter();

  public void testParse() {
    assertEquals(100.50F, formatter.parse("0000010050", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals(1234.56F, formatter.parse("0000123456", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));

    //use delimiter
    assertEquals(1234.56F, formatter.parse("0001234.56", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, true, '.', RoundingMode.UNNECESSARY))));
    assertEquals(123.456F, formatter.parse("000123_456", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(3, true, '_', RoundingMode.UNNECESSARY))));

    //no decimals
    assertEquals(123456.0F, formatter.parse("0000123456", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(0, true, '_', RoundingMode.UNNECESSARY))));

    assertEquals(0.0F, formatter.parse("0000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(0, true, '_', RoundingMode.UNNECESSARY))));

    assertEquals(0.0F, formatter.parse("-000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(0, true, '_', RoundingMode.UNNECESSARY))));

    assertEquals(-100.50F, formatter.parse("-000010050", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
  }

  public void testFormat() {
    assertEquals("+000010050", formatter.format(100.5F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000001005", formatter.format(100.51F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(1, false, '.', RoundingMode.HALF_UP))));
    assertEquals("+000123456", formatter.format(1234.562F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.HALF_UP))));

    assertEquals("+001234.56", formatter.format(1234.562F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP))));
    assertEquals("+00123.456", formatter.format(123.4562F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '.', RoundingMode.HALF_UP))));

    assertEquals("+00000.000", formatter.format(0.0F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+00000.000", formatter.format(null, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(3, true, '.', RoundingMode.UNNECESSARY))));

    //the number is bigger than the total length
    assertEquals("+45.01", formatter.format(12345.01F, new FormatInstructions(6, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP))));

    assertEquals("-000010050", formatter.format(-100.5F, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
  }
}