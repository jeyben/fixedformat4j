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
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestDateFormatter extends TestCase {

  public FixedFormatter formatter = new DateFormatter();

  public void testParse() {
    Assert.assertEquals(getDate(1979, 10, 13), formatter.parse("13101979", new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("ddMMyyyy"), null, null, null)));
    Assert.assertEquals(null, formatter.parse("        ", new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("ddMMyyyy"), null, null, null)));
  }

  public void testFormat() {
    Assert.assertEquals("10032008", formatter.format(getDate(2008, 3, 10), new FormatInstructions(8, Align.LEFT, ' ', new FixedFormatPatternData("ddMMyyyy"), null, null, null)));
    Assert.assertEquals("08", formatter.format(getDate(2008, 3, 10), new FormatInstructions(2, Align.LEFT, ' ', new FixedFormatPatternData("yy"), null, null, null)));
    Assert.assertEquals("  ", formatter.format(null, new FormatInstructions(2, Align.LEFT, ' ', new FixedFormatPatternData("yy"), null, null, null)));
  }

  public Date getDate(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month - 1);
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }
}
