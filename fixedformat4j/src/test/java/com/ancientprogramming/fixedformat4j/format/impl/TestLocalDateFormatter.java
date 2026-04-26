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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

  // --- Issue 33: paddingChar appears inside date pattern value ---

  @Test
  public void leftAlignedZeroPaddingDoesNotStripZeroValuedMonthOrDay() {
    FormatInstructions instr = new FormatInstructions(12, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    assertEquals(LocalDate.of(2024, 1, 1), formatter.parse("202401010000", instr));
  }

  @Test
  public void rightAlignedZeroPaddingRestoresLeadingZerosInDateComponents() {
    FormatInstructions instr = new FormatInstructions(12, Align.RIGHT, '0', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    assertEquals(LocalDate.of(2024, 1, 1), formatter.parse("000020240101", instr));
  }

  @Test
  public void allZeroFieldWithZeroPaddingParsesToNull() {
    FormatInstructions instr = new FormatInstructions(12, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    assertNull(formatter.parse("000000000000", instr));
  }

  @Test
  public void zeroPaddingRoundTrip() {
    FormatInstructions instr = new FormatInstructions(12, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    LocalDate original = LocalDate.of(2024, 1, 1);
    String formatted = formatter.format(original, instr);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  // --- Message assertions for exception paths ---

  @Test
  public void testInvalidDateStringExceptionContainsBadInput() {
    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        formatter.parse("NOTADATE", instructions(8, "yyyyMMdd")));
    assertTrue(ex.getMessage().contains("NOTADATE"),
        "message should contain bad input: " + ex.getMessage());
  }

  @Test
  public void testInvalidDateStringExceptionContainsPattern() {
    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        formatter.parse("NOTADATE", instructions(8, "yyyyMMdd")));
    assertTrue(ex.getMessage().contains("yyyyMMdd"),
        "message should contain pattern: " + ex.getMessage());
  }

  @Test
  public void testInvalidDateStringExceptionContainsTypeName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        formatter.parse("NOTADATE", instructions(8, "yyyyMMdd")));
    assertTrue(ex.getMessage().contains(LocalDate.class.getName()),
        "message should contain type name: " + ex.getMessage());
  }

  // --- Non-space padding char ---

  @Test
  public void testStarPaddingChar_leftAlign_roundTrip() {
    FormatInstructions instr = new FormatInstructions(12, Align.LEFT, '*',
        new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    LocalDate original = LocalDate.of(2026, 4, 15);
    String formatted = formatter.format(original, instr);
    assertEquals("20260415****", formatted);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  @Test
  public void testStarPaddingChar_rightAlign_roundTrip() {
    FormatInstructions instr = new FormatInstructions(12, Align.RIGHT, '*',
        new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    LocalDate original = LocalDate.of(2026, 4, 15);
    String formatted = formatter.format(original, instr);
    assertEquals("****20260415", formatted);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  // --- Restoration: date value ends in padding char ---

  @Test
  public void zeroPaddingRoundTrip_dateEndsInPaddingChar_dayTen() {
    // "20241010" ends in '0' — stripping trailing zeros removes the last digit of the date value,
    // requiring the restoration branch in AbstractPatternFormatter.stripPadding to re-add it.
    FormatInstructions instr = new FormatInstructions(12, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    LocalDate original = LocalDate.of(2024, 10, 10);
    String formatted = formatter.format(original, instr);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  @Test
  public void zeroPaddingRoundTrip_dateEndsInPaddingChar_dayTwenty() {
    FormatInstructions instr = new FormatInstructions(12, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    LocalDate original = LocalDate.of(2024, 3, 20);
    String formatted = formatter.format(original, instr);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  // --- Direct computeFormattedLengthForPattern coverage ---
  // The ClassValue cache in AbstractPatternFormatter is pre-populated during PIT's coverage scan,
  // so mutations in computeFormattedLengthForPattern are unreachable via the normal parse path.
  // Calling the method directly bypasses the cache and lets PIT exercise the mutated bytecode.

  private static final class DirectFormatter extends LocalDateFormatter {
    int length(String pattern) { return computeFormattedLengthForPattern(pattern); }
  }

  private static final DirectFormatter DIRECT = new DirectFormatter();

  @Test
  public void computeFormattedLengthForPattern_compactPattern_returnsEight() {
    assertEquals(8, DIRECT.length("yyyyMMdd"));
  }

  @Test
  public void computeFormattedLengthForPattern_dashPattern_returnsTen() {
    assertEquals(10, DIRECT.length("yyyy-MM-dd"));
  }

  @Test
  public void computeFormattedLengthForPattern_yearOnly_returnsFour() {
    assertEquals(4, DIRECT.length("yyyy"));
  }

  // --- Concurrency ---

  @Test
  void parseIsThreadSafe() throws Exception {
    int threadCount = 20;
    FormatInstructions instr = instructions(8, "yyyyMMdd");
    LocalDate expected = LocalDate.of(1970, 1, 1);

    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Future<LocalDate>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return (LocalDate) formatter.parse("19700101", instr);
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    for (Future<LocalDate> future : futures) {
      assertEquals(expected, future.get());
    }
  }
}
