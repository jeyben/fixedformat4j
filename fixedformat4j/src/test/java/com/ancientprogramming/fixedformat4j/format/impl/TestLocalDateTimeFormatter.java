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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LocalDateTimeFormatter}.
 *
 * @since 1.6.0
 */
public class TestLocalDateTimeFormatter {

  private final FixedFormatter<LocalDateTime> formatter = new LocalDateTimeFormatter();

  private FormatInstructions instructions(int length, String pattern) {
    return new FormatInstructions(length, Align.LEFT, ' ', new FixedFormatPatternData(pattern), null, null, null);
  }

  // --- Sunshine scenarios ---

  @Test
  public void testParseIsoPattern() {
    LocalDateTime result = (LocalDateTime) formatter.parse("2026-04-09T14:30:00", instructions(19, "yyyy-MM-dd'T'HH:mm:ss"));
    assertEquals(LocalDateTime.of(2026, 4, 9, 14, 30, 0), result);
  }

  @Test
  public void testFormatIsoPattern() {
    assertEquals("2026-04-09T14:30:00", formatter.format(LocalDateTime.of(2026, 4, 9, 14, 30, 0), instructions(19, "yyyy-MM-dd'T'HH:mm:ss")));
  }

  @Test
  public void testParseCompactPattern() {
    LocalDateTime result = (LocalDateTime) formatter.parse("20260409143000", instructions(14, "yyyyMMddHHmmss"));
    assertEquals(LocalDateTime.of(2026, 4, 9, 14, 30, 0), result);
  }

  @Test
  public void testFormatCompactPattern() {
    assertEquals("20260409143000", formatter.format(LocalDateTime.of(2026, 4, 9, 14, 30, 0), instructions(14, "yyyyMMddHHmmss")));
  }

  @Test
  public void testRoundTrip() {
    LocalDateTime original = LocalDateTime.of(2026, 4, 9, 14, 30, 45);
    String formatted = formatter.format(original, instructions(19, "yyyy-MM-dd'T'HH:mm:ss"));
    LocalDateTime parsed = (LocalDateTime) formatter.parse(formatted, instructions(19, "yyyy-MM-dd'T'HH:mm:ss"));
    assertEquals(original, parsed);
  }

  @Test
  public void testMidnight() {
    LocalDateTime result = (LocalDateTime) formatter.parse("2026-01-01T00:00:00", instructions(19, "yyyy-MM-dd'T'HH:mm:ss"));
    assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), result);
  }

  @Test
  public void testEndOfDay() {
    LocalDateTime result = (LocalDateTime) formatter.parse("2026-12-31T23:59:59", instructions(19, "yyyy-MM-dd'T'HH:mm:ss"));
    assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59, 59), result);
  }

  @Test
  public void testLeapDayDateTime() {
    LocalDateTime result = (LocalDateTime) formatter.parse("2000-02-29T12:00:00", instructions(19, "yyyy-MM-dd'T'HH:mm:ss"));
    assertEquals(LocalDateTime.of(2000, 2, 29, 12, 0, 0), result);
  }

  @Test
  public void testFormatWithSpaceSeparator() {
    LocalDateTime dt = LocalDateTime.of(2026, 4, 9, 8, 5, 3);
    assertEquals("2026-04-09 08:05:03", formatter.format(dt, instructions(19, "yyyy-MM-dd HH:mm:ss")));
  }

  @Test
  public void testParseWithSpaceSeparator() {
    FormatInstructions instr = new FormatInstructions(19, Align.LEFT, ' ', new FixedFormatPatternData("yyyy-MM-dd HH:mm:ss"), null, null, null);
    LocalDateTime result = (LocalDateTime) formatter.parse("2026-04-09 08:05:03", instr);
    assertEquals(LocalDateTime.of(2026, 4, 9, 8, 5, 3), result);
  }

  // --- Edge cases ---

  @Test
  public void testNullFormatReturnsSpaces() {
    assertEquals("                   ", formatter.format(null, instructions(19, "yyyy-MM-dd'T'HH:mm:ss")));
  }

  @Test
  public void testAllSpaceInputParsesToNull() {
    assertNull(formatter.parse("                   ", instructions(19, "yyyy-MM-dd'T'HH:mm:ss")));
  }

  @Test
  public void testRightAlignedPaddingStrippedBeforeParse() {
    FormatInstructions instr = new FormatInstructions(21, Align.RIGHT, ' ', new FixedFormatPatternData("yyyy-MM-dd'T'HH:mm:ss"), null, null, null);
    LocalDateTime result = (LocalDateTime) formatter.parse("  2026-04-09T14:30:00", instr);
    assertEquals(LocalDateTime.of(2026, 4, 9, 14, 30, 0), result);
  }

  @Test
  public void testInvalidDateStringThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        formatter.parse("9999-99-99T99:99:99", instructions(19, "yyyy-MM-dd'T'HH:mm:ss"))
    );
  }

  @Test
  public void testInvalidMonthThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        formatter.parse("2026-13-01T00:00:00", instructions(19, "yyyy-MM-dd'T'HH:mm:ss"))
    );
  }

  // --- Message assertions for exception paths ---

  @Test
  public void testInvalidDateTimeStringExceptionContainsPattern() {
    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        formatter.parse("NOT-A-DATE-------T-", instructions(19, "yyyy-MM-dd'T'HH:mm:ss")));
    assertTrue(ex.getMessage().contains("yyyy-MM-dd"),
        "message should contain pattern: " + ex.getMessage());
  }

  @Test
  public void testInvalidDateTimeStringExceptionContainsTypeName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        formatter.parse("NOT-A-DATE-------T-", instructions(19, "yyyy-MM-dd'T'HH:mm:ss")));
    assertTrue(ex.getMessage().contains(LocalDateTime.class.getName()),
        "message should contain type name: " + ex.getMessage());
  }

  @Test
  public void testInvalidDateTimeStringExceptionContainsBadInput() {
    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        formatter.parse("NOT-A-DATE-------T-", instructions(19, "yyyy-MM-dd'T'HH:mm:ss")));
    assertTrue(ex.getMessage().contains("NOT-A-DATE-------T-"),
        "message should contain bad input: " + ex.getMessage());
  }

  // --- Non-space padding char ---

  @Test
  public void testStarPaddingChar_leftAlign_roundTrip() {
    FormatInstructions instr = new FormatInstructions(21, Align.LEFT, '*',
        new FixedFormatPatternData("yyyy-MM-dd'T'HH:mm:ss"), null, null, null);
    LocalDateTime original = LocalDateTime.of(2026, 4, 15, 10, 30, 0);
    String formatted = formatter.format(original, instr);
    assertEquals("2026-04-15T10:30:00**", formatted);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  @Test
  public void testStarPaddingChar_rightAlign_roundTrip() {
    FormatInstructions instr = new FormatInstructions(21, Align.RIGHT, '*',
        new FixedFormatPatternData("yyyy-MM-dd'T'HH:mm:ss"), null, null, null);
    LocalDateTime original = LocalDateTime.of(2026, 4, 15, 10, 30, 0);
    String formatted = formatter.format(original, instr);
    assertEquals("**2026-04-15T10:30:00", formatted);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  // --- Issue 33: paddingChar appears inside date pattern value ---

  @Test
  public void leftAlignedZeroPaddingDoesNotStripZeroValuedTimeComponents() {
    FormatInstructions instr = new FormatInstructions(20, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertEquals(LocalDateTime.of(2012, 9, 12, 11, 11, 0), formatter.parse("20120912111100000000", instr));
  }

  @Test
  public void rightAlignedZeroPaddingRestoresLeadingZerosInDateComponents() {
    FormatInstructions instr = new FormatInstructions(20, Align.RIGHT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertEquals(LocalDateTime.of(2012, 1, 5, 0, 0, 0), formatter.parse("00000020120105000000", instr));
  }

  @Test
  public void allZeroFieldWithZeroPaddingParsesToNull() {
    FormatInstructions instr = new FormatInstructions(20, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertNull(formatter.parse("00000000000000000000", instr));
  }

  @Test
  public void zeroPaddingRoundTrip() {
    FormatInstructions instr = new FormatInstructions(20, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    LocalDateTime original = LocalDateTime.of(2012, 9, 12, 11, 11, 0);
    String formatted = formatter.format(original, instr);
    assertEquals(original, formatter.parse(formatted, instr));
  }
}
