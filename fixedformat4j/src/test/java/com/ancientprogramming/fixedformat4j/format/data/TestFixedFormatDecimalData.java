package com.ancientprogramming.fixedformat4j.format.data;

import org.junit.jupiter.api.Test;

import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

public class TestFixedFormatDecimalData {

  @Test
  void getDecimals_returnsConstructorArg() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(3, true, ',', RoundingMode.HALF_UP);
    assertEquals(3, data.getDecimals());
  }

  @Test
  void isUseDecimalDelimiter_trueWhenPassedTrue() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP);
    assertTrue(data.isUseDecimalDelimiter());
  }

  @Test
  void isUseDecimalDelimiter_falseWhenPassedFalse() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, false, '.', RoundingMode.HALF_UP);
    assertFalse(data.isUseDecimalDelimiter());
  }

  @Test
  void getDecimalDelimiter_returnsConstructorArg() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, true, ',', RoundingMode.HALF_UP);
    assertEquals(',', data.getDecimalDelimiter());
  }

  @Test
  void getDecimalDelimiter_distinctForDifferentInputs() {
    FixedFormatDecimalData dot = new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP);
    FixedFormatDecimalData comma = new FixedFormatDecimalData(2, true, ',', RoundingMode.HALF_UP);
    assertNotEquals(dot.getDecimalDelimiter(), comma.getDecimalDelimiter());
  }

  @Test
  void getRoundingMode_returnsConstructorArg() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, false, '.', RoundingMode.CEILING);
    assertEquals(RoundingMode.CEILING, data.getRoundingMode());
  }

  @Test
  void getRoundingMode_notConfusedBetweenDifferentModes() {
    FixedFormatDecimalData up = new FixedFormatDecimalData(2, false, '.', RoundingMode.UP);
    FixedFormatDecimalData down = new FixedFormatDecimalData(2, false, '.', RoundingMode.DOWN);
    assertNotEquals(up.getRoundingMode(), down.getRoundingMode());
  }

  @Test
  void defaultInstance_hasExpectedDecimals() {
    assertEquals(2, FixedFormatDecimalData.DEFAULT.getDecimals());
  }

  @Test
  void defaultInstance_doesNotUseDecimalDelimiter() {
    assertFalse(FixedFormatDecimalData.DEFAULT.isUseDecimalDelimiter());
  }

  @Test
  void defaultInstance_delimiterIsDot() {
    assertEquals('.', FixedFormatDecimalData.DEFAULT.getDecimalDelimiter());
  }

  @Test
  void toString_containsDecimalsValue() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(4, true, ',', RoundingMode.HALF_UP);
    assertTrue(data.toString().contains("4"),
        "toString should contain decimals: " + data.toString());
  }

  @Test
  void toString_containsDelimiterValue() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, true, ',', RoundingMode.HALF_UP);
    assertTrue(data.toString().contains(","),
        "toString should contain delimiter: " + data.toString());
  }

  @Test
  void toString_containsRoundingMode() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, false, '.', RoundingMode.FLOOR);
    assertTrue(data.toString().contains("FLOOR"),
        "toString should contain rounding mode: " + data.toString());
  }

  @Test
  void toString_containsUseDecimalDelimiterValue() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, true, '.', RoundingMode.HALF_UP);
    assertTrue(data.toString().contains("true"),
        "toString should contain useDecimalDelimiter: " + data.toString());
  }

  @Test
  void toString_containsClassName() {
    FixedFormatDecimalData data = new FixedFormatDecimalData(2, false, '.', RoundingMode.HALF_UP);
    assertTrue(data.toString().contains("FixedFormatDecimalData"),
        "toString should contain class name: " + data.toString());
  }
}
