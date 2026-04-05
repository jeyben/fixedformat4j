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
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LocalDateFormatter}.
 *
 * @since 1.4.0
 */
public class TestLocalDateFormatter {

  private final FixedFormatter<LocalDate> formatter = new LocalDateFormatter();

  private FormatInstructions instructions(int length, String pattern) {
    return new FormatInstructions(length, Align.LEFT, ' ', new FixedFormatPatternData(pattern), null, null, null);
  }

  // --- Sunshine scenarios ---

  @Test
  public void testParseDefaultPattern() {
    LocalDate result = (LocalDate) formatter.parse("20260405", instructions(8, "yyyyMMdd"));
    assertEquals(LocalDate.of(2026, 4, 5), result);
  }

  @Test
  public void testParseAlternativePattern() {
    LocalDate result = (LocalDate) formatter.parse("05042026", instructions(8, "ddMMyyyy"));
    assertEquals(LocalDate.of(2026, 4, 5), result);
  }

  @Test
  public void testFormatDefaultPattern() {
    assertEquals("20260405", formatter.format(LocalDate.of(2026, 4, 5), instructions(8, "yyyyMMdd")));
  }

  @Test
  public void testFormatAlternativePattern() {
    assertEquals("05042026", formatter.format(LocalDate.of(2026, 4, 5), instructions(8, "ddMMyyyy")));
  }

  @Test
  public void testRoundTrip() {
    LocalDate original = LocalDate.of(2026, 4, 5);
    String formatted = formatter.format(original, instructions(8, "yyyyMMdd"));
    LocalDate parsed = (LocalDate) formatter.parse(formatted, instructions(8, "yyyyMMdd"));
    assertEquals(original, parsed);
  }

  @Test
  public void testLeapDay() {
    LocalDate result = (LocalDate) formatter.parse("20000229", instructions(8, "yyyyMMdd"));
    assertEquals(LocalDate.of(2000, 2, 29), result);
  }

  @Test
  public void testEndOfYear() {
    assertEquals(LocalDate.of(2026, 12, 31),
        formatter.parse("20261231", instructions(8, "yyyyMMdd")));
  }

  @Test
  public void testFirstDayOfYear() {
    assertEquals(LocalDate.of(2026, 1, 1),
        formatter.parse("20260101", instructions(8, "yyyyMMdd")));
  }

  // --- Edge cases ---

  @Test
  public void testNullFormatReturnsSpaces() {
    // Null value → asString returns null → AbstractFixedFormatter pads to spaces
    assertEquals("        ", formatter.format(null, instructions(8, "yyyyMMdd")));
  }

  @Test
  public void testAllSpaceInputParsesToNull() {
    // All-space input is stripped to "" by AbstractFixedFormatter → asObject receives "" → returns null
    assertNull(formatter.parse("        ", instructions(8, "yyyyMMdd")));
  }

  @Test
  public void testRightAlignedPaddingStrippedBeforeParse() {
    // Right-aligned: leading spaces are stripped before parse
    FormatInstructions instr = new FormatInstructions(10, Align.RIGHT, ' ', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    LocalDate result = (LocalDate) formatter.parse("  20260405", instr);
    assertEquals(LocalDate.of(2026, 4, 5), result);
  }

  @Test
  public void testInvalidDateStringThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        formatter.parse("99999999", instructions(8, "yyyyMMdd"))
    );
  }

  @Test
  public void testInvalidMonthThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        formatter.parse("20261399", instructions(8, "yyyyMMdd"))
    );
  }

  @Test
  public void testFormatYearOnlyPattern() {
    assertEquals("2026", formatter.format(LocalDate.of(2026, 6, 15), instructions(4, "yyyy")));
  }

  @Test
  public void testParseWithDashSeparator() {
    LocalDate result = (LocalDate) formatter.parse("2026-04-05", instructions(10, "yyyy-MM-dd"));
    assertEquals(LocalDate.of(2026, 4, 5), result);
  }

  @Test
  public void testFormatWithDashSeparator() {
    assertEquals("2026-04-05", formatter.format(LocalDate.of(2026, 4, 5), instructions(10, "yyyy-MM-dd")));
  }
}
