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
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestBooleanFormatter {

  FixedFormatter formatter = new BooleanFormatter();

  @Test
  public void testParse() {
    assertEquals(true, formatter.parse("T", new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
    assertEquals(false, formatter.parse("F ", new FormatInstructions(2, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
    assertEquals(false, formatter.parse(" ", new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
    // empty string is treated as empty (not true/false value) → returns false
    assertEquals(false, formatter.parse("", new FormatInstructions(0, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
  }

  @Test
  public void testFormat() {
    assertEquals("T", formatter.format(true, new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
    assertEquals("F", formatter.format(false, new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
    assertEquals("F", formatter.format(null, new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
  }

  @Test
  public void testCustomTrueFalseValues() {
    assertEquals(true, formatter.parse("Y", new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("Y", "N"), null, null)));
    assertEquals(false, formatter.parse("N", new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("Y", "N"), null, null)));
    assertEquals("Y", formatter.format(true, new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("Y", "N"), null, null)));
    assertEquals("N", formatter.format(false, new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("Y", "N"), null, null)));
  }

  @Test
  public void testAllSpaceStringReturnsFalse() {
    // a space string is stripped to empty by padding removal, which returns false
    assertEquals(false, formatter.parse("   ", new FormatInstructions(3, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null)));
  }

  @Test
  public void testUnknownValueThrowsException() {
    assertThrows(com.ancientprogramming.fixedformat4j.exception.FixedFormatException.class, () ->
      formatter.parse("X", new FormatInstructions(1, Align.LEFT, ' ', null, new FixedFormatBooleanData("T", "F"), null, null))
    );
  }
}
