package com.ancientprogramming.fixedformat4j.annotation;

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import junit.framework.TestCase;

public class TestSign extends TestCase {

  public void testSignNoSign() {
    assertEquals("0000000000", Sign.NOSIGN.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0000000000", Sign.NOSIGN.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("0000001000", Sign.NOSIGN.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
    assertEquals("10000", Sign.NOSIGN.remove("0000010000", new FormatInstructions(10, Align.RIGHT, '0', null, null, FixedFormatNumberData.DEFAULT, null)));
  }

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
