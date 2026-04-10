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
package com.ancientprogramming.fixedformat4j.issues;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue 33 - DateFormatter strips paddingChars that are part of the
 * date pattern value (e.g. seconds "00" removed when paddingChar is '0').
 */
public class TestIssue33 {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  @Test
  public void dateWithZeroValuedComponentsAndZeroPaddingParsesCorrectly() {
    Issue33Record record = manager.load(Issue33Record.class, "20120912111100000000");
    assertEquals(dateTime(2012, 9, 12, 11, 11, 0), record.getTimestamp());
  }

  @Test
  public void exportedDateRoundTripsCorrectly() {
    Issue33Record record = manager.load(Issue33Record.class, "20120912111100000000");
    String exported = manager.export(record);
    assertEquals("20120912111100000000", exported);
  }

  @Test
  public void nullDateFieldExportsAsAllPadding() {
    Issue33Record record = new Issue33Record();
    assertEquals("00000000000000000000", manager.export(record));
  }

  private Date dateTime(int year, int month, int day, int hour, int minute, int second) {
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

  @Record(length = 20)
  public static class Issue33Record {

    private Date timestamp;

    @Field(offset = 1, length = 20, paddingChar = '0', align = Align.LEFT)
    @FixedFormatPattern("yyyyMMddHHmmss")
    public Date getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }
  }
}
