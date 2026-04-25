package com.ancientprogramming.fixedformat4j.format.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFixedFormatBooleanData {

  @Test
  void getTrueValue_returnsConstructorArg() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("YES", "NO");
    assertEquals("YES", data.getTrueValue());
  }

  @Test
  void getFalseValue_returnsConstructorArg() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("YES", "NO");
    assertEquals("NO", data.getFalseValue());
  }

  @Test
  void getTrueValue_distinctFromFalseValue() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("T", "F");
    assertNotEquals(data.getTrueValue(), data.getFalseValue());
  }

  @Test
  void getTrueValue_notConfusedWithFalseValue() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("TRUE_VAL", "FALSE_VAL");
    assertEquals("TRUE_VAL", data.getTrueValue());
    assertEquals("FALSE_VAL", data.getFalseValue());
  }

  @Test
  void defaultInstance_trueValueIsT() {
    assertEquals("T", FixedFormatBooleanData.DEFAULT.getTrueValue());
  }

  @Test
  void defaultInstance_falseValueIsF() {
    assertEquals("F", FixedFormatBooleanData.DEFAULT.getFalseValue());
  }

  @Test
  void toString_containsTrueValue() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("YES", "NO");
    assertTrue(data.toString().contains("YES"),
        "toString should contain trueValue: " + data.toString());
  }

  @Test
  void toString_containsFalseValue() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("YES", "NO");
    assertTrue(data.toString().contains("NO"),
        "toString should contain falseValue: " + data.toString());
  }

  @Test
  void toString_containsClassName() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("T", "F");
    assertTrue(data.toString().contains("FixedFormatBooleanData"),
        "toString should contain class name: " + data.toString());
  }

  @Test
  void toString_containsTrueValueFieldLabel() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("T", "F");
    assertTrue(data.toString().contains("trueValue"),
        "toString should label trueValue: " + data.toString());
  }

  @Test
  void toString_containsFalseValueFieldLabel() {
    FixedFormatBooleanData data = new FixedFormatBooleanData("T", "F");
    assertTrue(data.toString().contains("falseValue"),
        "toString should label falseValue: " + data.toString());
  }
}
