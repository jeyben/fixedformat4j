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
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Parameterized format/parse coverage for {@link LocalDateFormatter}, {@link DateFormatter},
 * and {@link LocalDateTimeFormatter}.
 *
 * <p><strong>Exact-fit cases</strong> (field length == pattern output length, LEFT align, space padding):
 * append rows to {@link #localDateCases()}, {@link #dateCases()}, or {@link #localDateTimeCases()}.
 *
 * <p><strong>Alignment / field-too-long cases</strong> (field length > pattern output length,
 * any alignment, any padding char — successful round-trip):
 * append rows to {@link #localDateAlignmentCases()}, {@link #dateAlignmentCases()}, or
 * {@link #localDateTimeAlignmentCases()}.
 *
 * <p><strong>Field-too-short cases</strong> (field length &lt; pattern output length — format
 * silently truncates, parse throws {@link FixedFormatException}):
 * append rows to {@link #localDateTooShortCases()}, {@link #dateTooShortCases()}, or
 * {@link #localDateTimeTooShortCases()}.
 *
 * <p>Each row schema per group:
 * <ul>
 *   <li>Exact-fit: {@code (pattern, input, expectedString)}
 *   <li>Alignment: {@code (alignment, paddingChar, pattern, fieldLength, input, expectedString)}
 *   <li>Too-short: {@code (alignment, paddingChar, pattern, fieldLength, input, expectedTruncated)}
 * </ul>
 */
public class TestDatePatternFormatting {

  // =========================================================================
  // Exact-fit cases (field length == pattern output, LEFT align, space pad)
  // =========================================================================

  // -------------------------------------------------------------------------
  // LocalDate — exact fit
  // -------------------------------------------------------------------------

  static Stream<Arguments> localDateCases() {
    return Stream.of(
        // pattern               input                           expected
        arguments("yyyyMMdd",        LocalDate.of(2026,  4, 10), "20260410"),
        arguments("dd/MM/yyyy",      LocalDate.of(2026,  4, 10), "10/04/2026"),
        arguments("yyyy-MM-dd",      LocalDate.of(2026,  4, 10), "2026-04-10"),
        arguments("yyyy''MM dd",     LocalDate.of(2026,  4, 10), "2026'04 10"),
        arguments("MM/dd/yyyy",      LocalDate.of(2026,  4, 10), "04/10/2026"),
        arguments("ddMMyyyy",        LocalDate.of(2000,  2, 29), "29022000"),
        arguments("yyyy-MM-dd",      LocalDate.of(2026, 12, 31), "2026-12-31"),
        arguments("yyyy-MM-dd",      LocalDate.of(2026,  1,  1), "2026-01-01")
    );
  }

  @ParameterizedTest(name = "[{index}] LocalDate pattern=''{0}'' → ''{2}''")
  @MethodSource("localDateCases")
  void formatsAndParsesLocalDate(String pattern, LocalDate input, String expected) {
    LocalDateFormatter formatter = new LocalDateFormatter();
    FormatInstructions instr = exactFitInstructions(expected.length(), pattern);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // -------------------------------------------------------------------------
  // Date — exact fit
  // -------------------------------------------------------------------------

  static Stream<Arguments> dateCases() {
    return Stream.of(
        // pattern                    input                                   expected
        arguments("yyyyMMdd",            dateOf(2026,  4, 10),                  "20260410"),
        arguments("dd/MM/yyyy",          dateOf(2026,  4, 10),                  "10/04/2026"),
        arguments("yyyy-MM-dd",          dateOf(2026, 12, 31),                  "2026-12-31"),
        arguments("yyyy''MM dd",         dateOf(2026,  4, 10),                  "2026'04 10"),
        arguments("yyyyMMddHHmmss",      dateTimeOf(2026,  4, 10, 14, 30, 0),   "20260410143000"),
        arguments("dd-MM-yyyy HH:mm:ss", dateTimeOf(2026,  4, 10,  0,  0, 0),   "10-04-2026 00:00:00"),
        arguments("yyyyMMddHHmmss",      dateTimeOf(2012,  9, 12, 11, 11, 0),   "20120912111100")
    );
  }

  @ParameterizedTest(name = "[{index}] Date pattern=''{0}'' → ''{2}''")
  @MethodSource("dateCases")
  void formatsAndParsesDate(String pattern, Date input, String expected) {
    DateFormatter formatter = new DateFormatter();
    FormatInstructions instr = exactFitInstructions(expected.length(), pattern);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // -------------------------------------------------------------------------
  // LocalDateTime — exact fit
  // -------------------------------------------------------------------------

  static Stream<Arguments> localDateTimeCases() {
    return Stream.of(
        // pattern                          input                                          expected
        arguments("yyyy-MM-dd'T'HH:mm:ss",  LocalDateTime.of(2026,  4, 10, 14, 30,  0), "2026-04-10T14:30:00"),
        arguments("yyyyMMddHHmmss",          LocalDateTime.of(2026,  4, 10, 14, 30,  0), "20260410143000"),
        arguments("yyyy''MM dd HH:mm:ss",    LocalDateTime.of(2026,  4, 10, 14, 30,  0), "2026'04 10 14:30:00"),
        arguments("yyyy-MM-dd HH:mm:ss",     LocalDateTime.of(2026, 12, 31, 23, 59, 59), "2026-12-31 23:59:59"),
        arguments("yyyy-MM-dd'T'HH:mm:ss",  LocalDateTime.of(2026,  1,  1,  0,  0,  0), "2026-01-01T00:00:00"),
        arguments("dd/MM/yyyy HH:mm",        LocalDateTime.of(2026,  4, 10,  8,  5,  0), "10/04/2026 08:05"),
        arguments("yyyyMMddHHmmss",          LocalDateTime.of(2012,  9, 12, 11, 11,  0), "20120912111100")
    );
  }

  @ParameterizedTest(name = "[{index}] LocalDateTime pattern=''{0}'' → ''{2}''")
  @MethodSource("localDateTimeCases")
  void formatsAndParsesLocalDateTime(String pattern, LocalDateTime input, String expected) {
    LocalDateTimeFormatter formatter = new LocalDateTimeFormatter();
    FormatInstructions instr = exactFitInstructions(expected.length(), pattern);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // =========================================================================
  // Alignment / field-too-long cases (field > pattern output, round-trip ok)
  // Row schema: (alignment, paddingChar, pattern, fieldLength, input, expectedString)
  // =========================================================================

  // -------------------------------------------------------------------------
  // LocalDate — alignment
  // -------------------------------------------------------------------------

  static Stream<Arguments> localDateAlignmentCases() {
    return Stream.of(
        // alignment   pad   pattern        fieldLen  input                     expected
        arguments(Align.RIGHT, ' ', "yyyyMMdd",   12, LocalDate.of(2026,  4, 10), "    20260410"),
        arguments(Align.LEFT,  ' ', "yyyyMMdd",   12, LocalDate.of(2026,  4, 10), "20260410    "),
        arguments(Align.RIGHT, '0', "yyyyMMdd",   12, LocalDate.of(2026,  1,  1), "000020260101"),
        arguments(Align.LEFT,  '0', "yyyyMMdd",   12, LocalDate.of(2026,  1,  1), "202601010000"),
        arguments(Align.RIGHT, ' ', "dd/MM/yyyy", 14, LocalDate.of(2026,  4, 10), "    10/04/2026"),
        arguments(Align.LEFT,  ' ', "yyyy-MM-dd", 12, LocalDate.of(2026, 12, 31), "2026-12-31  ")
    );
  }

  @ParameterizedTest(name = "[{index}] LocalDate {0} pad=''{1}'' pattern=''{2}'' len={3}")
  @MethodSource("localDateAlignmentCases")
  void formatsAndParsesLocalDateWithAlignment(
      Align alignment, char padding, String pattern, int fieldLength,
      LocalDate input, String expected) {
    LocalDateFormatter formatter = new LocalDateFormatter();
    FormatInstructions instr = instructions(fieldLength, alignment, padding, pattern);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // -------------------------------------------------------------------------
  // Date — alignment
  // -------------------------------------------------------------------------

  static Stream<Arguments> dateAlignmentCases() {
    return Stream.of(
        // alignment   pad   pattern            fieldLen  input                               expected
        arguments(Align.RIGHT, ' ', "yyyyMMdd",        12, dateOf(2026, 4, 10),                  "    20260410"),
        arguments(Align.LEFT,  ' ', "yyyyMMdd",        12, dateOf(2026, 4, 10),                  "20260410    "),
        arguments(Align.RIGHT, '0', "yyyyMMddHHmmss",  20, dateTimeOf(2012, 9, 12, 11, 11, 0),   "00000020120912111100"),
        arguments(Align.LEFT,  '0', "yyyyMMddHHmmss",  20, dateTimeOf(2012, 9, 12, 11, 11, 0),   "20120912111100000000"),
        arguments(Align.RIGHT, ' ', "dd/MM/yyyy",      14, dateOf(2026, 4, 10),                  "    10/04/2026")
    );
  }

  @ParameterizedTest(name = "[{index}] Date {0} pad=''{1}'' pattern=''{2}'' len={3}")
  @MethodSource("dateAlignmentCases")
  void formatsAndParsesDateWithAlignment(
      Align alignment, char padding, String pattern, int fieldLength,
      Date input, String expected) {
    DateFormatter formatter = new DateFormatter();
    FormatInstructions instr = instructions(fieldLength, alignment, padding, pattern);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // -------------------------------------------------------------------------
  // LocalDateTime — alignment
  // -------------------------------------------------------------------------

  static Stream<Arguments> localDateTimeAlignmentCases() {
    return Stream.of(
        // alignment   pad   pattern                    fieldLen  input                                      expected
        arguments(Align.RIGHT, ' ', "yyyyMMddHHmmss",          20, LocalDateTime.of(2026,  4, 10, 14, 30, 0), "      20260410143000"),
        arguments(Align.LEFT,  ' ', "yyyyMMddHHmmss",          20, LocalDateTime.of(2026,  4, 10, 14, 30, 0), "20260410143000      "),
        arguments(Align.RIGHT, '0', "yyyyMMddHHmmss",          20, LocalDateTime.of(2012,  9, 12, 11, 11, 0), "00000020120912111100"),
        arguments(Align.LEFT,  '0', "yyyyMMddHHmmss",          20, LocalDateTime.of(2012,  9, 12, 11, 11, 0), "20120912111100000000"),
        arguments(Align.RIGHT, ' ', "yyyy-MM-dd'T'HH:mm:ss",  21, LocalDateTime.of(2026,  4, 10, 14, 30, 0), "  2026-04-10T14:30:00")
    );
  }

  @ParameterizedTest(name = "[{index}] LocalDateTime {0} pad=''{1}'' pattern=''{2}'' len={3}")
  @MethodSource("localDateTimeAlignmentCases")
  void formatsAndParsesLocalDateTimeWithAlignment(
      Align alignment, char padding, String pattern, int fieldLength,
      LocalDateTime input, String expected) {
    LocalDateTimeFormatter formatter = new LocalDateTimeFormatter();
    FormatInstructions instr = instructions(fieldLength, alignment, padding, pattern);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // =========================================================================
  // Field-too-short cases (field < pattern output length)
  // Format silently truncates; parse throws FixedFormatException.
  // Row schema: (alignment, paddingChar, pattern, fieldLength, input, expectedTruncated)
  // =========================================================================

  // -------------------------------------------------------------------------
  // LocalDate — too short
  // -------------------------------------------------------------------------

  static Stream<Arguments> localDateTooShortCases() {
    return Stream.of(
        // alignment   pad   pattern        fieldLen  input                     expectedTruncated
        arguments(Align.LEFT,  ' ', "yyyyMMdd",   6, LocalDate.of(2026,  4, 10), "202604"),
        arguments(Align.RIGHT, ' ', "yyyyMMdd",   6, LocalDate.of(2026,  4, 10), "260410"),
        arguments(Align.LEFT,  ' ', "yyyy-MM-dd", 7, LocalDate.of(2026,  4, 10), "2026-04")
    );
  }

  @ParameterizedTest(name = "[{index}] LocalDate too-short {0} pad=''{1}'' pattern=''{2}'' len={3}")
  @MethodSource("localDateTooShortCases")
  void localDateFormatTruncatesAndParseTooShortThrowsException(
      Align alignment, char padding, String pattern, int fieldLength,
      LocalDate input, String expectedTruncated) {
    LocalDateFormatter formatter = new LocalDateFormatter();
    FormatInstructions instr = instructions(fieldLength, alignment, padding, pattern);
    assertAll(
        () -> assertEquals(expectedTruncated, formatter.format(input, instr)),
        () -> assertThrows(FixedFormatException.class, () -> formatter.parse(expectedTruncated, instr))
    );
  }

  // -------------------------------------------------------------------------
  // Date — too short (format truncation only)
  //
  // SimpleDateFormat is lenient by default, so parsing a re-padded truncated
  // string may succeed (interpreting out-of-range components leniently) rather
  // than throwing FixedFormatException. We therefore only assert the format
  // direction here; the no-exception parse outcome is an accepted behaviour of
  // SimpleDateFormat and is not verified.
  // -------------------------------------------------------------------------

  static Stream<Arguments> dateTooShortCases() {
    return Stream.of(
        // alignment   pad   pattern      fieldLen  input                 expectedTruncated
        arguments(Align.LEFT,  ' ', "yyyyMMdd", 6, dateOf(2026, 4, 10), "202604"),
        arguments(Align.RIGHT, ' ', "yyyyMMdd", 6, dateOf(2026, 4, 10), "260410")
    );
  }

  @ParameterizedTest(name = "[{index}] Date too-short format {0} pad=''{1}'' pattern=''{2}'' len={3}")
  @MethodSource("dateTooShortCases")
  void dateFormatTruncatesWhenFieldTooShort(
      Align alignment, char padding, String pattern, int fieldLength,
      Date input, String expectedTruncated) {
    DateFormatter formatter = new DateFormatter();
    FormatInstructions instr = instructions(fieldLength, alignment, padding, pattern);
    assertEquals(expectedTruncated, formatter.format(input, instr));
  }

  // -------------------------------------------------------------------------
  // LocalDateTime — too short
  // -------------------------------------------------------------------------

  static Stream<Arguments> localDateTimeTooShortCases() {
    return Stream.of(
        // alignment   pad   pattern            fieldLen  input                                      expectedTruncated
        arguments(Align.LEFT,  ' ', "yyyyMMddHHmmss", 10, LocalDateTime.of(2026, 4, 10, 14, 30, 0), "2026041014"),
        arguments(Align.RIGHT, ' ', "yyyyMMddHHmmss", 10, LocalDateTime.of(2026, 4, 10, 14, 30, 0), "0410143000")
    );
  }

  @ParameterizedTest(name = "[{index}] LocalDateTime too-short {0} pad=''{1}'' pattern=''{2}'' len={3}")
  @MethodSource("localDateTimeTooShortCases")
  void localDateTimeFormatTruncatesAndParseTooShortThrowsException(
      Align alignment, char padding, String pattern, int fieldLength,
      LocalDateTime input, String expectedTruncated) {
    LocalDateTimeFormatter formatter = new LocalDateTimeFormatter();
    FormatInstructions instr = instructions(fieldLength, alignment, padding, pattern);
    assertAll(
        () -> assertEquals(expectedTruncated, formatter.format(input, instr)),
        () -> assertThrows(FixedFormatException.class, () -> formatter.parse(expectedTruncated, instr))
    );
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  /** Instructions where field length exactly matches the formatted value — no padding involved. */
  private static FormatInstructions exactFitInstructions(int length, String pattern) {
    return instructions(length, Align.LEFT, ' ', pattern);
  }

  private static FormatInstructions instructions(int length, Align alignment, char padding, String pattern) {
    return new FormatInstructions(length, alignment, padding, new FixedFormatPatternData(pattern), null, null, null);
  }

  private static Date dateOf(int year, int month, int day) {
    return dateTimeOf(year, month, day, 0, 0, 0);
  }

  private static Date dateTimeOf(int year, int month, int day, int hour, int minute, int second) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month - 1);
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }
}
