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
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class TestDateFormatter {

  public FixedFormatter formatter = new DateFormatter();

  @Test
  public void testParse() {
    assertEquals(getDate(1979, 10, 13), formatter.parse("13101979", new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("ddMMyyyy"), null, null, null)));
    assertNull(formatter.parse("        ", new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("ddMMyyyy"), null, null, null)));
  }

  @Test
  public void testFormat() {
    assertEquals("10032008", formatter.format(getDate(2008, 3, 10), new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("ddMMyyyy"), null, null, null)));
    assertEquals("08", formatter.format(getDate(2008, 3, 10), new FormatInstructions(2, Align.LEFT, ' ', new FixedFormatPatternData("yy"), null, null, null)));
    assertEquals("  ", formatter.format(null, new FormatInstructions(2, Align.LEFT, ' ', new FixedFormatPatternData("yy"), null, null, null)));
  }

  @Test
  public void testAlternativePattern() {
    assertEquals("20080310", formatter.format(getDate(2008, 3, 10), new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("yyyyMMdd"), null, null, null)));
    assertEquals(getDate(2008, 3, 10), formatter.parse("20080310", new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("yyyyMMdd"), null, null, null)));
  }

  @Test
  public void testNullInputParseReturnsNull() {
    // all-spaces → stripped to empty → returns null
    assertNull(formatter.parse("        ", new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("yyyyMMdd"), null, null, null)));
  }

  @Test
  public void testNullFormatReturnsSpaces() {
    assertEquals("        ", formatter.format(null, new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("yyyyMMdd"), null, null, null)));
  }

  // --- Issue 33: paddingChar appears inside date pattern value ---

  @Test
  public void leftAlignedZeroPaddingDoesNotStripZeroValuedTimeComponents() {
    // seconds=00 would be incorrectly stripped when paddingChar='0' and Align.LEFT
    FormatInstructions instr = new FormatInstructions(20, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertEquals(getDateTime(2012, 9, 12, 11, 11, 0), formatter.parse("20120912111100000000", instr));
  }

  @Test
  public void rightAlignedZeroPaddingDoesNotStripLeadingZeroInDateComponents() {
    // month "01" starts with '0'; RIGHT alignment strips leading zeros
    FormatInstructions instr = new FormatInstructions(20, Align.RIGHT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertEquals(getDateTime(2012, 1, 5, 0, 0, 0), formatter.parse("00000020120105000000", instr));
  }

  @Test
  public void fieldLengthEqualToPatternLengthIsUnchanged() {
    FormatInstructions instr = new FormatInstructions(14, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertEquals(getDateTime(2012, 9, 12, 11, 11, 0), formatter.parse("20120912111100", instr));
  }

  @Test
  public void allZeroFieldWithZeroPaddingParsesToNull() {
    FormatInstructions instr = new FormatInstructions(20, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    assertNull(formatter.parse("00000000000000000000", instr));
  }

  @Test
  public void zeroPaddingRoundTrip() {
    FormatInstructions instr = new FormatInstructions(20, Align.LEFT, '0', new FixedFormatPatternData("yyyyMMddHHmmss"), null, null, null);
    Date original = getDateTime(2012, 9, 12, 11, 11, 0);
    String formatted = formatter.format(original, instr);
    assertEquals(original, formatter.parse(formatted, instr));
  }

  public Date getDate(int year, int month, int day) {
    return getDateTime(year, month, day, 0, 0, 0);
  }

  private Date getDateTime(int year, int month, int day, int hour, int minute, int second) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month - 1);
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }
}
