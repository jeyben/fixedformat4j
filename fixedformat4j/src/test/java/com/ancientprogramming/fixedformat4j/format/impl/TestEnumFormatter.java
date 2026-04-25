package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatEnumData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestEnumFormatter {

  enum Color { RED, GREEN, BLUE }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private final FormatContext<Color> colorContext =
      new FormatContext(1, Color.class, ByTypeFormatter.class);
  private final EnumFormatter formatter = new EnumFormatter(colorContext);

  private FormatInstructions literalInstr(int len) {
    return new FormatInstructions(len, Align.LEFT, ' ', null, null, null, null,
        new FixedFormatEnumData(EnumFormat.LITERAL));
  }

  private FormatInstructions numericInstr(int len) {
    return new FormatInstructions(len, Align.LEFT, ' ', null, null, null, null,
        new FixedFormatEnumData(EnumFormat.NUMERIC));
  }

  // --- LITERAL mode: parse ---

  @Test
  void literalParse_returnsRedConstant() {
    assertEquals(Color.RED, formatter.parse("RED       ", literalInstr(10)));
  }

  @Test
  void literalParse_returnsGreenConstant() {
    assertEquals(Color.GREEN, formatter.parse("GREEN     ", literalInstr(10)));
  }

  @Test
  void literalParse_returnsBlueConstant() {
    assertEquals(Color.BLUE, formatter.parse("BLUE      ", literalInstr(10)));
  }

  // --- LITERAL mode: format ---

  @Test
  void literalFormat_redProducesRedString() {
    assertEquals("RED       ", formatter.format(Color.RED, literalInstr(10)));
  }

  @Test
  void literalFormat_greenProducesGreenString() {
    assertEquals("GREEN     ", formatter.format(Color.GREEN, literalInstr(10)));
  }

  @Test
  void literalFormat_blueProducesBlueString() {
    assertEquals("BLUE      ", formatter.format(Color.BLUE, literalInstr(10)));
  }

  // --- LITERAL round-trip ---

  @Test
  void literalRoundTrip_allConstants() {
    FormatInstructions instr = literalInstr(10);
    for (Color c : Color.values()) {
      assertEquals(c, formatter.parse(formatter.format(c, instr), instr),
          "round-trip failed for " + c);
    }
  }

  // --- NUMERIC mode: parse ---

  @Test
  void numericParse_ordinalZeroIsRed() {
    assertEquals(Color.RED, formatter.parse("0  ", numericInstr(3)));
  }

  @Test
  void numericParse_ordinalOneIsGreen() {
    assertEquals(Color.GREEN, formatter.parse("1  ", numericInstr(3)));
  }

  @Test
  void numericParse_ordinalTwoIsBlue() {
    assertEquals(Color.BLUE, formatter.parse("2  ", numericInstr(3)));
  }

  // --- NUMERIC mode: format ---

  @Test
  void numericFormat_redIsOrdinalZero() {
    assertEquals("0  ", formatter.format(Color.RED, numericInstr(3)));
  }

  @Test
  void numericFormat_greenIsOrdinalOne() {
    assertEquals("1  ", formatter.format(Color.GREEN, numericInstr(3)));
  }

  @Test
  void numericFormat_blueIsOrdinalTwo() {
    assertEquals("2  ", formatter.format(Color.BLUE, numericInstr(3)));
  }

  // --- NUMERIC round-trip ---

  @Test
  void numericRoundTrip_allConstants() {
    FormatInstructions instr = numericInstr(3);
    for (Color c : Color.values()) {
      assertEquals(c, formatter.parse(formatter.format(c, instr), instr),
          "numeric round-trip failed for " + c);
    }
  }

  // --- LITERAL vs NUMERIC produce distinct output ---

  @Test
  void literalAndNumericProduceDistinctOutput() {
    assertNotEquals(
        formatter.format(Color.RED, literalInstr(5)),
        formatter.format(Color.RED, numericInstr(5)),
        "LITERAL and NUMERIC should produce different strings for the same value");
  }

  // --- Null / empty handling ---

  @Test
  void allPaddingInput_literal_parsesToNull() {
    assertNull(formatter.parse("          ", literalInstr(10)));
  }

  @Test
  void allPaddingInput_numeric_parsesToNull() {
    assertNull(formatter.parse("   ", numericInstr(3)));
  }

  @Test
  void nullValue_literal_formatsToAllPadding() {
    assertEquals("          ", formatter.format(null, literalInstr(10)));
  }

  @Test
  void nullValue_numeric_formatsToAllPadding() {
    assertEquals("   ", formatter.format(null, numericInstr(3)));
  }

  // --- Right-alignment padding ---

  @Test
  void rightAlignedLiteralParse_stripsLeadingSpaces() {
    FormatInstructions instr = new FormatInstructions(10, Align.RIGHT, ' ', null, null, null, null,
        new FixedFormatEnumData(EnumFormat.LITERAL));
    assertEquals(Color.RED, formatter.parse("       RED", instr));
  }

  @Test
  void rightAlignedNumericParse_stripsLeadingSpaces() {
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, ' ', null, null, null, null,
        new FixedFormatEnumData(EnumFormat.NUMERIC));
    assertEquals(Color.BLUE, formatter.parse("    2", instr));
  }

  // --- Error paths ---

  @Test
  void unknownLiteralName_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> formatter.parse("PURPLE    ", literalInstr(10)));
  }

  @Test
  void unknownLiteralName_exceptionMessageContainsBadValue() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> formatter.parse("PURPLE    ", literalInstr(10)));
    assertTrue(ex.getMessage().contains("PURPLE"),
        "message should contain the bad value: " + ex.getMessage());
  }

  @Test
  void unknownLiteralName_exceptionMessageContainsEnumClassName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> formatter.parse("PURPLE    ", literalInstr(10)));
    assertTrue(ex.getMessage().contains(Color.class.getName()),
        "message should contain the enum class name: " + ex.getMessage());
  }

  @Test
  void outOfRangeOrdinal_positive_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> formatter.asObject("99", numericInstr(3)));
  }

  @Test
  void outOfRangeOrdinal_positive_exceptionMessageMentionsOutOfRange() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> formatter.asObject("99", numericInstr(3)));
    assertTrue(ex.getMessage().contains("out of range"),
        "message should mention out of range: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("99"),
        "message should contain the bad ordinal: " + ex.getMessage());
  }

  @Test
  void outOfRangeOrdinal_negative_exceptionMessageMentionsOutOfRange() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> formatter.asObject("-1", numericInstr(3)));
    assertTrue(ex.getMessage().contains("out of range"),
        "message should mention out of range: " + ex.getMessage());
  }

  @Test
  void nonNumericOrdinal_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> formatter.asObject("XYZ", numericInstr(3)));
  }

  @Test
  void nonNumericOrdinal_exceptionMessageMentionsCannotParseOrdinal() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> formatter.asObject("XYZ", numericInstr(3)));
    assertTrue(ex.getMessage().contains("Cannot parse ordinal"),
        "message should mention parse failure: " + ex.getMessage());
    assertTrue(ex.getMessage().contains(Color.class.getName()),
        "message should contain enum class: " + ex.getMessage());
  }

  // --- Default enum data (null → LITERAL) ---

  @Test
  void nullEnumData_defaultsToLiteralBehavior_parse() {
    // 7-arg constructor uses FixedFormatEnumData.DEFAULT (LITERAL)
    FormatInstructions instr = new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null);
    assertEquals(Color.BLUE, formatter.parse("BLUE      ", instr));
  }

  @Test
  void nullEnumData_defaultsToLiteralBehavior_format() {
    FormatInstructions instr = new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null);
    assertEquals("GREEN     ", formatter.format(Color.GREEN, instr));
  }
}
