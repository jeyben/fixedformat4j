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
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Parameterized format/parse coverage for the three formatter data config classes:
 * {@link FixedFormatBooleanData}, {@link FixedFormatNumberData}, and {@link FixedFormatDecimalData}.
 *
 * <p><strong>Sections and their row schemas:</strong>
 * <ul>
 *   <li><em>Boolean</em> — {@code (trueValue, falseValue, length, align, pad, Boolean input, String expected)}
 *   <li><em>Boolean invalid parse</em> — {@code (trueValue, falseValue, length, String invalidInput)}
 *   <li><em>Integer sign</em> — {@code (signing, posSign, negSign, length, align, pad, Integer input, String expected)}
 *   <li><em>Long sign</em> — {@code (signing, posSign, negSign, length, align, pad, Long input, String expected)}
 *   <li><em>BigDecimal decimal config (NOSIGN)</em> — {@code (decimals, useDelimiter, delimiter, rounding, length, align, pad, BigDecimal input, String expected)}
 *   <li><em>BigDecimal sign + decimal config</em> — {@code (signing, decimals, useDelimiter, rounding, length, align, pad, BigDecimal input, String expected)}
 * </ul>
 *
 * <p>To add new cases, append a row to the relevant factory method. Every non-exception case is
 * verified in both directions: {@code format → String} and {@code parse → value}.
 */
public class TestBooleanNumberDecimalFormatting {

  // =========================================================================
  // Section 1 — Boolean (FixedFormatBooleanData)
  // Row schema: (trueValue, falseValue, length, align, pad, Boolean input, String expected)
  // =========================================================================

  static Stream<Arguments> booleanCases() {
    return Stream.of(
        // trueValue  falseValue  len  align       pad    input   expected
        arguments("T",    "F",    1, Align.LEFT,  ' ', true,  "T"),
        arguments("T",    "F",    1, Align.LEFT,  ' ', false, "F"),
        arguments("Y",    "N",    1, Align.LEFT,  ' ', true,  "Y"),
        arguments("1",    "0",    1, Align.LEFT,  ' ', false, "0"),
        arguments("true", "false",5, Align.LEFT,  ' ', true,  "true "),
        arguments("T",    "F",    3, Align.RIGHT, ' ', true,  "  T"),
        arguments("T",    "F",    3, Align.RIGHT, ' ', false, "  F")
    );
  }

  @ParameterizedTest(name = "[{index}] Boolean true=''{0}'' false=''{1}'' {3} input={5}")
  @MethodSource("booleanCases")
  void formatsAndParsesBooleanValue(
      String trueValue, String falseValue, int length, Align align, char pad,
      Boolean input, String expected) {
    BooleanFormatter formatter = new BooleanFormatter();
    FormatInstructions instr = booleanInstructions(length, align, pad, trueValue, falseValue);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // -------------------------------------------------------------------------
  // Boolean — invalid parse throws FixedFormatException
  // Row schema: (trueValue, falseValue, length, String invalidInput)
  // -------------------------------------------------------------------------

  static Stream<Arguments> booleanInvalidCases() {
    return Stream.of(
        // trueValue  falseValue  len  invalidInput
        arguments("T",    "F",    1, "X"),
        arguments("Y",    "N",    1, "T"),
        arguments("true", "false",5, "maybe")
    );
  }

  @ParameterizedTest(name = "[{index}] Boolean true=''{0}'' false=''{1}'' parse ''{3}'' → exception")
  @MethodSource("booleanInvalidCases")
  void parseBooleanInvalidValueThrowsException(
      String trueValue, String falseValue, int length, String invalidInput) {
    BooleanFormatter formatter = new BooleanFormatter();
    FormatInstructions instr = booleanInstructions(length, Align.LEFT, ' ', trueValue, falseValue);
    assertThrows(FixedFormatException.class, () -> formatter.parse(invalidInput, instr));
  }

  // =========================================================================
  // Section 2 — Integer with FixedFormatNumberData
  // Row schema: (signing, posSign, negSign, length, align, pad, Integer input, String expected)
  // =========================================================================

  static Stream<Arguments> integerSignCases() {
    return Stream.of(
        // signing         pos  neg  len  align       pad    input   expected
        arguments(Sign.NOSIGN,  '+', '-', 5, Align.LEFT,  ' ',  123,  "123  "),
        arguments(Sign.NOSIGN,  '+', '-', 5, Align.RIGHT, '0',  123,  "00123"),
        arguments(Sign.NOSIGN,  '+', '-', 5, Align.RIGHT, '0',  0,    "00000"),
        arguments(Sign.PREPEND, '+', '-', 6, Align.RIGHT, '0',  123,  "+00123"),
        arguments(Sign.PREPEND, '+', '-', 6, Align.RIGHT, '0', -456,  "-00456"),
        arguments(Sign.PREPEND, '+', '-', 6, Align.RIGHT, '0',  0,    "+00000"),
        arguments(Sign.APPEND,  '+', '-', 6, Align.RIGHT, '0',  123,  "00123+"),
        arguments(Sign.APPEND,  '+', '-', 6, Align.RIGHT, '0', -456,  "00456-"),
        arguments(Sign.APPEND,  '+', '-', 6, Align.RIGHT, '0',  0,    "00000+")
    );
  }

  @ParameterizedTest(name = "[{index}] Integer {0} {3}{4}'{5}' input={6}")
  @MethodSource("integerSignCases")
  void formatsAndParsesIntegerWithSign(
      Sign signing, char posSign, char negSign, int length, Align align, char pad,
      Integer input, String expected) {
    IntegerFormatter formatter = new IntegerFormatter();
    FormatInstructions instr = numberInstructions(length, align, pad, signing, posSign, negSign);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // =========================================================================
  // Section 3 — Long with FixedFormatNumberData
  // Row schema: (signing, posSign, negSign, length, align, pad, Long input, String expected)
  // =========================================================================

  static Stream<Arguments> longSignCases() {
    return Stream.of(
        // signing         pos  neg  len  align       pad    input              expected
        arguments(Sign.NOSIGN,  '+', '-', 10, Align.RIGHT, '0',  123456789L,  "0123456789"),
        arguments(Sign.NOSIGN,  '+', '-', 10, Align.RIGHT, '0',  0L,          "0000000000"),
        arguments(Sign.PREPEND, '+', '-', 11, Align.RIGHT, '0',  123456789L,  "+0123456789"),
        arguments(Sign.PREPEND, '+', '-', 11, Align.RIGHT, '0', -123456789L,  "-0123456789"),
        arguments(Sign.APPEND,  '+', '-', 11, Align.RIGHT, '0',  123456789L,  "0123456789+"),
        arguments(Sign.APPEND,  '+', '-', 11, Align.RIGHT, '0', -123456789L,  "0123456789-")
    );
  }

  @ParameterizedTest(name = "[{index}] Long {0} {3}{4}'{5}' input={6}")
  @MethodSource("longSignCases")
  void formatsAndParsesLongWithSign(
      Sign signing, char posSign, char negSign, int length, Align align, char pad,
      Long input, String expected) {
    LongFormatter formatter = new LongFormatter();
    FormatInstructions instr = numberInstructions(length, align, pad, signing, posSign, negSign);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // =========================================================================
  // Section 4 — BigDecimal with FixedFormatDecimalData (NOSIGN, no explicit sign)
  // Row schema: (decimals, useDelimiter, delimiter, rounding, length, align, pad, BigDecimal input, String expected)
  //
  // NOSIGN means the sign is not managed by FixedFormatNumberData — any '-' from
  // DecimalFormat becomes part of the data string (see case for -100.50 below).
  // =========================================================================

  static Stream<Arguments> bigDecimalDecimalDataCases() {
    return Stream.of(
        // dec  useDelim  delim  rounding    len  align       pad    input                      expected
        arguments(2, false, '.', RoundingMode.HALF_UP,  9, Align.RIGHT, '0', new BigDecimal("100.50"),  "000010050"),
        arguments(2, true,  '.', RoundingMode.HALF_UP,  9, Align.RIGHT, '0', new BigDecimal("100.50"),  "000100.50"),
        arguments(2, true,  '_', RoundingMode.HALF_UP,  9, Align.RIGHT, '0', new BigDecimal("100.50"),  "000100_50"),
        arguments(0, false, '.', RoundingMode.HALF_UP,  6, Align.RIGHT, '0', new BigDecimal("12345"),   "012345"),
        arguments(3, false, '.', RoundingMode.HALF_UP,  9, Align.RIGHT, '0', new BigDecimal("1.235"),   "000001235"),
        arguments(3, false, '.', RoundingMode.DOWN,     9, Align.RIGHT, '0', new BigDecimal("1.234"),   "000001234"),
        arguments(2, true,  '.', RoundingMode.HALF_UP,  5, Align.LEFT,  ' ', new BigDecimal("1.50"),    "1.50 "),
        arguments(2, false, '.', RoundingMode.HALF_UP,  5, Align.RIGHT, '0', BigDecimal.ZERO,           "00000"),
        // Negative with NOSIGN: DecimalFormat's '-' is embedded into the padded data string
        arguments(2, false, '.', RoundingMode.HALF_UP,  9, Align.RIGHT, '0', new BigDecimal("-100.50"), "000-10050")
    );
  }

  @ParameterizedTest(name = "[{index}] BigDecimal(NOSIGN) decimals={0} delim={1}/{2} {3} {5}{6}'{7}'")
  @MethodSource("bigDecimalDecimalDataCases")
  void formatsAndParsesBigDecimalWithDecimalData(
      int decimals, boolean useDelimiter, char delimiter, RoundingMode rounding,
      int length, Align align, char pad, BigDecimal input, String expected) {
    BigDecimalFormatter formatter = new BigDecimalFormatter();
    FormatInstructions instr = nosignDecimalInstructions(length, align, pad, decimals, useDelimiter, delimiter, rounding);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // =========================================================================
  // Section 5 — BigDecimal with FixedFormatNumberData + FixedFormatDecimalData
  // Row schema: (signing, decimals, useDelimiter, rounding, length, align, pad, BigDecimal input, String expected)
  //
  // PREPEND/APPEND sign is handled by AbstractNumberFormatter before and after
  // AbstractDecimalFormatter produces the raw value string.
  // =========================================================================

  static Stream<Arguments> bigDecimalSignAndDecimalCases() {
    return Stream.of(
        // signing         dec  useDelim  rounding           len  align       pad    input                     expected
        arguments(Sign.PREPEND, 2, false, RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("100.50"),  "+000010050"),
        arguments(Sign.PREPEND, 2, false, RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("-100.50"), "-000010050"),
        arguments(Sign.APPEND,  2, false, RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("100.50"),  "000010050+"),
        arguments(Sign.APPEND,  2, false, RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("-100.50"), "000010050-"),
        arguments(Sign.PREPEND, 2, true,  RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("100.50"),  "+000100.50"),
        arguments(Sign.PREPEND, 2, true,  RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("-100.50"), "-000100.50"),
        arguments(Sign.PREPEND, 3, true,  RoundingMode.HALF_UP, 10, Align.RIGHT, '0', new BigDecimal("123.456"), "+00123.456"),
        arguments(Sign.PREPEND, 2, true,  RoundingMode.HALF_UP, 10, Align.RIGHT, '0', BigDecimal.ZERO,           "+000000.00")
    );
  }

  @ParameterizedTest(name = "[{index}] BigDecimal {0} decimals={1} delim={2} {3}")
  @MethodSource("bigDecimalSignAndDecimalCases")
  void formatsAndParsesBigDecimalWithSignAndDecimalData(
      Sign signing, int decimals, boolean useDelimiter, RoundingMode rounding,
      int length, Align align, char pad, BigDecimal input, String expected) {
    BigDecimalFormatter formatter = new BigDecimalFormatter();
    FormatInstructions instr = signedDecimalInstructions(length, align, pad, signing, decimals, useDelimiter, rounding);
    assertAll(
        () -> assertEquals(expected, formatter.format(input, instr)),
        () -> assertEquals(input, formatter.parse(expected, instr))
    );
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static FormatInstructions booleanInstructions(
      int length, Align align, char pad, String trueValue, String falseValue) {
    return new FormatInstructions(length, align, pad, null,
        new FixedFormatBooleanData(trueValue, falseValue), null, null);
  }

  private static FormatInstructions numberInstructions(
      int length, Align align, char pad, Sign signing, char posSign, char negSign) {
    return new FormatInstructions(length, align, pad, null, null,
        new FixedFormatNumberData(signing, posSign, negSign), null);
  }

  private static FormatInstructions nosignDecimalInstructions(
      int length, Align align, char pad,
      int decimals, boolean useDelimiter, char delimiter, RoundingMode roundingMode) {
    return new FormatInstructions(length, align, pad, null, null,
        FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(decimals, useDelimiter, delimiter, roundingMode));
  }

  private static FormatInstructions signedDecimalInstructions(
      int length, Align align, char pad,
      Sign signing, int decimals, boolean useDelimiter, RoundingMode roundingMode) {
    return new FormatInstructions(length, align, pad, null, null,
        new FixedFormatNumberData(signing, '+', '-'),
        new FixedFormatDecimalData(decimals, useDelimiter, '.', roundingMode));
  }
}
