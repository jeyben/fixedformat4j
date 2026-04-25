package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatEnumData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ByTypeFormatter dispatch to each known type formatter.
 */
public class TestByTypeFormatter {

  enum Season { SPRING, SUMMER, AUTUMN, WINTER }

  private ByTypeFormatter formatterFor(Class<?> type) {
    return new ByTypeFormatter(new FormatContext(1, type, ByTypeFormatter.class));
  }

  private FormatInstructions strInstr(int len) {
    return new FormatInstructions(len, Align.LEFT, ' ', null, null, null, null);
  }

  private FormatInstructions numInstr(int len) {
    return new FormatInstructions(len, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null);
  }

  @Test
  public void testDispatchesToStringFormatter() {
    ByTypeFormatter f = formatterFor(String.class);
    assertEquals("hello     ", f.format("hello", strInstr(10)));
    assertEquals("hello", f.parse("hello     ", strInstr(10)));
  }

  @Test
  public void testDispatchesToIntegerFormatter() {
    ByTypeFormatter f = formatterFor(Integer.class);
    assertEquals(42, f.parse("00042", numInstr(5)));
  }

  @Test
  public void testDispatchesToIntPrimitive() {
    ByTypeFormatter f = formatterFor(int.class);
    assertEquals(7, f.parse("00007", numInstr(5)));
  }

  @Test
  public void testDispatchesToLongFormatter() {
    ByTypeFormatter f = formatterFor(Long.class);
    assertEquals(123L, f.parse("0000000123", numInstr(10)));
  }

  @Test
  public void testDispatchesToLongPrimitive() {
    ByTypeFormatter f = formatterFor(long.class);
    assertEquals(9L, f.parse("00009", numInstr(5)));
  }

  @Test
  public void testDispatchesToShortFormatter() {
    ByTypeFormatter f = formatterFor(Short.class);
    assertEquals((short) 3, f.parse("00003", numInstr(5)));
  }

  @Test
  public void testDispatchesToShortPrimitive() {
    ByTypeFormatter f = formatterFor(short.class);
    assertEquals((short) 5, f.parse("00005", numInstr(5)));
  }

  @Test
  public void testDispatchesToDoubleFormatter() {
    ByTypeFormatter f = formatterFor(Double.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals(1.23, f.parse("00123", instr));
  }

  @Test
  public void testDispatchesToDoublePrimitive() {
    ByTypeFormatter f = formatterFor(double.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals(0.50, f.parse("00050", instr));
  }

  @Test
  public void testDispatchesToFloatFormatter() {
    ByTypeFormatter f = formatterFor(Float.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals(1.23F, f.parse("00123", instr));
  }

  @Test
  public void testDispatchesToFloatPrimitive() {
    ByTypeFormatter f = formatterFor(float.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals(0.50F, f.parse("00050", instr));
  }

  @Test
  public void testDispatchesToBigDecimalFormatter() {
    ByTypeFormatter f = formatterFor(BigDecimal.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals(new BigDecimal("1.23"), f.parse("00123", instr));
  }

  @Test
  public void testDispatchesToBooleanFormatter() {
    ByTypeFormatter f = formatterFor(Boolean.class);
    FormatInstructions instr = new FormatInstructions(1, Align.LEFT, ' ', null,
        new FixedFormatBooleanData("T", "F"), null, null);
    assertEquals(true, f.parse("T", instr));
    assertEquals(false, f.parse("F", instr));
  }

  @Test
  public void testDispatchesToBooleanPrimitive() {
    ByTypeFormatter f = formatterFor(boolean.class);
    FormatInstructions instr = new FormatInstructions(1, Align.LEFT, ' ', null,
        new FixedFormatBooleanData("Y", "N"), null, null);
    assertEquals(true, f.parse("Y", instr));
  }

  @Test
  public void testDispatchesToCharacterFormatter() {
    ByTypeFormatter f = formatterFor(Character.class);
    FormatInstructions instr = new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null);
    assertEquals('X', f.parse("X", instr));
  }

  @Test
  public void testDispatchesToCharPrimitive() {
    ByTypeFormatter f = formatterFor(char.class);
    FormatInstructions instr = new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null);
    assertEquals('Z', f.parse("Z", instr));
  }

  @Test
  public void testDispatchesToDateFormatter() {
    ByTypeFormatter f = formatterFor(Date.class);
    FormatInstructions instr = new FormatInstructions(8, Align.LEFT, ' ',
        new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    assertNotNull(f.parse("20080514", instr));
  }

  @Test
  public void testDispatchesToLocalDateFormatter() {
    ByTypeFormatter f = formatterFor(LocalDate.class);
    FormatInstructions instr = new FormatInstructions(8, Align.LEFT, ' ',
        new FixedFormatPatternData("yyyyMMdd"), null, null, null);
    assertEquals(LocalDate.of(2026, 4, 5), f.parse("20260405", instr));
    assertEquals("20260405", f.format(LocalDate.of(2026, 4, 5), instr));
  }

  @Test
  public void testDispatchesToLocalDateTimeFormatter() {
    ByTypeFormatter f = formatterFor(LocalDateTime.class);
    FormatInstructions instr = new FormatInstructions(19, Align.LEFT, ' ',
        new FixedFormatPatternData("yyyy-MM-dd'T'HH:mm:ss"), null, null, null);
    assertEquals(LocalDateTime.of(2026, 4, 9, 14, 30, 0), f.parse("2026-04-09T14:30:00", instr));
    assertEquals("2026-04-09T14:30:00", f.format(LocalDateTime.of(2026, 4, 9, 14, 30, 0), instr));
  }

  @Test
  public void testUnknownTypeThrowsFixedFormatException() {
    ByTypeFormatter f = formatterFor(UUID.class);
    FormatInstructions instr = strInstr(36);
    assertThrows(FixedFormatException.class, () -> f.parse("some-uuid-string--------------------", instr));
  }

  @Test
  public void testUnknownTypeExceptionMessageContainsTypeName() {
    ByTypeFormatter f = formatterFor(UUID.class);
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> f.parse("some-uuid-string--------------------", strInstr(36)));
    assertTrue(ex.getMessage().contains("UUID"),
        "exception message should name the unsupported type: " + ex.getMessage());
  }

  // --- Format assertions (so type-swap mutations die) ---

  @Test
  public void testIntegerFormatterFormat() {
    ByTypeFormatter f = formatterFor(Integer.class);
    assertEquals("00042", f.format(42, numInstr(5)));
  }

  @Test
  public void testLongFormatterFormat() {
    ByTypeFormatter f = formatterFor(Long.class);
    assertEquals("0000000123", f.format(123L, numInstr(10)));
  }

  @Test
  public void testShortFormatterFormat() {
    ByTypeFormatter f = formatterFor(Short.class);
    assertEquals("00003", f.format((short) 3, numInstr(5)));
  }

  @Test
  public void testBooleanFormatterFormat() {
    ByTypeFormatter f = formatterFor(Boolean.class);
    FormatInstructions instr = new FormatInstructions(1, Align.LEFT, ' ', null,
        new FixedFormatBooleanData("T", "F"), null, null);
    assertEquals("T", f.format(true, instr));
    assertEquals("F", f.format(false, instr));
  }

  @Test
  public void testCharacterFormatterFormat() {
    ByTypeFormatter f = formatterFor(Character.class);
    FormatInstructions instr = new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null);
    assertEquals("X", f.format('X', instr));
  }

  @Test
  public void testDoubleFormatterFormat() {
    ByTypeFormatter f = formatterFor(Double.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null,
        FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals("00123", f.format(1.23, instr));
  }

  @Test
  public void testBigDecimalFormatterFormat() {
    ByTypeFormatter f = formatterFor(BigDecimal.class);
    FormatInstructions instr = new FormatInstructions(5, Align.RIGHT, '0', null, null,
        FixedFormatNumberData.DEFAULT,
        new FixedFormatDecimalData(2, false, '.', RoundingMode.UNNECESSARY));
    assertEquals("00123", f.format(new BigDecimal("1.23"), instr));
  }

  // --- Enum dispatch ---

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testDispatchesToEnumFormatter_parse() {
    ByTypeFormatter f = new ByTypeFormatter(new FormatContext(1, Season.class, ByTypeFormatter.class));
    FormatInstructions instr = new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null,
        new FixedFormatEnumData(EnumFormat.LITERAL));
    assertEquals(Season.SUMMER, f.parse("SUMMER    ", instr));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testDispatchesToEnumFormatter_format() {
    ByTypeFormatter f = new ByTypeFormatter(new FormatContext(1, Season.class, ByTypeFormatter.class));
    FormatInstructions instr = new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null,
        new FixedFormatEnumData(EnumFormat.LITERAL));
    assertEquals("WINTER    ", f.format(Season.WINTER, instr));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testEnumTypeRoutesToEnumFormatter() {
    ByTypeFormatter f = new ByTypeFormatter(new FormatContext(1, Season.class, ByTypeFormatter.class));
    assertTrue(f.actualFormatter(Season.class) instanceof EnumFormatter,
        "enum type should route to EnumFormatter");
  }

  @Test
  public void testStringTypeRoutesToStringFormatter() {
    ByTypeFormatter f = formatterFor(String.class);
    assertTrue(f.actualFormatter(String.class) instanceof StringFormatter,
        "String type should route to StringFormatter");
  }
}
