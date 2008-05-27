package com.ancientprogramming.fixedformat4j.annotation;

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import junit.framework.TestCase;

public class TestSign extends TestCase {

  public void testSignNoSign() {
    assertEquals("0000000000", Sign.NOSIGN.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("0000000000", Sign.NOSIGN.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("0000001000", Sign.NOSIGN.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("10000", Sign.NOSIGN.remove("0000010000", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
  }

  public void testSignAppend() {
    assertEquals("000000000+", Sign.APPEND.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("000000000+", Sign.APPEND.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("000001000+", Sign.APPEND.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("+1000", Sign.APPEND.remove("000001000+", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
  }

  public void testSignPrepend() {
    assertEquals("+000000000", Sign.PREPEND.apply("", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("+000000000", Sign.PREPEND.apply("0", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("+000001000", Sign.PREPEND.apply("1000", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
    assertEquals("+1000", Sign.PREPEND.remove("+000001000", new FormatInstructions(10, Align.RIGHT, '0', null, null, null, null)));
  }


}
