package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFixedFormatEnumData {

  @Test
  void getEnumFormat_returnsLiteralWhenPassedLiteral() {
    FixedFormatEnumData data = new FixedFormatEnumData(EnumFormat.LITERAL);
    assertEquals(EnumFormat.LITERAL, data.getEnumFormat());
  }

  @Test
  void getEnumFormat_returnsNumericWhenPassedNumeric() {
    FixedFormatEnumData data = new FixedFormatEnumData(EnumFormat.NUMERIC);
    assertEquals(EnumFormat.NUMERIC, data.getEnumFormat());
  }

  @Test
  void getEnumFormat_literalAndNumericAreDistinct() {
    FixedFormatEnumData literal = new FixedFormatEnumData(EnumFormat.LITERAL);
    FixedFormatEnumData numeric = new FixedFormatEnumData(EnumFormat.NUMERIC);
    assertNotEquals(literal.getEnumFormat(), numeric.getEnumFormat());
  }

  @Test
  void defaultInstance_isLiteral() {
    assertEquals(EnumFormat.LITERAL, FixedFormatEnumData.DEFAULT.getEnumFormat());
  }

  @Test
  void toString_containsEnumFormatValue_literal() {
    FixedFormatEnumData data = new FixedFormatEnumData(EnumFormat.LITERAL);
    assertTrue(data.toString().contains("LITERAL"),
        "toString should contain LITERAL: " + data.toString());
  }

  @Test
  void toString_containsEnumFormatValue_numeric() {
    FixedFormatEnumData data = new FixedFormatEnumData(EnumFormat.NUMERIC);
    assertTrue(data.toString().contains("NUMERIC"),
        "toString should contain NUMERIC: " + data.toString());
  }

  @Test
  void toString_containsClassName() {
    FixedFormatEnumData data = new FixedFormatEnumData(EnumFormat.LITERAL);
    assertTrue(data.toString().contains("FixedFormatEnumData"),
        "toString should contain class name: " + data.toString());
  }

  @Test
  void toString_containsFieldLabel() {
    FixedFormatEnumData data = new FixedFormatEnumData(EnumFormat.LITERAL);
    assertTrue(data.toString().contains("enumFormat"),
        "toString should label the enumFormat field: " + data.toString());
  }
}
