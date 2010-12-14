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
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
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
public class TestIntegerFormatter extends TestCase {

  private FixedFormatter formatter = new IntegerFormatter();

  public void testParse() {
    assertEquals(100, formatter.parse("0000000100", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(100, formatter.parse("100", new FormatInstructions(3, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(1, formatter.parse("01", new FormatInstructions(2, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(1234, formatter.parse("0000001234", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(0, formatter.parse("+000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), null)));
    assertEquals(0, formatter.parse("-000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), null)));
    assertEquals(-1234, formatter.parse("-000001234", new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), null)));
  }

  public void testFormat() {
    assertEquals("+000000100", formatter.format(100, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000000101", formatter.format(101, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(1, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000001234", formatter.format(1234, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("-000001234", formatter.format(-1234, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000000000", formatter.format(0, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
    assertEquals("+000000000", formatter.format(null, new FormatInstructions(10, Align.RIGHT, '0', null, null, new FixedFormatNumberData(Sign.PREPEND, DEFAULT_POSITIVE_SIGN, DEFAULT_NEGATIVE_SIGN), new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY))));
  }
}
