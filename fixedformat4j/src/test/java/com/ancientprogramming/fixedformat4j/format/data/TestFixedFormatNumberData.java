package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.Sign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFixedFormatNumberData {

  @Test
  void getSigning_returnsConstructorArg_nosign() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.NOSIGN, '+', '-');
    assertEquals(Sign.NOSIGN, data.getSigning());
  }

  @Test
  void getSigning_returnsConstructorArg_prepend() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.PREPEND, '+', '-');
    assertEquals(Sign.PREPEND, data.getSigning());
  }

  @Test
  void getSigning_returnsConstructorArg_append() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.APPEND, '+', '-');
    assertEquals(Sign.APPEND, data.getSigning());
  }

  @Test
  void getPositiveSign_returnsConstructorArg() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.PREPEND, '+', '-');
    assertEquals('+', (char) data.getPositiveSign());
  }

  @Test
  void getNegativeSign_returnsConstructorArg() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.PREPEND, '+', '-');
    assertEquals('-', (char) data.getNegativeSign());
  }

  @Test
  void positiveAndNegativeSigns_areDistinct() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.PREPEND, '+', '-');
    assertNotEquals((char) data.getPositiveSign(), (char) data.getNegativeSign());
  }

  @Test
  void positiveSign_notConfusedWithNegativeSign() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.APPEND, 'P', 'N');
    assertEquals('P', (char) data.getPositiveSign());
    assertEquals('N', (char) data.getNegativeSign());
  }

  @Test
  void defaultInstance_signingIsNoSign() {
    assertEquals(Sign.NOSIGN, FixedFormatNumberData.DEFAULT.getSigning());
  }

  @Test
  void defaultInstance_positiveSignIsPlus() {
    assertEquals('+', (char) FixedFormatNumberData.DEFAULT.getPositiveSign());
  }

  @Test
  void defaultInstance_negativeSignIsMinus() {
    assertEquals('-', (char) FixedFormatNumberData.DEFAULT.getNegativeSign());
  }

  @Test
  void toString_containsSigningValue() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.PREPEND, '+', '-');
    assertTrue(data.toString().contains("PREPEND"),
        "toString should contain signing: " + data.toString());
  }

  @Test
  void toString_containsPositiveSign() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.NOSIGN, '+', '-');
    assertTrue(data.toString().contains("+"),
        "toString should contain positiveSign: " + data.toString());
  }

  @Test
  void toString_containsNegativeSign() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.NOSIGN, '+', '-');
    assertTrue(data.toString().contains("-"),
        "toString should contain negativeSign: " + data.toString());
  }

  @Test
  void toString_containsClassName() {
    FixedFormatNumberData data = new FixedFormatNumberData(Sign.NOSIGN, '+', '-');
    assertTrue(data.toString().contains("FixedFormatNumberData"),
        "toString should contain class name: " + data.toString());
  }
}
