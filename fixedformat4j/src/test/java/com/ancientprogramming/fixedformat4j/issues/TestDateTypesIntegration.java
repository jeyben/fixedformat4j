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

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the full fixed-format API with Date, LocalDate, and LocalDateTime fields
 * using various patterns, including patterns that contain literal single-quote characters via
 * the {@code ''} escape (e.g. {@code "yyyy''MM dd"} → {@code "2026'04 10"}).
 *
 * <p>Each test performs a full round-trip: load a fixed-width string into a record, verify the
 * parsed field values, export back to a string, and assert the exported string matches the original.
 */
public class TestDateTypesIntegration {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  /**
   * Fixed-width string layout (80 chars total):
   *
   * <pre>
   * Offset  Len  Type            Pattern                   Value
   * ------  ---  --------------  ------------------------  -------------------
   *      1   14  Date            yyyyMMddHHmmss            20260410000000
   *     15   10  Date            dd/MM/yyyy                10/04/2026
   *     25    8  LocalDate       yyyyMMdd                  20260410
   *     33   10  LocalDate       yyyy''MM dd               2026'04 10
   *     43   19  LocalDateTime   yyyy-MM-dd'T'HH:mm:ss     2026-04-10T14:30:00
   *     62   19  LocalDateTime   yyyy''MM dd HH:mm:ss      2026'04 10 14:30:00
   * </pre>
   */
  private static final String RECORD_DATA =
      "20260410000000"        // Date  yyyyMMddHHmmss            (offset  1, len 14)
    + "10/04/2026"            // Date  dd/MM/yyyy                (offset 15, len 10)
    + "20260410"              // LocalDate  yyyyMMdd              (offset 25, len  8)
    + "2026'04 10"            // LocalDate  yyyy''MM dd           (offset 33, len 10)
    + "2026-04-10T14:30:00"   // LocalDateTime  yyyy-MM-dd'T'HH:mm:ss  (offset 43, len 19)
    + "2026'04 10 14:30:00";  // LocalDateTime  yyyy''MM dd HH:mm:ss   (offset 62, len 19)

  @Test
  public void loadsDateFieldsCorrectly() {
    DateTypesRecord record = manager.load(DateTypesRecord.class, RECORD_DATA);

    assertEquals(dateTimeOf(2026, 4, 10, 0, 0, 0), record.getTimestamp());
    assertEquals(dateOf(2026, 4, 10), record.getCalendarDate());
    assertEquals(LocalDate.of(2026, 4, 10), record.getLocalDate());
    assertEquals(LocalDate.of(2026, 4, 10), record.getLocalDateQuoted());
    assertEquals(LocalDateTime.of(2026, 4, 10, 14, 30, 0), record.getLocalDateTimeIso());
    assertEquals(LocalDateTime.of(2026, 4, 10, 14, 30, 0), record.getLocalDateTimeQuoted());
  }

  @Test
  public void exportedStringMatchesOriginal() {
    DateTypesRecord record = buildRecord();
    assertEquals(RECORD_DATA, manager.export(record));
  }

  @Test
  public void roundTripPreservesAllFields() {
    DateTypesRecord loaded = manager.load(DateTypesRecord.class, RECORD_DATA);
    String exported = manager.export(loaded);
    DateTypesRecord reloaded = manager.load(DateTypesRecord.class, exported);

    assertEquals(loaded.getTimestamp(), reloaded.getTimestamp());
    assertEquals(loaded.getCalendarDate(), reloaded.getCalendarDate());
    assertEquals(loaded.getLocalDate(), reloaded.getLocalDate());
    assertEquals(loaded.getLocalDateQuoted(), reloaded.getLocalDateQuoted());
    assertEquals(loaded.getLocalDateTimeIso(), reloaded.getLocalDateTimeIso());
    assertEquals(loaded.getLocalDateTimeQuoted(), reloaded.getLocalDateTimeQuoted());
  }

  private DateTypesRecord buildRecord() {
    DateTypesRecord record = new DateTypesRecord();
    record.setTimestamp(dateTimeOf(2026, 4, 10, 0, 0, 0));
    record.setCalendarDate(dateOf(2026, 4, 10));
    record.setLocalDate(LocalDate.of(2026, 4, 10));
    record.setLocalDateQuoted(LocalDate.of(2026, 4, 10));
    record.setLocalDateTimeIso(LocalDateTime.of(2026, 4, 10, 14, 30, 0));
    record.setLocalDateTimeQuoted(LocalDateTime.of(2026, 4, 10, 14, 30, 0));
    return record;
  }

  private static Date dateOf(int year, int month, int day) {
    return dateTimeOf(year, month, day, 0, 0, 0);
  }

  private static Date dateTimeOf(int year, int month, int day, int hour, int minute, int second) {
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

  // -------------------------------------------------------------------------
  // Record definition
  // -------------------------------------------------------------------------

  @Record(length = 80)
  public static class DateTypesRecord {

    private Date timestamp;
    private Date calendarDate;
    private LocalDate localDate;
    private LocalDate localDateQuoted;
    private LocalDateTime localDateTimeIso;
    private LocalDateTime localDateTimeQuoted;

    /** Date with a compact datetime pattern (no separators). */
    @Field(offset = 1, length = 14)
    @FixedFormatPattern("yyyyMMddHHmmss")
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    /** Date with a slash-separated date-only pattern. */
    @Field(offset = 15, length = 10)
    @FixedFormatPattern("dd/MM/yyyy")
    public Date getCalendarDate() { return calendarDate; }
    public void setCalendarDate(Date calendarDate) { this.calendarDate = calendarDate; }

    /** LocalDate with a compact pattern. */
    @Field(offset = 25, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate localDate) { this.localDate = localDate; }

    /**
     * LocalDate with a pattern that contains a literal {@code '} character via {@code ''}.
     * Pattern {@code yyyy''MM dd} produces e.g. {@code 2026'04 10}.
     */
    @Field(offset = 33, length = 10)
    @FixedFormatPattern("yyyy''MM dd")
    public LocalDate getLocalDateQuoted() { return localDateQuoted; }
    public void setLocalDateQuoted(LocalDate localDateQuoted) { this.localDateQuoted = localDateQuoted; }

    /** LocalDateTime with ISO-style pattern containing a quoted literal {@code T}. */
    @Field(offset = 43, length = 19)
    @FixedFormatPattern("yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getLocalDateTimeIso() { return localDateTimeIso; }
    public void setLocalDateTimeIso(LocalDateTime localDateTimeIso) { this.localDateTimeIso = localDateTimeIso; }

    /**
     * LocalDateTime with a pattern containing a literal {@code '} character via {@code ''}.
     * Pattern {@code yyyy''MM dd HH:mm:ss} produces e.g. {@code 2026'04 10 14:30:00}.
     */
    @Field(offset = 62, length = 19)
    @FixedFormatPattern("yyyy''MM dd HH:mm:ss")
    public LocalDateTime getLocalDateTimeQuoted() { return localDateTimeQuoted; }
    public void setLocalDateTimeQuoted(LocalDateTime localDateTimeQuoted) { this.localDateTimeQuoted = localDateTimeQuoted; }
  }
}
