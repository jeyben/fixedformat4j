package com.ancientprogramming.fixedformat4j.annotation;

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSign {

  @Test
  public void testSignNoSign() {
    assertEquals("0000000000", Sign.NOSIGN.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0000000000", Sign.NOSIGN.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0000001000", Sign.NOSIGN.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("10000", Sign.NOSIGN.remove("0000010000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  @Test
  public void testSignAppend() {
    assertEquals("000000000+", Sign.APPEND.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("000000000+", Sign.APPEND.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("000001000+", Sign.APPEND.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("000001000-", Sign.APPEND.apply("-1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("1000", Sign.APPEND.remove("000001000+", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("1001", Sign.APPEND.remove("000001001+", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0", Sign.APPEND.remove("000000000+", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0", Sign.APPEND.remove("000000000-", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("-1", Sign.APPEND.remove("000000001-", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("-2001", Sign.APPEND.remove("000002001-", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  @Test
  public void testSignZeroWithPrepend() {
    // Zero should get positive sign
    assertEquals("+000000000", Sign.PREPEND.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    // Removing sign from zero-valued string
    assertEquals("0", Sign.PREPEND.remove("+000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  @Test
  public void testSignZeroWithAppend() {
    assertEquals("000000000+", Sign.APPEND.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0", Sign.APPEND.remove("000000000+", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  @Test
  public void testRemoveSign_negativeZeroWithSpacePadding_stripsSign_prepend() {
    // valueWithoutSign after padding removal is "0" (not empty) — exercises the "0".equals branch
    assertEquals("0", Sign.PREPEND.remove("-0", new FormatInstructions(2, Align.LEFT, ' ', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  @Test
  public void testRemoveSign_negativeZeroWithSpacePadding_stripsSign_append() {
    // valueWithoutSign after padding removal is "0" (not empty) — exercises the "0".equals branch
    assertEquals("0", Sign.APPEND.remove("0-", new FormatInstructions(2, Align.LEFT, ' ', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

  // --- LEFT alignment: the sign must not consume the value's leading digit ---

  private FormatInstructions leftInstr(int length) {
    return new FormatInstructions(length, Align.LEFT, ' ', null, null, FixedFormatNumberData.DEFAULT, null);
  }

  @Test
  public void testSignPrependLeftAlignKeepsLeadingDigit() {
    assertEquals("+5   ", Sign.PREPEND.apply("5", leftInstr(5)));
    assertEquals("-5   ", Sign.PREPEND.apply("-5", leftInstr(5)));
    assertEquals("+1000 ", Sign.PREPEND.apply("1000", leftInstr(6)));
    assertEquals("+0   ", Sign.PREPEND.apply("0", leftInstr(5)));
  }

  @Test
  public void testSignAppendLeftAlignKeepsLeadingDigit() {
    assertEquals("5   +", Sign.APPEND.apply("5", leftInstr(5)));
    assertEquals("5   -", Sign.APPEND.apply("-5", leftInstr(5)));
    assertEquals("1000 +", Sign.APPEND.apply("1000", leftInstr(6)));
    assertEquals("0   +", Sign.APPEND.apply("0", leftInstr(5)));
  }

  @Test
  public void testSignPrependLeftAlignRoundTrip() {
    assertEquals("1000", Sign.PREPEND.remove(Sign.PREPEND.apply("1000", leftInstr(6)), leftInstr(6)));
    assertEquals("-1000", Sign.PREPEND.remove(Sign.PREPEND.apply("-1000", leftInstr(6)), leftInstr(6)));
    assertEquals("0", Sign.PREPEND.remove(Sign.PREPEND.apply("0", leftInstr(6)), leftInstr(6)));
  }

  @Test
  public void testSignAppendLeftAlignRoundTrip() {
    assertEquals("1000", Sign.APPEND.remove(Sign.APPEND.apply("1000", leftInstr(6)), leftInstr(6)));
    assertEquals("-1000", Sign.APPEND.remove(Sign.APPEND.apply("-1000", leftInstr(6)), leftInstr(6)));
    assertEquals("0", Sign.APPEND.remove(Sign.APPEND.apply("0", leftInstr(6)), leftInstr(6)));
  }

  @Test
  public void testSignPrepend() {
    assertEquals("+000000000", Sign.PREPEND.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("+000000000", Sign.PREPEND.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("+000001000", Sign.PREPEND.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("1000", Sign.PREPEND.remove("+000001000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("1001", Sign.PREPEND.remove("+000001001", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0", Sign.PREPEND.remove("+000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0", Sign.PREPEND.remove("-000000000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("-1", Sign.PREPEND.remove("-000000001", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("-2001", Sign.PREPEND.remove("-000002001", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }
}
