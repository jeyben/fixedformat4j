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
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestIntegerFormatter extends TestCase {

  private FixedFormatter formatter = new IntegerFormatter();

  public void testParse() {
    assertEquals(100, formatter.parse("+000000100", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(1234, formatter.parse("+000001234", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(0, formatter.parse("+000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(0, formatter.parse("-000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals(-1234, formatter.parse("-000001234", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  public void testFormat() {
    assertEquals("+000000100", formatter.format(100, new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.'))));
    assertEquals("+000000101", formatter.format(101, new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(1, false, '.'))));
    assertEquals("+000001234", formatter.format(1234, new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.'))));
    assertEquals("-000001234", formatter.format(-1234, new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.'))));
    assertEquals("+000000000", formatter.format(0, new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.'))));
    assertEquals("+000000000", formatter.format(null, new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, new FixedFormatDecimalData(2, false, '.'))));
  }
}
